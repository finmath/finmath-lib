package net.finmath.finitedifference;

import java.util.Arrays;

import net.finmath.finitedifference.products.FDMEuropeanPutOption;
import org.junit.Assert;
import org.junit.Test;

import net.finmath.finitedifference.models.FDMBlackScholesModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.finitedifference.products.FiniteDifference1DProduct;
import net.finmath.functions.AnalyticFormulas;

public class BlackScholesThetaTest {

	@Test
	public void testEuropeanCallOption() throws AssertionError {
		double riskFreeRate = 0.06;
		double volatility = 0.4;
		double optionMaturity = 1;
		double optionStrike = 50;

		int numTimesteps = 35;
		int numSpacesteps = 120;
		int numStandardDeviations = 5;
		double initialValue = 50;
		double theta = 0.5;

		FiniteDifference1DModel model = new FDMBlackScholesModel(
				numTimesteps,
				numSpacesteps,
				numStandardDeviations,
				optionStrike, // center of the grid.
				theta,
				initialValue,
				riskFreeRate,
				volatility);
		FiniteDifference1DProduct callOption = new FDMEuropeanCallOption(optionMaturity, optionStrike);

		double[][] valueCallFDM = callOption.getValue(0.0, model);
		double[] initialStockPriceForCall = valueCallFDM[0];
		double[] callOptionValue = valueCallFDM[1];
		double[] analyticalCallOptionValue = new double[callOptionValue.length];
		for (int i =0; i < analyticalCallOptionValue.length; i++) {
			analyticalCallOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPriceForCall[i], riskFreeRate,
					volatility, optionMaturity, optionStrike, true);
		}
		System.out.println(Arrays.toString(callOptionValue));
		Assert.assertArrayEquals(callOptionValue, analyticalCallOptionValue, 1e-2);


	}

	@Test
	public void testEuropeanPutOption() throws AssertionError {
		double riskFreeRate = 0.06;
		double volatility = 0.4;
		double optionMaturity = 1;
		double optionStrike = 50;

		int numTimesteps = 35;
		int numSpacesteps = 120;
		int numStandardDeviations = 5;
		double initialValue = 50;
		double theta = 0.5;

		FiniteDifference1DModel model = new FDMBlackScholesModel(
				numTimesteps,
				numSpacesteps,
				numStandardDeviations,
				optionStrike, // center of the grid.
				theta,
				initialValue,
				riskFreeRate,
				volatility);
		FiniteDifference1DProduct putOption = new FDMEuropeanPutOption(optionMaturity, optionStrike);
		double[][] valuePutFDM = putOption.getValue(0.0, model);
		double[] initialStockPriceForPut = valuePutFDM[0];
		double[] putOptionValue = valuePutFDM[1];
		double[] analyticalPutOptionValue = new double[putOptionValue.length];
		for (int i =0; i < analyticalPutOptionValue.length; i++) {
			analyticalPutOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPriceForPut[i], riskFreeRate,
					volatility, optionMaturity, optionStrike, false);
		}
		System.out.println(Arrays.toString(putOptionValue));
		Assert.assertArrayEquals(putOptionValue, analyticalPutOptionValue, 1e-2);
	}


}
