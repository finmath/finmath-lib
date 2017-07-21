/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 07.02.2014
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.BlackScholesDeltaHedgedPortfolio;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public class BlackScholesDeltaHedgedPortfolioTest {

    // Model properties
    private final double	initialValue   = 1.0;
    private final double	riskFreeRate   = 0.05;
    private final double	volatility     = 0.30;

    // Process discretization properties
    private final int		numberOfPaths		= 100000;
    private final int		numberOfTimeSteps	= 1000;
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

        EuropeanOption option = new EuropeanOption(maturity,strike);
        BlackScholesDeltaHedgedPortfolio hedge = new BlackScholesDeltaHedgedPortfolio(maturity, strike, riskFreeRate, volatility);

        RandomVariableInterface hedgeError = option.getValue(maturity, model).sub(hedge.getValue(maturity, model));

        double hedgeErrorRMS = hedgeError.getStandardDeviation();

        System.out.println("Hedge error (RMS): " + hedgeErrorRMS);

        Assert.assertTrue(hedgeErrorRMS < 0.01);
    }
}
