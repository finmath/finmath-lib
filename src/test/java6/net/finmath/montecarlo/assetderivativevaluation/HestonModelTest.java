/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 28.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.junit.internal.runners.ErrorReportingRunner;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.fouriermethod.products.AbstractProductFourierTransform;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.BrownianMotionView;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 *
 */
public class HestonModelTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	private final double theta = volatility*volatility;
	private final double kappa = 0.2;
	private final double xi = 0.75;
	private final double rho = -0.2;

	private final Scheme scheme = Scheme.FULL_TRUNCATION;

	// Process discretization properties
	private final int		numberOfPaths		= 100000;
	private final int		numberOfTimeSteps	= 100;
	private final double	deltaT				= 0.02;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 2.0;
	private final double	optionStrike = 1.10;

	private static DecimalFormat formatReal3 = new DecimalFormat("####0.000", new DecimalFormatSymbols(Locale.ENGLISH));
	
	@Test
	public void test() throws CalculationException {

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		BrownianMotionInterface brownianMotion = new BrownianMotion(timeDiscretization, 2 /* numberOfFactors */, numberOfPaths, seed);

		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel;
		{
			// Create a model
			AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

			// Create a corresponding MC process
			AbstractProcess process = new ProcessEulerScheme(new BrownianMotionView(brownianMotion, new Integer[] { new Integer(0) }));

			// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
			monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);
		}

		AssetModelMonteCarloSimulationInterface monteCarloHestonModel;
		{
			// Create a model
			AbstractModel model = new HestonModel(initialValue, riskFreeRate, volatility, theta, kappa, xi, rho, scheme);

			// Create a corresponding MC process
			AbstractProcess process = new ProcessEulerScheme(brownianMotion);

			// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
			monteCarloHestonModel = new MonteCarloAssetModel(model, process);
		}

        ProcessCharacteristicFunctionInterface characteristFunctionHeston = new net.finmath.fouriermethod.models.HestonModel(initialValue, riskFreeRate, volatility, theta, kappa, xi, rho);
		
		System.out.println("Implied volatilties using:\n"
				+ "  (mc/bs) = Monte-Carlo/Black-Scholes\n"
				+ "  (mc/hs) = Monte-Carlo/Heston\n"
				+ "  (ft/hs) = Fourtier-transform/Heston\n");

		/*
		 * Value a call option (using the product implementation)
		 */
		System.out.println("strike  " + "\t" + "vol(mc/bs)" + "\t" + "vol(mc/hs)" + "\t" + "vol(ft/hs)");
		for(double moneyness = 0.8; moneyness <= 1.5; moneyness += 0.1) {
			
			/*
			 * Valuation using Monte-Carlo models
			 */
			EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike * moneyness);

			// Black Scholes
			double valueBlackScholesMonteCarlo = europeanOption.getValue(monteCarloBlackScholesModel);
			double impliedVolBlackScholesMonteCarlo = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), valueBlackScholesMonteCarlo);
			double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			// Heston
			double valueHestonMonteCarlo = europeanOption.getValue(monteCarloHestonModel);
			double impliedVolHestonMonteCarlo = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), valueHestonMonteCarlo);

			/*
			 * Valuation using Fourier transform models
			 */
	        AbstractProductFourierTransform europeanFourier = new net.finmath.fouriermethod.products.EuropeanOption(optionMaturity, optionStrike * moneyness);
	        double valueHestonFourier = europeanFourier.getValue(characteristFunctionHeston);
			double impliedVolHestonFourier = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), valueHestonFourier);
						
			System.out.println(formatReal3.format(optionStrike * moneyness) + "    \t" + formatReal3.format(impliedVolBlackScholesMonteCarlo) +
			"    \t" + formatReal3.format(impliedVolHestonMonteCarlo) + "    \t" + formatReal3.format(impliedVolHestonFourier));
			
			Assert.assertEquals(impliedVolHestonFourier, impliedVolHestonMonteCarlo, 5E-3);
		}
	}
}
