/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2026
 */
package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDateTime;
import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

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
	
	private double[] maturities;
	private double[] periodLengths;

	/**
	 * Create a swap annuity with maturity \( T_{i} \) and period length \( \delta_{i} \).
	 * Here \( T_{i} \) is specified as a double offset to a given reference date \( t = 0 \).
	 *
	 * @param referenceDate The date corresponding to \( t = 0 \).
	 * @param maturities The maturities given as double (following the {@link FloatingpointDate} convention as an offset to referenceDate.
	 */
	public SwapAnnuity(final LocalDateTime referenceDate, final double[] maturities, final double[] periodLengths) {
		super();
		this.referenceDate = referenceDate;
		this.maturities = maturities;
		this.periodLengths = periodLengths;
	}

	/**
	 * @param maturities The maturities given as double.
	 */
	public SwapAnnuity(final double[] maturities, final double[] periodLengths) {
		super();
		this.referenceDate = null;
		this.maturities = maturities;
		this.periodLengths = periodLengths;
	}

	public SwapAnnuity(TimeDiscretization timeDiscretization) {
		this.referenceDate = null;
		this.maturities = new double[timeDiscretization.getNumberOfTimeSteps()];
		this.periodLengths = new double[timeDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex = 0; timeIndex<maturities.length; timeIndex++) {
			maturities[timeIndex] = timeDiscretization.getTime(timeIndex+1);
			periodLengths[timeIndex] = timeDiscretization.getTimeStep(timeIndex);
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

		double productToModelTimeOffset = 0;
		try {
			if(referenceDate != null) {
				productToModelTimeOffset = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate(), referenceDate);
			}
		}
		catch(final UnsupportedOperationException e) {}

		// Aggregation of value
		RandomVariable values = Scalar.of(0.0);
		for(int periodIndex=0; periodIndex < maturities.length; periodIndex++) {
			double maturity = maturities[periodIndex];
			
			if(evaluationTime > maturity) {
				continue;
			}

			// Get random variables
			final RandomVariable	numeraire				= model.getNumeraire(productToModelTimeOffset + maturity);
			final RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(productToModelTimeOffset + maturity);
	
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

	public LocalDateTime getReferenceDate() {
		return referenceDate;
	}

	/**
	 * @return Returns the maturities.
	 */
	public double[] getMaturities() {
		return maturities;
	}

	public double[] getPeriodLengths() {
		return periodLengths;
	}

	@Override
	public String toString() {
		return "SwapAnnuity [referenceDate=" + referenceDate + ", maturities=" + Arrays.toString(maturities)
				+ ", periodLengths=" + Arrays.toString(periodLengths) + "]";
	}
}
