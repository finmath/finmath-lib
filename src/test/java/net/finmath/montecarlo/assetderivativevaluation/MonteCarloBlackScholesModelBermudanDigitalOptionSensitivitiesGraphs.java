/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanDigitalOption.ExerciseMethod;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
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

	public static void main(String[] arguments) {
	}

	@Test
	public void testProductAADSensitivities() throws CalculationException {
		AbstractRandomVariableDifferentiableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory());

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		RandomVariableDifferentiableInterface initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		RandomVariableDifferentiableInterface riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		RandomVariableDifferentiableInterface volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, deltaT);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		/*
		 * Value a call option (using the product implementation)
		 */
		double[] exerciseDates = new double[] { 1.0, 2.0, 3.0, 4.0 };
		double[] notionals = new double[] { 1.0, 1.0, 1.0, 1.0 };
		double[] strikes = new double[] { 0.50, 0.60, 0.80, 1.00 };
		Map<String, Object> properties = new HashMap<String, Object>(); properties.put("orderOfRegressionPolynomial",new Integer(1));
		BermudanDigitalOption bermudanOption = new BermudanDigitalOption(exerciseDates, notionals, strikes, ExerciseMethod.ESTIMATE_COND_EXPECTATION, properties);
//		double[] strikes = new double[] { optionStrike, optionStrike, optionStrike, optionStrike };
//		BermudanOption bermudanOption = new BermudanOption(exerciseDates, notionals, strikes, BermudanOption.ExerciseMethod.ESTIMATE_COND_EXPECTATION);
		RandomVariableInterface value = (RandomVariableDifferentiableInterface) bermudanOption.getValue(0.0, monteCarloBlackScholesModel);

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

		for(double eps = 8; eps > 1E-6; eps/=1.2) {
			Map<String, Object> dataModifiedInitialValueUp = new HashMap<String, Object>();
			dataModifiedInitialValueUp.put("initialValue", modelInitialValue+eps);
			Map<String, Object> dataModifiedInitialValueDn = new HashMap<String, Object>();
			dataModifiedInitialValueDn.put("initialValue", modelInitialValue-eps);
			double deltaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValueUp)) - bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValueDn)))/(2*eps);

			Map<String, Object> dataModifiedRiskFreeRate = new HashMap<String, Object>();
			dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
			double rhoFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

			Map<String, Object> dataModifiedVolatility = new HashMap<String, Object>();
			dataModifiedVolatility.put("volatility", modelVolatility+eps);
			double vegaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;
			
			System.out.println(eps + "\t" + deltaAAD + "\t" + deltaFiniteDifference);
		}
		
		double eps = 1E-3;
		Map<String, Object> dataModifiedInitialValue = new HashMap<String, Object>();
		dataModifiedInitialValue.put("initialValue", modelInitialValue+eps);
		double deltaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValue)) - valueMonteCarlo)/eps ;

		Map<String, Object> dataModifiedRiskFreeRate = new HashMap<String, Object>();
		dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
		double rhoFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

		Map<String, Object> dataModifiedVolatility = new HashMap<String, Object>();
		dataModifiedVolatility.put("volatility", modelVolatility+eps);
		double vegaFiniteDifference = (bermudanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;

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
