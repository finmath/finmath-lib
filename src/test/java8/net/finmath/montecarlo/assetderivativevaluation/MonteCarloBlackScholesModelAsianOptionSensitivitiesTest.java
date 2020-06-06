/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 15.06.2016
 */

package net.finmath.montecarlo.assetderivativevaluation;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
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
public class MonteCarloBlackScholesModelAsianOptionSensitivitiesTest {

	// Model properties
	private final double	modelInitialValue   = 1.0;
	private final double	modelRiskFreeRate   = 0.05;
	private final double	modelVolatility     = 0.30;

	// Process discretization properties
	private final int		numberOfPaths		= 20000;
	private final int		numberOfTimeSteps	= 10;
	private final double	deltaT				= 0.5;

	private final int		seed				= 31415;

	// Product properties
	private final int		assetIndex = 0;
	private final double	optionMaturity = 2.0;
	private final double	optionStrike = 1.05;

	@Test
	public void testProductAADSensitivities() throws CalculationException {

		memoryUsageReset();

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
		final AsianOption europeanOption = new AsianOption(optionMaturity, optionStrike, new TimeDiscretizationFromArray(0.5, 1.0, 1.5, 2.0));
		final RandomVariable value = europeanOption.getValue(0.0, monteCarloBlackScholesModel);

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

		final double eps = 1E-6;

		final Map<String, Object> dataModifiedInitialValue = new HashMap<>();
		dataModifiedInitialValue.put("initialValue", modelInitialValue+eps);
		final double deltaFiniteDifference = (europeanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedInitialValue)) - valueMonteCarlo)/eps ;

		final Map<String, Object> dataModifiedRiskFreeRate = new HashMap<>();
		dataModifiedRiskFreeRate.put("riskFreeRate", modelRiskFreeRate+eps);
		final double rhoFiniteDifference = (europeanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedRiskFreeRate)) - valueMonteCarlo)/eps ;

		final Map<String, Object> dataModifiedVolatility = new HashMap<>();
		dataModifiedVolatility.put("volatility", modelVolatility+eps);
		final double vegaFiniteDifference = (europeanOption.getValue(monteCarloBlackScholesModel.getCloneWithModifiedData(dataModifiedVolatility)) - valueMonteCarlo)/eps ;

		/*
		 * Calculate sensitivities using analytic formulas
		 */
		final double valueAnalytic = Double.NaN;
		final double deltaAnalytic = Double.NaN;
		final double rhoAnalytic = Double.NaN;
		final double vegaAnalytic = Double.NaN;

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

		System.out.println(memoryUsagePeak()/1024/1024);
		Assert.assertEquals(deltaFiniteDifference, deltaAAD, 0.005);
	}

	private void memoryUsageReset() {
		for (final MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			pool.resetPeakUsage();
		}
	}

	private long memoryUsagePeak() {
		long sumOfPeak = 0;
		for (final MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
			final MemoryUsage peak = pool.getPeakUsage();
			sumOfPeak += peak.getUsed();
		}
		return sumOfPeak;
	}
}
