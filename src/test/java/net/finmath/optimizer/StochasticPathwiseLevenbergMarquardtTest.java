/**
 *
 */
package net.finmath.optimizer;

import java.util.Arrays;

import org.junit.Test;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 */
public class StochasticPathwiseLevenbergMarquardtTest {

	@Test
	public void test() throws SolverException {
		final int numberOfParameters = 2;

		RandomVariable[] initialParameters = new RandomVariable[numberOfParameters];
		RandomVariable[] parameterSteps = new RandomVariable[numberOfParameters];
		// The following two lines decides if we use AAD or FD
		//		Arrays.fill(initialParameters, new RandomVariableDifferentiableAAD(2.0));
		Arrays.fill(initialParameters, new RandomVariableFromDoubleArray(2.0));
		Arrays.fill(parameterSteps, new RandomVariableFromDoubleArray(1E-8));

		RandomVariable[] weights		= new RandomVariable[] { new RandomVariableFromDoubleArray(1.0) };
		RandomVariable[] targetValues	= new RandomVariable[] { new RandomVariableFromDoubleArray(0) };

		int maxIteration = 1000;

		StochasticPathwiseLevenbergMarquardt optimizer = new StochasticPathwiseLevenbergMarquardt(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(RandomVariable[] parameters, RandomVariable[] values) {
				values[0] = parameters[0].add(parameters[1].mult(2)).sub(7).pow(2).add(parameters[0].mult(2).add(parameters[1]).sub(5).pow(2));
			}
		};

		// Set solver parameters

		optimizer.run();

		RandomVariable[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError());
	}

	@Test
	public void testBoothFunctionWithAnalyticDerivative() throws SolverException {
		final int numberOfParameters = 2;

		RandomVariable[] initialParameters = new RandomVariable[numberOfParameters];
		RandomVariable[] parameterSteps = new RandomVariable[numberOfParameters];
		//		RandomVariable[] parameterSteps = null;
		// The following two lines decides if we use AAD or FD
		//		Arrays.fill(initialParameters, new RandomVariableDifferentiableAAD(2.0));
		Arrays.fill(initialParameters, new RandomVariableFromDoubleArray(2.0));
		Arrays.fill(parameterSteps, new RandomVariableFromDoubleArray(.000001));

		RandomVariable[] weights		= new RandomVariable[] { new RandomVariableFromDoubleArray(1.0) };
		RandomVariable[] targetValues	= new RandomVariable[] { new RandomVariableFromDoubleArray(0) };

		int maxIteration = 1000;

		StochasticPathwiseLevenbergMarquardt optimizer = new StochasticPathwiseLevenbergMarquardt(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(RandomVariable[] parameters, RandomVariable[] values) {
				values[0] = parameters[0].add(parameters[1].mult(2)).sub(7).squared().add(parameters[0].mult(2).add(parameters[1]).sub(5).squared());
			}

			@Override
			public void setDerivatives(RandomVariable[] parameters, RandomVariable[][] derivatives) {
				derivatives[0][0] = parameters[0].add(parameters[1].mult(2)).sub(7).mult(2).add(parameters[0].mult(2).add(parameters[1]).sub(5).mult(4));
				derivatives[1][0] = parameters[0].add(parameters[1].mult(2)).sub(7).mult(4).add(parameters[0].mult(2).add(parameters[1]).sub(5).mult(2));
			}
		};

		// Set solver parameters

		optimizer.run();

		RandomVariable[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError());
	}
}
