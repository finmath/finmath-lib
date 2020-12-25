/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel.Measure;
import net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelExponentialForm7Param;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Bond;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.DigitalCaplet;
import net.finmath.montecarlo.interestrate.products.SimpleSwap;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximationRebonato;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class tests the LIBOR market model and products.
 *
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class LIBORMarketModelMultiCurveValuationTest {

	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ Measure.SPOT }, { Measure.TERMINAL }
		});
	}

	private final int numberOfPaths		= 40000;
	private final int numberOfFactors	= 6;

	private final Measure measure;

	private final LIBORModelMonteCarloSimulationModel liborMarketModel;

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	public LIBORMarketModelMultiCurveValuationTest(final Measure measure) throws CalculationException {
		// Store measure
		this.measure = measure;

		// Create a libor market model
		liborMarketModel = createLIBORMarketModel(measure, numberOfPaths, numberOfFactors, 0.1 /* Correlation */);
	}

	public static LIBORModelMonteCarloSimulationModel createLIBORMarketModel(
			final Measure measure, final int numberOfPaths, final int numberOfFactors, final double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		final double liborPeriodLength	= 0.5;
		final double liborRateTimeHorzion	= 20.0;
		final TimeDiscretizationFromArray liborPeriodDiscretization = new TimeDiscretizationFromArray(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		final ForwardCurveInterpolation forwardCurveInterpolation = ForwardCurveInterpolation.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		// Create the discount curve
		final DiscountCurveInterpolation discountCurveInterpolation = DiscountCurveInterpolation.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);


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
		properties.put("measure", measure.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModelFromCovarianceModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		final CalibrationProduct[] calibrationItems = new CalibrationProduct[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		final LIBORMarketModel liborMarketModel = LIBORMarketModelFromCovarianceModel.of(
				liborPeriodDiscretization,
				new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurveInterpolation, discountCurveInterpolation}),
				forwardCurveInterpolation,
				discountCurveInterpolation,
				new RandomVariableFromArrayFactory(),
				covarianceModel,
				calibrationItems,
				properties);

		final BrownianMotion brownianMotion = new net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers(timeDiscretizationFromArray, numberOfFactors, numberOfPaths, 3141 /* seed */);

		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModel, brownianMotion, EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORMonteCarloSimulationFromLIBORModel(process);
	}

	@Test
	public void testBond() throws CalculationException {
		/*
		 * Value a bond
		 */

		System.out.println("Bond prices:\n");
		System.out.println("Maturity      Simulation       Analytic        Deviation");

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 0; maturityIndex <= liborMarketModel.getNumberOfLibors(); maturityIndex++) {
			final double maturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(maturity) + "          ");

			// Create a bond
			final Bond bond = new Bond(maturity);

			// Bond price with Monte Carlo
			final double priceOfBond = bond.getValue(liborMarketModel);
			System.out.print(formatterValue.format(priceOfBond) + "          ");

			// Bond price analytic
			final double priceOfBondAnalytic = liborMarketModel.getModel().getDiscountCurve().getDiscountFactor(maturity);

			System.out.print(formatterValue.format(priceOfBondAnalytic) + "          ");

			// Relative deviation
			final double deviation = (priceOfBond - priceOfBondAnalytic);
			System.out.println(formatterDeviation.format(deviation));

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		// jUnit assertion: condition under which we consider this test successful
		Assert.assertTrue(maxAbsDeviation < 5E-03);
	}

	@Test
	public void testSwap() throws CalculationException {
		/*
		 * Value a swap
		 */
		System.out.println("Par-Swap prices:\n");
		System.out.println("Swap \t\t\t Value");

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= liborMarketModel.getNumberOfLibors() - 10; maturityIndex++) {

			final double startDate = liborMarketModel.getLiborPeriod(maturityIndex);

			final int numberOfPeriods = 10;

			// Create a swap
			final double[]	fixingDates			= new double[numberOfPeriods];
			final double[]	paymentDates		= new double[numberOfPeriods];
			final double[]	swapTenor			= new double[numberOfPeriods + 1];
			final double		swapPeriodLength	= 0.5;

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex]	= startDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex]	= startDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex]		= startDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = startDate + numberOfPeriods * swapPeriodLength;

			System.out.print("(" + formatterMaturity.format(swapTenor[0]) + "," + formatterMaturity.format(swapTenor[numberOfPeriods-1]) + "," + swapPeriodLength + ")" + "\t");

			// Par swap rate
			final double swaprate = getParSwaprate(liborMarketModel, swapTenor);

			// Set swap rates for each period
			final double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Create a swap
			final SimpleSwap swap = new SimpleSwap(fixingDates, paymentDates, swaprates, 1.0 /* notional */);

			// Value the swap
			final double value = swap.getValue(liborMarketModel);
			System.out.print(formatterValue.format(value) + "\n");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(value));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 * The swap should be at par (close to zero)
		 */
		if(measure == Measure.SPOT) {
			Assert.assertEquals(0, maxAbsDeviation, 2E-3);
		} else {
			Assert.assertEquals(0, maxAbsDeviation, 2E-2);
		}
	}

	@Test
	public void testCaplet() throws CalculationException {
		/*
		 * Value a caplet
		 */
		System.out.println("Caplet prices:\n");
		System.out.println("Maturity      Simulation       Analytic        Deviation");

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= liborMarketModel.getNumberOfLibors() - 10; maturityIndex++) {

			final double optionMaturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(optionMaturity) + "          ");

			final double periodStart	= liborMarketModel.getLiborPeriod(maturityIndex);
			final double periodEnd	= liborMarketModel.getLiborPeriod(maturityIndex+1);
			final double periodLength	= periodEnd-periodStart;
			final double daycountFraction = periodEnd-periodStart;

			final double strike = 0.05;

			// Create a caplet
			final Caplet caplet = new Caplet(optionMaturity, periodLength, strike, daycountFraction, false /* isFloorlet */, Caplet.ValueUnit.VALUE);

			// Value with Monte Carlo
			final double valueSimulation = caplet.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			final double forward			= getParSwaprate(liborMarketModel, new double[] { periodStart , periodEnd});
			final double discountFactor	= getSwapAnnuity(liborMarketModel, new double[] { periodStart , periodEnd}) / periodLength;
			final int optionMaturityIndex = liborMarketModel.getTimeIndex(optionMaturity);
			final int liborIndex = liborMarketModel.getLiborPeriodIndex(periodStart);
			final double volatility = Math.sqrt(((LIBORMarketModel)liborMarketModel.getModel()).getIntegratedLIBORCovariance(liborMarketModel.getTimeDiscretization())[optionMaturityIndex][liborIndex][liborIndex]/optionMaturity);
			final double valueAnalytic = net.finmath.functions.AnalyticFormulas.blackModelCapletValue(forward, volatility, optionMaturity, strike, periodLength, discountFactor);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviation = (valueSimulation - valueAnalytic);
			System.out.println(formatterDeviation.format(deviation) + "          ");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		if(measure == Measure.SPOT) {
			Assert.assertTrue(Math.abs(maxAbsDeviation) < 2E-4);
		} else {
			Assert.assertTrue(Math.abs(maxAbsDeviation) < 2E-3);
		}
	}

	@Test
	public void testDigitalCaplet() throws CalculationException {
		/*
		 * Value a swaption
		 */
		System.out.println("Digital caplet prices:\n");
		System.out.println("Maturity      Simulation       Analytic        Deviation");

		double maxAbsDeviation = 0.0;
		for (int maturityIndex = 1; maturityIndex <= liborMarketModel.getNumberOfLibors() - 10; maturityIndex++) {

			final double optionMaturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(optionMaturity) + "          ");

			final double periodStart	= liborMarketModel.getLiborPeriod(maturityIndex);
			final double periodEnd	= liborMarketModel.getLiborPeriod(maturityIndex+1);

			final double strike = 0.02;

			// Create a digital caplet
			final DigitalCaplet digitalCaplet = new DigitalCaplet(optionMaturity, periodStart, periodEnd, strike);

			// Value with Monte Carlo
			final double valueSimulation = digitalCaplet.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			final double forward			= getParSwaprate(liborMarketModel, new double[] { periodStart , periodEnd});
			final double periodLength		= periodEnd-periodStart;
			final double discountFactor	= getSwapAnnuity(liborMarketModel, new double[] { periodStart , periodEnd}) / periodLength;
			final int optionMaturityIndex = liborMarketModel.getTimeIndex(optionMaturity);
			final int liborIndex = liborMarketModel.getLiborPeriodIndex(periodStart);
			final double volatility = Math.sqrt(((LIBORMarketModel)liborMarketModel.getModel()).getIntegratedLIBORCovariance(liborMarketModel.getTimeDiscretization())[optionMaturityIndex][liborIndex][liborIndex]/optionMaturity);
			final double valueAnalytic = net.finmath.functions.AnalyticFormulas.blackModelDigitalCapletValue(forward, volatility, periodLength, discountFactor, optionMaturity, strike);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			final double deviation = (valueSimulation - valueAnalytic);
			System.out.println(formatterDeviation.format(deviation) + "          ");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 5E-2);
	}

	@Test
	public void testSwaption() throws CalculationException {
		/*
		 * Value a swaption
		 */
		System.out.println("Swaption prices:\n");
		System.out.println("Maturity      Simulation       Analytic 1       Analytic 2       Deviation 1           Deviation 2");

		double maxAbsDeviation = 0.0;
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
			final double swaprate = getParSwaprate(liborMarketModel, swapTenor);

			// Set swap rates for each period
			final double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Value with Monte Carlo
			final Swaption swaptionMonteCarlo	= new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
			final double valueSimulation = swaptionMonteCarlo.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			final SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VALUE);
			final double valueAnalytic1 = swaptionAnalytic.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueAnalytic1) + "          ");

			// Value analytic 2
			final SwaptionAnalyticApproximationRebonato swaptionAnalytic2 = new SwaptionAnalyticApproximationRebonato(swaprate, swapTenor, SwaptionAnalyticApproximationRebonato.ValueUnit.VALUE);
			final double valueAnalytic2 = swaptionAnalytic2.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueAnalytic2) + "          ");

			// Absolute deviation
			final double deviation1 = (valueSimulation - valueAnalytic1);
			System.out.print(formatterDeviation.format(deviation1) + "          ");

			final double deviation2 = (valueAnalytic1 - valueAnalytic2);
			System.out.println(formatterDeviation.format(deviation2) + "          ");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation1));
			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation2));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 8E-3);
	}

	@Test
	public void testSwaptionSmile() throws CalculationException {
		/*
		 * Value a swaption
		 */
		System.out.println("Swaption prices:\n");
		System.out.println("Moneyness      Simulation       Analytic        Deviation");

		final double maturity = 5.0;
		final int numberOfPeriods = 10;
		final double swapPeriodLength = 0.5;

		double maxAbsDeviation = 0.0;
		for (double moneyness = 0.5; moneyness < 2.0; moneyness += 0.1) {

			final double exerciseDate = maturity;


			// Create a swaption

			final double[] fixingDates = new double[numberOfPeriods];
			final double[] paymentDates = new double[numberOfPeriods];
			final double[] swapTenor = new double[numberOfPeriods + 1];

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			final double swaprate = moneyness * getParSwaprate(liborMarketModel, swapTenor);

			// Set swap rates for each period
			final double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			final Swaption						swaptionMonteCarlo = new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
			final SwaptionAnalyticApproximation	swaptionAnalyitc = new SwaptionAnalyticApproximation(
					swaprate, swapTenor,
					SwaptionAnalyticApproximation.ValueUnit.VALUE);

			System.out.print(formatterValue.format(moneyness) + "          ");

			// Value with Monte Carlo
			final double valueSimulation = swaptionMonteCarlo.getValue(liborMarketModel);
			final double impliedVolSimulation = AnalyticFormulas.blackScholesOptionImpliedVolatility(getParSwaprate(liborMarketModel, swapTenor), exerciseDate, swaprate, getSwapAnnuity(liborMarketModel, swapTenor), valueSimulation);
			System.out.print(formatterValue.format(impliedVolSimulation) + "          ");

			// Value analytic
			final double valueAnalytic = swaptionAnalyitc.getValue(liborMarketModel);
			final double impliedVolAnalytic = AnalyticFormulas.blackScholesOptionImpliedVolatility(getParSwaprate(liborMarketModel, swapTenor), exerciseDate, swaprate, getSwapAnnuity(liborMarketModel, swapTenor), valueAnalytic);
			System.out.print(formatterValue.format(impliedVolAnalytic) + "          ");

			// Absolute deviation
			final double deviation = (impliedVolSimulation - impliedVolAnalytic);
			System.out.println(formatterDeviation.format(deviation) + "          ");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 1E-1);
	}

	@Test
	public void testSwaptionCalibration() throws CalculationException {

		/*
		 * Calibration test
		 */
		System.out.println("Calibration to Swaptions:");


		/*
		 * Create a set of calibration products.
		 */
		final ArrayList<CalibrationProduct> calibrationProducts = new ArrayList<>();
		for (int exerciseIndex = 4; exerciseIndex <= liborMarketModel.getNumberOfLibors() - 5; exerciseIndex+=4) {
			final double exerciseDate = liborMarketModel.getLiborPeriod(exerciseIndex);
			for (int numberOfPeriods = 1; numberOfPeriods < liborMarketModel.getNumberOfLibors() - exerciseIndex - 5; numberOfPeriods+=4) {

				// Create a swaption

				final double[]	fixingDates			= new double[numberOfPeriods];
				final double[]	paymentDates		= new double[numberOfPeriods];
				final double[]	swapTenor			= new double[numberOfPeriods + 1];
				final double		swapPeriodLength	= 0.5;

				for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
					fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
					paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
					swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				}
				swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

				// Swaptions swap rate
				final double swaprate = getParSwaprate(liborMarketModel,swapTenor);

				// Set swap rates for each period
				final double[] swaprates = new double[numberOfPeriods];
				Arrays.fill(swaprates, swaprate);

				// This is just some swaption volatility used for testing, true market data should go here.
				final double targetValueVolatilty = 0.20 + 0.20 * Math.exp(-exerciseDate / 10.0) + 0.20 * Math.exp(-(exerciseDate+numberOfPeriods) / 10.0);

				// Buid our calibration product

				// XXX1: Change the calibration product here
				final boolean isUseAnalyticCalibration = false;
				if(isUseAnalyticCalibration) {
					// Use an analytic approximation to the swaption - much faster
					final SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VOLATILITYLOGNORMAL);

					calibrationProducts.add(new CalibrationProduct(swaptionAnalytic, targetValueVolatilty, 1.0));
				}
				else {
					// You may also use full Monte-Carlo calibration - more accurate. Also possible for displaced diffusion.
					final Swaption swaptionMonteCarlo = new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
					final double targetValuePrice = AnalyticFormulas.blackModelSwaptionValue(swaprate, targetValueVolatilty, fixingDates[0], swaprate, getSwapAnnuity(liborMarketModel,swapTenor));
					calibrationProducts.add(new CalibrationProduct(swaptionMonteCarlo, targetValuePrice, 1.0));
				}
			}
		}
		System.out.println();

		/*
		 * Take discretization and forward curve from liborMarketModel
		 */
		final TimeDiscretization timeDiscretization = liborMarketModel.getTimeDiscretization();

		final DiscountCurve discountCurve = liborMarketModel.getModel().getDiscountCurve();
		final ForwardCurve forwardCurve = liborMarketModel.getModel().getForwardRateCurve();

		/*
		 * Create a LIBOR Market Model
		 */

		// XXX2 Change covariance model here
		final AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm7Param(timeDiscretization, liborMarketModel.getLiborPeriodDiscretization(), liborMarketModel.getNumberOfFactors());

		final LIBORMarketModelFromCovarianceModel liborMarketModelCalibrated = LIBORMarketModelFromCovarianceModel.of(
				liborMarketModel.getLiborPeriodDiscretization(),
				new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve, discountCurve }),
				forwardCurve, discountCurve,
				new RandomVariableFromArrayFactory(),
				covarianceModelParametric,
				calibrationProducts.toArray(new CalibrationProduct[0]), null);

		/*
		 * Test our calibration
		 */
		final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(liborMarketModelCalibrated,
				new net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers(timeDiscretization,
						numberOfFactors, numberOfPaths, 3141 /* seed */), EulerSchemeFromProcessModel.Scheme.PREDICTOR_CORRECTOR);

		final net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel calModel = new net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel(
				process);

		final double[] param = ((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).getParameterAsDouble();
		for (final double p : param) {
			System.out.println(p);
		}

		double deviationSum = 0.0;
		for (int i = 0; i < calibrationProducts.size(); i++) {
			final AbstractLIBORMonteCarloProduct calibrationProduct = calibrationProducts.get(i).getProduct();
			final double valueModel = calibrationProduct.getValue(calModel);
			final double valueTarget = calibrationProducts.get(i).getTargetValue().getAverage();
			deviationSum += (valueModel-valueTarget);
			System.out.println("Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget));
		}
		System.out.println("Mean Deviation:" + deviationSum/calibrationProducts.size());
		System.out.println("__________________________________________________________________________________________\n");
	}

	private static double getParSwaprate(final LIBORModelMonteCarloSimulationModel liborMarketModel, final double[] swapTenor) {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretizationFromArray(swapTenor), new TimeDiscretizationFromArray(swapTenor), liborMarketModel.getModel().getForwardRateCurve(), liborMarketModel.getModel().getDiscountCurve());
	}

	private static double getSwapAnnuity(final LIBORModelMonteCarloSimulationModel liborMarketModel, final double[] swapTenor) {
		return net.finmath.marketdata.products.SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor), liborMarketModel.getModel().getDiscountCurve());
	}
}
