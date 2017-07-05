/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 *
 */
@RunWith(Parameterized.class)
public class MonteCarloBlackScholesModelSensitivitiesTest {

	@Parameters
	public static Collection<Object[]> data() {
		return (Collection<Object[]>) Arrays.asList(
				new Object[][] { 
					{ new EuropeanOption(2.0 /* optionMaturity */, 1.05 /* optionStrike */) },
					{ new AsianOption(2.0 /* optionMaturity */, 1.05 /* optionStrike */, new TimeDiscretization(0.0, 0.5, 1.0, 1.5, 2.0)) },					
				}
				);
	}

	// Model properties
	private final double	modelInitialValue   = 1.0;
	private final double	modelRiskFreeRate   = 0.05;
	private final double	modelVolatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 20000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.25;

	private final int		seed				= 31415;

	// Product properties
	private final AbstractMonteCarloProduct product;

	public MonteCarloBlackScholesModelSensitivitiesTest(AbstractMonteCarloProduct product) {
		this.product = product;
	}

	@Test
	public void testProductAADSensitivities() throws CalculationException {
		RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory());

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		RandomVariableDifferentiableInterface initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		RandomVariableDifferentiableInterface riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		RandomVariableDifferentiableInterface volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		/*
		 * Value a call option (using the product implementation)
		 */
		RandomVariableInterface value = (RandomVariableDifferentiableInterface) product.getValue(0.0, monteCarloBlackScholesModel);

		/*
		 * Calculate sensitivities using AAD
		 */
		Map<Long, RandomVariableInterface> derivative = ((RandomVariableDifferentiableInterface)value).getGradient();
		
		double valueMonteCarlo = value.getAverage();
		double deltaAAD = derivative.get(initialValue.getID()).getAverage();
		double rhoAAD = derivative.get(riskFreeRate.getID()).getAverage();
		double vegaAAD = derivative.get(volatility.getID()).getAverage();
		
		/*
		 * Calculate sensitivities using finite differences
		 */
		
		double eps = 1E-6;

		Map<String, Object> dataModifiedInitialValue = new HashMap<String, Object>();
		dataModifiedInitialValue.put("initialValue", modelInitialValue+eps);
		double deltaFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValue)) - valueMonteCarlo)/eps ;

		Map<String, Object> dataModifiedRiskFreeRate = new HashMap<String, Object>();
		dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
		double rhoFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

		Map<String, Object> dataModifiedVolatility = new HashMap<String, Object>();
		dataModifiedVolatility.put("volatility", modelVolatility+eps);
		double vegaFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;

		System.out.println("Product: " + product.getClass().getSimpleName());
		System.out.println("value using Monte-Carlo.......: " + valueMonteCarlo);
		System.out.println("");
		
		System.out.println("delta using adj. auto diff....: " + deltaAAD);
		System.out.println("delta using finite differences: " + deltaFiniteDifference);
		System.out.println("");

		System.out.println("rho using adj. auto diff....: " + rhoAAD);
		System.out.println("rho using finite differences: " + rhoFiniteDifference);
		System.out.println("");

		System.out.println("vega using Madj. auto diff...: " + vegaAAD);
		System.out.println("vega using finite differences: " + vegaFiniteDifference);
		System.out.println("");

//		Assert.assertEquals(valueAnalytic, value, 0.005);
	}
}
