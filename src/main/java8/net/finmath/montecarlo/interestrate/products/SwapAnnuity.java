/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2026
 */
package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Objects;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;

/**
 * This class implements the valuation of a simple swap annuity, i.e.,
 * the \( \sum_{i=1}^{n} \delta_{i} P(T_{i}) \) where \( \delta_{i} \) are given weights (period length) and
 * \( T_{i} \) are given maturities.
 *
 * @author Christian Fries
 * @version 1.2
 */
public class SwapAnnuity extends AbstractTermStructureMonteCarloProduct {

	private final LocalDateTime referenceDate;

	private final double[] maturities;
	private final double[] periodLengths;

	/**
	 * Create a swap annuity with maturity \( T_{i} \) and period length \( \delta_{i} \).
	 * Here \( T_{i} \) is specified as a double offset to a given reference date \( t = 0 \).
	 *
	 * @param referenceDate The date corresponding to \( t = 0 \).
	 * @param maturities The maturities given as doubles, following the
	 *        {@link FloatingpointDate} convention as offsets to {@code referenceDate}.
	 * @param periodLengths The period lengths (cash-flow weights).
	 */
	public SwapAnnuity(final LocalDateTime referenceDate, final double[] maturities, final double[] periodLengths) {
		super();
		this.referenceDate = referenceDate;
		validateSchedule(maturities, periodLengths);
		this.maturities = maturities.clone();
		this.periodLengths = periodLengths.clone();
	}

	/**
	 * @param maturities The maturities given as double.
	 * @param periodLengths The period lengths (cash-flow weights).
	 */
	public SwapAnnuity(final double[] maturities, final double[] periodLengths) {
		this(null, maturities, periodLengths);
	}

	/**
	 * Creates a swap annuity from a tenor discretization. Each time step is one
	 * weighted payment at the end of the step.
	 *
	 * @param timeDiscretization The payment tenor.
	 */
	public SwapAnnuity(final TimeDiscretization timeDiscretization) {
		this.referenceDate = null;
		Objects.requireNonNull(timeDiscretization, "timeDiscretization must not be null.");
		this.maturities = new double[timeDiscretization.getNumberOfTimeSteps()];
		this.periodLengths = new double[timeDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex = 0; timeIndex<maturities.length; timeIndex++) {
			maturities[timeIndex] = timeDiscretization.getTime(timeIndex+1);
			periodLengths[timeIndex] = timeDiscretization.getTimeStep(timeIndex);
		}
		validateSchedule(maturities, periodLengths);
	}

	private static void validateSchedule(final double[] maturities, final double[] periodLengths) {
		Objects.requireNonNull(maturities, "maturities must not be null.");
		Objects.requireNonNull(periodLengths, "periodLengths must not be null.");
		if(maturities.length != periodLengths.length) {
			throw new IllegalArgumentException("maturities and periodLengths must have the same length.");
		}

		for(int periodIndex = 0; periodIndex < maturities.length; periodIndex++) {
			if(!Double.isFinite(maturities[periodIndex])) {
				throw new IllegalArgumentException("maturities must contain only finite values.");
			}
			if(!Double.isFinite(periodLengths[periodIndex]) || periodLengths[periodIndex] < 0.0) {
				throw new IllegalArgumentException("periodLengths must contain only finite, non-negative values.");
			}
		}
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

		final double productToModelTimeOffset = getProductToModelTimeOffset(model);

		// Aggregation of value
		RandomVariable values = Scalar.of(0.0);
		for(int periodIndex=0; periodIndex < maturities.length; periodIndex++) {
			final double maturity = maturities[periodIndex];
			final double paymentTime = productToModelTimeOffset + maturity;

			if(paymentTime < evaluationTime - Bond.PAYMENT_TIME_TOLERANCE) {
				continue;
			}

			// Get random variables
			final RandomVariable	numeraire				= model.getNumeraire(paymentTime);
			final RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(paymentTime);

			// Calculate numeraire relative value
			RandomVariable valueOfPeriod = model.getRandomVariableForConstant(periodLengths[periodIndex]);
			valueOfPeriod = valueOfPeriod.div(numeraire).mult(monteCarloProbabilities);
			values = values.add(valueOfPeriod);
		}

		// Convert back to values
		final RandomVariable	numeraireAtEvaluationTime				= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtEvaluationTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvaluationTime).div(monteCarloProbabilitiesAtEvaluationTime);

		// Return values
		return values;
	}

	double getProductToModelTimeOffset(final TermStructureMonteCarloSimulationModel model) {
		if(referenceDate == null) {
			return 0.0;
		}

		final LocalDateTime modelReferenceDate;
		try {
			modelReferenceDate = model.getReferenceDate();
		}
		catch(final UnsupportedOperationException exception) {
			throw new IllegalArgumentException(
					"The model must provide a reference date to value a date-based SwapAnnuity.",
					exception);
		}
		if(modelReferenceDate == null) {
			throw new IllegalArgumentException(
					"The model must provide a reference date to value a date-based SwapAnnuity.");
		}

		return FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, referenceDate);
	}

	public LocalDateTime getReferenceDate() {
		return referenceDate;
	}

	/**
	 * @return Returns the maturities.
	 */
	public double[] getMaturities() {
		return maturities.clone();
	}

	/**
	 * @return Returns the period lengths (cash-flow weights).
	 */
	public double[] getPeriodLengths() {
		return periodLengths.clone();
	}

	@Override
	public String toString() {
		return "SwapAnnuity [referenceDate=" + referenceDate + ", maturities=" + Arrays.toString(maturities)
				+ ", periodLengths=" + Arrays.toString(periodLengths) + "]";
	}
}
