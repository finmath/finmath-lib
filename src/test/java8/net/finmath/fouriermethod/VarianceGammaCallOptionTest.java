package net.finmath.fouriermethod;

import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.complex.Complex;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.models.VarianceGammaModel;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.fouriermethod.products.FourierTransformProduct;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmile;
import net.finmath.fouriermethod.products.smile.EuropeanOptionSmileByCarrMadan;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloVarianceGammaModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

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

	//Monte Carlo discretization
	private final int		numberOfPaths		= 10000;
	private final int		numberOfTimeSteps	= 100;
	private final double	deltaT				= 0.02;

	private final int		seed				= 3141;

	@Test
	public void testMartingaleProperty() throws CalculationException{
		//Characteristic function for Fourier pricing
		final CharacteristicFunctionModel model = new VarianceGammaModel(initialValue,riskFreeRate, riskFreeRate, sigma,theta,nu);

		final Complex minusI = new Complex(0.0,-1.0);
		System.out.println("Testing the martingale property of the characteristic function over multiple time horizons.");

		for(int i = 0; i<10; i++) {
			final double time = 0.5 * i;
			System.out.println(model.apply(time).apply(minusI));
		}
	}

	@Test
	public void testMartingalePropertyMonteCarlo() throws CalculationException{
		//Time discretization for Monte Carlo
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		final AssetModelMonteCarloSimulationModel monteCarloVarianceGammaModel = new MonteCarloVarianceGammaModel(
				timeDiscretization, numberOfPaths, seed, initialValue, riskFreeRate, sigma, theta, nu);

		System.out.println("Testing the martingale property of the Monte Carlo discretization over multiple time horizons.");

		for(int i = 0; i<10; i++) {
			final double time = 0.2 * i;
			System.out.println(monteCarloVarianceGammaModel.getAssetValue(time, 0).div(monteCarloVarianceGammaModel.getNumeraire(time)).getAverage());
		}
	}
	@Test
	public void test() throws CalculationException {
		//Characteristic function for Fourier pricing
		final CharacteristicFunctionModel model = new VarianceGammaModel(initialValue,riskFreeRate, riskFreeRate, sigma,theta,nu);

		//Time discretization for Monte Carlo
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		final AssetModelMonteCarloSimulationModel monteCarloVarianceGammaModel = new MonteCarloVarianceGammaModel(
				timeDiscretization, numberOfPaths, seed, initialValue, riskFreeRate, sigma, theta, nu);

		/*
		 * FFT inversion of the whole smile at once.
		 */
		final EuropeanOptionSmile mySmile = new EuropeanOptionSmileByCarrMadan(maturity,strikes);

		final Map<String, Function<Double, Double>> fftPrices = mySmile.getValue(0.0,model);

		System.out.println("Comparison of standard Fourier pricer, FFT pricer and Monte Carlo.");
		System.out.println("Strike" + "\t" + "Fourier Price" + "\t" + "FFT Price"+ "\t" + "External Validation Price");
		for(int i = 0; i<strikes.length; i++) {
			/*
			 * Fourier transform of each call option separately.
			 * In contrasto to the FFT this operation is O(M^2)
			 */
			final FourierTransformProduct product = new EuropeanOption(maturity, strikes[i]);

			//Monte Carlo Product
			final net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption mcProduct = new net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption(maturity,strikes[i]);

			final double value			= product.getValue(model);
			final double mcValue          = mcProduct.getValue(monteCarloVarianceGammaModel);
			final double fftPrice			= fftPrices.get("valuePerStrike").apply(strikes[i]);
			System.out.println(strikes[i] + "\t" + value + "\t" + fftPrice + "\t" + mcValue);
		}
	}

}
