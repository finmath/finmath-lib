/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 21.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements valuation of a European stock option.
 * 
 * @author Christian Fries
 * @version 1.3
 */
public class EuropeanOption extends AbstractAssetMonteCarloProduct {

	private final double maturity;
	private final double strike;
	
	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public EuropeanOption(double maturity, double strike) {
		super();
		this.maturity	= maturity;
		this.strike		= strike;
	}

    /**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    @Override
    public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {
		// Get underlying and numeraire
    	
    	// Get S(T)
		RandomVariableInterface underlyingAtMaturity	= model.getAssetValue(maturity,0);
		
		// The payoff: values = max(underlying - strike, 0) = V(T) = max(S(T)-K,0)
		RandomVariableInterface values = underlyingAtMaturity.sub(strike).floor(0.0);

		// Discounting...
		RandomVariableInterface numeraireAtMaturity		= model.getNumeraire(maturity);
		RandomVariableInterface monteCarloWeights		= model.getMonteCarloWeights(maturity);
        values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
        RandomVariableInterface	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
        RandomVariableInterface	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
        values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

        return values;
	}
}
