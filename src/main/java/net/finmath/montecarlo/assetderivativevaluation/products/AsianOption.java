/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 23.11.2011
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the valuation of an Asian option.
 * 
 * Given a model for an asset <i>S</i>, the Asian option with strike <i>K</i>, maturity <i>T</i>
 * and averaging points <i>T<sub>i</sub></i> for <i>i = 1,...,n</i> pays
 * <br>
 * 	<i>max(A(T) - K , 0)</i> in <i>T</i>
 * <br>
 * where
 * <br>
 * 	<i>A(T) = 1/n (S(T<sub>1</sub>)+ ... + S(T<sub>n</sub>))</i>
 * <br>
 * 
 * @author Christian Fries
 * @version 1.2
 */
public class AsianOption extends AbstractAssetMonteCarloProduct {

	private final double maturity;
	private final double strike;
	private final TimeDiscretizationInterface timesForAveraging;
	
	/**
	 * Construct a product representing an Asian option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * A(T) = 1/n sum_{i=1,...,n} S(t_i), where t_i are given observation times.
	 * 
	 * @param strike The strike K in the option payoff max(A(T)-K,0).
	 * @param maturity The maturity T in the option payoff maxAS(T)-K,0)
	 * @param timesForAveraging The times t_i used in the calculation of A(T) = 1/n sum_{i=1,...,n} S(t_i).
	 */
	public AsianOption(double maturity, double strike, TimeDiscretizationInterface timesForAveraging) {
		super();
		this.maturity				= maturity;
		this.strike					= strike;
		this.timesForAveraging		= timesForAveraging;
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
    	// Calculate average
    	RandomVariableInterface average = model.getRandomVariableForConstant(0.0);
    	for(double time : timesForAveraging) {
    		RandomVariableInterface underlying	= model.getAssetValue(time,0);
            average = average.add(underlying);
    	}
        average = average.div(timesForAveraging.getNumberOfTimes());
		
		// The payoff: values = max(underlying - strike, 0)
		RandomVariableInterface values = average.sub(strike).floor(0.0);

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
