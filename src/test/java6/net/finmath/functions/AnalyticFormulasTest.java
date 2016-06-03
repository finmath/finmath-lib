/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 10.05.2016
 */

package net.finmath.functions;

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
	

}
