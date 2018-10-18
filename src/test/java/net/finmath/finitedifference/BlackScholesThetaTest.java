package net.finmath.finitedifference;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.finitedifference.models.FDMBlackScholesModel;
import net.finmath.finitedifference.models.FiniteDifference1DModel;
import net.finmath.finitedifference.products.FDMEuropeanCallOption;
import net.finmath.finitedifference.products.FiniteDifference1DProduct;
import net.finmath.functions.AnalyticFormulas;

public class BlackScholesThetaTest {

	@Test
	public void test() throws AssertionError {
		double riskFreeRate = 0.06;
		double volatility = 0.4;
		double optionMaturity = 1;
		double optionStrike = 50;

		int numTimesteps = 35;
		int numSpacesteps = 120;
//		int numTimesteps = 3;
//		int numSpacesteps = 3;
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
		double[][] valueFDM = callOption.getValue(0.0, model);
		double[] initialStockPrice = valueFDM[0];
		double[] optionValue = valueFDM[1];
		double[] analyticalOptionValue = new double[optionValue.length];
		for (int i =0; i < analyticalOptionValue.length; i++) {
			analyticalOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPrice[i], riskFreeRate,
					volatility, optionMaturity, optionStrike, true);
		}
		System.out.println(Arrays.toString(optionValue));
		Assert.assertArrayEquals(optionValue, analyticalOptionValue, 1e-2);

	}

}
