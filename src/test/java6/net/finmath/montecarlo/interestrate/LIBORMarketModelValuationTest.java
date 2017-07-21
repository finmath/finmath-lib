/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMarketModel.CalibrationItem;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModelParametric;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelExponentialForm5Param;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelFourParameterExponentialForm;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Bond;
import net.finmath.montecarlo.interestrate.products.DigitalCaplet;
import net.finmath.montecarlo.interestrate.products.SimpleSwap;
import net.finmath.montecarlo.interestrate.products.Swaption;
import net.finmath.montecarlo.interestrate.products.SwaptionAnalyticApproximation;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple;
import net.finmath.montecarlo.interestrate.products.SwaptionSimple.ValueUnit;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class tests the LIBOR market model and products.
 * 
 * @author Christian Fries
 */
public class LIBORMarketModelValuationTest {

	private final int numberOfPaths		= 20000;
	private final int numberOfFactors	= 6;

	private LIBORModelMonteCarloSimulationInterface liborMarketModel; 

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat(" ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterMoneyness	= new DecimalFormat(" 000.0%;-000.0%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	public LIBORMarketModelValuationTest() throws CalculationException {

		// Create a libor market model
		liborMarketModel = createLIBORMarketModel(numberOfPaths, numberOfFactors, 0.1 /* Correlation */);
	}

	public static LIBORModelMonteCarloSimulationInterface createLIBORMarketModel(
			int numberOfPaths, int numberOfFactors, double correlationDecayParam) throws CalculationException {

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

		/*
		 * Create a simulation time discretization
		 */
		double lastTime	= 20.0;
		double dt		= 0.5;

		TimeDiscretization timeDiscretization = new TimeDiscretization(0.0, (int) (lastTime / dt), dt);

		/*
		 * Create a volatility structure v[i][j] = sigma_j(t_i)
		 */
		double a = 0.2, b = 0.0, c = 0.25, d = 0.3;
		LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialForm(timeDiscretization, liborPeriodDiscretization, a, b, c, d, false);		

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
		LIBORMarketModelInterface liborMarketModel = new LIBORMarketModel(liborPeriodDiscretization, forwardCurve, new DiscountCurveFromForwardCurve(forwardCurve), covarianceModel, calibrationItems, properties);

		BrownianMotionInterface brownianMotion = new net.finmath.montecarlo.BrownianMotion(timeDiscretization, numberOfFactors, numberOfPaths, 3141 /* seed */);

		ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.PREDICTOR_CORRECTOR);

		return new LIBORModelMonteCarloSimulation(liborMarketModel, process);
	}

	@Test
	public void testBond() throws CalculationException {
		/*
		 * Value a bond
		 */

		DiscountCurveInterface discountCurve = ((LIBORMarketModelInterface)liborMarketModel.getModel()).getDiscountCurve();

		System.out.println("Bond prices:\n");
		System.out.println("Maturity      Simulation       Analytic        Deviation");
		double maxAbsDeviation = 0.0;
		for (double maturity = 0.0; maturity <= 20.0; maturity += 1.0) {
			System.out.print(formatterMaturity.format(maturity) + "          ");

			// Create a bond
			Bond bond = new Bond(maturity);

			// Bond price with Monte Carlo
			double priceOfBond = bond.getValue(liborMarketModel);
			System.out.print(formatterValue.format(priceOfBond) + "          ");

			// Bond price analytic
			double priceOfBondAnalytic = discountCurve.getDiscountFactor(maturity);
			System.out.print(formatterValue.format(priceOfBondAnalytic) + "          ");

			// Relative deviation
			double deviation = (priceOfBond - priceOfBondAnalytic);
			System.out.println(formatterDeviation.format(deviation));

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		// jUnit assertion: condition under which we consider this test successful
		Assert.assertTrue(maxAbsDeviation < 8E-03);
	}

	@Test
	public void testFRA() throws CalculationException {
		/*
		 * Value a fra
		 */
		System.out.println("Par-FRA prices:\n");
		System.out.println("FRA \t\t Value");

		double maxAbsDeviation = 0.0;
		for (double startDate = 0.0; startDate < 20.0; startDate += 0.5) {

			int numberOfPeriods = 1;

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

			System.out.print("(" + formatterMaturity.format(swapTenor[0]) + "," + formatterMaturity.format(swapTenor[numberOfPeriods]) + ")" + "\t");

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
		Assert.assertTrue(maxAbsDeviation < 2E-3);
	}

	@Test
	public void testSwap() throws CalculationException {
		/*
		 * Value a swap
		 */
		System.out.println("Par-Swap prices:\n");
		System.out.println("Swap \t\t\t Value");

		double maxAbsDeviation = 0.0;
		for (double startDate = 0.0; startDate < 15.0; startDate += 0.5) {

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

			System.out.print("(" + formatterMaturity.format(swapTenor[0]) + "," + formatterMaturity.format(swapTenor[numberOfPeriods]) + "," + swapPeriodLength + ")" + "\t");

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
		Assert.assertTrue(maxAbsDeviation < 2E-3);
	}

	@Test
	public void testDigitalCaplet() throws CalculationException {
		/*
		 * Value a digital caplet
		 */
		System.out.println("Digital caplet prices:\n");
		System.out.println("Maturity      Simulation       Analytic        Deviation");

		double maxAbsDeviation = 0.0;
		for (double optionMaturity = 1.0; optionMaturity <= 19.5; optionMaturity += 0.5) {

			double periodStart	= optionMaturity;
			double periodEnd	= optionMaturity+0.5;

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

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
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
		System.out.println("Maturity      Simulation       Analytic        Deviation");

		double maxAbsDeviation = 0.0;
		for (double maturity = 1.0; maturity <= 17.5; maturity += 0.5) {

			double exerciseDate = maturity;
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
			SwaptionAnalyticApproximation swaptionAnalyitc = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VALUE);
			double valueAnalytic = swaptionAnalyitc.getValue(liborMarketModel);
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
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 8E-3);
	}

	@Test
	public void testCaplet() throws CalculationException {
		/*
		 * Value a caplet
		 */
		System.out.println("Caplet prices:\n");
		System.out.println("Maturity      Simulation       Analytic        Deviation");

		double maxAbsDeviation = 0.0;
		for (double maturity = 1.0; maturity <= 19.5; maturity += 0.5) {

			double exerciseDate = maturity;
			System.out.print(formatterMaturity.format(exerciseDate) + "          ");

			int numberOfPeriods = 1;

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
			double valueAnalytic = swaptionAnalytic.getValue(liborMarketModel);
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
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 8E-3);
	}

	@Test
	public void testCapletSmile() throws CalculationException {
		/*
		 * Value a caplet
		 */
		System.out.println("Caplet prices:\n");
		System.out.println("                 Valuation                                               Implied Bachelier Volatility              ");
		System.out.println("Moneyness        Simulation       Analytic         Deviation             Simulation       Analytic         Deviation");

		double maturity = 5.0;
		int numberOfPeriods = 1;
		double swapPeriodLength = 0.5;

		double maxAbsDeviation = 0.0;
		for (double moneyness = 0.5; moneyness < 2.0; moneyness += 0.1) {

			double exerciseDate = maturity;

			// Create a caplet

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

			System.out.print(formatterMoneyness.format(moneyness) + "          ");

			// Value with Monte Carlo
			double valueSimulation = swaptionMonteCarlo.getValue(liborMarketModel);
			double impliedVolSimulation = AnalyticFormulas.bachelierOptionImpliedVolatility(getParSwaprate(liborMarketModel, swapTenor), exerciseDate, swaprate, getSwapAnnuity(liborMarketModel, swapTenor), valueSimulation);

			// Value analytic
			SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VALUE);
			double valueAnalytic = swaptionAnalytic.getValue(liborMarketModel);
			double impliedVolAnalytic = AnalyticFormulas.bachelierOptionImpliedVolatility(getParSwaprate(liborMarketModel, swapTenor), exerciseDate, swaprate, getSwapAnnuity(liborMarketModel, swapTenor), valueAnalytic);

			// Absolute deviation
			double deviationValue = (valueSimulation - valueAnalytic);
			double deviationVol = (impliedVolSimulation - impliedVolAnalytic);

			System.out.print(formatterValue.format(valueSimulation) + "          ");
			System.out.print(formatterValue.format(valueAnalytic) + "          ");
			System.out.print(formatterDeviation.format(deviationValue) + "          ");
			System.out.print(formatterValue.format(impliedVolSimulation) + "          ");
			System.out.print(formatterValue.format(impliedVolAnalytic) + "          ");
			System.out.println(formatterDeviation.format(deviationVol) + "          ");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviationVol));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 5E-4);
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
		ArrayList<CalibrationItem> calibrationItems = new ArrayList<CalibrationItem>();
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
				boolean isUseAnalyticCalibration = true;
				if(isUseAnalyticCalibration) {
					// Use an analytic approximation to the swaption - much faster
					SwaptionAnalyticApproximation swaptionAnalytic = new SwaptionAnalyticApproximation(swaprate, swapTenor, SwaptionAnalyticApproximation.ValueUnit.VOLATILITY);

					calibrationItems.add(new CalibrationItem(swaptionAnalytic, targetValueVolatilty, 1.0));
				}
				else {
					// You may also use full Monte-Carlo calibration - more accurate. Also possible for displaced diffusion.
					SwaptionSimple swaptionMonteCarlo = new SwaptionSimple(swaprate, swapTenor, ValueUnit.VOLATILITY);
					calibrationItems.add(new CalibrationItem(swaptionMonteCarlo, targetValueVolatilty, 1.0));
					
					// Alternative: Calibration to prices
					//Swaption swaptionMonteCarlo = new Swaption(exerciseDate, fixingDates, paymentDates, swaprates);
					//double targetValuePrice = AnalyticFormulas.blackModelSwaptionValue(swaprate, targetValueVolatilty, fixingDates[0], swaprate, getSwapAnnuity(liborMarketModel,swapTenor));
					//calibrationItems.add(new CalibrationItem(swaptionMonteCarlo, targetValuePrice, 1.0));
				}
			}
		}
		System.out.println("");

		/*
		 * Take discretization and forward curve from liborMarketModel
		 */
		TimeDiscretizationInterface timeDiscretization = liborMarketModel.getTimeDiscretization();

		ForwardCurveInterface forwardCurve = ((LIBORMarketModelInterface)liborMarketModel.getModel()).getForwardRateCurve();

		/*
		 * Create a LIBOR Market Model
		 */

		// XXX2 Change covariance model here
		AbstractLIBORCovarianceModelParametric covarianceModelParametric = new LIBORCovarianceModelExponentialForm5Param(timeDiscretization, liborMarketModel.getLiborPeriodDiscretization(), liborMarketModel.getNumberOfFactors());

		// Set model properties
		Map<String, Object> properties = new HashMap<String, Object>();

		// Set calibration properties
		Map<String, Object> calibrationParameters = new HashMap<String, Object>();
		calibrationParameters.put("accuracy", new Double(1E-6));
		calibrationParameters.put("numberOfPaths", new Integer(4000));
		properties.put("calibrationParameters", calibrationParameters);

		LIBORMarketModel liborMarketModelCalibrated = new LIBORMarketModel(
				this.liborMarketModel.getLiborPeriodDiscretization(),
				forwardCurve, null, covarianceModelParametric, calibrationItems.toArray(new CalibrationItem[0]), properties);

		/*
		 * Test our calibration
		 */
		ProcessEulerScheme process = new ProcessEulerScheme(
				new net.finmath.montecarlo.BrownianMotion(timeDiscretization,
						numberOfFactors, numberOfPaths, 3141 /* seed */));

		double[] param = ((AbstractLIBORCovarianceModelParametric) liborMarketModelCalibrated.getCovarianceModel()).getParameter();
		for (double p : param) System.out.println(p);

		net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation simulationCalibrated = new net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation(
				liborMarketModelCalibrated, process);

		double deviationSum = 0.0;
		for (int i = 0; i < calibrationItems.size(); i++) {
			AbstractLIBORMonteCarloProduct calibrationProduct = calibrationItems.get(i).calibrationProduct;
			double valueModel = calibrationProduct.getValue(simulationCalibrated);
			double valueTarget = calibrationItems.get(i).calibrationTargetValue;
			deviationSum += (valueModel-valueTarget);
			System.out.println("Model: " + formatterValue.format(valueModel) + "\t Target: " + formatterValue.format(valueTarget) + "\t Deviation: " + formatterDeviation.format(valueModel-valueTarget));
		}
		System.out.println("Mean Deviation:" + deviationSum/calibrationItems.size());
		System.out.println("__________________________________________________________________________________________\n");
	}

	private static double getParSwaprate(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) throws CalculationException {
		return net.finmath.marketdata.products.Swap.getForwardSwapRate(new TimeDiscretization(swapTenor), new TimeDiscretization(swapTenor), liborMarketModel.getModel().getForwardRateCurve(), liborMarketModel.getModel().getDiscountCurve());
	}

	private static double getSwapAnnuity(LIBORModelMonteCarloSimulationInterface liborMarketModel, double[] swapTenor) throws CalculationException {
		return net.finmath.marketdata.products.SwapAnnuity.getSwapAnnuity(new TimeDiscretization(swapTenor), liborMarketModel.getModel().getDiscountCurve());
	}	
}