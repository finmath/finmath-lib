/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 28 Feb 2015
 */

package net.finmath.montecarlo.interestrate.products;

import static org.junit.Assert.assertEquals;

import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.ConstantMaturitySwaprate;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.montecarlo.interestrate.products.indices.LaggedIndex;
import net.finmath.montecarlo.interestrate.products.indices.LinearCombinationIndex;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;

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

		LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "35Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		/*
		 * Create Monte-Carlo leg
		 */
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex index = new LIBORIndex(0.0, 0.5);
		double spread = 0.0;
		ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
		SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationInterface model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		RandomVariableInterface value = leg.getValue(0.0, model);
		double valueSimulation = value.getAverage();
		System.out.println("Float leg (simulation): " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		String forwardCurveName = "forwardCurve";
		String discountCurveName = "discountCurve";
		net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		AnalyticModelInterface modelAnalytic = model.getModel().getAnalyticModel();
		double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
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

		LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12); 
		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "35Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		/*
		 * Create Monte-Carlo leg
		 */
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex index = null;
		double spread = 0.05;
		ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
		SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationInterface model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		RandomVariableInterface value = leg.getValue(0.0, model);
		double valueSimulation = value.getAverage();
		System.out.println("Fixed leg (simulation): " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		String forwardCurveName = null;
		String discountCurveName = "discountCurve";
		net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		AnalyticModelInterface modelAnalytic = model.getModel().getAnalyticModel();
		double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
		System.out.println("Fixed leg (analytic)..: " + valueAnalytic);

		System.out.println();

		assertEquals("Monte-Carlo value", valueAnalytic, valueSimulation, 4E-3);
	}

	@Test
	public void testCMSFloatLeg() throws CalculationException {

		/*
		 * Create a payment schedule from conventions
		 */
		LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "20Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex index = new ConstantMaturitySwaprate(10.0, 0.5);
		double spread = 0.0;

		SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationInterface model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		RandomVariableInterface value = leg.getValue(0.0, model);
		double valueSimulation = value.getAverage();
		System.out.println("CMS   leg (simulation)...........: " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		String forwardCurveName = "forwardCurve";
		String discountCurveName = "discountCurve";
		net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		AnalyticModelInterface modelAnalytic = model.getModel().getAnalyticModel();
		double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
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
		LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "20Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		AbstractNotional notional = new Notional(1.0);

		AbstractIndex cms10 = new ConstantMaturitySwaprate(10,0.5);
		AbstractIndex cms2 = new ConstantMaturitySwaprate(2,0.5);
		AbstractIndex cmsSpread = new LinearCombinationIndex(1.0, cms10, -1.0, cms2);
		AbstractIndex index = cmsSpread;

		double spread = 0.05;

		SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationInterface model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		RandomVariableInterface value = leg.getValue(0.0, model);
		double valueSimulation = value.getAverage();
		System.out.println("CMS leg (simulation)........: " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		String forwardCurveName = null;
		String discountCurveName = "discountCurve";
		net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		AnalyticModelInterface modelAnalytic = model.getModel().getAnalyticModel();
		double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
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
		LocalDate	referenceDate = LocalDate.of(2014,  Month.AUGUST,  12);
		int			spotOffsetDays = 2;
		String		forwardStartPeriod = "0D";
		String		maturity = "35Y";
		String		frequency = "semiannual";
		String		daycountConvention = "30/360";

		ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturity, frequency, daycountConvention, "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

		/*
		 * Create the leg with a notional and index
		 */
		AbstractNotional notional = new Notional(1.0);
		AbstractIndex liborIndex = new LIBORIndex(0.0, 0.5);
		AbstractIndex index = new LaggedIndex(liborIndex, 0.5 /* fixingOffset */);
		double spread = 0.0;

		SwapLeg leg = new SwapLeg(schedule, notional, index, spread, false /* isNotionalExchanged */);

		/*
		 * Create Monte-Carlo model
		 */
		int numberOfPaths = 10000;
		int numberOfFactors = 5;
		double correlationDecayParam = 0.2;
		LIBORModelMonteCarloSimulationInterface model = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		/*
		 * Monte-Carlo value
		 */
		RandomVariableInterface value = leg.getValue(0.0, model);
		double valueSimulation = value.getAverage();
		System.out.println("Arrears leg (simulation)........: " + value.getAverage() + "\t +/-" + value.getStandardError());

		/*
		 * Create analytic leg
		 */
		String forwardCurveName = "forwardCurve";
		String discountCurveName = "discountCurve";
		net.finmath.marketdata.products.SwapLeg legAnalytic = new net.finmath.marketdata.products.SwapLeg(schedule, forwardCurveName, spread, discountCurveName, false /* isNotionalExchanged */);

		/*
		 * Analytic value
		 */
		AnalyticModelInterface modelAnalytic = model.getModel().getAnalyticModel();
		double valueAnalytic = legAnalytic.getValue(0.0, modelAnalytic);
		System.out.println("Arrears leg (analytic, zero vol): " + valueAnalytic);
		System.out.println("Note: Analytic value does not consider the convexity adjustment.");

		System.out.println();

		Assert.assertTrue("Monte-Carlo value", valueSimulation > valueAnalytic);
	}

	public static LIBORModelMonteCarloSimulationInterface createMultiCurveLIBORMarketModel(int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

		LocalDate	referenceDate = LocalDate.of(2014, Month.AUGUST, 12); 


		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				referenceDate,
				"6M",
				new BusinessdayCalendarExcludingTARGETHolidays(),
				BusinessdayCalendarInterface.DateRollConvention.FOLLOWING,
				Curve.InterpolationMethod.LINEAR,
				Curve.ExtrapolationMethod.CONSTANT,
				Curve.InterpolationEntity.VALUE,
				ForwardCurve.InterpolationEntityForward.FORWARD,
				null,
				null,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */
				);

		// Create the discount curve
		DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);

		return createLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam, discountCurve, forwardCurve);
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			int numberOfPaths, int numberOfFactors, double correlationDecayParam, DiscountCurveInterface discountCurve, ForwardCurveInterface forwardCurve) throws CalculationException {

		AnalyticModelInterface model = new AnalyticModel(new CurveInterface[] { forwardCurve , discountCurve });

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 40.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);		

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 40.0;
		double dt		= 0.5;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double[][] volatility = new double[timeDiscretization.getNumberOfTimeSteps()][liborPeriodDiscretization.getNumberOfTimeSteps()];
		for (int timeIndex = 0; timeIndex < volatility.length; timeIndex++) {
			for (int liborIndex = 0; liborIndex < volatility[timeIndex].length; liborIndex++) {
				// Create a very simple volatility model here
				double time = timeDiscretization.getTime(timeIndex);
				double maturity = liborPeriodDiscretization.getTime(liborIndex);
				double timeToMaturity = maturity - time;

				double instVolatility;
				if(timeToMaturity <= 0)
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				else
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);

				// Store
				volatility[timeIndex][liborIndex] = instVolatility;
			}
		}
		LIBORVolatilityModelFromGivenMatrix volatilityModel = new LIBORVolatilityModelFromGivenMatrix(timeDiscretization, liborPeriodDiscretization, volatility);

		/*
		 * Create a correlation model rho_{i,j} = exp(-a * abs(T_i-T_j))
		 */
		LIBORCorrelationModelExponentialDecay correlationModel = new LIBORCorrelationModelExponentialDecay(
				timeDiscretization, liborPeriodDiscretization, numberOfFactors,
				correlationDecayParam);


		/*
		 * Combine volatility model and correlation model to a covariance model
		 */
		LIBORCovarianceModelFromVolatilityAndCorrelation covarianceModel =
				new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization,
						liborPeriodDiscretization, volatilityModel, correlationModel);

		// BlendedLocalVolatlityModel (future extension)
		//		AbstractLIBORCovarianceModel covarianceModel2 = new BlendedLocalVolatlityModel(covarianceModel, 0.00, false);

		// Set model properties
		Map<String, String> properties = new HashMap<String, String>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		LIBORMarketModel.CalibrationItem[] calibrationItems = new LIBORMarketModel.CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(
				liborPeriodDiscretization, model, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		ProcessEulerScheme process = new ProcessEulerScheme(
				new net.finmath.montecarlo.BrownianMotion(timeDiscretization,
						numberOfFactors, numberOfPaths, 3141 /* seed */));
		//		process.setScheme(ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}
}
