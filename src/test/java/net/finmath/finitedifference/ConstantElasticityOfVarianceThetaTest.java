package net.finmath.finitedifference;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.finitedifference.models.FDMConstantElasticityOfVarianceModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.finitedifference.products.FDMEuropeanPutOption;
import net.finmath.finitedifference.products.FiniteDifference1DProduct;
import net.finmath.functions.AnalyticFormulas;

public class ConstantElasticityOfVarianceThetaTest {

	@Test
	public void testEuropeanCallOption() throws AssertionError {
		final double riskFreeRate = 0.06;
		final double volatility = 0.4;
		final double exponent = 0.9;
		final double optionMaturity = 1;
		final double optionStrike = 100;

		final int numTimesteps = 80;
		final int numSpacesteps = 160;
		final int numStandardDeviations = 5;
		final double initialValue = 100;
		final double theta = 0.5;

		final FiniteDifference1DModel model = new FDMConstantElasticityOfVarianceModel(
				numTimesteps,
				numSpacesteps,
				numStandardDeviations,
				optionStrike, // center of the grid.
				theta,
				initialValue,
				riskFreeRate,
				volatility,
				exponent);

		final FiniteDifference1DProduct callOption = new FDMEuropeanCallOption(optionMaturity, optionStrike);
		final double[][] valueFDM = callOption.getValue(0.0, model);
		final double[] initialStockPrice = valueFDM[0];
		final double[] optionValue = valueFDM[1];
		final double[] analyticalOptionValue = new double[optionValue.length];
		for (int i =0; i < analyticalOptionValue.length; i++) {
			analyticalOptionValue[i] = AnalyticFormulas.constantElasticityOfVarianceOptionValue(initialStockPrice[i], riskFreeRate,
					volatility, exponent, optionMaturity, optionStrike, true);
		}
		System.out.println(Arrays.toString(optionValue));
		Assert.assertArrayEquals(analyticalOptionValue, optionValue, 5e-3);
	}

	@Test
	public void testEuropeanPutOption() throws AssertionError {
		final double riskFreeRate = 0.06;
		final double volatility = 0.4;
		final double exponent = 0.9;
		final double optionMaturity = 1;
		final double optionStrike = 100;

		final int numTimesteps = 80;
		final int numSpacesteps = 160;
		final int numStandardDeviations = 5;
		final double initialValue = 100;
		final double theta = 0.5;

		final FiniteDifference1DModel model = new FDMConstantElasticityOfVarianceModel(
				numTimesteps,
				numSpacesteps,
				numStandardDeviations,
				optionStrike, // center of the grid.
				theta,
				initialValue,
				riskFreeRate,
				volatility,
				exponent);

		final FiniteDifference1DProduct putOption = new FDMEuropeanPutOption(optionMaturity, optionStrike);
		final double[][] valueFDM = putOption.getValue(0.0, model);
		final double[] initialStockPrice = valueFDM[0];
		final double[] optionValue = valueFDM[1];
		final double[] analyticalOptionValue = new double[optionValue.length];
		for (int i =0; i < analyticalOptionValue.length; i++) {
			analyticalOptionValue[i] = AnalyticFormulas.constantElasticityOfVarianceOptionValue(initialStockPrice[i], riskFreeRate,
					volatility, exponent, optionMaturity, optionStrike, false);
		}
		System.out.println(Arrays.toString(optionValue));
		Assert.assertArrayEquals(analyticalOptionValue, optionValue, 5e-3);
	}
}
