/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 01.07.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.automaticdifferentiation.backward.alternative.RandomVariableDifferentiableAADPathwiseFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.alternative.RandomVariableDifferentiableAADStochasticNonOptimizedFactory;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

@RunWith(Parameterized.class)
public class RandomVariableDifferentiableAADPerformanceTest {
	@Parameters
	public static Collection<Object[]> data() {
		Collection<Object[]> testParameters = new ArrayList<>();
		for(int i=0; i<testCases.length; i++) {
			for(int j=0; j<testMethods.length; j++) {
				Object[] testParmeter = new Object[2];
				testParmeter[0] = testCases[i];
				testParmeter[1] = testMethods[j];
				testParameters.add(testParmeter);
			}
		}
		return testParameters;
	}

	private static DecimalFormat formatReal1 = new DecimalFormat("####0.0", new DecimalFormatSymbols(Locale.ENGLISH));

	private interface TestFunction {
		RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters);
		RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters);
	}

	private static class TestFunctionBigSum implements TestFunction {
		private static final int numberOfIterations = 7500;

		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface x = arguments[0];
			RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = sum.add(x);
			}
			return sum;
		}

		public RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			return new RandomVariableInterface[] {
					(new RandomVariableFactory()).createRandomVariable(numberOfIterations)
			};
		}
	}

	private static class TestFunctionGeometricSum implements TestFunction {
		private static final int numberOfIterations = 5000;

		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface x = arguments[0];
			RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = sum.add(x.pow(i));
			}
			return sum;
		}

		public RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface x = arguments[0];
			RandomVariableInterface sum = (new RandomVariableFactory()).createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = sum.add(x.pow(i-1).mult(i));
			}
			return new RandomVariableInterface[] { sum };
		}
	}

	private static class TestFunctionSumOfProducts implements TestFunction {
		private static final int numberOfIterations = 5;

		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++) {
				for(int j = 0; j < arguments.length; j++) {
					sum = sum.addProduct(arguments[j],parameters[j]);
				}
			}
			return sum;
		}

		@Override
		public RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface sum = (new RandomVariableFactory()).createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				{
					int j = 0;
					sum = sum.add(parameters[j]);
				}
			}
			return new RandomVariableInterface[] { sum };
		}
	}

	private static class TestFunctionSumOfProductsWithAddAndMult implements TestFunction {
		private static final int numberOfIterations = 5;
		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++) {
				for(int j = 0; j < arguments.length; j++) {
					sum = sum.add(arguments[j].mult(parameters[j]));
				}
			}
			return sum;
		}

		@Override
		public RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface sum = (new RandomVariableFactory()).createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++) {
				{
					int j = 0;
					sum = sum.add(parameters[j]);
				}
			}
			return new RandomVariableInterface[] { sum };
		}
	}

	private static class TestFunctionAccrue implements TestFunction {
		private static final int numberOfIterations = 1;
		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface product = randomVariableFactory.createRandomVariable(1.0);
			for(int i = 0; i < numberOfIterations; i++){
				for(int j = 0; j < arguments.length; j++){
					product = product.accrue(arguments[j], parameters[j].getAverage());
				}
			}
			return product;
		}

		@Override
		public RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			return null;
		}
	}

	private static class TestFunctionAccrueWithAddAndMult implements TestFunction {
		private static final int numberOfIterations = 1;

		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface product = randomVariableFactory.createRandomVariable(1.0);
			for(int i = 0; i < numberOfIterations; i++){
				for(int j = 0; j < arguments.length; j++){
					product = product.mult(arguments[j].mult(parameters[j].getAverage()).add(1.0));
				}
			}
			return product;
		}

		@Override
		public RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			return null;
		}
	}

	private static class TestFunctionMonteCarloAsianOption implements TestFunction {

		// Model properties
		private final double	modelInitialValue   = 1.0;
		private final double	modelRiskFreeRate   = 0.05;
		private final double	modelVolatility     = 0.30;

		// Process discretization properties
		private final int		numberOfPaths		= 500000;
		private final int		numberOfTimeSteps	= 100;
		private final double	deltaT				= 0.02;

		private final int		seed				= 31415;

		// Product properties
		private final int		assetIndex = 0;
		private final double	optionMaturity = 2.0;
		private final double	optionStrike = 1.05;

		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			long startMem = getAllocatedMemory();

			// Generate independent variables (quantities w.r.t. to which we like to differentiate)
			RandomVariableInterface initialValue	= arguments[0];
			RandomVariableInterface riskFreeRate	= arguments[1];
			RandomVariableInterface volatility	= arguments[2];

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
			AsianOption option = new AsianOption(optionMaturity, optionStrike, new TimeDiscretization(0.0, (int)(optionMaturity/deltaT), deltaT));
			RandomVariableInterface value = null;
			try {
				value = option.getValue(0.0, monteCarloBlackScholesModel);
			} catch (CalculationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long endMem = getAllocatedMemory();
			System.out.println("memory requirements.: " + formatReal1.format((endMem-startMem)/1024.0/1024.0) + " MB");
			return value;
		}

		@Override
		public RandomVariableInterface[] derivative(RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			return null;
		}
	}


	private static AbstractRandomVariableFactory[] testMethods = {
			new RandomVariableFactory(),
			new RandomVariableDifferentiableAADPathwiseFactory(),
			new RandomVariableDifferentiableAADStochasticNonOptimizedFactory(),
			new RandomVariableDifferentiableAADFactory()
	};

	private static int numberOfPaths = 10000;		/* In the paper we use 100000 */
	private static Object[][] testCases = {
			{ new TestFunctionBigSum(),
				new Integer(numberOfPaths)	/* number of paths for the arguments */,
				new Integer(1)				/* number of arguments */,
				new Integer(numberOfPaths)	/* number of paths for the parameters */,
				new Integer(0)				/* number of parameters */
			},
			{ new TestFunctionGeometricSum(),
				new Integer(numberOfPaths),
				new Integer(1),
				new Integer(numberOfPaths),
				new Integer(0)
			},
			{ new TestFunctionSumOfProductsWithAddAndMult(),
				new Integer(10*numberOfPaths),
				new Integer(100),
				new Integer(10*numberOfPaths),
				new Integer(100)
			},
			{ new TestFunctionSumOfProducts(),
				new Integer(10*numberOfPaths),
				new Integer(100),
				new Integer(10*numberOfPaths),
				new Integer(100)
			},
			{ new TestFunctionAccrueWithAddAndMult(),
				new Integer(10*numberOfPaths),
				new Integer(100),
				new Integer(1),
				new Integer(100)
			},
			{ new TestFunctionAccrue(),
				new Integer(10*numberOfPaths),
				new Integer(100),
				new Integer(1),
				new Integer(100)
			},
			{ new TestFunctionMonteCarloAsianOption(),
				new Integer(1),
				new Integer(3),
				new Integer(1),
				new Integer(1)
			}
	};


	private AbstractRandomVariableFactory	randomVariableFactory;
	private Object[]						testCase;

	public RandomVariableDifferentiableAADPerformanceTest(Object[] testCase, AbstractRandomVariableFactory testMethod) {
		this.testCase = testCase;
		this.randomVariableFactory = testMethod;
	}

	@Test
	public void test() {
		TestFunction function = (TestFunction)testCase[0];
		int argumentNumberOfPaths = ((Integer)testCase[1]).intValue();
		int numberOfArguments = ((Integer)testCase[2]).intValue();
		int parameterNumberOfPaths = ((Integer)testCase[3]).intValue();
		int numberOfParameters = ((Integer)testCase[4]).intValue();

		/*
		 * Build arguments
		 */
		Random random = new Random(314151);
		double[] valuesArgument = new double[argumentNumberOfPaths];
		for(int i=0; i < argumentNumberOfPaths; i++) {
			valuesArgument[i] = random.nextDouble()/100.0;
		}
		double[] valuesParameter = new double[parameterNumberOfPaths];
		for(int i=0; i < parameterNumberOfPaths; i++) {
			valuesParameter[i] = random.nextDouble();
		}

		RandomVariableInterface[] x = new RandomVariableInterface[numberOfArguments];
		RandomVariableInterface[] c = new RandomVariableInterface[numberOfParameters];
		for(int i=0; i<numberOfArguments; i++) {
			if(valuesArgument.length == 1) {
				x[i] = randomVariableFactory.createRandomVariable(0.0, valuesArgument[0]);
			}
			else {
				x[i] = randomVariableFactory.createRandomVariable(0.0, valuesArgument);
			}
		}
		for(int i=0; i<numberOfParameters; i++) {
			if(valuesParameter.length == 1) {
				c[i] = randomVariableFactory.createRandomVariable(0.0, valuesParameter[0]);
			}
			else {
				c[i] = randomVariableFactory.createRandomVariable(0.0, valuesParameter);
			}
		}

		/*
		 * Test valuation
		 */
		long startMemCalculation = getAllocatedMemory();
		long startCalculation = System.currentTimeMillis();

		RandomVariableInterface y = function.value(randomVariableFactory, x, c);

		long endCalculation = System.currentTimeMillis();

		/*
		 * Test derivative
		 */

		long startAutoDiff = System.currentTimeMillis();

		RandomVariableInterface[] dydx = new RandomVariableInterface[x.length];
		if(y instanceof RandomVariableDifferentiableInterface) {
			System.out.print("AAD - ");
			Map<Long, RandomVariableInterface> gradient = ((RandomVariableDifferentiableInterface)y).getGradient();
			for(int i=0; i<dydx.length; i++) {
				dydx[i] = gradient.get(((RandomVariableDifferentiableInterface)x[i]).getID());
			}
		}
		else {
			System.out.print("FD  - ");
			double epsilon = 1E-6;
			RandomVariableInterface[] xUp = x.clone();
			RandomVariableInterface[] xDn = x.clone();
			for(int i=0; i<dydx.length; i++) {
				xUp[i] = x[i].add(epsilon);
				xDn[i] = x[i].sub(epsilon);
				RandomVariableInterface yUp = function.value(randomVariableFactory, xUp, c);
				RandomVariableInterface yDn = function.value(randomVariableFactory, xDn, c);
				dydx[i] = yUp.sub(yDn).div(2 * epsilon);
			}
		}

		long endAutoDiff = System.currentTimeMillis();
		long endMemAutoDiff = getAllocatedMemory();

		if(y.isNaN().getAverage() > 0) System.out.println("error");
		if(dydx[0].isNaN().getAverage() > 0) System.out.println("error");

		RandomVariableInterface[] dydxAnalytic = function.derivative(x, c);

		System.out.print(function.getClass().getSimpleName() + " - ");
		System.out.println(randomVariableFactory.getClass().getSimpleName() + ":");
		System.out.println("evaluation..........: " + formatReal1.format((endCalculation-startCalculation)/1000.0) + " s");
		System.out.println("derivative..........: " + formatReal1.format((endAutoDiff-startAutoDiff)/1000.0) + " s");
		System.out.println("memory requirements.: " + formatReal1.format((endMemAutoDiff-startMemCalculation)/1024.0/1024.0) + " MB");

		System.out.print("dy/dx = (");
		for(RandomVariableInterface partialDerivative : dydx) System.out.print(formatReal1.format(partialDerivative.getAverage()) + ",");
		System.out.println(")");
		System.out.println("");

		if(dydxAnalytic != null) Assert.assertEquals(0.0, dydxAnalytic[0].sub(dydx[0]).getStandardError(), 1E-4);
	}

	static long getAllocatedMemory() {
		System.gc();
		long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		return allocatedMemory;
	}
}