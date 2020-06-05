/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.DigitalOption;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 *
 */
public class MonteCarloBlackScholesModelDigitalOptionSensitivitiesTest {

	// Model properties
	private final double	modelInitialValue   = 1.0;
	private final double	modelRiskFreeRate   = 0.05;
	private final double	modelVolatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 2000000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.5;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 2.0;
	private final double	optionStrike = 1.05;

	@Test
	public void testProductAADSensitivities() throws CalculationException {
		final RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory());

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		final RandomVariableDifferentiable initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		final RandomVariableDifferentiable riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		final RandomVariableDifferentiable volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		final AbstractProcessModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(model, new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		final AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		/*
		 * Value a call option (using the product implementation)
		 */
		final DigitalOption digitalOption = new DigitalOption(optionMaturity, optionStrike);
		final RandomVariable value = digitalOption.getValue(0.0, monteCarloBlackScholesModel);

		/*
		 * Calculate sensitivities using AAD
		 */
		final Map<Long, RandomVariable> derivative = ((RandomVariableDifferentiable)value).getGradient();

		final double valueMonteCarlo = value.getAverage();
		final double deltaAAD = derivative.get(initialValue.getID()).getAverage();
		final double rhoAAD = derivative.get(riskFreeRate.getID()).getAverage();
		final double vegaAAD = derivative.get(volatility.getID()).getAverage();

		/*
		 * Calculate sensitivities using finite differences
		 */

		final double eps = 1E-3;

		final double epsDelta = eps;
		final Map<String, Object> dataModifiedInitialValue = new HashMap<>();
		dataModifiedInitialValue.put("initialValue", modelInitialValue+eps);
		final double deltaFiniteDifference = (digitalOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValue)) - valueMonteCarlo)/epsDelta;

		final double epsRho = eps/10;
		final Map<String, Object> dataModifiedRiskFreeRate = new HashMap<>();
		dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+epsRho);
		final double rhoFiniteDifference = (digitalOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/epsRho ;

		final double epsVega = eps/10;
		final Map<String, Object> dataModifiedVolatility = new HashMap<>();
		dataModifiedVolatility.put("volatility", modelVolatility+epsVega);
		final double vegaFiniteDifference = (digitalOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/epsVega ;

		/*
		 * Calculate sensitivities using analytic formulas
		 */
		final double valueAnalytic = AnalyticFormulas.blackScholesDigitalOptionValue(modelInitialValue, modelRiskFreeRate, modelVolatility, optionMaturity, optionStrike);
		final double deltaAnalytic = AnalyticFormulas.blackScholesDigitalOptionDelta(modelInitialValue, modelRiskFreeRate, modelVolatility, optionMaturity, optionStrike);
		final double rhoAnalytic = AnalyticFormulas.blackScholesDigitalOptionRho(modelInitialValue, modelRiskFreeRate, modelVolatility, optionMaturity, optionStrike);
		final double vegaAnalytic = AnalyticFormulas.blackScholesDigitalOptionVega(modelInitialValue, modelRiskFreeRate, modelVolatility, optionMaturity, optionStrike);


		System.out.println("value using Monte-Carlo.......: " + valueMonteCarlo);
		System.out.println("value using analytic formula..: " + valueAnalytic);
		System.out.println();

		System.out.println("delta using adj. auto diff....: " + deltaAAD);
		System.out.println("delta using finite differences: " + deltaFiniteDifference);
		System.out.println("delta using analytic formula..: " + deltaAnalytic);
		System.out.println();

		System.out.println("rho using adj. auto diff....: " + rhoAAD);
		System.out.println("rho using finite differences: " + rhoFiniteDifference);
		System.out.println("rho using analytic formula..: " + rhoAnalytic);
		System.out.println();

		System.out.println("vega using Madj. auto diff...: " + vegaAAD);
		System.out.println("vega using finite differences: " + vegaFiniteDifference);
		System.out.println("vega using analytic formula..: " + vegaAnalytic);
		System.out.println();

		Assert.assertEquals("value", valueAnalytic, valueMonteCarlo, 1E-3);
		Assert.assertEquals("delta", deltaAnalytic, deltaAAD, 1E-2);
		Assert.assertEquals("rho", rhoAnalytic, rhoAAD, 2E-2);
		Assert.assertEquals("vega", vegaAnalytic, vegaAAD, 1E-2);
	}

	@Test
	public void testSensitivities() throws CalculationException {
		final RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory());

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		final RandomVariableDifferentiable initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		final RandomVariableDifferentiable riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		final RandomVariableDifferentiable volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		final AbstractProcessModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(model, new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		final AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		final double optionMaturity = 5.0;
		final double optionStrike = 1.25;

		final DigitalOption option = new DigitalOption(optionMaturity, optionStrike);
		final RandomVariable value = option.getValue(0.0, monteCarloBlackScholesModel);

		/*
		 * Calculate sensitivities using AAD
		 */
		final Map<Long, RandomVariable> derivative = ((RandomVariableDifferentiable)value).getGradient();

		final double valueMonteCarlo = value.getAverage();
		final double deltaAAD = derivative.get(initialValue.getID()).getAverage();
		final double rhoAAD = derivative.get(riskFreeRate.getID()).getAverage();
		final double vegaAAD = derivative.get(volatility.getID()).getAverage();

		final Map<String, Double> sensitivities = new HashMap<>();
		sensitivities.put("value", valueMonteCarlo);
		sensitivities.put("delta", deltaAAD);
		sensitivities.put("rho", rhoAAD);
		sensitivities.put("vega", vegaAAD);

		final double deltaAnalytic = AnalyticFormulas.blackScholesDigitalOptionDelta(modelInitialValue, modelRiskFreeRate, modelVolatility, optionMaturity, optionStrike);

		final double epsilon = 5E-4;
		final Map<String, Object> shiftedValues = new HashMap<>();
		shiftedValues.put("initialValue", modelInitialValue+epsilon);
		final RandomVariable valueUp = option.getValue(0.0, monteCarloBlackScholesModel.getCloneWithModifiedData(shiftedValues));
		final double deltaFD = (valueUp.getAverage()-value.getAverage())/epsilon;

		Assert.assertEquals("digital option delta aad", deltaAnalytic, deltaAAD, 2E-3);
		Assert.assertEquals("digital option delta finite difference", deltaAnalytic, deltaFD, 1E-2);

		/*
		System.out.println("Delta " + deltaAAD);
		System.out.println("Delta " + deltaAnalytic);
		System.out.println("Delta " + deltaFD);
		 */
	}
}
