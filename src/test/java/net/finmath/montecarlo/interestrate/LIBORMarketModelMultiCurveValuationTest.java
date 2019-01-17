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
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.LIBORMarketModel.Measure;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelExponentialForm7Param;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFromGivenMatrix;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Bond;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.DigitalCaplet;
import net.finmath.montecarlo.interestrate.products.SimpleSwap;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximationRebonato;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

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

	private LIBORModelMonteCarloSimulationInterface liborMarketModel;

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	public LIBORMarketModelMultiCurveValuationTest(Measure measure) throws CalculationException {
		// Store measure
		this.measure = measure;

		// Create a libor market model
		liborMarketModel = createLIBORMarketModel(measure, numberOfPaths, numberOfFactors, 0.1 /* Correlation */);
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			Measure measure, int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

		/*
		 * Create the libor tenor structure and the initial values
		 */
		double liborPeriodLength	= 0.5;
		double liborRateTimeHorzion	= 20.0;
		TimeDiscretization liborPeriodDiscretization = new TimeDiscretization(0.0, (int) (liborRateTimeHorzion / liborPeriodLength), liborPeriodLength);

		// Create the forward curve (initial value of the LIBOR market model)
		ForwardCurve forwardCurve = ForwardCurve.createForwardCurveFromForwards(
				"forwardCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* fixings of the forward */,
				new double[] {0.05, 0.05, 0.05, 0.05, 0.05}	/* forwards */,
				liborPeriodLength							/* tenor / period length */
				);

		// Create the discount curve
		DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromZeroRates(
				"discountCurve"								/* name of the curve */,
				new double[] {0.5 , 1.0 , 2.0 , 5.0 , 40.0}	/* maturities */,
				new double[] {0.04, 0.04, 0.04, 0.04, 0.05}	/* zero rates */
				);


		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 20.0;
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
				if(timeToMaturity <= 0) {
					instVolatility = 0;				// This forward rate is already fixed, no volatility
				} else {
					instVolatility = 0.3 + 0.2 * Math.exp(-0.25 * timeToMaturity);
				}

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
		Map<String, String> properties = new HashMap<>();

		// Choose the simulation measure
		properties.put("measure", measure.name());

		// Choose log normal model
		properties.put("stateSpace", LIBORMarketModel.StateSpace.LOGNORMAL.name());

		// Empty array of calibration items - hence, model will use given covariance
		CalibrationItem[] calibrationItems = new CalibrationItem[0];

		/*
		 * Create corresponding LIBOR Market Model
		 */
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(
				liborPeriodDiscretization, forwardCurve, discountCurve, covarianceModel, calibrationItems, properties);

		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 3141 /* seed */);

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
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
			double maturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(maturity) + "          ");

			// Create a bond
			Bond bond = new Bond(maturity);

			// Bond price with Monte Carlo
			double priceOfBond = bond.getValue(liborMarketModel);
			System.out.print(formatterValue.format(priceOfBond) + "          ");

			// Bond price analytic
			double priceOfBondAnalytic = liborMarketModel.getModel().getDiscountCurve().getDiscountFactor(maturity);

			System.out.print(formatterValue.format(priceOfBondAnalytic) + "          ");

			// Relative deviation
			double deviation = (priceOfBond - priceOfBondAnalytic);
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

			double startDate = liborMarketModel.getLiborPeriod(maturityIndex);

			int numberOfPeriods = 10;

			// Create a swap
			double[]	fixingDates			= new double[numberOfPeriods];
			double[]	paymentDates		= new double[numberOfPeriods];
			double[]	swapTenor			= new double[numberOfPeriods + 1];
			double		swapPeriodLength	= 0.5;

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex]	= startDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex]	= startDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex]		= startDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = startDate + numberOfPeriods * swapPeriodLength;

			System.out.print("(" + formatterMaturity.format(swapTenor[0]) + "," + formatterMaturity.format(swapTenor[numberOfPeriods-1]) + "," + swapPeriodLength + ")" + "\t");

			// Par swap rate
			double swaprate = getParSwaprate(liborMarketModel, swapTenor);

			// Set swap rates for each period
			double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Create a swap
			SimpleSwap swap = new SimpleSwap(fixingDates, paymentDates, swaprates);

			// Value the swap
			double value = swap.getValue(liborMarketModel);
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

			double optionMaturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(optionMaturity) + "          ");

			double periodStart	= liborMarketModel.getLiborPeriod(maturityIndex);
			double periodEnd	= liborMarketModel.getLiborPeriod(maturityIndex+1);
			double periodLength	= periodEnd-periodStart;
			double daycountFraction = periodEnd-periodStart;

			double strike = 0.05;

			// Create a caplet
			Caplet caplet = new Caplet(optionMaturity, periodLength, strike, daycountFraction, false /* isFloorlet */, Caplet.ValueUnit.VALUE);

			// Value with Monte Carlo
			double valueSimulation = caplet.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			double forward			= getParSwaprate(liborMarketModel, new double[] { periodStart , periodEnd});
			double discountFactor	= getSwapAnnuity(liborMarketModel, new double[] { periodStart , periodEnd}) / periodLength;
			int optionMaturityIndex = liborMarketModel.getTimeIndex(optionMaturity);
			int liborIndex = liborMarketModel.getLiborPeriodIndex(periodStart);
			double volatility = Math.sqrt(((LIBORMarketModelInterface)liborMarketModel.getModel()).getIntegratedLIBORCovariance()[optionMaturityIndex][liborIndex][liborIndex]/optionMaturity);
			double valueAnalytic = net.finmath.functions.AnalyticFormulas.blackModelCapletValue(forward, volatility, optionMaturity, strike, periodLength, discountFactor);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviation = (valueSimulation - valueAnalytic);
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

			double optionMaturity = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(optionMaturity) + "          ");

			double periodStart	= liborMarketModel.getLiborPeriod(maturityIndex);
			double periodEnd	= liborMarketModel.getLiborPeriod(maturityIndex+1);

			double strike = 0.02;

			// Create a digital caplet
			DigitalCaplet digitalCaplet = new DigitalCaplet(optionMaturity, periodStart, periodEnd, strike);

			// Value with Monte Carlo
			double valueSimulation = digitalCaplet.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			double forward			= getParSwaprate(liborMarketModel, new double[] { periodStart , periodEnd});
			double periodLength		= periodEnd-periodStart;
			double discountFactor	= getSwapAnnuity(liborMarketModel, new double[] { periodStart , periodEnd}) / periodLength;
			int optionMaturityIndex = liborMarketModel.getTimeIndex(optionMaturity);
			int liborIndex = liborMarketModel.getLiborPeriodIndex(periodStart);
			double volatility = Math.sqrt(((LIBORMarketModelInterface)liborMarketModel.getModel()).getIntegratedLIBORCovariance()[optionMaturityIndex][liborIndex][liborIndex]/optionMaturity);
			double valueAnalytic = net.finmath.functions.AnalyticFormulas.blackModelDgitialCapletValue(forward, volatility, periodLength, discountFactor, optionMaturity, strike);
			System.out.print(formatterValue.format(valueAnalytic) + "          ");

			// Absolute deviation
			double deviation = (valueSimulation - valueAnalytic);
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

			double exerciseDate = liborMarketModel.getLiborPeriod(maturityIndex);
			System.out.print(formatterMaturity.format(exerciseDate) + "          ");

			int numberOfPeriods = 5;

			// Create a swaption

			double[] fixingDates = new double[numberOfPeriods];
			double[] paymentDates = new double[numberOfPeriods];
			double[] swapTenor = new double[numberOfPeriods + 1];
			double swapPeriodLength = 0.5;

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			double swaprate = getParSwaprate(liborMarketModel, swapTenor);

			// Set swap rates for each period
			double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			// Value with Monte Carlo
			Swaption swaptionMonteCarlo	= new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
			double valueSimulation = swaptionMonteCarlo.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueSimulation) + "          ");

			// Value analytic
			SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VALUE);
			double valueAnalytic1 = swaptionAnalytic.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueAnalytic1) + "          ");

			// Value analytic 2
			SwaptionAnalyticApproximationRebonato swaptionAnalytic2 = new SwaptionAnalyticApproximationRebonato(swaprate, swapTenor, SwaptionAnalyticApproximationRebonato.ValueUnit.VALUE);
			double valueAnalytic2 = swaptionAnalytic2.getValue(liborMarketModel);
			System.out.print(formatterValue.format(valueAnalytic2) + "          ");

			// Absolute deviation
			double deviation1 = (valueSimulation - valueAnalytic1);
			System.out.print(formatterDeviation.format(deviation1) + "          ");

			double deviation2 = (valueAnalytic1 - valueAnalytic2);
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

		double maturity = 5.0;
		int numberOfPeriods = 10;
		double swapPeriodLength = 0.5;

		double maxAbsDeviation = 0.0;
		for (double moneyness = 0.5; moneyness < 2.0; moneyness += 0.1) {

			double exerciseDate = maturity;


			// Create a swaption

			double[] fixingDates = new double[numberOfPeriods];
			double[] paymentDates = new double[numberOfPeriods];
			double[] swapTenor = new double[numberOfPeriods + 1];

			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
				swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
			}
			swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

			// Swaptions swap rate
			double swaprate = moneyness * getParSwaprate(liborMarketModel, swapTenor);

			// Set swap rates for each period
			double[] swaprates = new double[numberOfPeriods];
			for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
				swaprates[periodStartIndex] = swaprate;
			}

			Swaption						swaptionMonteCarlo = new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
			SwaptionAnalyticApproximation	swaptionAnalyitc = new SwaptionAnalyticApproximation(
					swaprate, swapTenor,
					SwaptionAnalyticApproximation.ValueUnit.VALUE);

			System.out.print(formatterValue.format(moneyness) + "          ");

			// Value with Monte Carlo
			double valueSimulation = swaptionMonteCarlo.getValue(liborMarketModel);
			double impliedVolSimulation = AnalyticFormulas.blackScholesOptionImpliedVolatility(getParSwaprate(liborMarketModel, swapTenor), exerciseDate, swaprate, getSwapAnnuity(liborMarketModel, swapTenor), valueSimulation);
			System.out.print(formatterValue.format(impliedVolSimulation) + "          ");

			// Value analytic
			double valueAnalytic = swaptionAnalyitc.getValue(liborMarketModel);
			double impliedVolAnalytic = AnalyticFormulas.blackScholesOptionImpliedVolatility(getParSwaprate(liborMarketModel, swapTenor), exerciseDate, swaprate, getSwapAnnuity(liborMarketModel, swapTenor), valueAnalytic);
			System.out.print(formatterValue.format(impliedVolAnalytic) + "          ");

			// Absolute deviation
			double deviation = (impliedVolSimulation - impliedVolAnalytic);
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
		ArrayList<CalibrationItem> calibrationItems = new ArrayList<>();
		for (int exerciseIndex = 4; exerciseIndex <= liborMarketModel.getNumberOfLibors() - 5; exerciseIndex+=4) {
			double exerciseDate = liborMarketModel.getLiborPeriod(exerciseIndex);
			for (int numberOfPeriods = 1; numberOfPeriods < liborMarketModel.getNumberOfLibors() - exerciseIndex - 5; numberOfPeriods+=4) {

				// Create a swaption

				double[]	fixingDates			= new double[numberOfPeriods];
				double[]	paymentDates		= new double[numberOfPeriods];
				double[]	swapTenor			= new double[numberOfPeriods + 1];
				double		swapPeriodLength	= 0.5;

				for (int periodStartIndex = 0; periodStartIndex < numberOfPeriods; periodStartIndex++) {
					fixingDates[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
					paymentDates[periodStartIndex] = exerciseDate + (periodStartIndex + 1) * swapPeriodLength;
					swapTenor[periodStartIndex] = exerciseDate + periodStartIndex * swapPeriodLength;
				}
				swapTenor[numberOfPeriods] = exerciseDate + numberOfPeriods * swapPeriodLength;

				// Swaptions swap rate
				double swaprate = getParSwaprate(liborMarketModel,swapTenor);

				// Set swap rates for each period
				double[] swaprates = new double[numberOfPeriods];
				Arrays.fill(swaprates, swaprate);

				// This is just some swaption volatility used for testing, true market data should go here.
				double targetValueVolatilty = 0.20 + 0.20 * Math.exp(-exerciseDate / 10.0) + 0.20 * Math.exp(-(exerciseDate+numberOfPeriods) / 10.0);

				// Buid our calibration product

				// XXX1: Change the calibration product here
				boolean isUseAnalyticCalibration = false;
				if(isUseAnalyticCalibration) {
					// Use an analytic approximation to the swaption - much faster
					SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VOLATILITY);

					calibrationItems.add(new CalibrationItem(swaptionAnalytic, targetValueVolatilty, 1.0));
				}
				else {
					// You may also use full Monte-Carlo calibration - more accurate. Also possible for displaced diffusion.
					Swaption swaptionMonteCarlo = new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
					double targetValuePrice = AnalyticFormulas.blackModelSwaptionValue(swaprate, targetValueVolatilty, fixingDates[0], swaprate, getSwapAnnuity(liborMarketModel,swapTenor));
					calibrationItems.add(new CalibrationItem(swaptionMonteCarlo, targetValuePrice, 1.0));
				}
			}
		}
		System.out.println();

		/*
		 * Take discretization and forward curve from liborMarketModel
		 */
		TimeDiscretizationInterface timeDiscretization = liborMarketModel.getTimeDiscretization();

		DiscountCurveInterface discountCurve = liborMarketModel.getModel().getDiscountCurve();
		ForwardCurveInterface forwardCurve = liborMarketModel.getModel().getForwardRateCurve();

		/*
		 * Create a LIBOR Market Model
		 */

		// XXX2 Change covariance model here
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm7Param(timeDiscretization, liborMarketModel.getLiborPeriodDiscretization(), liborMarketModel.getNumberOfFactors());

		LIBORMarketModel liborMarketModelCalibrated = new LIBORMarketModel(
				this.liborMarketModel.getLiborPeriodDiscretization(),
				forwardCurve, discountCurve, covarianceModelParametric, calibrationItems.toArray(new CalibrationItem[0]), null);

		/*
		 * Test our calibration
		 */
		ProcessEulerScheme process = new ProcessEulerScheme(
				new net.finmath.montecarlo.BrownianMotion(timeDiscretization,
						numberOfFactors, numberOfPaths, 3141 /* seed */), ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation calMode = new net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation(
				liborMarketModelCalibrated, process);

		double[] param = ((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).getParameter();
		for (double p : param) {
			System.out.println(p);
		}

		double deviationSum = 0.0;
		for (int i = 0; i < calibrationItems.size(); i++) {
			AbstractLIBORMonteCarloProduct calibrationProduct = calibrationItems.get(i).getProduct();
			double valueModel = calibrationProduct.getValue(calMode);
			double valueTarget = calibrationItems.get(i).getTargetValue().getAverage();
			deviationSum += (valueModel-valueTarget);
			System.out.println("Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget));
		}
		System.out.println("Mean Deviation:" + deviationSum/calibrationItems.size());
		System.out.println("__________________________________________________________________________________________\n");
	}

	private static double getParSwaprate(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretization(swapTenor), new TimeDiscretization(swapTenor), liborMarketModel.getModel().getForwardRateCurve(), liborMarketModel.getModel().getDiscountCurve());
	}

	private static double getSwapAnnuity(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) {
		return net.finmath.marketdata.products.SwapAnnuity.getSwapAnnuity(new TimeDiscretization(swapTenor), liborMarketModel.getModel().getDiscountCurve());
	}
}
