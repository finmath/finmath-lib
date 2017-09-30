/**
 * 
 */
package net.finmath.optimizer;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.function.Function;

import org.junit.Test;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public class StochasticLevenbergMarquardtTest {

	@Test
	public void test() throws SolverException {
		final int numberOfParameters = 2;

		RandomVariableInterface[] initialParameters = new RandomVariableInterface[numberOfParameters];
		RandomVariableInterface[] parameterSteps = new RandomVariableInterface[numberOfParameters];
		// The following two lines decides if we use AAD or FD
//		Arrays.fill(initialParameters, new RandomVariableDifferentiableAAD(2.0));
		Arrays.fill(initialParameters, new RandomVariable(2.0));
		Arrays.fill(parameterSteps, new RandomVariable(1E-8));
		
		RandomVariableInterface[] weights		= new RandomVariableInterface[] { new RandomVariable(1.0) };
		RandomVariableInterface[] targetValues	= new RandomVariableInterface[] { new RandomVariable(0) };

		int maxIteration = 1000;

		StochasticLevenbergMarquardt optimizer = new StochasticLevenbergMarquardt(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) {
				values[0] = parameters[0].add(parameters[1].mult(2)).sub(7).pow(2).add(parameters[0].mult(2).add(parameters[1]).sub(5).pow(2));
			}
		};

		// Set solver parameters

		optimizer.run();

		RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError().getAverage());
	}

	@Test
	public void testBoothFunctionWithAnalyticDerivative() throws SolverException {
		final int numberOfParameters = 2;

		RandomVariableInterface[] initialParameters = new RandomVariableInterface[numberOfParameters];
		RandomVariableInterface[] parameterSteps = new RandomVariableInterface[numberOfParameters];
//		RandomVariableInterface[] parameterSteps = null;
		// The following two lines decides if we use AAD or FD
//		Arrays.fill(initialParameters, new RandomVariableDifferentiableAAD(2.0));
		Arrays.fill(initialParameters, new RandomVariable(2.0));
		Arrays.fill(parameterSteps, new RandomVariable(.000001));
		
		RandomVariableInterface[] weights		= new RandomVariableInterface[] { new RandomVariable(1.0) };
		RandomVariableInterface[] targetValues	= new RandomVariableInterface[] { new RandomVariable(0) };

		int maxIteration = 1000;

		StochasticLevenbergMarquardt optimizer = new StochasticLevenbergMarquardt(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) {
				values[0] = parameters[0].add(parameters[1].mult(2)).sub(7).squared().add(parameters[0].mult(2).add(parameters[1]).sub(5).squared());
			}

			@Override
			public void setDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[][] derivatives) {
				derivatives[0][0] = parameters[0].add(parameters[1].mult(2)).sub(7).mult(2).add(parameters[0].mult(2).add(parameters[1]).sub(5).mult(4));
				derivatives[1][0] = parameters[0].add(parameters[1].mult(2)).sub(7).mult(4).add(parameters[0].mult(2).add(parameters[1]).sub(5).mult(2));
			}
		};

		// Set solver parameters

		optimizer.run();

		RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError().getAverage());
	}
}
