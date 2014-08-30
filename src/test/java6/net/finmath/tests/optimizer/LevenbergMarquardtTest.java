/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 12.07.2014
 */

package net.finmath.tests.optimizer;

import java.util.ArrayList;

import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Christian Fries
 *
 */
public class LevenbergMarquardtTest {

	@Test
	public void testSmallLinearSystem() throws CloneNotSupportedException, SolverException {
		LevenbergMarquardt optimizer = new LevenbergMarquardt() {

			// Override your objective function here
			@Override
			public void setValues(double[] parameters, double[] values) {
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

		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		System.out.println();

		Assert.assertTrue(Math.abs(bestParameters[0] - 2.5) < 1E-12);
		Assert.assertTrue(Math.abs(bestParameters[1] - 5.0) < 1E-12);
		
		/*
		 * Creating a clone, continuing the search with new target values.
		 * Note that we do not re-define the setValues method.
		 */
		LevenbergMarquardt optimizer2 = optimizer.getCloneWithModifiedTargetValues(new double[] { 5.1, 10.2 }, new double[] { 1, 1 }, true);
		optimizer2.run();

		double[] bestParameters2 = optimizer2.getBestFitParameters();
		System.out.println("The solver for problem 2 required " + optimizer2.getIterations() + " iterations. Accuracy is " + optimizer2.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters2.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters2[i]);

		System.out.println();

		Assert.assertTrue(Math.abs(bestParameters2[0] - 2.55) < 1E-12);
		Assert.assertTrue(Math.abs(bestParameters2[1] - 5.10) < 1E-12);
	}

	@Test
	public void testMultiThreaddedOptimizer() throws SolverException {
		LevenbergMarquardt optimizer = new LevenbergMarquardt(
				new double[] { 0,  0, 0 },		// Initial parameters
				new double[] { 5, 10, 2 }, 	// Target values
				100,						// Max iterations
				10							// Number of threads
				) {

			// Override your objective function here
			@Override
			public void setValues(double[] parameters, double[] values) {
				values[0] = 1.0 * parameters[0] + 2.0 * parameters[1] + parameters[2] + parameters[0] * parameters[1];
				values[1] = 2.0 * parameters[0] + 1.0 * parameters[1] + parameters[2] + parameters[1] * parameters[2];
				values[2] = 3.0 * parameters[0] + 0.0 * parameters[1] + parameters[2];
			}
		};

		optimizer.run();

		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 3 required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		double[] values = new double[3];
		optimizer.setValues(bestParameters, values);
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tvalue[" + i + "]: " + values[i]);
		
		System.out.println();

		Assert.assertTrue(optimizer.getRootMeanSquaredError() < 1E-2);
	}
	
	@Test
	public void testRosenbrockFunction() throws SolverException {
		LevenbergMarquardt optimizer = new LevenbergMarquardt(
				new double[] { 0.5, 0.5 },		// Initial parameters
				new double[] { 0.0, 0.0 }, 		// Target values
				100,							// Max iterations
				10								// Number of threads
				) {

			// Override your objective function here
			@Override
			public void setValues(double[] parameters, double[] values) {
				values[0] = 10.0 * (parameters[1] - parameters[0]*parameters[0]);
				values[1] = 1.0 - parameters[0];
			}
		};

		optimizer.run();

		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 'Rosebrock' required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		double[] values = new double[2];
		optimizer.setValues(bestParameters, values);
		for (int i = 0; i < values.length; i++) System.out.println("\tvalue[" + i + "]: " + values[i]);

		System.out.println();
		
		Assert.assertTrue(Math.abs(bestParameters[0] - 1.0) < 1E-10);
		Assert.assertTrue(Math.abs(bestParameters[1] - 1.0) < 1E-10);
	}

	@Test
	public void testRosenbrockFunctionWithList() throws SolverException {
		ArrayList<Number> initialParams = new ArrayList<Number>();
		initialParams.add(0.5);
		initialParams.add(0.5);

		ArrayList<Number> targetValues = new ArrayList<Number>();
		targetValues.add(0.0);
		targetValues.add(0.0);

		LevenbergMarquardt optimizer = new LevenbergMarquardt(
				initialParams,					// Initial parameters
				targetValues, 					// Target values
				100,							// Max iterations
				10								// Number of threads
				) {

			// Override your objective function here
			@Override
			public void setValues(double[] parameters, double[] values) {
				values[0] = 10.0 * (parameters[1] - parameters[0]*parameters[0]);
				values[1] = 1.0 - parameters[0];
			}
		};

		optimizer.run();

		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 'Rosebrock' required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		double[] values = new double[2];
		optimizer.setValues(bestParameters, values);
		for (int i = 0; i < values.length; i++) System.out.println("\tvalue[" + i + "]: " + values[i]);

		System.out.println();
		
		Assert.assertTrue(Math.abs(bestParameters[0] - 1.0) < 1E-10);
		Assert.assertTrue(Math.abs(bestParameters[1] - 1.0) < 1E-10);
	}
}
