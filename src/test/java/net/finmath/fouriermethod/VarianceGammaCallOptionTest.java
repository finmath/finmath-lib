package net.finmath.fouriermethod;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.complex.Complex;
import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.models.MertonModel;
import net.finmath.fouriermethod.models.VarianceGammaModel;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.fouriermethod.products.FourierTransformProduct;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmile;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmileByCarrMadan;

/**
 * Unit test for the Variance Gamma model.
 *
 * We test the standard Fourier pricer, the Carr Madan Fast Fourier Transform.
 *
 * @author Alessandro Gnoatto
 *
 */
public class VarianceGammaCallOptionTest {
	private static final double initialValue	= 100.0;
	private static final double riskFreeRate	= 0.04;

	private static final double sigma = 0.25;
	private static double theta = -0.3;
	private static double nu = 0.05;

	//Product properties
	private static final double maturity	= 1.0;
	private static final double[] strikes		= {10,20,30,40,50,60,70,80,90,100,110,120,130,140,150,160,170,180,190,200};
	//Prices computes externally for validation purposes.
	private static final double[] validationPrices = {90.3921,
			   80.7842,
			   71.1764,
			   61.5706,
			   51.9874,
			   42.5200,
			   33.4079,
			   25.0388,
			   17.8294,
			   12.0574,
			    7.7664,
			    4.7896,
			    2.8468,
			    1.6422,
			    0.9256,
			    0.5130,
			    0.2810,
			    0.1529,
			    0.0829,
			    0.0449};

	@Test
	public void testMartingaleProperty() throws CalculationException{
		//Characteristic function for Fourier pricing
		CharacteristicFunctionModel model = new VarianceGammaModel(initialValue,riskFreeRate, riskFreeRate, sigma,theta,nu);

		Complex minusI = new Complex(0.0,-1.0);
		System.out.println("Testing the martingale property of the characteristic function over multiple time horizons.");

		for(int i = 0; i<10; i++) {
			double time = 0.5 * i;
			System.out.println(model.apply(time).apply(minusI));
		}
	}

	@Test
	public void test() throws CalculationException {
		//Characteristic function for Fourier pricing
		CharacteristicFunctionModel model = new VarianceGammaModel(initialValue,riskFreeRate, riskFreeRate, sigma,theta,nu);

		/*
		 * FFT inversion of the whole smile at once.
		 */
		EuropeanOptionSmile mySmile = new EuropeanOptionSmileByCarrMadan(maturity,strikes);

		Map<String, Function<Double, Double>> fftPrices = mySmile.getValue(0.0,model);

		System.out.println("Comparison of standard Fourier pricer, FFT pricer and Monte Carlo.");
		System.out.println("Strike" + "\t" + "Fourier Price" + "\t" + "FFT Price"+ "\t" + "External Validation Price");
		for(int i = 0; i<strikes.length; i++) {
			/*
			 * Fourier transform of each call option separately.
			 * In contrasto to the FFT this operation is O(M^2)
			 */
			FourierTransformProduct product = new EuropeanOption(maturity, strikes[i]);

			double value			= product.getValue(model);
			double fftPrice			= fftPrices.get("valuePerStrike").apply(strikes[i]);
			System.out.println(strikes[i] + "\t" + value + "\t" + fftPrice + "\t" + validationPrices[i]);
			Assert.assertEquals("Value", validationPrices[i], fftPrice, 1E-4);
		}
	}
	
}
