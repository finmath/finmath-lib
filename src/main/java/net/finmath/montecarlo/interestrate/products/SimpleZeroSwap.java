/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the valuation of a zero swap under a LIBORModelMonteCarloSimulationInterface.
 * The notional of the swap accrues with the floatIndex.
 * 
 * The swap is "simple" in the sense that it does not consider complex schedules and
 * daycount fractions. See {@link Swap} for a more general implementation.
 * 
 * @author Christian Fries
 * @version 1.2
 */
public class SimpleZeroSwap extends AbstractLIBORMonteCarloProduct {
	private final double[] fixingDates;	// Vector of fixing dates
	private final double[] paymentDates;	// Vector of payment dates (same length as fixing dates)
	private final double[] swaprates;		// Vector of strikes
	private final AbstractIndex floatIndex;	// The float index. If null, LIBOR will be used.
	private final boolean isPayFix;
	
	/**
	 * Create a swap.
	 * 
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param floatIndex The float index. If null, LIBOR will be used.
	 * @param isPayFix If true, the swap is receive float - pay fix, otherwise its receive fix - pay float.
	 */
	public SimpleZeroSwap(
			double[] fixingDates,
			double[] paymentDates,
			double[] swaprates,
			AbstractIndex floatIndex,
			boolean isPayFix) {
		super();
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.swaprates = swaprates;
		this.floatIndex = floatIndex;
		this.isPayFix = isPayFix;
	}

	/**
	 * Create a swap.
	 * 
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param isPayFix If true, the swap is receive float - pay fix, otherwise its receive fix - pay float.
	 */
	public SimpleZeroSwap(
			double[] fixingDates,
			double[] paymentDates,
			double[] swaprates,
			boolean isPayFix) {
		this(fixingDates, paymentDates, swaprates, null, isPayFix);
	}

	/**
	 * Create a swap.
	 * 
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public SimpleZeroSwap(
			double[] fixingDates,
			double[] paymentDates,
			double[] swaprates) {
		this(fixingDates, paymentDates, swaprates, true);
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
		RandomVariableInterface values						= model.getRandomVariableForConstant(0.0);

		RandomVariableInterface notional					= model.getRandomVariableForConstant(1.0);
		for(int period=0; period<fixingDates.length; period++)
		{
			double fixingDate		= fixingDates[period];
			double paymentDate		= paymentDates[period];
			double swaprate 		= swaprates[period];
			double periodLength		= paymentDate - fixingDate;

			if(paymentDate < evaluationTime) continue;

			// Get random variables
			RandomVariableInterface index	= floatIndex != null ? floatIndex.getValue(fixingDate, model) : model.getLIBOR(fixingDate, fixingDate, paymentDate);
			RandomVariableInterface payoff	= index.sub(swaprate).mult(periodLength).mult(notional);
			if(!isPayFix) payoff = payoff.mult(-1.0);

			RandomVariableInterface numeraire				= model.getNumeraire(paymentDate);
			RandomVariableInterface monteCarloProbabilities	= model.getMonteCarloWeights(paymentDate);
			payoff = payoff.div(numeraire).mult(monteCarloProbabilities);

			values = values.add(payoff);

			notional = notional.mult(swaprate*periodLength);
		}

		RandomVariableInterface	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return values;
	}
}
