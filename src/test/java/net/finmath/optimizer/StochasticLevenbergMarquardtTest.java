/**
 *
 */
package net.finmath.optimizer;

import java.util.Arrays;

import org.junit.Test;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAAD;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 */
public class StochasticLevenbergMarquardtTest {

	@Test
	public void test() throws SolverException {
		final int numberOfParameters = 2;

		final RandomVariable[] initialParameters = new RandomVariable[numberOfParameters];
		final RandomVariable[] parameterSteps = new RandomVariable[numberOfParameters];
		// The following two lines decides if we use AAD or FD
		Arrays.fill(initialParameters, new RandomVariableDifferentiableAAD(2.0));
		//		Arrays.fill(initialParameters, new RandomVariableFromDoubleArray(2.0));
		Arrays.fill(parameterSteps, new RandomVariableFromDoubleArray(1E-8));

		final RandomVariable[] weights		= new RandomVariable[] { new RandomVariableFromDoubleArray(1.0) };
		final RandomVariable[] targetValues	= new RandomVariable[] { new RandomVariableFromDoubleArray(0) };

		final int maxIteration = 1000;

		final StochasticPathwiseLevenbergMarquardtAD optimizer = new StochasticPathwiseLevenbergMarquardtAD(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) {
				values[0] = parameters[0].add(parameters[1].mult(2)).sub(7).pow(2).add(parameters[0].mult(2).add(parameters[1]).sub(5).pow(2));
			}
		};

		// Set solver parameters

		optimizer.run();

		final RandomVariable[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError());
	}

	@Test
	public void testBoothFunctionWithAnalyticDerivative() throws SolverException {
		final int numberOfParameters = 2;

		final RandomVariable[] initialParameters = new RandomVariable[numberOfParameters];
		final RandomVariable[] parameterSteps = new RandomVariable[numberOfParameters];
		//		RandomVariable[] parameterSteps = null;
		// The following two lines decides if we use AAD or FD
		//		Arrays.fill(initialParameters, new RandomVariableDifferentiableAAD(2.0));
		Arrays.fill(initialParameters, new RandomVariableFromDoubleArray(2.0));
		Arrays.fill(parameterSteps, new RandomVariableFromDoubleArray(.000001));

		final RandomVariable[] weights		= new RandomVariable[] { new RandomVariableFromDoubleArray(1.0) };
		final RandomVariable[] targetValues	= new RandomVariable[] { new RandomVariableFromDoubleArray(0) };

		final int maxIteration = 1000;

		final StochasticPathwiseLevenbergMarquardt optimizer = new StochasticPathwiseLevenbergMarquardt(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) {
				values[0] = parameters[0].add(parameters[1].mult(2)).sub(7).squared().add(parameters[0].mult(2).add(parameters[1]).sub(5).squared());
			}

			@Override
			public void setDerivatives(final RandomVariable[] parameters, final RandomVariable[][] derivatives) {
				derivatives[0][0] = parameters[0].add(parameters[1].mult(2)).sub(7).mult(2).add(parameters[0].mult(2).add(parameters[1]).sub(5).mult(4));
				derivatives[1][0] = parameters[0].add(parameters[1].mult(2)).sub(7).mult(4).add(parameters[0].mult(2).add(parameters[1]).sub(5).mult(2));
			}
		};

		// Set solver parameters

		optimizer.run();

		final RandomVariable[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError());
	}
}
