/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
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
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AsianOption;
import net.finmath.montecarlo.assetderivativevaluation.products.BermudanOption;
import net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This unit test checks automatic differentiation of some test functions and
 * compares it with a finite difference. The assert statement checks the values
 * against an analytic value (if the test function provides this).
 *
 * The values used in this test are lower than the test cases presented in the paper
 * https://ssrn.com/abstract=2995695, but apart from this we
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 */
@RunWith(Parameterized.class)
public class RandomVariableDifferentiableAADPerformanceTest {
	private static DecimalFormat formatReal1 = new DecimalFormat("####0.00", new DecimalFormatSymbols(Locale.ENGLISH));

	public static void main(String[] args) {
		for(Object[] testData : data()) {
			(new RandomVariableDifferentiableAADPerformanceTest((String) testData[0], (Object[]) testData[1], (AbstractRandomVariableFactory)testData[2])).test();
		}
	}

	/**
	 * Interface definition for test functions. Different test functions are defined below.
	 *
	 * @author Christian Fries
	 */
	private interface TestFunction {
		RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters);
		RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters);

		long getPeakMemory();
	}

	private static class TestFunctionBigSum implements TestFunction {
		private static final int numberOfIterations = 7500;

		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable x = arguments[0];
			RandomVariable sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = sum.add(x);
			}
			return sum;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			return new RandomVariable[] {
					(new RandomVariableFactory()).createRandomVariable(numberOfIterations)
			};
		}

		@Override
		public long getPeakMemory() { return 0; }
	}

	private static class TestFunctionGeometricSum implements TestFunction {
		private static final int numberOfIterations = 1000;	/* In the paper we use 5000 */

		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable x = arguments[0];
			RandomVariable sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = sum.add(x.pow(i));
			}
			return sum;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable x = arguments[0];
			RandomVariable sum = (new RandomVariableFactory()).createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = sum.add(x.pow(i-1).mult(i));
			}
			return new RandomVariable[] { sum };
		}

		@Override
		public long getPeakMemory() { return 0; }
	}

	private static class TestFunctionSumOfProducts implements TestFunction {
		private static final int numberOfIterations = 5;

		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++) {
				for(int j = 0; j < arguments.length; j++) {
					sum = sum.addProduct(arguments[j],parameters[j]);
				}
			}
			return sum;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable sum = (new RandomVariableFactory()).createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				{
					int j = 0;
					sum = sum.add(parameters[j]);
				}
			}
			return new RandomVariable[] { sum };
		}

		@Override
		public long getPeakMemory() { return 0; }
	}

	private static class TestFunctionSumOfProductsWithAddAndMult implements TestFunction {
		private static final int numberOfIterations = 10;
		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++) {
				for(int j = 0; j < arguments.length; j++) {
					sum = sum.add(arguments[j].mult(parameters[j]));
				}
			}
			return sum;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable sum = (new RandomVariableFactory()).createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++) {
				{
					int j = 0;
					sum = sum.add(parameters[j]);
				}
			}
			return new RandomVariable[] { sum };
		}

		@Override
		public long getPeakMemory() { return 0; }
	}

	private static class TestFunctionAccrue implements TestFunction {
		private static final int numberOfIterations = 1;
		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable product = randomVariableFactory.createRandomVariable(1.0);
			for(int i = 0; i < numberOfIterations; i++){
				for(int j = 0; j < arguments.length; j++){
					product = product.accrue(arguments[j], parameters[j].getAverage());
				}
			}
			return product;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			return null;
		}

		@Override
		public long getPeakMemory() { return 0; }
	}

	private static class TestFunctionAccrueWithAddAndMult implements TestFunction {
		private static final int numberOfIterations = 1;

		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			RandomVariable product = randomVariableFactory.createRandomVariable(1.0);
			for(int i = 0; i < numberOfIterations; i++){
				for(int j = 0; j < arguments.length; j++){
					product = product.mult(arguments[j].mult(parameters[j].getAverage()).add(1.0));
				}
			}
			return product;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			return null;
		}

		@Override
		public long getPeakMemory() { return 0; }
	}

	private static class TestFunctionMonteCarloEuropeanOption implements TestFunction {

		// Model properties
		private final double	modelInitialValue   = 1.0;
		private final double	modelRiskFreeRate   = 0.05;
		private final double	modelVolatility     = 0.30;

		// Process discretization properties
		private final int		numberOfPaths		= 250000;
		private final int		numberOfTimeSteps	= 201;
		private final double	deltaT				= 0.25;	//  quaterly, 50 year

		private final int		seed				= 31415;

		// Product properties
		private final int		assetIndex = 0;
		private final double	optionMaturity = 50.0;
		private final double	optionStrike = 1.10;

		private AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel;

		private long peakMemory;

		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			long startMem = getAllocatedMemory();

			// Generate independent variables (quantities w.r.t. to which we like to differentiate)
			RandomVariable initialValue	= arguments[0].mult(0).add(modelInitialValue);
			RandomVariable riskFreeRate	= arguments[1].mult(0).add(modelRiskFreeRate);
			RandomVariable volatility		= arguments[2].mult(0).add(modelVolatility);

			// Create a model
			AbstractProcessModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

			// Create a time discretization
			TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

			// Create a corresponding MC process
			MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(new BrownianMotionLazyInit(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

			// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
			monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

			/*
			 * Value a call option (using the product implementation)
			 */
			EuropeanOption option = new EuropeanOption(optionMaturity, optionStrike);
			RandomVariable value = null;
			try {
				value = option.getValue(0.0, monteCarloBlackScholesModel);
			} catch (CalculationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			long endMem = getAllocatedMemory();
			peakMemory = Math.max(peakMemory, endMem-startMem);

			return value;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			return null;
		}

		@Override
		public long getPeakMemory() { return peakMemory; }
	}

	private static class TestFunctionMonteCarloAsianOption implements TestFunction {

		// Model properties
		private final double	modelInitialValue   = 1.0;
		private final double	modelRiskFreeRate   = 0.05;
		private final double	modelVolatility     = 0.30;

		// Process discretization properties
		private final int		numberOfPaths		= 250000;
		private final int		numberOfTimeSteps	= 201;
		private final double	deltaT				= 0.25;	//  quaterly, 50 year

		private final int		seed				= 31415;

		// Product properties
		private final int		assetIndex = 0;
		private final double	optionMaturity = 50.0;
		private final double	optionStrike = 1.05;

		private AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel;

		private long peakMemory;

		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			long startMem = getAllocatedMemory();

			// Generate independent variables (quantities w.r.t. to which we like to differentiate)
			RandomVariable initialValue	= arguments[0];
			RandomVariable riskFreeRate	= arguments[1];
			RandomVariable volatility		= arguments[2];

			// Create a model
			AbstractProcessModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility, randomVariableFactory);

			// Create a time discretization
			TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

			// Create a corresponding MC process
			MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(new BrownianMotionLazyInit(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

			// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
			monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

			/*
			 * Value a call option (using the product implementation)
			 */
			AsianOption option = new AsianOption(optionMaturity, optionStrike, new TimeDiscretizationFromArray(0.0, (int)(optionMaturity/deltaT), deltaT));
			RandomVariable value = null;
			try {
				value = option.getValue(0.0, monteCarloBlackScholesModel);
			} catch (CalculationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			long endMem = getAllocatedMemory();
			peakMemory = Math.max(peakMemory, endMem-startMem);

			return value;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			return null;
		}

		@Override
		public long getPeakMemory() { return peakMemory; }
	}

	private static class TestFunctionMonteCarloBermudanOption implements TestFunction {

		// Model properties
		private final double	modelInitialValue   = 1.0;
		private final double	modelRiskFreeRate   = 0.05;
		private final double	modelVolatility     = 0.30;

		// Process discretization properties
		private final int		numberOfPaths		= 100000; // you may try: 250000;
		private final int		numberOfTimeSteps	= 201;
		private final double	deltaT				= 0.25;	//  quaterly, 50 year

		private final int		seed				= 31415;

		private AssetModelMonteCarloSimulationModel monteCarloBlackScholesModel;

		private long peakMemory;

		@Override
		public RandomVariable value(AbstractRandomVariableFactory randomVariableFactory, RandomVariable[] arguments, RandomVariable[] parameters) {
			long startMem = getAllocatedMemory();

			// Generate independent variables (quantities w.r.t. to which we like to differentiate)
			RandomVariable initialValue	= arguments[0];
			RandomVariable riskFreeRate	= arguments[1];
			RandomVariable volatility		= arguments[2];

			// Create a model
			AbstractProcessModel model = new BlackScholesModel(initialValue.mult(0.0).add(modelInitialValue), riskFreeRate.mult(0.0).add(modelRiskFreeRate), volatility.mult(0.0).add(modelVolatility), randomVariableFactory);

			// Create a time discretization
			TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0 /* initial */, numberOfTimeSteps, deltaT);

			// Create a corresponding MC process
			MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(new BrownianMotionLazyInit(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

			// Using the process (Euler scheme), create an MC simulation of a Black-Scholes model
			monteCarloBlackScholesModel = new MonteCarloAssetModel(model, process);

			/*
			 * Value a call option (using the product implementation)
			 */
			double[] exerciseDate = new double[50];
			double[] notionals = new double[50];
			double[] strikes = new double[50];
			for(int periodIndex = 1; periodIndex<=50; periodIndex++) {
				exerciseDate[periodIndex-1] = (double)periodIndex;
				notionals[periodIndex-1] = 1.0;
				strikes[periodIndex-1] = 1.0 * Math.exp(modelRiskFreeRate*exerciseDate[periodIndex-1]);
			}
			BermudanOption option = new BermudanOption(exerciseDate, notionals, strikes);
			RandomVariable value = null;
			try {
				value = option.getValue(0.0, monteCarloBlackScholesModel);
			} catch (CalculationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			long endMem = getAllocatedMemory();
			peakMemory = Math.max(peakMemory, endMem-startMem);
			return value;
		}

		@Override
		public RandomVariable[] derivative(RandomVariable[] arguments, RandomVariable[] parameters) {
			return null;
		}

		@Override
		public long getPeakMemory() { return peakMemory; }
	}

	private static AbstractRandomVariableFactory[] testMethods = {
			new RandomVariableFactory(),
			//			new RandomVariableDifferentiableAADPathwiseFactory(),
			//			new RandomVariableDifferentiableAADStochasticNonOptimizedFactory(),
			new RandomVariableDifferentiableAADFactory()
	};

	private static int numberOfPaths = 10000;//10000;		/* In the paper we use 100000 */
	private static Object[][] testCases = {
			{ new TestFunctionBigSum(),
				new Integer(numberOfPaths),	// number of paths for the arguments
				new Integer(1),				// number of arguments
				new Integer(numberOfPaths),	// number of paths for the parameters
				new Integer(0)				// number of parameters
			},
			{ new TestFunctionGeometricSum(),
				new Integer(numberOfPaths),
				new Integer(1),
				new Integer(numberOfPaths),
				new Integer(0)
			},
			{ new TestFunctionSumOfProductsWithAddAndMult(),
				new Integer(numberOfPaths),		/* In the paper we use 10*100000 */
				new Integer(10),	/* In the paper we use 100 */
				new Integer(numberOfPaths),
				new Integer(10)		/* In the paper we use 100 */
			},
			{ new TestFunctionSumOfProducts(),
				new Integer(numberOfPaths),		/* In the paper we use 10*100000 */
				new Integer(100),
				new Integer(numberOfPaths),		/* In the paper we use 10*100000 */
				new Integer(100)
			},
			{ new TestFunctionAccrueWithAddAndMult(),
				new Integer(numberOfPaths),		/* In the paper we use 10*100000 */
				new Integer(100),
				new Integer(1),
				new Integer(100)
			},
			{ new TestFunctionAccrue(),
				new Integer(numberOfPaths),		/* In the paper we use 10*100000 */
				new Integer(100),
				new Integer(1),
				new Integer(100)
			},
			{ new TestFunctionMonteCarloEuropeanOption(),
				new Integer(1),
				new Integer(3),
				new Integer(1),
				new Integer(1)
			},
			{ new TestFunctionMonteCarloAsianOption(),
				new Integer(1),
				new Integer(3),
				new Integer(1),
				new Integer(1)
			},
			{ new TestFunctionMonteCarloBermudanOption(),
				new Integer(1),
				new Integer(3),
				new Integer(1),
				new Integer(1)
			}
	};



	@Parameters(name="{0}")
	public static Collection<Object[]> data() {
		Collection<Object[]> testParameters = new ArrayList<>();
		for(int i=0; i<testCases.length; i++) {
			for(int j=0; j<testMethods.length; j++) {
				Object[] testParmeter = new Object[3];
				testParmeter[0] = testMethods[j].getClass().getSimpleName() + " "
						+ testCases[i][0].getClass().getSimpleName() + "("
						+ testCases[i][1] + ","
						+ testCases[i][2] + ","
						+ testCases[i][3] + ","
						+ testCases[i][4] + ")"
						;
				testParmeter[1] = testCases[i];
				testParmeter[2] = testMethods[j];
				testParameters.add(testParmeter);
			}
		}
		return testParameters;
	}

	private String							name;
	private AbstractRandomVariableFactory	randomVariableFactory;
	private Object[]						testCase;

	public RandomVariableDifferentiableAADPerformanceTest(String name, Object[] testCase, AbstractRandomVariableFactory testMethod) {
		this.testCase = testCase;
		randomVariableFactory = testMethod;
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

		RandomVariable[] x = new RandomVariable[numberOfArguments];
		RandomVariable[] c = new RandomVariable[numberOfParameters];
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

		System.gc();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*
		 * Test valuation
		 */
		long startMemCalculation = getAllocatedMemory();
		long startCalculation = System.currentTimeMillis();

		RandomVariable y = function.value(randomVariableFactory, x, c);

		long endCalculation = System.currentTimeMillis();

		/*
		 * Test derivative
		 */

		long startAutoDiff = System.currentTimeMillis();

		RandomVariable[] dydx = new RandomVariable[x.length];
		if(y instanceof RandomVariableDifferentiable) {
			System.out.print("AAD - ");
			Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)y).getGradient();
			for(int i=0; i<dydx.length; i++) {
				dydx[i] = gradient.get(((RandomVariableDifferentiable)x[i]).getID());
			}
		}
		else {
			System.out.print("FD  - ");
			double epsilon = 1E-6;
			RandomVariable[] xUp = x.clone();
			RandomVariable[] xDn = x.clone();
			for(int i=0; i<dydx.length; i++) {
				xUp[i] = x[i].add(epsilon);
				xDn[i] = x[i].sub(epsilon);
				RandomVariable yUp = function.value(randomVariableFactory, xUp, c);
				RandomVariable yDn = function.value(randomVariableFactory, xDn, c);
				dydx[i] = yUp.sub(yDn).div(2 * epsilon);
			}
		}

		long endAutoDiff = System.currentTimeMillis();
		long endMemAutoDiff = getAllocatedMemory();

		if(y.isNaN().getAverage() > 0) {
			System.out.println("error");
		}
		if(dydx[0].isNaN().getAverage() > 0) {
			System.out.println("error");
		}

		RandomVariable[] dydxAnalytic = function.derivative(x, c);

		System.out.print(function.getClass().getSimpleName() + " - ");
		System.out.println(randomVariableFactory.getClass().getSimpleName() + ":");
		System.out.println("evaluation..........: " + formatReal1.format((endCalculation-startCalculation)/1000.0) + " s");
		System.out.println("derivative..........: " + formatReal1.format((endAutoDiff-startAutoDiff)/1000.0) + " s");
		System.out.println("memory requirements.: " + formatReal1.format((endMemAutoDiff-startMemCalculation)/1024.0/1024.0) + " MB");
		System.out.println("memory requirements.: " + formatReal1.format((function.getPeakMemory())/1024.0/1024.0) + " MB");

		System.out.println();

		if(dydxAnalytic != null) {
			Assert.assertEquals(0.0, dydxAnalytic[0].sub(dydx[0]).getStandardError(), 1E-4);
		}
	}

	static long getAllocatedMemory() {
		System.gc();
		System.gc();
		System.gc();
		long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		return allocatedMemory;
	}
}
