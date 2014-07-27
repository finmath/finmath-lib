/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
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
	private final boolean	isFloorlet;

	/**
	 * Create a caplet or a floorlet.
	 * 
	 * A caplet pays \( max(L-K,0) * periodLength \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * A floorlet pays \( -min(L-K,0) * periodLength \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period.
	 * @param strike The strike given as double.
	 * @param isFloorlet If true, this object will represent a floorlet, otherwise a caplet.
	 */
	public Caplet(double maturity, double periodLength, double strike, boolean isFloorlet) {
		super();
		this.maturity = maturity;
		this.periodLength = periodLength;
		this.strike = strike;
		this.isFloorlet = isFloorlet;
	}

	/**
	 * Create a caplet.
	 * 
	 * A caplet pays \( max(L-K,0) * periodLength \) at maturity+periodLength
	 * where L is fixed at maturity.
	 * 
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period.
	 * @param strike The strike given as double.
	 */
	public Caplet(double maturity, double periodLength, double strike) {
		this(maturity, periodLength, strike, false);
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
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        
		// This is on the LIBOR discretization
		double	paymentDate	= maturity+periodLength;

		// Get random variables
		RandomVariableInterface				libor					= model.getLIBOR(maturity, maturity, maturity+periodLength);
		RandomVariableInterface	numeraire				= model.getNumeraire(paymentDate);
		RandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(paymentDate));

		/*
		 * Calculate the payoff, which is
		 *    max(L-K,0) * periodLength         for caplet or
		 *   -min(L-K,0) * periodLength         for floorlet.
		 */
		RandomVariableInterface values = libor;		
		if(!isFloorlet)	values = values.sub(strike).floor(0.0).mult(periodLength);
		else			values = values.sub(strike).cap(0.0).mult(-1.0 * periodLength);

		values = values.div(numeraire).mult(monteCarloProbabilities);

		RandomVariableInterface	numeraireAtValuationTime				= model.getNumeraire(evaluationTime);		
		RandomVariableInterface	monteCarloProbabilitiesAtValuationTime	= model.getMonteCarloWeights(evaluationTime);		
		values = values.mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime);

		return values;
	}
}
