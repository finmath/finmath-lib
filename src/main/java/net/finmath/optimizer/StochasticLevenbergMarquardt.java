/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 16.06.2006
 */
package net.finmath.optimizer;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
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
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a stochastic Levenberg Marquardt non-linear least-squares fit
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
 * {@code setDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[][] derivatives)} is not
 * provided) may be performed in parallel by setting the parameter <code>numberOfThreads</code>.
 * </p>
 * 
 * <p>
 * To use the solver inherit from it and implement the objective function as
 * {@code setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values)} where values has
 * to be set to the value of the objective functions for the given parameters.
 * <br>
 * You may also provide an a derivative for your objective function by
 * additionally overriding the function {@code setDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[][] derivatives)},
 * otherwise the solver will calculate the derivative via finite differences.
 * </p>
 * <p>
 * To reject a point, it is allowed to set an element of <code>values</code> to {@link java.lang.Double#NaN}
 * in the implementation of {@code setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values)}.
 * Put differently: The solver handles NaN values in <code>values</code> as an error larger than
 * the current one (regardless of the current error) and rejects the point.
 * <br>
 * Note, however, that is is an error if the initial parameter guess results in an NaN value.
 * That is, the solver should be initialized with an initial parameter in an admissible region.
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
 * <code>
 * 	LevenbergMarquardt optimizer = new LevenbergMarquardt() {
 * 		// Override your objective function here
 * 		public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) {
 * 			values[0] = parameters[0] * 0.0 + parameters[1];
 * 			values[1] = parameters[0] * 2.0 + parameters[1];
 * 		}
 * 	};
 * 
 * 	// Set solver parameters
 * 	optimizer.setInitialParameters(new RandomVariableInterface[] { 0, 0 });
 * 	optimizer.setWeights(new RandomVariableInterface[] { 1, 1 });
 * 	optimizer.setMaxIteration(100);
 * 	optimizer.setTargetValues(new RandomVariableInterface[] { 5, 10 });
 * 
 * 	optimizer.run();
 * 
 * 	RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
 * </code>
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
 * @version 1.6
 */
public abstract class StochasticLevenbergMarquardt implements Serializable, Cloneable, StochasticOptimizerInterface {

	private static final long serialVersionUID = 4560864869394838155L;

	private RandomVariableInterface[] initialParameters = null;
	private RandomVariableInterface[] parameterSteps = null;
	private RandomVariableInterface[] targetValues = null;
	private RandomVariableInterface[] weights = null;

	private int		maxIteration;

	// Local state of the solver
	private double[]	lambda;
	private double		lambdaInitialValue = 0.001;
	private double		lambdaDivisor = 1.3;
	private double 		lambdaMultiplicator	= 2.0;
	private int			numberOfPaths;

	private RandomVariableInterface	errorTolerance;

	private int iteration = 0;

	private RandomVariableInterface[] parameterTest = null;
	private RandomVariableInterface[] valueTest = null;

	private RandomVariableInterface[] parameterCurrent = null;
	private RandomVariableInterface[] valueCurrent = null;
	private RandomVariableInterface[][] derivativeCurrent = null;

	private RandomVariableInterface errorMeanSquaredCurrent	= new RandomVariable(Double.POSITIVE_INFINITY);
	private RandomVariableInterface errorRootMeanSquaredChange	= new RandomVariable(Double.POSITIVE_INFINITY);

	private boolean[]		isParameterCurrentDerivativeValid;

	/*
	 * Used for multi-threadded calculation of the derivative.
	 * The use may provide its own executor. If not and numberOfThreads > 1
	 * we will temporarily create an executor with the specified number of threads.
	 * Note: If an executor was provided upon construction, it will not receive a shutdown when done.
	 */
	private ExecutorService executor					= null;
	private boolean			executorShutdownWhenDone	= true;

	private final Logger logger = Logger.getLogger("net.finmath");

	// A simple test
	public static void main(String[] args) throws SolverException, CloneNotSupportedException {
		// RandomVariableDifferentiableAAD is possible here!
		// RandomVariableInterface[] initialParameters = new RandomVariableInterface[] { new RandomVariableDifferentiableAAD(2), new RandomVariableDifferentiableAAD(2) };
		RandomVariableInterface[] initialParameters = new RandomVariableInterface[] { new RandomVariable(2), new RandomVariable(2) };
		RandomVariableInterface[] weights = new RandomVariableInterface[] { new RandomVariable(1), new RandomVariable(1) };
		RandomVariableInterface[] parameterSteps = new RandomVariableInterface[] { new RandomVariable(1), new RandomVariable(1) };
		int maxIteration = 100;
		RandomVariableInterface[] targetValues = new RandomVariableInterface[] { new RandomVariable(25), new RandomVariable(100) };

		StochasticLevenbergMarquardt optimizer = new StochasticLevenbergMarquardt(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = -282626938650139518L;

			// Override your objective function here
			@Override
			public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) {
				values[0] = parameters[0].mult(0.0).add(parameters[1]).squared();
				values[1] = parameters[0].mult(2.0).add(parameters[1]).squared();
			}
		};

		// Set solver parameters

		optimizer.run();

		RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		System.out.println("The solver accuracy is " + optimizer.getRootMeanSquaredError().getAverage());

		/*
		 * Creating a clone, continuing the search with new target values.
		 * Note that we do not re-define the setValues method.
		 */
		//		OptimizerInterface optimizer2 = optimizer.getCloneWithModifiedTargetValues(new double[] { 5.1, 10.2 }, new double[] { 1, 1 }, true);
		//		optimizer2.run();

		//		double[] bestParameters2 = optimizer2.getBestFitParameters();
		//		System.out.println("The solver for problem 2 required " + optimizer2.getIterations() + " iterations. The best fit parameters are:");
		//		for (int i = 0; i < bestParameters2.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters2[i]);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 * 
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param weights Weights applied to the error.
	 * @param parameterSteps Step used for finite difference approximation.
	 * @param maxIteration Maximum number of iterations.
	 * @param errorTolerance Error tolerance / accuracy.
	 * @param executorService Executor to be used for concurrent valuation of the derivatives. This is only performed if setDerivative is not overwritten. <i>Warning</i>: The implementation of setValues has to be thread safe!
	 */
	public StochasticLevenbergMarquardt(RandomVariableInterface[] initialParameters, RandomVariableInterface[] targetValues, RandomVariableInterface[] weights, RandomVariableInterface[] parameterSteps, int maxIteration, RandomVariableInterface errorTolerance, ExecutorService executorService) {
		super();
		this.initialParameters	= initialParameters;
		this.targetValues		= targetValues;
		this.weights			= weights;
		this.parameterSteps		= parameterSteps;
		this.maxIteration		= maxIteration;
		this.errorTolerance		= errorTolerance != null ? errorTolerance : new RandomVariable(0.0);

		if(weights == null) {
			this.weights = new RandomVariableInterface[targetValues.length];
			for(int i=0; i<targetValues.length; i++) this.weights[i] = new RandomVariable(1.0);
		}

		this.executor = executorService;
		this.executorShutdownWhenDone = (executorService == null);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 * 
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public StochasticLevenbergMarquardt(RandomVariableInterface[] initialParameters, RandomVariableInterface[] targetValues, int maxIteration, int numberOfThreads) {
		this(initialParameters, targetValues, null, null, maxIteration, null, numberOfThreads > 1 ? Executors.newFixedThreadPool(numberOfThreads) : null);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 * 
	 * @param initialParameters List of initial values for the parameters where the solver starts its search.
	 * @param targetValues List of target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param executorService Executor to be used for concurrent valuation of the derivatives. This is only performed if setDerivative is not overwritten. <i>Warning</i>: The implementation of setValues has to be thread safe!
	 */
	public StochasticLevenbergMarquardt(List<RandomVariableInterface> initialParameters, List<RandomVariableInterface> targetValues, int maxIteration, ExecutorService executorService) {
		this(numberListToDoubleArray(initialParameters), numberListToDoubleArray(targetValues), null, null, maxIteration, null, executorService);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 * 
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public StochasticLevenbergMarquardt(List<RandomVariableInterface> initialParameters, List<RandomVariableInterface> targetValues, int maxIteration, int numberOfThreads) {
		this(numberListToDoubleArray(initialParameters), numberListToDoubleArray(targetValues), maxIteration, numberOfThreads);
	}

	/**
	 * Convert a list of numbers to an array of doubles.
	 * 
	 * @param listOfNumbers A list of numbers.
	 * @return A corresponding array of doubles executing <code>doubleValue()</code> on each element.
	 */
	private static RandomVariableInterface[] numberListToDoubleArray(List<RandomVariableInterface> listOfNumbers) {
		RandomVariableInterface[] array	= new RandomVariableInterface[listOfNumbers.size()];
		for(int i=0; i<array.length; i++) array[i] = listOfNumbers.get(i);
		return array;
	}

	/**
	 * Get the parameter &lambda; used in the Tikhonov-like regularization of the Hessian matrix,
	 * that is the \( \lambda \) in \( H + \lambda \diag H \).
	 * 
	 * @return the parameter \( \lambda \).
	 */
	public double[] getLambda() {
		return lambda;
	}

	/**
	 * Set the parameter &lambda; used in the Tikhonov-like regularization of the Hessian matrix,
	 * that is the \( \lambda \) in \( H + \lambda \diag H \).
	 * 
	 * @param lambda the lambda to set
	 */
	public void setLambda(double[] lambda) {
		this.lambda = lambda;
	}

	/**
	 * Get the multiplicator applied to lambda if the inversion of regularized
	 * Hessian fails, that is, if \( H + \lambda \diag H \) is not invertable.
	 * 
	 * @return the lambdaMultiplicator
	 */
	public double getLambdaMultiplicator() {
		return lambdaMultiplicator;
	}

	/**
	 * Set the multiplicator applied to lambda if the inversion of regularized
	 * Hessian fails, that is, if \( H + \lambda \diag H \) is not invertable.
	 * 
	 * This will make lambda larger, hence let the stepping move slower.
	 * 
	 * @param lambdaMultiplicator the lambdaMultiplicator to set. Should be &gt; 1.
	 */
	public void setLambdaMultiplicator(double lambdaMultiplicator) {
		if(lambdaMultiplicator <= 1.0) throw new IllegalArgumentException("Parameter lambdaMultiplicator is required to be > 1.");
		this.lambdaMultiplicator = lambdaMultiplicator;
	}

	/**
	 * Get the divisor applied to lambda (for the next iteration) if the inversion of regularized
	 * Hessian succeeds, that is, if \( H + \lambda \diag H \) is invertable.
	 * 
	 * @return the lambdaDivisor
	 */
	public double getLambdaDivisor() {
		return lambdaDivisor;
	}

	/**
	 * Set the divisor applied to lambda (for the next iteration) if the inversion of regularized
	 * Hessian succeeds, that is, if \( H + \lambda \diag H \) is invertable.
	 * 
	 * This will make lambda smaller, hence let the stepping move faster.
	 * 
	 * @param lambdaDivisor the lambdaDivisor to set. Should be &gt; 1.
	 */
	public void setLambdaDivisor(double lambdaDivisor) {
		if(lambdaDivisor <= 1.0) throw new IllegalArgumentException("Parameter lambdaDivisor is required to be > 1.");
		this.lambdaDivisor = lambdaDivisor;
	}

	@Override
	public RandomVariableInterface[] getBestFitParameters() {
		return parameterCurrent;
	}

	@Override
	public RandomVariableInterface getRootMeanSquaredError() {
		return errorMeanSquaredCurrent.sqrt();
	}

	/**
	 * @param errorMeanSquaredCurrent the errorMeanSquaredCurrent to set
	 */
	public void setErrorMeanSquaredCurrent(RandomVariableInterface errorMeanSquaredCurrent) {
		this.errorMeanSquaredCurrent = errorMeanSquaredCurrent;
	}

	@Override
	public int getIterations() {
		return iteration;
	}

	protected void prepareAndSetValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) throws SolverException {
		setValues(parameters, values);
	}

	protected void prepareAndSetDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[] values, RandomVariableInterface[][] derivatives) throws SolverException {
		setDerivatives(parameters, derivatives);
	}

	/**
	 * The objective function. Override this method to implement your custom
	 * function.
	 * 
	 * @param parameters Input value. The parameter vector.
	 * @param values Output value. The vector of values f(i,parameters), i=1,...,n
	 * @throws SolverException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public abstract void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) throws SolverException;

	/**
	 * The derivative of the objective function. You may override this method
	 * if you like to implement your own derivative.
	 * 
	 * @param parameters Input value. The parameter vector.
	 * @param derivatives Output value, where derivatives[i][j] is d(value(j)) / d(parameters(i)
	 * @throws SolverException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public void setDerivatives(RandomVariableInterface[] parameters, RandomVariableInterface[][] derivatives) throws SolverException {
		// Calculate new derivatives. Note that this method is called only with
		// parameters = parameterTest, so we may use valueTest.

			parameters = parameterCurrent;
			Vector<Future<RandomVariableInterface[]>> valueFutures = new Vector<Future<RandomVariableInterface[]>>(parameterCurrent.length);
			for (int parameterIndex = 0; parameterIndex < parameterCurrent.length; parameterIndex++) {
				final RandomVariableInterface[] parametersNew	= parameters.clone();
				final RandomVariableInterface[] derivative		= derivatives[parameterIndex];

				final int workerParameterIndex = parameterIndex;
				Callable<RandomVariableInterface[]> worker = new  Callable<RandomVariableInterface[]>() {
					public RandomVariableInterface[] call() throws SolverException {
						RandomVariableInterface parameterFiniteDifference;
						if(parameterSteps != null) {
							parameterFiniteDifference = parameterSteps[workerParameterIndex];
						}
						else {
							/*
							 * Try to adaptively set a parameter shift. Note that in some
							 * applications it may be important to set parameterSteps.
							 * appropriately.
							 */
							parameterFiniteDifference = parametersNew[workerParameterIndex].abs().add(1.0).mult(1E-8);
						}

						// Shift parameter value
						parametersNew[workerParameterIndex] = parametersNew[workerParameterIndex].add(parameterFiniteDifference);

						// Calculate derivative as (valueUpShift - valueCurrent) / parameterFiniteDifference
						try {
							prepareAndSetValues(parametersNew, derivative);
						} catch (Exception e) {
							// We signal an exception to calculate the derivative as NaN
							Arrays.fill(derivative, new RandomVariable(Double.NaN));
						}
						for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
							derivative[valueIndex] = derivative[valueIndex].sub(valueCurrent[valueIndex]).div(parameterFiniteDifference);
							derivative[valueIndex] = derivative[valueIndex].barrier(derivative[valueIndex].isNaN().sub(0.5).mult(-1), derivative[valueIndex], 0.0);
						}
						return derivative;
					}
				};
				if(executor != null) {
					Future<RandomVariableInterface[]> valueFuture = executor.submit(worker);
					valueFutures.add(parameterIndex, valueFuture);
				}
				else {
					FutureTask<RandomVariableInterface[]> valueFutureTask = new FutureTask<RandomVariableInterface[]>(worker);
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
		// The solver terminates if...
		return 
				// Maximum number of iterations is reached
				(iteration > maxIteration)	
				||
				// Error does not improve by more that the given error tolerance
				(errorRootMeanSquaredChange.sub(errorTolerance).getMax() <= 0);
	}

	@Override
	public void run() throws SolverException {
		try {

			// Allocate memory
			int numberOfParameters	= initialParameters.length;
			int numberOfValues		= targetValues.length;

			parameterTest		= initialParameters.clone();
			parameterCurrent	= initialParameters.clone();

			valueTest		= new RandomVariableInterface[numberOfValues];
			valueCurrent		= new RandomVariableInterface[numberOfValues];
			Arrays.fill(valueCurrent, new RandomVariable(Double.NaN));
			derivativeCurrent	= new RandomVariableInterface[numberOfParameters][numberOfValues];

			iteration = 0;

			while(true) {
				// Count iterations
				iteration++;

				// Calculate values for test parameters
				prepareAndSetValues(parameterTest, valueTest);

				// Calculate error
				RandomVariableInterface errorMeanSquaredTest = getMeanSquaredError(valueTest);

				/*
				 * Note: The following test will be false if errorMeanSquaredTest is NaN.
				 * That is: NaN is consider as a rejected point.
				 */
				RandomVariableInterface isPointAccepted = errorMeanSquaredCurrent.sub(errorMeanSquaredTest);

				for(int parameterIndex = 0; parameterIndex<parameterCurrent.length; parameterIndex++) {
					parameterCurrent[parameterIndex] = parameterTest[parameterIndex].barrier(isPointAccepted, parameterTest[parameterIndex], parameterCurrent[parameterIndex]);
				}
				for(int valueIndex = 0; valueIndex<valueCurrent.length; valueIndex++) {
					valueCurrent[valueIndex] = valueTest[valueIndex].barrier(isPointAccepted, valueTest[valueIndex], valueCurrent[valueIndex]);
				}

				// @TODO Always update change? - maybe improve?
				errorRootMeanSquaredChange = isPointAccepted.barrier(isPointAccepted, errorMeanSquaredCurrent.sqrt().sub(errorMeanSquaredTest.sqrt()), errorRootMeanSquaredChange);
				errorMeanSquaredCurrent = errorMeanSquaredTest.cap(errorMeanSquaredCurrent);

				// Check if we are done
				if (done()) break;

				
				// Lazy init of lambda and isParameterCurrentDerivativeValid
				this.numberOfPaths = isPointAccepted.size();		// @TODO: check for parameter and target value sizes!
				if(lambda == null) {
					this.lambda = new double[numberOfPaths];
					Arrays.fill(lambda, lambdaInitialValue);
				}
				if(isParameterCurrentDerivativeValid == null) {
					this.isParameterCurrentDerivativeValid = new boolean[numberOfPaths];
					Arrays.fill(isParameterCurrentDerivativeValid, false);
				}

				/*
				 * Update lambda
				 */
				for(int pathIndex=0; pathIndex<isPointAccepted.size(); pathIndex++) {
					isParameterCurrentDerivativeValid[pathIndex] = isPointAccepted.get(pathIndex) <= 0;
					lambda[pathIndex] = isPointAccepted.get(pathIndex) >= 0 ? lambda[pathIndex] / lambdaDivisor : lambda[pathIndex] * lambdaMultiplicator;
				}

				/*
				 * Calculate new derivative at parameterTest (where point is accepted).
				 * Note: the first argument should be parameterTest to use shortest operator tree.
				 */
				prepareAndSetDerivatives(parameterTest, valueTest, derivativeCurrent);

				/*
				 * Calculate new parameterTest
				 */
				double[][]	parameterIncrement = new double[parameterCurrent.length][numberOfPaths];
				for(int pathIndex=0; pathIndex<numberOfPaths; pathIndex++) {
					// These members will be updated in each iteration. These are members to prevent repeated memory allocation.
					double[][]	hessianMatrix = new double[parameterCurrent.length][parameterCurrent.length];
					double[]	beta = new double[parameterCurrent.length];

					boolean hessianInvalid = true;
					while (hessianInvalid) {
						// Build matrix H (hessian approximation)
						for (int i = 0; i < parameterCurrent.length; i++) {
							for (int j = i; j < parameterCurrent.length; j++) {
								double alphaElement = 0.0;
								for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
									alphaElement += weights[valueIndex].get(pathIndex) * derivativeCurrent[i][valueIndex].get(pathIndex) * derivativeCurrent[j][valueIndex].get(pathIndex);
								}
								if (i == j) {
									if (alphaElement == 0.0)
										alphaElement = 1.0;
									else
										alphaElement *= 1 + lambda[pathIndex];
								}

								hessianMatrix[i][j] = alphaElement;
								hessianMatrix[j][i] = alphaElement;
							}
						}

						// Build beta (Newton step)
						for (int i = 0; i < parameterCurrent.length; i++) {
							double betaElement = 0.0;
							for (int k = 0; k < valueCurrent.length; k++) {
								betaElement += weights[k].get(pathIndex) * (targetValues[k].get(pathIndex) - valueCurrent[k].get(pathIndex)) * derivativeCurrent[i][k].get(pathIndex);
							}
							beta[i] = betaElement;
						}

						try {
							// Calculate new increment
							double[] parameterIncrementOnPath = LinearAlgebra.solveLinearEquationSymmetric(hessianMatrix, beta);
							for(int i=0; i<parameterIncrementOnPath.length; i++) {
								parameterIncrement[i][pathIndex] = parameterIncrementOnPath[i];
							}
							hessianInvalid = false;
						} catch (Exception e) {
							hessianInvalid	= true;
							lambda[pathIndex] *= 16;
						}
					}
				}

				// Calculate new parameter
				for (int i = 0; i < parameterCurrent.length; i++) {
					parameterTest[i] = parameterCurrent[i].add(new RandomVariable(0.0, parameterIncrement[i]));
				}

				// Log iteration
				if (logger.isLoggable(Level.FINE))
				{
					String logString = "Iteration: " + iteration + "\tLambda="
							+ lambda + "\tError Current:" + errorMeanSquaredCurrent
							+ "\tError Change:" + errorRootMeanSquaredChange + "\t";
					for (int i = 0; i < parameterCurrent.length; i++) {
						logString += "[" + i + "] = " + parameterCurrent[i] + "\t";
					}
					logger.fine(logString);
				}
			}
		}
		finally {
			// Shutdown executor if present.
			if(executor != null && executorShutdownWhenDone) {
				executor.shutdown();
				executor = null;
			}
		}
	}

	public RandomVariableInterface getMeanSquaredError(RandomVariableInterface[] value) {
		// Note: it is intentional to use a specific RandomVariable implementation here.
		RandomVariableInterface error = new RandomVariable(0.0);

		for (int valueIndex = 0; valueIndex < value.length; valueIndex++) {
			RandomVariableInterface deviation = value[valueIndex].sub(targetValues[valueIndex]);
			error = error.addProduct(weights[valueIndex], deviation.squared());
		}

		return error.div(value.length);
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer.
	 * 
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(RandomVariableInterface[], RandomVariableInterface[])} and
	 * that of {@link #setDerivatives(RandomVariableInterface[], RandomVariableInterface[][])} is reused.
	 */
	@Override
	public StochasticLevenbergMarquardt clone() throws CloneNotSupportedException {
		/*
		StochasticLevenbergMarquardt clonedOptimizer = (StochasticLevenbergMarquardt)super.clone();
		clonedOptimizer.isParameterCurrentDerivativeValid = false;
		clonedOptimizer.iteration = 0;
		clonedOptimizer.errorMeanSquaredCurrent	= Double.POSITIVE_INFINITY;
		clonedOptimizer.errorRootMeanSquaredChange	= Double.POSITIVE_INFINITY;
		return clonedOptimizer;
		 */
		// @TODO add clone
		return null;
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer with a new vector for the
	 * target values and weights.
	 * 
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(RandomVariableInterface[], RandomVariableInterface[])} and
	 * that of {@link #setDerivatives(RandomVariableInterface[], RandomVariableInterface[][])} is reused.
	 * 
	 * The initial values of the cloned optimizer will either be the original
	 * initial values of this object or the best parameters obtained by this
	 * optimizer, the latter is used only if this optimized signals a {@link #done()}.
	 * 
	 * @param newTargetVaues New array of target values.
	 * @param newWeights New array of weights.
	 * @param isUseBestParametersAsInitialParameters If true and this optimizer is done(), then the clone will use this.{@link #getBestFitParameters()} as initial parameters.
	 * @return A new LevenbergMarquardt optimizer, cloning this one except modified target values and weights.
	 * @throws CloneNotSupportedException Thrown if this optimizer cannot be cloned.
	 */
	public StochasticLevenbergMarquardt getCloneWithModifiedTargetValues(RandomVariableInterface[] newTargetVaues, RandomVariableInterface[] newWeights, boolean isUseBestParametersAsInitialParameters) throws CloneNotSupportedException {
		StochasticLevenbergMarquardt clonedOptimizer = (StochasticLevenbergMarquardt)clone();
		clonedOptimizer.targetValues = newTargetVaues.clone();		// Defensive copy
		clonedOptimizer.weights = newWeights.clone();				// Defensive copy

		if(isUseBestParametersAsInitialParameters && this.done()) clonedOptimizer.initialParameters = this.getBestFitParameters();

		return clonedOptimizer;
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer with a new vector for the
	 * target values and weights.
	 * 
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(RandomVariableInterface[], RandomVariableInterface[])} and
	 * that of {@link #setDerivatives(RandomVariableInterface[], RandomVariableInterface[][])} is reused.
	 * 
	 * The initial values of the cloned optimizer will either be the original
	 * initial values of this object or the best parameters obtained by this
	 * optimizer, the latter is used only if this optimized signals a {@link #done()}.
	 * 
	 * @param newTargetVaues New list of target values.
	 * @param newWeights New list of weights.
	 * @param isUseBestParametersAsInitialParameters If true and this optimizer is done(), then the clone will use this.{@link #getBestFitParameters()} as initial parameters.
	 * @return A new LevenbergMarquardt optimizer, cloning this one except modified target values and weights.
	 * @throws CloneNotSupportedException Thrown if this optimizer cannot be cloned.
	 */
	public StochasticLevenbergMarquardt getCloneWithModifiedTargetValues(List<RandomVariableInterface> newTargetVaues, List<RandomVariableInterface> newWeights, boolean isUseBestParametersAsInitialParameters) throws CloneNotSupportedException {
		StochasticLevenbergMarquardt clonedOptimizer = (StochasticLevenbergMarquardt)clone();
		clonedOptimizer.targetValues = numberListToDoubleArray(newTargetVaues);
		clonedOptimizer.weights = numberListToDoubleArray(newWeights);

		if(isUseBestParametersAsInitialParameters && this.done()) clonedOptimizer.initialParameters = this.getBestFitParameters();

		return clonedOptimizer;
	}
}
