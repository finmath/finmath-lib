/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.text.DecimalFormat;

import org.junit.Test;

import net.finmath.exception.CalculationException;
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
public class MertonModelTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	private final double theta = volatility*volatility;
	private final double kappa = 1.0;
	private final double xi = 0.3;
	private final double rho = 0.0;

	private final Scheme scheme = Scheme.FULL_TRUNCATION;

	// Process discretization properties
	private final int		numberOfPaths		= 100000;
	private final int		numberOfTimeSteps	= 100;
	private final double	deltaT				= 0.02;

	private final int		seed				= 3141;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 2.0;
	private final double	optionStrike = 1.10;

	@Test
	public void test() throws CalculationException {

		long start = System.currentTimeMillis();

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


		AssetModelMonteCarloSimulationInterface monteCarloMertonModel;
		{
			double m = 1.0;
			double nu = 0.15;

			double lambda = 0.4;
			double jumpSizeStdDev = nu;
			double jumpSizeMean = Math.log(m);

			monteCarloMertonModel = new MonteCarloMertonModel(
					timeDiscretization, numberOfPaths, seed, initialValue, riskFreeRate, volatility,
					lambda, jumpSizeMean, jumpSizeStdDev);

		}

		/*
		 * Value a call option (using the product implementation)
		 */
		for(double moneyness = 0.0; moneyness <= 2.0; moneyness += 0.1) {
			EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike * moneyness);
			double value = europeanOption.getValue(monteCarloBlackScholesModel);
			double value2 = europeanOption.getValue(monteCarloHestonModel);
			double value3 = europeanOption.getValue(monteCarloMertonModel);
			double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			double impliedVol1 = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), value);
			double impliedVol2 = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), value2);
			double impliedVol3 = AnalyticFormulas.blackScholesOptionImpliedVolatility(initialValue*Math.exp(riskFreeRate*optionMaturity), optionMaturity, optionStrike*moneyness, Math.exp(-riskFreeRate*optionMaturity), value3);

			DecimalFormat formatter2 = new DecimalFormat("0.00");
			DecimalFormat formatter4 = new DecimalFormat("0.00%");
			System.out.print(formatter2.format(optionStrike * moneyness) + "\t" + formatter4.format(impliedVol1));
			//			System.out.println("\t " + value2 + "\t " + impliedVol2);
			System.out.println("\t " + formatter4.format(value3) + "\t " + formatter4.format(impliedVol3));
		}

		long end = System.currentTimeMillis();

		System.out.print("Test took " + (end-start) / 1000.0 + " sec.");

	}
}
