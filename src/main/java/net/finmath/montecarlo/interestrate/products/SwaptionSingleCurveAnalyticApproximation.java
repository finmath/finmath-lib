/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.05.2007
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.HashMap;
import java.util.Map;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * This class implements an analytic swaption valuation formula under
 * a LIBOR market model. The algorithm implemented here is
 * taken from ISBN 0470047224.
 *
 * The approximation assumes that the forward rates (LIBOR) follow a
 * log normal model and that the model provides the integrated
 * instantaneous covariance of the log-forward rates.
 *
 * The getValue method calculates the approximated integrated instantaneous variance of the swap rate,
 * using the approximation d log(S(t))/d log(L(t)) = d log(S(0))/d log(L(0)).
 *
 * The valuation can be performed in terms of value or implied
 * volatility.
 *
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SwaptionSingleCurveAnalyticApproximation extends AbstractTermStructureMonteCarloProduct implements net.finmath.modelling.products.Swaption {

	private final double      swaprate;
	private final double[]    swapTenor;       // Vector of swap tenor (period start and end dates). Start of first period is the option maturity.
	private final ValueUnit   valueUnit;

	/**
	 * Create an analytic swaption approximation product for
	 * log normal forward rate model.
	 *
	 * Note: It is implicitly assumed that swapTenor.getTime(0) is the exercise date (no forward starting).
	 *
	 * @param swaprate The strike swap rate of the swaption.
	 * @param swapTenor The swap tenor in doubles.
	 */
	public SwaptionSingleCurveAnalyticApproximation(final double swaprate, final TimeDiscretization swapTenor) {
		this(swaprate, swapTenor.getAsDoubleArray(), ValueUnit.VALUE);
	}

	/**
	 * Create an analytic swaption approximation product for
	 * log normal forward rate model.
	 *
	 * Note: It is implicitly assumed that swapTenor[0] is the exercise date (no forward starting).
	 *
	 * @param swaprate The strike swap rate of the swaption.
	 * @param swapTenor The swap tenor in doubles.
	 * @param valueUnit The unit of the quantity returned by the getValues method.
	 */
	public SwaptionSingleCurveAnalyticApproximation(final double swaprate, final double[] swapTenor, final ValueUnit valueUnit) {
		super();
		this.swaprate	= swaprate;
		this.swapTenor	= swapTenor;
		this.valueUnit	= valueUnit;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) {
		final ProcessModel modelBase = model.getModel();
		if(modelBase instanceof LIBORMarketModel) {
			return getValues(evaluationTime, model.getTimeDiscretization(), (LIBORMarketModel)modelBase);
		} else {
			throw new IllegalArgumentException("This product requires a simulation where the underlying model is of type LIBORMarketModel.");
		}
	}

	/**
	 * Calculates the approximated integrated instantaneous variance of the swap rate,
	 * using the approximation d log(S(t))/d log(L(t)) = d log(S(0))/d log(L(0)).
	 *
	 * @param evaluationTime Time at which the product is evaluated.
	 * @param timeDiscretization The time discretization used for integrating the covariance.
	 * @param model A model implementing the LIBORModelMonteCarloSimulationModel
	 * @return Depending on the value of value unit, the method returns either
	 * the approximated integrated instantaneous variance of the swap rate (ValueUnit.INTEGRATEDVARIANCE)
	 * or the value using the Black formula (ValueUnit.VALUE).
	 * @TODO make initial values an arg and use evaluation time.
	 */
	public RandomVariable getValues(final double evaluationTime, final TimeDiscretization timeDiscretization, final LIBORMarketModel model) {
		if(evaluationTime > 0) {
			throw new RuntimeException("Forward start evaluation currently not supported.");
		}

		final double swapStart    = swapTenor[0];
		final double swapEnd      = swapTenor[swapTenor.length-1];

		final int swapStartIndex  = model.getLiborPeriodIndex(swapStart);
		final int swapEndIndex    = model.getLiborPeriodIndex(swapEnd);
		final int optionMaturityIndex = model.getCovarianceModel().getTimeDiscretization().getTimeIndex(swapStart)-1;

		final Map<String, double[]>  logSwaprateDerivative  = getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getForwardRateCurve(), swapTenor);
		final double[]    swapCovarianceWeights  = logSwaprateDerivative.get("values");
		final double[]    discountFactors        = logSwaprateDerivative.get("discountFactors");
		final double[]    swapAnnuities          = logSwaprateDerivative.get("swapAnnuities");

		// Get the integrated libor covariance from the model
		final double[][]	integratedLIBORCovariance = model.getIntegratedLIBORCovariance(timeDiscretization)[optionMaturityIndex];

		// Calculate integrated swap rate covariance
		double integratedSwapRateVariance = 0.0;
		for(int componentIndex1 = swapStartIndex; componentIndex1 < swapEndIndex; componentIndex1++) {
			// Sum the libor cross terms (use symmetry)
			for(int componentIndex2 = componentIndex1+1; componentIndex2 < swapEndIndex; componentIndex2++) {
				integratedSwapRateVariance += 2.0 * swapCovarianceWeights[componentIndex1-swapStartIndex] * swapCovarianceWeights[componentIndex2-swapStartIndex] * integratedLIBORCovariance[componentIndex1][componentIndex2];
			}
			// Add diagonal term (libor variance term)
			integratedSwapRateVariance += swapCovarianceWeights[componentIndex1-swapStartIndex] * swapCovarianceWeights[componentIndex1-swapStartIndex] * integratedLIBORCovariance[componentIndex1][componentIndex1];
		}

		// Return integratedSwapRateVariance if requested
		if(valueUnit == ValueUnit.INTEGRATEDVARIANCELOGNORMAL || valueUnit == ValueUnit.INTEGRATEDVARIANCE) {
			return new Scalar(integratedSwapRateVariance);
		}

		final double volatility		= Math.sqrt(integratedSwapRateVariance / swapStart);

		// Return integratedSwapRateVariance if requested
		if(valueUnit == ValueUnit.VOLATILITYLOGNORMAL || valueUnit == ValueUnit.VOLATILITY) {
			return new Scalar(volatility);
		}

		// Use black formula for swaption to calculate the price
		final double swapAnnuity      =   swapAnnuities[0];
		final double parSwaprate      =   (discountFactors[0] - discountFactors[swapEndIndex-swapStartIndex]) / swapAnnuity;

		final double optionMaturity	= swapStart;

		final double valueSwaption = AnalyticFormulas.blackModelSwaptionValue(parSwaprate, volatility, optionMaturity, swaprate, swapAnnuity);
		return new Scalar(valueSwaption);
	}

	/**
	 * This function calculate the partial derivative <i>d log(S) / d log(L<sub>k</sub>)</i> for
	 * a given swap rate with respect to a vector of forward rates (on a given forward rate tenor).
	 *
	 * It also returns some useful other quantities like the corresponding discount factors and swap annuities.
	 *
	 * @param liborPeriodDiscretization The libor period discretization.
	 * @param forwardCurve The forward curve.
	 * @param swapTenor The swap tenor.
	 * @return A map containing the partial derivatives (key "value"), the discount factors (key "discountFactors") and the annuities (key "annuities") as vectors of double[] (indexed by forward rate tenor index starting at swap start)
	 */
	public static Map<String, double[]> getLogSwaprateDerivative(final TimeDiscretization liborPeriodDiscretization, final ForwardCurve forwardCurve, final double[] swapTenor) {
		final double swapStart    = swapTenor[0];
		final double swapEnd      = swapTenor[swapTenor.length-1];

		// Get the indices of the swap start and end on the forward rate tenor
		final int swapStartIndex  = liborPeriodDiscretization.getTimeIndex(swapStart);
		final int swapEndIndex    = liborPeriodDiscretization.getTimeIndex(swapEnd);

		// Precalculate forward rates and discount factors. Note: the swap contains swapEndIndex-swapStartIndex forward rates
		final double[] forwardRates       = new double[swapEndIndex-swapStartIndex+1];
		final double[] discountFactors    = new double[swapEndIndex-swapStartIndex+1];

		// Calculate discount factor at swap start
		discountFactors[0] = 1.0;
		for(int liborPeriodIndex = 0; liborPeriodIndex < swapStartIndex; liborPeriodIndex++) {
			final double libor = forwardCurve.getForward(null, liborPeriodDiscretization.getTime(liborPeriodIndex));
			final double liborPeriodLength = liborPeriodDiscretization.getTimeStep(liborPeriodIndex);
			discountFactors[0] /= 1 + libor * liborPeriodLength;
		}

		// Calculate discount factors for swap period ends (used for swap annuity)
		for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
			final double libor = forwardCurve.getForward(null, liborPeriodDiscretization.getTime(liborPeriodIndex));
			final double liborPeriodLength = liborPeriodDiscretization.getTimeStep(liborPeriodIndex);

			forwardRates[liborPeriodIndex-swapStartIndex]       = libor;
			discountFactors[liborPeriodIndex-swapStartIndex+1]  = discountFactors[liborPeriodIndex-swapStartIndex] / (1 + libor * liborPeriodLength);
		}

		// Precalculate swap annuities
		final double[]    swapAnnuities   = new double[swapTenor.length-1];
		double      swapAnnuity     = 0.0;
		for(int swapPeriodIndex = swapTenor.length-2; swapPeriodIndex >= 0; swapPeriodIndex--) {
			final int periodEndIndex = liborPeriodDiscretization.getTimeIndex(swapTenor[swapPeriodIndex+1]);
			swapAnnuity += discountFactors[periodEndIndex-swapStartIndex] * (swapTenor[swapPeriodIndex+1]-swapTenor[swapPeriodIndex]);
			swapAnnuities[swapPeriodIndex] = swapAnnuity;
		}


		// Precalculate weights: The formula is take from ISBN 0470047224
		final double longForwardRate = discountFactors[swapEndIndex-swapStartIndex] / ( discountFactors[0] - discountFactors[swapEndIndex-swapStartIndex]);

		final double[] swapCovarianceWeights = new double[swapEndIndex-swapStartIndex];

		int swapPeriodIndex = 0;
		for(int liborPeriodIndex = swapStartIndex; liborPeriodIndex < swapEndIndex; liborPeriodIndex++) {
			if(liborPeriodDiscretization.getTime(liborPeriodIndex) >= swapTenor[swapPeriodIndex+1]) {
				swapPeriodIndex++;
			}

			swapCovarianceWeights[liborPeriodIndex-swapStartIndex] = (longForwardRate + swapAnnuities[swapPeriodIndex] / swapAnnuity) * (1.0 - discountFactors[liborPeriodIndex-swapStartIndex+1] / discountFactors[liborPeriodIndex-swapStartIndex]);
		}

		// Return results
		final Map<String, double[]> results = new HashMap<>();
		results.put("values",			swapCovarianceWeights);
		results.put("discountFactors",	discountFactors);
		results.put("swapAnnuities",	swapAnnuities);

		return results;
	}
}
