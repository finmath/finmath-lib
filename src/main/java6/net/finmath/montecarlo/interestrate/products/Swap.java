/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the valuation of a swap under a LIBORModelMonteCarloSimulationInterface
 * 
 * @author Christian Fries
 * @version 1.2
 */
public class Swap extends AbstractLIBORMonteCarloProduct {
	private double[] fixingDates;	// Vector of fixing dates
	private double[] paymentDates;	// Vector of payment dates (same length as fixing dates)
	private double[] swaprates;		// Vector of strikes
	
	/**
	 * Create a swap.
	 * 
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public Swap(
		double[] fixingDates,
		double[] paymentDates,
		double[] swaprates) {
		super();
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.swaprates = swaprates;
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
        RandomVariableInterface values						= new RandomVariable(0.0,0.0);

		for(int period=0; period<fixingDates.length; period++)
		{
			double fixingDate		= fixingDates[period];
			double paymentDate		= paymentDates[period];
			double swaprate 		= swaprates[period];
			double periodLength		= paymentDate - fixingDate;
			
			if(paymentDate < evaluationTime) continue;

			// Get random variables
			RandomVariableInterface libor	= model.getLIBOR(fixingDate, fixingDate, paymentDate);
			RandomVariableInterface payoff	= libor.sub(swaprate).mult(periodLength);

			RandomVariableInterface numeraire					= model.getNumeraire(paymentDate);
			RandomVariableInterface monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(paymentDate));
			payoff = payoff.div(numeraire).mult(monteCarloProbabilities);

			values = values.add(payoff);
		}

		RandomVariableInterface	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

        return values;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return super.toString()
				+ "\n" + "fixingDates: " + Arrays.toString(fixingDates)
				+ "\n" + "paymentDates: " + Arrays.toString(paymentDates)
				+ "\n" + "swaprates: " + Arrays.toString(swaprates);
	}
}
