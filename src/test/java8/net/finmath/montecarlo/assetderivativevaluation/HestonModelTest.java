/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.products.FourierTransformProduct;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.BrownianMotionView;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel.Scheme;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class HestonModelTest {

	/**
	 * The parameters for this test, that is an array consisting of
	 * { xi }.
	 *
	 * @return Array of parameters.
	 */
	@Parameters(name="xi={0}}")
	public static Collection<Object[]> generateData()
	{
		final ArrayList<Object[]> parameters = new ArrayList<>();
		parameters.add(new Object[] { 0.0 });
		parameters.add(new Object[] { 0.5 });
		return parameters;
	}

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	private final double theta = volatility*volatility;
	private final double kappa = 0.1;
	private final double xi;		// Will be set by parameterized test
	private final double rho = 0.1;

	private final Scheme scheme = Scheme.FULL_TRUNCATION;

	// Process discretization properties
	private final int		numberOfPaths		= 100000;
	private final int		numberOfTimeSteps	= 100;
	private final double	deltaT				= 0.05;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 5.0;
	private final double	optionStrike = 1.10;

	private static DecimalFormat formatReal3 = new DecimalFormat("####0.000", new DecimalFormatSymbols(Locale.ENGLISH));

	public HestonModelTest(final double xi) {
		super();
		this.xi = xi;
	}

	@Test
	public void test() throws CalculationException {

		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 2 /* numberOfFactors */, numberOfPaths, seed);

		final AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel = getMonteCarloSimulationFromModel(
				new BlackScholesModel(initialValue, riskFreeRate, volatility),
				new BrownianMotionView(brownianMotion, new Integer[] { 0 }));

		final AssetModelMonteCarloSimulationModel monteCarloHestonModel = getMonteCarloSimulationFromModel(
				new HestonModel(initialValue, riskFreeRate, volatility, theta, kappa, xi, rho, scheme),
				brownianMotion);

		final CharacteristicFunctionModel characteristFunctionHeston = new net.finmath.fouriermethod.models.HestonModel(initialValue, riskFreeRate, volatility, theta, kappa, xi, rho);

		System.out.println("Implied volatilties using:\n"
				+ "  (mc/bs) = Monte-Carlo/Black-Scholes\n"
				+ "  (mc/hs) = Monte-Carlo/Heston\n"
				+ "  (ft/hs) = Fourier-transform/Heston\n");

		/*
		 * Value a call option (using the product implementation)
		 */
		System.out.println("strike  " + "\t" + "vol(mc/bs)" + "\t" + "vol(mc/hs)" + "\t" + "vol(ft/hs)");
		for(double moneyness = 0.8; moneyness <= 1.5; moneyness += 0.1) {

			/*
			 * Valuation using Monte-Carlo models
			 */
			final EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike * moneyness);

			// Black Scholes
			final double valueBlackScholesMonteCarlo = europeanOption.getValue(monteCarloBlackScholesModel);
			final double impliedVolBlackScholesMonteCarlo = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), valueBlackScholesMonteCarlo);
			final double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			// Heston
			final double valueHestonMonteCarlo = europeanOption.getValue(monteCarloHestonModel);
			final double impliedVolHestonMonteCarlo = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), valueHestonMonteCarlo);

			/*
			 * Valuation using Fourier transform models
			 */
			final FourierTransformProduct europeanFourier = new net.finmath.fouriermethod.products.EuropeanOption(optionMaturity, optionStrike * moneyness);
			final double valueHestonFourier = europeanFourier.getValue(characteristFunctionHeston);
			final double impliedVolHestonFourier = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), valueHestonFourier);

			System.out.println(formatReal3.format(optionStrike * moneyness) + "    \t" + formatReal3.format(impliedVolBlackScholesMonteCarlo) +
					"    \t" + formatReal3.format(impliedVolHestonMonteCarlo) + "    \t" + formatReal3.format(impliedVolHestonFourier));

			if(xi == 0) {
				Assert.assertEquals(impliedVolBlackScholesMonteCarlo, impliedVolHestonMonteCarlo, 1E-10);
			}
			else {
				Assert.assertEquals(impliedVolHestonFourier, impliedVolHestonMonteCarlo, 5E-3);
			}
		}
		System.out.println();
	}

	private AssetModelMonteCarloSimulationModel getMonteCarloSimulationFromModel(ProcessModel model, BrownianMotion brownianMotion) {
		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);

		// Using the process (Euler scheme), create an MC simulation model
		return new MonteCarloAssetModel(process);
	}
}
