/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 25.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 *
 */
public class DisplacedLognomalModelTest {

	// Model properties
	private final double	initialValue	= 1.0;
	private final double	riskFreeRate	= 0.05;
	private final double	displacement	= 0.5;
	private final double	volatility		= 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 20000;
	private final int		numberOfTimeSteps	= 1000;
	private final double	deltaT				= 0.005;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 5.0;
	private final double	optionStrike = 1.05;

	@Test
	public void testProductImplementation() throws CalculationException {
		// Create a time discretizeion
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a brownianMotion
		BrownianMotionInterface brownianMotion = new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed);

		AssetModelMonteCarloSimulationInterface monteCarloDisplacedModel;
		{
			// Create a corresponding MC process
			AbstractProcess process = new ProcessEulerScheme(brownianMotion);

			// Using the process (Euler scheme), create an MC simulation of a displaced model
			AbstractModelInterface displacedModel2 = new InhomogeneousDisplacedLognomalModel(initialValue, riskFreeRate, displacement, volatility);

			monteCarloDisplacedModel = new MonteCarloAssetModel(displacedModel2, process);
		}
		
		double alpha = 1/(1+displacement);
		/*
		 * sigma (S+d) = sigma* (a S + (1-a))
		 * sigma = sigma* a
		 * sigma d = sigma* (1-a)
		 * sigma* = sigma* (1-a)/a/d
		 * d = (1-a)/a
		 */
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel;
		{
			// Create a corresponding MC process
			AbstractProcess process = new ProcessEulerScheme(brownianMotion);

			// Using the process (Euler scheme), create an MC simulation of a displaced model
			AbstractModelInterface blackScholesModel = new BlackScholesModel(initialValue*alpha, riskFreeRate*alpha, volatility/alpha);

			monteCarloBlackScholesModel = new MonteCarloAssetModel(blackScholesModel, process);
		}
		
		AssetModelMonteCarloSimulationInterface monteCarloBachelierModel;
		{
			// Create a corresponding MC process
			AbstractProcess process = new ProcessEulerScheme(brownianMotion);

			// Using the process (Euler scheme), create an MC simulation of a displaced model
			AbstractModelInterface bachelierModel = new InhomogenousBachelierModel(initialValue*(1-alpha), riskFreeRate*(1-alpha), volatility*displacement);

			monteCarloBachelierModel = new MonteCarloAssetModel(bachelierModel, process);
		}
		


		/*
		 * Value a call option (using the product implementation)
		 */
		EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike);
		double valueX = europeanOption.getValue(monteCarloDisplacedModel);
		double value1 = europeanOption.getValue(monteCarloBlackScholesModel);
		double value2 = europeanOption.getValue(monteCarloBachelierModel);
		double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		System.out.println("value using Monte-Carlo Displaced Model.......: " + valueX);
		System.out.println("value using Monte-Carlo Black Scholes Model...: " + value1);
		System.out.println("value using Monte-Carlo Bachelier Model.......: " + value2);
		System.out.println("value using Monte-Carlo Average of 2 and 3....: " + (alpha * value1 + (1-alpha) * value2));
		System.out.println("value using analytic formula..: " + valueAnalytic);

		/*
		 * Value a call option - directly
		 */
		
		RandomVariableInterface asset1 = monteCarloBlackScholesModel.getAssetValue(timeDiscretization.getTimeIndex(optionMaturity), assetIndex);
		RandomVariableInterface asset2 = monteCarloBachelierModel.getAssetValue(timeDiscretization.getTimeIndex(optionMaturity), assetIndex);
		RandomVariableInterface asset = asset1.mult(1).add(asset2.mult(1));
		RandomVariableInterface numeraireAtPayment = monteCarloBlackScholesModel.getNumeraire(optionMaturity);
		RandomVariableInterface numeraireAtEval = monteCarloBlackScholesModel.getNumeraire(0.0);
		
		RandomVariableInterface payoff = asset.sub(optionStrike).floor(0.0);
		double value3 = payoff.div(numeraireAtPayment).mult(numeraireAtEval).getAverage();
		System.out.println("value using Monte-Carlo.......: " + value3);

		Assert.assertEquals(valueAnalytic, valueX, 0.005);
	}

}
