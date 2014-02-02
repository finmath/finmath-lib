/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 16.06.2006
 */
package net.finmath.optimizer;

import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.finmath.functions.LinearAlgebra;

/**
 * This class implements a parallel Levenberg Marquardt non-linear least-squares fit
 * algorithm.
 * <p>
 * The design avoids the need to define the objective function as a
 * separate class. The objective function is defined by overriding a class
 * method, see the sample code below.
 * </p>
 * 
 * <p>
 * The Levenberg-Marquardt solver is implemented in using multi-threading.
 * The calculation of the derivatives (in case a specific implementation of
 * {@code setDerivatives(double[] parameters, double[][] derivatives)} is not
 * provided) may be performed in parallel by setting the parameter <code>numberOfThreads</code>.
 * </p>
 * 
 * <p>
 * To use the solver inherit from it and implement the objective function as
 * {@code setValues(double[] parameters, double[] values)} where values has
 * to be set to the value of the objective functions for the given parameters.
 * <br>
 * You may also provide an a derivative for your objective function by
 * additionally overriding the function {@code setDerivatives(double[] parameters, double[][] derivatives)},
 * otherwise the solver will calculate the derivative via finite differences.
 * </p>
 * 
 * The following simple example finds a solution for the equation <br>
 * <center>
 * <table>
 * <caption>Sample linear system of equations.</caption>
 * <tr><td>
 * 0.0 * x<sub>1</sub> + 1.0 * x<sub>2</sub> = 5.0
 * </td></tr>
 * <tr><td>
 * 2.0 * x<sub>1</sub> + 1.0 * x<sub>2</sub> = 10.0
 * </td></tr>
 * </table>
 * </center>
 * 
 * <pre>
 * {@code
 * 	LevenbergMarquardt optimizer = new LevenbergMarquardt() {
 * 		// Override your objective function here
 * 		public void setValues(double[] parameters, double[] values) {
 * 			values[0] = parameters[0] * 0.0 + parameters[1];
 * 			values[1] = parameters[0] * 2.0 + parameters[1];
 * 		}
 * 	};
 * 
 * 	// Set solver parameters
 * 	optimizer.setInitialParameters(new double[] { 0, 0 });
 * 	optimizer.setWeights(new double[] { 1, 1 });
 * 	optimizer.setMaxIteration(100);
 * 	optimizer.setTargetValues(new double[] { 5, 10 });
 * 
 * 	optimizer.run();
 * 
 * 	double[] bestParameters = optimizer.getBestFitParameters();
 * }
 * </pre>
 * 
 * See the example in the main method below.
 * 
 * <p>
 * The class can be initialized to use a multi-threaded valuation. If initialized
 * this way the implementation of <code>setValues</code> must be thread-safe.
 * The solver will evaluate the gradient of the value vector in parallel, i.e.,
 * use as many threads as the number of parameters.
 * </p>
 * 
 * Note: Iteration steps will be logged (java.util.logging) with LogLevel.FINE
 * 
 * @author Christian Fries
 * @version 1.4
 */
public abstract class LevenbergMarquardt {

	private double[] initialParameters = null;
	private double[] parameterSteps = null;
	private double[] targetValues = null;
	private double[] weights = null;

	private int		maxIteration = 100;

	private double	lambda				= 0.001;
	private double	lambdaMultiplicator	= 2.0;

	private double	errorTolerance = 0.0;	// by default we solve upto machine presicion

	private int iteration = 0;

	private double[] parameterTest = null;
	private double[] parameterIncrement = null;
	private double[] valueTest = null;

	private double[] parameterCurrent = null;
	private double[] valueCurrent = null;
	private double[][] derivativeCurrent = null;

	private double errorCurrent	= Double.POSITIVE_INFINITY;
    private double errorChange	= Double.POSITIVE_INFINITY;

	private boolean isParameterCurrentDerivativeValid = false;

	// These members will be updated in each iteration. These are members to prevent repeated memory allocation.
	private double[][]	hessianMatrix = null;
	private double[]	beta = null;

	private int				numberOfThreads	= 1;
	private ExecutorService executor		= null;

	private final Logger logger = Logger.getLogger("net.finmath");

	// A simple test
	public static void main(String[] args) throws SolverException {

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
		System.out.println("The solver required " + optimizer.getIterations()
				+ " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 * 
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(double[] initialParameters, double[] targetValues, int maxIteration, int numberOfThreads) {
		super();
		this.initialParameters	= initialParameters;
		this.targetValues		= targetValues;
		this.maxIteration		= maxIteration;

		this.weights			= new double[targetValues.length];
		java.util.Arrays.fill(weights, 1.0);

		this.numberOfThreads = numberOfThreads;
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 */
	public LevenbergMarquardt() {
		super();
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 * 
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(int numberOfThreads) {
		super();
		this.numberOfThreads = numberOfThreads;
	}


	/**
	 * Set the initial parameters for the solver.
	 * 
	 * @param initialParameters
	 *            The initial parameters.
	 */
	public void setInitialParameters(double[] initialParameters) {
		this.initialParameters = initialParameters;
	}

	/**
	 * Set the parameter step for the solver.
	 * The parameter step is used to evaluate the derivatives via
	 * finite differences, if analytic derivatives are not provided.
	 * 
	 * @param parameterSteps The parameter step.
	 */
	public void setParameterSteps(double[] parameterSteps) {
		this.parameterSteps = parameterSteps;
	}

	/**
	 * Set the target values for the solver. The solver will solver the
	 * equation weights * objectiveFunction = targetValues.
	 * 
	 * @param targetValues
	 *            The target values.
	 */
	public void setTargetValues(double[] targetValues) {
		this.targetValues = targetValues;
	}

	/**
	 * Set the maximum number of iterations to be performed until the solver
	 * gives up.
	 * 
	 * @param maxIteration
	 *            The maximum number of iterations.
	 */
	public void setMaxIteration(int maxIteration) {
		this.maxIteration = maxIteration;
	}

	/**
	 * Set the weight for the objective function.
	 * 
	 * @param weights
	 *            The weights for the objective function.
	 */
	public void setWeights(double[] weights) {
		this.weights = weights;
	}


	/**
	 * Set the error tolerance. The solver considers the solution "found"
	 * if the error is not improving by this given error tolerance.
	 * 
	 * @param errorTolerance The error tolerance.
	 */
	public void setErrorTolerance(double errorTolerance) {
		this.errorTolerance = errorTolerance;
	}

	/**
	 * Get the best fit parameter vector.
	 * 
	 * @return The best fit parameter.
	 */
	public double[] getBestFitParameters() {
		return parameterCurrent;
	}

	/**
	 * Get the number of iterations.
	 * 
	 * @return The number of iterations required
	 */
	public int getIterations() {
		return iteration;
	}

	/**
	 * The objective function. Override this method to implement your custom
	 * function.
	 * 
	 * @param parameters Input value. The parameter vector.
	 * @param values Output value. The vector of values f(i,parameters), i=1,...,n
     * @throws SolverException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public abstract void setValues(double[] parameters, double[] values) throws SolverException;

	/**
	 * The derivative of the objective function. You may override this method
	 * if you like to implement your own derivative.
	 * 
	 * @param parameters Input value. The parameter vector.
	 * @param derivatives Output value, where derivatives[i][j] is d(value(j)) / d(parameters(i)
     * @throws SolverException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public void setDerivatives(double[] parameters, double[][] derivatives) throws SolverException {
		// Calculate new derivatives. Note that this method is called only with
		// parameters = parameterCurrent, so we may use valueCurrent.

    	Vector<Future<double[]>> valueFutures = new Vector<Future<double[]>>(parameterCurrent.length);
		for (int parameterIndex = 0; parameterIndex < parameterCurrent.length; parameterIndex++) {
			final double[] parametersNew	= parameters.clone();
			final double[] derivative		= derivatives[parameterIndex];

			final int workerParameterIndex = parameterIndex;
			Callable<double[]> worker = new  Callable<double[]>() {
				public double[] call() throws SolverException {
					double parameterFiniteDifference;
					if(parameterSteps != null) {
						parameterFiniteDifference = parameterSteps[workerParameterIndex];
					}
					else {
						/*
						 * Try to adaptively set a parameter shift. Note that in some
						 * applications it may be important to set parameterSteps.
						 * appropriately.
						 */
						parameterFiniteDifference = (Math.abs(parametersNew[workerParameterIndex]) + 1) * 1E-8;
					}
		
					// Shift parameter value
					parametersNew[workerParameterIndex] += parameterFiniteDifference;
		
					// Calculate derivative as (valueUpShift - valueCurrent) /
					// parameterFiniteDifference
					setValues(parametersNew, derivative);
					for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
						derivative[valueIndex] -= valueCurrent[valueIndex];
						derivative[valueIndex] /= parameterFiniteDifference;
					}
					return derivative;
				}
			};
			if(executor != null) {
				Future<double[]> valueFuture = executor.submit(worker);
				valueFutures.add(parameterIndex, valueFuture);
			}
			else {
				FutureTask<double[]> valueFutureTask = new FutureTask<double[]>(worker);
				valueFutureTask.run();
				valueFutures.add(parameterIndex, valueFutureTask);
			}
		}

		for (int parameterIndex = 0; parameterIndex < parameterCurrent.length; parameterIndex++) {
        	try {
        		derivatives[parameterIndex] = valueFutures.get(parameterIndex).get();
        	}
    		catch (InterruptedException e) {
    			throw new SolverException(e);
			} catch (ExecutionException e) {
    			throw new SolverException(e);
			}
		}
	}

	/**
	 * You may override this method to implement a custom stop condition.
	 * 
	 * @return Stop condition.
	 */
	boolean done() {
		return iteration > maxIteration ||  errorChange <= errorTolerance;
	}

	/**
	 * Runs the optimization.
	 * 
     * @throws SolverException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public void run() throws SolverException {
		// Create an executor for concurrent evaluation of derivatives
		if(numberOfThreads > 1) executor = Executors.newFixedThreadPool(numberOfThreads);

		// Allocate memory
		parameterTest = initialParameters.clone();

		int numberOfParameters = initialParameters.length;
		int numberOfValues = targetValues.length;

		parameterIncrement = new double[numberOfParameters];
		parameterCurrent = new double[numberOfParameters];

		valueTest = new double[numberOfValues];
		valueCurrent = new double[numberOfValues];
		derivativeCurrent = new double[parameterCurrent.length][valueCurrent.length];

		hessianMatrix = new double[parameterCurrent.length][parameterCurrent.length];
		beta = new double[parameterCurrent.length];

		iteration = 0;

		while(true) {
			// Count iterations
			iteration++;

			// Calculate values for test parameters
			setValues(parameterTest, valueTest);

			// calculate error
			double errorTest = getError(valueTest);

			if (errorTest < errorCurrent) {
				errorChange = errorCurrent - errorTest;

				// Accept point
				parameterCurrent	= parameterTest.clone();
				valueCurrent		= valueTest.clone();
				errorCurrent		= errorTest;

				// Derivative has to be recalculated
				isParameterCurrentDerivativeValid = false;

				// Decrease lambda (move faster)
				lambda				/= 3.0;
				lambdaMultiplicator	= 2.0;
			} else {
				errorChange = errorTest - errorCurrent;

				// Reject point, increase lambda (move slower)
				lambda				*= lambdaMultiplicator;
				lambdaMultiplicator *= 2.0;
			}

			// Update a new parameter trial, if we are not done
			if (!done())
				updateParameterTest();
			else
				break;

			// Log iteration
			if (logger.isLoggable(Level.FINE))
			{
				String logString = "Iteration: " + iteration + "\tLambda="
						+ lambda + "\tError Current:" + errorCurrent
						+ "\tError Change:" + errorChange + "\t";
				for (int i = 0; i < parameterCurrent.length; i++) {
					logString += "[" + i + "] = " + parameterCurrent[i] + "\t";
				}
				logger.fine(logString);
			}
		}

		// Create an executor for concurrent evaluation of derivatives
		if(executor != null) {
			executor.shutdown();
			executor = null;
		}
	}

	private double getError(double[] value) {
		double error = 0.0;

		for (int valueIndex = 0; valueIndex < value.length; valueIndex++) {
			double deviation = value[valueIndex] - targetValues[valueIndex];
			error += weights[valueIndex] * deviation * deviation;
		}

		return error;
	}

	/**
	 * Calculate a new parameter guess.
	 * 
     * @throws SolverException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	private void updateParameterTest() throws SolverException {
		if (!isParameterCurrentDerivativeValid) {
			this.setDerivatives(parameterCurrent, derivativeCurrent);
			isParameterCurrentDerivativeValid = true;
		}

		boolean hessianInvalid = true;

		while (hessianInvalid) {
			hessianInvalid = false;
			// Build matrix H (hessian approximation)
			for (int i = 0; i < parameterCurrent.length; i++) {
				for (int j = i; j < parameterCurrent.length; j++) {
					double alphaElement = 0.0;
					for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
						alphaElement += weights[valueIndex] * derivativeCurrent[i][valueIndex] * derivativeCurrent[j][valueIndex];
					}
					if (i == j) {
						if (alphaElement == 0.0)
							alphaElement = 1.0;
						else
							alphaElement *= 1 + lambda;
					}

					hessianMatrix[i][j] = alphaElement;
					hessianMatrix[j][i] = alphaElement;
				}
			}

			// Build beta (Newton step)
			for (int i = 0; i < parameterCurrent.length; i++) {
				double betaElement = 0.0;
				double[] derivativeCurrentSingleParam = derivativeCurrent[i];
				for (int k = 0; k < valueCurrent.length; k++) {
					betaElement += weights[k] * (targetValues[k] - valueCurrent[k]) * derivativeCurrentSingleParam[k];
				}
				beta[i] = betaElement;
			}

			try {
				// Calculate new increment
				parameterIncrement = LinearAlgebra.solveLinearEquationSymmetric(hessianMatrix, beta);
			} catch (Exception e) {
				hessianInvalid	= true;
				lambda			*= 16;
			}
		}

		// Calculate new parameter
		for (int i = 0; i < parameterCurrent.length; i++) {
			parameterTest[i] = parameterCurrent[i] + parameterIncrement[i];
		}
	}
}
