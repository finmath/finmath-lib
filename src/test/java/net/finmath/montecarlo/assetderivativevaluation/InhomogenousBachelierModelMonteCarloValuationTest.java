/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 19.01.2004, 21.12.2012
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.models.InhomogenousBachelierModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanOption;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;


/**
 * This class represents a collection of several "tests" illustrating different aspects
 * related to the Monte-Carlo Simulation and derivative pricing (using a simple
 * Bachelier model).
 *
 * @author Christian Fries
 */
public class InhomogenousBachelierModelMonteCarloValuationTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 100000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.5;

	private final int		seed				= 3141;

	private AssetModelMonteCarloSimulationModel model = null;

	/**
	 * This main method will test a Monte-Carlo simulation of a Black-Scholes model and some valuations
	 * performed with this model.
	 *
	 * @param args Arguments - not used.
	 * @throws CalculationException Thrown if s.th. went wrong during calculation (check getCause for details).
	 * @throws InterruptedException Thrown if multi-threadded execution is interrupted.
	 */
	public static void main(final String[] args) throws CalculationException, InterruptedException
	{
		final InhomogenousBachelierModelMonteCarloValuationTest pricingTest = new InhomogenousBachelierModelMonteCarloValuationTest();

		/*
		 * Read input
		 */
		final int testNumber = readTestNumber();

		final long start = System.currentTimeMillis();

		switch(testNumber) {
		case 1:
			// This test requires a MonteCarloBlackScholesModel and will not work with others models
			pricingTest.testEuropeanCall();
			break;
		case 2:
			pricingTest.testModelProperties();
			break;
		case 3:
			pricingTest.testModelRandomVariable();
			break;
		case 4:
			pricingTest.testEuropeanAsianBermudanOption();
			break;
		case 5:
			pricingTest.testMultiThreaddedValuation();
			break;
		case 6:
			// This test requires a MonteCarloBlackScholesModel and will not work with others models
			pricingTest.testEuropeanCallDelta();
			break;
		case 7:
			// This test requires a MonteCarloBlackScholesModel and will not work with others models
			pricingTest.testEuropeanCallVega();
			break;
		default:
			throw new IllegalArgumentException("Unknown test.");
		}

		final long end = System.currentTimeMillis();

		System.out.println("\nCalculation time required: " + (end-start)/1000.0 + " seconds.");
	}

	public InhomogenousBachelierModelMonteCarloValuationTest() {
		super();
	}

	private static int readTestNumber() {
		System.out.println("Please select a test to run (click in this window and enter a number):");
		System.out.println("\t 1: Valuation of European call options (with different strikes).");
		System.out.println("\t 2: Some model properties.");
		System.out.println("\t 3: Print some realizations of the S(1).");
		System.out.println("\t 4: Valuation of European, Asian, Bermudan option.");
		System.out.println("\t 5: Multi-Threadded valuation of some ten thousand Asian options.");
		System.out.println("\t 6: Sensitivity (Delta) of European call options (with different strikes) using different methods.");
		System.out.println("\t 7: Sensitivity (Vega) of European call options (with different strikes) using different methods.");
		System.out.println();
		System.out.print("Test to run: ");

		//  open up standard input
		final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));

		int testNumber = 0;
		try {
			final String test = br.readLine();
			testNumber = Integer.valueOf(test);
		} catch (final IOException ioe) {
			System.out.println("IO error trying to read test number!");
			System.exit(1);
		}

		System.out.println();
		return testNumber;
	}

	public AssetModelMonteCarloSimulationModel getModel()
	{
		/*
		 * Lazy initialize the model
		 */
		if(model == null) {
			// Create the time discretization
			final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, deltaT);

			// Create the model
			final ProcessModel bachelierModel = new InhomogenousBachelierModel(initialValue, riskFreeRate, volatility);

			// Create a corresponding MC process
			final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(bachelierModel, new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

			model = new MonteCarloAssetModel(bachelierModel, process);
		}

		return model;
	}

	/**
	 * @throws CalculationException Thrown if s.th. went wrong during calculation (check getCause for details).
	 */
	@Test
	public void testEuropeanCall() throws CalculationException
	{
		/*
		 * Create the valuation model (see <code>getModel</code>)
		 */
		final AssetModelMonteCarloSimulationModel model = getModel();

		// Java DecimalFormat for our output format
		final DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		final DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		final DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		// Test options with different strike
		System.out.println("Valuation of European Options");
		System.out.println(" Strike \t Monte-Carlo \t Analytic \t Deviation");

		final double initialValue	= this.getInitialValue();
		final double riskFreeRate	= this.getRiskFreeRate();
		final double volatility	= this.getVolatility();

		final double optionMaturity	= 5.0;

		final double payoffUnit	= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;

		for(double optionStrike = 0.75/payoffUnit; optionStrike < 1.25/payoffUnit; optionStrike += 0.05/payoffUnit) {

			// Create a product
			final EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);
			// Value the product with Monte Carlo
			final double valueMonteCarlo	= callOption.getValue(model);

			// Calculate the analytic value
			final double valueAnalytic	= net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) +
					"\t" + numberFormatValue.format(valueMonteCarlo) +
					"\t" + numberFormatValue.format(valueAnalytic) +
					"\t" + numberFormatDeviation.format(valueMonteCarlo-valueAnalytic));

			Assert.assertTrue(Math.abs(valueMonteCarlo-valueAnalytic) < 1E-02);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	/**
	 * Test some properties of the model
	 *
	 * @throws CalculationException Thrown if s.th. went wrong during calculation (check getCause for details).
	 */
	@Test
	public void testModelProperties() throws CalculationException {
		/*
		 * Create the valuation model (see <code>getModel</code>)
		 */
		final AssetModelMonteCarloSimulationModel model = getModel();

		final TimeDiscretization modelTimeDiscretization = model.getTimeDiscretization();

		System.out.println("Time \tAverage \t\tVariance");
		for(final double time : modelTimeDiscretization) {
			final RandomVariable assetValue = model.getAssetValue(time, 0);
			final RandomVariable numeraire = model.getNumeraire(time);

			final double average	= assetValue.getAverage();
			final double variance	= assetValue.getVariance();
			final double error	= assetValue.getStandardError();

			final DecimalFormat formater2Digits = new DecimalFormat("0.00");
			final DecimalFormat formater4Digits = new DecimalFormat("0.0000");
			System.out.println(formater2Digits.format(time) + " \t" + formater4Digits.format(average) + "\t+/- " + formater4Digits.format(error) + "\t" + formater4Digits.format(variance));
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	/**
	 * @throws CalculationException Thrown if s.th. went wrong during calculation (check getCause for details).
	 */
	@Test
	public void testModelRandomVariable() throws CalculationException {
		/*
		 * Create the valuation model (see <code>getModel</code>)
		 */
		final AssetModelMonteCarloSimulationModel model = getModel();

		final RandomVariable stockAtTimeOne = model.getAssetValue(1.0, 0);

		System.out.println("The first 100 realizations of the " + stockAtTimeOne.size() + " realizations of S(1) are:");
		System.out.println("Path\tValue");
		for(int i=0; i<100;i++) {
			System.out.println(i + "\t" + stockAtTimeOne.get(i));
		}

		final double spot = stockAtTimeOne.div(model.getNumeraire(1.0)).mult(model.getNumeraire(model.getTime(0))).getAverage();
		System.out.println("Expectation of S(1)/N(1)*N(0) = " + spot + " (expected " + initialValue + ")");

		System.out.println("__________________________________________________________________________________________\n");

		Assert.assertEquals(initialValue, spot, 2E-3);
	}

	/**
	 * Evaluates different options (European, Asian, Bermudan) using the given model.
	 *
	 * The options share the same maturity and strike for the at t=3.0.
	 * Observations which can be made:
	 * <ul>
	 * <li>The Asian is cheaper than the European since averaging reduces the volatility.
	 * <li>The European is cheaper than the Bermudan since exercises into the European is one (out of may) exercises strategies of the Bermudan.
	 * </ul>
	 *
	 * @throws CalculationException Thrown if s.th. went wrong during calculation (check getCause for details).
	 */
	@Test
	@Ignore
	public void testEuropeanAsianBermudanOption() throws CalculationException {
		/*
		 * Create the valuation model (see <code>getModel</code>)
		 */
		final AssetModelMonteCarloSimulationModel model = getModel();

		/*
		 * Common parameters
		 */
		final double maturity = 3.0;
		final double strike = 1.07;

		/*
		 * European Option
		 */
		final EuropeanOption myEuropeanOption = new EuropeanOption(maturity,strike);
		final double valueOfEuropeanOption = myEuropeanOption.getValue(model);

		/*
		 * Asian Option
		 */
		final double[] averagingPoints = { 1.0, 1.5, 2.0, 2.5 , 3.0 };

		final AsianOption myAsianOption = new AsianOption(maturity,strike, new TimeDiscretizationFromArray(averagingPoints));
		final double valueOfAsianOption = myAsianOption.getValue(model);

		/*
		 * Bermudan Option
		 */
		final double[] exerciseDates	= { 1.0,  2.0,  3.0};
		final double[] notionals		= { 1.20, 1.10, 1.0};
		final double[] strikes		= { 1.03, 1.05, 1.07 };

		// Lower bound method
		final BermudanOption myBermudanOptionLowerBound = new BermudanOption(exerciseDates, notionals, strikes, BermudanOption.ExerciseMethod.ESTIMATE_COND_EXPECTATION);
		final double valueOfBermudanOptionLowerBound = myBermudanOptionLowerBound.getValue(model);

		// Upper bound method
		final BermudanOption myBermudanOptionUpperBound = new BermudanOption(exerciseDates, notionals, strikes, BermudanOption.ExerciseMethod.UPPER_BOUND_METHOD);
		final double valueOfBermudanOptionUpperBound = myBermudanOptionUpperBound.getValue(model);

		/*
		 * Output
		 */
		System.out.println("Value of Asian Option is \t"	+ valueOfAsianOption);
		System.out.println("Value of European Option is \t"	+ valueOfEuropeanOption);
		System.out.println("Value of Bermudan Option is \t"	+ "(" + valueOfBermudanOptionLowerBound + "," + valueOfBermudanOptionUpperBound + ")");

		Assert.assertTrue(valueOfAsianOption < valueOfEuropeanOption);
		Assert.assertTrue(valueOfBermudanOptionLowerBound < valueOfBermudanOptionUpperBound);
		Assert.assertTrue(valueOfEuropeanOption < valueOfBermudanOptionUpperBound);
	}

	/**
	 * Evaluates 100000 Asian options in 10 parallel threads (each valuing 10000 options)
	 *
	 * @throws InterruptedException Thrown if multi-threadded execution is interrupted.
	 */
	public void testMultiThreaddedValuation() throws InterruptedException {
		final double[] averagingPoints = { 0.5, 1.0, 1.5, 2.0, 2.5, 2.5, 3.0, 3.0 , 3.0, 3.5, 4.5, 5.0 };
		final double maturity = 5.0;
		final double strike = 1.07;

		final int			numberOfThreads	= 10;
		final Thread[]	myThreads		= new Thread[numberOfThreads];

		for(int k=0; k<myThreads.length; k++) {

			final int threadNummer = k;

			// Create a runnable - piece of code which can be run in parallel.
			final Runnable myRunnable = new Runnable() {
				@Override
				public void run() {
					try {
						for(int i=0;i<10000; i++) {
							final AsianOption myAsianOption = new AsianOption(maturity,strike, new TimeDiscretizationFromArray(averagingPoints));
							final double valueOfAsianOption = myAsianOption.getValue(model);
							System.out.println("Thread " + threadNummer + ": Value of Asian Option " + i + " is " + valueOfAsianOption);
						}
					} catch (final CalculationException e) {
					}
				}
			};

			// Create a thread (will run asynchronously)
			myThreads[k] = new Thread(myRunnable);
			myThreads[k].start();
		}

		// Wait for all threads to complete
		for(int i=0; i<myThreads.length; i++) {
			myThreads[i].join();
		}

		// Threads are completed at this point
	}

	/**
	 * @throws CalculationException Thrown if s.th. went wrong during calculation (check getCause for details).
	 */
	@Test
	public void testEuropeanCallDelta() throws CalculationException
	{
		/*
		 * Create the valuation model (see <code>getModel</code>)
		 */
		final AssetModelMonteCarloSimulationModel model = getModel();

		// Java DecimalFormat for our output format
		final DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		final DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		final DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		final double initialValue	= this.getInitialValue();
		final double riskFreeRate	= this.getRiskFreeRate();
		final double volatility	= this.getVolatility();

		// Test options with different strike
		System.out.println("Calculation of Option Delta (European options with maturity 1.0):");
		System.out.println(" Strike \t MC Fin.Diff.\t Analytic FD\t Analytic\t Diff MC-AN\t Diff FD-AN");

		final double optionMaturity	= 1.0;
		final double payoffUnit	= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;
		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.05) {

			// Create a product
			final EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);

			// Value the product with Monte Carlo
			final double shift = initialValue * 1E-6;

			final Map<String,Object> dataUpShift = new HashMap<>();
			dataUpShift.put("initialValue", initialValue + shift);

			final double valueUpShift	= (Double)(callOption.getValuesForModifiedData(model, dataUpShift).get("value"));

			final Map<String,Object> dataDownShift = new HashMap<>();
			dataDownShift.put("initialValue", initialValue - shift);
			final double valueDownShift	= (Double)(callOption.getValuesForModifiedData(model, dataDownShift).get("value"));

			// Calculate the finite difference of the monte-carlo value
			final double delta = (valueUpShift-valueDownShift) / ( 2 * shift );

			// Calculate the finite difference of the analytic value
			final double deltaFiniteDiffAnalytic	=
					(
							net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward+shift/payoffUnit, volatility, optionMaturity, optionStrike, payoffUnit)
							- net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward-shift/payoffUnit, volatility, optionMaturity, optionStrike, payoffUnit)
							)/(2*shift);

			// Calculate the analytic value
			final double deltaAnalytic	= net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionDelta(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) +
					"\t" + numberFormatValue.format(delta) +
					"\t" + numberFormatValue.format(deltaFiniteDiffAnalytic) +
					"\t" + numberFormatValue.format(deltaAnalytic) +
					"\t" + numberFormatDeviation.format((delta-deltaAnalytic)) +
					"\t" + numberFormatDeviation.format((deltaFiniteDiffAnalytic-deltaAnalytic)));

			Assert.assertTrue(Math.abs(delta-deltaFiniteDiffAnalytic) < 1E-02);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	/**
	 * @throws CalculationException Thrown if s.th. went wrong during calculation (check getCause for details).
	 */
	@Test
	public void testEuropeanCallVega() throws CalculationException
	{
		/*
		 * Create the valuation model (see <code>getModel</code>)
		 */
		final AssetModelMonteCarloSimulationModel model = getModel();

		// Java DecimalFormat for our output format
		final DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
		final DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
		final DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

		final double initialValue	= this.getInitialValue();
		final double riskFreeRate	= this.getRiskFreeRate();
		final double volatility	= this.getVolatility();

		// Test options with different strike
		System.out.println("Calculation of Option Vega (European options with maturity 1.0):");
		System.out.println(" Strike \t MC Fin.Diff.\t Analytic FD\t Analytic\t Diff MC-AN\t Diff FD-AN");

		final double optionMaturity	= 5.0;
		final double payoffUnit	= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;
		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.05) {

			// Create a product
			final EuropeanOption		callOption	= new EuropeanOption(optionMaturity, optionStrike);

			// Value the product with Monte Carlo
			final double shift = volatility * 1E-5;

			final Map<String,Object> dataUpShift = new HashMap<>();
			dataUpShift.put("volatility", volatility + shift);

			final double valueUpShift	= (Double)(callOption.getValuesForModifiedData(model, dataUpShift).get("value"));

			final Map<String,Object> dataDownShift = new HashMap<>();
			dataDownShift.put("volatility", volatility - shift);
			final double valueDownShift	= (Double)(callOption.getValuesForModifiedData(model, dataDownShift).get("value"));

			// Calculate the finite difference of the monte-carlo value
			final double vega = (valueUpShift-valueDownShift) / ( 2 * shift );

			final double volatilityUp = (volatility+shift);
			final double volatilityDown = (volatility-shift);
			// Calculate the finite difference of the analytic value
			final double vegaFiniteDiffAnalytic	=
					(
							net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward, volatilityUp, optionMaturity, optionStrike, payoffUnit)
							-
							net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward, volatilityDown, optionMaturity, optionStrike, payoffUnit)
							)/(2*shift);

			// Calculate the analytic value
			final double vegaAnalytic	= net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionVega(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) +
					"\t" + numberFormatValue.format(vega) +
					"\t" + numberFormatValue.format(vegaFiniteDiffAnalytic) +
					"\t" + numberFormatValue.format(vegaAnalytic) +
					"\t" + numberFormatDeviation.format((vega-vegaAnalytic)) +
					"\t" + numberFormatDeviation.format((vegaFiniteDiffAnalytic-vegaAnalytic)));

			Assert.assertTrue(Math.abs(vega-vegaFiniteDiffAnalytic) < 1E-02);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	/**
	 * @return the initialValue
	 */
	public double getInitialValue() {
		return initialValue;
	}

	/**
	 * @return the riskFreeRate
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * @return the volatility
	 */
	public double getVolatility() {
		return volatility;
	}
}
