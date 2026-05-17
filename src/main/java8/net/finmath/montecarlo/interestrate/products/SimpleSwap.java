/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.Arrays;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the valuation of a swap under a LIBORModelMonteCarloSimulationModel
 *
 * @author Christian Fries
 * @version 1.2
 */
public class SimpleSwap extends AbstractTermStructureMonteCarloProduct {
	private final double[] fixingDates;		// Vector of fixing dates
	private final double[] paymentDates;	// Vector of payment dates (same length as fixing dates)
	private final double[] swaprates;		// Vector of strikes

	private final boolean isPayFix;
	private final double[] notional;		// Vector of notionals

	/**
	 * Create a swap.
	 *
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param isPayFix If true, the swap is receive float - pay fix, otherwise its receive fix - pay float.
	 * @param notional The notional as a vector for all periods
	 */
	public SimpleSwap(
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates,
			final boolean isPayFix,
			final double[] notional) {
		super();
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.swaprates = swaprates;
		this.isPayFix = isPayFix;
		this.notional = notional;
	}

	/**
	 * Create a swap.
	 *
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param isPayFix If true, the swap is receive float - pay fix, otherwise its receive fix - pay float.
	 * @param notional The constant notional
	 */
	public SimpleSwap(
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates,
			final boolean isPayFix,
			final double notional) {
		super();
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.swaprates = swaprates;
		this.isPayFix = isPayFix;
		this.notional = new double[swaprates.length];
		Arrays.fill(this.notional, notional);
	}


	/**
	 * Create a swap.
	 *
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param notional The constant notional
	 */
	public SimpleSwap(
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates,
			final double notional) {
		this(fixingDates, paymentDates, swaprates, true, notional);
	}

	/**
	 * Create a swap.
	 *
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param notional The notional as a vector for all periods
	 */
	public SimpleSwap(
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates,
			final double[] notional) {
		this(fixingDates, paymentDates, swaprates, true, notional);
	}

	/**
	 * Create a swap.
	 *
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @deprecated
	 */
	@Deprecated
	public SimpleSwap(
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates) {
		this(fixingDates, paymentDates, swaprates, true, 1.0);
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
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		RandomVariable values						= model.getRandomVariableForConstant(0.0);

		for(int period=0; period<fixingDates.length; period++)
		{
			final double fixingDate		= fixingDates[period];
			final double paymentDate		= paymentDates[period];
			final double swaprate 		= swaprates[period];
			final double periodLength		= paymentDate - fixingDate;

			if(paymentDate < evaluationTime) {
				continue;
			}

			// Get random variables
			final RandomVariable forwardRate	= model.getForwardRate(fixingDate, fixingDate, paymentDate);
			RandomVariable payoff	= forwardRate.sub(swaprate).mult(periodLength).mult(notional[period]);
			if(!isPayFix) {
				payoff = payoff.mult(-1.0);
			}

			final RandomVariable numeraire				= model.getNumeraire(paymentDate);
			final RandomVariable monteCarloProbabilities	= model.getMonteCarloWeights(paymentDate);
			payoff = payoff.div(numeraire).mult(monteCarloProbabilities);

			values = values.add(payoff);
		}

		final RandomVariable	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return values;
	}

	@Override
	public String toString() {
		return super.toString()
				+ "\n" + "fixingDates: " + Arrays.toString(fixingDates)
				+ "\n" + "paymentDates: " + Arrays.toString(paymentDates)
				+ "\n" + "swaprates: " + Arrays.toString(swaprates);
	}

	public double getStartTime(){
		return fixingDates[0];
	}

	public double[] getFixingDates(){
		return fixingDates;
	}

	public double[] getNotional(){
		return notional;
	}

	public double[] getSwapRates(){
		return swaprates;
	}

	public double[] getPaymentDates(){
		return paymentDates;
	}

	public double[] getPeriodLengths(){
		double[] periodLengths = new double[paymentDates.length];
		periodLengths = IntStream.range(0, periodLengths.length).mapToDouble(new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return paymentDates[i]-fixingDates[i];
			}
		}).toArray();
		return periodLengths;
	}


}
