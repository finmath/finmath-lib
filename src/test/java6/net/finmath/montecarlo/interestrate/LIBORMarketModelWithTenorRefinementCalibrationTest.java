/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 16.01.2015
 */
package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.calibration.ParameterObjectInterface;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.products.AnalyticProductInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.LIBORMarketModelWithTenorRefinement.CalibrationItem;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.modelplugins.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.DisplacedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelPiecewiseConstant;
import net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolailityModelInterface;
import net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.TermStructCovarianceModelFromLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.modelplugins.TermStructureCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.modelplugins.TermStructureTenorTimeScalingInterface;
import net.finmath.montecarlo.interestrate.modelplugins.TermStructureTenorTimeScalingPicewiseConstant;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.interestrate.products.VolatilitySufaceRoughness;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.OptimizerFactoryCMAES;
import net.finmath.optimizer.OptimizerFactoryInterface;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretization.ShortPeriodLocation;
import net.finmath.time.TimeDiscretizationInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * This class tests the LIBOR market model and products.
 * 
 * @author Christian Fries
 */
public class LIBORMarketModelWithTenorRefinementCalibrationTest {

	private final int numberOfPaths		= 10000;
	private final int numberOfFactors	= 1;

	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.0000%;-##0.0000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterParam		= new DecimalFormat(" #0.00000;-#0.00000", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	public static void main(String[] args) throws CalculationException, SolverException {
		LIBORMarketModelWithTenorRefinementCalibrationTest test = new LIBORMarketModelWithTenorRefinementCalibrationTest();
		test.testATMSwaptionCalibration();
	}
	
	public LIBORMarketModelWithTenorRefinementCalibrationTest() throws CalculationException {
	}

	private CalibrationItem createCalibrationItem(double weight, double exerciseDate, double swapPeriodLength, int numberOfPeriods, double moneyness, double targetVolatility, String targetVolatilityType, ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve) throws CalculationException {

		double[]	fixingDates			= new double[numberOfPeriods];
		double[]	paymentDates		= new double[numberOfPeriods];
		double[]	swapTenor			= new double[numberOfPeriods + 1];

		for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
			fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
			swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
		}
		swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

		// Swaptions swap rate
		double swaprate = moneyness + getParSwaprate(forwardCurve, discountCurve, swapTenor);

		// Set swap rates for each period
		double[] swaprates = new double[numberOfPeriods];
		Arrays.fill(swaprates, swaprate);

		/*
		 * We use Monte-Carlo calibration on implied volatility.
		 * Alternatively you may change here to Monte-Carlo valuation on price or
		 * use an analytic approximation formula, etc.
		 */
		SwaptionSimple swaptionMonteCarlo = new SwaptionSimple(swaprate, swapTenor, SwaptionSimple.ValueUnit.valueOf(targetVolatilityType));
		//		double targetValuePrice = AnalyticFormulas.blackModelSwaptionValue(swaprate, targetVolatility, fixingDates[0], swaprate, getSwapAnnuity(discountCurve, swapTenor));
		return new CalibrationItem(swaptionMonteCarlo, targetVolatility, weight);
	}

	public void testSwaptionSmileCalibration() throws CalculationException {

		/*
		 * Calibration test
		 */
		System.out.println("Calibration to Swaptions:");

		double[] fixingTimes = new double[] {
				0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5, 10.0, 10.5, 11.0, 11.5, 12.0, 12.5, 13.0, 13.5, 14.0, 14.5, 15.0, 15.5, 16.0, 16.5, 17.0, 17.5, 18.0, 18.5, 19.0, 19.5, 20.0, 20.5, 21.0, 21.5, 22.0, 22.5, 23.0, 23.5, 24.0, 24.5, 25.0, 25.5, 26.0, 26.5, 27.0, 27.5, 28.0, 28.5, 29.0, 29.5, 30.0, 30.5, 31.0, 31.5, 32.0, 32.5, 33.0, 33.5, 34.0, 34.5, 35.0, 35.5, 36.0, 36.5, 37.0, 37.5, 38.0, 38.5, 39.0, 39.5, 40.0, 40.5, 41.0, 41.5, 42.0, 42.5, 43.0, 43.5, 44.0, 44.5, 45.0, 45.5, 46.0, 46.5, 47.0, 47.5, 48.0, 48.5, 49.0, 49.5, 50.0
		};

		double[] forwardRates = new double[] {
				0.61/100.0,	0.61/100.0,	0.67/100.0,	0.73/100.0,	0.80/100.0,	0.92/100.0,	1.11/100.0,	1.36/100.0,	1.60/100.0,	1.82/100.0,	2.02/100.0,	2.17/100.0,	2.27/100.0,	2.36/100.0,	2.46/100.0,	2.52/100.0,	2.54/100.0,	2.57/100.0,	2.68/100.0,	2.82/100.0,	2.92/100.0,	2.98/100.0,	3.00/100.0,	2.99/100.0,	2.95/100.0,	2.89/100.0,	2.82/100.0,	2.74/100.0,	2.66/100.0,	2.59/100.0,	2.52/100.0,	2.47/100.0,	2.42/100.0,	2.38/100.0,	2.35/100.0,	2.33/100.0,	2.31/100.0,	2.30/100.0,	2.29/100.0,	2.28/100.0,	2.27/100.0,	2.27/100.0,	2.26/100.0,	2.26/100.0,	2.26/100.0,	2.26/100.0,	2.26/100.0,	2.26/100.0,	2.27/100.0,	2.28/100.0,	2.28/100.0,	2.30/100.0,	2.31/100.0,	2.32/100.0,	2.34/100.0,	2.35/100.0,	2.37/100.0,	2.39/100.0,	2.42/100.0,	2.44/100.0,	2.47/100.0,	2.50/100.0,	2.52/100.0,	2.56/100.0,	2.59/100.0,	2.62/100.0,	2.65/100.0,	2.68/100.0,	2.72/100.0,	2.75/100.0,	2.78/100.0,	2.81/100.0,	2.83/100.0,	2.86/100.0,	2.88/100.0,	2.91/100.0,	2.93/100.0,	2.94/100.0,	2.96/100.0,	2.97/100.0,	2.97/100.0,	2.97/100.0,	2.97/100.0,	2.97/100.0,	2.96/100.0,	2.95/100.0,	2.94/100.0,	2.93/100.0,	2.91/100.0,	2.89/100.0,	2.87/100.0,	2.85/100.0,	2.83/100.0,	2.80/100.0,	2.78/100.0,	2.75/100.0,	2.72/100.0,	2.69/100.0,	2.67/100.0,	2.64/100.0,	2.64/100.0
		};

		double liborPeriodLength = 0.5;

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"		/* name of the curve */,
				fixingTimes			/* fixings of the forward */,
				forwardRates		/* forwards */,
				liborPeriodLength	/* tenor / period length */
				);


		DiscountCurveInterface discountCurve = new DiscountCurveFromForwardCurve(forwardCurve, liborPeriodLength);

		/*
		 * Create a set of calibration products.
		 */
		ArrayList<CalibrationItem> calibrationItems = new ArrayList<CalibrationItem>();

		double	swapPeriodLength	= 0.5;
		int		numberOfPeriods		= 20;

		double[] smileMoneynesses	= { -0.02,	-0.01, -0.005, -0.0025,	0.0,	0.0025,	0.0050,	0.01,	0.02 };
		double[] smileVolatilities	= { 0.559,	0.377,	0.335,	 0.320,	0.308, 0.298, 0.290, 0.280, 0.270 };

		for(int i=0; i<smileMoneynesses.length; i++ ) {
			double	exerciseDate		= 5.0;
			double	moneyness			= smileMoneynesses[i];
			double	targetVolatility	= smileVolatilities[i];
			String	targetVolatilityType = "VOLATILITY";

			calibrationItems.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurve, discountCurve));
		}

		double[] atmOptionMaturities	= { 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 };
		double[] atmOptionVolatilities	= { 0.385, 0.351, 0.325, 0.308, 0.288, 0.279, 0.290, 0.272, 0.235, 0.192 };

		for(int i=0; i<atmOptionMaturities.length; i++ ) {

			double	exerciseDate		= atmOptionMaturities[i];
			double	moneyness			= 0.0;
			double	targetVolatility	= atmOptionVolatilities[i];
			String	targetVolatilityType = "VOLATILITY";

			calibrationItems.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurve, discountCurve));
		}

		/*
		 * Create a simulation time discretization
		 */
		// If simulation time is below libor time, exceptions will be hard to track.
		double lastTime	= 40.0;
		double dt		= 1.0;//0.0625;
		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);
		TimeDiscretizationInterface liborPeriodDiscretization = timeDiscretization;

		/*
		 * Create Brownian motions 
		 */
		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors , numberOfPaths, 31415 /* seed */);

		//		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelTimeHomogenousPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 ), new double[] { 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0 });
		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 ), new TimeDiscretization(2.00, 5.00,10.00, 15.00, 30.00 ), 0.20 / 100);
		LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, 0.25);
		// Create a covariance model
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretization, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20/100.0, 0.05/100.0, 0.10, 0.05/100.0, 0.10} );
		covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization, liborPeriodDiscretization, volatilityModel, correlationModel);

		// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
		AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModelParametric, 0.0, false);
		// Create stochastic scaling (pass brownianMotionView2 to it)

		// Set model properties
		Map<String, Object> properties = new HashMap<String, Object>();

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
		Map<String, Object> calibrationParameters = new HashMap<String, Object>();
		calibrationParameters.put("accuracy", new Double(1E-6));
		calibrationParameters.put("brownianMotion", brownianMotion);
		properties.put("calibrationParameters", calibrationParameters);

		/*
		 * Create corresponding LIBOR Market Model
		 */
		TimeDiscretizationInterface liborPeriodDiscretizationFine = new TimeDiscretization(0.0, 40.0, 0.0625, ShortPeriodLocation.SHORT_PERIOD_AT_START);
		TimeDiscretizationInterface liborPeriodDiscretizationMedium = new TimeDiscretization(0.0, 40.0, 0.25, ShortPeriodLocation.SHORT_PERIOD_AT_START);
		TimeDiscretizationInterface liborPeriodDiscretizationCoarse = new TimeDiscretization(0.0, 40.0, 4.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
		TermStructureModelInterface liborMarketModelCalibrated = new LIBORMarketModelWithTenorRefinement(
				new TimeDiscretizationInterface[] { liborPeriodDiscretizationFine, liborPeriodDiscretizationMedium, liborPeriodDiscretizationCoarse },
				new Integer[] { 4, 8, 200 },
				null,
				forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve),
				new TermStructCovarianceModelFromLIBORCovarianceModelParametric(null, covarianceModelParametric), calibrationItems.toArray(new CalibrationItem[0]), properties);


		/*
		 * Test our calibration
		 */
		System.out.println("\nCalibrated parameters are:");
		double[] param = ((TermStructureCovarianceModelParametric)((LIBORMarketModelWithTenorRefinement) liborMarketModelCalibrated).getCovarianceModel()).getParameter();
		//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
		for (double p : param) System.out.println(formatterParam.format(p));

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
		TermStructureModelMonteCarloSimulation simulationCalibrated = new TermStructureModelMonteCarloSimulation(liborMarketModelCalibrated, process);

		System.out.println("\nValuation on calibrated model:");
		double deviationSum			= 0.0;
		double deviationSquaredSum	= 0.0;
		for (int i = 0; i < calibrationItems.size(); i++) {
			AbstractLIBORMonteCarloProduct calibrationProduct = calibrationItems.get(i).calibrationProduct;
			try {
				double valueModel = calibrationProduct.getValue(simulationCalibrated);
				double valueTarget = calibrationItems.get(i).calibrationTargetValue;
				double error = valueModel-valueTarget;
				deviationSum += error;
				deviationSquaredSum += error*error;
				System.out.println("Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget) + "\t" + calibrationProduct.toString());
			}
			catch(Exception e) {
				e.printStackTrace();
			}
		}
		double averageDeviation = deviationSum/calibrationItems.size();
		System.out.println("Mean Deviation:" + formatterValue.format(averageDeviation));
		System.out.println("RMS Error.....:" + formatterValue.format(Math.sqrt(deviationSquaredSum/calibrationItems.size())));
		System.out.println("__________________________________________________________________________________________\n");

		Assert.assertTrue(Math.abs(averageDeviation) < 1E-2);
	}

	public void testATMSwaptionCalibration() throws CalculationException, SolverException {

		/*
		 * Calibration test
		 */
		System.out.println("Calibration to Swaptions:");

		final AnalyticModelInterface curveModel = getCalibratedCurve();

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterface forwardCurve = curveModel.getForwardCurve("ForwardCurveFromDiscountCurve(discountCurve-EUR,6M)");

		final DiscountCurveInterface discountCurve = curveModel.getDiscountCurve("discountCurve-EUR");

		/*
		 * Create a set of calibration products.
		 */
		ArrayList<String>			calibrationItemNames	= new ArrayList<String>();
		final ArrayList<CalibrationItem>	calibrationItems		= new ArrayList<CalibrationItem>();

		double	swapPeriodLength	= 0.5;

		String[] atmExpiries = { "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y" };
		String[] atmTenors = { "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y" };
		double[] atmNormalVolatilities = { 0.00151, 0.00169, 0.0021, 0.00248, 0.00291, 0.00329, 0.00365, 0.004, 0.00437, 0.00466, 0.00527, 0.00571, 0.00604, 0.00625, 0.0016, 0.00174, 0.00217, 0.00264, 0.00314, 0.00355, 0.00398, 0.00433, 0.00469, 0.00493, 0.00569, 0.00607, 0.00627, 0.00645, 0.00182, 0.00204, 0.00238, 0.00286, 0.00339, 0.00384, 0.00424, 0.00456, 0.00488, 0.0052, 0.0059, 0.00623, 0.0064, 0.00654, 0.00205, 0.00235, 0.00272, 0.0032, 0.00368, 0.00406, 0.00447, 0.00484, 0.00515, 0.00544, 0.00602, 0.00629, 0.0064, 0.00646, 0.00279, 0.00319, 0.0036, 0.00396, 0.00436, 0.00469, 0.00503, 0.0053, 0.00557, 0.00582, 0.00616, 0.00628, 0.00638, 0.00641, 0.00379, 0.00406, 0.00439, 0.00472, 0.00504, 0.00532, 0.0056, 0.00582, 0.00602, 0.00617, 0.0063, 0.00636, 0.00638, 0.00639, 0.00471, 0.00489, 0.00511, 0.00539, 0.00563, 0.00583, 0.006, 0.00618, 0.0063, 0.00644, 0.00641, 0.00638, 0.00635, 0.00634, 0.00544, 0.00557, 0.00572, 0.00591, 0.00604, 0.00617, 0.0063, 0.00641, 0.00651, 0.00661, 0.00645, 0.00634, 0.00627, 0.00624, 0.00625, 0.00632, 0.00638, 0.00644, 0.0065, 0.00655, 0.00661, 0.00667, 0.00672, 0.00673, 0.00634, 0.00614, 0.00599, 0.00593, 0.00664, 0.00671, 0.00675, 0.00676, 0.00676, 0.00675, 0.00676, 0.00674, 0.00672, 0.00669, 0.00616, 0.00586, 0.00569, 0.00558, 0.00647, 0.00651, 0.00651, 0.00651, 0.00652, 0.00649, 0.00645, 0.0064, 0.00637, 0.00631, 0.00576, 0.00534, 0.00512, 0.00495, 0.00615, 0.0062, 0.00618, 0.00613, 0.0061, 0.00607, 0.00602, 0.00596, 0.00591, 0.00586, 0.00536, 0.00491, 0.00469, 0.0045, 0.00578, 0.00583, 0.00579, 0.00574, 0.00567, 0.00562, 0.00556, 0.00549, 0.00545, 0.00538, 0.00493, 0.00453, 0.00435, 0.0042, 0.00542, 0.00547, 0.00539, 0.00532, 0.00522, 0.00516, 0.0051, 0.00504, 0.005, 0.00495, 0.00454, 0.00418, 0.00404, 0.00394 };

		LocalDate referenceDate = new LocalDate(2016, DateTimeConstants.SEPTEMBER, 30); 
		BusinessdayCalendarExcludingTARGETHolidays cal = new BusinessdayCalendarExcludingTARGETHolidays();
		DayCountConvention_ACT_365 modelDC = new DayCountConvention_ACT_365();
		for(int i=0; i<atmNormalVolatilities.length; i++ ) {

			LocalDate exerciseDate = cal.createDateFromDateAndOffsetCode(referenceDate, atmExpiries[i]);
			LocalDate tenorEndDate = cal.createDateFromDateAndOffsetCode(exerciseDate, atmTenors[i]);
			double	exercise		= modelDC.getDaycountFraction(referenceDate, exerciseDate);
			double	tenor			= modelDC.getDaycountFraction(exerciseDate, tenorEndDate);

			exercise = Math.round(exercise/0.25)*0.25;
			tenor = Math.round(tenor/0.25)*0.25;

			if(exercise < 0.25) continue;
//			if(exercise < 1.0) continue;

			int numberOfPeriods = (int)Math.round(tenor / swapPeriodLength);

			double	moneyness			= 0.0;
			double	targetVolatility	= atmNormalVolatilities[i];

			String	targetVolatilityType = "VOLATILITYNORMAL";

			double	weight = 1.0;

			//			if(exercise != 1.0 && (exercise+tenor < 30 || exercise+tenor >= 40)) weight = 0.01;
			//			if((exercise+tenor < 30 || exercise+tenor >= 40)) weight = 0.01;

			calibrationItems.add(createCalibrationItem(weight, exercise, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurve, discountCurve));
			calibrationItemNames.add(atmExpiries[i]+"\t"+atmTenors[i]);
		}

		calibrationItems.add(new CalibrationItem(new VolatilitySufaceRoughness(), 0.0, 1.0));
		calibrationItemNames.add("Volatility surface roughness");

		/*
		 * Create a simulation time discretization
		 */
		// If simulation time is below libor time, exceptions will be hard to track.
		double lastTime	= 40.0;
		double dt		= 0.25;
		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);
		final TimeDiscretizationInterface liborPeriodDiscretization = timeDiscretization;

		/*
		 * Create Brownian motions 
		 */
		final BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */);

		int test = 0;			// 0 LMM with refinment, 1 LMMM, 2 HW 1 mr param, 3 HW with vector mr
		LIBORModelMonteCarloSimulationInterface simulationCalibrated = null;
		if(test == 0) {
			TimeDiscretizationInterface optionMaturityDiscretization = new TimeDiscretization(0.0, 0.25, 0.50, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 15.0, 20.0, 25.0, 30.0, 40.0);

			TimeDiscretizationInterface timeToMaturityDiscretization = new TimeDiscretization(0.00, 40, 1.0);
			ArrayList<Double> timeToMaturityList = timeToMaturityDiscretization.getAsArrayList();
			timeToMaturityList.add(0.25);
			timeToMaturityList.add(0.50);
			timeToMaturityList.add(0.75);
			timeToMaturityDiscretization = new TimeDiscretization(timeToMaturityList);
			//			optionMaturityDiscretization = timeToMaturityDiscretization;

			double[] volatilityFineInitial = { 0.00188142173350013, 0.00244713812905251, 0.00312935232764786, 0.00424272403585511, 0.00561176019992977, 0.00568485209131756, 0.00683434262426612, 0.00755557139079009, 0.00771211664332339, 0.00824545731419025, 0.00861010546271496, 0.00774567189155376, 0.00756151667905925, 0.00716622512847832, 0.00680188750589074, 0.00698276842259019, 0.00673562900064002, 0.00736245353917806, 0.00793386630005653, 0.00774736825344931, 0.00703201120614119, 0.00689689547976478, 0.00693214319886021, 0.00679403321903707, 0.00689875296307854, 0.00723724500604481, 0.00679067514222039, 0.00701609275468305, 0.00671237185635425, 0.0061010213268778, 0.00639090714283796, 0.00591785251069359, 0.00547572428092251, 0.00452818855618976, 0.0040916097994791, 0.00506326717558218, 0.00447523023657468, 0.00380562299280579, 0.0033997609886344, 0.00157369148981344, 0.004, 0.00264668487730639, 0.00338358438549667, 0.0042583392466523, 0.00374251014853167, 0.00609664367488931, 0.00527491641895377, 0.00668315097703198, 0.00658202701405219, 0.00709801065756526, 0.00838042553100674, 0.00776896234056017, 0.00763102774582922, 0.00717721971340214, 0.00630362543200968, 0.00599253151486988, 0.00514204782796695, 0.00578960682825584, 0.00675665772559523, 0.00603929424704131, 0.00616580840780932, 0.00678814177521793, 0.00695349322073888, 0.00664405284337943, 0.00612446829351134, 0.00666094465333253, 0.00646524846239828, 0.00678975705219538, 0.00620950886390075, 0.00603812195184095, 0.00635765268485236, 0.00648999847163647, 0.0065402845256706, 0.00666109353707837, 0.00546149067254712, 0.00617349066597743, 0.0053197806621577, 0.00499758197738068, 0.00456967178656529, 0.00549217394102514, 0.004, 0.00412412862348145, 0.00360997813643166, 0.00481676441508378, 0.00442173545912594, 0.00626435623508068, 0.00490748817476142, 0.00745873785986973, 0.00627030538387987, 0.00603995044927501, 0.00707050614556265, 0.00808277626556295, 0.00714374328360619, 0.00667678601812034, 0.00604972234695157, 0.00429429262103971, 0.00477842598113308, 0.00583478068977273, 0.00570395559364015, 0.0053526504964704, 0.00580800537932058, 0.00561348824545989, 0.00483235138677368, 0.00554873189039507, 0.00524526349971304, 0.005110275849157, 0.00563323921458802, 0.00431021537816501, 0.00612095274226146, 0.00607222630125266, 0.0070207867102145, 0.00750607561405071, 0.00755101553992851, 0.00694551280489971, 0.00640251046333924, 0.0056583463253938, 0.00548564944007219, 0.0059362809916561, 0.010022429086807, 0.004, 0.00616465524445924, 0.0060194741377253, 0.0051515868776049, 0.00657705473695597, 0.0068820531698432, 0.00609478020957857, 0.00595194833262245, 0.00773773013331189, 0.00620603067736448, 0.00860041000265634, 0.0103714955260017, 0.00711409936312723, 0.00708490397224151, 0.00588614545075859, 0.00476822741196506, 0.0049249797906214, 0.00543872065493878, 0.00500218620026466, 0.00515646681969159, 0.00598922898075584, 0.00525394949034156, 0.00577209171498963, 0.00551211081588153, 0.00560892381073576, 0.00539597721127869, 0.00331362471109036, 0.00565077664923615, 0.00563266715477447, 0.00604349161423452, 0.00677268092065568, 0.00689029823433145, 0.00652434453656715, 0.00601450362015644, 0.00531049433504766, 0.00523967548428067, 0.00547120426275253, -0.00169670883759053, 0.004, 0.00682404350456255, 0.00675930436370809, 0.00534471722873658, 0.00659228819184702, 0.00548138103279273, 0.00669369679725406, 0.00528976217365383, 0.00789897557589221, 0.00510347852791096, 0.00868517525213428, 0.00836176024146618, 0.00816808466527832, 0.00679595780220292, 0.00534104826948273, 0.00443401432258604, 0.00516725665277164, 0.00455550155687438, 0.00516694150274943, 0.00622634160268162, 0.00543749614961861, 0.00589445102409619, 0.00613008761449019, 0.00549987898889946, 0.00587770649323352, 0.00541341048287516, 0.00492256772868173, 0.00482451314574211, 0.0044311830979994, 0.00532354748417067, 0.00504800515131747, 0.00495170632665564, 0.00538150801291063, 0.00462242464575121, 0.00413514271461505, 0.0036373147797081, -0.00195378506178586, 0.004, -0.197222484469832, 0.00626696027150197, 0.00552676863465162, 0.00540402131929257, 0.0055417967222204, 0.00514070546415228, 0.00473197076473461, 0.00535460847223553, 0.00447658651597976, 0.00750782960285101, 0.00755712431211006, 0.00620198564717612, 0.00437692785834355, 0.0026357748104504, 0.00373127246514, 0.00338447072006536, 0.00249017660932901, 0.00219493792926755, 0.000886416064192735, 0.00396800934108589, 0.00400387567622326, 0.00279081254689441, 0.00188456892939609, 0.000122667470926923, 0.0043881238544917, 0.00462899765423492, 0.00442815274857422, 0.00439771012641959, 0.00424063086758808, 0.00476637224080197, 0.0054040801853329, 0.00496393960945804, 0.0049014659862974, 0.00510887865003792, 0.00753551511998958, 0.004, 0.00600089469060071, 0.00531519990199881, 0.00565204942888863, 0.00532763837356964, 0.00501618432994098, 0.00492594213392192, 0.00559698190755737, 0.00461084121390951, 0.00681530395874622, 0.0076402678613766, 0.00622318152889423, 0.00493293616530465, 0.00368919642570353, 0.00432588286239437, 0.00353563111213374, 0.00326407483908194, 0.00344121740951852, 0.0019183247127117, 0.00470411107841229, 0.00482331079413266, 0.00415499318796653, 0.00373015182732561, 0.00235117185785453, 0.00421434154159793, 0.00350061462351939, 0.00310990364933925, 0.00316824645365618, 0.00164459036802477, 0.00402934548454256, 0.00515060329163864, 0.00438002737635865, 0.00416314580951372, 0.00378943063610239, 0.0210491352392416, 0.004, 0.0189846773797999, -0.0183387615658739, 0.00665993807873783, 0.00678237644387676, 0.00718086739590955, 0.00640397688334268, 0.00595962385957377, 0.00556680089907536, 0.00672242882927451, 0.00902285941034415, 0.0103753585107828, 0.0107896503436788, 0.00684201688207598, 0.00637048591780432, 0.004468240509815, 0.00351797221719007, 0.00124556830714796, 0.00538759627970082, 0.00528812242408913, 0.00342125527486584, 0.00218778990631438, 0.000585577720059274, 0.0057460194737967, 0.00513802760025306, 0.00343810471566253, 0.0014634108780903, -0.00063745034512957, -0.000179179052024604, 0.00444580295869898, 0.00547673206293821, 0.00653463642612653, 0.00617449845701861, -0.00484997751055757, 0.004, -0.16074721850062, 0.00620069252881296, 0.00625409069119784, 0.00656773324939692, 0.00591364979644733, 0.00551369213534098, 0.0051444882388198, 0.00600822928084645, 0.00775163683222475, 0.00880026883541508, 0.00895358327866209, 0.00593365732425627, 0.00563877987306613, 0.00429460320962464, 0.00344505667319563, 0.00203132491797663, 0.00502377258935102, 0.00519852484055304, 0.00426039670971861, 0.003159274763909, 0.00166268433610453, 0.00480637759852153, 0.00475627411593264, 0.00398376147396027, 0.00335143798554606, 0.00258677483153201, 0.00328978459023786, 0.00504326371713696, 0.00489819846759245, 0.00521822159465031, 0.00557888888039177, 0.00629722566415151, 0.004, 0.0062833734704122, 0.00644470507858546, 0.00677102384340264, 0.00597102303978095, 0.00558771259164609, 0.00508664479437948, 0.00582094769855885, 0.00737041419321413, 0.00808416467957846, 0.00816626141716939, 0.00622836882535576, 0.00617125193933055, 0.0038559085062547, 0.00286618604519527, 0.000462565675257915, 0.00499053772244887, 0.00460598022590686, 0.00372410616688586, 0.00128611185357028, 0.000696031971736591, 0.00483247638684995, 0.00415721810015557, 0.00247348119475523, 0.00181055981341248, 0.000428488318382846, 0.000247486801972927, 0.00292640411764009, 0.00374379254037987, 0.00425529189405349, 0.0039979945685008, 0.00457482451733148, 0.004, -0.134018679711584, -0.115735115419455, -0.115735115429311, -0.115735115417353, 0.00524927086613273, 0.00515362952240186, 0.00568454708983756, 0.00558456624654833, 0.00580012291775871, 0.00711310698884615, 0.00734038661928209, 0.00731264299439564, 0.00817329850180583, 0.00811193595515245, 0.00585924752358954, 0.00459034964003944, 0.00301640838920548, 0.00184987288158387, -0.000448959187163014, 0.00443786408989724, 0.00509192693327725, 0.00384214906341748, 0.00272340779075718, 0.00137419149165576, 0.0031388343641706, 0.00466704471959889, 0.00425824608039059, 0.00440904153751598, 0.00421819782947236, -0.00178943907970017, 0.004, 0.153084893035999, 0.111042750063195, 0.111042750080336, 0.00541226654759576, 0.00547544261368078, 0.00644325063511948, 0.00617408696781758, 0.00684081118159238, 0.0069419713771668, 0.00796266189336458, 0.00764917054935741, 0.00907708544163661, 0.008904706308452, 0.00677972970217715, 0.005245960248911, 0.00512059472947709, 0.00411373459469543, 0.00461969677184784, 0.0053052435499631, 0.00392469423260614, 0.000867293441495013, -0.000541190467158872, -0.000986920823345379, -0.00410231216263519, -0.000792923384355021, 0.00233349997450476, 0.0033540283837796, 0.00179243369973643, -0.00816334109015774, 0.004, -0.112168648541389, -0.0895613553951765, 0.00600325143287847, 0.0058842850949162, 0.00681490159584115, 0.00689898197602102, 0.00755293051956682, 0.00697441633930242, 0.00752951884442625, 0.00762445652524131, 0.00839424109367274, 0.00866802093018094, 0.0068898635904766, 0.00469263172536492, 0.00429348891109959, 0.00182945269169529, 0.00104504973487811, 0.00295812554739881, 0.00364991222229353, 0.00302741080687442, -0.00159079490412379, -0.00225685225154633, 0.00330906136340458, 0.0084377551240854, 0.0037697170394465, 0.00208233772942516, 0.0022438525413249, -0.00598826353189619, 0.004, 0.246603313911467, 0.00542401424153042, 0.00524554752668135, 0.00570084493653443, 0.00577870314529835, 0.00586865219201785, 0.00793978389568166, 0.00750017453100902, 0.00801647322038517, 0.00897338789082769, 0.00936900472776055, 0.00615748005743816, 0.00469838091358023, 0.0042649944198027, 0.00267659237455928, 0.00181301733066789, 0.00371869131987452, 0.00324952673251296, 0.00181953519265541, 0.000598418250498434, -0.00143590257865953, -0.000128400559433996, 0.0027745601361867, 0.00266887333841286, 0.00237781436204573, 0.00129116441308689, -0.0025243521459801, 0.004, 0.00540549023399865, 0.00546978661454686, 0.00630124752396011, 0.00608465661329026, 0.0066574387447782, 0.00688944223516516, 0.00807957816141563, 0.00777489344575464, 0.00930437155701677, 0.0095460087839841, 0.0056898848792815, 0.00513225131858054, 0.00394186925927661, 0.00362493377994459, 0.00219555541659419, 0.00369630467052962, 0.00270790609552732, 0.000549795423912195, -0.00132825171584076, -0.00249191495490888, -0.000101332520740995, 0.00333736770596378, 0.00229765100537349, 0.00164964689076992, 0.00085092084954786, -0.00372252151682471, 0.004, 0.201007852692451, -0.0127342164007155, -0.0127342163964287, -0.0127342163966136, 0.00480297703961716, 0.00573781323392882, 0.0061650607689565, 0.00400640871061565, 0.00504719171171648, 0.00603680695947247, 0.00672995731699367, 0.00736580287569749, 0.00841279457366484, 0.00905406781519812, 0.00670865408580991, 0.00861546752796367, 0.00834669199892589, 0.00825016714366856, 0.00725021895446231, 0.0064058540903628, 0.00505764539825815, 0.00428083274448697, 0.00432890885383873, 0.00341859005372526, -0.0194555490576774, 0.004, -0.190550019534513, -0.125378578756109, -0.125378578755967, 0.00467736246770524, 0.00569910322355304, 0.00592169355255533, 0.00405921302507087, 0.00535912902861615, 0.00623649448105408, 0.00706776606626972, 0.00788294315400603, 0.00971364013456496, 0.0107059873490125, 0.00490058164749244, 0.00456262832755202, 0.00356385625235498, 0.0034565143717254, 0.00157092953372462, 0.00121431290639782, 0.00154952219648425, 0.00118467505499431, 0.0004131975586109, -0.000699902401586334, -0.0342377537367556, 0.004, -0.0182521519679822, 0.065411029816537, 0.00495233956297924, 0.00702967837488282, 0.00700122769135017, 0.00362812387972704, 0.00732296050684732, 0.00698320109299191, 0.0078162738794499, 0.00893125547333946, 0.0108753050239391, 0.0117424700700635, 0.00586920922616124, 0.00633121669246589, 0.00552407179093373, 0.00636915846674222, 0.00597243270062556, 0.00200181169415653, 0.00112777665513735, 0.00163189358790245, 0.001447666618704, -0.00221626832649058, -0.0230820701409544, 0.004, 0.0697882947721137, 0.0049339327383482, 0.00565025938245767, 0.00624535792667892, 0.00459955153813207, 0.00534910070156601, 0.00655393378910615, 0.00725893539893254, 0.00767871744229052, 0.00935753960863785, 0.0102754521550055, 0.00563840019175984, 0.00460575972942088, 0.0050309261892919, 0.00445387995544703, 0.00210388226432139, 0.00250613521266417, 0.00251836352313703, 0.00162373849874021, 0.000609526720925567, -0.00027691615199658, -0.00909954480102535, 0.004, 0.00510196438145562, 0.00586338249723067, 0.00649871981448966, 0.00452444934911543, 0.00533176743249264, 0.00689746840266128, 0.00793159703707077, 0.00862583621584827, 0.0104098894094926, 0.012214544788938, 0.00516175103227768, 0.0045297117911066, 0.00402604411367467, 0.00406299301565469, 0.00283047696527217, 0.0018274784749506, 0.00213956923838279, 0.00141068797272239, 0.000560250005339239, -0.00121541051051462, -0.0304438890138148, 0.004, -0.180639517363353, -0.141024318706879, -0.141024318705057, -0.141024318712856, 0.00437219872442167, 0.00609653039419425, 0.00542242887424487, 0.00440794230531482, 0.00251041422761071, 0.00507347871822824, 0.00589464359501231, 0.00601054860050314, 0.00794137984880719, 0.00900074461209995, 0.00546367378831234, 0.00445327054692074, 0.0046184963438731, 0.00534191918158842, 0.00557780601638256, 0.0117883747651444, 0.004, -0.134813842076166, -0.078766190032586, -0.0787661900294195, 0.00414139362313966, 0.00578914930389675, 0.00567830290024663, 0.00450171144891663, 0.00190411399806994, 0.0055550257259982, 0.00593667800733581, 0.00657229929647184, 0.00745053586068448, 0.00766592403082185, 0.00692484995260122, 0.00479284385326522, 0.00436343834355784, 0.00462807167679338, 0.00509816454730392, 0.0126167571816246, 0.004, 0.15167986186512, 0.220773786220517, 0.00387038781829683, 0.00608993715554102, 0.00580939426154654, 0.00470095382749162, 0.00245984453764531, 0.0135705578078898, 0.00819169973112957, 0.0102111211140499, 0.0117832726693842, 0.0126503297055948, 0.0116572599209275, 0.00806543436308163, 0.00659492630254821, 0.00703377814684929, 0.00794932203019307, 0.0156759603193197, 0.004, 0.0962458734903418, 0.00474413533313822, 0.00659863599033649, 0.00669999283975872, 0.00498304459854345, 0.00372575472786237, 0.00591552722486111, 0.00673741419050148, 0.00724821542024324, 0.0095534949807549, 0.011296915238539, 0.00728923843833547, 0.00472134646775808, 0.00478377199562191, 0.00560708945972712, 0.00594773448114153, 0.0192083621487655, 0.004, 0.00442575360436979, 0.00599223816435319, 0.00570035936021275, 0.00412814288485424, 0.00143901404790231, 0.00662607902713897, 0.00599335302367598, 0.00589533363322908, 0.00683315583733948, 0.00609123911153978, 0.00300029583721264, 0.00235068570122878, 0.00304630244693257, 0.00338238932005341, 0.00273036191288129, 0.00345991687648601, 0.004, 0.282046574748334, 0.115347935312723, 0.115347935337489, 0.115347935312131, 0.00348957742798341, 0.00471690797915702, 0.00480084757131838, 0.00285399051633338, 0.0028312300197789, 0.00355849937942815, 0.00407725073937473, 0.00447230921594607, 0.00525579725053393, 0.00638689910397696, 0.00934185729900714, 0.004, 0.00982418122121637, 1.71382898596364, 1.71382898643197, 0.00370387845848235, 0.00526379001894508, 0.00519277014799883, 0.00323130372656083, 0.00352631784303996, 0.00419357057707047, 0.00448057239787154, 0.00478343130654781, 0.00574879502088379, 0.00712223269913981, 0.0147749184740494, 0.004, 0.484004739614467, 0.141555755811279, 0.00355786334851272, 0.00512141868669868, 0.00494770222580906, 0.00267247383184296, 0.00255391713134704, 0.00412539591162217, 0.0045242000604618, 0.00464386743627561, 0.00548274312946107, 0.00686371250421262, 0.0299857381881637, 0.004, -0.319428915895427, 0.00251783886548631, 0.00504093386325881, 0.0038481117246798, 0.00207952110680643, 0.0023105342871514, 0.00243253400392396, 0.00333824010070498, 0.00401865956915386, 0.00492592615107898, 0.00581416101019428, 0.0174477454594243, 0.004, 0.00334412396718098, 0.00530232529771076, 0.00580760365537931, 0.00337226579345837, 0.00343647572772882, 0.00417033556540962, 0.00450290123623779, 0.00491957325897116, 0.00601023235622087, 0.00790855522847006, 0.0142496665914079, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004 };
			//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelTimeHomogenousPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 0.50, 1.00, 2.00, 30.00 ), new double[] { 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0 });
			//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelTimeHomogenousPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 ), new double[] { 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0 });
			//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 1.00, 2.00, 3.00, 4.00, 5.00, 6.00, 7.00, 8.00, 9.00, 10.00, 11.00, 12.00, 14.00, 15.00, 16.00, 17.00, 18.50, 20.00, 22.50, 25.00, 30.00 ), new TimeDiscretization(0.00, 1.00, 2.00, 3.00, 4.00, 5.00, 6.00, 7.00, 8.00, 9.00, 10.00, 11.00, 12.00, 14.00, 15.00, 16.00, 17.00, 18.50, 20.00, 22.50, 25.00, 30.00 ), 0.40 / 100);
			LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, optionMaturityDiscretization, timeToMaturityDiscretization, 0.40 / 100, true);
			//			LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 30, 1.0), new TimeDiscretization(0.00, 40, 1.0), volatilityFineInitial);
			LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, 0.05, false);
			// Create a covariance model
			AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretization, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20/100.0, 0.05/100.0, 0.10, 0.05/100.0, 0.10} );
			covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization, liborPeriodDiscretization, volatilityModel, correlationModel);

			TimeDiscretizationInterface tenorTimeScalingDiscretization = new TimeDiscretization(0.0, 40.0, 0.25, ShortPeriodLocation.SHORT_PERIOD_AT_START);
			double[] tenorTimeScalings = new double[tenorTimeScalingDiscretization.getNumberOfTimes()];
			Arrays.fill(tenorTimeScalings, 0.0);
			TermStructureTenorTimeScalingInterface tenorTimeScalingModel = new TermStructureTenorTimeScalingPicewiseConstant(tenorTimeScalingDiscretization, tenorTimeScalings);

			// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
			AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModelParametric, 0.0, false);

			TermStructureCovarianceModelParametric termStructureCovarianceModel;
			termStructureCovarianceModel = new TermStructCovarianceModelFromLIBORCovarianceModelParametric(tenorTimeScalingModel, covarianceModelParametric);

			double[] bestParameters = null;
			for(int i = 0; i<1; i++) {
				if(i>0) termStructureCovarianceModel = termStructureCovarianceModel.getCloneWithModifiedParameters(bestParameters);


				// Set model properties
				Map<String, Object> properties = new HashMap<String, Object>();

				Double accuracy = new Double(1E-9);
				int maxIterations = 400;
				int numberOfThreads = 5;
				OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

				double[] parameterStandardDeviation = new double[termStructureCovarianceModel.getParameter().length];
				double[] parameterLowerBound = new double[termStructureCovarianceModel.getParameter().length];
				double[] parameterUpperBound = new double[termStructureCovarianceModel.getParameter().length];
				Arrays.fill(parameterStandardDeviation, i==0 ? 0.20/100.0 : 0.2/100.0);
				Arrays.fill(parameterLowerBound, Double.NEGATIVE_INFINITY);
				Arrays.fill(parameterUpperBound, Double.POSITIVE_INFINITY);

				// optimizerFactory = new OptimizerFactoryCMAES(accuracy, maxIterations, parameterLowerBound, parameterUpperBound, parameterStandardDeviation);

				// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
				Map<String, Object> calibrationParameters = new HashMap<String, Object>();
				calibrationParameters.put("accuracy", accuracy);
				calibrationParameters.put("brownianMotion", brownianMotion);
				calibrationParameters.put("parameterStep", i == 0 ? new Double(1E-5) : new Double(5E-5) );
				calibrationParameters.put("optimizerFactory", optimizerFactory);
				if(i==0) calibrationParameters.put("brownianMotion", new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */));
				if(i==1) calibrationParameters.put("brownianMotion", new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */));
				if(i==2) calibrationParameters.put("brownianMotion", new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */));
				properties.put("calibrationParameters", calibrationParameters);


				System.out.println("Numerber of covariance parameters: " + termStructureCovarianceModel.getParameter().length);

				/*
				 * Create corresponding LIBOR Market Model
				 */
				TimeDiscretizationInterface liborPeriodDiscretizationFine = new TimeDiscretization(0.0, 40.0, 0.0625, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				TimeDiscretizationInterface liborPeriodDiscretizationMedium = new TimeDiscretization(0.0, 40.0, 0.25, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				TimeDiscretizationInterface liborPeriodDiscretizationCoarse = new TimeDiscretization(0.0, 40.0, 1.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				TimeDiscretizationInterface liborPeriodDiscretizationCoarse2 = new TimeDiscretization(0.0, 40.0, 5.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				TimeDiscretizationInterface liborPeriodDiscretizationNormal = new TimeDiscretization(0.0, 40.0, 1.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				TermStructureModelInterface liborMarketModelCalibrated = new LIBORMarketModelWithTenorRefinement(
						//					new TimeDiscretizationInterface[] { liborPeriodDiscretizationNormal },
						//					new Integer[] { 200 },
						new TimeDiscretizationInterface[] { liborPeriodDiscretizationMedium, liborPeriodDiscretizationCoarse, liborPeriodDiscretizationCoarse2 },
						new Integer[] { 4, 9, 200 },
						//					new TimeDiscretizationInterface[] { liborPeriodDiscretizationCoarse, liborPeriodDiscretizationCoarse2 },
						//					new Integer[] { 5, 200 },
						//					new TimeDiscretizationInterface[] { liborPeriodDiscretizationFine, liborPeriodDiscretizationMedium, liborPeriodDiscretizationCoarse, liborPeriodDiscretizationCoarse2 },
						//					new Integer[] { 4, 3, 9, 200 },
						curveModel,
						forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve),
						termStructureCovarianceModel,
						calibrationItems.toArray(new CalibrationItem[0]), properties);

				System.out.println("\nCalibrated parameters are:");
				double[] param = ((TermStructureCovarianceModelParametric)((LIBORMarketModelWithTenorRefinement) liborMarketModelCalibrated).getCovarianceModel()).getParameter();
				//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
				for (double p : param) System.out.println(p);
				bestParameters = param;

				ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
				simulationCalibrated = new TermStructureModelMonteCarloSimulation(liborMarketModelCalibrated, process);
			}
		}
		if(test == 1) {
			LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 30, 1.0), new TimeDiscretization(0.00, 40, 1.0), 0.40 / 100);
			LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, 0.05, false);
			// Create a covariance model
			AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretization, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20/100.0, 0.05/100.0, 0.10, 0.05/100.0, 0.10} );
			covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization, liborPeriodDiscretization, volatilityModel, correlationModel);

			// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").			
			AbstractLIBORCovarianceModelParametric covarianceModelDisplaced = new DisplacedLocalVolatilityModel(covarianceModelParametric, 1.0/0.25, false /* isCalibrateable */);

			// Set model properties
			Map<String, Object> properties = new HashMap<String, Object>();

			Double accuracy = new Double(1E-8);
			int maxIterations = 400;
			int numberOfThreads = 2;
			OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

			double[] parameterStandardDeviation = new double[covarianceModelParametric.getParameter().length];
			double[] parameterLowerBound = new double[covarianceModelParametric.getParameter().length];
			double[] parameterUpperBound = new double[covarianceModelParametric.getParameter().length];
			Arrays.fill(parameterStandardDeviation, 0.20/100.0);
			Arrays.fill(parameterLowerBound, Double.NEGATIVE_INFINITY);
			Arrays.fill(parameterUpperBound, Double.POSITIVE_INFINITY);

			//optimizerFactory = new OptimizerFactoryCMAES(accuracy, maxIterations, parameterLowerBound, parameterUpperBound, parameterStandardDeviation);

			// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
			Map<String, Object> calibrationParameters = new HashMap<String, Object>();
			calibrationParameters.put("accuracy", accuracy);
			calibrationParameters.put("brownianMotion", brownianMotion);
			calibrationParameters.put("optimizerFactory", optimizerFactory);
			calibrationParameters.put("brownianMotion", new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */));
			properties.put("calibrationParameters", calibrationParameters);

			/*
			 * Create corresponding LIBOR Market Model
			 */
			LIBORMarketModel.CalibrationItem[] calibrationItemsLMM = new LIBORMarketModel.CalibrationItem[calibrationItemNames.size()];
			for(int i=0; i<calibrationItemNames.size(); i++) calibrationItemsLMM[i] = new LIBORMarketModel.CalibrationItem(calibrationItems.get(i).calibrationProduct,calibrationItems.get(i).calibrationTargetValue,calibrationItems.get(i).calibrationWeight);
			TermStructureModelInterface liborMarketModelCalibrated = new LIBORMarketModel(
					liborPeriodDiscretization,
					curveModel,
					forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve),
					covarianceModelDisplaced,
					calibrationItemsLMM,
					properties);

			System.out.println("\nCalibrated parameters are:");
			double[] param = ((AbstractLIBORCovarianceModelParametric)((LIBORMarketModel) liborMarketModelCalibrated).getCovarianceModel()).getParameter();
			//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
			for (double p : param) System.out.println(p);

			ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
			simulationCalibrated = new TermStructureModelMonteCarloSimulation(liborMarketModelCalibrated, process);
		}
		else if(test == 2) {
			final TimeDiscretizationInterface shortRateVolTimeDis = new TimeDiscretization(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00, 35.00, 40.00, 45.00 );
			double[] shortRateVolatility = new double[shortRateVolTimeDis.getNumberOfTimes()];
			double[] meanReversion = new double[shortRateVolTimeDis.getNumberOfTimes()];
			Arrays.fill(shortRateVolatility, 0.20 / 100);
			Arrays.fill(meanReversion, 0.15);			

			double[] initialParameters = new double[shortRateVolatility.length+1];
			System.arraycopy(shortRateVolatility, 0, initialParameters, 0, shortRateVolatility.length);
			initialParameters[initialParameters.length-1] = 0.15;

			int maxIterations = 400;
			double accuracy		= 1E-7;

			final double[] calibrationTargetValues = new double[calibrationItems.size()];
			for(int i=0; i<calibrationTargetValues.length; i++) calibrationTargetValues[i] = calibrationItems.get(i).calibrationTargetValue;

			final double[] calibrationWeights = new double[calibrationItems.size()];
			for(int i=0; i<calibrationWeights.length; i++) calibrationWeights[i] = calibrationItems.get(i).calibrationWeight;

			int numberOfThreadsForProductValuation = 2 * Math.min(2, Runtime.getRuntime().availableProcessors());
			final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

			/*
			 * We allow for 5 simultaneous calibration models.
			 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
			 * one model with 5 times the number of paths. In the case of an analytic calibration
			 * memory requirement is not the limiting factor.
			 */
			int numberOfThreads = 5;			
			LevenbergMarquardt optimizer = new LevenbergMarquardt(initialParameters, calibrationTargetValues, maxIterations, numberOfThreads)
			{
				// Calculate model values for given parameters
				@Override
				public void setValues(double[] parameters, double[] values) throws SolverException {

					double[] shortRateVolatility = new double[parameters.length-1];
					double[] meanReversion = new double[parameters.length-1];
					System.arraycopy(parameters, 0, shortRateVolatility, 0, shortRateVolatility.length);
					Arrays.fill(meanReversion, parameters[parameters.length-1]);

					ShortRateVolailityModelInterface volatilityModel = new ShortRateVolatilityModel(
							shortRateVolTimeDis,
							shortRateVolatility,
							meanReversion);

					// Create a LIBOR market model with the new covariance structure.
					LIBORModelInterface model = new HullWhiteModel(
							liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModel, null);
					ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
					final LIBORModelMonteCarloSimulation liborMarketModelMonteCarloSimulation =  new LIBORModelMonteCarloSimulation(model, process);

					ArrayList<Future<Double>> valueFutures = new ArrayList<Future<Double>>(calibrationItems.size());
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationItems.size(); calibrationProductIndex++) {
						final int workerCalibrationProductIndex = calibrationProductIndex;
						Callable<Double> worker = new  Callable<Double>() {
							public Double call() throws SolverException {
								try {
									return calibrationItems.get(workerCalibrationProductIndex).calibrationProduct.getValue(liborMarketModelMonteCarloSimulation);
								} catch (CalculationException e) {
									// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
									return new Double(calibrationItems.get(workerCalibrationProductIndex).calibrationTargetValue);
								} catch (Exception e) {
									// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
									return new Double(calibrationItems.get(workerCalibrationProductIndex).calibrationTargetValue);
								}
							}
						};
						if(executor != null) {
							Future<Double> valueFuture = executor.submit(worker);
							valueFutures.add(calibrationProductIndex, valueFuture);
						}
						else {
							FutureTask<Double> valueFutureTask = new FutureTask<Double>(worker);
							valueFutureTask.run();
							valueFutures.add(calibrationProductIndex, valueFutureTask);
						}
					}
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationItems.size(); calibrationProductIndex++) {
						try {
							double value = valueFutures.get(calibrationProductIndex).get();
							values[calibrationProductIndex] = value;
						}
						catch (InterruptedException e) {
							throw new SolverException(e);
						} catch (ExecutionException e) {
							throw new SolverException(e);
						}
					}
					System.out.println(this.getRootMeanSquaredError() + "\t" + Arrays.toString(values));
				}
			};

			// Set solver parameters
			optimizer.setWeights(calibrationWeights);
			optimizer.setErrorTolerance(accuracy);
			double[] parameterSteps = new double[initialParameters.length];
			Arrays.fill(parameterSteps, 1E-4);
			optimizer.setParameterSteps(parameterSteps);

			try {
				optimizer.run();
			}
			catch(SolverException e) {
				throw new CalculationException(e);
			}
			finally {
				if(executor != null) {
					executor.shutdown();
				}
			}

			// Get covariance model corresponding to the best parameter set.
			double[] bestParameters = optimizer.getBestFitParameters();

			System.out.println("\nCalibrated parameters are:");
			for (double p : bestParameters) System.out.println(formatterParam.format(p));

			double[] shortRateVolatilityCalib = new double[bestParameters.length-1];
			double[] meanReversionCalib = new double[bestParameters.length-1];
			System.arraycopy(bestParameters, 0, shortRateVolatilityCalib, 0, shortRateVolatilityCalib.length);
			Arrays.fill(meanReversionCalib, bestParameters[bestParameters.length-1]);

			ShortRateVolailityModelInterface volatilityModelCalibrated = new ShortRateVolatilityModel(
					shortRateVolTimeDis,
					shortRateVolatilityCalib,
					meanReversionCalib);

			// Create a LIBOR market model with the new covariance structure.
			LIBORModelInterface modelCalibrated = new HullWhiteModel(
					liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModelCalibrated, null);
			ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
			simulationCalibrated =  new LIBORModelMonteCarloSimulation(modelCalibrated, process);
		}
		else if(test == 3) {
			final TimeDiscretizationInterface shortRateVolTimeDis = new TimeDiscretization(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00, 35.00, 40.00, 45.00 );
			double[] shortRateVolatility = new double[shortRateVolTimeDis.getNumberOfTimes()];
			double[] meanReversion = new double[shortRateVolTimeDis.getNumberOfTimes()];
			Arrays.fill(shortRateVolatility, 0.40 / 100);
			Arrays.fill(meanReversion, 0.15);			

			double[] initialParameters = new double[2*shortRateVolatility.length];
			System.arraycopy(shortRateVolatility, 0, initialParameters, 0, shortRateVolatility.length);
			System.arraycopy(meanReversion, 0, initialParameters, shortRateVolatility.length, shortRateVolatility.length);

			int maxIterations = 400;
			double accuracy		= 1E-7;

			final double[] calibrationTargetValues = new double[calibrationItems.size()];
			for(int i=0; i<calibrationTargetValues.length; i++) calibrationTargetValues[i] = calibrationItems.get(i).calibrationTargetValue;

			final double[] calibrationWeights = new double[calibrationItems.size()];
			for(int i=0; i<calibrationWeights.length; i++) calibrationWeights[i] = calibrationItems.get(i).calibrationWeight;

			int numberOfThreadsForProductValuation = 2 * Math.min(2, Runtime.getRuntime().availableProcessors());
			final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

			/*
			 * We allow for 5 simultaneous calibration models.
			 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
			 * one model with 5 times the number of paths. In the case of an analytic calibration
			 * memory requirement is not the limiting factor.
			 */
			int numberOfThreads = 5;			
			LevenbergMarquardt optimizer = new LevenbergMarquardt(initialParameters, calibrationTargetValues, maxIterations, numberOfThreads)
			{
				// Calculate model values for given parameters
				@Override
				public void setValues(double[] parameters, double[] values) throws SolverException {

					double[] shortRateVolatility = new double[parameters.length/2];
					double[] meanReversion = new double[parameters.length/2];
					System.arraycopy(parameters, 0, shortRateVolatility, 0, shortRateVolatility.length);
					System.arraycopy(parameters, shortRateVolatility.length, meanReversion, 0, shortRateVolatility.length);

					ShortRateVolailityModelInterface volatilityModel = new ShortRateVolatilityModel(
							shortRateVolTimeDis,
							shortRateVolatility,
							meanReversion);

					// Create a LIBOR market model with the new covariance structure.
					LIBORModelInterface model = new HullWhiteModel(
							liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModel, null);
					ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
					final LIBORModelMonteCarloSimulation liborMarketModelMonteCarloSimulation =  new LIBORModelMonteCarloSimulation(model, process);

					ArrayList<Future<Double>> valueFutures = new ArrayList<Future<Double>>(calibrationItems.size());
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationItems.size(); calibrationProductIndex++) {
						final int workerCalibrationProductIndex = calibrationProductIndex;
						Callable<Double> worker = new  Callable<Double>() {
							public Double call() throws SolverException {
								try {
									return  calibrationItems.get(workerCalibrationProductIndex).calibrationProduct.getValue(liborMarketModelMonteCarloSimulation);
								} catch (CalculationException e) {
									//									e.printStackTrace();
									// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
									return calibrationTargetValues[workerCalibrationProductIndex];
								} catch (Exception e) {
									//									e.printStackTrace();
									// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
									return calibrationTargetValues[workerCalibrationProductIndex];
								}
							}
						};
						if(executor != null) {
							Future<Double> valueFuture = executor.submit(worker);
							valueFutures.add(calibrationProductIndex, valueFuture);
						}
						else {
							FutureTask<Double> valueFutureTask = new FutureTask<Double>(worker);
							valueFutureTask.run();
							valueFutures.add(calibrationProductIndex, valueFutureTask);
						}
					}
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationItems.size(); calibrationProductIndex++) {
						try {
							double value = valueFutures.get(calibrationProductIndex).get();
							values[calibrationProductIndex] = value;
						}
						catch (InterruptedException e) {
							throw new SolverException(e);
						} catch (ExecutionException e) {
							throw new SolverException(e);
						}
					}
					System.out.println(this.getRootMeanSquaredError() + "\t" + Arrays.toString(values));
				}
			};

			// Set solver parameters
			optimizer.setWeights(calibrationWeights);
			optimizer.setErrorTolerance(accuracy);
			double[] parameterSteps = new double[initialParameters.length];
			Arrays.fill(parameterSteps, 1E-4);
			optimizer.setParameterSteps(parameterSteps);

			try {
				optimizer.run();
			}
			catch(SolverException e) {
				throw new CalculationException(e);
			}
			finally {
				if(executor != null) {
					executor.shutdown();
				}
			}

			// Get covariance model corresponding to the best parameter set.
			double[] bestParameters = optimizer.getBestFitParameters();

			System.out.println("\nCalibrated parameters are:");
			for (double p : bestParameters) System.out.println(formatterParam.format(p));

			double[] shortRateVolatilityCalib = new double[bestParameters.length/2];
			double[] meanReversionCalib = new double[bestParameters.length/2];
			System.arraycopy(bestParameters, 0, shortRateVolatilityCalib, 0, shortRateVolatilityCalib.length);
			System.arraycopy(bestParameters, shortRateVolatilityCalib.length, meanReversionCalib, 0, shortRateVolatilityCalib.length);

			ShortRateVolailityModelInterface volatilityModelCalibrated = new ShortRateVolatilityModel(
					shortRateVolTimeDis,
					shortRateVolatilityCalib,
					meanReversionCalib);

			// Create a LIBOR market model with the new covariance structure.
			LIBORModelInterface modelCalibrated = new HullWhiteModel(
					liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModelCalibrated, null);
			ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
			simulationCalibrated =  new LIBORModelMonteCarloSimulation(modelCalibrated, process);
		}


		System.out.println("\nValuation on calibrated model:");
		double deviationSum			= 0.0;
		double deviationSquaredSum	= 0.0;
		for (int i = 0; i < calibrationItems.size(); i++) {
			AbstractLIBORMonteCarloProduct calibrationProduct = calibrationItems.get(i).calibrationProduct;
			try {
				double valueModel = calibrationProduct.getValue(simulationCalibrated);
				double valueTarget = calibrationItems.get(i).calibrationTargetValue;
				double error = valueModel-valueTarget;
				deviationSum += error;
				deviationSquaredSum += error*error;
				System.out.println(calibrationItemNames.get(i) + "\t" + "Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget));// + "\t" + calibrationProduct.toString());
			}
			catch(Exception e) {
			}
		}
		double averageDeviation = deviationSum/calibrationItems.size();
		System.out.println("Mean Deviation:" + formatterValue.format(averageDeviation));
		System.out.println("RMS Error.....:" + formatterValue.format(Math.sqrt(deviationSquaredSum/calibrationItems.size())));
		System.out.println("__________________________________________________________________________________________\n");

		Assert.assertTrue(Math.abs(averageDeviation) < 1E-2);
	}

	public AnalyticModelInterface getCalibratedCurve() throws SolverException {
		final String[] maturity					= { "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y", "35Y", "40Y", "45Y", "50Y" };
		final String[] frequency				= { "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual" };
		final String[] frequencyFloat			= { "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual" };
		final String[] daycountConventions		= { "ACT/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360" };
		final String[] daycountConventionsFloat	= { "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360" };
		final double[] rates					= { -0.00216 ,-0.00208 ,-0.00222 ,-0.00216 ,-0.0019 ,-0.0014 ,-0.00072 ,0.00011 ,0.00103 ,0.00196 ,0.00285 ,0.00367 ,0.0044 ,0.00604 ,0.00733 ,0.00767 ,0.00773 ,0.00765 ,0.00752 ,0.007138 ,0.007 };

		HashMap<String, Object> parameters = new HashMap<String, Object>();

		parameters.put("referenceDate", new LocalDate(2016, DateTimeConstants.SEPTEMBER, 30)); 
		parameters.put("currency", "EUR");
		parameters.put("forwardCurveTenor", "6M");
		parameters.put("maturities", maturity);
		parameters.put("fixLegFrequencies", frequency);
		parameters.put("floatLegFrequencies", frequencyFloat);
		parameters.put("fixLegDaycountConventions", daycountConventions);
		parameters.put("floatLegDaycountConventions", daycountConventionsFloat);
		parameters.put("rates", rates);

		return getCalibratedCurve(null, parameters);
	}

	private static AnalyticModelInterface getCalibratedCurve(AnalyticModelInterface model2, Map<String, Object> parameters) throws SolverException {

		final LocalDate	referenceDate		= (LocalDate) parameters.get("referenceDate");
		final String	currency			= (String) parameters.get("currency");
		final String	forwardCurveTenor	= (String) parameters.get("forwardCurveTenor");
		final String[]	maturities			= (String[]) parameters.get("maturities");
		final String[]	frequency			= (String[]) parameters.get("fixLegFrequencies");
		final String[]	frequencyFloat		= (String[]) parameters.get("floatLegFrequencies");
		final String[]	daycountConventions	= (String[]) parameters.get("fixLegDaycountConventions");
		final String[]	daycountConventionsFloat	= (String[]) parameters.get("floatLegDaycountConventions");
		final double[]	rates						= (double[]) parameters.get("rates");

		Assert.assertEquals(maturities.length, frequency.length);
		Assert.assertEquals(maturities.length, daycountConventions.length);
		Assert.assertEquals(maturities.length, rates.length);

		Assert.assertEquals(frequency.length, frequencyFloat.length);
		Assert.assertEquals(daycountConventions.length, daycountConventionsFloat.length);

		int		spotOffsetDays = 2;
		String	forwardStartPeriod = "0D";

		String curveNameDiscount = "discountCurve-" + currency;

		/*
		 * We create a forward curve by referencing the same discount curve, since
		 * this is a single curve setup.
		 * 
		 * Note that using an independent NSS forward curve with its own NSS parameters
		 * would result in a problem where both, the forward curve and the discount curve
		 * have free parameters.
		 */
		ForwardCurveInterface forwardCurve		= new ForwardCurveFromDiscountCurve(curveNameDiscount, referenceDate, forwardCurveTenor);

		// Create a collection of objective functions (calibration products)
		Vector<AnalyticProductInterface> calibrationProducts = new Vector<AnalyticProductInterface>();
		double[] curveMaturities	= new double[rates.length+1];
		double[] curveValue			= new double[rates.length+1];
		boolean[] curveIsParameter	= new boolean[rates.length+1];
		curveMaturities[0] = 0.0;
		curveValue[0] = 1.0;
		curveIsParameter[0] = false;
		for(int i=0; i<rates.length; i++) {

			ScheduleInterface schedulePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequency[i], daycountConventions[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
			ScheduleInterface scheduleRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequencyFloat[i], daycountConventionsFloat[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

			curveMaturities[i+1] = Math.max(schedulePay.getPayment(schedulePay.getNumberOfPeriods()-1),scheduleRec.getPayment(scheduleRec.getNumberOfPeriods()-1));
			curveValue[i+1] = 1.0;
			curveIsParameter[i+1] = true;
			calibrationProducts.add(new Swap(schedulePay, null, rates[i], curveNameDiscount, scheduleRec, forwardCurve.getName(), 0.0, curveNameDiscount));
		}

		InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;

		// Create a discount curve
		DiscountCurve			discountCurve					= DiscountCurve.createDiscountCurveFromDiscountFactors(
				curveNameDiscount								/* name */,
				curveMaturities	/* maturities */,
				curveValue		/* discount factors */,
				curveIsParameter,
				interpolationMethod ,
				ExtrapolationMethod.CONSTANT,
				InterpolationEntity.LOG_OF_VALUE
				);

		/*
		 * Model consists of the two curves, but only one of them provides free parameters.
		 */
		AnalyticModelInterface model = new AnalyticModel(new CurveInterface[] { discountCurve, forwardCurve });

		/*
		 * Create a collection of curves to calibrate
		 */
		Set<ParameterObjectInterface> curvesToCalibrate = new HashSet<ParameterObjectInterface>();
		curvesToCalibrate.add(discountCurve);

		/*
		 * Calibrate the curve
		 */
		Solver solver = new Solver(model, calibrationProducts);
		AnalyticModelInterface calibratedModel = solver.getCalibratedModel(curvesToCalibrate);
		System.out.println("Solver reported acccurary....: " + solver.getAccuracy());

		Assert.assertEquals("Calibration accurarcy", 0.0, solver.getAccuracy(), 1E-3);

		// Get best parameters
		double[] parametersBest = calibratedModel.getDiscountCurve(discountCurve.getName()).getParameter();

		// Test calibration
		model			= calibratedModel;

		double squaredErrorSum = 0.0;
		for(AnalyticProductInterface c : calibrationProducts) {
			double value = c.getValue(0.0, model);
			double valueTaget = 0.0;
			double error = value - valueTaget;
			squaredErrorSum += error*error;
		}
		double rms = Math.sqrt(squaredErrorSum/calibrationProducts.size());

		System.out.println("Independent checked acccurary: " + rms);

		System.out.println("Calibrated discount curve: ");
		for(int i=0; i<curveMaturities.length; i++) {
			double maturity = curveMaturities[i];
			System.out.println(maturity + "\t" + calibratedModel.getDiscountCurve(discountCurve.getName()).getDiscountFactor(maturity));
		}
		return model;
	}

	private static double getParSwaprate(ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve, double[] swapTenor) throws CalculationException {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretization(swapTenor), new TimeDiscretization(swapTenor), forwardCurve, discountCurve);
	}
}