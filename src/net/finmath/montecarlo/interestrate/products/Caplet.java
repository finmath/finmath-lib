/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the pricing of a Caplet using a given <code>AbstractLIBORMarketModel</code>.
 * 
 * @author Christian Fries
 * @version 1.0
 */
public class Caplet extends AbstractLIBORMonteCarloProduct {
	private final double	maturity;
	private final double	periodLength;
	private final double	strike;
	private boolean	isFloorlet = false;

	/**
	 * @param maturity The fixing date given as double. The payment is at the period end.
     * @param periodLength The length of the libor period.
	 * @param strike The strike given as double.
	 */
	public Caplet(double maturity, double periodLength, double strike) {
		super();
        this.maturity = maturity;
        this.periodLength = periodLength;
		this.strike = strike;
	}

	/**
	 * Makes the caplet a floorlet.
	 * 
	 * @param isFloorlet
	 */
	public Caplet setFloorlet(boolean isFloorlet) {
		this.isFloorlet = isFloorlet;

		return this;
	}
	
    /**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws net.finmath.exception.CalculationException
     */
    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        
		// This is on the LIBOR discretization
		double	paymentDate	= maturity+periodLength;
				
		// Get random variables
		RandomVariableInterface				libor					= model.getLIBOR(maturity, maturity, maturity+periodLength);
		ImmutableRandomVariableInterface	numeraire				= model.getNumeraire(paymentDate);
		ImmutableRandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(paymentDate));
	
		/*
		 * Calculate the payoff, which is
		 *   max(L-K,0) * periodLength         for caplet or
		 *   min(L-K,0) * periodLength         for floorlet.
		 */
		RandomVariableInterface values = libor;		
		if(!isFloorlet)	values = values.sub(strike).floor(0.0).mult(periodLength);
		else			values = values.sub(strike).cap(0.0).mult(-1.0 * periodLength);

        values = values.div(numeraire).mult(monteCarloProbabilities);

		ImmutableRandomVariableInterface	numeraireAtValuationTime				= model.getNumeraire(evaluationTime);		
		ImmutableRandomVariableInterface	monteCarloProbabilitiesAtValuationTime	= model.getMonteCarloWeights(evaluationTime);		
		values = values.mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime);

		return values;
	}
}
