/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 31.03.2014
 */

package net.finmath.montecarlo.interestrate.products;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
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
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 *
 */
public class SwaptionAnalyticApproximationTest {

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	private final int numberOfPaths = 20000;
	private final int numberOfFactors = 5;
	private final double correlationDecayParam = 0.04;

	@Test
	public void testSingleCurveModel() throws CalculationException {
		System.out.println("Runnning tests with a single curve LIBOR Market Model");

		final LIBORModelMonteCarloSimulationModel liborMarketModel = createSingleCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		testModel(liborMarketModel, false);
	}

	@Test
	public void testMultiCurveModel() throws CalculationException {
		System.out.println("Runnning tests with a multi curve LIBOR Market Model");

		final LIBORModelMonteCarloSimulationModel liborMarketModel = createMultiCurveLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam);

		testModel(liborMarketModel, true);
	}

	public void testModel(final LIBORModelMonteCarloSimulationModel liborMarketModel, final boolean isMultiCurve) throws CalculationException {
		/*
		 * Value a swaption
		 */
		System.out.println("Swaption prices (isMultiCurve = " + isMultiCurve + "):\n");
		System.out.println("Maturity      Simulation (MC)   Analytic (SC)   Analytic (MC)    Deviation (AMC-S)      Deviation (AMC-ASC)");

		double maxAbsDeviationSimulation	= 0.0;
		double maxAbsDeviationAnalytic		= 0.0;
		for (int maturityIndex = 1; maturityIndex <= liborMarketModel.getNumberOfLibors() - 10; maturityIndex++) {

			final double exerciseDate = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(exerciseDate) + "          ");

			final int numberOfPeriods = 5;

			// Create a swaption

			final double[] fixingDates = new double[numberOfPeriods];
			final double[] paymentDates = new double[numberOfPeriods];
			final double[] swapTenor = new double[numberOfPeriods + 1];
			final double swapPeriodLength = 0.5;

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			final double swaprate = 0.05;// getParSwaprate(liborMarketModel, swapTenor);

			// Set swap rates for each period
			final double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Value with Monte Carlo
			final Swaption swaptionMonteCarlo	= new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
			final double valueSimulation = swaptionMonteCarlo.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "           ");

			// Value analytic (single curve approximation)
			final SwaptionSingleCurveAnalyticApproximation swaptionAnalyitcSingleCurve = new SwaptionSingleCurveAnalyticApproximation(swaprate, swapTenor, SwaptionSingleCurveAnalyticApproximation.ValueUnit.VALUE);
			final double valueAnalyticSingleCurve = swaptionAnalyitcSingleCurve.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueAnalyticSingleCurve) + "         ");

			// Value analytic
			final SwaptionAnalyticApproximation swaptionAnalyitc = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VALUE);
			final double valueAnalytic = swaptionAnalyitc.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviation1 = (valueAnalytic - valueSimulation);
			System.out.print(formatterDeviation.format(deviation1) + "           ");

			final double deviation2 = (valueAnalytic - valueAnalyticSingleCurve);
			System.out.println(formatterDeviation.format(deviation2));

			maxAbsDeviationSimulation	= Math.max(maxAbsDeviationSimulation, Math.abs(deviation1));
			maxAbsDeviationAnalytic		= Math.max(maxAbsDeviationAnalytic, Math.abs(deviation2));
		}

		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertEquals("Deviation", 0.0, maxAbsDeviationSimulation, 9E-4);
		Assert.assertTrue(isMultiCurve || Math.abs(maxAbsDeviationAnalytic) < 1E-15);
	}

	public static LIBORModelMonteCarloSimulationModel createSingleCurveLIBORMarketModel(final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException {

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				0.5											/* tenor / period length */
				);

		// No discount curve
		final DiscountCurve discountCurve = new DiscountCurveFromForwardCurve(forwardCurve);

		return createLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam, discountCurve, forwardCurve);
	}

	public static LIBORModelMonteCarloSimulationModel createMultiCurveLIBORMarketModel(final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException {

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				0.5											/* tenor / period length */
				);

		// Create the discount curve
		final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);

		return createLIBORMarketModel(numberOfPaths, numberOfFactors, correlationDecayParam, discountCurve, forwardCurve);
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam, final DiscountCurve discountCurve, final ForwardCurve forwardCurve) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		/*
		 * Create a simulation time discretization
		 */
		final double lastTime	= 20.0;
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
				liborPeriodDiscretization, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(
				liborMarketModel,
				new net.finmath.montecarlo.BrownianMotionLazyInit(timeDiscretizationFromArray,
						numberOfFactors, numberOfPaths, 3141 /* seed */));
		//		process.setScheme(EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(liborMarketModel, process);
	}
}
