package net.finmath.functions;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.functions.BarrierOptions.BarrierType;

/**
 * Unit tests for {@link net.finmath.functions.BarrierOptions}.
 *
 * @author Alessandro Gnoatto
 */
public class BarrierOptionsTest {

	static final DecimalFormat formatterReal2 = new DecimalFormat("#0.0000");

	static final double initialStockValue = 100;
	static final double riskFreeRate = 0.08;
	static final double dividendYield = 0.04;
	static final double volatility = 0.25;
	static final double optionMaturity = 0.5;
	static final double rebate = 3;

	@Test
	public void testDownAndInPut() {
		final boolean isCall = false;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {95, 100};
		final BarrierType barrierType = BarrierType.DOWN_IN;

		double optionValue;

		final double[][] haughValues = {{2.9586, 2.2845},{6.5677, 5.9085},{11.9752, 11.6465}};
		System.out.println("Testing Down and In Puts againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}

	@Test
	public void testDownAndInCall() {
		final boolean isCall = true;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {95, 100};
		final BarrierType barrierType = BarrierType.DOWN_IN;

		double optionValue;

		final double[][] haughValues = {{7.7627,13.8333},{4.0109, 7.8494},{2.0576, 3.9795}};
		System.out.println("Testing Down and In Calls againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}


	@Test
	public void testUpAndInPut() {
		final boolean isCall = false;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {105};
		final BarrierType barrierType = BarrierType.UP_IN;

		double optionValue;

		final double[][] haughValues = {{1.4653},{3.3721},{7.0846}};
		System.out.println("Testing Up and In Puts againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}

	@Test
	public void testUpAndInCall() {
		final boolean isCall = true;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {105};
		final BarrierType barrierType = BarrierType.UP_IN;

		double optionValue;

		final double[][] haughValues = {{14.1112},{8.4482},{4.5910}};
		System.out.println("Testing Up and In Calls againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}

	@Test
	public void testDownAndOutPut() {
		final boolean isCall = false;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {95,100};
		final BarrierType barrierType = BarrierType.DOWN_OUT;

		double optionValue;

		final double[][] haughValues = {{2.2798, 3.0000},{2.2947, 3.0000},{2.6252, 3.0000}};
		System.out.println("Testing Down and Out Puts againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}

	@Test
	public void testDownAndOutCall() {
		final boolean isCall = true;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {95,100};
		final BarrierType barrierType = BarrierType.DOWN_OUT;

		double optionValue;

		final double[][] haughValues = {{9.0246, 3.0000},{6.7924, 3.0000},{4.8759, 3.0000}};
		System.out.println("Testing Down and Out Calls againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}

	@Test
	public void testUpAndOutPut() {
		final boolean isCall = false;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {105};
		final BarrierType barrierType = BarrierType.UP_OUT;

		double optionValue;

		final double[][] haughValues = {{3.7760},{5.4932},{7.5187}};
		System.out.println("Testing Up and Out Puts againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}

	@Test
	public void testUpAndOutCall() {
		final boolean isCall = true;
		final double[] optionStrike = {90, 100, 110};
		final double[] barrierValue = {105};
		final BarrierType barrierType = BarrierType.UP_OUT;

		double optionValue;

		final double[][] haughValues = {{2.6789},{2.3580},{2.3453}};
		System.out.println("Testing Up and Out Calls againts Haugh's values");

		for(int i = 0; i < optionStrike.length; i++) {
			for(int j = 0; j < barrierValue.length; j++) {
				optionValue= BarrierOptions.blackScholesBarrierOptionValue(initialStockValue,
						riskFreeRate, dividendYield, volatility, optionMaturity, optionStrike[i], isCall,
						rebate, barrierValue[j], barrierType);
				System.out.println(formatterReal2.format(optionValue) + "\t" + formatterReal2.format(haughValues[i][j]));
				Assert.assertEquals(optionValue, haughValues[i][j], 1E-3);
			}
		}
	}
}
