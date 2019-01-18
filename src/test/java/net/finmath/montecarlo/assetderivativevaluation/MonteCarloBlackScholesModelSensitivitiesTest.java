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
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption.ExerciseMethod;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

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

		double[] exerciseDates = new double[] { 1.0, 2.0, 3.0, 4.0 };
		double[] notionals = new double[] { 1.0, 1.0, 1.0, 1.0 };
		double[] strikes = new double[] { 0.30*optionStrike, 0.40*optionStrike, 0.50*optionStrike, 1.25 };
		Map<String, Object> properties = new HashMap<String, Object>(); properties.put("orderOfRegressionPolynomial", 1);
		bermudanOption = new BermudanDigitalOption(exerciseDates, notionals, strikes, ExerciseMethod.ESTIMATE_COND_EXPECTATION, properties);
	}

	@Parameters
	public static Collection<Object[]> data() {
		return Arrays.asList(
				new Object[][] {
					{ new EuropeanOption(2.0 /* optionMaturity */, 1.05 /* optionStrike */) },
					{ new AsianOption(2.0 /* optionMaturity */, 1.05 /* optionStrike */, new TimeDiscretization(0.0, 0.5, 1.0, 1.5, 2.0)) },
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
	private final AbstractMonteCarloProduct product;

	public MonteCarloBlackScholesModelSensitivitiesTest(AbstractMonteCarloProduct product) {
		this.product = product;
	}

	@Test
	public void testProductAADSensitivities() throws CalculationException {
		Map<String, Double> sensitivitiesAAD	= getSensitivitiesViaAAD();
		Map<String, Double> sensitivitiesFD		= getSensitivitiesViaFiniteDifferences();


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
		RandomVariableFactory randomVariableFactory = new RandomVariableFactory();

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		RandomVariable initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		RandomVariable riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		RandomVariable volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotionLazyInit(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		/*
		 * Value a call option (using the product implementation)
		 */
		RandomVariable value = product.getValue(0.0, monteCarloBlackScholesModel);

		/*
		 * Calculate value
		 */
		double valueMonteCarlo = value.getAverage();

		/*
		 * Calculate sensitivities using finite differences
		 */

		double eps = 1E-3;

		Map<String, Object> dataModifiedInitialValue = new HashMap<String, Object>();
		dataModifiedInitialValue.put("initialValue", modelInitialValue+eps);
		double deltaFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValue)) - valueMonteCarlo)/eps ;

		Map<String, Object> dataModifiedRiskFreeRate = new HashMap<String, Object>();
		dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
		double rhoFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

		Map<String, Object> dataModifiedVolatility = new HashMap<String, Object>();
		dataModifiedVolatility.put("volatility", modelVolatility+eps);
		double vegaFiniteDifference = (product.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;

		Map<String, Double> sensitivities = new HashMap<>();
		sensitivities.put("value", valueMonteCarlo);
		sensitivities.put("delta", deltaFiniteDifference);
		sensitivities.put("rho", rhoFiniteDifference);
		sensitivities.put("vega", vegaFiniteDifference);

		return sensitivities;
	}

	private Map<String, Double> getSensitivitiesViaAAD() throws CalculationException {
		RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory());

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		RandomVariableDifferentiable initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		RandomVariableDifferentiable riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		RandomVariableDifferentiable volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotionLazyInit(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		RandomVariable value = product.getValue(0.0, monteCarloBlackScholesModel);

		/*
		 * Calculate sensitivities using AAD
		 */
		Map<Long, RandomVariable> derivative = ((RandomVariableDifferentiable)value).getGradient();

		double valueMonteCarlo = value.getAverage();
		double deltaAAD = derivative.get(initialValue.getID()).getAverage();
		double rhoAAD = derivative.get(riskFreeRate.getID()).getAverage();
		double vegaAAD = derivative.get(volatility.getID()).getAverage();

		Map<String, Double> sensitivities = new HashMap<>();
		sensitivities.put("value", valueMonteCarlo);
		sensitivities.put("delta", deltaAAD);
		sensitivities.put("rho", rhoAAD);
		sensitivities.put("vega", vegaAAD);

		return sensitivities;
	}
}
