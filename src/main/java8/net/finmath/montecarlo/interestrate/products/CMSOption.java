/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implements the valuation of an option on a CMS rate.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class CMSOption extends AbstractTermStructureMonteCarloProduct {
	private final double		exerciseDate;	// Exercise date
	private final double[]	fixingDates;    // Vector of fixing dates (must be sorted)
	private final double[]	paymentDates;	// Vector of payment dates (same length as fixing dates)
	private final double[]	periodLengths;	// Vector of payment dates (same length as fixing dates)
	private final double		strike;			// Vector of strikes

	/**
	 * Create the option on a CMS rate.
	 *
	 * @param exerciseDate The exercise date of the option.
	 * @param fixingDates Vector of fixing dates.
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param periodLengths Vector of period length (must have same length as fixing dates)
	 * @param strike Strike swap rate.
	 */
	public CMSOption(
			final double		exerciseDate,
			final double[]	fixingDates,
			final double[]	paymentDates,
			final double[]	periodLengths,
			final double		strike) {
		super();
		this.exerciseDate = exerciseDate;
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.periodLengths = periodLengths;
		this.strike = strike;
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
		 * Calculate value of the swap at exercise date on each path (beware of perfect forsight - all rates are simulationTime=exerciseDate)
		 */
		RandomVariable valueFixLeg		= new RandomVariableFromDoubleArray(fixingDates[fixingDates.length-1], 0.0);
		RandomVariable valueFloatLeg	= new RandomVariableFromDoubleArray(paymentDates[paymentDates.length-1], -1.0);

		// Calculate the value of the swap by working backward through all periods
		for(int period=fixingDates.length-1; period>=0; period--)
		{
			final double fixingDate	= fixingDates[period];
			final double paymentDate	= paymentDates[period];

			final double periodLength	= periodLengths != null ? periodLengths[period] : paymentDate - fixingDate;

			// Get random variables - note that this is the rate at simulation time = exerciseDate
			final RandomVariable libor	= model.getForwardRate(exerciseDate, fixingDate, paymentDate);

			// Add payment received at end of period
			final RandomVariable payoff = new RandomVariableFromDoubleArray(paymentDate, 1.0 * periodLength);
			valueFixLeg = valueFixLeg.add(payoff);

			// Discount back to beginning of period
			valueFloatLeg = valueFloatLeg.discount(libor, periodLength);
			valueFixLeg = valueFixLeg.discount(libor, periodLength);
		}
		valueFloatLeg = valueFloatLeg.add(1.0);

		final RandomVariable parSwapRate = valueFloatLeg.div(valueFixLeg);

		RandomVariable payoffUnit	= new RandomVariableFromDoubleArray(paymentDates[0], periodLengths[0]);
		payoffUnit = payoffUnit.discount(model.getForwardRate(exerciseDate, fixingDates[0], paymentDates[0]),paymentDates[0]-fixingDates[0]);

		RandomVariable value = parSwapRate.sub(strike).floor(0.0).mult(payoffUnit);

		// If the exercise date is not the first periods start date, then discount back to the exercise date (calculate the forward starting swap)
		if(fixingDates[0] != exerciseDate) {
			final RandomVariable libor	= model.getForwardRate(exerciseDate, exerciseDate, fixingDates[0]);
			final double periodLength	= fixingDates[0] - exerciseDate;

			// Discount back to beginning of period
			value = value.discount(libor, periodLength);
		}

		/*
		 * Calculate value
		 */
		final RandomVariable	numeraire				= model.getNumeraire(exerciseDate);
		final RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(model.getTimeIndex(exerciseDate));
		value = value.div(numeraire).mult(monteCarloProbabilities);

		final RandomVariable	numeraireAtZero					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
		value = value.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		return value;
	}

	/**
	 * This method returns the value of the product using a Black-Scholes model for the swap rate with the Hunt-Kennedy convexity adjustment.
	 * The model is determined by a discount factor curve and a swap rate volatility.
	 *
	 * @param forwardCurve The forward curve from which the swap rate is calculated. The discount curve, associated with this forward curve is used for discounting this option.
	 * @param swaprateVolatility The volatility of the log-swaprate.
	 * @return Value of this product
	 */
	public double getValue(final ForwardCurve forwardCurve, final double swaprateVolatility) {
		final double[] swapTenor = new double[fixingDates.length+1];
		System.arraycopy(fixingDates, 0, swapTenor, 0, fixingDates.length);
		swapTenor[swapTenor.length-1] = paymentDates[paymentDates.length-1];

		final TimeDiscretization fixTenor	= new TimeDiscretizationFromArray(swapTenor);
		final TimeDiscretization floatTenor	= new TimeDiscretizationFromArray(swapTenor);
		final double forwardSwapRate = Swap.getForwardSwapRate(fixTenor, floatTenor, forwardCurve);
		final double swapAnnuity = SwapAnnuity.getSwapAnnuity(fixTenor, forwardCurve);
		final double payoffUnit = SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor[0], swapTenor[1]), forwardCurve) / (swapTenor[1] - swapTenor[0]);
		return AnalyticFormulas.huntKennedyCMSOptionValue(forwardSwapRate, swaprateVolatility, swapAnnuity, exerciseDate, swapTenor[swapTenor.length-1]-swapTenor[0], payoffUnit, strike) * (swapTenor[1] - swapTenor[0]);
	}
}
