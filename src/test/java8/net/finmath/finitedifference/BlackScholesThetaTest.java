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
		final double riskFreeRate = 0.06;
		final double volatility = 0.4;
		final double optionMaturity = 1;
		final double optionStrike = 50;

		/*
		BlackScholesTheta blackScholesFD = new BlackScholesTheta();
		double[][] stockAndOptionPrice = blackScholesFD.solve();
		double[] initialStockPrice = stockAndOptionPrice[0];
		double[] optionValue = stockAndOptionPrice[1];

		double[] analyticalOptionValue = new double[stockAndOptionPrice[0].length];
		for (int i =0; i < analyticalOptionValue.length; i++) {
			analyticalOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPrice[i], riskFreeRate,
					volatility, optionMaturity, optionStrike, true);
		}

		Assert.assertArrayEquals(optionValue, analyticalOptionValue, 2e-2);
		 */

		// First refactoring attempt
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
		final double[][] valueFDM = callOption.getValue(0.0, model);
		final double[] initialStockPrice = valueFDM[0];
		final double[] optionValue = valueFDM[1];
		final double[] analyticalOptionValue = new double[optionValue.length];
		for (int i =0; i < analyticalOptionValue.length; i++) {
			analyticalOptionValue[i] = AnalyticFormulas.blackScholesOptionValue(initialStockPrice[i], riskFreeRate,
					volatility, optionMaturity, optionStrike, true);
		}

		Assert.assertArrayEquals(optionValue, analyticalOptionValue, 1.2e-2);
		System.out.println(Arrays.toString(optionValue));
	}
}
