/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 01.10.2015
 */

package net.finmath.montecarlo.interestrate.products.components;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel.Measure;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Swap;
import net.finmath.montecarlo.interestrate.products.SwapLeg;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.RegularSchedule;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

/**
 * @author Christian Fries
 *
 */
public class ExposureTest {

	private final NumberFormat formatter6 = new DecimalFormat("0.000000", new DecimalFormatSymbols(new Locale("en")));

	@Test
	public void testExpectedPositiveExposure() throws CalculationException {
		/*
		 * Create a receiver swap (receive fix, pay float)
		 */
		final Schedule legScheduleRec = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2015, Month.JANUARY, 3) /* referenceDate */,
				LocalDate.of(2015, Month.JANUARY, 6) /* startDate */,
				LocalDate.of(2025, Month.JANUARY, 6) /* maturityDate */,
				ScheduleGenerator.Frequency.ANNUAL /* frequency */,
				ScheduleGenerator.DaycountConvention.ACT_365 /* daycountConvention */,
				ScheduleGenerator.ShortPeriodConvention.FIRST /* shortPeriodConvention */,
				BusinessdayCalendar.DateRollConvention.FOLLOWING /* dateRollConvention */,
				new BusinessdayCalendarExcludingTARGETHolidays() /* businessdayCalendar */,
				0 /* fixingOffsetDays */,
				0 /* paymentOffsetDays */);

		final Schedule legSchedulePay = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2015, Month.JANUARY, 3) /* referenceDate */,
				LocalDate.of(2015, Month.JANUARY, 6) /* startDate */,
				LocalDate.of(2025, Month.JANUARY, 6) /* maturityDate */,
				ScheduleGenerator.Frequency.QUARTERLY /* frequency */,
				ScheduleGenerator.DaycountConvention.ACT_365 /* daycountConvention */,
				ScheduleGenerator.ShortPeriodConvention.FIRST /* shortPeriodConvention */,
				BusinessdayCalendar.DateRollConvention.FOLLOWING /* dateRollConvention */,
				new BusinessdayCalendarExcludingTARGETHolidays() /* businessdayCalendar */,
				0 /* fixingOffsetDays */,
				0 /* paymentOffsetDays */);
		final Notional notional = new NotionalFromConstant(1.0);
		final AbstractIndex index = new LIBORIndex("forwardCurve", 0.0, 0.25);
		final double fixedCoupon = 0.025;

		final SwapLeg swapLegRec = new SwapLeg(legScheduleRec, notional, null, fixedCoupon /* spread */, false /* isNotionalExchanged */);
		final SwapLeg swapLegPay = new SwapLeg(legSchedulePay, notional, index, 0.0 /* spread */, false /* isNotionalExchanged */);
		final AbstractLIBORMonteCarloProduct swap = new Swap(swapLegRec, swapLegPay);
		final TermStructureMonteCarloProduct swapExposureEstimator = new ExposureEstimator(swap);


		final LIBORModelMonteCarloSimulationModel lmm = createLIBORMarketModel(Measure.SPOT, 10000, 5, 0.1);

		// Print a single exposure path and the expected positive exposure
		for(final double observationDate : lmm.getTimeDiscretization()) {

			if(observationDate == 0) {
				continue;
			}

			/*
			 * Calculate expected positive exposure of a swap
			 */
			final RandomVariable valuesSwap = swap.getValue(observationDate, lmm);
			final RandomVariable valuesEstimatedExposure = swapExposureEstimator.getValue(observationDate, lmm);
			final RandomVariable valuesPositiveExposure = valuesSwap.mult(valuesEstimatedExposure.choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0)));

			final double exposureOnPath				= valuesEstimatedExposure.get(0);
			final double expectedPositiveExposure		= valuesPositiveExposure.getAverage();
			final double exposureQuantil				= valuesEstimatedExposure.getQuantile(0.95);

			System.out.println(observationDate + "\t" + formatter6.format(exposureQuantil) + " \t " + formatter6.format(expectedPositiveExposure));

			final double basisPoint = 1E-4;
			Assert.assertTrue("Expected positive exposure", expectedPositiveExposure >= 0-1*basisPoint);
		}
	}


	@Test
	public void testAgainstSwaption() throws CalculationException {
		/*
		 * Create a receiver swap (receive fix, pay float)
		 */
		final TimeDiscretizationFromArray tenor = new TimeDiscretizationFromArray(0.0, 40, 0.25);
		final Schedule schedule = new RegularSchedule(tenor);

		final Notional notional = new NotionalFromConstant(1.0);
		final AbstractIndex index = new LIBORIndex("forwardCurve", 0.0, 0.25);
		final double fixedCoupon = 0.025;

		final SwapLeg swapLegRec = new SwapLeg(schedule, notional, index, 0.0 /* spread */, false /* isNotionalExchanged */);
		final SwapLeg swapLegPay = new SwapLeg(schedule, notional, null, fixedCoupon /* spread */, false /* isNotionalExchanged */);
		final AbstractLIBORMonteCarloProduct swap = new Swap(swapLegRec, swapLegPay);
		final TermStructureMonteCarloProduct swapExposureEstimator = new ExposureEstimator(swap);

		final LIBORModelMonteCarloSimulationModel lmm = createLIBORMarketModel(Measure.SPOT, 10000, 5, 0.1);

		// Print a single exposure path and the expected positive exposure
		for(final double observationDate : lmm.getTimeDiscretization()) {

			/*
			 * Calculate expected positive exposure of a swap
			 */
			final RandomVariable valuesSwap = swap.getValue(observationDate, lmm);
			final RandomVariable valuesEstimatedExposure = swapExposureEstimator.getValue(observationDate, lmm);
			final RandomVariable valuesPositiveExposure = valuesSwap.mult(valuesEstimatedExposure.choose(new Scalar(1.0), new Scalar(0.0)));

			final double exposureOnPath = valuesEstimatedExposure.get(0);
			final double expectedPositiveExposure				= valuesPositiveExposure.getAverage();
			final double expectedPositiveExposureFromEstimate	= valuesEstimatedExposure.floor(0.0).getAverage();

			/*
			 * Benchmark value against a swaption
			 */
			final double exerciseDate = observationDate;
			final TermStructureMonteCarloProduct swaption = new Swaption(exerciseDate, tenor, fixedCoupon);
			final double swaptionValue = (Double)swaption.getValues(observationDate, lmm).get("value");

			System.out.println(observationDate + "\t" + formatter6.format(exposureOnPath) + " \t " + formatter6.format(expectedPositiveExposureFromEstimate) + " \t " + formatter6.format(expectedPositiveExposure) + " \t " + formatter6.format(swaptionValue) + " \t " + formatter6.format((expectedPositiveExposure-swaptionValue)*10000));

			final double basisPoint = 1E-4;
			Assert.assertEquals("Expected positive exposure", swaptionValue, expectedPositiveExposure, 25*basisPoint);
		}
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final Measure measure, final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.25;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.0 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.025, 0.025, 0.025, 0.025, 0.025}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		// Create the discount curve
		final DiscountCurveInterpolation discountCurveInterpolation = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.02, 0.02, 0.02, 0.02, 0.03}	/* zero rates */
				);

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 15.0;
		final double dt		= 0.125;

		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		final double[][] volatility = new double[timeDiscretizationFromArray.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
			for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
				// Create a very simple volatility model here
				final double time = timeDiscretizationFromArray.getTime(timeIndex);
				final double maturity = liborPeriodDiscretization.getTime(liborIndex);
				final double timeToMaturity = maturity - time;

				double instVolatility;
				if(timeToMaturity <= 0) {
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				} else {
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);
				}

				// Store
				volatility[timeIndex][liborIndex] = instVolatility;
			}
		}
		final LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretizationFromArray, liborPeriodDiscretization, volatility);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		final LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		final LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		final Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", measure.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(
				liborPeriodDiscretization, forwardCurveInterpolation, discountCurveInterpolation, covarianceModel, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel,
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 3141 /* seed */), EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}
}
