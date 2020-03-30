/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28 Feb 2015
 */

package net.finmath.montecarlo.interestrate.products;

import static org.junit.Assert.assertEquals;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.components.NotionalFromConstant;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.ConstantMaturitySwaprate;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.interestrate.products.indices.LaggedIndex;
import net.finmath.montecarlo.interestrate.products.indices.LinearCombinationIndex;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

/**
 * @author Christian Fries
 *
 */
public class SwapLegTest {

	/**
	 * Test a floating rate leg.
	 *
	 * @throws CalculationException Thrown if calculation fails.
	 */
	@Test
	public void testFloatLeg() throws CalculationException {

		final LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		final int			spotOffsetDays = 2;
		final String		forwardStartPeriod = "0D";
		final String		maturity = "35Y";
		final String		frequency = "semiannual";
		final String		daycountConvention = "30/360";

		/*
		 * Create Monte-Carlo leg
		 */
		final Notional notional = new NotionalFromConstant(1.0);
		final AbstractIndex index = new LIBORIndex(0.0, 0.5);
		final double spread = 0.0;
		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
		final SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		final int numberOfPaths = 10000;
		final int numberOfFactors = 5;
		final double correlationDecayParam = 0.2;
		final LIBORModelMonteCarloSimulationModel model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		final RandomVariable value = leg.getValue(0.0, model);
		final double valueSimulation = value.getAverage();
		System.out.println("Float leg (simulation): " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		final String forwardCurveName = "forwardCurve";
		final String discountCurveName = "discountCurve";
		final net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		final AnalyticModel modelAnalytic = model.getModel().getAnalyticModel();
		final double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
		System.out.println("Float leg (analytic)..: " + valueAnalytic);

		System.out.println();

		assertEquals("Monte-Carlo value", valueAnalytic, valueSimulation, 5E-3);
	}

	/**
	 * Test a floating rate leg.
	 *
	 * @throws CalculationException Thrown if calculation fails.
	 */
	@Test
	public void testFixLeg() throws CalculationException {

		final LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12);
		final int			spotOffsetDays = 2;
		final String		forwardStartPeriod = "0D";
		final String		maturity = "35Y";
		final String		frequency = "semiannual";
		final String		daycountConvention = "30/360";

		/*
		 * Create Monte-Carlo leg
		 */
		final Notional notional = new NotionalFromConstant(1.0);
		final AbstractIndex index = null;
		final double spread = 0.05;
		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
		final SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		final int numberOfPaths = 10000;
		final int numberOfFactors = 5;
		final double correlationDecayParam = 0.2;
		final LIBORModelMonteCarloSimulationModel model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		final RandomVariable value = leg.getValue(0.0, model);
		final double valueSimulation = value.getAverage();
		System.out.println("Fixed leg (simulation): " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		final String forwardCurveName = null;
		final String discountCurveName = "discountCurve";
		final net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		final AnalyticModel modelAnalytic = model.getModel().getAnalyticModel();
		final double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
		System.out.println("Fixed leg (analytic)..: " + valueAnalytic);

		System.out.println();

		assertEquals("Monte-Carlo value", valueAnalytic, valueSimulation, 4E-3);
	}

	@Test
	public void testCMSFloatLeg() throws CalculationException {

		/*
		 * Create a payment schedule from conventions
		 */
		final LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		final int			spotOffsetDays = 2;
		final String		forwardStartPeriod = "0D";
		final String		maturity = "20Y";
		final String		frequency = "semiannual";
		final String		daycountConvention = "30/360";

		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		final Notional notional = new NotionalFromConstant(1.0);
		final AbstractIndex index = new ConstantMaturitySwaprate(10.0, 0.5);
		final double spread = 0.0;

		final SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		final int numberOfPaths = 10000;
		final int numberOfFactors = 5;
		final double correlationDecayParam = 0.2;
		final LIBORModelMonteCarloSimulationModel model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		final RandomVariable value = leg.getValue(0.0, model);
		final double valueSimulation = value.getAverage();
		System.out.println("CMS   leg (simulation)...........: " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		final String forwardCurveName = "forwardCurve";
		final String discountCurveName = "discountCurve";
		final net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		final AnalyticModel modelAnalytic = model.getModel().getAnalyticModel();
		final double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
		System.out.println("CMS   leg (analytic, zero vol)...: " + valueAnalytic);
		System.out.println("Note: Analytic value does not consider the convexity adjustment.");

		System.out.println();

		Assert.assertTrue("Monte-Carlo value", valueSimulation > valueAnalytic);
	}

	/**
	 * Test a CMS spread floating rate leg.
	 *
	 * @throws CalculationException Thrown if calculation fails.
	 */
	@Test
	public void testCMSSpreadLeg() throws CalculationException {

		/*
		 * Create a payment schedule from conventions
		 */
		final LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		final int			spotOffsetDays = 2;
		final String		forwardStartPeriod = "0D";
		final String		maturity = "20Y";
		final String		frequency = "semiannual";
		final String		daycountConvention = "30/360";

		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		final Notional notional = new NotionalFromConstant(1.0);

		final AbstractIndex cms10 = new ConstantMaturitySwaprate(10,0.5);
		final AbstractIndex cms2 = new ConstantMaturitySwaprate(2,0.5);
		final AbstractIndex cmsSpread = new LinearCombinationIndex(1.0, cms10, -1.0, cms2);
		final AbstractIndex index = cmsSpread;

		final double spread = 0.05;

		final SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		final int numberOfPaths = 10000;
		final int numberOfFactors = 5;
		final double correlationDecayParam = 0.2;
		final LIBORModelMonteCarloSimulationModel model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		final RandomVariable value = leg.getValue(0.0, model);
		final double valueSimulation = value.getAverage();
		System.out.println("CMS leg (simulation)........: " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		final String forwardCurveName = null;
		final String discountCurveName = "discountCurve";
		final net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		final AnalyticModel modelAnalytic = model.getModel().getAnalyticModel();
		final double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
		System.out.println("CMS leg (analytic, zero vol): " + valueAnalytic);
		System.out.println("Note: Analytic value does not consider the convexity adjustment.");

		System.out.println();

		Assert.assertTrue("Monte-Carlo value", valueSimulation > valueAnalytic);
	}

	@Test
	public void testLIBORInArrearsFloatLeg() throws CalculationException {

		/*
		 * Create a payment schedule from conventions
		 */
		final LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		final int			spotOffsetDays = 2;
		final String		forwardStartPeriod = "0D";
		final String		maturity = "35Y";
		final String		frequency = "semiannual";
		final String		daycountConvention = "30/360";

		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		final Notional notional = new NotionalFromConstant(1.0);
		final AbstractIndex liborIndex = new LIBORIndex(0.0, 0.5);
		final AbstractIndex index = new LaggedIndex(liborIndex, 0.5 /* fixingOffset */);
		final double spread = 0.0;

		final SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		final int numberOfPaths = 10000;
		final int numberOfFactors = 5;
		final double correlationDecayParam = 0.2;
		final LIBORModelMonteCarloSimulationModel model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		final RandomVariable value = leg.getValue(0.0, model);
		final double valueSimulation = value.getAverage();
		System.out.println("Arrears leg (simulation)........: " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		final String forwardCurveName = "forwardCurve";
		final String discountCurveName = "discountCurve";
		final net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		final AnalyticModel modelAnalytic = model.getModel().getAnalyticModel();
		final double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
		System.out.println("Arrears leg (analytic, zero vol): " + valueAnalytic);
		System.out.println("Note: Analytic value does not consider the convexity adjustment.");

		System.out.println();

		Assert.assertTrue("Monte-Carlo value", valueSimulation > valueAnalytic);
	}

	public static LIBORModelMonteCarloSimulationModel createMultiCurveLIBORMarketModel(final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException {

		final LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12);


		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				referenceDate,
				"6M",
				new BusinessdayCalendarExcludingTARGETHolidays(),
				BusinessdayCalendar.DateRollConvention.FOLLOWING,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.CONSTANT,
				CurveInterpolation.InterpolationEntity.VALUE,
				ForwardCurveInterpolation.InterpolationEntityForward.FORWARD,
				null,
				null,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */
				);

		// Create the discount curve
		final DiscountCurveInterpolation discountCurveInterpolation = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);

		return createLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam, discountCurveInterpolation, forwardCurveInterpolation);
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam, final DiscountCurve discountCurve, final ForwardCurve forwardCurve) throws CalculationException {

		final AnalyticModel model = new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve , discountCurve });

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 40.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 40.0;
		final double dt		= 0.5;

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
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModel = new LIBORMarketModelFromCovarianceModel(
				liborPeriodDiscretization, model, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(
				liborMarketModel,
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 3141 /* seed */));
		//		process.setScheme(EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}
}
