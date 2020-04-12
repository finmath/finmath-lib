package net.finmath.finitedifference;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.finitedifference.models.FDMBlackScholesModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.finitedifference.products.FDMEuropeanPutOption;
import net.finmath.finitedifference.products.FiniteDifference1DProduct;
import net.finmath.functions.AnalyticFormulas;

public class BlackScholesThetaTest {

	@Test
	public void testEuropeanCallOption() throws AssertionError {
		final double riskFreeRate = 0.06;
		final double volatility = 0.4;
		final double optionMaturity = 1;
		final double optionStrike = 50;

		final int numTimesteps = 35;
		final int numSpacesteps = 120;
		final int numStandardDeviations = 5;
		final double initialValue = 50;
		final double theta = 0.5;

		final FiniteDifference1DModel model = new FDMBlackScholesModel(
				numTimesteps,
				numSpacesteps,
				numStandardDeviations,
				optionStrike, // center of the grid.
				theta,
				initialValue,
				riskFreeRate,
				volatility);
		final FiniteDifference1DProduct callOption = new FDMEuropeanCallOption(optionMaturity, optionStrike);

		final double[][] valueCallFDM = callOption.getValue(0.0, model);
		final double[] initialStockPriceForCall = valueCallFDM[0];
		final double[] callOptionValue = valueCallFDM[1];
		final double[] analyticalCallOptionValue = new double[callOptionValue.length];
		for (int i =0; i < analyticalCallOptionValue.length; i++) {
			analyticalCallOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPriceForCall[i], riskFreeRate,
					volatility, optionMaturity, optionStrike, true);
		}
		System.out.println(Arrays.toString(callOptionValue));
		Assert.assertArrayEquals(callOptionValue, analyticalCallOptionValue, 1e-2);


	}

	@Test
	public void testEuropeanPutOption() throws AssertionError {
		final double riskFreeRate = 0.06;
		final double volatility = 0.4;
		final double optionMaturity = 1;
		final double optionStrike = 50;

		final int numTimesteps = 35;
		final int numSpacesteps = 120;
		final int numStandardDeviations = 5;
		final double initialValue = 50;
		final double theta = 0.5;

		final FiniteDifference1DModel model = new FDMBlackScholesModel(
				numTimesteps,
				numSpacesteps,
				numStandardDeviations,
				optionStrike, // center of the grid.
				theta,
				initialValue,
				riskFreeRate,
				volatility);
		final FiniteDifference1DProduct putOption = new FDMEuropeanPutOption(optionMaturity, optionStrike);
		final double[][] valuePutFDM = putOption.getValue(0.0, model);
		final double[] initialStockPriceForPut = valuePutFDM[0];
		final double[] putOptionValue = valuePutFDM[1];
		final double[] analyticalPutOptionValue = new double[putOptionValue.length];
		for (int i =0; i < analyticalPutOptionValue.length; i++) {
			analyticalPutOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPriceForPut[i], riskFreeRate,
					volatility, optionMaturity, optionStrike, false);
		}
		System.out.println(Arrays.toString(putOptionValue));
		Assert.assertArrayEquals(putOptionValue, analyticalPutOptionValue, 1e-2);
	}


}
