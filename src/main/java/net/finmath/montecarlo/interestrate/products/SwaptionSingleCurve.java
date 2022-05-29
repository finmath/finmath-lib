/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implements the valuation of a swaption under a LIBORModelMonteCarloSimulationModel
 *
 * Important: If the LIBOR Market Model is a multi-curve model in the sense that the
 * numeraire is not calculated from the forward curve, then this valuation does
 * not result in the valuation of a collaterlized option on a collateralized swap.
 * It will be a collaterlized option on a single curve (uncolateralized) swap.
 * For a multi-curve valuation see {@link Swaption}.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class SwaptionSingleCurve extends AbstractTermStructureMonteCarloProduct {
	private final double     exerciseDate;	// Exercise date
	private final double[]   fixingDates;		// Vector of fixing dates (must be sorted)
	private final double[]   paymentDates;	// Vector of payment dates (same length as fixing dates)
	private final double[]   periodLengths;	// Vector of payment dates (same length as fixing dates)
	private final double[]   swaprates;		// Vector of strikes

	/**
	 * Create a swaption.
	 *
	 * @param exerciseDate Vector of exercise dates.
	 * @param fixingDates Vector of fixing dates.
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates).
	 * @param periodLengths Vector of period lengths.
	 * @param swaprates Vector of strikes (must have same length as fixing dates).
	 */
	public SwaptionSingleCurve(final double exerciseDate, final double[] fixingDates, final double[] paymentDates, final double[] periodLengths, final double[] swaprates) {
		super();
		this.exerciseDate = exerciseDate;
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.periodLengths = periodLengths;
		this.swaprates = swaprates;
	}

	/**
	 * Create a swaption.
	 *
	 * @param exerciseDate Vector of exercise dates.
	 * @param fixingDates Vector of fixing dates.
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates).
	 * @param swaprates Vector of strikes (must have same length as fixing dates).
	 */
	public SwaptionSingleCurve(
			final double exerciseDate,
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates) {
		super();
		this.exerciseDate = exerciseDate;
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		periodLengths = null;
		this.swaprates = swaprates;
	}

	/**
	 * Creates a swaption using a TimeDiscretizationFromArray
	 *
	 * @param exerciseDate Exercise date.
	 * @param swapTenor Object specifying period start and end dates.
	 * @param swaprate Strike.
	 */
	public SwaptionSingleCurve(
			final double				exerciseDate,
			final TimeDiscretization	swapTenor,
			final double				swaprate) {
		super();
		this.exerciseDate = exerciseDate;

		fixingDates	= new double[swapTenor.getNumberOfTimeSteps()];
		paymentDates	= new double[swapTenor.getNumberOfTimeSteps()];
		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			fixingDates[periodIndex] = swapTenor.getTime(periodIndex);
			paymentDates[periodIndex] = swapTenor.getTime(periodIndex+1);
		}

		periodLengths = null;

		swaprates = new double[swapTenor.getNumberOfTimeSteps()];
		java.util.Arrays.fill(swaprates, swaprate);
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
		/*
		 * Calculate value of the swap at exercise date on each path (beware of perfect foresight - all rates are simulationTime=exerciseDate)
		 */
		RandomVariable valueOfSwapAtExerciseDate	= model.getRandomVariableForConstant(/*fixingDates[fixingDates.length-1],*/0.0);

		// Calculate the value of the swap by working backward through all periods
		for(int period=fixingDates.length-1; period>=0; period--)
		{
			final double fixingDate	= fixingDates[period];
			final double paymentDate	= paymentDates[period];
			final double swaprate		= swaprates[period];

			final double periodLength	= periodLengths != null ? periodLengths[period] : paymentDate - fixingDate;

			// Get random variables - note that this is the rate at simulation time = exerciseDate
			final RandomVariable libor	= model.getForwardRate(exerciseDate, fixingDate, paymentDate);

			// Add payment received at end of period
			final RandomVariable payoff = libor.sub(swaprate).mult(periodLength);
			valueOfSwapAtExerciseDate = valueOfSwapAtExerciseDate.add(payoff);

			// Discount back to beginning of period
			valueOfSwapAtExerciseDate = valueOfSwapAtExerciseDate.discount(libor, paymentDate - fixingDate);
		}

		// If the exercise date is not the first periods start date, then discount back to the exercise date (calculate the forward starting swap)
		if(fixingDates[0] != exerciseDate) {
			final RandomVariable libor	= model.getForwardRate(exerciseDate, exerciseDate, fixingDates[0]);
			final double periodLength	= fixingDates[0] - exerciseDate;

			// Discount back to beginning of period
			valueOfSwapAtExerciseDate = valueOfSwapAtExerciseDate.discount(libor, periodLength);
		}

		/*
		 * Calculate swaption value
		 */
		RandomVariable values = valueOfSwapAtExerciseDate.floor(0.0);

		final RandomVariable	numeraire				= model.getNumeraire(exerciseDate);
		final RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(exerciseDate));
		values = values.div(numeraire).mult(monteCarloProbabilities);

		final RandomVariable	numeraireAtZero					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		return values;
	}

	/**
	 * This method returns the value of the product using a Black-Scholes model for the swap rate
	 * The model is determined by a discount factor curve and a swap rate volatility.
	 *
	 * @param forwardCurve The forward curve on which to value the swap.
	 * @param swaprateVolatility The Black volatility.
	 * @return Value of this product
	 */
	public double getValue(final ForwardCurve forwardCurve, final double swaprateVolatility) {
		final double swaprate = swaprates[0];
		for (final double swaprate1 : swaprates) {
			if (swaprate1 != swaprate) {
				throw new RuntimeException("Uneven swaprates not allows for analytical pricing.");
			}
		}

		final double[] swapTenor = new double[fixingDates.length+1];
		System.arraycopy(fixingDates, 0, swapTenor, 0, fixingDates.length);
		swapTenor[swapTenor.length-1] = paymentDates[paymentDates.length-1];

		final double forwardSwapRate = net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), forwardCurve);
		final double swapAnnuity = SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor), forwardCurve);

		return AnalyticFormulas.blackModelSwaptionValue(forwardSwapRate, swaprateVolatility, exerciseDate, swaprate, swapAnnuity);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString()
				+ "\n" + "exerciseDate: " + exerciseDate
				+ "\n" + "fixingDates: " + Arrays.toString(fixingDates)
				+ "\n" + "paymentDates: " + Arrays.toString(paymentDates)
				+ "\n" + "periodLengths: " + Arrays.toString(periodLengths)
				+ "\n" + "swaprates: " + Arrays.toString(swaprates);
	}
}
