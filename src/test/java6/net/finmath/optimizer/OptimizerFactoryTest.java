/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 30.05.2015
 */

package net.finmath.optimizer;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.math.optimizer.OptimizerFactoryCMAES;
import net.finmath.math.optimizer.OptimizerFactoryInterface;
import net.finmath.math.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.math.optimizer.OptimizerInterface;
import net.finmath.math.optimizer.SolverException;

/**
 * @author Christian Fries
 *
 */
public class OptimizerFactoryTest {

	@Test
	public void testRosenbrockFunctionWithCMAES() throws SolverException {
		
		OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryCMAES(0.0 /* accuracy */, 200 /* maxIterations */);
		this.testOptimizerWithRosenbrockFunction(optimizerFactory);
	}
	
	@Test
	public void testRosenbrockFunctionWithLevenbergMarquard() throws SolverException {
		
		OptimizerFactoryInterface optimizerFactory = new OptimizerFactoryLevenbergMarquardt(200 /* maxIterations */, 2 /* maxThreads */);
		this.testOptimizerWithRosenbrockFunction(optimizerFactory);
	}

	public void testOptimizerWithRosenbrockFunction(OptimizerFactoryInterface optimizerFactory) throws SolverException {
		OptimizerInterface.ObjectiveFunction objectiveFunction = new OptimizerInterface.ObjectiveFunction() {
				@Override
				public void setValues(double[] parameters, double[] values) {
					values[0] = 10.0 * (parameters[1] - parameters[0]*parameters[0]);
					values[1] = 1.0 - parameters[0];
				}
		};
		
		OptimizerInterface optimizer = optimizerFactory.getOptimizer(
				objectiveFunction,
				new double[] { 0.5, 0.5 } /* initialParameters */,
				new double[] { Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY } /* lowerBound */,
				new double[] { Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY } /* upperBound */,
				new double[] { 0.5, 0.5 } /* parameterStep */,
				new double[] { 0.0, 0.0 } /* targetValues */);

		optimizer.run();

		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver " + optimizer.getClass() + " for problem 'Rosebrock' required " + optimizer.getIterations() + " iterations. Accuracy is " + optimizer.getRootMeanSquaredError() + ". The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);

		System.out.println();
		
		Assert.assertTrue(Math.abs(bestParameters[0] - 1.0) < 1E-10);
		Assert.assertTrue(Math.abs(bestParameters[1] - 1.0) < 1E-10);
	}

}
