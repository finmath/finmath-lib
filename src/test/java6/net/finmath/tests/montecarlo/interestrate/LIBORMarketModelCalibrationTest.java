/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 16.01.2015
 */
package net.finmath.tests.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

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
import net.finmath.montecarlo.BrownianMotionView;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModel.CalibrationItem;
import net.finmath.montecarlo.interestrate.LIBORModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.modelplugins.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.DisplacedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelStochasticVolatility;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelPiecewiseConstant;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.optimizer.OptimizerFactoryInterface;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * This class tests the LIBOR market model and products.
 * 
 * @author Christian Fries
 */
public class LIBORMarketModelCalibrationTest {

	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterParam		= new DecimalFormat(" #0.000;-#0.000", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

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

	@Test
	public void testSwaptionSmileCalibration() throws CalculationException {

		final int numberOfPaths		= 5000;
		final int numberOfFactors	= 5;

		/*
		 * Calibration test
		 */
		System.out.println("Calibration to Swaption Smile Products.");

		/*
		 * Calibration of curves
		 */
		System.out.println("Calibration of rate curves:");
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

			calibrationItems.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, "VOLATILITYLOGNORMAL", forwardCurve, discountCurve));
		}


		double[] atmOptionMaturities	= { 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 };
		double[] atmOptionVolatilities	= { 0.385, 0.351, 0.325, 0.308, 0.288, 0.279, 0.290, 0.272, 0.235, 0.192 };

		for(int i=0; i<atmOptionMaturities.length; i++ ) {

			double	exerciseDate		= atmOptionMaturities[i];
			double	moneyness			= 0.0;
			double	targetVolatility	= atmOptionVolatilities[i];

			calibrationItems.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, "VOLATILITYLOGNORMAL", forwardCurve, discountCurve));
		}

		/*
		 * Create a LIBOR Market Model
		 */

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborRateTimeHorzion	= 20.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 20.0;
		double dt		= 0.5;
		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create Brownian motions 
		 */
		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors + 1, numberOfPaths, 31415 /* seed */);
		BrownianMotionInterface brownianMotionView1 = new BrownianMotionView(brownianMotion, new Integer[] { 0, 1, 2, 3, 4 });
		BrownianMotionInterface brownianMotionView2 = new BrownianMotionView(brownianMotion, new Integer[] { 0, 5 });

		// Create a covariance model
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretization, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20, 0.05, 0.10, 0.05, 0.10} );
		// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
		AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModelParametric, 0.0, false);
		// Create stochastic scaling (pass brownianMotionView2 to it)
		AbstractLIBORCovarianceModelParametric covarianceModelStochasticParametric = new LIBORCovarianceModelStochasticVolatility(covarianceModelBlended, brownianMotionView2, 0.01, -0.30, true);

		// Set model properties
		Map<String, Object> properties = new HashMap<String, Object>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

		// Choose normal state space for the Euler scheme (the covariance model above carries a linear local volatility model, such that the resulting model is log-normal).
		properties.put("stateSpace", LIBORMarketModel.StateSpace.NORMAL.name());

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
		Map<String, Object> calibrationParameters = new HashMap<String, Object>();
		// The brownianMotion to be used - if a full Monte-Carlo valuation is necessary.
		calibrationParameters.put("brownianMotion", brownianMotionView1);
		// The step size vector used to calculate first derivatives via finite differences
		calibrationParameters.put("parameterStep", new Double(1E-5));

		/*
		 * The optimizer to use and some of its parameters
		 */

		// The accuracy of the slower. The solver steps if the value does not improve more thatn the given parameter.
		Double accuracy = new Double(1E-8);
		int maxIterations = 100;
		int numberOfThreads = 2;		// two concurrent models
		OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);
		calibrationParameters.put("optimizerFactory", optimizerFactory);

		// Pass the calibrationParameters to the model.
		properties.put("calibrationParameters", calibrationParameters);

		LIBORMarketModel liborMarketModelCalibrated = new LIBORMarketModel(
				liborPeriodDiscretization,
				forwardCurve, discountCurve, covarianceModelStochasticParametric, calibrationItems.toArray(new CalibrationItem[0]), properties);	


		/*
		 * Test our calibration
		 */
		System.out.println("\nCalibrated parameters are:");
		double[] param = ((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).getParameter();
		//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
		for (double p : param) System.out.println(formatterParam.format(p));

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotionView1);
		net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation simulationCalibrated = new net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation(
				liborMarketModelCalibrated, process);

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
				//
			}
		}
		double averageDeviation = deviationSum/calibrationItems.size();
		System.out.println("Mean Deviation:" + formatterValue.format(averageDeviation));
		System.out.println("RMS Error.....:" + formatterValue.format(Math.sqrt(deviationSquaredSum/calibrationItems.size())));
		System.out.println("__________________________________________________________________________________________\n");

		Assert.assertTrue(Math.abs(averageDeviation) < 1E-2);
	}

	/**
	 * Brute force Monte-Carlo calibration of swaptions.
	 * 
	 * @throws CalculationException
	 * @throws SolverException
	 */
	@Test
	public void testATMSwaptionCalibration() throws CalculationException, SolverException {

		final int numberOfPaths		= 1000;
		final int numberOfFactors	= 1;
		
		long millisCurvesStart = System.currentTimeMillis();
		
		/*
		 * Calibration test
		 */
		System.out.println("Calibration to Swaptions.\n");

		/*
		 * Calibration of rate curves
		 */
		System.out.println("Calibration of rate curves:");

		final AnalyticModelInterface curveModel = getCalibratedCurve();

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterface forwardCurve = curveModel.getForwardCurve("ForwardCurveFromDiscountCurve(discountCurve-EUR,6M)");
		final DiscountCurveInterface discountCurve = curveModel.getDiscountCurve("discountCurve-EUR");
//		curveModel.addCurve(discountCurve.getName(), discountCurve);
		
		long millisCurvesEnd = System.currentTimeMillis();
		System.out.println("");

		/*
		 * Calibration of model volatilities
		 */
		System.out.println("Brute force Monte-Carlo calibration of model volatilities:");

		/*
		 * Create a set of calibration products.
		 */
		ArrayList<String>					calibrationItemNames	= new ArrayList<String>();
		final ArrayList<CalibrationItem>	calibrationItems		= new ArrayList<CalibrationItem>();

		double	swapPeriodLength	= 0.5;

		String[] atmExpiries = { "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y" };
		String[] atmTenors = { "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y" };
		double[] atmNormalVolatilities = { 0.00151, 0.00169, 0.0021, 0.00248, 0.00291, 0.00329, 0.00365, 0.004, 0.00437, 0.00466, 0.00527, 0.00571, 0.00604, 0.00625, 0.0016, 0.00174, 0.00217, 0.00264, 0.00314, 0.00355, 0.00398, 0.00433, 0.00469, 0.00493, 0.00569, 0.00607, 0.00627, 0.00645, 0.00182, 0.00204, 0.00238, 0.00286, 0.00339, 0.00384, 0.00424, 0.00456, 0.00488, 0.0052, 0.0059, 0.00623, 0.0064, 0.00654, 0.00205, 0.00235, 0.00272, 0.0032, 0.00368, 0.00406, 0.00447, 0.00484, 0.00515, 0.00544, 0.00602, 0.00629, 0.0064, 0.00646, 0.00279, 0.00319, 0.0036, 0.00396, 0.00436, 0.00469, 0.00503, 0.0053, 0.00557, 0.00582, 0.00616, 0.00628, 0.00638, 0.00641, 0.00379, 0.00406, 0.00439, 0.00472, 0.00504, 0.00532, 0.0056, 0.00582, 0.00602, 0.00617, 0.0063, 0.00636, 0.00638, 0.00639, 0.00471, 0.00489, 0.00511, 0.00539, 0.00563, 0.00583, 0.006, 0.00618, 0.0063, 0.00644, 0.00641, 0.00638, 0.00635, 0.00634, 0.00544, 0.00557, 0.00572, 0.00591, 0.00604, 0.00617, 0.0063, 0.00641, 0.00651, 0.00661, 0.00645, 0.00634, 0.00627, 0.00624, 0.00625, 0.00632, 0.00638, 0.00644, 0.0065, 0.00655, 0.00661, 0.00667, 0.00672, 0.00673, 0.00634, 0.00614, 0.00599, 0.00593, 0.00664, 0.00671, 0.00675, 0.00676, 0.00676, 0.00675, 0.00676, 0.00674, 0.00672, 0.00669, 0.00616, 0.00586, 0.00569, 0.00558, 0.00647, 0.00651, 0.00651, 0.00651, 0.00652, 0.00649, 0.00645, 0.0064, 0.00637, 0.00631, 0.00576, 0.00534, 0.00512, 0.00495, 0.00615, 0.0062, 0.00618, 0.00613, 0.0061, 0.00607, 0.00602, 0.00596, 0.00591, 0.00586, 0.00536, 0.00491, 0.00469, 0.0045, 0.00578, 0.00583, 0.00579, 0.00574, 0.00567, 0.00562, 0.00556, 0.00549, 0.00545, 0.00538, 0.00493, 0.00453, 0.00435, 0.0042, 0.00542, 0.00547, 0.00539, 0.00532, 0.00522, 0.00516, 0.0051, 0.00504, 0.005, 0.00495, 0.00454, 0.00418, 0.00404, 0.00394 };

		LocalDate referenceDate = LocalDate.of(2016, Month.SEPTEMBER, 30); 
		BusinessdayCalendarExcludingTARGETHolidays cal = new BusinessdayCalendarExcludingTARGETHolidays();
		DayCountConvention_ACT_365 modelDC = new DayCountConvention_ACT_365();
		for(int i=0; i<atmNormalVolatilities.length; i++ ) {

			LocalDate exerciseDate = cal.createDateFromDateAndOffsetCode(referenceDate, atmExpiries[i]);
			LocalDate tenorEndDate = cal.createDateFromDateAndOffsetCode(exerciseDate, atmTenors[i]);
			double	exercise		= modelDC.getDaycountFraction(referenceDate, exerciseDate);
			double	tenor			= modelDC.getDaycountFraction(exerciseDate, tenorEndDate);

			// We consider an idealized tenor grid (alternative: adapt the model grid)
			exercise	= Math.round(exercise/0.25)*0.25;
			tenor		= Math.round(tenor/0.25)*0.25;

			if(exercise < 1.0) continue;

			int numberOfPeriods = (int)Math.round(tenor / swapPeriodLength);

			double	moneyness			= 0.0;
			double	targetVolatility	= atmNormalVolatilities[i];

			String	targetVolatilityType = "VOLATILITYNORMAL";

			double	weight = 1.0;

			calibrationItems.add(createCalibrationItem(weight, exercise, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurve, discountCurve));
			calibrationItemNames.add(atmExpiries[i]+"\t"+atmTenors[i]);
		}

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
		//final BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotionCudaWithHostRandomVariable(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */);
		//final BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotionCudaWithRandomVariableCuda(timeDiscretization, numberOfFactors, numberOfPaths, 31415 /* seed */);

		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretization, liborPeriodDiscretization, new TimeDiscretization(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0, 40.0), new TimeDiscretization(0.00, 1.0, 2.0, 5.0, 10.0, 20.0, 30.0, 40.0), 0.50 / 100);
		LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretization, liborPeriodDiscretization, numberOfFactors, 0.05, false);
		// Create a covariance model
		//AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretization, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20/100.0, 0.05/100.0, 0.10, 0.05/100.0, 0.10} );
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization, liborPeriodDiscretization, volatilityModel, correlationModel);

		// Create blended local volatility model with fixed parameter (0=lognormal, > 1 = almost a normal model).			
		AbstractLIBORCovarianceModelParametric covarianceModelDisplaced = new DisplacedLocalVolatilityModel(covarianceModelParametric, 1.0/0.25, false /* isCalibrateable */);

		// Set model properties
		Map<String, Object> properties = new HashMap<String, Object>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModel.Measure.SPOT.name());

		// Choose normal state space for the Euler scheme (the covariance model above carries a linear local volatility model, such that the resulting model is log-normal).
		properties.put("stateSpace", LIBORMarketModel.StateSpace.NORMAL.name());

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).		
		Double accuracy = new Double(5E-4);	// Lower accuracy to reduce runtime of the unit test
		int maxIterations = 400;
		int numberOfThreads = 4;
		OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

		double[] parameterStandardDeviation = new double[covarianceModelParametric.getParameter().length];
		double[] parameterLowerBound = new double[covarianceModelParametric.getParameter().length];
		double[] parameterUpperBound = new double[covarianceModelParametric.getParameter().length];
		Arrays.fill(parameterStandardDeviation, 0.20/100.0);
		Arrays.fill(parameterLowerBound, 0.0);
		Arrays.fill(parameterUpperBound, Double.POSITIVE_INFINITY);

//		optimizerFactory = new OptimizerFactoryCMAES(accuracy, maxIterations, parameterLowerBound, parameterUpperBound, parameterStandardDeviation);

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
		Map<String, Object> calibrationParameters = new HashMap<String, Object>();
		calibrationParameters.put("accuracy", accuracy);
		calibrationParameters.put("brownianMotion", brownianMotion);
		calibrationParameters.put("optimizerFactory", optimizerFactory);
		calibrationParameters.put("parameterStep", new Double(1E-4));
		properties.put("calibrationParameters", calibrationParameters);

		long millisCalibrationStart = System.currentTimeMillis();

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModel.CalibrationItem[] calibrationItemsLMM = new LIBORMarketModel.CalibrationItem[calibrationItemNames.size()];
		for(int i=0; i<calibrationItemNames.size(); i++) calibrationItemsLMM[i] = new LIBORMarketModel.CalibrationItem(calibrationItems.get(i).calibrationProduct,calibrationItems.get(i).calibrationTargetValue,calibrationItems.get(i).calibrationWeight);
		LIBORModelInterface liborMarketModelCalibrated = new LIBORMarketModel(
				liborPeriodDiscretization,
				curveModel,
				forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve),
				covarianceModelDisplaced,
				calibrationItemsLMM,
				properties);

		long millisCalibrationEnd = System.currentTimeMillis();

		System.out.println("\nCalibrated parameters are:");
		double[] param = ((AbstractLIBORCovarianceModelParametric)((LIBORMarketModel) liborMarketModelCalibrated).getCovarianceModel()).getParameter();
		for (double p : param) System.out.println(p);

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
		LIBORModelMonteCarloSimulationInterface simulationCalibrated = new LIBORModelMonteCarloSimulation(liborMarketModelCalibrated, process);

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
		
		System.out.println("Calibration of curves........." + (millisCurvesEnd-millisCurvesStart)/1000.0);
		System.out.println("Calibration of volatilities..." + (millisCalibrationEnd-millisCalibrationStart)/1000.0);
		
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

		parameters.put("referenceDate", LocalDate.of(2016, Month.SEPTEMBER, 30)); 
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