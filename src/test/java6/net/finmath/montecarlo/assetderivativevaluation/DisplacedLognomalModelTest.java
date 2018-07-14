/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
	private final double	displacement	= 0.75;
	private final double	volatility		= 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 20000;
	private final int		numberOfTimeSteps	= 1000;
	private final double	deltaT				= 0.005;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 5.0;
	private final double	optionStrike = 1.25;

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

		/*
		 * Note: The inhomogenous displaced model is not the same as the corresponding interpolation
		 * of Black and Bachelier. For a homogenous displaced model there is a relation between dispalcement
		 * and interpolation:
		 * 	sigma (S+d) = sigma* (a S + (1-a))
		 * implies
		 * 	sigma = sigma* a
		 * 	sigma d = sigma* (1-a)
		 * that is
		 * 	d = (1-a)/a
		 * 	a = 1/(1+d)
		 */
		double alpha = 1/(1+displacement);
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel;
		{
			// Create a corresponding MC process
			AbstractProcess process = new ProcessEulerScheme(brownianMotion);

			// Using the process (Euler scheme), create an MC simulation of a displaced model
			AbstractModelInterface blackScholesModel = new BlackScholesModel(initialValue, riskFreeRate, volatility);

			monteCarloBlackScholesModel = new MonteCarloAssetModel(blackScholesModel, process);
		}

		AssetModelMonteCarloSimulationInterface monteCarloBachelierModel;
		{
			// Create a corresponding MC process
			AbstractProcess process = new ProcessEulerScheme(brownianMotion);

			// Using the process (Euler scheme), create an MC simulation of a displaced model
			AbstractModelInterface bachelierModel = new InhomogenousBachelierModel(initialValue, riskFreeRate, volatility*displacement);

			monteCarloBachelierModel = new MonteCarloAssetModel(bachelierModel, process);
		}

		/*
		 * Value a call option (using the product implementation)
		 */
		EuropeanOption europeanOption = new EuropeanOption(optionMaturity, optionStrike);
		double valueDisplaced = europeanOption.getValue(monteCarloDisplacedModel);
		double valueBlack = europeanOption.getValue(monteCarloBlackScholesModel);
		double valueBachelier = europeanOption.getValue(monteCarloBachelierModel);
		double valueAnalytic = AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, optionMaturity, optionStrike);

		System.out.println("value using Monte-Carlo Displaced Model.......: " + valueDisplaced);
		System.out.println("value using Monte-Carlo Black Scholes Model...: " + valueBlack);
		System.out.println("value using Monte-Carlo Bachelier Model.......: " + valueBachelier);
		System.out.println("value using sum of Black and Bachelier........: " + (valueBlack + valueBachelier));
		System.out.println("value using analytic formula for Black Model..: " + valueAnalytic);

		Assert.assertEquals(valueBlack, valueAnalytic, 0.005);
	}

}

