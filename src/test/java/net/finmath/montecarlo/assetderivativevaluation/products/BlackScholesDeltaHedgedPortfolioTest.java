/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.02.2014
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public class BlackScholesDeltaHedgedPortfolioTest {

	private static boolean isPrintStatistics = true;
	private static boolean isPrintHedgeErrorDistribution = false;
	private static boolean isPrintHedgeFinalValues = false;

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 50000;
	private final int		numberOfTimeSteps	= 100;
	private final double	timeHorizon 		= 5;

	private AssetModelMonteCarloSimulationInterface model = null;

	public BlackScholesDeltaHedgedPortfolioTest() {
		super();

		// Create a Model (see method getModel)
		model = getModel();
	}

	public AssetModelMonteCarloSimulationInterface getModel()
	{
		// Create the time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0, numberOfTimeSteps, timeHorizon/numberOfTimeSteps);

		// Create an instance of a black scholes monte carlo model
		AssetModelMonteCarloSimulationInterface model = new MonteCarloBlackScholesModel(
				timeDiscretization,
				numberOfPaths,
				initialValue,
				riskFreeRate,
				volatility);

		return model;
	}

	@Test
	public void testHedgePerformance() throws CalculationException {
		double maturity = timeHorizon;
		double strike = initialValue*Math.exp(riskFreeRate * maturity);

		long timingCalculationStart = System.currentTimeMillis();

		EuropeanOption option = new EuropeanOption(maturity,strike);
		BlackScholesDeltaHedgedPortfolio hedge = new BlackScholesDeltaHedgedPortfolio(maturity, strike, riskFreeRate, volatility);

		RandomVariable hedgeValue = hedge.getValue(maturity, model);

		long timingCalculationEnd = System.currentTimeMillis();

		RandomVariable underlyingAtMaturity = model.getAssetValue(maturity, 0);
		RandomVariable optionValue = option.getValue(maturity, model);
		RandomVariable hedgeError = optionValue.sub(hedgeValue);

		double hedgeErrorRMS = hedgeError.getStandardDeviation();

		TimeDiscretizationInterface td = new TimeDiscretization(-1.0-0.01, 101, 0.02);
		double[] hedgeErrorHist = hedgeError.getHistogram(td.getAsDoubleArray());

		if(isPrintHedgeErrorDistribution) {
			System.out.println(td.getTime(0) + "\t" + hedgeErrorHist[0]);
			for(int i=0; i<hedgeErrorHist.length-2; i++) {
				System.out.println((td.getTime(i)+td.getTime(i+1))/2 + "\t" + hedgeErrorHist[i+1]);
			}
			System.out.println(td.getTime(hedgeErrorHist.length-2) + "\t" + hedgeErrorHist[hedgeErrorHist.length-1]);
		}

		if(isPrintHedgeFinalValues) {
			for(int i=0; i<hedgeError.size(); i++) {
				System.out.println(underlyingAtMaturity.get(i) + "\t" + hedgeValue.get(i) + "\t" + optionValue.get(i));
			}
		}

		if(isPrintStatistics) {
			System.out.println("Calculation time: " + (timingCalculationEnd-timingCalculationStart) / 1000.0 + " s.");
			System.out.println("Hedge error (RMS): " + hedgeErrorRMS);
		}

		Assert.assertTrue(hedgeErrorRMS < 0.035);
	}
}
