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

import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;

/**
 * @author Christian Fries
 */
public class AnalyticFormulasTest {

	static final DecimalFormat formatterReal2 = new DecimalFormat("#0.00");
	@Test
	public void testBachelierOptionImpliedVolatility() {
		double spot = 100;
		double riskFreeRate = 0.05;
		for(double volatilityNormal = 5.0 / 100.0 * spot; volatilityNormal < 1.0 * spot; volatilityNormal += 5.0 / 100.0 * spot) {
			for(double optionMaturity = 0.5; optionMaturity < 10; optionMaturity += 0.25) {
				for(double moneynessInStdDev = -6.0; moneynessInStdDev <= 6.0; moneynessInStdDev += 0.5) {

					double moneyness = moneynessInStdDev * volatilityNormal * Math.sqrt(optionMaturity);

					double volatility = volatilityNormal;
					double forward = spot * Math.exp(riskFreeRate * optionMaturity);
					double optionStrike = forward + moneyness;
					double payoffUnit = Math.exp(-riskFreeRate * optionMaturity);

					double optionValue = AnalyticFormulas.bachelierOptionValue(forward, volatility, optionMaturity, optionStrike, payoffUnit);
					double impliedVolatility = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);

					System.out.println(formatterReal2.format(optionMaturity) + " \t" + formatterReal2.format(moneyness) + " \t" + formatterReal2.format(optionValue) + " \t" + formatterReal2.format(volatility) + " \t" + formatterReal2.format(impliedVolatility));
					Assert.assertEquals(volatility, impliedVolatility, 1E-3);
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

		double alpha = 0.006;
		double beta = 0.05;
		double rho = 0.95;
		double nu = 0.075;
		double displacement = 0.02;


		double[] initialParameters = { alpha, beta, rho, nu, displacement };
		double[] targetValues = givenVolatilities;
		int maxIteration = 500;
		int numberOfThreads = 8;

		for(double displacement2 = 0.5; displacement2>0; displacement2 -= 0.001) {
			givenVolatilities[0] = givenVolatilities[0] + 0.00001;
			final double displacement3 = displacement2;
			LevenbergMarquardt lm = new LevenbergMarquardt(initialParameters, targetValues, maxIteration, numberOfThreads) {
				private static final long serialVersionUID = -4799790311777696204L;

				@Override
				public void setValues(double[] parameters, double[] values) {
					for(int strikeIndex = 0; strikeIndex < givenStrikes.length; strikeIndex++) {
						double strike = givenStrikes[strikeIndex];
						values[strikeIndex] = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(parameters[0] /* alpha */, parameters[1] /* beta */, parameters[2] /* rho */, parameters[3] /* nu */, parameters[4] /* displacement */, underlying, strike, maturity);
					}
				}
			};
			lm.setErrorTolerance(1E-16);

			lm.run();

			double[] bestParameters = lm.getBestFitParameters();

			//			System.out.println(lm.getRootMeanSquaredError() + "\t");
			System.out.println(givenVolatilities[0] + "\t" + lm.getRootMeanSquaredError() + "\t" + Arrays.toString(bestParameters));
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
			double riskReversal = AnalyticFormulas.sabrNormalVolatilitySkewApproximation(alpha, beta, rho, nu, displacement, underlying, maturity);

			double epsilon = 1E-4;
			double valueUp = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying+epsilon, maturity);
			double valueDn = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying-epsilon, maturity);
			double riskReversalNumerical = (valueUp-valueDn) / 2 / epsilon;

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

			double curvature = AnalyticFormulas.sabrNormalVolatilityCurvatureApproximation(alpha, beta, rho, nu, displacement, underlying, maturity);

			/*
			 * Finite difference approximation of the curvature.
			 */
			double epsilon = 1E-4;
			double value = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying, maturity);
			double valueUp = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying+epsilon, maturity);
			double valueDn = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(alpha, beta, rho, nu, displacement, underlying, underlying-epsilon, maturity);
			double curvatureNumerical = (valueUp - 2.0*value + valueDn) / epsilon / epsilon;

			System.out.println(curvature);
			System.out.println(curvatureNumerical);

			Assert.assertEquals("Curvature", curvatureNumerical, curvature, 5.0/100/100);
		}
	}

	@Test
	public void testBlackScholesPutCallParityATM() {
		double initialStockValue = 100.0;
		Double riskFreeRate = 0.02;
		Double volatility = 0.20;
		double optionMaturity = 8.0;
		double optionStrike = initialStockValue * Math.exp(riskFreeRate * optionMaturity);

		double valueCall = AnalyticFormulas.blackScholesOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike);
		double valuePut = AnalyticFormulas.blackScholesOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike, false);

		Assert.assertEquals(valueCall, valuePut, 1E-15);
	}

	@Test
	public void testBlackScholesNegativeForward() {
		double initialStockValue = 100.0;
		double riskFreeRate = 0.02;
		double volatility = 0.20;
		double optionMaturity = 8.0;
		double optionStrike = -10;

		double valueExpected = initialStockValue -optionStrike * Math.exp(- riskFreeRate * optionMaturity);
		double valueCall = AnalyticFormulas.blackScholesOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike);

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
		DecimalFormat numberFormatStrike		= new DecimalFormat(" 0.00% ");
		DecimalFormat numberFormatValue			= new DecimalFormat(" 0.000%");
		DecimalFormat numberFormatProbability	= new DecimalFormat("  0.00%; -0.00%");

		Double riskFreeRate = 0.01;
		Double volatilityN = 0.0065;
		Double volatilityLN = 0.849;
		Double optionMaturity = 10.0;

		// We calculate risk neutral probs using a finite difference approx. of Breden-Litzenberger
		double eps = 1E-8;

		System.out.println("Strike K" + "          \t" +
				"Bachelier Value " + "     \t" +
				"Bachelier P(S<K) " + "    \t" +
				"Black-Scholes Value " + " \t" +
				"Black-Scholes P(S<K) " + "\t");
		for(double optionStrike = 0.02; optionStrike > -0.10; optionStrike -= 0.005) {

			double payoffUnit	= Math.exp(-riskFreeRate * optionMaturity);
			double forward		= 0.01;

			double valuePutBa1 = -(forward-optionStrike)*payoffUnit + AnalyticFormulas.bachelierOptionValue(forward, volatilityN, optionMaturity, optionStrike, payoffUnit);
			double valuePutBa2 = -(forward-optionStrike-eps)*payoffUnit + AnalyticFormulas.bachelierOptionValue(forward, volatilityN, optionMaturity, optionStrike+eps, payoffUnit);
			double probabilityBachelier = Math.max((valuePutBa2 - valuePutBa1) / eps / payoffUnit,0);

			double valuePutBS1 = -(forward-optionStrike)*payoffUnit + AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatilityLN, optionMaturity, optionStrike, payoffUnit);
			double valuePutBS2 = -(forward-optionStrike-eps)*payoffUnit + AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatilityLN, optionMaturity, optionStrike+eps, payoffUnit);
			double probabilityBlackScholes = Math.max((valuePutBS2 - valuePutBS1) / eps / payoffUnit,0);

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

}
