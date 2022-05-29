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

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
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
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionView;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.BlendedLocalVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelStochasticVolatility;
import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.SwaptionGeneralizedAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;

/**
 * This class tests the LIBOR market model and products.
 *
 * @author Christian Fries
 */
public class LIBORMarketModelCalibrationSmileTest {

	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterParam		= new DecimalFormat(" #0.000;-#0.000", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	private final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

	private CalibrationProduct createCalibrationItem(double weight, double exerciseDate, double swapPeriodLength, int numberOfPeriods, double moneyness, double targetVolatility, String targetVolatilityType, ForwardCurve forwardCurve, DiscountCurve discountCurve, String productType) throws CalculationException {

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
		Double targetValue;
		switch(targetVolatilityType) {
		case "VOLATILITYNORMAL":
		case "VOLATILITYLOGNORMAL":
			targetValue = targetVolatility;
			break;
		case "VALUE":
			targetValue = AnalyticFormulas.bachelierOptionValue(swaprate, targetVolatility, fixingDates[0], swaprate, SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor), discountCurve));
			break;
		default:
			throw new IllegalArgumentException("Unknown targetVolatilityType " + targetVolatilityType);
		}


		AbstractTermStructureMonteCarloProduct product;
		switch(productType) {
		case "MONTECARLO":
			product = new SwaptionSimple(swaprate, swapTenor, SwaptionSimple.ValueUnit.valueOf(targetVolatilityType));
			break;
		case "ANALYTIC":
			product = new SwaptionGeneralizedAnalyticApproximation(swaprate, swapTenor,
					SwaptionGeneralizedAnalyticApproximation.ValueUnit.VOLATILITY,
					SwaptionGeneralizedAnalyticApproximation.StateSpace.NORMAL);
			break;
		default:
			throw new IllegalArgumentException("Unknown producType " + productType);
		}

		return new CalibrationProduct(product, targetValue, weight);
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
		 * Definition of curves
		 */
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

		final double tenorPeriodLength = 0.5;

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"		/* name of the curve */,
				fixingTimes			/* fixings of the forward */,
				forwardRates		/* forwards */,
				tenorPeriodLength	/* tenor / period length */
				);


		final DiscountCurve discountCurve = new DiscountCurveFromForwardCurve(forwardCurveInterpolation, tenorPeriodLength);

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

			calibrationProducts.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, "VOLATILITYLOGNORMAL", forwardCurveInterpolation, discountCurve, "MONTECARLO"));
		}


		final double[] atmOptionMaturities	= { 2.00, 3.00, 4.00, 5.00, 7.00, 10.00, 15.00, 20.00, 25.00, 30.00 };
		final double[] atmOptionVolatilities	= { 0.385, 0.351, 0.325, 0.308, 0.288, 0.279, 0.290, 0.272, 0.235, 0.192 };

		for(int i=0; i<atmOptionMaturities.length; i++ ) {

			final double	exerciseDate		= atmOptionMaturities[i];
			final double	moneyness			= 0.0;
			final double	targetVolatility	= atmOptionVolatilities[i];

			calibrationProducts.add(createCalibrationItem(1.0 /* weight */, exerciseDate, swapPeriodLength, numberOfPeriods, moneyness, targetVolatility, "VOLATILITYLOGNORMAL", forwardCurveInterpolation, discountCurve, "MONTECARLO"));
		}

		/*
		 * Create a LIBOR Market Model
		 */

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / tenorPeriodLength), tenorPeriodLength);

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 20.0;
		final double dt		= 0.5;
		final TimeDiscretizationFromArray timeDiscretizationFromArray = new TimeDiscretizationFromArray(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create Brownian motions
		 */
		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray, numberOfFactors + 1, numberOfPaths, 31415 /* seed */);
		final BrownianMotion brownianMotionView1 = new BrownianMotionView(brownianMotion, new Integer[] { 0, 1, 2, 3, 4 });
		final BrownianMotion brownianMotionView2 = new BrownianMotionView(brownianMotion, new Integer[] { 0, 5 });

		// Create a covariance model
		final AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretizationFromArray, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20, 0.05, 0.10, 0.05, 0.10} );
		// Create blended local volatility model with fixed parameter 0.0 (that is "lognormal").
		final AbstractLIBORCovarianceModelParametric covarianceModelBlended = new BlendedLocalVolatilityModel(covarianceModelParametric, 0.0, false);
		// Create stochastic scaling (pass brownianMotionView2 to it)
		final AbstractLIBORCovarianceModelParametric covarianceModelStochasticParametric = new LIBORCovarianceModelStochasticVolatility(covarianceModelBlended, brownianMotionView2, 0.01, -0.30, true);

		// Set model properties
		final Map<String, Object> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", LIBORMarketModelFromCovarianceModel.Measure.SPOT.name());

		// Choose normal state space for the Euler scheme (the covariance model above carries a linear local volatility model, such that the resulting model is log-normal).
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.NORMAL.name());

		// Set calibration properties (should use our brownianMotion for calibration - needed to have to right correlation).
		final Map<String, Object> calibrationParameters = new HashMap<>();
		// The brownianMotion to be used - if a full Monte-Carlo valuation is necessary.
		calibrationParameters.put("brownianMotion", brownianMotionView1);
		// The step size vector used to calculate first derivatives via finite differences
		calibrationParameters.put("parameterStep", new Double(1E-4));

		/*
		 * The optimizer to use and some of its parameters
		 */

		// The accuracy of the solver. The solver stops if the value does not improve more than the given parameter.
		final Double accuracy = new Double(1E-5);
		final int maxIterations = 100;
		final int numberOfThreads = 4;		// two concurrent models
		final OptimizerFactory optimizerFactory = new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);
		calibrationParameters.put("optimizerFactory", optimizerFactory);

		// Pass the calibrationParameters to the model.
		properties.put("calibrationParameters", calibrationParameters);

		final long millisCalibrationStart = System.currentTimeMillis();

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModelCalibrated = LIBORMarketModelFromCovarianceModel.of(
				liborPeriodDiscretization,
				null,
				forwardCurveInterpolation,
				discountCurve,
				randomVariableFactory,
				covarianceModelStochasticParametric,
				calibrationProducts.toArray(new CalibrationProduct[calibrationProducts.size()]), properties);

		final long millisCalibrationEnd = System.currentTimeMillis();

		/*
		 * Test our calibration
		 */
		System.out.println("\nCalibrated parameters are:");
		final double[] param = ((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).getParameterAsDouble();
		//		((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).setParameter(param);
		for (final double p : param) {
			System.out.println(formatterParam.format(p));
		}

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModelCalibrated, brownianMotionView1);
		final net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel simulationCalibrated = new net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel(
				liborMarketModelCalibrated, process);

		System.out.println("\nValuation on calibrated model:");
		double deviationSum			= 0.0;
		double deviationSquaredSum	= 0.0;
		for (int i = 0; i < calibrationProducts.size(); i++) {
			final AbstractTermStructureMonteCarloProduct calibrationProduct = calibrationProducts.get(i).getProduct();
			try {
				final double valueModel = calibrationProduct.getValue(simulationCalibrated);
				final double valueTarget = calibrationProducts.get(i).getTargetValue().getAverage();
				final double error = valueModel-valueTarget;
				deviationSum += error;
				deviationSquaredSum += error*error;
				System.out.println("Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget) + "\t" + calibrationProduct.toString());
			}
			catch(final Exception e) {
				//
			}
		}

		System.out.println("Time required for calibration of volatilities...: " + (millisCalibrationEnd-millisCalibrationStart)/1000.0 + " s.");

		final double averageDeviation = deviationSum/calibrationProducts.size();
		System.out.println("Mean Deviation:" + formatterValue.format(averageDeviation));
		System.out.println("RMS Error.....:" + formatterValue.format(Math.sqrt(deviationSquaredSum/calibrationProducts.size())));
		System.out.println("__________________________________________________________________________________________\n");

		Assert.assertTrue(Math.abs(averageDeviation) < 1E-2);
	}

	private static double getParSwaprate(final ForwardCurve forwardCurve, final DiscountCurve discountCurve, final double[] swapTenor) {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), forwardCurve, discountCurve);
	}
}
