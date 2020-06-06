package net.finmath.montecarlo.assetderivativevaluation.products;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

public class EuropeanOptionVegaPathwiseTest {

	// Process discretization properties
	private final int		numberOfPaths		= 1000000;
	private final int		numberOfTimeSteps	= 10;
	private final double	timeHorizon 		= 5;

	public static void main(final String[] args) throws CalculationException {
		(new EuropeanOptionVegaPathwiseTest()).test();
	}

	@Test
	public void test() throws CalculationException {

		/*
		 * The following just checks the valuation
		 */

		final double	initialValue   = 1.0;
		final double	riskFreeRate   = 0.05;
		final double	volatility     = 0.30;

		final AssetModelMonteCarloSimulationModel model = getModel(initialValue, riskFreeRate, volatility);

		final double	optionMaturity	= 5.0;
		final double	optionStrike	= 1.20;

		/*
		 * Product valuation
		 */
		final EuropeanOption product = new EuropeanOption(optionMaturity, optionStrike);
		final double valueMonteCarlo = product.getValue(model);

		final double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		/*
		 * Product vega
		 */
		final EuropeanOptionVegaPathwise productVega = new EuropeanOptionVegaPathwise(optionMaturity, optionStrike);
		final double vegaMonteCarlo = productVega.getValue(model);

		final double vegaAnalytic = AnalyticFormulas.blackScholesOptionVega(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		System.out.println("value monte-carlo..: " + valueMonteCarlo);
		System.out.println("value analytic.....: " + valueAnalytic);
		System.out.println("vega monte-carlo...: " + vegaMonteCarlo);
		System.out.println("vega analytic......: " + vegaAnalytic);

		Assert.assertEquals("value", valueAnalytic, valueMonteCarlo, 1E-2);
		Assert.assertEquals("vega", vegaAnalytic, vegaMonteCarlo, 1E-2);
	}

	private AssetModelMonteCarloSimulationModel getModel(final double initialValue, final double riskFreeRate, final double volatility)
	{
		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, numberOfTimeSteps, timeHorizon/numberOfTimeSteps);

		// Create an instance of a black scholes monte carlo model
		final AssetModelMonteCarloSimulationModel model = new MonteCarloBlackScholesModel(
				timeDiscretization,
				numberOfPaths,
				initialValue,
				riskFreeRate,
				volatility);

		return model;
	}
}
