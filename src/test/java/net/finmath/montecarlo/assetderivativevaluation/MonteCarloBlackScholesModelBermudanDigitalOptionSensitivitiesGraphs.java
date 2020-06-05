/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption.ExerciseMethod;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
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
public class MonteCarloBlackScholesModelBermudanDigitalOptionSensitivitiesGraphs {

	// Model properties
	private final double	modelInitialValue   = 1.0;
	private final double	modelRiskFreeRate   = 0.05;
	private final double	modelVolatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 1000000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.5;

	private final int		seed				= 31415; //501; //101; //21781; //31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 2.0;
	private final double	optionStrike = 1.05;

	public static void main(final String[] arguments) {
	}

	@Test
	public void testProductAADSensitivities() throws CalculationException {
		final AbstractRandomVariableDifferentiableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory());

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
		final double[] exerciseDates = new double[] { 1.0, 2.0, 3.0, 4.0 };
		final double[] notionals = new double[] { 1.0, 1.0, 1.0, 1.0 };
		final double[] strikes = new double[] { 0.50, 0.60, 0.80, 1.00 };
		final Map<String, Object> properties = new HashMap<>(); properties.put("orderOfRegressionPolynomial",new Integer(1));
		final BermudanDigitalOption bermudanOption = new BermudanDigitalOption(exerciseDates, notionals, strikes, ExerciseMethod.ESTIMATE_COND_EXPECTATION, properties);
		//		double[] strikes = new double[] { optionStrike, optionStrike, optionStrike, optionStrike };
		//		BermudanOption bermudanOption = new BermudanOption(exerciseDates, notionals, strikes, BermudanOption.ExerciseMethod.ESTIMATE_COND_EXPECTATION);
		final RandomVariable value = bermudanOption.getValue(0.0, monteCarloBlackScholesModel);

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

		for(double eps = 0.8; eps > 1E-6; eps/=1.2) {
			final Map<String, Object> dataModifiedInitialValueUp = new HashMap<>();
			dataModifiedInitialValueUp.put("initialValue", modelInitialValue+eps);
			final Map<String, Object> dataModifiedInitialValueDn = new HashMap<>();
			dataModifiedInitialValueDn.put("initialValue", modelInitialValue-eps);
			final double deltaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValueUp)) - bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValueDn)))/(2*eps);

			final Map<String, Object> dataModifiedRiskFreeRate = new HashMap<>();
			dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
			final double rhoFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

			final Map<String, Object> dataModifiedVolatility = new HashMap<>();
			dataModifiedVolatility.put("volatility", modelVolatility+eps);
			final double vegaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;

			System.out.println(eps + "\t" + deltaAAD + "\t" + deltaFiniteDifference);
		}

		final double eps = 1E-3;
		final Map<String, Object> dataModifiedInitialValue = new HashMap<>();
		dataModifiedInitialValue.put("initialValue", modelInitialValue+eps);
		final double deltaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValue)) - valueMonteCarlo)/eps ;

		final Map<String, Object> dataModifiedRiskFreeRate = new HashMap<>();
		dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
		final double rhoFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

		final Map<String, Object> dataModifiedVolatility = new HashMap<>();
		dataModifiedVolatility.put("volatility", modelVolatility+eps);
		final double vegaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;

		System.out.println("value using Monte-Carlo.......: " + valueMonteCarlo);
		System.out.println();

		System.out.println("delta using adj. auto diff....: " + deltaAAD);
		System.out.println("delta using finite differences: " + deltaFiniteDifference);
		System.out.println();

		System.out.println("rho using adj. auto diff....: " + rhoAAD);
		System.out.println("rho using finite differences: " + rhoFiniteDifference);
		System.out.println();

		System.out.println("vega using Madj. auto diff...: " + vegaAAD);
		System.out.println("vega using finite differences: " + vegaFiniteDifference);
		System.out.println();

		//		Assert.assertEquals(valueAnalytic, value, 0.005);
	}
}
