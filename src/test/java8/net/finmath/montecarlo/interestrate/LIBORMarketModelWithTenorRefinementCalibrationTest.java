/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 16.01.2015
 */
package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.Month;
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

import org.junit.Assert;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.calibration.ParameterObject;
import net.finmath.marketdata.calibration.Solver;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveFromDiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.marketdata.products.Swap;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.interestrate.models.HullWhiteModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelWithTenorRefinement;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.DisplacedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelPiecewiseConstant;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModelAsGiven;
import net.finmath.montecarlo.interestrate.models.covariance.TermStructCovarianceModelFromLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.TermStructureCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.TermStructureTenorTimeScaling;
import net.finmath.montecarlo.interestrate.models.covariance.TermStructureTenorTimeScalingPicewiseConstant;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.TimeDiscretizationFromArray.ShortPeriodLocation;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * This class tests the LIBOR market model and products.
 *
 * @author Christian Fries
 */
public class LIBORMarketModelWithTenorRefinementCalibrationTest {

	private final int numberOfPaths		= 5000;
	private final int numberOfFactors	= 1;

	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.0000%;-##0.0000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterParam		= new DecimalFormat(" #0.00000;-#0.00000", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	public static void main(final String[] args) throws CalculationException, SolverException {
		final LIBORMarketModelWithTenorRefinementCalibrationTest test = new LIBORMarketModelWithTenorRefinementCalibrationTest();
		test.testATMSwaptionCalibration();
	}

	public LIBORMarketModelWithTenorRefinementCalibrationTest() {
	}

	private CalibrationProduct createCalibrationItem(final double weight, final double exerciseDate, final double swapPeriodLength, final int numberOfPeriods, final double moneyness, final double targetVolatility, final String targetVolatilityType, final ForwardCurve forwardCurve, final DiscountCurve discountCurve) throws CalculationException {

		final double[]	fixingDates			= new double[numberOfPeriods];
		final double[]	paymentDates		= new double[numberOfPeriods];
		final double[]	swapTenor			= new double[numberOfPeriods + 1];

		for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
			fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
			swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
		}
		swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

		// Swaptions swap rate
		final double swaprate = moneyness + getParSwaprate(forwardCurve, discountCurve, swapTenor);

		// Set swap rates for each period
		final double[] swaprates = new double[numberOfPeriods];
		Arrays.fill(swaprates, swaprate);

		/*
		 * We use Monte-Carlo calibration on implied volatility.
		 * Alternatively you may change here to Monte-Carlo valuation on price or
		 * use an analytic approximation formula, etc.
		 */
		final SwaptionSimple swaptionMonteCarlo = new SwaptionSimple(swaprate, swapTenor, SwaptionSimple.ValueUnit.valueOf(targetVolatilityType));
		//		double targetValuePrice = AnalyticFormulas.blackModelSwaptionValue(swaprate, targetVolatility, fixingDates[0], swaprate, getSwapAnnuity(discountCurve, swapTenor));
		return new CalibrationProduct(swaptionMonteCarlo, targetVolatility, weight);
	}

	public void testSwaptionSmileCalibration() throws CalculationException {

		/*
		 * Calibration test
		 */
		System.out.println("Calibration to Swaptions:");

		final double[] fixingTimes = new double[] {
				0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5,
				10.0, 10.5, 11.0, 11.5, 12.0, 12.5, 13.0, 13.5, 14.0, 14.5, 15.0, 15.5, 16.0, 16.5, 17.0, 17.5, 18.0,
				18.5, 19.0, 19.5, 20.0, 20.5, 21.0, 21.5, 22.0, 22.5, 23.0, 23.5, 24.0, 24.5, 25.0, 25.5, 26.0, 26.5,
				27.0, 27.5, 28.0, 28.5, 29.0, 29.5, 30.0, 30.5, 31.0, 31.5, 32.0, 32.5, 33.0, 33.5, 34.0, 34.5, 35.0,
				35.5, 36.0, 36.5, 37.0, 37.5, 38.0, 38.5, 39.0, 39.5, 40.0, 40.5, 41.0, 41.5, 42.0, 42.5, 43.0, 43.5,
				44.0, 44.5, 45.0, 45.5, 46.0, 46.5, 47.0, 47.5, 48.0, 48.5, 49.0, 49.5, 50.0
		};

		final double[] forwardRates = new double[] {
				0.61 / 100.0, 0.61 / 100.0, 0.67 / 100.0, 0.73 / 100.0, 0.80 / 100.0, 0.92 / 100.0, 1.11 / 100.0,
				1.36 / 100.0, 1.60 / 100.0, 1.82 / 100.0, 2.02 / 100.0, 2.17 / 100.0, 2.27 / 100.0, 2.36 / 100.0,
				2.46 / 100.0, 2.52 / 100.0, 2.54 / 100.0, 2.57 / 100.0, 2.68 / 100.0, 2.82 / 100.0, 2.92 / 100.0,
				2.98 / 100.0, 3.00 / 100.0, 2.99 / 100.0, 2.95 / 100.0, 2.89 / 100.0, 2.82 / 100.0, 2.74 / 100.0,
				2.66 / 100.0, 2.59 / 100.0, 2.52 / 100.0, 2.47 / 100.0, 2.42 / 100.0, 2.38 / 100.0, 2.35 / 100.0,
				2.33 / 100.0, 2.31 / 100.0, 2.30 / 100.0, 2.29 / 100.0, 2.28 / 100.0, 2.27 / 100.0, 2.27 / 100.0,
				2.26 / 100.0, 2.26 / 100.0, 2.26 / 100.0, 2.26 / 100.0, 2.26 / 100.0, 2.26 / 100.0, 2.27 / 100.0,
				2.28 / 100.0, 2.28 / 100.0, 2.30 / 100.0, 2.31 / 100.0, 2.32 / 100.0, 2.34 / 100.0, 2.35 / 100.0,
				2.37 / 100.0, 2.39 / 100.0, 2.42 / 100.0, 2.44 / 100.0, 2.47 / 100.0, 2.50 / 100.0, 2.52 / 100.0,
				2.56 / 100.0, 2.59 / 100.0, 2.62 / 100.0, 2.65 / 100.0, 2.68 / 100.0, 2.72 / 100.0, 2.75 / 100.0,
				2.78 / 100.0, 2.81 / 100.0, 2.83 / 100.0, 2.86 / 100.0, 2.88 / 100.0, 2.91 / 100.0, 2.93 / 100.0,
				2.94 / 100.0, 2.96 / 100.0, 2.97 / 100.0, 2.97 / 100.0, 2.97 / 100.0, 2.97 / 100.0, 2.97 / 100.0,
				2.96 / 100.0, 2.95 / 100.0, 2.94 / 100.0, 2.93 / 100.0, 2.91 / 100.0, 2.89 / 100.0, 2.87 / 100.0,
				2.85 / 100.0, 2.83 / 100.0, 2.80 / 100.0, 2.78 / 100.0, 2.75 / 100.0, 2.72 / 100.0, 2.69 / 100.0,
				2.67 / 100.0, 2.64 / 100.0, 2.64 / 100.0
		};

		final double liborPeriodLength = 0.5;

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"		/* name of the curve */,
				fixingTimes			/* fixings of the forward */,
				forwardRates		/* forwards */,
				liborPeriodLength	/* tenor / period length */
				);


		final DiscountCurve discountCurve = new DiscountCurveFromForwardCurve(forwardCurveInterpolation, liborPeriodLength);

		/*
		 * Create a set of calibration products.
		 */
		final ArrayList<CalibrationProduct> calibrationProducts = new ArrayList<>();

		final double	swapPeriodLength	= 0.5;
		final int		numberOfPeriods		= 20;

		final double[] smileMoneynesses	= { -0.02,	-0.01, -0.005, -0.0025,	0.0,	0.0025,	0.0050,	0.01,	0.02 };
		final double[] smileVolatilities	= { 0.559,	0.377,	0.335,	 0.320,	0.308, 0.298, 0.290, 0.280, 0.270 };

		for(int i=0; i<smileMoneynesses.length; i++ ) {
			final double	exerciseDate		= 5.0;
			final double	moneyness			= smileMoneynesses[i];
			final double	targetVolatility	= smileVolatilities[i];
			final String	targetVolatilityType = "VOLATILITY";

			calibrationProducts.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurveInterpolation, discountCurve));
		}

		final double[] atmOptionMaturities	= { 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 };
		final double[] atmOptionVolatilities	= { 0.385, 0.351, 0.325, 0.308, 0.288, 0.279, 0.290, 0.272, 0.235, 0.192 };

		for(int i=0; i<atmOptionMaturities.length; i++ ) {

			final double	exerciseDate		= atmOptionMaturities[i];
			final double	moneyness			= 0.0;
			final double	targetVolatility	= atmOptionVolatilities[i];
			final String	targetVolatilityType = "VOLATILITY";

			calibrationProducts.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurveInterpolation, discountCurve));
		}

		/*
		 * Create a simulation time discretization
		 */
		// If simulation time is below libor time, exceptions will be hard to track.
		final double lastTime	= 40.0;
		final double dt		= 1.0;//0.0625;
		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);
		final TimeDiscretization liborPeriodDiscretization = timeDiscretizationFromArray;

		/*
		 * Create Brownian motions
		 */
		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors , numberOfPaths, 31415 /* seed */);

		//		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelTimeHomogenousPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 ), new double[] { 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0 });
		final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 ), new TimeDiscretizationFromArray(2.00, 5.00,10.00, 15.00, 30.00 ), 0.20 / 100);
		final LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, 0.25);
		// Create a covariance model
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20/100.0, 0.05/100.0, 0.10, 0.05/100.0, 0.10} );
		covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray, liborPeriodDiscretization, volatilityModel, correlationModel);

		// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
		final AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModelParametric, 0.0, false);
		// Create stochastic scaling (pass brownianMotionView2 to it)

		// Set model properties
		final Map<String, Object> properties = new HashMap<>();

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
		final Map<String, Object> calibrationParameters = new HashMap<>();
		calibrationParameters.put("accuracy", new Double(1E-6));
		calibrationParameters.put("brownianMotion", brownianMotion);
		properties.put("calibrationParameters", calibrationParameters);

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final TimeDiscretization liborPeriodDiscretizationFine = new TimeDiscretizationFromArray(0.0, 40.0, 0.0625, ShortPeriodLocation.SHORT_PERIOD_AT_START);
		final TimeDiscretization liborPeriodDiscretizationMedium = new TimeDiscretizationFromArray(0.0, 40.0, 0.25, ShortPeriodLocation.SHORT_PERIOD_AT_START);
		final TimeDiscretization liborPeriodDiscretizationCoarse = new TimeDiscretizationFromArray(0.0, 40.0, 4.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
		final TermStructureModel liborMarketModelCalibrated = new LIBORMarketModelWithTenorRefinement(
				new TimeDiscretization[] { liborPeriodDiscretizationFine, liborPeriodDiscretizationMedium, liborPeriodDiscretizationCoarse },
				new Integer[] { 4, 8, 200 },
				null,
				forwardCurveInterpolation, new DiscountCurveFromForwardCurve(forwardCurveInterpolation),
				new TermStructCovarianceModelFromLIBORCovarianceModelParametric(null, covarianceModelParametric), calibrationProducts.toArray(new CalibrationProduct[0]), properties);


		/*
		 * Test our calibration
		 */
		System.out.println("\nCalibrated parameters are:");
		final double[] param = ((LIBORMarketModelWithTenorRefinement) liborMarketModelCalibrated).getCovarianceModel().getParameter();
		//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
		for (final double p : param) {
			System.out.println(formatterParam.format(p));
		}

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModelCalibrated, brownianMotion);
		final TermStructureMonteCarloSimulationFromTermStructureModel simulationCalibrated = new TermStructureMonteCarloSimulationFromTermStructureModel(liborMarketModelCalibrated, process);

		System.out.println("\nValuation on calibrated model:");
		double deviationSum			= 0.0;
		double deviationSquaredSum	= 0.0;
		for (int i = 0; i < calibrationProducts.size(); i++) {
			final AbstractLIBORMonteCarloProduct calibrationProduct = calibrationProducts.get(i).getProduct();
			try {
				final double valueModel = calibrationProduct.getValue(simulationCalibrated);
				final double valueTarget = calibrationProducts.get(i).getTargetValue().getAverage();
				final double error = valueModel-valueTarget;
				deviationSum += error;
				deviationSquaredSum += error*error;
				System.out.println("Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget) + "\t" + calibrationProduct.toString());
			}
			catch(final Exception e) {
				e.printStackTrace();
			}
		}
		final double averageDeviation = deviationSum/calibrationProducts.size();
		System.out.println("Mean Deviation:" + formatterValue.format(averageDeviation));
		System.out.println("RMS Error.....:" + formatterValue.format(Math.sqrt(deviationSquaredSum/calibrationProducts.size())));
		System.out.println("__________________________________________________________________________________________\n");

		Assert.assertTrue(Math.abs(averageDeviation) < 1E-2);
	}

	public void testATMSwaptionCalibration() throws CalculationException, SolverException {
		/*
		 * Calibration test
		 */
		System.out.println("Calibration to Swaptions:");

		final AnalyticModel curveModel = getCalibratedCurve();

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurve forwardCurve = curveModel.getForwardCurve("ForwardCurveFromDiscountCurve(discountCurve-EUR,6M)");

		final DiscountCurve discountCurve = curveModel.getDiscountCurve("discountCurve-EUR");

		/*
		 * Create a set of calibration products.
		 */
		final ArrayList<String>			calibrationItemNames	= new ArrayList<>();
		final ArrayList<CalibrationProduct>	calibrationProducts		= new ArrayList<>();

		final double	swapPeriodLength	= 0.5;

		final String[] atmExpiries = {
				"1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "1M", "3M", "3M", "3M",
				"3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "3M", "6M", "6M", "6M", "6M", "6M", "6M",
				"6M", "6M", "6M", "6M", "6M", "6M", "6M", "6M", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y", "1Y",
				"1Y", "1Y", "1Y", "1Y", "1Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y", "2Y",
				"2Y", "2Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "3Y", "4Y",
				"4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "4Y", "5Y", "5Y", "5Y", "5Y",
				"5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "5Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y",
				"7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "7Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y", "10Y",
				"10Y", "10Y", "10Y", "10Y", "10Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y", "15Y",
				"15Y", "15Y", "15Y", "15Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y", "20Y",
				"20Y", "20Y", "20Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y", "25Y",
				"25Y", "25Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y", "30Y",
		"30Y" };
		final String[] atmTenors = {
				"1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y",
				"3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y",
				"5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y",
				"7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y",
				"9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y",
				"15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y",
				"25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y",
				"1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y",
				"3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y",
				"5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y",
				"7Y", "8Y", "9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y",
				"9Y", "10Y", "15Y", "20Y", "25Y", "30Y", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y",
				"15Y", "20Y", "25Y", "30Y" };
		final double[] atmNormalVolatilities = {
				0.00151, 0.00169, 0.0021, 0.00248, 0.00291, 0.00329, 0.00365, 0.004, 0.00437, 0.00466, 0.00527, 0.00571,
				0.00604, 0.00625, 0.0016, 0.00174, 0.00217, 0.00264, 0.00314, 0.00355, 0.00398, 0.00433, 0.00469,
				0.00493, 0.00569, 0.00607, 0.00627, 0.00645, 0.00182, 0.00204, 0.00238, 0.00286, 0.00339, 0.00384,
				0.00424, 0.00456, 0.00488, 0.0052, 0.0059, 0.00623, 0.0064, 0.00654, 0.00205, 0.00235, 0.00272, 0.0032,
				0.00368, 0.00406, 0.00447, 0.00484, 0.00515, 0.00544, 0.00602, 0.00629, 0.0064, 0.00646, 0.00279,
				0.00319, 0.0036, 0.00396, 0.00436, 0.00469, 0.00503, 0.0053, 0.00557, 0.00582, 0.00616, 0.00628,
				0.00638, 0.00641, 0.00379, 0.00406, 0.00439, 0.00472, 0.00504, 0.00532, 0.0056, 0.00582, 0.00602,
				0.00617, 0.0063, 0.00636, 0.00638, 0.00639, 0.00471, 0.00489, 0.00511, 0.00539, 0.00563, 0.00583, 0.006,
				0.00618, 0.0063, 0.00644, 0.00641, 0.00638, 0.00635, 0.00634, 0.00544, 0.00557, 0.00572, 0.00591,
				0.00604, 0.00617, 0.0063, 0.00641, 0.00651, 0.00661, 0.00645, 0.00634, 0.00627, 0.00624, 0.00625,
				0.00632, 0.00638, 0.00644, 0.0065, 0.00655, 0.00661, 0.00667, 0.00672, 0.00673, 0.00634, 0.00614,
				0.00599, 0.00593, 0.00664, 0.00671, 0.00675, 0.00676, 0.00676, 0.00675, 0.00676, 0.00674, 0.00672,
				0.00669, 0.00616, 0.00586, 0.00569, 0.00558, 0.00647, 0.00651, 0.00651, 0.00651, 0.00652, 0.00649,
				0.00645, 0.0064, 0.00637, 0.00631, 0.00576, 0.00534, 0.00512, 0.00495, 0.00615, 0.0062, 0.00618,
				0.00613, 0.0061, 0.00607, 0.00602, 0.00596, 0.00591, 0.00586, 0.00536, 0.00491, 0.00469, 0.0045,
				0.00578, 0.00583, 0.00579, 0.00574, 0.00567, 0.00562, 0.00556, 0.00549, 0.00545, 0.00538, 0.00493,
				0.00453, 0.00435, 0.0042, 0.00542, 0.00547, 0.00539, 0.00532, 0.00522, 0.00516, 0.0051, 0.00504, 0.005,
				0.00495, 0.00454, 0.00418, 0.00404, 0.00394 };

		final LocalDate referenceDate = LocalDate.of(2016, Month.SEPTEMBER, 30);
		final BusinessdayCalendarExcludingTARGETHolidays cal = new BusinessdayCalendarExcludingTARGETHolidays();
		final DayCountConvention_ACT_365 modelDC = new DayCountConvention_ACT_365();
		for(int i=0; i<atmNormalVolatilities.length; i++ ) {

			final LocalDate exerciseDate = cal.getDateFromDateAndOffsetCode(referenceDate, atmExpiries[i]);
			final LocalDate tenorEndDate = cal.getDateFromDateAndOffsetCode(exerciseDate, atmTenors[i]);
			double	exercise		= modelDC.getDaycountFraction(referenceDate, exerciseDate);
			double	tenor			= modelDC.getDaycountFraction(exerciseDate, tenorEndDate);

			exercise = Math.round(exercise/0.25)*0.25;
			tenor = Math.round(tenor/0.25)*0.25;

			if(exercise < 0.25) {
				continue;
			}
			if(exercise < 1.0) {
				continue;
			}

			final int numberOfPeriods = (int)Math.round(tenor / swapPeriodLength);

			final double	moneyness			= 0.0;
			final double	targetVolatility	= atmNormalVolatilities[i];

			final String	targetVolatilityType = "VOLATILITYNORMAL";

			final double	weight = 1.0;

			//			if(exercise != 1.0 && (exercise+tenor < 30 || exercise+tenor >= 40)) weight = 0.01;
			//			if((exercise+tenor < 30 || exercise+tenor >= 40)) weight = 0.01;

			calibrationProducts.add(createCalibrationItem(weight, exercise, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, targetVolatilityType, forwardCurve, discountCurve));
			calibrationItemNames.add(atmExpiries[i]+"\t"+atmTenors[i]);
		}

		//		calibrationItems.add(new CalibrationProduct(new VolatilitySufaceRoughness(), 0.0, 1.0));
		//		calibrationItemNames.add("Volatility surface roughness");

		/*
		 * Create a simulation time discretization
		 */
		// If simulation time is below libor time, exceptions will be hard to track.
		final double lastTime	= 40.0;
		final double dt		= 0.0625;
		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);
		final TimeDiscretization liborPeriodDiscretization = timeDiscretizationFromArray;

		/*
		 * Create Brownian motions
		 */
		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 31415 /* seed */);
		//		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionCudaWithRandomVariableCuda(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 31415 /* seed */);

		final int test = 0;			// 0 LMM with refinment, 1 LMMM, 2 HW 1 mr param, 3 HW with vector mr
		TermStructureMonteCarloSimulationModel simulationCalibrated = null;
		if(test == 0) {
			final TimeDiscretization optionMaturityDiscretization = new TimeDiscretizationFromArray(0.0, 0.25, 0.50, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 15.0, 20.0, 25.0, 30.0, 40.0);

			TimeDiscretization timeToMaturityDiscretization = new TimeDiscretizationFromArray(0.00, 40, 1.0);
			final ArrayList<Double> timeToMaturityList = timeToMaturityDiscretization.getAsArrayList();
			timeToMaturityList.add(0.25);
			timeToMaturityList.add(0.50);
			timeToMaturityList.add(0.75);
			timeToMaturityDiscretization = new TimeDiscretizationFromArray(timeToMaturityList);
			//			optionMaturityDiscretization = timeToMaturityDiscretization;

			final double[] volatilityFineInitial = { 0.00188142173350013, 0.00244713812905251, 0.00312935232764786, 0.00424272403585511, 0.00561176019992977, 0.00568485209131756, 0.00683434262426612, 0.00755557139079009, 0.00771211664332339, 0.00824545731419025, 0.00861010546271496, 0.00774567189155376, 0.00756151667905925, 0.00716622512847832, 0.00680188750589074, 0.00698276842259019, 0.00673562900064002, 0.00736245353917806, 0.00793386630005653, 0.00774736825344931, 0.00703201120614119, 0.00689689547976478, 0.00693214319886021, 0.00679403321903707, 0.00689875296307854, 0.00723724500604481, 0.00679067514222039, 0.00701609275468305, 0.00671237185635425, 0.0061010213268778, 0.00639090714283796, 0.00591785251069359, 0.00547572428092251, 0.00452818855618976, 0.0040916097994791, 0.00506326717558218, 0.00447523023657468, 0.00380562299280579, 0.0033997609886344, 0.00157369148981344, 0.004, 0.00264668487730639, 0.00338358438549667, 0.0042583392466523, 0.00374251014853167, 0.00609664367488931, 0.00527491641895377, 0.00668315097703198, 0.00658202701405219, 0.00709801065756526, 0.00838042553100674, 0.00776896234056017, 0.00763102774582922, 0.00717721971340214, 0.00630362543200968, 0.00599253151486988, 0.00514204782796695, 0.00578960682825584, 0.00675665772559523, 0.00603929424704131, 0.00616580840780932, 0.00678814177521793, 0.00695349322073888, 0.00664405284337943, 0.00612446829351134, 0.00666094465333253, 0.00646524846239828, 0.00678975705219538, 0.00620950886390075, 0.00603812195184095, 0.00635765268485236, 0.00648999847163647, 0.0065402845256706, 0.00666109353707837, 0.00546149067254712, 0.00617349066597743, 0.0053197806621577, 0.00499758197738068, 0.00456967178656529, 0.00549217394102514, 0.004, 0.00412412862348145, 0.00360997813643166, 0.00481676441508378, 0.00442173545912594, 0.00626435623508068, 0.00490748817476142, 0.00745873785986973, 0.00627030538387987, 0.00603995044927501, 0.00707050614556265, 0.00808277626556295, 0.00714374328360619, 0.00667678601812034, 0.00604972234695157, 0.00429429262103971, 0.00477842598113308, 0.00583478068977273, 0.00570395559364015, 0.0053526504964704, 0.00580800537932058, 0.00561348824545989, 0.00483235138677368, 0.00554873189039507, 0.00524526349971304, 0.005110275849157, 0.00563323921458802, 0.00431021537816501, 0.00612095274226146, 0.00607222630125266, 0.0070207867102145, 0.00750607561405071, 0.00755101553992851, 0.00694551280489971, 0.00640251046333924, 0.0056583463253938, 0.00548564944007219, 0.0059362809916561, 0.010022429086807, 0.004, 0.00616465524445924, 0.0060194741377253, 0.0051515868776049, 0.00657705473695597, 0.0068820531698432, 0.00609478020957857, 0.00595194833262245, 0.00773773013331189, 0.00620603067736448, 0.00860041000265634, 0.0103714955260017, 0.00711409936312723, 0.00708490397224151, 0.00588614545075859, 0.00476822741196506, 0.0049249797906214, 0.00543872065493878, 0.00500218620026466, 0.00515646681969159, 0.00598922898075584, 0.00525394949034156, 0.00577209171498963, 0.00551211081588153, 0.00560892381073576, 0.00539597721127869, 0.00331362471109036, 0.00565077664923615, 0.00563266715477447, 0.00604349161423452, 0.00677268092065568, 0.00689029823433145, 0.00652434453656715, 0.00601450362015644, 0.00531049433504766, 0.00523967548428067, 0.00547120426275253, -0.00169670883759053, 0.004, 0.00682404350456255, 0.00675930436370809, 0.00534471722873658, 0.00659228819184702, 0.00548138103279273, 0.00669369679725406, 0.00528976217365383, 0.00789897557589221, 0.00510347852791096, 0.00868517525213428, 0.00836176024146618, 0.00816808466527832, 0.00679595780220292, 0.00534104826948273, 0.00443401432258604, 0.00516725665277164, 0.00455550155687438, 0.00516694150274943, 0.00622634160268162, 0.00543749614961861, 0.00589445102409619, 0.00613008761449019, 0.00549987898889946, 0.00587770649323352, 0.00541341048287516, 0.00492256772868173, 0.00482451314574211, 0.0044311830979994, 0.00532354748417067, 0.00504800515131747, 0.00495170632665564, 0.00538150801291063, 0.00462242464575121, 0.00413514271461505, 0.0036373147797081, -0.00195378506178586, 0.004, -0.197222484469832, 0.00626696027150197, 0.00552676863465162, 0.00540402131929257, 0.0055417967222204, 0.00514070546415228, 0.00473197076473461, 0.00535460847223553, 0.00447658651597976, 0.00750782960285101, 0.00755712431211006, 0.00620198564717612, 0.00437692785834355, 0.0026357748104504, 0.00373127246514, 0.00338447072006536, 0.00249017660932901, 0.00219493792926755, 0.000886416064192735, 0.00396800934108589, 0.00400387567622326, 0.00279081254689441, 0.00188456892939609, 0.000122667470926923, 0.0043881238544917, 0.00462899765423492, 0.00442815274857422, 0.00439771012641959, 0.00424063086758808, 0.00476637224080197, 0.0054040801853329, 0.00496393960945804, 0.0049014659862974, 0.00510887865003792, 0.00753551511998958, 0.004, 0.00600089469060071, 0.00531519990199881, 0.00565204942888863, 0.00532763837356964, 0.00501618432994098, 0.00492594213392192, 0.00559698190755737, 0.00461084121390951, 0.00681530395874622, 0.0076402678613766, 0.00622318152889423, 0.00493293616530465, 0.00368919642570353, 0.00432588286239437, 0.00353563111213374, 0.00326407483908194, 0.00344121740951852, 0.0019183247127117, 0.00470411107841229, 0.00482331079413266, 0.00415499318796653, 0.00373015182732561, 0.00235117185785453, 0.00421434154159793, 0.00350061462351939, 0.00310990364933925, 0.00316824645365618, 0.00164459036802477, 0.00402934548454256, 0.00515060329163864, 0.00438002737635865, 0.00416314580951372, 0.00378943063610239, 0.0210491352392416, 0.004, 0.0189846773797999, -0.0183387615658739, 0.00665993807873783, 0.00678237644387676, 0.00718086739590955, 0.00640397688334268, 0.00595962385957377, 0.00556680089907536, 0.00672242882927451, 0.00902285941034415, 0.0103753585107828, 0.0107896503436788, 0.00684201688207598, 0.00637048591780432, 0.004468240509815, 0.00351797221719007, 0.00124556830714796, 0.00538759627970082, 0.00528812242408913, 0.00342125527486584, 0.00218778990631438, 0.000585577720059274, 0.0057460194737967, 0.00513802760025306, 0.00343810471566253, 0.0014634108780903, -0.00063745034512957, -0.000179179052024604, 0.00444580295869898, 0.00547673206293821, 0.00653463642612653, 0.00617449845701861, -0.00484997751055757, 0.004, -0.16074721850062, 0.00620069252881296, 0.00625409069119784, 0.00656773324939692, 0.00591364979644733, 0.00551369213534098, 0.0051444882388198, 0.00600822928084645, 0.00775163683222475, 0.00880026883541508, 0.00895358327866209, 0.00593365732425627, 0.00563877987306613, 0.00429460320962464, 0.00344505667319563, 0.00203132491797663, 0.00502377258935102, 0.00519852484055304, 0.00426039670971861, 0.003159274763909, 0.00166268433610453, 0.00480637759852153, 0.00475627411593264, 0.00398376147396027, 0.00335143798554606, 0.00258677483153201, 0.00328978459023786, 0.00504326371713696, 0.00489819846759245, 0.00521822159465031, 0.00557888888039177, 0.00629722566415151, 0.004, 0.0062833734704122, 0.00644470507858546, 0.00677102384340264, 0.00597102303978095, 0.00558771259164609, 0.00508664479437948, 0.00582094769855885, 0.00737041419321413, 0.00808416467957846, 0.00816626141716939, 0.00622836882535576, 0.00617125193933055, 0.0038559085062547, 0.00286618604519527, 0.000462565675257915, 0.00499053772244887, 0.00460598022590686, 0.00372410616688586, 0.00128611185357028, 0.000696031971736591, 0.00483247638684995, 0.00415721810015557, 0.00247348119475523, 0.00181055981341248, 0.000428488318382846, 0.000247486801972927, 0.00292640411764009, 0.00374379254037987, 0.00425529189405349, 0.0039979945685008, 0.00457482451733148, 0.004, -0.134018679711584, -0.115735115419455, -0.115735115429311, -0.115735115417353, 0.00524927086613273, 0.00515362952240186, 0.00568454708983756, 0.00558456624654833, 0.00580012291775871, 0.00711310698884615, 0.00734038661928209, 0.00731264299439564, 0.00817329850180583, 0.00811193595515245, 0.00585924752358954, 0.00459034964003944, 0.00301640838920548, 0.00184987288158387, -0.000448959187163014, 0.00443786408989724, 0.00509192693327725, 0.00384214906341748, 0.00272340779075718, 0.00137419149165576, 0.0031388343641706, 0.00466704471959889, 0.00425824608039059, 0.00440904153751598, 0.00421819782947236, -0.00178943907970017, 0.004, 0.153084893035999, 0.111042750063195, 0.111042750080336, 0.00541226654759576, 0.00547544261368078, 0.00644325063511948, 0.00617408696781758, 0.00684081118159238, 0.0069419713771668, 0.00796266189336458, 0.00764917054935741, 0.00907708544163661, 0.008904706308452, 0.00677972970217715, 0.005245960248911, 0.00512059472947709, 0.00411373459469543, 0.00461969677184784, 0.0053052435499631, 0.00392469423260614, 0.000867293441495013, -0.000541190467158872, -0.000986920823345379, -0.00410231216263519, -0.000792923384355021, 0.00233349997450476, 0.0033540283837796, 0.00179243369973643, -0.00816334109015774, 0.004, -0.112168648541389, -0.0895613553951765, 0.00600325143287847, 0.0058842850949162, 0.00681490159584115, 0.00689898197602102, 0.00755293051956682, 0.00697441633930242, 0.00752951884442625, 0.00762445652524131, 0.00839424109367274, 0.00866802093018094, 0.0068898635904766, 0.00469263172536492, 0.00429348891109959, 0.00182945269169529, 0.00104504973487811, 0.00295812554739881, 0.00364991222229353, 0.00302741080687442, -0.00159079490412379, -0.00225685225154633, 0.00330906136340458, 0.0084377551240854, 0.0037697170394465, 0.00208233772942516, 0.0022438525413249, -0.00598826353189619, 0.004, 0.246603313911467, 0.00542401424153042, 0.00524554752668135, 0.00570084493653443, 0.00577870314529835, 0.00586865219201785, 0.00793978389568166, 0.00750017453100902, 0.00801647322038517, 0.00897338789082769, 0.00936900472776055, 0.00615748005743816, 0.00469838091358023, 0.0042649944198027, 0.00267659237455928, 0.00181301733066789, 0.00371869131987452, 0.00324952673251296, 0.00181953519265541, 0.000598418250498434, -0.00143590257865953, -0.000128400559433996, 0.0027745601361867, 0.00266887333841286, 0.00237781436204573, 0.00129116441308689, -0.0025243521459801, 0.004, 0.00540549023399865, 0.00546978661454686, 0.00630124752396011, 0.00608465661329026, 0.0066574387447782, 0.00688944223516516, 0.00807957816141563, 0.00777489344575464, 0.00930437155701677, 0.0095460087839841, 0.0056898848792815, 0.00513225131858054, 0.00394186925927661, 0.00362493377994459, 0.00219555541659419, 0.00369630467052962, 0.00270790609552732, 0.000549795423912195, -0.00132825171584076, -0.00249191495490888, -0.000101332520740995, 0.00333736770596378, 0.00229765100537349, 0.00164964689076992, 0.00085092084954786, -0.00372252151682471, 0.004, 0.201007852692451, -0.0127342164007155, -0.0127342163964287, -0.0127342163966136, 0.00480297703961716, 0.00573781323392882, 0.0061650607689565, 0.00400640871061565, 0.00504719171171648, 0.00603680695947247, 0.00672995731699367, 0.00736580287569749, 0.00841279457366484, 0.00905406781519812, 0.00670865408580991, 0.00861546752796367, 0.00834669199892589, 0.00825016714366856, 0.00725021895446231, 0.0064058540903628, 0.00505764539825815, 0.00428083274448697, 0.00432890885383873, 0.00341859005372526, -0.0194555490576774, 0.004, -0.190550019534513, -0.125378578756109, -0.125378578755967, 0.00467736246770524, 0.00569910322355304, 0.00592169355255533, 0.00405921302507087, 0.00535912902861615, 0.00623649448105408, 0.00706776606626972, 0.00788294315400603, 0.00971364013456496, 0.0107059873490125, 0.00490058164749244, 0.00456262832755202, 0.00356385625235498, 0.0034565143717254, 0.00157092953372462, 0.00121431290639782, 0.00154952219648425, 0.00118467505499431, 0.0004131975586109, -0.000699902401586334, -0.0342377537367556, 0.004, -0.0182521519679822, 0.065411029816537, 0.00495233956297924, 0.00702967837488282, 0.00700122769135017, 0.00362812387972704, 0.00732296050684732, 0.00698320109299191, 0.0078162738794499, 0.00893125547333946, 0.0108753050239391, 0.0117424700700635, 0.00586920922616124, 0.00633121669246589, 0.00552407179093373, 0.00636915846674222, 0.00597243270062556, 0.00200181169415653, 0.00112777665513735, 0.00163189358790245, 0.001447666618704, -0.00221626832649058, -0.0230820701409544, 0.004, 0.0697882947721137, 0.0049339327383482, 0.00565025938245767, 0.00624535792667892, 0.00459955153813207, 0.00534910070156601, 0.00655393378910615, 0.00725893539893254, 0.00767871744229052, 0.00935753960863785, 0.0102754521550055, 0.00563840019175984, 0.00460575972942088, 0.0050309261892919, 0.00445387995544703, 0.00210388226432139, 0.00250613521266417, 0.00251836352313703, 0.00162373849874021, 0.000609526720925567, -0.00027691615199658, -0.00909954480102535, 0.004, 0.00510196438145562, 0.00586338249723067, 0.00649871981448966, 0.00452444934911543, 0.00533176743249264, 0.00689746840266128, 0.00793159703707077, 0.00862583621584827, 0.0104098894094926, 0.012214544788938, 0.00516175103227768, 0.0045297117911066, 0.00402604411367467, 0.00406299301565469, 0.00283047696527217, 0.0018274784749506, 0.00213956923838279, 0.00141068797272239, 0.000560250005339239, -0.00121541051051462, -0.0304438890138148, 0.004, -0.180639517363353, -0.141024318706879, -0.141024318705057, -0.141024318712856, 0.00437219872442167, 0.00609653039419425, 0.00542242887424487, 0.00440794230531482, 0.00251041422761071, 0.00507347871822824, 0.00589464359501231, 0.00601054860050314, 0.00794137984880719, 0.00900074461209995, 0.00546367378831234, 0.00445327054692074, 0.0046184963438731, 0.00534191918158842, 0.00557780601638256, 0.0117883747651444, 0.004, -0.134813842076166, -0.078766190032586, -0.0787661900294195, 0.00414139362313966, 0.00578914930389675, 0.00567830290024663, 0.00450171144891663, 0.00190411399806994, 0.0055550257259982, 0.00593667800733581, 0.00657229929647184, 0.00745053586068448, 0.00766592403082185, 0.00692484995260122, 0.00479284385326522, 0.00436343834355784, 0.00462807167679338, 0.00509816454730392, 0.0126167571816246, 0.004, 0.15167986186512, 0.220773786220517, 0.00387038781829683, 0.00608993715554102, 0.00580939426154654, 0.00470095382749162, 0.00245984453764531, 0.0135705578078898, 0.00819169973112957, 0.0102111211140499, 0.0117832726693842, 0.0126503297055948, 0.0116572599209275, 0.00806543436308163, 0.00659492630254821, 0.00703377814684929, 0.00794932203019307, 0.0156759603193197, 0.004, 0.0962458734903418, 0.00474413533313822, 0.00659863599033649, 0.00669999283975872, 0.00498304459854345, 0.00372575472786237, 0.00591552722486111, 0.00673741419050148, 0.00724821542024324, 0.0095534949807549, 0.011296915238539, 0.00728923843833547, 0.00472134646775808, 0.00478377199562191, 0.00560708945972712, 0.00594773448114153, 0.0192083621487655, 0.004, 0.00442575360436979, 0.00599223816435319, 0.00570035936021275, 0.00412814288485424, 0.00143901404790231, 0.00662607902713897, 0.00599335302367598, 0.00589533363322908, 0.00683315583733948, 0.00609123911153978, 0.00300029583721264, 0.00235068570122878, 0.00304630244693257, 0.00338238932005341, 0.00273036191288129, 0.00345991687648601, 0.004, 0.282046574748334, 0.115347935312723, 0.115347935337489, 0.115347935312131, 0.00348957742798341, 0.00471690797915702, 0.00480084757131838, 0.00285399051633338, 0.0028312300197789, 0.00355849937942815, 0.00407725073937473, 0.00447230921594607, 0.00525579725053393, 0.00638689910397696, 0.00934185729900714, 0.004, 0.00982418122121637, 1.71382898596364, 1.71382898643197, 0.00370387845848235, 0.00526379001894508, 0.00519277014799883, 0.00323130372656083, 0.00352631784303996, 0.00419357057707047, 0.00448057239787154, 0.00478343130654781, 0.00574879502088379, 0.00712223269913981, 0.0147749184740494, 0.004, 0.484004739614467, 0.141555755811279, 0.00355786334851272, 0.00512141868669868, 0.00494770222580906, 0.00267247383184296, 0.00255391713134704, 0.00412539591162217, 0.0045242000604618, 0.00464386743627561, 0.00548274312946107, 0.00686371250421262, 0.0299857381881637, 0.004, -0.319428915895427, 0.00251783886548631, 0.00504093386325881, 0.0038481117246798, 0.00207952110680643, 0.0023105342871514, 0.00243253400392396, 0.00333824010070498, 0.00401865956915386, 0.00492592615107898, 0.00581416101019428, 0.0174477454594243, 0.004, 0.00334412396718098, 0.00530232529771076, 0.00580760365537931, 0.00337226579345837, 0.00343647572772882, 0.00417033556540962, 0.00450290123623779, 0.00491957325897116, 0.00601023235622087, 0.00790855522847006, 0.0142496665914079, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004 };
			//			double[] paramTimeScalingVolParam = { 0.09, 0.09, 0.09, 0.09, -0.001900851034611, -0.00419575274955029, -0.00442084917971079, -0.00424776052425492, 0.0013764351846531, -0.00424098708715426, -0.00432276020850956, -0.00484327828049977, -0.00350582260076357, 0.004103522850748, -0.0033822564678249, -0.00223657909794653, 0.00143883962989193, 0.00156242850096184, 0.000881836104654567, 0.000878353505005478, 0.00202916721414624, 0.00744677974748718, -0.00499777720394327, -0.0032375775025563, -0.00609439733504146, -0.00238708478822353, -0.00183051148761336, 0.00745903406524448, 0.00145028601939714, 0.00149330089465308, 0.00156737975294966, 0.00161404363808074, 0.00141793953028837, -0.0052268458202927, 0.00365425462942354, 0.00946325638395848, 0.00681170414948539, -0.000940158807611766, -0.00699719094804223, 0.0135496307619622, 0.0185688809967169, 0.0311470500521249, 0.0311443375311536, 0.0295441217680911, 0.011892840783714, 0.0105499541420107, 0.00980427909840784, 0.0108669835180354, -0.00103425214317539, -0.00132711749747869, -0.00136280556843658, -0.000492166651541481, -0.00684797314217561, -0.00716493781129585, -0.00716738015141388, -0.00795060850560716, -0.00805094521185637, -0.00798743825874311, -0.00774609653300004, -0.00857869815592934, 0.00482854937626569, 0.00197785983094334, 0.00280251478929372, 0.000213109666955375, -0.000596924822051647, 0.0123793060132364, 0.0157545703846876, -0.0011865071259416, -0.00559066498178182, -0.00458095318497953, -0.00532061024929078, -0.00671344086117528, -0.00900000000000005, -0.00721630270143222, -0.0058148332445711, -0.0065798467479648, -0.00833700942159353, -0.00478590931714208, -0.00679184440698776, -0.00865227523856503, -0.001362554514414, -0.00215421678503531, 0.00563395174074059, 0.00288830137075635, -0.00322590555903346, -0.00319071778582056, -0.00332317957646992, -0.00333655953685308, -0.00737945746580621, -0.00559783201459666, -0.0064796064969056, -0.00681079187296774, -0.00562770110641906, -0.00386385774918665, -0.00543514188911274, -0.00313767056339287, -0.00610601314773561, -0.00804130953605721, -0.0059856471488905, -0.0051825868236844, -0.00157742038394218, 0.00234885673931785, -0.00900000000000005, -0.00900000000000005, -0.00733946049440504, -0.00769440762248876, -0.00900000000000005, -0.00899617654774175, -0.00701160001265094, -0.00858582578346101, -0.00746246514379436, -0.00900000000000005, -0.00900000000000005, 0.000598322579645014, -0.00900000000000005, -0.00900000000000005, -0.00892773221649463, -0.00893756172546233, -0.00893395964777937, -0.00900000000000005, -0.00900000000000005, 0.00329899008574813, 0.00281093577300424, 0.00226508346888493, -0.00509846886780593, -0.00509846886780252, -0.00508223132113386, -0.00508223132113414, -0.00548312892577001, -0.0054831289257163, -0.0054950348199361, -0.00549503481997561, 0.0034570427157422, 0.00345704271559782, 0.00361842409308451, 0.00361842409316437, -0.0028284582869719, -0.0028284582867792, -0.00242531833401301, -0.00242531833380979, -0.00813797926683094, -0.00813797926681104, -0.00813699944417436, -0.00813699944416896, -0.00883451143820678, -0.00883451143820678, -0.0088348640358015, -0.00883486403580235, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, -0.00864709861410944, -0.00864709861410944, -0.00864798790067908, -0.00864798790067908, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, 0.004, -0.494432859305378, -0.494432859006778, -0.494432859307916, -0.00157456511415939, 1.21851090326318E-06, 0.0027202838940817, 0.00452697788832656, 0.00549810253240709, 0.010995027658776, 0.00333689489212563, 0.00197221982391949, 0.00141531726062687, -0.00188531949594071, 0.004, 0.004, 0.004, 0.004, -0.000141069120803055, 0.004, 0.004, 0.004, 0.004, -0.000387912613996856, 0.004, 0.004, 0.004, 0.004, -0.000270490730546011, 0.004, 0.004, 0.004, 0.004, -0.000938307181808428, 0.004, 0.004, 0.004, 0.004, -0.00113333707074173, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 1.10645690745991, 1.10645690723855, -0.000196522038430659, -0.000155975866775778, 0.00392811095533235, 0.00734455193779385, 0.00860604548768743, 0.0115863782454146, 0.0097501847215871, 0.0127380105057194, 0.0153888755271697, 0.000730431658179325, 0.004, 0.004, 0.004, 0.004, -0.000209317243014865, 0.004, 0.004, 0.004, 0.004, -0.000225865837606041, 0.004, 0.004, 0.004, 0.004, -0.000314735418794094, 0.004, 0.004, 0.004, 0.004, -0.000546643602310953, 0.004, 0.004, 0.004, 0.004, -0.00437598358073981, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.00389706087474156, 0.00375800084091632, 0.00384260482602033, 0.00487086422437719, 0.00440649505996209, 0.00326807613630755, 0.00363821688177974, 0.00178214622005669, 0.00459380269628534, 0.00337814431416731, 0.00148870087545478, 0.00347388051058275, -0.000150015736133534, 0.004, 0.004, 0.004, 0.00174036886717814, 0.004, 0.004, 0.004, 0.004, 0.00184685842633084, 0.004, 0.004, 0.004, 0.004, 0.00489521607373615, 0.004, 0.004, 0.004, 0.004, 0.000200747953398207, 0.004, 0.004, 0.004, 0.004, -0.000697283865190439, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.000915431872142883, 0.00923966021143272, 0.0056517320473431, 0.00433388821481831, 0.00318723142202518, 0.00340970571823595, 0.00422203922755135, 0.00453976680412294, 0.0046202877861286, 0.00473675955977054, -0.00015508535585088, 0.0101430947793994, -0.000200687523000682, 0.004, 0.004, 0.00192670843135786, -0.000170668681166593, 0.004, 0.004, 0.004, 0.00134722059955132, -0.000262884611492943, 0.004, 0.004, 0.004, 0.00591055345048618, -0.00194419654256857, 0.004, 0.004, 0.004, 0.0006059521491806, -0.000666125818866443, 0.004, 0.004, 0.004, -0.000848820207580652, -0.00202603929390789, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.00421963564488254, 0.0197061148874491, -0.000264471542406444, 0.00285455039246963, 0.00430789439341713, 0.00497175435171098, 0.00520666482053443, 0.00512970956948983, 0.00756654678975618, -0.000156210177176093, -0.000167385712828239, 0.0235174249487206, -0.00446261437531541, 0.004, -0.000137292454515056, -0.000113727761598494, 0.004, 0.004, 0.004, 0.00195878853690182, -0.00243036614907955, 0.004, 0.004, 0.004, -0.00012589615196831, -0.00149689910704691, 0.004, 0.004, 0.004, 0.00292208772529403, -0.000992719687024317, 0.004, 0.004, 0.004, -0.000578660129118478, -0.00154971276165391, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.00491898584259906, 0.00276870412397827, 0.00494455774812606, 0.00656766760623052, 0.00727402059485561, 0.00790510525317462, 0.00695734080343227, 0.00795911616244319, 0.00149771814724309, 0.0064872647513368, 0.00838941933783797, 0.0503795079249428, 0.133565403436032, 0.00118111799840934, -0.000111966052028284, 0.004, 0.004, 0.004, 0.00297489888596317, -0.000616551518766928, 0.004, 0.004, 0.004, 0.00469478817973125, -0.000769862434642523, 0.004, 0.004, 0.004, -0.00015699987838295, 0.0088374935085937, 0.004, 0.004, 0.004, 0.0202955256196721, -0.00152105472223549, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.00162485948842415, 0.00719031288216238, 0.0117184425418287, 0.00650778161041364, 0.00520731821545655, 0.00626210936476919, 0.00528387320775105, 0.00241329476750947, 0.00222541926230401, 0.00486105177918752, -0.000137992232836665, 0.0551088997284223, 0.00277581090829659, -0.000130489857128356, 0.004, 0.004, 0.004, -0.00105027521524037, 0.00421240391851075, 0.004, 0.004, 0.004, -0.000246495319370642, 0.00752914495257707, 0.004, 0.004, 0.004, -0.000455549647551561, 0.00391330861938618, 0.004, 0.004, 0.004, -0.00506357089028416, -0.00112587601697093, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.394658127207145, 0.132198623338349, 0.122779672297155, -0.000787701729946966, -0.000136773490015179, -0.000326350252038345, -0.000160577124974238, 0.00588741182298707, -3.31833880405605E-05, 0.0659420306907641, 0.119489865608271, 0.000906659443464936, -0.000460157995175247, 0.004, 0.004, 0.004, 0.00453739023451891, -0.00100206634666101, 0.004, 0.004, 0.004, 0.00403250216799566, -0.00119057735244476, 0.004, 0.004, 0.004, 0.00277244308388306, -0.000127162097771313, 0.004, 0.004, 0.004, -0.000168637450522381, -0.000698080659819837, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.000379259851061545, 0.008930926532953, 0.00705386419964922, 0.00368061212439558, 0.00349628921563389, 0.00184085339117002, 0.00252589965226819, 0.00297244615576368, 0.0148402531245904, 0.0175789399147927, 0.022328701157952, 0.0188790551362496, -0.000253005586834373, 0.004, 0.004, 0.000842464090188183, -0.000417509287812737, 0.004, 0.004, 0.004, -0.000219265001748337, -0.000642050739424298, 0.004, 0.004, 0.004, -0.000240190513843333, -0.00243004105821436, 0.004, 0.004, 0.004, -0.000338893840987368, -0.00391503152590853, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.109993601332107, -0.109993601311115, -0.109993601459089, -0.0133670221877993, 0.00232441190071383, -0.000128105395900993, 0.0107338200028579, -0.000130417550122584, 0.138246889117962, -0.00013023903862643, 0.020993427908333, 0.0230762421048228, 0.172880843964545, 0.004, -0.00348352964218206, 0.00411608259520258, 0.004, 0.004, 0.004, 0.0240751417424512, -0.000487442887420718, 0.004, 0.004, 0.004, -0.000607923747538089, -0.000740448669514782, 0.004, 0.004, 0.004, -0.000643336986985209, -0.000158910769727784, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 2.1227776026512, 2.25992582522148, 2.06331304123723, -0.000199345727005251, -0.000135482960452555, -0.00381441967855232, -0.000157566209191, -0.000952614525146895, 0.00789035617904118, -0.00029433404969332, 0.0121285401742267, 0.138783444344786, 0.0529849519358424, 0.00860490424520297, 0.00658248537022571, 0.004, 0.004, 0.004, 0.0181498454883923, -0.000186243234500176, 0.004, 0.004, 0.004, 0.0161619894432673, -0.000116094266358386, 0.004, 0.004, 0.004, 0.263742291098308, -0.00254706434660157, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.0204331793450363, 0.0240088328087552, 0.0118546874297609, 0.00645316827422745, 0.0300920966583917, 0.100868412150879, 0.230459456758348, 0.034301607706075, 0.00787222228670539, 0.0322862931710811, 0.107292222613912, 0.103550562241627, -0.000132681071538273, -0.000922380548960452, 0.004, 0.004, 0.004, -0.00287874326257359, 0.00099663634834937, 0.004, 0.004, 0.004, -0.00128150821598214, 0.00570938044135608, 0.004, 0.004, 0.004, -0.00208780047793811, -0.00180873055592343, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.000239936641638167, -0.000287603387052978, -0.00429263576586227, -0.000371451495500384, -0.00184959318433455, -0.000142838210890787, -0.000179925409573789, 4.68893898482993E-05, -0.000258226041305802, 0.0204605293928246, 0.00618641280580659, 0.0100777004107312, 0.00514965369473394, -0.000295356616052253, 0.00781644091887058, 0.00139384322772456, 0.00577230392273254, 0.00139463045314927, -0.00379759219133755, 0.00311785033020844, -0.000108243724912957, 0.00420175495115992, -0.00016454429682426, -0.00156292493154358, -0.000279465662044163, -0.000107542649539694, 0.0495656136591202, -0.00718763061288769, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.0957602894247488, 0.088505252712156, 0.084539296232394, -0.000326593022090893, 0.00910414712742187, 0.0238844279293658, -0.000214358210562323, 0.0125287260866412, 0.0160594427311772, 0.0117496109222664, 0.0230134245178559, -0.00044087903132657, 0.0462713370533124, -0.000115491702336862, -0.000157492029160331, 0.00308867046785184, -0.000334734023139097, 0.000305104920306903, -0.00442061855034225, -0.000586404099713766, -0.000246446717356828, -0.00274095313668139, -0.000334325688944815, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.0220838810096625, -0.000505982726252018, -0.00077412283945304, -0.00047165995860855, -0.000161014859172399, -0.00545669272342152, 0.0164948802518483, 0.0124610574303309, 0.0303527412281259, 0.046304097235554, -0.000112019943446957, 0.0178629056426194, 0.00779302599967594, 0.0108021572160552, 0.0398937599329156, -0.000215427682184859, 0.0303404569581742, 0.0565081117677152, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.802517658984342, -0.00794287927733691, -0.0442200662873663, -0.000112885343174783, -0.000199713996031243, -0.000211692984338995, 0.000337909961358949, -0.000123876763619046, 0.00134278687038659, -0.000220368768960302, 0.00121832676286868, -0.000138440355803643, 0.0517570250183169, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004 };
			//			double[] paramTimeScalingVolParam = { 0.09, 0.09, 0.09, 0.09, -0.00190097787134732, -0.00419590120019691, -0.00442154435401484, -0.00424846567608874, 0.00137712555277076, -0.00424161240209748, -0.00432352128920968, -0.00484452818505744, -0.00350643269038414, 0.00410434441284486, -0.00338337893789635, -0.00223729829039029, 0.00143928175324461, 0.00156279993287448, 0.000882219669933377, 0.00087858922202642, 0.00202929037830635, 0.00744927957417381, -0.00500027137731493, -0.00323844129206122, -0.00609572360262511, -0.00238673519209321, -0.00183016167068025, 0.00745938347305667, 0.00144980839497321, 0.00149287969060239, 0.00156692679334085, 0.00161356810305676, 0.00141819354173748, -0.00522770198740843, 0.0036530064269786, 0.00946242325084313, 0.00681123661326154, -0.000941535930111427, -0.00699908077691986, 0.0135513446137497, 0.0185696857226379, 0.0311508391374471, 0.0311475798650828, 0.0295459938752459, 0.0118950344234283, 0.0105521443618978, 0.00980639635012437, 0.0108691036216198, -0.00103312659271807, -0.00132598913645182, -0.00136167199879508, -0.000491041445734623, -0.00684802580642085, -0.00716499255123125, -0.00716743596882224, -0.00795066952891517, -0.00805118074768557, -0.00798767354753764, -0.00774633615369594, -0.00857894105711807, 0.00483165224609379, 0.00197872595984947, 0.0028023031548291, 0.000211833309988378, -0.000601559967557108, 0.0123740405675009, 0.0157641514894444, -0.00117198328024826, -0.0055900820896187, -0.00458056622119841, -0.00532030127482883, -0.00671334528423159, -0.00900000000000005, -0.0072165363863634, -0.00581429135569024, -0.00657963421007124, -0.00833817361502568, -0.00478673575456156, -0.00679331347824799, -0.00865192464985526, -0.0013609027615098, -0.00215243433265754, 0.00563862962962574, 0.00289233572318153, -0.00322657070666991, -0.00319136535784423, -0.0033238750917151, -0.00333722755195808, -0.0073800093704834, -0.00559847211093028, -0.00648023650509543, -0.00681142626402021, -0.00562812498060651, -0.00386429169683736, -0.00543562398267425, -0.00313826587108536, -0.00610611568918017, -0.00804128721333455, -0.00598595598964749, -0.00518278688729338, -0.00158056604955106, 0.00234465373660043, -0.00900000000000005, -0.00900000000000005, -0.00733958059615872, -0.00769452205404235, -0.00900000000000005, -0.00899632218600572, -0.00701161052361897, -0.00858620774498291, -0.00746237354977296, -0.00900000000000005, -0.00900000000000005, 0.000596741960370934, -0.00900000000000005, -0.00900000000000005, -0.00892789575377719, -0.00893796347537232, -0.00893430849298908, -0.00900000000000005, -0.00900000000000005, 0.00330093412727023, 0.00281288884742537, 0.00226704069057603, -0.00509805989916486, -0.00509805989916145, -0.00508181743208837, -0.00508181743208865, -0.00548315009241264, -0.00548315009235892, -0.00549506202667203, -0.00549506202671153, 0.00345055618151803, 0.00345055618141543, 0.00361189808030076, 0.00361189808037948, -0.00282419265072178, -0.00282419265053164, -0.00242106499133541, -0.0024210649911302, -0.00813661265709669, -0.00813661265707708, -0.00813566744348236, -0.00813566744347696, -0.00883110041039373, -0.00883110041039373, -0.0088314896350738, -0.00883148963507437, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, -0.008681301670874, -0.008681301670874, -0.00868239665752185, -0.00868239665752213, -0.00892387656210701, -0.00892387656210729, -0.00892403326119904, -0.00892403326119904, 0.004, -0.494432859305378, -0.494432859006778, -0.494432859307916, -0.00157456511415939, 2.23875606682757E-06, 0.00271980468129379, 0.00452718662971476, 0.00549849694726185, 0.0109955211326429, 0.00333791751309602, 0.00197424400667448, 0.00141821313613722, -0.00188531949594071, 0.004, 0.004, 0.004, 0.004, -0.000141069120803055, 0.004, 0.004, 0.004, 0.004, -0.000387912613996856, 0.004, 0.004, 0.004, 0.004, -0.000270490730546011, 0.004, 0.004, 0.004, 0.004, -0.000938307181808428, 0.004, 0.004, 0.004, 0.004, -0.00113333707074173, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 1.10646188747636, 1.10646188725494, -0.000196522038430659, -0.000155975866775778, 0.00392795413904269, 0.00734487029453608, 0.00860651393058887, 0.0115870658166438, 0.00975035913235195, 0.0127383479585874, 0.0153886736255186, 0.000730110647696436, 0.004, 0.004, 0.004, 0.004, -0.000209317243014865, 0.004, 0.004, 0.004, 0.004, -0.000225865837606041, 0.004, 0.004, 0.004, 0.004, -0.000314735418794094, 0.004, 0.004, 0.004, 0.004, -0.000546643602310953, 0.004, 0.004, 0.004, 0.004, -0.00437598358073981, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.00389846213461929, 0.00375785901238129, 0.00384249670648788, 0.00487114836268511, 0.00440679917267407, 0.00326762574127898, 0.00363782719012919, 0.0017815977459734, 0.00459365612016309, 0.00337822866009006, 0.00148885067299307, 0.00347396860328752, -0.000150015736133534, 0.004, 0.004, 0.004, 0.00174006668078972, 0.004, 0.004, 0.004, 0.004, 0.00184684653565276, 0.004, 0.004, 0.004, 0.004, 0.00489575080287115, 0.004, 0.004, 0.004, 0.004, 0.000200266529267268, 0.004, 0.004, 0.004, 0.004, -0.000697283865190439, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.000915431872142883, 0.00923887771259087, 0.00565390119937938, 0.00433394038007836, 0.00318721561418647, 0.00340972323364204, 0.0042218004152189, 0.00453971274964544, 0.00461983538564253, 0.00473589020429284, -0.00015508535585088, 0.0101423106572126, -0.000200687523000682, 0.004, 0.004, 0.00192679387294915, -0.000170668681166593, 0.004, 0.004, 0.004, 0.00134712999999713, -0.000262884611492943, 0.004, 0.004, 0.004, 0.0059113377725755, -0.00194419654256857, 0.004, 0.004, 0.004, 0.000605696485271024, -0.000666125818866443, 0.004, 0.004, 0.004, -0.000848820207580652, -0.00202603929390789, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.00421963564488254, 0.0197104126080123, -0.000264471542406444, 0.00285450932108113, 0.00430810186815455, 0.00497192195505427, 0.00520685942844218, 0.00513010617482125, 0.00756671013100785, -0.000156210177176093, -0.000167385712828239, 0.0235158289275841, -0.00446261437531541, 0.004, -0.000137292454515056, -0.000113727761598494, 0.004, 0.004, 0.004, 0.00195892418925122, -0.00243036614907955, 0.004, 0.004, 0.004, -0.00012589615196831, -0.00149689910704691, 0.004, 0.004, 0.004, 0.00292329225679398, -0.000992719687024317, 0.004, 0.004, 0.004, -0.000578660129118478, -0.00154971276165391, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.00491703643427579, 0.0027684447711612, 0.00494388358312357, 0.00656755627714717, 0.0072744071894234, 0.00790547012140927, 0.00695750881528198, 0.00795891182167572, 0.0014978271803415, 0.00648723455910296, 0.00838590326906171, 0.0503754930052234, 0.133593434436975, 0.00118114672022288, -0.000111966052028284, 0.004, 0.004, 0.004, 0.00297476294366878, -0.000616551518766928, 0.004, 0.004, 0.004, 0.00469404328965833, -0.000769862434642523, 0.004, 0.004, 0.004, -0.00015699987838295, 0.00883835934535913, 0.004, 0.004, 0.004, 0.0203198736019049, -0.00152105472223549, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.00162485948842415, 0.00719046836982704, 0.0117192340095115, 0.00650780733684163, 0.00520715204910745, 0.00626209709665451, 0.00528343573105177, 0.00241289355579578, 0.00222531803241417, 0.00485856309858356, -0.000137992232836665, 0.0550687163437821, 0.0027759596708017, -0.000130489857128356, 0.004, 0.004, 0.004, -0.00105027521524037, 0.00421093450453416, 0.004, 0.004, 0.004, -0.000246495319370642, 0.00753000861981105, 0.004, 0.004, 0.004, -0.000455549647551561, 0.00391242953135605, 0.004, 0.004, 0.004, -0.00506357089028416, -0.00112587601697093, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.395057779279837, 0.13207787597752, 0.122758184730706, -0.000787701729946966, -0.000136773490015179, -0.000326350252038345, -0.000160577124974238, 0.00588373066998627, -3.31833880405605E-05, 0.0660387206473205, 0.119329549451619, 0.000906861904564925, -0.000460157995175247, 0.004, 0.004, 0.004, 0.00453816261372077, -0.00100206634666101, 0.004, 0.004, 0.004, 0.00403277235846243, -0.00119057735244476, 0.004, 0.004, 0.004, 0.00277260063373411, -0.000127162097771313, 0.004, 0.004, 0.004, -0.000168637450522381, -0.000698080659819837, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.000379259851061545, 0.00893137919739513, 0.00705284016395368, 0.00368073406972097, 0.0034956761227613, 0.00184058607352815, 0.00252516399467623, 0.00297204255194926, 0.0148402172608644, 0.0175832833588252, 0.0223320778080915, 0.0188777605470357, -0.000253005586834373, 0.004, 0.004, 0.000841583899513429, -0.000417509287812737, 0.004, 0.004, 0.004, -0.000219265001748337, -0.000642050739424298, 0.004, 0.004, 0.004, -0.000240190513843333, -0.00243004105821436, 0.004, 0.004, 0.004, -0.000338893840987368, -0.00391503152590853, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.109993601332107, -0.109993601311115, -0.109993601459089, -0.0133670221877993, 0.00232533098711986, -0.000128105395900993, 0.0107453564339516, -0.000130417550122584, 0.138395099933783, -0.00013023903862643, 0.0209977235806283, 0.0230810278121359, 0.172909238505893, 0.004, -0.00348352964218206, 0.00411921850179813, 0.004, 0.004, 0.004, 0.0240748343646457, -0.000487442887420718, 0.004, 0.004, 0.004, -0.000607923747538089, -0.000740448669514782, 0.004, 0.004, 0.004, -0.000643336986985209, -0.000158910769727784, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 2.12273167048654, 2.25987618959384, 2.06321871532434, -0.000199345727005251, -0.000135482960452555, -0.00381441967855232, -0.000157566209191, -0.000952614525146895, 0.00788651811622235, -0.00029433404969332, 0.0121286883339575, 0.138783807730769, 0.0529848287484363, 0.00860655315595881, 0.00658326575785168, 0.004, 0.004, 0.004, 0.0181462629716603, -0.000186243234500176, 0.004, 0.004, 0.004, 0.0161552083241596, -0.000116094266358386, 0.004, 0.004, 0.004, 0.262456115848721, -0.00254706434660157, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.0204819226289001, 0.024012604278405, 0.0118550153126997, 0.0064535217118774, 0.0300952145846644, 0.100862498205974, 0.230538198475846, 0.0343081545771976, 0.00787726308969092, 0.0322870625123467, 0.107302149191733, 0.103623531500667, -0.000132681071538273, -0.000922380548960452, 0.004, 0.004, 0.004, -0.00287874326257359, 0.000994889980365937, 0.004, 0.004, 0.004, -0.00128150821598214, 0.00570668438520093, 0.004, 0.004, 0.004, -0.00208780047793811, -0.00180873055592343, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, -0.000239936641638167, -0.000287603387052978, -0.00429263576586227, -0.000371451495500384, -0.00184959318433455, -0.000142838210890787, -0.000179925409573789, 4.63016593848466E-05, -0.000258226041305802, 0.0204618747505135, 0.00618620670839583, 0.0100783464438685, 0.00515018034252464, -0.000295356616052253, 0.00781687138111249, 0.00139518012251891, 0.00577306583924265, 0.0013945975309417, -0.00379759219133755, 0.00311611413267564, -0.000108243724912957, 0.00420151853255861, -0.00016454429682426, -0.00156292493154358, -0.000279465662044163, -0.000107542649539694, 0.0494666026096236, -0.00718763061288769, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.0957607252460275, 0.088483455269572, 0.0845247649813609, -0.000326593022090893, 0.00910552495524043, 0.0238856877877859, -0.000214358210562323, 0.0125286186996896, 0.016059010542125, 0.0117533664732769, 0.0230114719021376, -0.00044087903132657, 0.0462724608306735, -0.000115491702336862, -0.000157492029160331, 0.00308737194735963, -0.000334734023139097, 0.000306589627012157, -0.00442061855034225, -0.000586404099713766, -0.000246446717356828, -0.00274095313668139, -0.000334325688944815, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.0220783012301211, -0.000505982726252018, -0.00077412283945304, -0.00047165995860855, -0.000161014859172399, -0.00545669272342152, 0.0164884172627285, 0.0124614288101561, 0.0303504408839174, 0.0463007653907493, -0.000112019943446957, 0.0178631678582904, 0.00778323120978539, 0.0107547907172037, 0.0398234976401572, -0.000215427682184859, 0.030335113730973, 0.0562309521617786, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.80194450491443, -0.00794287927733691, -0.0442200662873663, -0.000112885343174783, -0.000199713996031243, -0.000211692984338995, 0.000335660150901678, -0.000123876763619046, 0.00133082165371709, -0.000220368768960302, 0.00122492195039399, -0.000138440355803643, 0.0517480181299594, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004, 0.004 };
			final double[] paramTimeScalingVolParam = { 0.089999984104462, 0.089993900990014, 0.0899901381500183, 0.0899845802184755, -0.00190194626729436, -0.00418576153191772, -0.00441813992248022, -0.00424528297453292, 0.00137189236415217, -0.00424012554558316, -0.00432221274145632, -0.00483647112245279, -0.00350345393803074, 0.00410390432565215, -0.00337651325449009, -0.00223245717648495, 0.00144781460237176, 0.00157384764851954, 0.000885233023712288, 0.000887727811942866, 0.00203665671181731, 0.00745080464532293, -0.0050077709913031, -0.0032360208915442, -0.00608705649088229, -0.00238780165327092, -0.00182870700628789, 0.00745598857460763, 0.00144844839078437, 0.0014903601419369, 0.00158601875086631, 0.00162150201575499, 0.00141187051230204, -0.00523312245340903, 0.0036498310707617, 0.00947164189040023, 0.00680900408902559, -0.000944907659866203, -0.00700426166316745, 0.0135575386635909, 0.0185659945912829, 0.0311569532311878, 0.0311491952246717, 0.0295444715838745, 0.0119027392354009, 0.0105503815107718, 0.00981187720991385, 0.0108686092521016, -0.00102609863041806, -0.00132404828297197, -0.00136659746062036, -0.000492807134885282, -0.00684581203436721, -0.007173834474506, -0.00716186758490906, -0.00794955864124858, -0.00804588974458283, -0.00798414268222274, -0.0077513603346857, -0.0085821316837881, 0.00483146372085457, 0.00198405481146835, 0.00280073798027928, 0.000214532758988497, -0.000606143302083523, 0.0123661513483746, 0.015763826965351, -0.0011732066931269, -0.00558653548706701, -0.00458230269486747, -0.00531797169359265, -0.00671516758264544, -0.00899400774658346, -0.00721681741356519, -0.00581835092251395, -0.00657903985460052, -0.00834198448965509, -0.0047904979265391, -0.00679363669166775, -0.00863808953844284, -0.00135870327632147, -0.00216134597150485, 0.00563484601100739, 0.0028891889237083, -0.00322788036100675, -0.00320578170793069, -0.00332017825431748, -0.00333796003083108, -0.00738260779548298, -0.00558566964708603, -0.00648562153961535, -0.00682239785630116, -0.00562208147942271, -0.00386778437264297, -0.00544935588956036, -0.00313993895400358, -0.00610512461105912, -0.00804633461716719, -0.00598433956038178, -0.00518802497554446, -0.00158767012422657, 0.00233171469230825, -0.00900000000000005, -0.00900000000000005, -0.00734040441362481, -0.00769899523590027, -0.00900000000000005, -0.00899242077496552, -0.0070058160828367, -0.008586826201944, -0.00746747216087698, -0.00899909885981117, -0.00900000000000005, 0.000598073267528036, -0.00900000000000005, -0.00899982203573159, -0.00892659602533228, -0.00894688063213806, -0.00892911465011963, -0.00900000000000005, -0.00900000000000005, 0.00330669971399458, 0.00280840467593776, 0.00226621963642401, -0.00509745871525581, -0.00509885341076341, -0.00508907670217468, -0.00508074462799044, -0.00547879989150885, -0.00548091270161478, -0.00549441668043954, -0.00549625007676212, 0.00344414029016832, 0.00344522046529704, 0.00360607833290572, 0.00361465017196252, -0.00282130455451834, -0.00281457297773471, -0.00241092871079786, -0.00242527597238421, -0.00813534074291396, -0.00814126990976035, -0.00813025770034016, -0.00813892371500514, -0.00882396620502618, -0.0088297104920116, -0.00883612513126053, -0.00883042201502831, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, -0.00900000000000005, -0.00868628673246206, -0.00867012748975867, -0.0086912365589896, -0.00868687066957818, -0.00892331104406707, -0.00891576800786055, -0.0089161919184329, -0.00891980289420473, 0.00399780701070502, -0.49444126851566, -0.494435363513558, -0.494432553650804, -0.00157162236828548, -1.0992842756501E-07, 0.00272063643065594, 0.00452985810651097, 0.00549556049936806, 0.010991441414052, 0.0033474290063268, 0.00197733376151605, 0.00142219063203068, -0.00188288841850331, 0.00399843012476706, 0.00399379216572144, 0.00399458175772137, 0.0039971511939114, -0.000139025663220937, 0.00400315391709513, 0.00399030728704296, 0.00400148539584672, 0.00400591414162075, -0.000406592063214253, 0.00400197814251451, 0.00400602207829323, 0.00398776446932046, 0.00400556894803626, -0.000266370310796592, 0.00400056550853854, 0.00399304195663744, 0.00400527909708902, 0.00400121588526432, -0.000929427453239216, 0.00401105627197073, 0.00399766713487625, 0.00400013819560811, 0.00399622054236744, -0.00112420144779585, 0.00400802693288153, 0.00399932667960264, 0.00399648950953365, 0.00399573250122974, 0.00399424624289481, 0.00400216759709767, 1.10646555687144, 1.1064589736536, -0.000183472135494369, -0.000172993390802076, 0.00393408908741404, 0.00733956560440598, 0.00861480187466026, 0.0115934582051673, 0.00974304003593672, 0.0127365325344546, 0.0153987434538555, 0.000725046076883778, 0.00399840170459536, 0.00399422017558321, 0.00401026169985445, 0.00400762949863013, -0.00021029126404189, 0.00400859984450043, 0.00400679024797902, 0.00399663219188377, 0.00399790837755902, -0.000228518151922764, 0.00400006011119813, 0.00399769112283165, 0.00400553399930391, 0.00400162107476618, -0.000313082716650621, 0.00400456292147056, 0.00400556968611433, 0.00400710530036603, 0.00400606294385858, -0.000554947624728905, 0.00398904569272785, 0.00399589266442923, 0.00400340781910198, 0.00399741813587968, -0.00437608771398979, 0.00400330296163454, 0.00399530784908118, 0.00399533951401766, 0.00401481261039315, 0.00400048819256143, 0.00400367416024569, 0.00389234920206552, 0.0037521667485533, 0.00384664174489545, 0.00486940225863406, 0.0044068222621924, 0.00326066030660317, 0.00363875022535542, 0.00177095962023269, 0.00458638200652051, 0.00338891727970305, 0.00148434141913449, 0.00347731515803139, -0.000150100738647379, 0.00399957477319647, 0.00399470099720745, 0.00400421187177353, 0.00173882579289788, 0.00400354292405236, 0.00399889648078441, 0.00401179116205208, 0.0039982703765957, 0.00184672898920138, 0.00400930833812459, 0.00400019630145089, 0.00399990110508255, 0.00401240326399964, 0.00489917875743417, 0.00400551781725645, 0.00399082291481226, 0.00400004193694341, 0.00401312185941792, 0.000203135587026336, 0.0039995495373251, 0.00399836672469344, 0.00400691498748744, 0.00400293211471976, -0.000696076651915027, 0.00399916077540136, 0.00399308501871636, 0.00399901046641418, 0.00400247100334813, 0.00398855232134889, 0.00399680411411167, -0.000913309187310931, 0.00923578351251054, 0.00565112096116737, 0.00433051150215994, 0.00318602582607875, 0.00341003341852321, 0.00421255887301701, 0.00453322303618831, 0.00462286524899222, 0.00473119716680763, -0.000143174718108278, 0.0101437528977208, -0.000201776972129086, 0.00399235104769332, 0.0039865400408277, 0.0019279212352188, -0.000177113086952985, 0.00400715958323341, 0.00400094279126635, 0.00400946124702011, 0.00134856050417628, -0.000268532793612923, 0.00400729868163326, 0.00400653049788387, 0.00399007195125664, 0.00590525535263523, -0.00194353869264568, 0.00399871791866517, 0.00400130118780169, 0.00400417116305861, 0.000610252068293993, -0.000658835336057235, 0.00400172899208264, 0.00400421144376383, 0.00400958732940494, -0.000850420437484669, -0.00203608813573197, 0.00399157310080955, 0.00399903174894247, 0.00400422026915309, 0.00399169734265986, 0.00400533483896996, 0.00399419664259638, -0.00422803682938843, 0.0197084555975708, -0.000257593581581099, 0.00285166866777017, 0.00431279319786545, 0.00498496514789032, 0.00520380834306612, 0.00513177431355671, 0.00756916987994435, -0.000150255252009589, -0.000161352954684722, 0.0235210099733678, -0.00446925775867501, 0.00399332548653183, -0.000147760885851023, -0.000115562257972509, 0.00399280420714138, 0.00400379600851552, 0.00399446397969584, 0.00195911180272385, -0.00243256388308474, 0.00400346605430943, 0.00400710482820076, 0.00400124996517238, -0.000132541770384485, -0.00148826895690495, 0.00399368178444763, 0.00400336715447685, 0.00399082194221145, 0.00291659809737315, -0.000997885402463235, 0.00399742042610546, 0.00400825592823958, 0.0040098736062748, -0.000580220365986495, -0.00155453164551143, 0.00400014563648007, 0.00400999864235201, 0.00399469503601508, 0.00400214756002654, 0.00398799876007819, 0.00400902506774626, 0.00492262585008514, 0.00277239054147526, 0.00494114891687885, 0.0065649394799405, 0.00727776153467931, 0.00790252348029478, 0.00695721366990139, 0.00795020828580415, 0.00150054473476394, 0.00648605765855751, 0.00837910943709391, 0.0503804515639715, 0.133603540055207, 0.00118074955960002, -0.000106384002954701, 0.00399836593326441, 0.00399975066350749, 0.00399971285379956, 0.00297815318540599, -0.000617569350050213, 0.00399729439950767, 0.00399684067077337, 0.00400426817060598, 0.00468809256081054, -0.000779672522481478, 0.0039983612720994, 0.00399404890081026, 0.0040052127607445, -0.000169466645079028, 0.00883652411877378, 0.00400355399099511, 0.00400303373148152, 0.00399891042225148, 0.0203126128852496, -0.00152706291335349, 0.00399781345209943, 0.00400160667802596, 0.00398755092520426, 0.00399656380252021, 0.0040065239538153, 0.00400208193358509, -0.00162699486450058, 0.0071920195408265, 0.011707060950816, 0.00650686908601564, 0.0052076550962668, 0.0062544184902681, 0.0052908532345961, 0.00241469318064073, 0.00222474971498637, 0.00484788868126099, -0.000141868322674157, 0.0550729366816687, 0.00277821696305189, -0.000122669337107752, 0.00400017267854154, 0.00399348330852289, 0.00399176498059539, -0.00103891696621039, 0.00420400979621641, 0.00400231524279263, 0.00399742020931394, 0.00400220918297029, -0.000227923751275513, 0.0075325070103828, 0.00400537031815308, 0.00400204547204036, 0.00399112808742699, -0.00046611510135596, 0.00390523645274286, 0.00400431064682218, 0.00398457478368309, 0.00399895872686772, -0.0050611782462024, -0.00112000274369474, 0.00400526191452558, 0.00399347179978057, 0.00399330079541019, 0.00400648127725935, 0.00400027795693062, 0.00399168552925152, 0.395067162378213, 0.132072956741606, 0.122754417939007, -0.000784196354458672, -0.000138235987104404, -0.000311900849230872, -0.000162042552188412, 0.00588356086811242, -3.53939689548158E-05, 0.066038021449398, 0.119325688692816, 0.000906737263871121, -0.000466309868929055, 0.00400061739921156, 0.00399513155195567, 0.00401036898056119, 0.00454105388651191, -0.00101137382471542, 0.00399738898788433, 0.00399497494402889, 0.00400176967863601, 0.00403892033066293, -0.0011872796669467, 0.0039980410438325, 0.00399447871671042, 0.00399433725267537, 0.00277273799570392, -0.000134031849509847, 0.0040079675265262, 0.00400770484702454, 0.00400433907258317, -0.000163643351428294, -0.000700705942142426, 0.00399994669038151, 0.00400662301876197, 0.00399698763069894, 0.0040045319961418, 0.00400617855369554, 0.00400214462362018, -0.000363542302689785, 0.00893214697607924, 0.0070464782835267, 0.00367976945506546, 0.00350338431221762, 0.00184027356053214, 0.0025221124976663, 0.00296684233761205, 0.0148423342382019, 0.0175859170330717, 0.0223329216288011, 0.01887797690199, -0.000247208346609389, 0.00399601535296208, 0.00399235601880748, 0.00084025610702231, -0.000424856316275215, 0.00399155251086519, 0.00401253333980356, 0.00399421004887061, -0.000214548549358204, -0.000646941159642577, 0.00401716178748541, 0.00399104212050509, 0.00399880774696967, -0.000241494328561845, -0.00242416774214484, 0.00399611863726235, 0.00399517086295241, 0.00400683242425706, -0.000334639655188627, -0.0039064039194833, 0.00399856168092896, 0.00400096082849641, 0.00400912112765976, 0.0039998414026744, 0.00399039750001721, 0.00400099640449435, -0.1099837964771, -0.109992724444491, -0.109982130083171, -0.0133631553656129, 0.00232518938304419, -0.000136640315476804, 0.0107353281194772, -0.000130294176112994, 0.138390042682926, -0.000132434303359212, 0.0210006430642306, 0.023084426210275, 0.172903333461268, 0.00399544822814204, -0.00348744652863597, 0.00412224730156018, 0.00399966724976868, 0.0039957643035143, 0.00399392009350188, 0.0240772359623621, -0.000488155517869532, 0.00399592978039879, 0.0039945597546101, 0.00401348404954144, -0.000599754212627483, -0.000745773028253532, 0.00399402869504536, 0.0039926803829258, 0.00400048116720232, -0.00065478589618655, -0.000158698520876535, 0.00400223156030028, 0.00399112005153314, 0.00399086095225361, 0.00399522588766668, 0.00399936374449912, 0.00399587987802305, 2.12272740176158, 2.25987913179547, 2.06323161986756, -0.000191063835124816, -0.000133332203657637, -0.0038183660917851, -0.000167037606899628, -0.000957237414739019, 0.00789026592113766, -0.000291587678961229, 0.0121200458837448, 0.138776553946212, 0.0529798181862248, 0.00861624640983701, 0.00659314981759569, 0.00399453979500096, 0.00400948094275374, 0.00400293967934368, 0.0181373891364167, -0.000183887927706963, 0.00400276465289603, 0.0039956313171267, 0.00400842205967217, 0.0161415498752904, -0.000115242887143754, 0.00400243459918743, 0.0040056495939698, 0.00400049928357782, 0.262447109876817, -0.00255194050012678, 0.00400487633013106, 0.00400303035185663, 0.00399860419723923, 0.00400192782997921, 0.0039909282840045, 0.00399928813321253, 0.0204772265101831, 0.0240188966562148, 0.0118544490210018, 0.00645311318916334, 0.0301017508284892, 0.100872004994125, 0.230535326396742, 0.034300629876816, 0.00787847919541187, 0.0322838852873132, 0.107294370184529, 0.1036153884209, -0.000128620412551977, -0.000930224875503104, 0.00400476051855667, 0.00400541342922024, 0.00400543624016981, -0.00287911155593107, 0.000998972972493756, 0.00399745965300019, 0.00401111709921132, 0.00400369664569743, -0.00127272245202417, 0.00570340038128389, 0.00399333692621244, 0.00400614158893996, 0.00399206214794711, -0.00209770779169251, -0.00181059704266888, 0.00400578848064937, 0.00399705125314804, 0.00399758938088308, 0.00400750543112501, 0.00400117956224315, 0.00398404819314136, -0.000235431722605949, -0.000289292192424331, -0.00429710724460976, -0.000375550335955473, -0.00185284868248381, -0.000136703019546342, -0.000180918925305957, 4.25478459443189E-05, -0.00025528090814467, 0.0204601903521506, 0.00619183289714929, 0.0100805530923725, 0.00516721617628374, -0.000295708021707873, 0.00780654577675436, 0.00139290080196226, 0.00576136809411688, 0.00139411475532367, -0.00380109298866226, 0.00310557423509122, -0.000120722330837101, 0.00419903403910578, -0.000161175784422497, -0.00156662410736716, -0.000276097439916945, -0.000117442859907528, 0.0494671574014293, -0.00718835627261953, 0.00399851965168418, 0.00401190837952466, 0.00400046777564649, 0.00399480087060629, 0.00400584058570769, 0.00400366423364113, 0.0957676093689149, 0.0884847080503522, 0.0845253137912114, -0.000338574360196422, 0.0091092601082336, 0.0238923996167416, -0.000204205856107072, 0.0125222501519829, 0.0160639233073764, 0.0117649125168953, 0.0230107053736233, -0.000439750014219888, 0.0462792733656938, -0.000119355962821415, -0.000159608913319949, 0.00307538346688976, -0.000328344480056022, 0.000300470644286628, -0.00441702839225111, -0.000583707014139153, -0.00024462290746606, -0.00274451299918907, -0.000337133910062902, 0.00400337294585505, 0.00400371580320374, 0.00399127654149084, 0.0040064423450648, 0.00400691109083355, 0.00401868428269433, 0.0220804167549368, -0.000504612030822202, -0.000780331256426177, -0.000473492077473064, -0.000156333078463891, -0.00545762407740487, 0.0164847202518829, 0.0124618287699428, 0.0303514569854341, 0.0462991269495574, -0.000107265204768476, 0.0178558518867034, 0.00778034365730244, 0.0107502111616525, 0.0398244686141763, -0.000207874137143602, 0.0303335606057633, 0.0562231598862078, 0.00399258154063248, 0.00399088760461249, 0.00400142119514921, 0.00400857366132433, 0.00400269036461805, 0.00400187480121562, 0.80194315711381, -0.00794602438985766, -0.0442260460173735, -0.00012057451027344, -0.000198620086347637, -0.000213891717274347, 0.000326016980683264, -0.000121825380544764, 0.00132024127437892, -0.000225713972805136, 0.00122350909091257, -0.000147251264995611, 0.0517474847755777, 0.00400374927699573, 0.00398662227835639, 0.0040041452676069, 0.00399513349920769, 0.00400601687723186, 0.00400444498317761, 0.00399481456922337, 0.00400802003555727, 0.00400640833329286, 0.00398824559189416, 0.00399382035565428, 0.00399703875652569, 0.00400315418170189, 0.00400146333002795, 0.00399968680594135, 0.00399288512227374, 0.00399868258926832, 0.00399417814824357, 0.00400187122505196, 0.00399916195565957 };
			//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelTimeHomogenousPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 0.50, 1.00, 2.00, 30.00 ), new double[] { 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0 });
			//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelTimeHomogenousPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 ), new double[] { 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0, 0.20/100.0 });
			//LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 1.00, 2.00, 3.00, 4.00, 5.00, 6.00, 7.00, 8.00, 9.00, 10.00, 11.00, 12.00, 14.00, 15.00, 16.00, 17.00, 18.50, 20.00, 22.50, 25.00, 30.00 ), new TimeDiscretizationFromArray(0.00, 1.00, 2.00, 3.00, 4.00, 5.00, 6.00, 7.00, 8.00, 9.00, 10.00, 11.00, 12.00, 14.00, 15.00, 16.00, 17.00, 18.50, 20.00, 22.50, 25.00, 30.00 ), 0.40 / 100);
			final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, optionMaturityDiscretization, timeToMaturityDiscretization, 0.40 / 100, true);
			//			LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 30, 1.0), new TimeDiscretizationFromArray(0.00, 40, 1.0), volatilityFineInitial);
			final LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, 0.05, false);
			// Create a covariance model
			AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20/100.0, 0.05/100.0, 0.10, 0.05/100.0, 0.10} );
			covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray, liborPeriodDiscretization, volatilityModel, correlationModel);

			final TimeDiscretization tenorTimeScalingDiscretization = new TimeDiscretizationFromArray(0.0, 40.0, 0.25, ShortPeriodLocation.SHORT_PERIOD_AT_START);
			final double[] tenorTimeScalings = new double[tenorTimeScalingDiscretization.getNumberOfTimes()];
			Arrays.fill(tenorTimeScalings, 0.0);
			final TermStructureTenorTimeScaling tenorTimeScalingModel = new TermStructureTenorTimeScalingPicewiseConstant(tenorTimeScalingDiscretization, tenorTimeScalings);

			// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
			final AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModelParametric, 0.0, false);

			TermStructureCovarianceModelParametric termStructureCovarianceModel;
			termStructureCovarianceModel = new TermStructCovarianceModelFromLIBORCovarianceModelParametric(tenorTimeScalingModel, covarianceModelParametric);

			termStructureCovarianceModel = termStructureCovarianceModel.getCloneWithModifiedParameters(paramTimeScalingVolParam);
			double[] bestParameters = null;
			for(int i = 0; i<1; i++) {
				if(i>0) {
					termStructureCovarianceModel = termStructureCovarianceModel.getCloneWithModifiedParameters(bestParameters);
				}


				// Set model properties
				final Map<String, Object> properties = new HashMap<>();

				final Double accuracy = 1E-12;
				final int maxIterations = 400;
				final int numberOfThreads = 6;
				final OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

				final double[] parameterStandardDeviation = new double[termStructureCovarianceModel.getParameter().length];
				final double[] parameterLowerBound = new double[termStructureCovarianceModel.getParameter().length];
				final double[] parameterUpperBound = new double[termStructureCovarianceModel.getParameter().length];
				Arrays.fill(parameterStandardDeviation, i==0 ? 0.0020/100.0 : 0.2/100.0);
				Arrays.fill(parameterLowerBound, Double.NEGATIVE_INFINITY);
				Arrays.fill(parameterUpperBound, Double.POSITIVE_INFINITY);

				//				optimizerFactory = new OptimizerFactoryCMAES(accuracy, maxIterations, parameterLowerBound, parameterUpperBound, parameterStandardDeviation);

				// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
				final Map<String, Object> calibrationParameters = new HashMap<>();
				calibrationParameters.put("accuracy", accuracy);
				calibrationParameters.put("brownianMotion", brownianMotion);
				calibrationParameters.put("parameterStep", i == 0 ? new Double(1E-6) : new Double(5E-5) );
				calibrationParameters.put("optimizerFactory", optimizerFactory);
				properties.put("calibrationParameters", calibrationParameters);

				System.out.println("Number of covariance parameters: " + termStructureCovarianceModel.getParameter().length);

				/*
				 * Create corresponding LIBOR Market Model
				 */
				final TimeDiscretization liborPeriodDiscretizationFine = new TimeDiscretizationFromArray(0.0, 40.0, 0.0625, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				final TimeDiscretization liborPeriodDiscretizationMedium = new TimeDiscretizationFromArray(0.0, 40.0, 0.25, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				final TimeDiscretization liborPeriodDiscretizationCoarse = new TimeDiscretizationFromArray(0.0, 40.0, 1.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				final TimeDiscretization liborPeriodDiscretizationCoarse2 = new TimeDiscretizationFromArray(0.0, 40.0, 5.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				final TimeDiscretization liborPeriodDiscretizationNormal = new TimeDiscretizationFromArray(0.0, 40.0, 1.0, ShortPeriodLocation.SHORT_PERIOD_AT_START);
				final TermStructureModel liborMarketModelCalibrated = new LIBORMarketModelWithTenorRefinement(
						//					new TimeDiscretization[] { liborPeriodDiscretizationNormal },
						//					new Integer[] { 200 },
						new TimeDiscretization[] { liborPeriodDiscretizationMedium, liborPeriodDiscretizationCoarse, liborPeriodDiscretizationCoarse2 },
						new Integer[] { 4, 9, 200 },
						//					new TimeDiscretization[] { liborPeriodDiscretizationCoarse, liborPeriodDiscretizationCoarse2 },
						//					new Integer[] { 5, 200 },
						//					new TimeDiscretization[] { liborPeriodDiscretizationFine, liborPeriodDiscretizationMedium, liborPeriodDiscretizationCoarse, liborPeriodDiscretizationCoarse2 },
						//					new Integer[] { 4, 3, 9, 200 },
						curveModel,
						forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve),
						termStructureCovarianceModel,
						calibrationProducts.toArray(new CalibrationProduct[0]), properties);

				System.out.println("\nCalibrated parameters are:");
				final double[] param = ((LIBORMarketModelWithTenorRefinement) liborMarketModelCalibrated).getCovarianceModel().getParameter();
				//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
				for (final double p : param) {
					System.out.println(p);
				}
				bestParameters = param;

				final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModelCalibrated, brownianMotion);
				simulationCalibrated = new TermStructureMonteCarloSimulationFromTermStructureModel(liborMarketModelCalibrated, process);
			}
		}
		if(test == 1) {
			final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelPiecewiseConstant(timeDiscretizationFromArray, liborPeriodDiscretization, new TimeDiscretizationFromArray(0.00, 30, 1.0), new TimeDiscretizationFromArray(0.00, 40, 1.0), 0.40 / 100);
			final LIBORCorrelationModel correlationModel = new LIBORCorrelationModelExponentialDecay(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, 0.05, false);
			// Create a covariance model
			AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20/100.0, 0.05/100.0, 0.10, 0.05/100.0, 0.10} );
			covarianceModelParametric = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretizationFromArray, liborPeriodDiscretization, volatilityModel, correlationModel);

			// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
			final AbstractLIBORCovarianceModelParametric covarianceModelDisplaced = new DisplacedLocalVolatilityModel(covarianceModelParametric, 1.0/0.25, false /* isCalibrateable */);

			// Set model properties
			final Map<String, Object> properties = new HashMap<>();

			final Double accuracy = new Double(1E-8);
			final int maxIterations = 400;
			final int numberOfThreads = 2;
			final OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

			final double[] parameterStandardDeviation = new double[covarianceModelParametric.getParameterAsDouble().length];
			final double[] parameterLowerBound = new double[covarianceModelParametric.getParameterAsDouble().length];
			final double[] parameterUpperBound = new double[covarianceModelParametric.getParameterAsDouble().length];
			Arrays.fill(parameterStandardDeviation, 0.20/100.0);
			Arrays.fill(parameterLowerBound, Double.NEGATIVE_INFINITY);
			Arrays.fill(parameterUpperBound, Double.POSITIVE_INFINITY);

			//optimizerFactory = new OptimizerFactoryCMAES(accuracy, maxIterations, parameterLowerBound, parameterUpperBound, parameterStandardDeviation);

			// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
			final Map<String, Object> calibrationParameters = new HashMap<>();
			calibrationParameters.put("accuracy", accuracy);
			calibrationParameters.put("brownianMotion", brownianMotion);
			calibrationParameters.put("optimizerFactory", optimizerFactory);
			calibrationParameters.put("brownianMotion", new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 31415 /* seed */));
			properties.put("calibrationParameters", calibrationParameters);

			/*
			 * Create corresponding LIBOR Market Model
			 */
			final CalibrationProduct[] calibrationItemsLMM = new CalibrationProduct[calibrationItemNames.size()];
			for(int i=0; i<calibrationItemNames.size(); i++) {
				calibrationItemsLMM[i] = new CalibrationProduct(calibrationProducts.get(i).getProduct(), calibrationProducts.get(i).getTargetValue(), calibrationProducts.get(i).getWeight());
			}
			final TermStructureModel liborMarketModelCalibrated = new LIBORMarketModelFromCovarianceModel(
					liborPeriodDiscretization,
					curveModel,
					forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve),
					covarianceModelDisplaced,
					calibrationItemsLMM,
					properties);

			System.out.println("\nCalibrated parameters are:");
			final double[] param = ((AbstractLIBORCovarianceModelParametric)((LIBORMarketModelFromCovarianceModel) liborMarketModelCalibrated).getCovarianceModel()).getParameterAsDouble();
			//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
			for (final double p : param) {
				System.out.println(p);
			}

			final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModelCalibrated, brownianMotion);
			simulationCalibrated = new TermStructureMonteCarloSimulationFromTermStructureModel(liborMarketModelCalibrated, process);
		}
		else if(test == 2) {
			final TimeDiscretization shortRateVolTimeDis = new TimeDiscretizationFromArray(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00, 35.00, 40.00, 45.00 );
			final double[] shortRateVolatility = new double[shortRateVolTimeDis.getNumberOfTimes()];
			final double[] meanReversion = new double[shortRateVolTimeDis.getNumberOfTimes()];
			Arrays.fill(shortRateVolatility, 0.20 / 100);
			Arrays.fill(meanReversion, 0.15);

			final double[] initialParameters = new double[shortRateVolatility.length+1];
			System.arraycopy(shortRateVolatility, 0, initialParameters, 0, shortRateVolatility.length);
			initialParameters[initialParameters.length-1] = 0.15;

			final int maxIterations = 400;
			final double accuracy		= 1E-7;

			final double[] calibrationTargetValues = new double[calibrationProducts.size()];
			for(int i=0; i<calibrationTargetValues.length; i++) {
				calibrationTargetValues[i] = calibrationProducts.get(i).getTargetValue().getAverage();
			}

			final double[] calibrationWeights = new double[calibrationProducts.size()];
			for(int i=0; i<calibrationWeights.length; i++) {
				calibrationWeights[i] = calibrationProducts.get(i).getWeight();
			}

			final int numberOfThreadsForProductValuation = 2 * Math.min(2, Runtime.getRuntime().availableProcessors());
			final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

			/*
			 * We allow for 5 simultaneous calibration models.
			 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
			 * one model with 5 times the number of paths. In the case of an analytic calibration
			 * memory requirement is not the limiting factor.
			 */
			final int numberOfThreads = 5;
			final LevenbergMarquardt optimizer = new LevenbergMarquardt(initialParameters, calibrationTargetValues, maxIterations, numberOfThreads)
			{
				private static final long serialVersionUID = 7213979669076698360L;

				// Calculate model values for given parameters
				@Override
				public void setValues(final double[] parameters, final double[] values) throws SolverException {

					final double[] shortRateVolatility = new double[parameters.length-1];
					final double[] meanReversion = new double[parameters.length-1];
					System.arraycopy(parameters, 0, shortRateVolatility, 0, shortRateVolatility.length);
					Arrays.fill(meanReversion, parameters[parameters.length-1]);

					final ShortRateVolatilityModel volatilityModel = new ShortRateVolatilityModelAsGiven(
							shortRateVolTimeDis,
							shortRateVolatility,
							meanReversion);

					// Create a LIBOR market model with the new covariance structure.
					final LIBORModel model = new HullWhiteModel(
							liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModel, null);
					final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);
					final LIBORMonteCarloSimulationFromLIBORModel liborMarketModelMonteCarloSimulation =  new LIBORMonteCarloSimulationFromLIBORModel(model, process);

					final ArrayList<Future<Double>> valueFutures = new ArrayList<>(calibrationProducts.size());
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.size(); calibrationProductIndex++) {
						final int workerCalibrationProductIndex = calibrationProductIndex;
						final Callable<Double> worker = new  Callable<Double>() {
							@Override
							public Double call() {
								try {
									return calibrationProducts.get(workerCalibrationProductIndex).getProduct().getValue(liborMarketModelMonteCarloSimulation);
								} catch (final Exception e) {
									// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
									return calibrationProducts.get(workerCalibrationProductIndex).getTargetValue().getAverage();
								}
							}
						};
						if(executor != null) {
							final Future<Double> valueFuture = executor.submit(worker);
							valueFutures.add(calibrationProductIndex, valueFuture);
						}
						else {
							final FutureTask<Double> valueFutureTask = new FutureTask<>(worker);
							valueFutureTask.run();
							valueFutures.add(calibrationProductIndex, valueFutureTask);
						}
					}
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.size(); calibrationProductIndex++) {
						try {
							final double value = valueFutures.get(calibrationProductIndex).get();
							values[calibrationProductIndex] = value;
						}
						catch (final InterruptedException | ExecutionException e) {
							throw new SolverException(e);
						}
					}
					System.out.println(this.getRootMeanSquaredError() + "\t" + Arrays.toString(values));
				}
			};

			// Set solver parameters
			optimizer.setWeights(calibrationWeights);
			optimizer.setErrorTolerance(accuracy);
			final double[] parameterSteps = new double[initialParameters.length];
			Arrays.fill(parameterSteps, 1E-4);
			optimizer.setParameterSteps(parameterSteps);

			try {
				optimizer.run();
			}
			catch(final SolverException e) {
				throw new CalculationException(e);
			}
			finally {
				if(executor != null) {
					executor.shutdown();
				}
			}

			// Get covariance model corresponding to the best parameter set.
			final double[] bestParameters = optimizer.getBestFitParameters();

			System.out.println("\nCalibrated parameters are:");
			for (final double p : bestParameters) {
				System.out.println(formatterParam.format(p));
			}

			final double[] shortRateVolatilityCalib = new double[bestParameters.length-1];
			final double[] meanReversionCalib = new double[bestParameters.length-1];
			System.arraycopy(bestParameters, 0, shortRateVolatilityCalib, 0, shortRateVolatilityCalib.length);
			Arrays.fill(meanReversionCalib, bestParameters[bestParameters.length-1]);

			final ShortRateVolatilityModel volatilityModelCalibrated = new ShortRateVolatilityModelAsGiven(
					shortRateVolTimeDis,
					shortRateVolatilityCalib,
					meanReversionCalib);

			// Create a LIBOR market model with the new covariance structure.
			final LIBORModel modelCalibrated = new HullWhiteModel(
					liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModelCalibrated, null);
			final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(modelCalibrated, brownianMotion);
			simulationCalibrated =  new LIBORMonteCarloSimulationFromLIBORModel(modelCalibrated, process);
		}
		else if(test == 3) {
			final TimeDiscretization shortRateVolTimeDis = new TimeDiscretizationFromArray(0.00, 0.50, 1.00, 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00, 35.00, 40.00, 45.00 );
			final double[] shortRateVolatility = new double[shortRateVolTimeDis.getNumberOfTimes()];
			final double[] meanReversion = new double[shortRateVolTimeDis.getNumberOfTimes()];
			Arrays.fill(shortRateVolatility, 0.40 / 100);
			Arrays.fill(meanReversion, 0.15);

			final double[] initialParameters = new double[2*shortRateVolatility.length];
			System.arraycopy(shortRateVolatility, 0, initialParameters, 0, shortRateVolatility.length);
			System.arraycopy(meanReversion, 0, initialParameters, shortRateVolatility.length, shortRateVolatility.length);

			final int maxIterations = 400;
			final double accuracy		= 1E-7;

			final double[] calibrationTargetValues = new double[calibrationProducts.size()];
			for(int i=0; i<calibrationTargetValues.length; i++) {
				calibrationTargetValues[i] = calibrationProducts.get(i).getTargetValue().getAverage();
			}

			final double[] calibrationWeights = new double[calibrationProducts.size()];
			for(int i=0; i<calibrationWeights.length; i++) {
				calibrationWeights[i] = calibrationProducts.get(i).getWeight();
			}

			final int numberOfThreadsForProductValuation = 2 * Math.min(2, Runtime.getRuntime().availableProcessors());
			final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

			/*
			 * We allow for 5 simultaneous calibration models.
			 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
			 * one model with 5 times the number of paths. In the case of an analytic calibration
			 * memory requirement is not the limiting factor.
			 */
			final int numberOfThreads = 5;
			final LevenbergMarquardt optimizer = new LevenbergMarquardt(initialParameters, calibrationTargetValues, maxIterations, numberOfThreads)
			{
				private static final long serialVersionUID = 4823173102012628165L;

				// Calculate model values for given parameters
				@Override
				public void setValues(final double[] parameters, final double[] values) throws SolverException {

					final double[] shortRateVolatility = new double[parameters.length/2];
					final double[] meanReversion = new double[parameters.length/2];
					System.arraycopy(parameters, 0, shortRateVolatility, 0, shortRateVolatility.length);
					System.arraycopy(parameters, shortRateVolatility.length, meanReversion, 0, shortRateVolatility.length);

					final ShortRateVolatilityModel volatilityModel = new ShortRateVolatilityModelAsGiven(
							shortRateVolTimeDis,
							shortRateVolatility,
							meanReversion);

					// Create a LIBOR market model with the new covariance structure.
					final LIBORModel model = new HullWhiteModel(
							liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModel, null);
					final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);
					final LIBORMonteCarloSimulationFromLIBORModel liborMarketModelMonteCarloSimulation =  new LIBORMonteCarloSimulationFromLIBORModel(model, process);

					final ArrayList<Future<Double>> valueFutures = new ArrayList<>(calibrationProducts.size());
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.size(); calibrationProductIndex++) {
						final int workerCalibrationProductIndex = calibrationProductIndex;
						final Callable<Double> worker = new  Callable<Double>() {
							@Override
							public Double call() {
								try {
									return  calibrationProducts.get(workerCalibrationProductIndex).getProduct().getValue(liborMarketModelMonteCarloSimulation);
								} catch (final Exception e) {
									//									e.printStackTrace();
									// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
									return calibrationTargetValues[workerCalibrationProductIndex];
								}
							}
						};
						if(executor != null) {
							final Future<Double> valueFuture = executor.submit(worker);
							valueFutures.add(calibrationProductIndex, valueFuture);
						}
						else {
							final FutureTask<Double> valueFutureTask = new FutureTask<>(worker);
							valueFutureTask.run();
							valueFutures.add(calibrationProductIndex, valueFutureTask);
						}
					}
					for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.size(); calibrationProductIndex++) {
						try {
							final double value = valueFutures.get(calibrationProductIndex).get();
							values[calibrationProductIndex] = value;
						}
						catch (final InterruptedException | ExecutionException e) {
							throw new SolverException(e);
						}
					}
					System.out.println(this.getRootMeanSquaredError() + "\t" + Arrays.toString(values));
				}
			};

			// Set solver parameters
			optimizer.setWeights(calibrationWeights);
			optimizer.setErrorTolerance(accuracy);
			final double[] parameterSteps = new double[initialParameters.length];
			Arrays.fill(parameterSteps, 1E-4);
			optimizer.setParameterSteps(parameterSteps);

			try {
				optimizer.run();
			}
			catch(final SolverException e) {
				throw new CalculationException(e);
			}
			finally {
				if(executor != null) {
					executor.shutdown();
				}
			}

			// Get covariance model corresponding to the best parameter set.
			final double[] bestParameters = optimizer.getBestFitParameters();

			System.out.println("\nCalibrated parameters are:");
			for (final double p : bestParameters) {
				System.out.println(formatterParam.format(p));
			}

			final double[] shortRateVolatilityCalib = new double[bestParameters.length/2];
			final double[] meanReversionCalib = new double[bestParameters.length/2];
			System.arraycopy(bestParameters, 0, shortRateVolatilityCalib, 0, shortRateVolatilityCalib.length);
			System.arraycopy(bestParameters, shortRateVolatilityCalib.length, meanReversionCalib, 0, shortRateVolatilityCalib.length);

			final ShortRateVolatilityModel volatilityModelCalibrated = new ShortRateVolatilityModelAsGiven(
					shortRateVolTimeDis,
					shortRateVolatilityCalib,
					meanReversionCalib);

			// Create a LIBOR market model with the new covariance structure.
			final LIBORModel modelCalibrated = new HullWhiteModel(
					liborPeriodDiscretization, curveModel, forwardCurve, discountCurve, volatilityModelCalibrated, null);
			final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(modelCalibrated, brownianMotion);
			simulationCalibrated =  new LIBORMonteCarloSimulationFromLIBORModel(modelCalibrated, process);
		}


		System.out.println("\nValuation on calibrated model:");
		double deviationSum			= 0.0;
		double deviationSquaredSum	= 0.0;
		for (int i = 0; i < calibrationProducts.size(); i++) {
			final AbstractLIBORMonteCarloProduct calibrationProduct = calibrationProducts.get(i).getProduct();
			try {
				final double valueModel = calibrationProduct.getValue(simulationCalibrated);
				final double valueTarget = calibrationProducts.get(i).getTargetValue().getAverage();
				final double error = valueModel-valueTarget;
				deviationSum += error;
				deviationSquaredSum += error*error;
				System.out.println(calibrationItemNames.get(i) + "\t" + "Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget));// + "\t" + calibrationProduct.toString());
			}
			catch(final Exception e) {
			}
		}
		final double averageDeviation = deviationSum/calibrationProducts.size();
		System.out.println("Mean Deviation:" + formatterValue.format(averageDeviation));
		System.out.println("RMS Error.....:" + formatterValue.format(Math.sqrt(deviationSquaredSum/calibrationProducts.size())));
		System.out.println("__________________________________________________________________________________________\n");

		Assert.assertTrue(Math.abs(averageDeviation) < 1E-2);
	}

	public AnalyticModel getCalibratedCurve() throws SolverException {
		final String[] maturity					= { "6M", "1Y", "2Y", "3Y", "4Y", "5Y", "6Y", "7Y", "8Y", "9Y", "10Y", "11Y", "12Y", "15Y", "20Y", "25Y", "30Y", "35Y", "40Y", "45Y", "50Y" };
		final String[] frequency				= { "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual", "annual" };
		final String[] frequencyFloat			= { "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual", "semiannual" };
		final String[] daycountConventions		= { "ACT/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360", "E30/360" };
		final String[] daycountConventionsFloat	= { "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360", "ACT/360" };
		final double[] rates					= { -0.00216 ,-0.00208 ,-0.00222 ,-0.00216 ,-0.0019 ,-0.0014 ,-0.00072 ,0.00011 ,0.00103 ,0.00196 ,0.00285 ,0.00367 ,0.0044 ,0.00604 ,0.00733 ,0.00767 ,0.00773 ,0.00765 ,0.00752 ,0.007138 ,0.007 };

		final HashMap<String, Object> parameters = new HashMap<>();

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

	private static AnalyticModel getCalibratedCurve(final AnalyticModel model2, final Map<String, Object> parameters) throws SolverException {

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

		final int		spotOffsetDays = 2;
		final String	forwardStartPeriod = "0D";

		final String curveNameDiscount = "discountCurve-" + currency;

		/*
		 * We create a forward curve by referencing the same discount curve, since
		 * this is a single curve setup.
		 *
		 * Note that using an independent NSS forward curve with its own NSS parameters
		 * would result in a problem where both, the forward curve and the discount curve
		 * have free parameters.
		 */
		final ForwardCurve forwardCurve		= new ForwardCurveFromDiscountCurve(curveNameDiscount, referenceDate, forwardCurveTenor);

		// Create a collection of objective functions (calibration products)
		final Vector<AnalyticProduct> calibrationProducts = new Vector<>();
		final double[] curveMaturities	= new double[rates.length+1];
		final double[] curveValue			= new double[rates.length+1];
		final boolean[] curveIsParameter	= new boolean[rates.length+1];
		curveMaturities[0] = 0.0;
		curveValue[0] = 1.0;
		curveIsParameter[0] = false;
		for(int i=0; i<rates.length; i++) {

			final Schedule schedulePay = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequency[i], daycountConventions[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);
			final Schedule scheduleRec = ScheduleGenerator.createScheduleFromConventions(referenceDate, spotOffsetDays, forwardStartPeriod, maturities[i], frequencyFloat[i], daycountConventionsFloat[i], "first", "following", new BusinessdayCalendarExcludingTARGETHolidays(), -2, 0);

			curveMaturities[i+1] = Math.max(schedulePay.getPayment(schedulePay.getNumberOfPeriods()-1),scheduleRec.getPayment(scheduleRec.getNumberOfPeriods()-1));
			curveValue[i+1] = 1.0;
			curveIsParameter[i+1] = true;
			calibrationProducts.add(new Swap(schedulePay, null, rates[i], curveNameDiscount, scheduleRec, forwardCurve.getName(), 0.0, curveNameDiscount));
		}

		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;

		// Create a discount curve
		final DiscountCurveInterpolation discountCurveInterpolation = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				curveNameDiscount								/* name */,
				referenceDate	/* referenceDate */,
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
		AnalyticModel model = new AnalyticModelFromCurvesAndVols(new Curve[] { discountCurveInterpolation, forwardCurve });

		/*
		 * Create a collection of curves to calibrate
		 */
		final Set<ParameterObject> curvesToCalibrate = new HashSet<>();
		curvesToCalibrate.add(discountCurveInterpolation);

		/*
		 * Calibrate the curve
		 */
		final Solver solver = new Solver(model, calibrationProducts);
		final AnalyticModel calibratedModel = solver.getCalibratedModel(curvesToCalibrate);
		System.out.println("Solver reported acccurary....: " + solver.getAccuracy());

		Assert.assertEquals("Calibration accurarcy", 0.0, solver.getAccuracy(), 1E-3);

		// Get best parameters
		final double[] parametersBest = calibratedModel.getDiscountCurve(discountCurveInterpolation.getName()).getParameter();

		// Test calibration
		model			= calibratedModel;

		double squaredErrorSum = 0.0;
		for(final AnalyticProduct c : calibrationProducts) {
			final double value = c.getValue(0.0, model);
			final double valueTaget = 0.0;
			final double error = value - valueTaget;
			squaredErrorSum += error*error;
		}
		final double rms = Math.sqrt(squaredErrorSum/calibrationProducts.size());

		System.out.println("Independent checked acccurary: " + rms);

		System.out.println("Calibrated discount curve: ");
		for(int i=0; i<curveMaturities.length; i++) {
			final double maturity = curveMaturities[i];
			System.out.println(maturity + "\t" + calibratedModel.getDiscountCurve(discountCurveInterpolation.getName()).getDiscountFactor(maturity));
		}
		return model;
	}

	private static double getParSwaprate(final ForwardCurve forwardCurve, final DiscountCurve discountCurve, final double[] swapTenor) {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), forwardCurve, discountCurve);
	}
}
