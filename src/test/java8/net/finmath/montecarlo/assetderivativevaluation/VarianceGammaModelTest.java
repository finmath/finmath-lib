package net.finmath.montecarlo.assetderivativevaluation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.products.FourierTransformProduct;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Alessandro Gnoatto
 */
public class VarianceGammaModelTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;

	private final double sigma = 0.25;
	private final double nu = 0.1;
	private final double theta = 0.4;

	// Process discretization properties
	private final int		numberOfPaths		= 10000;
	private final int		numberOfTimeSteps	= 100;
	private final double	deltaT				= 0.05;

	private final int		seed				= 31415;

	private final double	optionMaturity = 2.0;
	private final double	optionStrike = 1.10;

	private static DecimalFormat formatReal3 = new DecimalFormat("####0.000", new DecimalFormatSymbols(Locale.ENGLISH));

	@Test
	public void test() throws CalculationException {

		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		AssetModelMonteCarloSimulationModel monteCarloVarianceGammaModel;
		{
			monteCarloVarianceGammaModel = new MonteCarloVarianceGammaModel(timeDiscretization, numberOfPaths, seed,
					initialValue, riskFreeRate, sigma, theta, nu);
		}

		final CharacteristicFunctionModel characteristFunctionVarianceGamma = new net.finmath.fouriermethod.models.VarianceGammaModel(initialValue,
				riskFreeRate, riskFreeRate, sigma, theta, nu);

		/*
		 * Value a call option (using the product implementation)
		 */
		System.out.println("strike  " + "\t" + "price MC" + "\t" + "price FFT");
		for(double moneyness = 0.8; moneyness <= 1.5; moneyness += 0.1) {
			/*
			 * Valuation using Monte-Carlo models
			 */
			final EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike * moneyness);

			// Heston
			final double valueVarianceGammaMonteCarlo = europeanOption.getValue(monteCarloVarianceGammaModel);

			/*
			 * Valuation using Fourier transform models
			 */
			final FourierTransformProduct europeanFourier = new net.finmath.fouriermethod.products.EuropeanOption(optionMaturity, optionStrike * moneyness);
			final double valueVarianceGammaFourier = europeanFourier.getValue(characteristFunctionVarianceGamma);


			System.out.println(formatReal3.format(optionStrike * moneyness) + "    \t" + formatReal3.format(valueVarianceGammaMonteCarlo) +
					"    \t" + formatReal3.format(valueVarianceGammaFourier));
			Assert.assertEquals(valueVarianceGammaMonteCarlo, valueVarianceGammaFourier, 1E-2);
		}

	}

}
