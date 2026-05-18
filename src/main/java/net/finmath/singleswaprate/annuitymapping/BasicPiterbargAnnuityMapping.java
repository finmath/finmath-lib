package net.finmath.singleswaprate.annuitymapping;

import java.util.Arrays;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.VolVolCube;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
import net.finmath.singleswaprate.products.AnnuityDummyProduct;
import net.finmath.singleswaprate.products.NormalizingDummyProduct;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;

/**
 * Implements an annuity mapping following Vladimir Piterbarg's approach. This class does not take into account multi curve convexity adjustment.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class BasicPiterbargAnnuityMapping implements AnnuityMapping {

	private static QuotingConvention quotingConvention = VolatilitySurface.QuotingConvention.VOLATILITYNORMAL;

	private final int numberOfPeriods;
	private final double[] periodLengths;

	private final double[] initialAnnuities;
	private final double[] initialSwapRates;

	private final double[] exponentialDriverMeans;
	private final double[] exponents;
	private final double[] denominators;

	private final double expectationCorrection;
	private final NormalizingFunction normalizer;

	/**
	 * Create the annuity mapping. When used without strike the volatilities are taken out of the cube at par swap rate.
	 *
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param model The model containing curve and cube.
	 * @param discountCurveName The name of the discount curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 */
	public BasicPiterbargAnnuityMapping(final Schedule fixSchedule, final Schedule floatSchedule, final VolatilityCubeModel model, final String discountCurveName, final String volatilityCubeName) {
		this(fixSchedule, floatSchedule, Double.NaN, model, discountCurveName, volatilityCubeName);
	}

	/**
	 * Create the annuity mapping.
	 *
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param strike The strike of the product this annuity mapping is being created for.
	 * @param model The model containing curve and cube.
	 * @param discountCurveName The name of the discount curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 */
	public BasicPiterbargAnnuityMapping(final Schedule fixSchedule, final Schedule floatSchedule, final double strike, final VolatilityCubeModel model,
			final String discountCurveName, final String volatilityCubeName) {
		this(fixSchedule, floatSchedule, strike, model, discountCurveName, volatilityCubeName, 0, 0, -1);
	}

	/**
	 * Create the annuity mapping.
	 *
	 * @param fixSchedule Fix leg schedule of the swap.
	 * @param floatSchedule Float leg schedule of the swap.
	 * @param strike The strike of the product this annuity mapping is being created for.
	 * @param model The model containing curve and cube.
	 * @param discountCurveName The name of the discount curve.
	 * @param volatilityCubeName The name of the volatility cube.
	 * @param lowerBound The lowest strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param upperBound The maximum strike the Piterbarg annuity mapping may use during replication, when normalizing.
	 * @param numberOfEvaluationPoints The number of points the replication may evaluate Piterbarg annuity mapping is normalizing.
	 */
	public BasicPiterbargAnnuityMapping(final Schedule fixSchedule, final Schedule floatSchedule, double strike, final VolatilityCubeModel model,
			final String discountCurveName, final String volatilityCubeName, final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		super();
		this.numberOfPeriods = fixSchedule.getNumberOfPeriods();

		final double maturity 	= fixSchedule.getPeriodStart(0);
		final double[] periodLengths	= new double[numberOfPeriods];
		final double[] periodEnds		= new double[numberOfPeriods];
		for(int index = 0; index < numberOfPeriods; index++) {
			periodLengths[index] 	= fixSchedule.getPeriodLength(index);
			periodEnds[index]		= fixSchedule.getPeriodEnd(index);
		}
		this.periodLengths 	= periodLengths;

		this.initialAnnuities = getAnnuities(fixSchedule, discountCurveName, model);
		this.initialSwapRates = getForwardSwapRates(fixSchedule, discountCurveName, model);

		final String volvolCubeName = "VolVolFrom" + volatilityCubeName;
		final VolatilityCube volvolCube = new VolVolCube(volvolCubeName, model.getVolatilityCube(volatilityCubeName).getReferenceDate(),
				volatilityCubeName, fixSchedule, initialSwapRates);

		if(Double.isNaN(strike)) {
			strike = initialSwapRates[numberOfPeriods-1];
		}

		//calculation of the various exp mu
		this.exponentialDriverMeans = findExponentialDriverMeans(periodEnds, maturity, strike, volvolCube, model);

		//calculation of arrays of the denominators and exponents of the individual summands in the function of the annuity mapping
		final double[] exponents    = new double[numberOfPeriods];
		final double[] denominators = new double[numberOfPeriods];
		Arrays.fill(denominators, 1.0);

		double currentSummand;
		double currentFactor;
		final double terminalVolvol = volvolCube.getValue(model, periodEnds[numberOfPeriods-1], maturity, strike, quotingConvention);

		for(int outerIndex = numberOfPeriods-1; outerIndex > -1; outerIndex--){

			currentSummand = volvolCube.getValue(model, periodEnds[outerIndex], maturity, strike, quotingConvention);
			currentFactor = (periodLengths[outerIndex] *initialSwapRates[outerIndex] +1.0) *exponentialDriverMeans[outerIndex];

			for(int innerIndex = 0; innerIndex <= outerIndex; innerIndex++){
				exponents[innerIndex]	 += currentSummand;
				denominators[innerIndex] *= currentFactor;
			}
			exponents[outerIndex] /= terminalVolvol;
		}
		this.exponents		= exponents;
		this.denominators 	= denominators;

		//calibrate the normalizing function to the measure of the annuity
		final NormalizingDummyProduct unscaledNormalizerDummy = new NormalizingDummyProduct(fixSchedule, floatSchedule, discountCurveName, null, volatilityCubeName,
				new ExponentialNormalizer(initialSwapRates[initialSwapRates.length-1], 1));
		if(numberOfEvaluationPoints > 0) {
			unscaledNormalizerDummy.setIntegrationParameters(lowerBound, upperBound, numberOfEvaluationPoints);
		}
		this.normalizer = new ExponentialNormalizer(initialSwapRates[initialSwapRates.length-1],
				1 / unscaledNormalizerDummy.getValue(fixSchedule.getFixing(0), model));

		//create a base mapping without correction of the expectation and calibrate the expectation correction
		final BasicPiterbargAnnuityMapping uncorrectedMapping = new BasicPiterbargAnnuityMapping(numberOfPeriods, periodLengths, initialAnnuities,
				initialSwapRates, exponentialDriverMeans, exponents, denominators, normalizer);
		final AnnuityDummyProduct uncorrectedAnnuityDummy = new AnnuityDummyProduct(fixSchedule, floatSchedule, discountCurveName, null, volatilityCubeName,
				uncorrectedMapping);
		if(numberOfEvaluationPoints > 0) {
			uncorrectedAnnuityDummy.setIntegrationParameters(lowerBound, upperBound, numberOfEvaluationPoints);
		}

		this.expectationCorrection = uncorrectedAnnuityDummy.getValue(fixSchedule.getFixing(0), model) - 1;
	}

	/**
	 * Private constructor to create a base copy which is used to determine the actual expectationCorrection.
	 *
	 * @param numberOfPeriods
	 * @param periodLengths
	 * @param initialAnnuities
	 * @param initialSwapRates
	 * @param exponentialDriverMeans
	 * @param exponents
	 * @param denominators
	 * @param normalizer
	 */
	private BasicPiterbargAnnuityMapping(final int numberOfPeriods, final double[] periodLengths, final double[] initialAnnuities,
			final double[] initialSwapRates, final double[] exponentialDriverMeans, final double[] exponents, final double[] denominators, final NormalizingFunction normalizer) {
		super();
		this.numberOfPeriods = numberOfPeriods;
		this.periodLengths = periodLengths;
		this.initialAnnuities = initialAnnuities;
		this.initialSwapRates = initialSwapRates;
		this.exponentialDriverMeans = exponentialDriverMeans;
		this.exponents = exponents;
		this.denominators = denominators;
		this.expectationCorrection = 0;
		this.normalizer = normalizer;
	}

	@Override
	public double getValue(final double swapRate) {

		double term  = periodLengths[numberOfPeriods-1] *swapRate +1.0;
		term 		/= periodLengths[numberOfPeriods-1] *initialSwapRates[numberOfPeriods -1] +1.0;
		term 		/= exponentialDriverMeans[numberOfPeriods -1];

		double value = 0.0;
		for(int index = 0; index < numberOfPeriods; index++){
			value += (Math.pow(term, -exponents[index])) *periodLengths[index] /denominators[index];
		}

		return initialAnnuities[numberOfPeriods-1]  /value - expectationCorrection * normalizer.getValue(swapRate);
	}

	@Override
	public double getFirstDerivative(final double swapRate) {

		double term  = periodLengths[numberOfPeriods-1] *swapRate +1.0;
		term 		/= periodLengths[numberOfPeriods-1] *initialSwapRates[numberOfPeriods -1] +1.0;
		term 		/= exponentialDriverMeans[numberOfPeriods -1];

		double value = 0.0;
		double innerDerivative = 0.0;
		for(int index = 0; index < numberOfPeriods; index++){
			value += (Math.pow(term, -exponents[index])) *periodLengths[index] /denominators[index];
			innerDerivative += (Math.pow(term, -exponents[index] -1)) *periodLengths[index]
					*-exponents[index] /denominators[index];
		}

		innerDerivative *= periodLengths[numberOfPeriods-1]  /exponentialDriverMeans[numberOfPeriods -1]
				/(periodLengths[numberOfPeriods-1] *initialSwapRates[numberOfPeriods -1] +1.0);

		return -initialAnnuities[numberOfPeriods-1] *innerDerivative /value /value - expectationCorrection * normalizer.getFirstDerivative(swapRate);
	}

	@Override
	public double getSecondDerivative(final double swapRate) {

		double term  = periodLengths[numberOfPeriods-1] *swapRate +1.0;
		term 		/= periodLengths[numberOfPeriods-1] *initialSwapRates[numberOfPeriods -1] +1.0;
		term 		/= exponentialDriverMeans[numberOfPeriods -1];

		double value = 0.0;
		double innerFirst = 0.0;
		double innerSecond = 0.0;
		for(int index = 0; index < numberOfPeriods; index++){
			value += (Math.pow(term, -exponents[0])) *periodLengths[index] /denominators[index];
			innerFirst +=(Math.pow(term, -exponents[index] -1)) *periodLengths[index]
					*-exponents[index] /denominators[index];
			innerSecond += (Math.pow(term, -exponents[index] -2)) *periodLengths[index]
					*exponents[index] *(exponents[index] +1.0) /denominators[index];
		}

		innerFirst *= periodLengths[numberOfPeriods-1] /(periodLengths[numberOfPeriods-1] *initialSwapRates[numberOfPeriods -1] +1.0)
				/exponentialDriverMeans[numberOfPeriods -1];

		innerSecond *=periodLengths[numberOfPeriods-1] *periodLengths[numberOfPeriods-1]
				/(periodLengths[numberOfPeriods-1] *initialSwapRates[numberOfPeriods -1] +1.0)
				/(periodLengths[numberOfPeriods-1] *initialSwapRates[numberOfPeriods -1] +1.0)
				/exponentialDriverMeans[numberOfPeriods -1] /exponentialDriverMeans[numberOfPeriods -1];

		return ((2.0 *innerFirst *innerFirst /value /value /value) - (innerSecond /value /value)) *initialAnnuities[numberOfPeriods-1]
				- expectationCorrection * normalizer.getSecondDerivative(swapRate);
	}

	/**
	 * Calculates the constants \(e^{\mu_j} \) for all sub tenors.
	 *
	 * @param timePoints
	 * @param maturity
	 * @param strike
	 * @param volvolCube
	 * @param model
	 * @return
	 */
	private double[] findExponentialDriverMeans(final double[] timePoints, final double maturity, final double strike, final VolatilityCube volvolCube,
			final VolatilityCubeModel model){

		final double[] exponentialMeans = new double[numberOfPeriods];
		final double[] volatilities = new double[numberOfPeriods];
		for(int index = 0; index < numberOfPeriods; index++){
			volatilities[index] = volvolCube.getValue(model, timePoints[index], maturity, strike, quotingConvention);
		}

		//initial ExponentialMean does not require the adjustment term
		exponentialMeans[0]  = Math.exp(maturity *0.5 *volatilities[0] *volatilities[0]);
		exponentialMeans[0] *= periodLengths[0] /initialAnnuities[0] /(periodLengths[0] *initialSwapRates[0] +1.0);

		double adjustment;

		//used to calculate the adjustment term in the loop
		final double[] volatilitySums = new double[numberOfPeriods];
		final double[] adjustmentSummands = new double[numberOfPeriods];
		for(int index = 0; index < numberOfPeriods; index++) {
			volatilitySums[index] = volatilities[0];
			adjustmentSummands[index] = 1.0;
		}

		for(int index = 1; index < numberOfPeriods; index++){

			//approximation adjustment roughly annuities[index-1]
			//			adjustment = initialAnnuities[index-1];

			// calculate adjustment
			adjustment = 0.0;
			for(int innerIndex = 0; innerIndex < index; innerIndex++){
				adjustmentSummands[innerIndex] /= exponentialMeans[index-1] *(periodLengths[index-1] *initialSwapRates[index-1] +1.0);
				volatilitySums[innerIndex] += volatilities[index];
			}
			for(int sumIndex = 0; sumIndex < index; sumIndex++){
				adjustment += periodLengths[sumIndex] *adjustmentSummands[sumIndex] *Math.exp(maturity *volatilitySums[sumIndex]
						*volatilitySums[sumIndex] /2);
			}

			//finalize exponentialMeans
			exponentialMeans[index]  = Math.exp(maturity *0.5 *volatilities[index] *volatilities[index]);
			exponentialMeans[index] *= periodLengths[index];
			exponentialMeans[index] += adjustment;
			exponentialMeans[index] /= initialAnnuities[index] *(periodLengths[index] *initialSwapRates[index] + 1.0);
		}
		return exponentialMeans;
	}

	/**
	 * Calculates initial annuities on all sub-tenors.
	 *
	 * @param schedule
	 * @param discountCurveName
	 * @param model
	 * @return
	 */
	private double[] getAnnuities(final Schedule schedule, final String discountCurveName, final AnalyticModel model){
		final double[] annuities = new double[schedule.getNumberOfPeriods()];
		for(int annuityIndex=0; annuityIndex < schedule.getNumberOfPeriods();annuityIndex++){
			final Period[] periods = new Period[annuityIndex +1];
			for(int periodIndex=0; periodIndex <= annuityIndex; periodIndex++){
				periods[periodIndex] = schedule.getPeriod(periodIndex);
			}
			final ScheduleFromPeriods partSchedule = new ScheduleFromPeriods(schedule.getReferenceDate(), schedule.getDaycountconvention(), periods);
			annuities[annuityIndex] = SwapAnnuity.getSwapAnnuity(schedule.getFixing(0), partSchedule, model.getDiscountCurve(discountCurveName), model);
		}
		return annuities;
	}

	/**
	 * Calculates initial swap rates on all sub-tenors, after annuities have been determined.
	 *
	 * @param schedule
	 * @param discountCurveName
	 * @param model
	 * @return
	 */
	private double[] getForwardSwapRates(final Schedule schedule, final String discountCurveName, final AnalyticModel model){
		final double[] swapRates = new double[schedule.getNumberOfPeriods()];
		final double discount = model.getDiscountCurve(discountCurveName).getDiscountFactor(model, schedule.getFixing(0));
		for(int swapRateIndex=0; swapRateIndex < schedule.getNumberOfPeriods();swapRateIndex++){
			swapRates[swapRateIndex] = 1 - model.getDiscountCurve(discountCurveName).getDiscountFactor(model, schedule.getPayment(swapRateIndex)) / discount;
			swapRates[swapRateIndex] /= initialAnnuities[swapRateIndex];
		}
		return swapRates;
	}

}
