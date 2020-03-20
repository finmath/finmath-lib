package net.finmath.fouriermethod.products;

import java.util.Map;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.BlackScholesModel;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmile;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmileByCarrMadan;
import net.finmath.functions.AnalyticFormulas;

/**
 * This class tests the Carr Madan formula against the Black-Scholes closed-form solution.
 *
 * @author Alessandro Gnoatto
 *
 */
public class TestCarrMadan {


	private static final double initialValue	= 100.0;
	private static final double volatility		= 0.25;
	private static final double riskFreeRate	= 0.05;

	private static final double maturity	= 3.0;

	@Test
	public void test() throws CalculationException {

		final double[] strikes = new double[20];

		for(int i = 0; i<20;i++) {
			strikes[i] = 10 + i*10;
		}

		final CharacteristicFunctionModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		final EuropeanOptionSmile mySmile = new EuropeanOptionSmileByCarrMadan(maturity,strikes);

		final long startMillis	= System.currentTimeMillis();

		final Map<String, Function<Double, Double>> results = mySmile.getValue(0.0,model);

		final long endMillis		= System.currentTimeMillis();

		final double calculationTime = ((endMillis-startMillis)/1000.0);

		System.out.println("FFT prices computed in " +calculationTime + " seconds");

		for(int i = 0; i<strikes.length; i++) {
			final double valueAnalytic	= AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, maturity, strikes[i]);
			final double value = results.get("valuePerStrike").apply(strikes[i]);
			final double relativeError			= (value-valueAnalytic)/valueAnalytic;
			System.out.println("FFT Value: " + value + "\tAnalytic Value: " + valueAnalytic + ". \t Relative Error: " + relativeError + ".");
			Assert.assertEquals("Value", valueAnalytic, value, 1E-2);
		}

	}

}
