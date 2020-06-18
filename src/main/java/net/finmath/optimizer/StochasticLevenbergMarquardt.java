/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

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
 * {@code setDerivatives(RandomVariable[] parameters, RandomVariable[][] derivatives)} is not
 * provided) may be performed in parallel by setting the parameter <code>numberOfThreads</code>.
 * </p>
 *
 * <p>
 * To use the solver inherit from it and implement the objective function as
 * {@code setValues(RandomVariable[] parameters, RandomVariable[] values)} where values has
 * to be set to the value of the objective functions for the given parameters.
 * <br>
 * You may also provide an a derivative for your objective function by
 * additionally overriding the function {@code setDerivatives(RandomVariable[] parameters, RandomVariable[][] derivatives)},
 * otherwise the solver will calculate the derivative via finite differences.
 * </p>
 * <p>
 * To reject a point, it is allowed to set an element of <code>values</code> to {@link java.lang.Double#NaN}
 * in the implementation of {@code setValues(RandomVariable[] parameters, RandomVariable[] values)}.
 * Put differently: The solver handles NaN values in <code>values</code> as an error larger than
 * the current one (regardless of the current error) and rejects the point.
 * <br>
 * Note, however, that is is an error if the initial parameter guess results in an NaN value.
 * That is, the solver should be initialized with an initial parameter in an admissible region.
 * </p>
 *
 * The following simple example finds a solution for the equation <br>
 * <table>
 * <caption>Sample linear system of equations.</caption>
 * <tr><td>
 * 0.0 * x<sub>1</sub> + 1.0 * x<sub>2</sub> = 5.0
 * </td></tr>
 * <tr><td>
 * 2.0 * x<sub>1</sub> + 1.0 * x<sub>2</sub> = 10.0
 * </td></tr>
 * </table>
 *
 * <pre>
 * <code>
 * 	LevenbergMarquardt optimizer = new LevenbergMarquardt() {
 * 		// Override your objective function here
 * 		public void setValues(RandomVariable[] parameters, RandomVariable[] values) {
 * 			values[0] = parameters[0] * 0.0 + parameters[1];
 * 			values[1] = parameters[0] * 2.0 + parameters[1];
 * 		}
 * 	};
 *
 * 	// Set solver parameters
 * 	optimizer.setInitialParameters(new RandomVariable[] { 0, 0 });
 * 	optimizer.setWeights(new RandomVariable[] { 1, 1 });
 * 	optimizer.setMaxIteration(100);
 * 	optimizer.setTargetValues(new RandomVariable[] { 5, 10 });
 *
 * 	optimizer.run();
 *
 * 	RandomVariable[] bestParameters = optimizer.getBestFitParameters();
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
public abstract class StochasticLevenbergMarquardt implements Serializable, Cloneable, StochasticOptimizer {

	private static final long serialVersionUID = 4560864869394838155L;

	/**
	 * The regularization method used to invert the approximation of the
	 * Hessian matrix.
	 *
	 * @author Christian Fries
	 */
	public enum RegularizationMethod {
		/**
		 * The Hessian approximated and regularized as
		 * \( H_{\lambda} = J^T J + \lambda I \).
		 */
		LEVENBERG,

		/**
		 * The Hessian approximated and regularized as
		 * \( H_{\lambda} = J^T J + \lambda \text{diag}(J^T J) \).
		 */
		LEVENBERG_MARQUARDT
	}

	private final RegularizationMethod regularizationMethod;

	private RandomVariable[] initialParameters = null;
	private RandomVariable[] parameterSteps = null;
	private RandomVariable[] targetValues = null;

	private final int		maxIteration;

	// Local state of the solver
	private double		lambda;
	private final double		lambdaInitialValue = 0.001;
	private double		lambdaDivisor = 3.0;
	private double 		lambdaMultiplicator	= 2.0;

	private final double	errorTolerance;

	private int iteration = 0;

	private RandomVariable[] parameterTest = null;
	private RandomVariable[] valueTest = null;

	private RandomVariable[] parameterCurrent = null;
	private RandomVariable[] valueCurrent = null;
	private RandomVariable[][] derivativeCurrent = null;

	private double errorMeanSquaredCurrent		= Double.POSITIVE_INFINITY;
	private double errorRootMeanSquaredChange	= Double.POSITIVE_INFINITY;

	private boolean	isParameterCurrentDerivativeValid;

	/*
	 * Used for multi-threadded calculation of the derivative.
	 * The use may provide its own executor. If not and numberOfThreads > 1
	 * we will temporarily create an executor with the specified number of threads.
	 * Note: If an executor was provided upon construction, it will not receive a shutdown when done.
	 */
	private int				numberOfThreads	= 1;
	private ExecutorService executor					= null;
	private boolean			executorShutdownWhenDone	= true;

	private final Logger logger = Logger.getLogger("net.finmath");

	// A simple test
	public static void main(final String[] args) throws SolverException {
		// RandomVariableDifferentiableAAD is possible here!
		// RandomVariable[] initialParameters = new RandomVariable[] { new RandomVariableDifferentiableAAD(2), new RandomVariableDifferentiableAAD(2) };
		final RandomVariable[] initialParameters = new RandomVariable[] { new RandomVariableFromDoubleArray(2), new RandomVariableFromDoubleArray(2) };
		final RandomVariable[] parameterSteps = new RandomVariable[] { new RandomVariableFromDoubleArray(1), new RandomVariableFromDoubleArray(1) };
		final int maxIteration = 100;
		final RandomVariable[] targetValues = new RandomVariable[] { new RandomVariableFromDoubleArray(25), new RandomVariableFromDoubleArray(100) };

		final StochasticLevenbergMarquardt optimizer = new StochasticLevenbergMarquardt(initialParameters, targetValues, parameterSteps, maxIteration, 1E-12, null) {
			private static final long serialVersionUID = -282626938650139518L;

			// Override your objective function here
			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) {
				values[0] = parameters[0].mult(0.0).add(parameters[1]).squared();
				values[1] = parameters[0].mult(2.0).add(parameters[1]).squared();
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

		/*
		 * Creating a clone, continuing the search with new target values.
		 * Note that we do not re-define the setValues method.
		 */
		//		Optimizer optimizer2 = optimizer.getCloneWithModifiedTargetValues(new double[] { 5.1, 10.2 }, new double[] { 1, 1 }, true);
		//		optimizer2.run();

		//		double[] bestParameters2 = optimizer2.getBestFitParameters();
		//		System.out.println("The solver for problem 2 required " + optimizer2.getIterations() + " iterations. The best fit parameters are:");
		//		for (int i = 0; i < bestParameters2.length; i++) System.out.println("\tparameter[" + i + "]: " + bestParameters2[i]);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param regularizationMethod The regularization method to use. See {@link RegularizationMethod}.
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param parameterSteps Step used for finite difference approximation.
	 * @param maxIteration Maximum number of iterations.
	 * @param errorTolerance Error tolerance / accuracy.
	 * @param executorService Executor to be used for concurrent valuation of the derivatives. This is only performed if setDerivative is not overwritten. <i>Warning</i>: The implementation of setValues has to be thread safe!
	 */
	public StochasticLevenbergMarquardt(final RegularizationMethod regularizationMethod, final RandomVariable[] initialParameters, final RandomVariable[] targetValues, final RandomVariable[] parameterSteps, final int maxIteration, final double errorTolerance, final ExecutorService executorService) {
		super();
		this.regularizationMethod = regularizationMethod;
		this.initialParameters	= initialParameters;
		this.targetValues		= targetValues;
		this.parameterSteps		= parameterSteps;
		this.maxIteration		= maxIteration;
		this.errorTolerance		= errorTolerance;
		executor = executorService;
		executorShutdownWhenDone = (executorService == null);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param parameterSteps Step used for finite difference approximation.
	 * @param maxIteration Maximum number of iterations.
	 * @param errorTolerance Error tolerance / accuracy.
	 * @param executorService Executor to be used for concurrent valuation of the derivatives. This is only performed if setDerivative is not overwritten. <i>Warning</i>: The implementation of setValues has to be thread safe!
	 */
	public StochasticLevenbergMarquardt(final RandomVariable[] initialParameters, final RandomVariable[] targetValues, final RandomVariable[] parameterSteps, final int maxIteration, final double errorTolerance, final ExecutorService executorService) {
		this(RegularizationMethod.LEVENBERG_MARQUARDT, initialParameters, targetValues, parameterSteps, maxIteration, errorTolerance, executorService);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param regularizationMethod The regularization method to use. See {@link RegularizationMethod}.
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param parameterSteps Step used for finite difference approximation.
	 * @param maxIteration Maximum number of iterations.
	 * @param errorTolerance Error tolerance / accuracy.
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public StochasticLevenbergMarquardt(final RegularizationMethod regularizationMethod, final RandomVariable[] initialParameters, final RandomVariable[] targetValues, final RandomVariable[] parameterSteps, final int maxIteration, final double errorTolerance,  final int numberOfThreads) {
		this(regularizationMethod, initialParameters, targetValues, parameterSteps, maxIteration, errorTolerance, null);

		this.numberOfThreads = numberOfThreads;
	}

	/**
	 * Get the parameter &lambda; used in the Tikhonov-like regularization of the Hessian matrix,
	 * that is the \( \lambda \) in \( H + \lambda \diag H \).
	 *
	 * @return the parameter \( \lambda \).
	 */
	public double getLambda() {
		return lambda;
	}

	/**
	 * Set the parameter &lambda; used in the Tikhonov-like regularization of the Hessian matrix,
	 * that is the \( \lambda \) in \( H + \lambda \diag H \).
	 *
	 * @param lambda the lambda to set
	 */
	public void setLambda(final double lambda) {
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
	public void setLambdaMultiplicator(final double lambdaMultiplicator) {
		if(lambdaMultiplicator <= 1.0) {
			throw new IllegalArgumentException("Parameter lambdaMultiplicator is required to be > 1.");
		}
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
	public void setLambdaDivisor(final double lambdaDivisor) {
		if(lambdaDivisor <= 1.0) {
			throw new IllegalArgumentException("Parameter lambdaDivisor is required to be > 1.");
		}
		this.lambdaDivisor = lambdaDivisor;
	}

	@Override
	public RandomVariable[] getBestFitParameters() {
		return parameterCurrent;
	}

	@Override
	public double getRootMeanSquaredError() {
		return Math.sqrt(errorMeanSquaredCurrent);
	}

	/**
	 * @param errorMeanSquaredCurrent the errorMeanSquaredCurrent to set
	 */
	public void setErrorMeanSquaredCurrent(final double errorMeanSquaredCurrent) {
		this.errorMeanSquaredCurrent = errorMeanSquaredCurrent;
	}

	@Override
	public int getIterations() {
		return iteration;
	}

	protected void prepareAndSetValues(final RandomVariable[] parameters, final RandomVariable[] values) throws SolverException {
		setValues(parameters, values);
	}

	protected void prepareAndSetDerivatives(final RandomVariable[] parameters, final RandomVariable[] values, final RandomVariable[][] derivatives) throws SolverException {
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
	public abstract void setValues(RandomVariable[] parameters, RandomVariable[] values) throws SolverException;

	/**
	 * The derivative of the objective function. You may override this method
	 * if you like to implement your own derivative.
	 *
	 * @param parameters Input value. The parameter vector.
	 * @param derivatives Output value, where derivatives[i][j] is d(value(j)) / d(parameters(i)
	 * @throws SolverException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public void setDerivatives(RandomVariable[] parameters, final RandomVariable[][] derivatives) throws SolverException {
		// Calculate new derivatives. Note that this method is called only with
		// parameters = parameterTest, so we may use valueTest.

		parameters = parameterCurrent;
		final Vector<Future<RandomVariable[]>> valueFutures = new Vector<>(parameterCurrent.length);
		for (int parameterIndex = 0; parameterIndex < parameterCurrent.length; parameterIndex++) {
			final RandomVariable[] parametersNew	= parameters.clone();
			final RandomVariable[] derivative		= derivatives[parameterIndex];

			final int workerParameterIndex = parameterIndex;
			final Callable<RandomVariable[]> worker = new  Callable<RandomVariable[]>() {
				@Override
				public RandomVariable[] call() {
					RandomVariable parameterFiniteDifference;
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
					} catch (final Exception e) {
						// We signal an exception to calculate the derivative as NaN
						Arrays.fill(derivative, new RandomVariableFromDoubleArray(Double.NaN));
					}
					for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
						derivative[valueIndex] = derivative[valueIndex].sub(valueCurrent[valueIndex]).div(parameterFiniteDifference);
						//						derivative[valueIndex] = derivative[valueIndex].isNaN().sub(0.5).mult(-1).choose(derivative[valueIndex], new Scalar(0.0));
					}
					return derivative;
				}
			};
			if(executor != null) {
				final Future<RandomVariable[]> valueFuture = executor.submit(worker);
				valueFutures.add(parameterIndex, valueFuture);
			}
			else {
				final FutureTask<RandomVariable[]> valueFutureTask = new FutureTask<>(worker);
				valueFutureTask.run();
				valueFutures.add(parameterIndex, valueFutureTask);
			}
		}

		for (int parameterIndex = 0; parameterIndex < parameterCurrent.length; parameterIndex++) {
			try {
				derivatives[parameterIndex] = valueFutures.get(parameterIndex).get();
			}
			catch (final InterruptedException | ExecutionException e) {
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
				(errorRootMeanSquaredChange <= errorTolerance);
	}

	@Override
	public void run() throws SolverException {
		// Create an executor for concurrent evaluation of derivatives
		if(numberOfThreads > 1) {
			if(executor == null) {
				executor = Executors.newFixedThreadPool(numberOfThreads);
				executorShutdownWhenDone = true;
			}
		}

		try {

			// Allocate memory
			final int numberOfParameters	= initialParameters.length;
			final int numberOfValues		= targetValues.length;

			parameterTest		= initialParameters.clone();
			parameterCurrent	= initialParameters.clone();

			valueTest		= new RandomVariable[numberOfValues];
			valueCurrent		= new RandomVariable[numberOfValues];
			Arrays.fill(valueCurrent, new RandomVariableFromDoubleArray(Double.NaN));
			derivativeCurrent	= new RandomVariable[numberOfParameters][numberOfValues];

			iteration = 0;
			lambda = lambdaInitialValue;
			isParameterCurrentDerivativeValid = false;

			while(true) {
				// Count iterations
				iteration++;

				// Calculate values for test parameters
				prepareAndSetValues(parameterTest, valueTest);

				// Calculate error
				final double errorMeanSquaredTest = getMeanSquaredError(valueTest);

				/*
				 * Note: The following test will be false if errorMeanSquaredTest is NaN.
				 * That is: NaN is consider as a rejected point.
				 */
				final boolean isPointAccepted = errorMeanSquaredCurrent > errorMeanSquaredTest;

				if(isPointAccepted) {
					parameterCurrent = parameterTest.clone();
					valueCurrent = valueTest.clone();
					errorRootMeanSquaredChange = Math.sqrt(errorMeanSquaredCurrent) -  Math.sqrt(errorMeanSquaredTest);
					errorMeanSquaredCurrent = errorMeanSquaredTest;
				}

				// Check if we are done
				if (done()) {
					break;
				}

				/*
				 * Update lambda
				 */
				isParameterCurrentDerivativeValid = !isPointAccepted;
				if(isPointAccepted) {
					lambda /= lambdaDivisor;
				}
				else {
					lambda *= lambdaMultiplicator;
				}

				/*
				 * Calculate new derivative at parameterTest (where point is accepted).
				 * Note: the first argument should be parameterTest to use shortest operator tree.
				 */
				prepareAndSetDerivatives(parameterTest, valueTest, derivativeCurrent);

				/*
				 * Calculate new parameterTest
				 */
				double[]	parameterIncrement = new double[parameterCurrent.length];
				// These members will be updated in each iteration. These are members to prevent repeated memory allocation.
				final double[][]	hessianMatrix = new double[parameterCurrent.length][parameterCurrent.length];
				final double[]	beta = new double[parameterCurrent.length];

				boolean hessianInvalid = true;
				while (hessianInvalid) {
					// Build matrix H (hessian approximation)
					for (int i = 0; i < parameterCurrent.length; i++) {
						for (int j = i; j < parameterCurrent.length; j++) {
							double alphaElement = 0.0;
							for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
								if(derivativeCurrent[i][valueIndex] != null && derivativeCurrent[j][valueIndex] != null) {
									alphaElement += derivativeCurrent[i][valueIndex].mult(derivativeCurrent[j][valueIndex]).getAverage();
								}
							}
							if (i == j) {
								if(regularizationMethod == RegularizationMethod.LEVENBERG) {
									// RegularizationMethod.LEVENBERG - Regularization with a constant lambda
									alphaElement += lambda;
								}
								else {
									// RegularizationMethod.LEVENBERG_MARQUARDT - Regularization with a lambda time the diagonal of JTJ
									if (alphaElement == 0.0) {
										alphaElement = lambda;
									}
									else {
										alphaElement *= 1 + lambda;
									}
								}
							}

							hessianMatrix[i][j] = alphaElement;
							hessianMatrix[j][i] = alphaElement;
						}
					}

					// Build beta (Newton step)
					for (int i = 0; i < parameterCurrent.length; i++) {
						double betaElement = 0.0;
						final RandomVariable[] derivativeCurrentSingleParam = derivativeCurrent[i];
						for (int k = 0; k < valueCurrent.length; k++) {
							if(derivativeCurrentSingleParam[k] != null) {
								betaElement += targetValues[k].sub(valueCurrent[k]).mult(derivativeCurrentSingleParam[k]).getAverage();
							}
						}
						beta[i] = betaElement;
					}

					try {
						// Calculate new increment
						parameterIncrement = LinearAlgebra.solveLinearEquationSymmetric(hessianMatrix, beta);
						hessianInvalid = false;
					} catch (final Exception e) {
						hessianInvalid	= true;
						lambda *= 16;
					}
				}

				// Calculate new parameter
				for (int i = 0; i < parameterCurrent.length; i++) {
					parameterTest[i] = parameterCurrent[i].add(parameterIncrement[i]);
				}

				// Log iteration
				if (logger.isLoggable(Level.FINE))
				{
					String logString =
							"Iteration: " + iteration +
							"\tLambda=" + lambda +
							"\tError Current (RMS):" + Math.sqrt(errorMeanSquaredCurrent) +
							"\tError Change:" + errorRootMeanSquaredChange + "\t";
					for (int i = 0; i < parameterCurrent.length; i++) {
						logString += "[" + i + "] = " + parameterCurrent[i].doubleValue() + "\t";
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

	public double getMeanSquaredError(final RandomVariable[] value) {
		double error = 0.0;

		for (int valueIndex = 0; valueIndex < value.length; valueIndex++) {
			final double deviationSquared = value[valueIndex].sub(targetValues[valueIndex]).squared().getAverage();
			error = error + deviationSquared;
		}

		return error / value.length;
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer.
	 *
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(RandomVariable[], RandomVariable[])} and
	 * that of {@link #setDerivatives(RandomVariable[], RandomVariable[][])} is reused.
	 */
	@Override
	public StochasticLevenbergMarquardt clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer with a new vector for the
	 * target values and weights.
	 *
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(RandomVariable[], RandomVariable[])} and
	 * that of {@link #setDerivatives(RandomVariable[], RandomVariable[][])} is reused.
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
	public StochasticLevenbergMarquardt getCloneWithModifiedTargetValues(final RandomVariable[] newTargetVaues, final RandomVariable[] newWeights, final boolean isUseBestParametersAsInitialParameters) throws CloneNotSupportedException {
		final StochasticLevenbergMarquardt clonedOptimizer = clone();
		clonedOptimizer.targetValues = newTargetVaues.clone();		// Defensive copy

		if(isUseBestParametersAsInitialParameters && this.done()) {
			clonedOptimizer.initialParameters = this.getBestFitParameters();
		}

		return clonedOptimizer;
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer with a new vector for the
	 * target values and weights.
	 *
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(RandomVariable[], RandomVariable[])} and
	 * that of {@link #setDerivatives(RandomVariable[], RandomVariable[][])} is reused.
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
	public StochasticLevenbergMarquardt getCloneWithModifiedTargetValues(final List<RandomVariable> newTargetVaues, final List<RandomVariable> newWeights, final boolean isUseBestParametersAsInitialParameters) throws CloneNotSupportedException {
		final StochasticLevenbergMarquardt clonedOptimizer = clone();
		clonedOptimizer.targetValues = newTargetVaues.toArray(new RandomVariable[] {});

		if(isUseBestParametersAsInitialParameters && this.done()) {
			clonedOptimizer.initialParameters = this.getBestFitParameters();
		}

		return clonedOptimizer;
	}
}
