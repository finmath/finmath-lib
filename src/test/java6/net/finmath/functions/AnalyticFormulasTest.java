/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 10.05.2016
 */

package net.finmath.functions;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Fries
 *
 */
public class AnalyticFormulasTest {

	@Before
	public void setUp() throws Exception {
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
