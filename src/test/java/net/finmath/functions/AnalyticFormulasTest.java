/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.05.2016
 */

package net.finmath.functions;

import java.text.DecimalFormat;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;

/**
 * Unit tests for {@link net.finmath.functions.AnalyticFormulas}.
 *
 * @author Christian Fries
 */
public class AnalyticFormulasTest {

	static final DecimalFormat formatterReal2 = new DecimalFormat("#0.00");
	private final boolean isPrintOutVerbose = false;

	@Test
	public void testBlackModelCapletImpliedVol() {
		double forward = 0.04;
		double volatility = 0.30;
		double optionMaturity = 4.0;
		double optionStrike = 0.045;
		double periodLength = 0.5;
		double discountFactor = 0.9;
		
		double optionValue = AnalyticFormulas.blackModelCapletValue(forward, volatility, optionMaturity, optionStrike, periodLength, discountFactor);
		
		final double impliedVol1 = AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, discountFactor, optionValue/periodLength);
		final double impliedVol2 = AnalyticFormulas.blackModelCapletImpliedVolatility(forward, optionMaturity, optionStrike, periodLength, discountFactor, optionValue);

		Assertions.assertEquals(volatility, impliedVol1, 1E-12, "Implied Volatility");
		Assertions.assertEquals(volatility, impliedVol2, 1E-12, "Implied Volatility");
	}

	@Test
	public void testBlackModelDigitalCapletDelta() {

		final double forward = 0.05;
		final double volatility = 0.30;
		final double periodLength = 0.5;
		final double discountFactor = 0.9;
		final double optionMaturity = 2.0;
		final double optionStrike = 0.06;

		final double shift = 1E-5;

		final double valueDn = AnalyticFormulas.blackModelDigitalCapletValue(forward-shift, volatility, periodLength, discountFactor, optionMaturity, optionStrike);
		final double valueUp = AnalyticFormulas.blackModelDigitalCapletValue(forward+shift, volatility, periodLength, discountFactor, optionMaturity, optionStrike);
		final double deltaFiniteDifference = (valueUp - valueDn) / (2*shift);

		final double deltaAnalytic = AnalyticFormulas.blackModelDigitalCapletDelta(forward, volatility, periodLength, discountFactor, optionMaturity, optionStrike);

		Assertions.assertEquals(deltaAnalytic, deltaFiniteDifference, 1E-5, "Digital Caplet Delta");
	}

	@Test
	public void testBachelierOptionImpliedVolatility() {
		final double spot = 100;
		final double riskFreeRate = 0.05;
		for(double volatilityNormal = 5.0 / 100.0 * spot; volatilityNormal < 1.0 * spot; volatilityNormal += 5.0 / 100.0 * spot) {
			for(double optionMaturity = 0.5; optionMaturity < 10; optionMaturity += 0.25) {
				for(double moneynessInStdDev = -6.0; moneynessInStdDev <= 6.0; moneynessInStdDev += 0.5) {

					final double moneyness = moneynessInStdDev * volatilityNormal * Math.sqrt(optionMaturity);

					final double volatility = volatilityNormal;
					final double forward = spot * Math.exp(riskFreeRate * optionMaturity);
					final double optionStrike = forward + moneyness;
					final double payoffUnit = Math.exp(-riskFreeRate * optionMaturity);

					final double optionValue = AnalyticFormulas.bachelierOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);
					final double impliedVolatility = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);

					if(isPrintOutVerbose) {
						System.out.println(formatterReal2.format(optionMaturity) + " \t" + formatterReal2.format(moneyness) + " \t" + formatterReal2.format(optionValue) + " \t" + formatterReal2.format(volatility) + " \t" + formatterReal2.format(impliedVolatility));
					}
					Assert.assertEquals(volatility, impliedVolatility, 1E-3);
				}
			}
		}
	}

	@Test
	public void testBachelierOptionDelta() {
		final double spot = 100;
		final double riskFreeRate = 0.05;
		for(double volatilityNormal = 5.0 / 100.0 * spot; volatilityNormal < 1.0 * spot; volatilityNormal += 5.0 / 100.0 * spot) {
			for(double optionMaturity = 0.5; optionMaturity < 10; optionMaturity += 0.25) {
				for(double moneynessInStdDev = -6.0; moneynessInStdDev <= 6.0; moneynessInStdDev += 0.5) {

					final double moneyness = moneynessInStdDev * volatilityNormal * Math.sqrt(optionMaturity);

					final double volatility = volatilityNormal;
					final double forward = spot * Math.exp(riskFreeRate * optionMaturity);
					final double optionStrike = forward + moneyness;
					final double payoffUnit = Math.exp(-riskFreeRate * optionMaturity);

					final double optionDelta = AnalyticFormulas.bachelierOptionDelta(forward, volatility, optionMaturity, optionStrike, payoffUnit);

					final double epsilon = 1E-5*spot;
					final double forwardUp = (spot+epsilon) * Math.exp(riskFreeRate * optionMaturity);
					final double forwardDn = (spot-epsilon) * Math.exp(riskFreeRate * optionMaturity);
					final double optionDeltaFiniteDifference = (AnalyticFormulas.bachelierOptionValue(forwardUp, volatility, optionMaturity, optionStrike, payoffUnit)-AnalyticFormulas.bachelierOptionValue(forwardDn, volatility, optionMaturity, optionStrike, payoffUnit))/(2*epsilon);

					if(isPrintOutVerbose) {
						System.out.println(formatterReal2.format(optionMaturity) + " \t" + formatterReal2.format(moneyness) + " \t" + formatterReal2.format(optionDelta) + " \t" + formatterReal2.format(optionDeltaFiniteDifference));
					}
					Assert.assertEquals(optionDelta, optionDeltaFiniteDifference, 1E-8);
				}
			}
		}
	}

	@Test
	public void testSABRCalibration() throws SolverException {
		/*
		 * Using Levenberg Marquardt to calibrate SABR
		 */

		final double[] givenStrikes		= {  -0.01, -0.005,    0.0,  0.005,   0.01,   0.02,   0.03 };
		final double[] givenVolatilities	= { 0.0055, 0.0059, 0.0060, 0.0061, 0.0063, 0.0066, 0.0070 };

		final double underlying = 0.0076;
		final double maturity = 20;

		final double alpha = 0.006;
		final double beta = 0.05;
		final double rho = 0.95;
		final double nu = 0.075;
		final double displacement = 0.02;


		final double[] initialParameters = { alpha, beta, rho, nu, displacement };
		final double[] targetValues = givenVolatilities;
		final int maxIteration = 500;
		final int numberOfThreads = 8;

		for(double displacement2 = 0.5; displacement2>0; displacement2 -= 0.001) {
			givenVolatilities[0] = givenVolatilities[0] + 0.00001;
			final double displacement3 = displacement2;
			final LevenbergMarquardt lm = new LevenbergMarquardt(initialParameters, targetValues, maxIteration, numberOfThreads) {
				private static final long serialVersionUID = -4799790311777696204L;

				@Override
				public void setValues(final double[] parameters, final double[] values) {
					for(int strikeIndex = 0; strikeIndex < givenStrikes.length; strikeIndex++) {
						final double strike = givenStrikes[strikeIndex];
						values[strikeIndex] = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(parameters[0] /* alpha */, parameters[1] /* beta */, parameters[2] /* rho */, parameters[3] /* nu */, parameters[4] /* displacement */, underlying, strike, maturity);
					}
				}
			};
			lm.setErrorTolerance(1E-16);

			lm.run();

			final double[] bestParameters = lm.getBestFitParameters();

			if(isPrintOutVerbose) {
				System.out.println(givenVolatilities[0] + "\t" + lm.getRootMeanSquaredError() + "\t" + Arrays.toString(bestParameters));
			}
		}
	}

	@Test
	public void testSABRSkewApproximation() {
		double alpha, beta, rho, nu, displacement, underlying, maturity;
		for(int testCase = 1; testCase <= 3; testCase++) {
			switch (testCase) {
			case 1:
			default:
				alpha = 0.1122;
				beta = 0.9;
				rho = 0.2;
				nu = 0.4;
				displacement = 0.02;
				underlying = 0.015;
				maturity = 1;
				break;

			case 2:
				alpha = 0.006;
				beta = 0.0001;
				rho = 0.95;
				nu = 0.075;
				displacement = 0.02;
				underlying = 0.0076;
				maturity = 20;
				break;

			case 3:
				alpha = 0.1122;
				beta = 0.2;
				rho = 0.9;
				nu = 0.4;
				displacement = 0.02;
				underlying = 0.015;
				maturity = 10;

				break;
			}
			final double riskReversal = AnalyticFormulas.sabrNormalVolatilitySkewApproximation(alpha, beta, rho, nu, displacement, underlying, maturity);

			final double epsilon = 1E-4;
			final double valueUp = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying+epsilon, maturity);
			final double valueDn = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying-epsilon, maturity);
			final double riskReversalNumerical = (valueUp-valueDn) / 2 / epsilon;

			System.out.println(riskReversal);
			System.out.println(riskReversalNumerical);

			Assert.assertEquals("RR", riskReversalNumerical, riskReversal, 5.0/100/100);
		}
	}

	@Test
	public void testSABRCurvatureApproximation() {

		double alpha, beta, rho, nu, displacement, underlying, maturity;
		for(int testCase = 1; testCase <= 3; testCase++) {
			switch (testCase) {
			case 1:
			default:
				alpha = 0.1122;
				beta = 0.9;
				rho = 0.2;
				nu = 0.4;
				displacement = 0.02;
				underlying = 0.015;
				maturity = 1;
				break;

			case 2:
				alpha = 0.006;
				beta = 0.0001;
				rho = 0.95;
				nu = 0.075;
				displacement = 0.02;
				underlying = 0.0076;
				maturity = 20;
				break;

			case 3:
				alpha = 0.1122;
				beta = 0.2;
				rho = 0.9;
				nu = 0.4;
				displacement = 0.02;
				underlying = 0.015;
				maturity = 10;

				break;
			}

			final double curvature = AnalyticFormulas.sabrNormalVolatilityCurvatureApproximation(alpha, beta, rho, nu, displacement, underlying, maturity);

			/*
			 * Finite difference approximation of the curvature.
			 */
			final double epsilon = 1E-4;
			final double value = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying, maturity);
			final double valueUp = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying+epsilon, maturity);
			final double valueDn = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying-epsilon, maturity);
			final double curvatureNumerical = (valueUp - 2.0*value + valueDn) / epsilon / epsilon;

			System.out.println(curvature);
			System.out.println(curvatureNumerical);

			Assert.assertEquals("Curvature", curvatureNumerical, curvature, 5.0/100/100);
		}
	}

	@Test
	public void testBlackScholesPutCallParityATM() {
		final double initialStockValue = 100.0;
		final Double riskFreeRate = 0.02;
		final Double volatility = 0.20;
		final double optionMaturity = 8.0;
		final double optionStrike = initialStockValue * Math.exp(riskFreeRate * optionMaturity);

		final double valueCall = AnalyticFormulas.blackScholesOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike);
		final double valuePut = AnalyticFormulas.blackScholesOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike, false);

		Assert.assertEquals(valueCall, valuePut, 1E-15);
	}

	@Test
	public void testBlackScholesNegativeForward() {
		final double initialStockValue = 100.0;
		final double riskFreeRate = 0.02;
		final double volatility = 0.20;
		final double optionMaturity = 8.0;
		final double optionStrike = -10;

		final double valueExpected = initialStockValue -optionStrike * Math.exp(- riskFreeRate * optionMaturity);
		final double valueCall = AnalyticFormulas.blackScholesOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		Assert.assertEquals(valueExpected, valueCall, 1E-12);
	}

	/**
	 * This test shows the Bachelier risk neutral probabilities
	 * compared to Black-Scholes risk neutral probabilities.
	 *
	 * The Bachelier model allows for negative values of the underlying.
	 *
	 * The parameters in this test are such that value of the ATM option
	 * is similar in both models.
	 *
	 */
	@Test
	public void testBachelierRiskNeutralProbabilities() {
		final DecimalFormat numberFormatStrike		= new DecimalFormat(" 0.00% ");
		final DecimalFormat numberFormatValue			= new DecimalFormat(" 0.000%");
		final DecimalFormat numberFormatProbability	= new DecimalFormat("  0.00%; -0.00%");

		final Double riskFreeRate = 0.01;
		final Double volatilityN = 0.0065;
		final Double volatilityLN = 0.849;
		final Double optionMaturity = 10.0;

		// We calculate risk neutral probs using a finite difference approx. of Breden-Litzenberger
		final double eps = 1E-8;

		System.out.println("Strike K" + "          \t" +
				"Bachelier Value " + "     \t" +
				"Bachelier P(S<K) " + "    \t" +
				"Black-Scholes Value " + " \t" +
				"Black-Scholes P(S<K) " + "\t");
		for(double optionStrike = 0.02; optionStrike > -0.10; optionStrike -= 0.005) {

			final double payoffUnit	= Math.exp(-riskFreeRate * optionMaturity);
			final double forward		= 0.01;

			final double valuePutBa1 = -(forward-optionStrike)*payoffUnit + AnalyticFormulas.bachelierOptionValue(forward, volatilityN, optionMaturity, optionStrike, payoffUnit);
			final double valuePutBa2 = -(forward-optionStrike-eps)*payoffUnit + AnalyticFormulas.bachelierOptionValue(forward, volatilityN, optionMaturity, optionStrike+eps, payoffUnit);
			final double probabilityBachelier = Math.max((valuePutBa2 - valuePutBa1) / eps / payoffUnit,0);

			final double valuePutBS1 = -(forward-optionStrike)*payoffUnit + AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatilityLN, optionMaturity, optionStrike, payoffUnit);
			final double valuePutBS2 = -(forward-optionStrike-eps)*payoffUnit + AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatilityLN, optionMaturity, optionStrike+eps, payoffUnit);
			final double probabilityBlackScholes = Math.max((valuePutBS2 - valuePutBS1) / eps / payoffUnit,0);

			System.out.println(
					numberFormatStrike.format(optionStrike) + "         \t" +
							numberFormatValue.format(valuePutBa1) + "         \t" +
							numberFormatProbability.format(probabilityBachelier) + "         \t" +
							numberFormatValue.format(valuePutBS1) + "         \t" +
							numberFormatProbability.format(probabilityBlackScholes));

			if(optionStrike > forward) {
				Assert.assertTrue("For strike>forward: Bacherlier probability for high underlying value should be lower than Black Scholes:", probabilityBlackScholes > probabilityBachelier);
			}
			if(optionStrike < -eps) {
				Assert.assertTrue("For strike<0: Bacherlier probability for low underlying value should be higher than Black Scholes:", probabilityBlackScholes < probabilityBachelier);
				Assert.assertTrue("For strike<0: Black Scholes probability for underlying < 0 should be 0:", probabilityBlackScholes < 1E-8);

			}
		}
	}

	@Test
	public void testVolatilityConversionLognormalToNormal() {

		final double forward = 0.02;
		final double displacement = 0.03;
		final double optionMaturity = 5.0;
		final double lognormalVolatility = 0.212;
		final double optionStrike = forward;

		final double normalATM = AnalyticFormulas.volatilityConversionLognormalATMtoNormalATM(
				forward, displacement, optionMaturity, lognormalVolatility);

		final double normal2 = AnalyticFormulas.volatilityConversionLognormalToNormal(
				forward, displacement, optionMaturity, optionStrike, lognormalVolatility);

		Assertions.assertEquals(normalATM, normal2, 1E-10);
	}
}
