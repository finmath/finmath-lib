/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 07.02.2014
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
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
 */
@RunWith(Parameterized.class)
public class BlackScholesDeltaHedgedPortfolioWithAADTest {

	private static boolean isPrintHedgeErrorDistribution = false;
	private static boolean isPrintHedgeFinalValues = false;
	private static boolean isPrintExerciseProbabilities = true;
	private static boolean isPrintStatistics = true;

	// Model properties
	private static final double	modelInitialValue   = 1.0;
	private static final double	modelRiskFreeRate   = 0.05;
	private static final double	modelVolatility     = 0.30;

	// Process discretization properties
	private static final int	numberOfPaths		= 50000;
	private static final int	numberOfTimeSteps	= 100;
	private static final double	timeHorizon 		= 5;

	private final int		seed				= 31415;

	private AssetModelMonteCarloSimulationInterface model = null;
	private AbstractAssetMonteCarloProduct option = null;

	@Parameters
	public static Collection<Object[]> data() {
		Collection<Object[]> testParameters = new ArrayList<>();

		double maturity = timeHorizon;
		double strike = modelInitialValue*Math.exp(modelRiskFreeRate * maturity);
		AbstractAssetMonteCarloProduct europeanOption = new EuropeanOption(maturity,strike);

		double[] exerciseDates = new double[] {2.0, 3.0, 4.0, maturity };
		double[] notionals = new double[] { 1.0, 1.0, 1.0, 1.0 };
		double[] strikes = new double[] { 0.7*strike, 0.75*strike, 0.85*strike, strike };
		AbstractAssetMonteCarloProduct bermudanOption = new BermudanOption(exerciseDates, notionals, strikes);


		/*
		 * For performance: either use a warm up period or focus on a single product.
		 */
		testParameters.add(new Object[] { europeanOption });
		testParameters.add(new Object[] { bermudanOption });

		return testParameters;
	}

	private static Double savedBarrierDiracWidth;
	private static Boolean savedIsGradientRetainsLeafNodesOnly;

	public BlackScholesDeltaHedgedPortfolioWithAADTest(AbstractAssetMonteCarloProduct product) {
		super();

		this.option = product;
		// Create a Model (see method getModel)
		this.model = getModel();
	}

	public AssetModelMonteCarloSimulationInterface getModel()
	{
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("barrierDiracWidth", new Double(0.0));
		properties.put("isGradientRetainsLeafNodesOnly", new Boolean(false));
		RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(), properties);

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		RandomVariableDifferentiableInterface initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		RandomVariableDifferentiableInterface riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		RandomVariableDifferentiableInterface volatility	= randomVariableFactory.createRandomVariable(modelVolatility);

		// Create a model
		AbstractModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, timeHorizon/numberOfTimeSteps);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		return monteCarloBlackScholesModel;
	}

	@Test
	public void testHedgePerformance() throws CalculationException {

		double maturity = timeHorizon;

		long timingCalculationStart = System.currentTimeMillis();

		BlackScholesDeltaHedgedPortfolioWithAAD hedge = new BlackScholesDeltaHedgedPortfolioWithAAD(option);

		RandomVariableInterface hedgeValue = hedge.getValue(maturity, model);

		long timingCalculationEnd = System.currentTimeMillis();

		RandomVariableInterface underlyingAtMaturity = model.getAssetValue(maturity, 0);
		RandomVariableInterface optionValue = option.getValue(0.0, model).mult(model.getNumeraire(maturity));
		RandomVariableInterface hedgeError = optionValue.sub(hedgeValue);

		double hedgeErrorRMS = Math.sqrt(hedgeError.getVariance());

		TimeDiscretizationInterface td = new TimeDiscretization(-1.0-0.01, 101, 0.02);
		double[] hedgeErrorHist = hedgeError.getHistogram(td.getAsDoubleArray());

		System.out.println("Testing delta hedge of: " + option.getClass().getSimpleName());

		if(isPrintHedgeErrorDistribution) {
			System.out.println(td.getTime(0) + "\t" + hedgeErrorHist[0]);
			for(int i=0; i<hedgeErrorHist.length-2; i++) {
				System.out.println((td.getTime(i)+td.getTime(i+1))/2 + "\t" + hedgeErrorHist[i+1]);
			}
			System.out.println(td.getTime(hedgeErrorHist.length-2) + "\t" + hedgeErrorHist[hedgeErrorHist.length-1]);
		}

		if(isPrintHedgeFinalValues) {
			if(option instanceof BermudanOption) {
				RandomVariableInterface exerciseTime = ((BermudanOption) option).getLastValuationExerciseTime();
				for(double time : new double[] { 4.0, 3.0, 2.0}) {
					underlyingAtMaturity = underlyingAtMaturity.barrier(exerciseTime.sub(time+0.01), underlyingAtMaturity, model.getAssetValue(time, 0));

				}
			}
			for(int i=0; i<hedgeError.size(); i++) {
				if(i==10000) break;
				System.out.println(underlyingAtMaturity.get(i) + "\t" + hedgeValue.get(i) + "\t" + optionValue.get(i));
			}
		}

		if(isPrintExerciseProbabilities && option instanceof BermudanOption) {
			RandomVariableInterface exerciseTime = ((BermudanOption) option).getLastValuationExerciseTime();
			double[] exerciseDates = ((BermudanOption) option).getExerciseDates();
			double[] probabilities = exerciseTime.getHistogram(exerciseDates);
			for(int exerciseDateIndex=0; exerciseDateIndex<exerciseDates.length; exerciseDateIndex++)
			{
				double time = exerciseDates[exerciseDateIndex];
				System.out.println(time + "\t" + probabilities[exerciseDateIndex]);
			}
			System.out.println("NEVER" + "\t" + probabilities[exerciseDates.length]);
		}

		if(isPrintStatistics) {
			System.out.println("Number of sensitivities in delta hedge simulation...:" + (numberOfTimeSteps*numberOfPaths));
			System.out.println("Calculation time (total).......: " + (timingCalculationEnd-timingCalculationStart) / 1000.0 + " s.");
			System.out.println("Calculation time (valuation)...: " + hedge.getLastOperationTimingValuation() + " s.");
			System.out.println("Calculation time (derivative)..: " + hedge.getLastOperationTimingDerivative() + " s.");
			System.out.println("Hedge error (RMS)..............: " + hedgeErrorRMS);
		}

		System.out.println("________________________________________________\n");

		Assert.assertTrue(hedgeErrorRMS < 0.05);

	}
}
