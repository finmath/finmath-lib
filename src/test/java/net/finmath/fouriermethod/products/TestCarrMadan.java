package net.finmath.fouriermethod.products;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.BlackScholesModel;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.fouriermethod.products.EuropeanOptionSmile;
import net.finmath.fouriermethod.products.EuropeanOptionSmileByCarrMadan;
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
		
		double[] strikes = new double[20];
		
		for(int i = 0; i<20;i++)
			strikes[i] = 10 + i*10;

		ProcessCharacteristicFunctionInterface model = new BlackScholesModel(initialValue, riskFreeRate, volatility);
		
		EuropeanOptionSmile mySmile = new EuropeanOptionSmileByCarrMadan(maturity,strikes);

		long startMillis	= System.currentTimeMillis();
		
		Map<Double, Double> results = mySmile.getValue(model);
		
		long endMillis		= System.currentTimeMillis();
		
		double calculationTime = ((endMillis-startMillis)/1000.0);
		
		System.out.println("FFT prices computed in " +calculationTime + " seconds");
		
		for(int i = 0; i<strikes.length; i++) {
			double valueAnalytic	= AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, maturity, strikes[i]);
			double value = results.get(strikes[i]);
			double relativeError			= (value-valueAnalytic)/valueAnalytic;
			System.out.println("FFT Value: " + value + "\tAnalytic Value: " + valueAnalytic + ". \t Relative Error: " + relativeError + ".");
			Assert.assertEquals("Value", valueAnalytic, value, 1E-2);
		}
				
	}
	
}
