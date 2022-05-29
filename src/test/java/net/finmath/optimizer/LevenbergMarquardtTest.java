/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.07.2014
 */

package net.finmath.optimizer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.LogManager;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the LevenbergMarquardt optimizer.
 *
 * @author Christian Fries
 */
public class LevenbergMarquardtTest {

	@Test
	public void testSmallLinearSystem() throws CloneNotSupportedException, SolverException {
		final LevenbergMarquardt optimizer = new LevenbergMarquardt() {
			private static final long serialVersionUID = -6582160713209444489L;

			@Override
			public void setValues(final double[] parameters, final double[] values) {
				values[0] = parameters[0] * 0.0 + parameters[1];
				values[1] = parameters[0] * 2.0 + parameters[1];
			}
		};

		// Set solver parameters
		optimizer.setInitialParameters(new double[] { 0, 0 });
		optimizer.setWeights(new double[] { 1, 1 });
		optimizer.setMaxIteration(100);
		optimizer.setTargetValues(new double[] { 5, 10 });

		optimizer.run();

		final double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}

		System.out.println();

		Assert.assertTrue(Math.abs(bestParameters[0] - 2.5) < 1E-12);
		Assert.assertTrue(Math.abs(bestParameters[1] - 5.0) < 1E-12);

		/*
		 * Creating a clone, continuing the search with new target values.
		 * Note that we do not re-define the setValues method.
		 */
		final Optimizer optimizer2 = optimizer.getCloneWithModifiedTargetValues(new double[] { 5.1, 10.2 }, new double[] { 1, 1 }, true);
		optimizer2.run();

		final double[] bestParameters2 = optimizer2.getBestFitParameters();
		System.out.println("The solver for problem 2 required " + optimizer2.getIterations() + " iterations. Accuracy is " + optimizer2.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters2.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters2[i]);
		}

		System.out.println("________________________________________________________________________________");
		System.out.println();

		Assert.assertTrue(Math.abs(bestParameters2[0] - 2.55) < 1E-12);
		Assert.assertTrue(Math.abs(bestParameters2[1] - 5.10) < 1E-12);
	}

	@Test
	public void testMultiThreaddedOptimizer() throws SolverException {
		final LevenbergMarquardt optimizer = new LevenbergMarquardt(
				new double[] { 0,  0, 0 },		// Initial parameters
				new double[] { 5, 10, 2 }, 	// Target values
				100,						// Max iterations
				10							// Number of threads
				) {
			private static final long serialVersionUID = -4656732051928259036L;

			// Override your objective function here
			@Override
			public void setValues(final double[] parameters, final double[] values) {
				values[0] = 1.0 * parameters[0] + 2.0 * parameters[1] + parameters[2] + parameters[0] * parameters[1];
				values[1] = 2.0 * parameters[0] + 1.0 * parameters[1] + parameters[2] + parameters[1] * parameters[2];
				values[2] = 3.0 * parameters[0] + 0.0 * parameters[1] + parameters[2];
			}
		};

		optimizer.run();

		final double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 3 required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}

		final double[] values = new double[3];
		optimizer.setValues(bestParameters, values);
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tvalue[" + i + "]: " + values[i]);
		}

		System.out.println("________________________________________________________________________________");
		System.out.println();

		Assert.assertTrue(optimizer.getRootMeanSquaredError() < 1E-1);
	}

	@Test
	public void testRosenbrockFunction() throws SolverException {
		final LevenbergMarquardt optimizer = new LevenbergMarquardt(
				new double[] { 0.5, 0.5 },		// Initial parameters
				new double[] { 0.0, 0.0 }, 		// Target values
				100,							// Max iterations
				10								// Number of threads
				) {
			private static final long serialVersionUID = 1636120150299382088L;

			// Override your objective function here
			@Override
			public void setValues(final double[] parameters, final double[] values) {
				values[0] = 10.0 * (parameters[1] - parameters[0]*parameters[0]);
				values[1] = 1.0 - parameters[0];
			}
		};

		optimizer.run();

		final double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 'Rosebrock' required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}

		final double[] values = new double[2];
		optimizer.setValues(bestParameters, values);
		for (int i = 0; i < values.length; i++) {
			System.out.println("\tvalue[" + i + "]: " + values[i]);
		}

		System.out.println("________________________________________________________________________________");
		System.out.println();

		Assert.assertTrue(Math.abs(bestParameters[0] - 1.0) < 1E-10);
		Assert.assertTrue(Math.abs(bestParameters[1] - 1.0) < 1E-10);
	}

	@Test
	public void testRosenbrockFunctionWithList() throws SolverException {
		final ArrayList<Number> initialParams = new ArrayList<>();
		initialParams.add(0.5);
		initialParams.add(0.5);

		final ArrayList<Number> targetValues = new ArrayList<>();
		targetValues.add(0.0);
		targetValues.add(0.0);

		final LevenbergMarquardt optimizer = new LevenbergMarquardt(
				initialParams,					// Initial parameters
				targetValues, 					// Target values
				100,							// Max iterations
				10								// Number of threads
				) {
			private static final long serialVersionUID = 5999706680609011046L;

			// Override your objective function here
			@Override
			public void setValues(final double[] parameters, final double[] values) {
				values[0] = 10.0 * (parameters[1] - parameters[0]*parameters[0]);
				values[1] = 1.0 - parameters[0];
			}
		};

		optimizer.run();

		final double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 'Rosebrock' required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}

		final double[] values = new double[2];
		optimizer.setValues(bestParameters, values);
		for (int i = 0; i < values.length; i++) {
			System.out.println("\tvalue[" + i + "]: " + values[i]);
		}

		System.out.println("________________________________________________________________________________");
		System.out.println();

		Assert.assertTrue(Math.abs(bestParameters[0] - 1.0) < 1E-10);
		Assert.assertTrue(Math.abs(bestParameters[1] - 1.0) < 1E-10);
	}

	/**
	 * Optimization of booth function \( f(x,y) = \left(x+2y-7\right)^{2}+\left(2x+y-5\right)^{2} \).
	 * The solution of \( f(x,y) = 0 \) is \( x=1 \), \( y=3 \).
	 *
	 * The test uses a finite difference approximation for the derivative.
	 *
	 * @throws SolverException Thrown if the solver fails to find a solution.
	 */
	@Test
	public void testBoothFunction() throws SolverException {
		final int numberOfParameters = 2;

		final double[] initialParameters = new double[numberOfParameters];
		final double[] parameterSteps = new double[numberOfParameters];
		Arrays.fill(initialParameters, 2.0);
		Arrays.fill(parameterSteps, 1E-8);

		final double[] targetValues	= new double[] { 0.0 };

		final int maxIteration = 1000;

		final LevenbergMarquardt optimizer = new LevenbergMarquardt(
				initialParameters,
				targetValues,
				maxIteration, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(final double[] parameters, final double[] values) {
				values[0] = Math.pow(parameters[0] + 2* parameters[1] - 7,2) + Math.pow(2 * parameters[0] + parameters[1] - 5,2);
			}
		};
		optimizer.setParameterSteps(parameterSteps);

		// Set solver parameters

		optimizer.run();

		final double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for Booth's function required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError());

		System.out.println("________________________________________________________________________________");
		System.out.println();

		Assert.assertEquals(0.0, optimizer.getRootMeanSquaredError(), 2E-4);
	}

	/**
	 * Optimization of booth function \( f(x,y) = \left(x+2y-7\right)^{2}+\left(2x+y-5\right)^{2} \).
	 * The solution of \( f(x,y) = 0 \) is \( x=1 \), \( y=3 \).
	 *
	 * The test uses a a analytic calculation of derivative.
	 *
	 * @throws SolverException Thrown if the solver fails to find a solution.
	 */
	@Test
	public void testBoothFunctionWithAnalyticDerivative() throws SolverException {
		final int numberOfParameters = 2;

		final double[] initialParameters = new double[numberOfParameters];
		Arrays.fill(initialParameters, 2.0);

		final double[] targetValues	= new double[] { 0.0 };

		final int maxIteration = 1000;

		final LevenbergMarquardt optimizer = new LevenbergMarquardt(initialParameters, targetValues, maxIteration, null) {
			private static final long serialVersionUID = -282626938650139518L;

			@Override
			public void setValues(final double[] parameters, final double[] values) {
				values[0] = Math.pow(parameters[0] + 2* parameters[1] - 7,2) + Math.pow(2 * parameters[0] + parameters[1] - 5,2);
			}

			@Override
			public void setDerivatives(final double[] parameters, final double[][] derivatives) {
				derivatives[0][0] = Math.pow(parameters[0] + 2 * parameters[1] - 7,1) * 2 + Math.pow(2 * parameters[0] + parameters[1] - 5,1) * 4;
				derivatives[1][0] = Math.pow(parameters[0] + 2 * parameters[1] - 7,1) * 4 + Math.pow(2 * parameters[0] + parameters[1] - 5,1) * 2;
			}
		};

		// Set solver parameters

		optimizer.run();

		final double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for Booth's function with analytic derivative required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError());

		System.out.println("________________________________________________________________________________");
		System.out.println();

		Assert.assertEquals(0.0, optimizer.getRootMeanSquaredError(), 2E-4);
	}
}
