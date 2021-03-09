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

/**
 * This class implements a parallel Levenberg-Marquardt non-linear least-squares fit
 * algorithm.
 *
 * <p>
 * The solver minimizes \( || f ||_{L_{2}} \) for a function \( f:\mathbb{R}^n \rightarrow \mathbb{R}^m \).
 * The solver requires the calculation of a Jacobi-matrix \( J = \frac{\mathrm{d}f}{\mathrm{d}x} \). The iteration steps
 * are then defined by
 * \[
 * 		\Delta x = H_{\lambda}^{-1} J^T f
 * \]
 * where \( H_{\lambda} \) is a regularized approximation of the Hessian matrix.
 * The solver supports two different regularizations. For <code>RegularizationMethod.LEVENBERG</code> the solver uses
 * \( H_{\lambda} = J^T J + \lambda I \). For <code>RegularizationMethod.LEVENBERG_MARQUARDT</code> the solver uses
 * \( H_{\lambda} = J^T J + \lambda \text{diag}(J^T J) \).
 * </p>
 *
 * <p>
 * The design avoids the need to define the objective function as a
 * separate class. The objective function is defined by overriding a class
 * method, see the sample code below.
 * </p>

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
 * <p>
 * To reject a point, it is allowed to set an element of <code>values</code> to {@link java.lang.Double#NaN}
 * in the implementation of {@code setValues(double[] parameters, double[] values)}.
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
public abstract class LevenbergMarquardt implements Serializable, Cloneable, Optimizer {

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

	private double[] initialParameters = null;
	private double[] parameterSteps = null;
	private double[] targetValues = null;
	private double[] weights = null;

	private int		maxIteration = 100;

	private double	lambda				= 0.001;
	private double	lambdaDivisor		= 3.0;
	private double	lambdaMultiplicator	= 2.0;

	private double	errorRootMeanSquaredTolerance = 0.0;	// by default we solve upto machine presicion

	private int iteration = 0;

	private double[] parameterTest = null;
	private double[] parameterIncrement = null;
	private double[] valueTest = null;

	private double[] parameterCurrent = null;
	private double[] valueCurrent = null;
	private double[][] derivativeCurrent = null;

	private double errorMeanSquaredCurrent	= Double.POSITIVE_INFINITY;
	private double errorRootMeanSquaredChange	= Double.POSITIVE_INFINITY;

	private boolean isParameterCurrentDerivativeValid = false;

	// These members will be updated in each iteration. These are members to prevent repeated memory allocation.
	private double[][]	hessianMatrix = null;
	private double[]	beta = null;

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
	public static void main(final String[] args) throws SolverException, CloneNotSupportedException {

		final LevenbergMarquardt optimizer = new LevenbergMarquardt() {
			private static final long serialVersionUID = -282626938650139518L;

			// Override your objective function here
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
		System.out.println("The solver for problem 1 required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters[i]);
		}

		/*
		 * Creating a clone, continuing the search with new target values.
		 * Note that we do not re-define the setValues method.
		 */
		final Optimizer optimizer2 = optimizer.getCloneWithModifiedTargetValues(new double[] { 5.1, 10.2 }, new double[] { 1, 1 }, true);
		optimizer2.run();

		final double[] bestParameters2 = optimizer2.getBestFitParameters();
		System.out.println("The solver for problem 2 required " + optimizer2.getIterations() + " iterations. The best fit parameters are:");
		for (int i = 0; i < bestParameters2.length; i++) {
			System.out.println("\tparameter[" + i + "]: " + bestParameters2[i]);
		}
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param regularizationMethod The regularization method to use. See {@link RegularizationMethod}.
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param executorService Executor to be used for concurrent valuation of the derivatives. This is only performed if setDerivative is not overwritten. <i>Warning</i>: The implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(final RegularizationMethod regularizationMethod, final double[] initialParameters, final double[] targetValues, final int maxIteration, final ExecutorService executorService) {
		super();
		this.regularizationMethod = regularizationMethod;
		this.initialParameters	= initialParameters;
		this.targetValues		= targetValues;
		this.maxIteration		= maxIteration;

		weights			= new double[targetValues.length];
		java.util.Arrays.fill(weights, 1.0);

		executor = executorService;
		executorShutdownWhenDone = (executorService == null);
		numberOfThreads = 1;
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param executorService Executor to be used for concurrent valuation of the derivatives. This is only performed if setDerivative is not overwritten. <i>Warning</i>: The implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(final double[] initialParameters, final double[] targetValues, final int maxIteration, final ExecutorService executorService) {
		this(RegularizationMethod.LEVENBERG_MARQUARDT,
				initialParameters, targetValues, maxIteration, executorService);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param regularizationMethod The regularization method to use. See {@link RegularizationMethod}.
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(final RegularizationMethod regularizationMethod, final double[] initialParameters, final double[] targetValues, final int maxIteration, final int numberOfThreads) {
		this(regularizationMethod, initialParameters, targetValues, maxIteration, null);

		this.numberOfThreads = numberOfThreads;
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(final double[] initialParameters, final double[] targetValues, final int maxIteration, final int numberOfThreads) {
		this(RegularizationMethod.LEVENBERG_MARQUARDT, initialParameters, targetValues, maxIteration, numberOfThreads);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param initialParameters List of initial values for the parameters where the solver starts its search.
	 * @param targetValues List of target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param executorService Executor to be used for concurrent valuation of the derivatives. This is only performed if setDerivative is not overwritten. <i>Warning</i>: The implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(final List<Number> initialParameters, final List<Number> targetValues, final int maxIteration, final ExecutorService executorService) {
		this(numberListToDoubleArray(initialParameters), numberListToDoubleArray(targetValues), maxIteration, executorService);
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param initialParameters Initial value for the parameters where the solver starts its search.
	 * @param targetValues Target values to achieve.
	 * @param maxIteration Maximum number of iterations.
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(final List<Number> initialParameters, final List<Number> targetValues, final int maxIteration, final int numberOfThreads) {
		this(initialParameters, targetValues, maxIteration, null);

		this.numberOfThreads = numberOfThreads;
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 */
	public LevenbergMarquardt() {
		super();
		regularizationMethod = RegularizationMethod.LEVENBERG_MARQUARDT;
	}

	/**
	 * Convert a list of numbers to an array of doubles.
	 *
	 * @param listOfNumbers A list of numbers.
	 * @return A corresponding array of doubles executing <code>doubleValue()</code> on each element.
	 */
	private static double[] numberListToDoubleArray(final List<Number> listOfNumbers) {
		final double[] arrayOfDoubles	= new double[listOfNumbers.size()];
		for(int i=0; i<arrayOfDoubles.length; i++) {
			arrayOfDoubles[i] = listOfNumbers.get(i).doubleValue();
		}
		return arrayOfDoubles;
	}

	/**
	 * Create a Levenberg-Marquardt solver.
	 *
	 * @param numberOfThreads Maximum number of threads. <i>Warning</i>: If this number is larger than one, the implementation of setValues has to be thread safe!
	 */
	public LevenbergMarquardt(final int numberOfThreads) {
		super();
		regularizationMethod = RegularizationMethod.LEVENBERG_MARQUARDT;
		this.numberOfThreads = numberOfThreads;
	}


	/**
	 * Set the initial parameters for the solver.
	 *
	 * @param initialParameters The initial parameters.
	 * @return A self reference.
	 */
	public LevenbergMarquardt setInitialParameters(final double[] initialParameters) {
		if(done()) {
			throw new UnsupportedOperationException("Solver cannot be modified after it has run.");
		}
		this.initialParameters = initialParameters;
		return this;
	}

	/**
	 * Set the parameter step for the solver.
	 * The parameter step is used to evaluate the derivatives via
	 * finite differences, if analytic derivatives are not provided.
	 *
	 * @param parameterSteps The parameter step.
	 * @return A self reference.
	 */
	public LevenbergMarquardt setParameterSteps(final double[] parameterSteps) {
		if(done()) {
			throw new UnsupportedOperationException("Solver cannot be modified after it has run.");
		}
		this.parameterSteps = parameterSteps;
		return this;
	}

	/**
	 * Set the target values for the solver. The solver will solver the
	 * equation weights * objectiveFunction = targetValues.
	 *
	 * @param targetValues The target values.
	 * @return A self reference.
	 */
	public LevenbergMarquardt setTargetValues(final double[] targetValues) {
		if(done()) {
			throw new UnsupportedOperationException("Solver cannot be modified after it has run.");
		}
		this.targetValues = targetValues;
		return this;
	}

	/**
	 * Set the maximum number of iterations to be performed until the solver
	 * gives up.
	 *
	 * @param maxIteration The maximum number of iterations.
	 * @return A self reference.
	 */
	public LevenbergMarquardt setMaxIteration(final int maxIteration) {
		if(done()) {
			throw new UnsupportedOperationException("Solver cannot be modified after it has run.");
		}
		this.maxIteration = maxIteration;
		return this;
	}

	/**
	 * Set the weight for the objective function.
	 *
	 * @param weights The weights for the objective function.
	 * @return A self reference.
	 */
	public LevenbergMarquardt setWeights(final double[] weights) {
		if(done()) {
			throw new UnsupportedOperationException("Solver cannot be modified after it has run.");
		}
		this.weights = weights;
		return this;
	}


	/**
	 * Set the error tolerance. The solver considers the solution "found"
	 * if the error is not improving by this given error tolerance.
	 *
	 * @param errorTolerance The error tolerance.
	 * @return A self reference.
	 */
	public LevenbergMarquardt setErrorTolerance(final double errorTolerance) {
		if(done()) {
			throw new UnsupportedOperationException("Solver cannot be modified after it has run.");
		}
		errorRootMeanSquaredTolerance = errorTolerance;
		return this;
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
	 * @return Self reference to this optimizer.
	 */
	public LevenbergMarquardt setLambda(final double lambda) {
		this.lambda = lambda;
		return this;
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
	public double[] getBestFitParameters() {
		return parameterCurrent;
	}

	@Override
	public double getRootMeanSquaredError() {
		return Math.sqrt(errorMeanSquaredCurrent);
	}

	/**
	 * @param errorMeanSquaredCurrent the errorMeanSquaredCurrent to set
	 */
	private void setErrorMeanSquaredCurrent(final double errorMeanSquaredCurrent) {
		this.errorMeanSquaredCurrent = errorMeanSquaredCurrent;
	}

	@Override
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
	public void setDerivatives(final double[] parameters, final double[][] derivatives) throws SolverException {
		// Calculate new derivatives. Note that this method is called only with
		// parameters = parameterCurrent, so we may use valueCurrent.

		final Vector<Future<double[]>> valueFutures = new Vector<>(parameterCurrent.length);
		for (int parameterIndex = 0; parameterIndex < parameterCurrent.length; parameterIndex++) {
			final double[] parametersNew	= parameters.clone();
			final double[] derivative		= derivatives[parameterIndex];

			final int workerParameterIndex = parameterIndex;
			final Callable<double[]> worker = new  Callable<double[]>() {
				@Override
				public double[] call() {
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

					// Calculate derivative as (valueUpShift - valueCurrent) / parameterFiniteDifference
					try {
						setValues(parametersNew, derivative);
					} catch (final Exception e) {
						logger.severe("Valuation failed with exaption " + e.getMessage() + "\n" + e.getStackTrace());
						// We signal an exception to calculate the derivative as NaN
						Arrays.fill(derivative, Double.NaN);
					}
					for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
						derivative[valueIndex] -= valueCurrent[valueIndex];
						derivative[valueIndex] /= parameterFiniteDifference;
						if(Double.isNaN(derivative[valueIndex])) {
							derivative[valueIndex] = 0.0;
						}
					}
					return derivative;
				}
			};
			if(executor != null) {
				final Future<double[]> valueFuture = executor.submit(worker);
				valueFutures.add(parameterIndex, valueFuture);
			}
			else {
				final FutureTask<double[]> valueFutureTask = new FutureTask<>(worker);
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
				(errorRootMeanSquaredChange <= errorRootMeanSquaredTolerance)
				||
				/*
				 * Lambda is infinite, i.e., no new point is acceptable.
				 * For example, this may happen if setValue repeatedly give contains invalid (NaN) values.
				 */
				Double.isInfinite(lambda);
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
			parameterIncrement	= new double[numberOfParameters];
			parameterCurrent	= new double[numberOfParameters];

			valueTest			= new double[numberOfValues];
			valueCurrent		= new double[numberOfValues];
			derivativeCurrent	= new double[parameterCurrent.length][valueCurrent.length];

			hessianMatrix = new double[parameterCurrent.length][parameterCurrent.length];
			beta = new double[parameterCurrent.length];

			iteration = 0;

			while(true) {
				// Count iterations
				iteration++;

				// Calculate values for test parameters
				setValues(parameterTest, valueTest);

				// Calculate error
				final double errorMeanSquaredTest = getMeanSquaredError(valueTest);

				/*
				 * Note: The following test will be false if errorMeanSquaredTest is NaN.
				 * That is: NaN is consider as a rejected point.
				 */
				if(errorMeanSquaredTest < errorMeanSquaredCurrent) {
					errorRootMeanSquaredChange = Math.sqrt(errorMeanSquaredCurrent) - Math.sqrt(errorMeanSquaredTest);

					// Accept point
					System.arraycopy(parameterTest, 0, parameterCurrent, 0, parameterCurrent.length);
					System.arraycopy(valueTest, 0, valueCurrent, 0, valueCurrent.length);
					errorMeanSquaredCurrent		= errorMeanSquaredTest;

					// Derivative has to be recalculated
					isParameterCurrentDerivativeValid = false;

					// Decrease lambda (move faster)
					lambda			/= lambdaDivisor;
				} else {
					errorRootMeanSquaredChange = Math.sqrt(errorMeanSquaredTest) - Math.sqrt(errorMeanSquaredCurrent);

					// Reject point, increase lambda (move slower)
					lambda				*= lambdaMultiplicator;
				}

				// Update a new parameter trial, if we are not done
				if (!done()) {
					updateParameterTest();
				} else {
					break;
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

	public double getMeanSquaredError(final double[] value) {
		double error = 0.0;

		for (int valueIndex = 0; valueIndex < value.length; valueIndex++) {
			final double deviation = value[valueIndex] - targetValues[valueIndex];
			error += weights[valueIndex] * deviation * deviation;
		}

		return error/value.length;
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
			// Build matrix H (Hessian approximation)
			for (int i = 0; i < parameterCurrent.length; i++) {
				for (int j = i; j < parameterCurrent.length; j++) {
					double alphaElement = 0.0;
					for (int valueIndex = 0; valueIndex < valueCurrent.length; valueIndex++) {
						alphaElement += weights[valueIndex] * derivativeCurrent[i][valueIndex] * derivativeCurrent[j][valueIndex];
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
				final double[] derivativeCurrentSingleParam = derivativeCurrent[i];
				for (int k = 0; k < valueCurrent.length; k++) {
					betaElement += weights[k] * (targetValues[k] - valueCurrent[k]) * derivativeCurrentSingleParam[k];
				}
				beta[i] = betaElement;
			}

			try {
				// Calculate new increment
				//				parameterIncrement = LinearAlgebra.solveLinearEquationLeastSquare(hessianMatrix, beta);
				parameterIncrement = LinearAlgebra.solveLinearEquationSymmetric(hessianMatrix, beta);
			} catch (final Exception e) {
				hessianInvalid	= true;
				lambda			*= 16;
			}
		}

		// Calculate new parameter
		for (int i = 0; i < parameterCurrent.length; i++) {
			parameterTest[i] = parameterCurrent[i] + parameterIncrement[i];
		}
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer.
	 *
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(double[], double[])} and
	 * that of {@link #setDerivatives(double[], double[][])} is reused.
	 */
	@Override
	public LevenbergMarquardt clone() throws CloneNotSupportedException {
		final LevenbergMarquardt clonedOptimizer = (LevenbergMarquardt)super.clone();
		clonedOptimizer.isParameterCurrentDerivativeValid = false;
		clonedOptimizer.iteration = 0;
		clonedOptimizer.errorMeanSquaredCurrent	= Double.POSITIVE_INFINITY;
		clonedOptimizer.errorRootMeanSquaredChange	= Double.POSITIVE_INFINITY;
		return clonedOptimizer;
	}

	/**
	 * Create a clone of this LevenbergMarquardt optimizer with a new vector for the
	 * target values and weights.
	 *
	 * The clone will use the same objective function than this implementation,
	 * i.e., the implementation of {@link #setValues(double[], double[])} and
	 * that of {@link #setDerivatives(double[], double[][])} is reused.
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
	public LevenbergMarquardt getCloneWithModifiedTargetValues(final double[] newTargetVaues, final double[] newWeights, final boolean isUseBestParametersAsInitialParameters) throws CloneNotSupportedException {
		final LevenbergMarquardt clonedOptimizer = clone();
		clonedOptimizer.targetValues = newTargetVaues.clone();		// Defensive copy
		clonedOptimizer.weights = newWeights.clone();				// Defensive copy

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
	 * i.e., the implementation of {@link #setValues(double[], double[])} and
	 * that of {@link #setDerivatives(double[], double[][])} is reused.
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
	public LevenbergMarquardt getCloneWithModifiedTargetValues(final List<Number> newTargetVaues, final List<Number> newWeights, final boolean isUseBestParametersAsInitialParameters) throws CloneNotSupportedException {
		final LevenbergMarquardt clonedOptimizer = clone();
		clonedOptimizer.targetValues = numberListToDoubleArray(newTargetVaues);
		clonedOptimizer.weights = numberListToDoubleArray(newWeights);

		if(isUseBestParametersAsInitialParameters && this.done()) {
			clonedOptimizer.initialParameters = this.getBestFitParameters();
		}

		return clonedOptimizer;
	}
}
