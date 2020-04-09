/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 19.01.2004, 21.12.2012
 */
package net.finmath.functions;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;


/**
 * Testing the analytic formulas of the Bachelier model.
 *
 * There is also a test comparing analytic formulas with a Monte-Carlo simulation,
 * see {@link net.finmath.montecarlo.assetderivativevaluation.BachelierModelMonteCarloValuationTest}.
 *
 * @author Christian Fries
 */
public class BachelierModelTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	// Java DecimalFormat for our output format
	private final DecimalFormat numberFormatStrike	= new DecimalFormat("     0.00 ");
	private final DecimalFormat numberFormatValue		= new DecimalFormat(" 0.00E00");
	private final DecimalFormat numberFormatDeviation	= new DecimalFormat("  0.00E00; -0.00E00");

	@Test
	public void testDelta() throws CalculationException
	{
		final double optionMaturity	= 1.0;
		final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;

		// Test options with different strike
		System.out.println("Calculation of Option Delta (European options with maturity 1.0):");
		System.out.println(" Strike \t Finite Diff\t Analytic\t Diff");

		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.10) {

			final double shift = initialValue * 1E-6;

			// Calculate the finite difference of the analytic value
			final double deltaFiniteDiffAnalytic	=
					(
							net.finmath.functions.BachelierModel.bachelierHomogeneousOptionValue(forward+shift/payoffUnit, volatility, optionMaturity, optionStrike, payoffUnit)
							- net.finmath.functions.BachelierModel.bachelierHomogeneousOptionValue(forward-shift/payoffUnit, volatility, optionMaturity, optionStrike, payoffUnit)
							)/(2*shift);

			// Calculate the analytic value
			final double deltaAnalytic	= net.finmath.functions.BachelierModel.bachelierHomogeneousOptionDelta(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) +
					"\t" + numberFormatValue.format(deltaFiniteDiffAnalytic) +
					"\t" + numberFormatValue.format(deltaAnalytic) +
					"\t" + numberFormatDeviation.format((deltaFiniteDiffAnalytic-deltaAnalytic)));

			Assert.assertTrue(Math.abs(deltaAnalytic-deltaFiniteDiffAnalytic) < 1E-10);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testVega() throws CalculationException
	{
		final double optionMaturity	= 1.0;
		final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;

		// Test options with different strike
		System.out.println("Calculation of Option Vega (European options with maturity 1.0):");
		System.out.println(" Strike \t Finite Diff\t Analytic\t Diff");

		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.10) {

			final double shift = initialValue * 1E-6;

			// Calculate the finite difference of the analytic value
			final double vegaFiniteDiffAnalytic	=
					(
							net.finmath.functions.BachelierModel.bachelierHomogeneousOptionValue(forward, volatility+shift, optionMaturity, optionStrike, payoffUnit)
							- net.finmath.functions.BachelierModel.bachelierHomogeneousOptionValue(forward, volatility-shift, optionMaturity, optionStrike, payoffUnit)
							)/(2*shift);

			// Calculate the analytic value
			final double vegaAnalytic	= net.finmath.functions.BachelierModel.bachelierHomogeneousOptionVega(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) +
					"\t" + numberFormatValue.format(vegaFiniteDiffAnalytic) +
					"\t" + numberFormatValue.format(vegaAnalytic) +
					"\t" + numberFormatDeviation.format((vegaFiniteDiffAnalytic-vegaAnalytic)));

			Assert.assertTrue(Math.abs(vegaAnalytic-vegaFiniteDiffAnalytic) < 1E-10);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testInhomogenousDelta() throws CalculationException
	{
		final double optionMaturity	= 1.0;
		final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;

		// Test options with different strike
		System.out.println("Calculation of Option Delta (European options with maturity 1.0):");
		System.out.println(" Strike \t Finite Diff\t Analytic\t Diff");

		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.10) {

			final double shift = initialValue * 1E-6;

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
					"\t" + numberFormatValue.format(deltaFiniteDiffAnalytic) +
					"\t" + numberFormatValue.format(deltaAnalytic) +
					"\t" + numberFormatDeviation.format((deltaFiniteDiffAnalytic-deltaAnalytic)));

			Assert.assertTrue(Math.abs(deltaAnalytic-deltaFiniteDiffAnalytic) < 1E-10);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testInhomogenousVega() throws CalculationException
	{
		final double optionMaturity	= 1.0;
		final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;

		// Test options with different strike
		System.out.println("Calculation of Option Vega (European options with maturity 1.0):");
		System.out.println(" Strike \t Finite Diff\t Analytic\t Diff");

		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.10) {

			final double shift = initialValue * 1E-6;

			// Calculate the finite difference of the analytic value
			final double vegaFiniteDiffAnalytic	=
					(
							net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward, volatility+shift, optionMaturity, optionStrike, payoffUnit)
							- net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward, volatility-shift, optionMaturity, optionStrike, payoffUnit)
							)/(2*shift);

			// Calculate the analytic value
			final double vegaAnalytic	= net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionVega(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			// Print result
			System.out.println(numberFormatStrike.format(optionStrike) +
					"\t" + numberFormatValue.format(vegaFiniteDiffAnalytic) +
					"\t" + numberFormatValue.format(vegaAnalytic) +
					"\t" + numberFormatDeviation.format((vegaFiniteDiffAnalytic-vegaAnalytic)));

			Assert.assertTrue(Math.abs(vegaAnalytic-vegaFiniteDiffAnalytic) < 1E-10);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testHomogeneousRandomVariableImplementations() throws CalculationException
	{
		final double optionMaturity	= 1.0;
		final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;

		// Test options with different strike
		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.10) {

			final double value = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);
			final double delta = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionDelta(forward, volatility, optionMaturity, optionStrike, payoffUnit);
			final double vega = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionVega(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			final RandomVariable valueRandomVariable = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionValue(new Scalar(forward), new Scalar(volatility), optionMaturity, optionStrike, new Scalar(payoffUnit));
			final RandomVariable deltaRandomVariable = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionDelta(new Scalar(forward), new Scalar(volatility), optionMaturity, optionStrike, new Scalar(payoffUnit));
			final RandomVariable vegaRandomVariable = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionVega(new Scalar(forward), new Scalar(volatility), optionMaturity, optionStrike, new Scalar(payoffUnit));

			Assert.assertEquals("value", value, valueRandomVariable.doubleValue(), 1E-10);
			Assert.assertEquals("delta", delta, deltaRandomVariable.doubleValue(), 1E-10);
			Assert.assertEquals("vega", vega, vegaRandomVariable.doubleValue(), 1E-10);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testInhomogeneousRandomVariableImplementations() throws CalculationException
	{
		final double optionMaturity	= 1.0;
		final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
		final double forward		= initialValue / payoffUnit;

		// Test options with different strike
		for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.10) {

			final double value = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);
			final double delta = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionDelta(forward, volatility, optionMaturity, optionStrike, payoffUnit);
			final double vega = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionVega(forward, volatility, optionMaturity, optionStrike, payoffUnit);

			final RandomVariable valueRandomVariable = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(new Scalar(forward), new Scalar(volatility), optionMaturity, optionStrike, new Scalar(payoffUnit));
			final RandomVariable deltaRandomVariable = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionDelta(new Scalar(forward), new Scalar(volatility), optionMaturity, optionStrike, new Scalar(payoffUnit));
			final RandomVariable vegaRandomVariable = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionVega(new Scalar(forward), new Scalar(volatility), optionMaturity, optionStrike, new Scalar(payoffUnit));

			Assert.assertEquals("value", value, valueRandomVariable.doubleValue(), 1E-10);
			Assert.assertEquals("delta", delta, deltaRandomVariable.doubleValue(), 1E-10);
			Assert.assertEquals("vega", vega, vegaRandomVariable.doubleValue(), 1E-10);
		}
		System.out.println("__________________________________________________________________________________________\n");
	}

	@Test
	public void testImpliedVolatility() throws CalculationException
	{
		final double optionMaturity	= 1.0;

		// Test conversion with different strike
		for(double volatility = 1E-1; volatility < 10000.0; volatility *= 10) {
			for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.20) {
				for(double riskFreeRate = 0.00; riskFreeRate < 0.20; riskFreeRate += 0.02) {

					final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
					final double forward		= initialValue / payoffUnit;

					final double optionValue = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);
					final double volatilityImplied = net.finmath.functions.BachelierModel.bachelierHomogeneousOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);

					final String testCase = "volatility = " + volatility + ","
							+ "optionStrike" + optionStrike + ","
							+ "riskFreeRate" + riskFreeRate + ".";

					Assert.assertEquals(testCase, volatility, volatilityImplied, 1E-7);
				}
			}
		}
	}

	@Test
	public void testInhomogeneousImpliedVolatility() throws CalculationException
	{
		final double optionMaturity	= 1.0;

		// Test conversion with different strike
		for(double volatility = 1E-1; volatility < 10000.0; volatility *= 10) {
			for(double optionStrike = 0.60; optionStrike < 1.50; optionStrike += 0.20) {
				for(double riskFreeRate = 0.00; riskFreeRate < 0.20; riskFreeRate += 0.02) {

					final double payoffUnit		= Math.exp(- riskFreeRate * optionMaturity);
					final double forward		= initialValue / payoffUnit;

					final double optionValue = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);
					final double volatilityImplied = net.finmath.functions.BachelierModel.bachelierInhomogeneousOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);

					final String testCase = "volatility = " + volatility + ","
							+ "optionStrike" + optionStrike + ","
							+ "riskFreeRate" + riskFreeRate + ".";

					Assert.assertEquals(testCase, volatility, volatilityImplied, 1E-9);
				}
			}
		}
	}
}
