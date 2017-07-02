/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 01.07.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 * @author Christian Fries
 *
 */
import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADPathwiseFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADStochasticNonOptimizedFactory;
import net.finmath.stochastic.RandomVariableInterface;

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

	private static DecimalFormat formatReal2 = new DecimalFormat("####0.00", new DecimalFormatSymbols(Locale.ENGLISH));

	private interface TestFunction {
		RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters);
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
	}

	private static class TestFunctionSumOfProducts implements TestFunction {
		private static final int numberOfIterations = 5;
		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				for(int j = 0; j < arguments.length; j++){
					sum = sum.addProduct(arguments[j],parameters[j]);
				}
			}
			return sum;
		}
	}

	private static class TestFunctionSumOfProductsWithAddAndMult implements TestFunction {
		private static final int numberOfIterations = 5;
		public RandomVariableInterface value(AbstractRandomVariableFactory randomVariableFactory, RandomVariableInterface[] arguments, RandomVariableInterface[] parameters) {
			RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				for(int j = 0; j < arguments.length; j++){
					sum = sum.add(arguments[j].mult(parameters[j]));
				}
			}
			return sum;
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
	}

	private static AbstractRandomVariableFactory[] testMethods = {
//			new RandomVariableFactory(),
//			new RandomVariableDifferentiableAADPathwiseFactory(),
			new RandomVariableDifferentiableAADStochasticNonOptimizedFactory(),
			new RandomVariableDifferentiableAADFactory()
	};

	private static Object[][] testCases = {
//			{ new TestFunctionBigSum(), new Integer(100000), new Integer(1), new Integer(100000), new Integer(0) },
//			{ new TestFunctionGeometricSum(), new Integer(100000), new Integer(1), new Integer(100000), new Integer(0) },
//			{ new TestFunctionSumOfProductsWithAddAndMult(), new Integer(1000000), new Integer(100), new Integer(1000000), new Integer(100) },
//			{ new TestFunctionSumOfProducts(), new Integer(1000000), new Integer(100), new Integer(1000000), new Integer(100) },
//			{ new TestFunctionAccrueWithAddAndMult(), new Integer(1000000), new Integer(100), new Integer(1), new Integer(100) },
			{ new TestFunctionAccrue(), new Integer(1000000), new Integer(100), new Integer(1), new Integer(100) }
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

		System.out.print(function.getClass().getSimpleName() + " - ");
		System.out.println(randomVariableFactory.getClass().getSimpleName() + ":");
		System.out.println("evaluation..........: " + formatReal2.format((endCalculation-startCalculation)/1000.0) + " s");
		System.out.println("derivative..........: " + formatReal2.format((endAutoDiff-startAutoDiff)/1000.0) + " s");
		System.out.println("memory requirements.: " + formatReal2.format((endMemAutoDiff-startMemCalculation)/1024.0/1024.0) + " MB");
		System.out.print("dy/dx = (");
		for(RandomVariableInterface partialDerivative : dydx) System.out.print(formatReal2.format(partialDerivative.getAverage()) + ",");
		System.out.println(")");
		System.out.println("");
	}

	static long getAllocatedMemory() {
		System.gc();
		long allocatedMemory = (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());
		return allocatedMemory;
	}
}