/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

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
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.MonteCarloProduct;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption.ExerciseMethod;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
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
@RunWith(Parameterized.class)
public class MonteCarloBlackScholesModelSensitivitiesTest {

	/*
	 * Test products (static)
	 */
	static BermudanDigitalOption bermudanOption;
	static {
		final double	optionStrike = 1.05;

		final double[] exerciseDates = new double[] { 1.0, 2.0, 3.0, 4.0 };
		final double[] notionals = new double[] { 1.0, 1.0, 1.0, 1.0 };
		final double[] strikes = new double[] { 0.30*optionStrike, 0.40*optionStrike, 0.50*optionStrike, 1.25 };
		final Map<String, Object> properties = new HashMap<>(); properties.put("orderOfRegressionPolynomial", 1);
		bermudanOption = new BermudanDigitalOption(exerciseDates, notionals, strikes, ExerciseMethod.ESTIMATE_COND_EXPECTATION, properties);
	}

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(
				new Object[][] {
					{ new EuropeanOption(2.0 /* optionMaturity */, 1.05 /* optionStrike */) },
					{ new AsianOption(2.0 /* optionMaturity */, 1.05 /* optionStrike */, new TimeDiscretizationFromArray(0.0, 0.5, 1.0, 1.5, 2.0)) },
					{ bermudanOption }
				}
				);
	}

	// Model properties
	private final double	modelInitialValue   = 1.0;
	private final double	modelRiskFreeRate   = 0.05;
	private final double	modelVolatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 200000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.5;

	private final int		seed				= 31415;

	// Product properties
	private final MonteCarloProduct product;

	public MonteCarloBlackScholesModelSensitivitiesTest(final MonteCarloProduct product) {
		this.product = product;
	}

	@Test
	public void testProductAADSensitivities() throws CalculationException {
		final Map<String, Double> sensitivitiesAAD	= getSensitivitiesViaAAD();
		final Map<String, Double> sensitivitiesFD		= getSensitivitiesViaFiniteDifferences();


		System.out.println("\n");
		System.out.println("Testing " + product.getClass().getSimpleName());
		System.out.println("__________________________________________________________");
		System.out.println();

		System.out.println("value using Monte-Carlo (AAD random variable).: " + sensitivitiesAAD.get("value"));
		System.out.println("value using Monte-Carlo (plain)...............: " + sensitivitiesFD.get("value"));
		System.out.println();

		System.out.println("delta using adj. auto diff....: " + sensitivitiesAAD.get("delta"));
		System.out.println("delta using finite differences: " + sensitivitiesFD.get("delta"));
		System.out.println();

		System.out.println("rho using adj. auto diff....: " + sensitivitiesAAD.get("rho"));
		System.out.println("rho using finite differences: " + sensitivitiesFD.get("rho"));
		System.out.println();

		System.out.println("vega using Madj. auto diff...: " + sensitivitiesAAD.get("vega"));
		System.out.println("vega using finite differences: " + sensitivitiesFD.get("vega"));
		System.out.println();

		Assert.assertEquals(sensitivitiesAAD.get("value"), sensitivitiesFD.get("value"), 0.0);
		Assert.assertEquals(sensitivitiesAAD.get("delta"), sensitivitiesFD.get("delta"), 0.05);
		Assert.assertEquals(sensitivitiesAAD.get("rho"), sensitivitiesFD.get("rho"), 0.05);
		Assert.assertEquals(sensitivitiesAAD.get("vega"), sensitivitiesFD.get("vega"), 0.10);
	}

	private Map<String, Double> getSensitivitiesViaFiniteDifferences() throws CalculationException {
		final RandomVariableFromArrayFactory randomVariableFromArrayFactory = new RandomVariableFromArrayFactory();

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		final RandomVariable initialValue	= randomVariableFromArrayFactory.createRandomVariable(modelInitialValue);
		final RandomVariable riskFreeRate	= randomVariableFromArrayFactory.createRandomVariable(modelRiskFreeRate);
		final RandomVariable volatility	= randomVariableFromArrayFactory.createRandomVariable(modelVolatility);

		// Create a model
		final AbstractProcessModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFromArrayFactory);

		// Create a time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(model, new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		final AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		/*
		 * Value a call option (using the product implementation)
		 */
		final RandomVariable value = product.getValue(0.0, monteCarloBlackScholesModel);

		/*
		 * Calculate value
		 */
		final double valueMonteCarlo = value.getAverage();

		/*
		 * Calculate sensitivities using finite differences
		 */

		final double eps = 1E-3;

		final Map<String, Object> dataModifiedInitialValue = new HashMap<>();
		dataModifiedInitialValue.put("initialValue", modelInitialValue+eps);
		final double deltaFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValue)) - valueMonteCarlo)/eps ;

		final Map<String, Object> dataModifiedRiskFreeRate = new HashMap<>();
		dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
		final double rhoFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

		final Map<String, Object> dataModifiedVolatility = new HashMap<>();
		dataModifiedVolatility.put("volatility", modelVolatility+eps);
		final double vegaFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;

		final Map<String, Double> sensitivities = new HashMap<>();
		sensitivities.put("value", valueMonteCarlo);
		sensitivities.put("delta", deltaFiniteDifference);
		sensitivities.put("rho", rhoFiniteDifference);
		sensitivities.put("vega", vegaFiniteDifference);

		return sensitivities;
	}

	private Map<String, Double> getSensitivitiesViaAAD() throws CalculationException {
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

		final RandomVariable value = product.getValue(0.0, monteCarloBlackScholesModel);

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

		return sensitivities;
	}
}
