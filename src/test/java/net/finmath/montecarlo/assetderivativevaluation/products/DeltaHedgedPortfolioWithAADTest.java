/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme;
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
public class DeltaHedgedPortfolioWithAADTest {

	private static boolean isPrintHedgeErrorDistribution = false;
	private static boolean isPrintHedgeFinalValues = false;
	private static boolean isPrintExerciseProbabilities = true;
	private static boolean isPrintStatistics = true;

	// Model properties
	private static final double	modelInitialValue   = 1.0;
	private static final double	modelRiskFreeRate   = 0.05;
	private static final double	modelVolatility     = 0.30;

	private static final double modelTheta = modelVolatility*modelVolatility;
	private static final double modelKappa = 0.1;
	private static final double modelXi = 0.0;//0.5;		// For a pure delta hedge, quality depends on vol-of-vol
	private static final double modelRho = 0.1;

	private final static Scheme scheme = Scheme.REFLECTION;

	// Process discretization properties
	private static final int	numberOfPaths		= 50000;
	private static final int	numberOfTimeSteps	= 100;
	private static final double	timeHorizon 		= 5;

	private static final int	seed				= 31415;

	private AssetModelMonteCarloSimulationInterface model = null;
	private AbstractAssetMonteCarloProduct option = null;

	@Parameters(name="{0}-{1}")
	public static Collection<Object[]> data() {
		Collection<Object[]> testParameters = new ArrayList<>();

		double maturity = timeHorizon;
		double strike = modelInitialValue*Math.exp(modelRiskFreeRate * maturity);
		AbstractAssetMonteCarloProduct europeanOption = new EuropeanOption(maturity,strike);

		double[] exerciseDates = new double[] {2.0, 3.0, 4.0, maturity };
		double[] notionals = new double[] { 1.0, 1.0, 1.0, 1.0 };
		double[] strikes = new double[] { 0.7*strike, 0.75*strike, 0.85*strike, strike };
		AbstractAssetMonteCarloProduct bermudanOption = new BermudanOption(exerciseDates, notionals, strikes);

		// Create a Model (see method getModel)
		AssetModelMonteCarloSimulationInterface blackScholesModel = getBlackScholesModel();
		AssetModelMonteCarloSimulationInterface hestonModel = getHestonModel();

		/*
		 * For performance: either use a warm up period or focus on a single product.
		 */
		testParameters.add(new Object[] { blackScholesModel, europeanOption });
		testParameters.add(new Object[] { blackScholesModel, bermudanOption });
		//		testParameters.add(new Object[] { hestonModel, europeanOption });
		//		testParameters.add(new Object[] { hestonModel, bermudanOption });

		return testParameters;
	}

	public DeltaHedgedPortfolioWithAADTest(AssetModelMonteCarloSimulationInterface model, AbstractAssetMonteCarloProduct product) {
		super();

		this.model = model;
		this.option = product;
	}

	public static AssetModelMonteCarloSimulationInterface getBlackScholesModel()
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

		// Create a Brownian motion
		BrownianMotionInterface brownianMotion = new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(brownianMotion, net.finmath.montecarlo.process.ProcessEulerScheme.Scheme.EULER_FUNCTIONAL);

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

		return monteCarloBlackScholesModel;
	}

	public static AssetModelMonteCarloSimulationInterface getHestonModel()
	{
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("barrierDiracWidth", new Double(0.0));
		properties.put("isGradientRetainsLeafNodesOnly", new Boolean(false));
		RandomVariableDifferentiableAADFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(), properties);

		// Generate independent variables (quantities w.r.t. to which we like to differentiate)
		RandomVariableDifferentiableInterface initialValue	= randomVariableFactory.createRandomVariable(modelInitialValue);
		RandomVariableDifferentiableInterface riskFreeRate	= randomVariableFactory.createRandomVariable(modelRiskFreeRate);
		RandomVariableDifferentiableInterface volatility	= randomVariableFactory.createRandomVariable(modelVolatility);
		RandomVariableDifferentiableInterface theta	= randomVariableFactory.createRandomVariable(modelTheta);
		RandomVariableDifferentiableInterface kappa	= randomVariableFactory.createRandomVariable(modelKappa);
		RandomVariableDifferentiableInterface xi	= randomVariableFactory.createRandomVariable(modelXi);
		RandomVariableDifferentiableInterface rho	= randomVariableFactory.createRandomVariable(modelRho);

		// Create a model
		AbstractModel model = new HestonModel(initialValue, riskFreeRate, volatility, riskFreeRate, theta, kappa, xi, rho, scheme, randomVariableFactory);

		// Create a time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0 /* initial */, numberOfTimeSteps, timeHorizon/numberOfTimeSteps);

		// Create a Brownian motion
		BrownianMotionInterface brownianMotion = new BrownianMotion(timeDiscretization, 2 /* numberOfFactors */, numberOfPaths, seed);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(brownianMotion, ProcessEulerScheme.Scheme.EULER_FUNCTIONAL);

		// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
		AssetModelMonteCarloSimulationInterface monteCarloHestonModel = new MonteCarloAssetModel(model, process);

		return monteCarloHestonModel;
	}

	@Test
	public void testHedgePerformance() throws CalculationException {

		double maturity = timeHorizon;

		long timingCalculationStart = System.currentTimeMillis();

		DeltaHedgedPortfolioWithAAD hedge = new DeltaHedgedPortfolioWithAAD(option);

		RandomVariableInterface hedgeValue = hedge.getValue(maturity, model);

		long timingCalculationEnd = System.currentTimeMillis();

		RandomVariableInterface underlyingAtMaturity = model.getAssetValue(maturity, 0);
		RandomVariableInterface optionValue = option.getValue(0.0, model).mult(model.getNumeraire(maturity)).div(model.getNumeraire(0));
		RandomVariableInterface hedgeError = optionValue.sub(hedgeValue);

		double hedgeErrorRMS = Math.sqrt(hedgeError.getVariance());

		TimeDiscretizationInterface td = new TimeDiscretization(-1.0-0.01, 101, 0.02);
		double[] hedgeErrorHist = hedgeError.getHistogram(td.getAsDoubleArray());

		System.out.println("Testing delta hedge of " + option.getClass().getSimpleName() + " using " + model.getClass().getSimpleName() + ".");

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
				if(i==10000) {
					break;
				}
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
				System.out.println("P(\u03C4 = " + time + ") = " + probabilities[exerciseDateIndex]);
			}
			System.out.println("P(\u03C4 > " + exerciseDates[exerciseDates.length-1] + ") = " + probabilities[exerciseDates.length]);
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
