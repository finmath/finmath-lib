/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORMonteCarloSimulationFromLIBORModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.optimizer.Optimizer;
import net.finmath.optimizer.Optimizer.ObjectiveFunction;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.optimizer.StochasticOptimizer;
import net.finmath.optimizer.StochasticOptimizerFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * Base class for parametric covariance models, see also {@link AbstractLIBORCovarianceModel}.
 *
 * Parametric models feature a parameter vector which can be inspected
 * and modified for calibration purposes.
 *
 * The parameter vector may have zero length, which indicated that the model
 * is not calibrateable.
 *
 * This class includes the implementation of a generic calibration algorithm.
 * If you provide an arbitrary list of calibration products, the class can return
 * a new instance where the parameters are chosen such that the (weighted) root-mean-square
 * error of the difference of the value of the calibration products and given target
 * values is minimized.
 *
 * @author Christian Fries
 * @date 20.05.2006
 * @date 23.02.2014
 * @version 1.1
 */
public abstract class AbstractLIBORCovarianceModelParametric extends AbstractLIBORCovarianceModel implements LIBORCovarianceModelCalibrateable {

	private static final long serialVersionUID = 7015719361182945464L;

	private static final Logger logger = Logger.getLogger("net.finmath");

	/**
	 * Constructor consuming time discretizations, which are handled by the super class.
	 *
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 * @param numberOfFactors The number of factors to use (a factor reduction is performed)
	 */
	public AbstractLIBORCovarianceModelParametric(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);
	}

	/**
	 * Get the parameters of determining this parametric
	 * covariance model. The parameters are usually free parameters
	 * which may be used in calibration.
	 *
	 * @return Parameter vector.
	 */
	public RandomVariable[]	getParameter() {
		final double[] parameterAsDouble = this.getParameterAsDouble();
		final RandomVariable[] parameter = new RandomVariable[parameterAsDouble.length];
		for(int i=0; i<parameter.length; i++) {
			parameter[i] = new Scalar(parameterAsDouble[i]);
		}
		return parameter;
	}

	/**
	 * Get the parameters of determining this parametric
	 * covariance model. The parameters are usually free parameters
	 * which may be used in calibration.
	 *
	 * @return Parameter vector.
	 */
	public abstract double[]	getParameterAsDouble();

	@Override
	public abstract Object clone();

	/**
	 * Return an instance of this model using a new set of parameters.
	 * Note: To improve performance it is admissible to return the same instance of the object given that the parameters have not changed. Models should be immutable.
	 *
	 * @param parameters The new set of parameters.
	 * @return An instance of AbstractLIBORCovarianceModelParametric with modified parameters.
	 */
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(final RandomVariable[] parameters) {
		final double[] parameterAsDouble = new double[parameters.length];
		for(int i=0; i<parameterAsDouble.length; i++) {
			parameterAsDouble[i] = parameters[i].doubleValue();
		}
		return getCloneWithModifiedParameters(parameterAsDouble);
	}

	/**
	 * Return an instance of this model using a new set of parameters.
	 * Note: To improve performance it is admissible to return the same instance of the object given that the parameters have not changed. Models should be immutable.
	 *
	 * @param parameters The new set of parameters.
	 * @return An instance of AbstractLIBORCovarianceModelParametric with modified parameters.
	 */
	public abstract AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters);

	public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModel calibrationModel, final CalibrationProduct[] calibrationProducts) throws CalculationException {
		return getCloneCalibrated(calibrationModel, calibrationProducts, null);
	}

	/**
	 * Performs a generic calibration of the parametric model by trying to match a given vector of calibration product to a given vector of target values
	 * using a given vector of weights.
	 *
	 * Optional calibration parameters may be passed using the map calibrationParameters. The keys are (<code>String</code>s):
	 * <ul>
	 * 	<li><code>brownianMotion</code>: Under this key an object implementing {@link net.finmath.montecarlo.BrownianMotion} may be provided. If so, this Brownian motion is used to build the valuation model.</li>
	 * 	<li><code>maxIterations</code>: Under this key an object of type Integer may be provided specifying the maximum number of iterations.</li>
	 * 	<li><code>accuracy</code>: Under this key an object of type Double may be provided specifying the desired accuracy. Note that this is understood in the sense that the solver will stop if the iteration does not improve by more than this number.</li>
	 * </ul>
	 *
	 * @param calibrationModel The LIBOR market model to be used for calibrations (specifies forward curve and tenor discretization).
	 * @param calibrationProducts The array of calibration products.
	 * @param calibrationParameters A map of type Map&lt;String, Object&gt; specifying some (optional) calibration parameters.
	 * @return A new parametric model of the same type than <code>this</code> one, but with calibrated parameters.
	 * @throws CalculationException Thrown if calibration has failed.
	 */
	@Override
	public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModel calibrationModel, final CalibrationProduct[] calibrationProducts, Map<String,Object> calibrationParameters) throws CalculationException {

		if(calibrationParameters == null) {
			calibrationParameters = new HashMap<>();
		}

		final int numberOfPaths	= (Integer)calibrationParameters.getOrDefault("numberOfPaths", 2000);
		final int seed			= (Integer)calibrationParameters.getOrDefault("seed", 31415);
		final int maxIterations	= (Integer)calibrationParameters.getOrDefault("maxIterations", 400);
		final double accuracy		= (Double)calibrationParameters.getOrDefault("accuracy", 1E-7);
		final BrownianMotion brownianMotion = (BrownianMotion)calibrationParameters.getOrDefault("brownianMotion", new BrownianMotionFromMersenneRandomNumbers(getTimeDiscretization(), getNumberOfFactors(), numberOfPaths, seed));

		final RandomVariable[] initialParameters = this.getParameter();
		final RandomVariable[] lowerBound = new RandomVariable[initialParameters.length];
		final RandomVariable[] upperBound = new RandomVariable[initialParameters.length];
		final RandomVariable[] parameterStep = new RandomVariable[initialParameters.length];
		Arrays.fill(lowerBound, new RandomVariableFromDoubleArray(Double.NEGATIVE_INFINITY));
		Arrays.fill(upperBound, new RandomVariableFromDoubleArray(Double.POSITIVE_INFINITY));
		final Double	parameterStepParameter	= (Double)calibrationParameters.get("parameterStep");
		Arrays.fill(parameterStep,  new RandomVariableFromDoubleArray(parameterStepParameter != null ? parameterStepParameter.doubleValue() : 1E-4));

		final int numberOfThreadsForProductValuation = Runtime.getRuntime().availableProcessors();
		final ExecutorService executor = Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

		final StochasticOptimizer.ObjectiveFunction calibrationError = new StochasticOptimizer.ObjectiveFunction() {
			// Calculate model values for given parameters
			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) throws SolverException {

				final AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = AbstractLIBORCovarianceModelParametric.this.getCloneWithModifiedParameters(parameters);
				//				System.arraycopy(calibrationCovarianceModel.getParameter(), 0, parameters, 0, parameters.length);

				// Create a LIBOR market model with the new covariance structure.
				final LIBORMarketModel model = calibrationModel.getCloneWithModifiedCovarianceModel(calibrationCovarianceModel);
				final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);
				final LIBORMonteCarloSimulationFromLIBORModel liborMarketModelMonteCarloSimulation =  new LIBORMonteCarloSimulationFromLIBORModel(model, process);

				final ArrayList<Future<RandomVariable>> valueFutures = new ArrayList<>(calibrationProducts.length);
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					final int workerCalibrationProductIndex = calibrationProductIndex;
					final Callable<RandomVariable> worker = new  Callable<RandomVariable>() {
						@Override
						public RandomVariable call() {
							try {
								return calibrationProducts[workerCalibrationProductIndex].getProduct().getValue(0.0, liborMarketModelMonteCarloSimulation).sub(calibrationProducts[workerCalibrationProductIndex].getTargetValue()).mult(calibrationProducts[workerCalibrationProductIndex].getWeight());
							} catch (final Exception e) {
								// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
								return new Scalar(0.0);
							}
						}
					};
					if(executor != null) {
						final Future<RandomVariable> valueFuture = executor.submit(worker);
						valueFutures.add(calibrationProductIndex, valueFuture);
					}
					else {
						final FutureTask<RandomVariable> valueFutureTask = new FutureTask<>(worker);
						valueFutureTask.run();
						valueFutures.add(calibrationProductIndex, valueFutureTask);
					}
				}
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					try {
						final RandomVariable value = valueFutures.get(calibrationProductIndex).get();
						values[calibrationProductIndex] = value;
					}
					catch (final InterruptedException | ExecutionException e) {
						throw new SolverException(e);
					}
				}
			}
		};

		/*
		 * We allow for 2 simultaneous calibration models.
		 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
		 * one model with 2 times the number of paths. In the case of an analytic calibration
		 * memory requirement is not the limiting factor.
		 */
		final int numberOfThreads = 2;
		final Object optimizerFactory = calibrationParameters.getOrDefault("optimizerFactory", new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads));
		//		Object optimizerFactory = calibrationParameters.getOrDefault("optimizerFactory", new StochasticPathwiseOptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads));

		/*
		 * Obtain besterParameters and numberOfIterations
		 */
		AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = null;
		if(optimizerFactory instanceof StochasticOptimizerFactory) {
			final RandomVariable[] zerosForTargetValues = new RandomVariable[calibrationProducts.length];
			Arrays.fill(zerosForTargetValues, new RandomVariableFromDoubleArray(0.0));
			final StochasticOptimizer optimizer = ((StochasticOptimizerFactory)optimizerFactory).getOptimizer(calibrationError, initialParameters, lowerBound, upperBound, parameterStep, zerosForTargetValues);
			try {
				optimizer.run();
			}
			catch(final SolverException e) {
				throw new CalculationException(e);
			}
			finally {
				if(executor != null) {
					executor.shutdown();
				}
			}

			// Get covariance model corresponding to the best parameter set.
			final RandomVariable[] bestParameters = optimizer.getBestFitParameters();
			final int numberOfIterations = optimizer.getIterations();
			calibrationCovarianceModel = this.getCloneWithModifiedParameters(bestParameters);

			// Diagnostic output
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("The solver required " + optimizer.getIterations() + " iterations. The best fit parameters are:");

				String logString = "Best parameters:";
				for(int i=0; i<bestParameters.length; i++) {
					logString += "\tparameter["+i+"]: " + bestParameters[i];
				}
				logger.fine(logString);
			}
		}
		else if(optimizerFactory instanceof OptimizerFactory) {
			return getCloneCalibratedLegazy(calibrationModel, calibrationProducts, calibrationParameters);
			/*
			double[] zerosForTargetValues = new double[calibrationProducts.length];
			Arrays.fill(zerosForTargetValues, 0.0);
			Optimizer optimizer = ((OptimizerFactory)optimizerFactory).getOptimizer(calibrationError, initialParameters, lowerBound, upperBound, parameterStep, zerosForTargetValues);
			try {
				optimizer.run();
			}
			catch(SolverException e) {
				throw new CalculationException(e);
			}
			finally {
				if(executor != null) {
					executor.shutdown();
				}
			}

			// Get covariance model corresponding to the best parameter set.
			double[] bestParameters = optimizer.getBestFitParameters();
			int numberOfIterations = optimizer.getIterations();
			calibrationCovarianceModel = this.getCloneWithModifiedParameters(bestParameters);

			// Diagnostic output
			if (logger.isLoggable(Level.FINE)) {
				logger.fine("The solver required " + optimizer.getIterations() + " iterations. The best fit parameters are:");

				String logString = "Best parameters:";
				for(int i=0; i<bestParameters.length; i++) {
					logString += "\tparameter["+i+"]: " + bestParameters[i];
				}
				logger.fine(logString);
			}
			 */
		}
		else {
			throw new IllegalArgumentException(optimizerFactory + " not supported.");
		}

		return calibrationCovarianceModel;
	}

	static class FutureTaskWithPriority<T> extends FutureTask<T> implements Comparable<FutureTaskWithPriority<T>> {
		private final int priority;
		FutureTaskWithPriority(final Callable<T> callable, final int priority) {
			super(callable);
			this.priority = priority;
		}
		public int getPriority() {
			return priority;
		}
		@Override
		public int compareTo(final FutureTaskWithPriority<T> o) {
			return this.getPriority() < o.getPriority() ? -1 : this.getPriority() == o.getPriority() ? 0 : 1;
		}

	}

	public AbstractLIBORCovarianceModelParametric getCloneCalibratedLegazy(final LIBORMarketModel calibrationModel, final CalibrationProduct[] calibrationProducts, Map<String,Object> calibrationParameters) throws CalculationException {

		if(calibrationParameters == null) {
			calibrationParameters = new HashMap<>();
		}
		final Integer numberOfPathsParameter	= (Integer)calibrationParameters.get("numberOfPaths");
		final Integer seedParameter			= (Integer)calibrationParameters.get("seed");
		final Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		final Double	parameterStepParameter	= (Double)calibrationParameters.get("parameterStep");
		final Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		final BrownianMotion brownianMotionParameter	= (BrownianMotion)calibrationParameters.get("brownianMotion");

		final double[] initialParameters = this.getParameterAsDouble();
		final double[] lowerBound = new double[initialParameters.length];
		final double[] upperBound = new double[initialParameters.length];
		final double[] parameterStep = new double[initialParameters.length];
		final double[] zero = new double[calibrationProducts.length];
		Arrays.fill(lowerBound, Double.NEGATIVE_INFINITY);
		Arrays.fill(upperBound, Double.POSITIVE_INFINITY);
		Arrays.fill(parameterStep, parameterStepParameter != null ? parameterStepParameter.doubleValue() : 1E-4);
		Arrays.fill(zero, 0);

		/*
		 * We allow for 2 simultaneous calibration models.
		 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
		 * one model with 2 times the number of paths. In the case of an analytic calibration
		 * memory requirement is not the limiting factor.
		 */
		final int numberOfThreads = 2;
		final OptimizerFactory optimizerFactoryParameter = (OptimizerFactory)calibrationParameters.get("optimizerFactory");

		final int numberOfPaths	= numberOfPathsParameter != null ? numberOfPathsParameter.intValue() : 2000;
		final int seed			= seedParameter != null ? seedParameter.intValue() : 31415;
		final int maxIterations	= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 400;
		final double accuracy		= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-7;
		final BrownianMotion brownianMotion = brownianMotionParameter != null ? brownianMotionParameter : new BrownianMotionFromMersenneRandomNumbers(getTimeDiscretization(), getNumberOfFactors(), numberOfPaths, seed);
		final OptimizerFactory optimizerFactory = optimizerFactoryParameter != null ? optimizerFactoryParameter : new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

		final PriorityBlockingQueue<Runnable> queue = new PriorityBlockingQueue<>();
		final ExecutorService executorForProductValuation = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors()*zero.length, 5, TimeUnit.SECONDS, queue);

		final ObjectiveFunction calibrationError = new ObjectiveFunction() {
			// Calculate model values for given parameters
			@Override
			public void setValues(final double[] parameters, final double[] values) throws SolverException {

				final AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = AbstractLIBORCovarianceModelParametric.this.getCloneWithModifiedParameters(parameters);
				//				System.arraycopy(calibrationCovarianceModel.getParameterAsDouble(), 0, parameters, 0, parameters.length);

				// Create a LIBOR market model with the new covariance structure.
				final LIBORMarketModel model = calibrationModel.getCloneWithModifiedCovarianceModel(calibrationCovarianceModel);
				final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);
				final LIBORMonteCarloSimulationFromLIBORModel liborMarketModelMonteCarloSimulation =  new LIBORMonteCarloSimulationFromLIBORModel(process);

				final ArrayList<Future<RandomVariable>> valueFutures = new ArrayList<>(calibrationProducts.length);
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					final int workerCalibrationProductIndex = calibrationProductIndex;

					// Define the task to be executed in parallel
					final FutureTaskWithPriority<RandomVariable> valueFuture = new FutureTaskWithPriority<>(
							new Callable<RandomVariable>() {
								@Override
								public RandomVariable call() throws Exception {
									try {
										return calibrationProducts[workerCalibrationProductIndex].getProduct().getValue(0.0, liborMarketModelMonteCarloSimulation).sub(calibrationProducts[workerCalibrationProductIndex].getTargetValue()).mult(calibrationProducts[workerCalibrationProductIndex].getWeight());
									} catch(final Exception e) {
										// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
										return null;
									}
								}
							},
							calibrationProducts[workerCalibrationProductIndex].getPriority());

					if(executorForProductValuation != null) {
						executorForProductValuation.execute(valueFuture);
						valueFutures.add(calibrationProductIndex, valueFuture);
					}
					else {
						valueFuture.run();
						valueFutures.add(calibrationProductIndex, valueFuture);
					}
				}
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					try {
						final RandomVariable value = valueFutures.get(calibrationProductIndex).get();
						values[calibrationProductIndex] = value != null ? value.getAverage() : 0.0;
					}
					catch (final InterruptedException | ExecutionException e) {
						throw new SolverException(e);
					}
				}
			}
		};

		final Optimizer optimizer = optimizerFactory.getOptimizer(calibrationError, initialParameters, lowerBound, upperBound, parameterStep, zero);
		try {
			optimizer.run();

			// Diagnostic output
			if (logger.isLoggable(Level.FINE)) {
				final Format formatterSci3 = new DecimalFormat("+0.###E0;-0.###E0");

				logger.fine("The solver required " + optimizer.getIterations() + " iterations. The best fit parameters are:");

				final double[] bestParameters = optimizer.getBestFitParameters();
				String logString = "Best parameters:";
				for(int i=0; i<bestParameters.length; i++) {
					logString += "\tparameter["+i+"]: " + bestParameters[i];
				}
				logger.fine(logString);

				final double[] bestValues = new double[calibrationProducts.length];
				calibrationError.setValues(bestParameters, bestValues);
				String logString2 = "Best values:";
				for(int i=0; i<calibrationProducts.length; i++) {
					logString2 += "\n\t" + calibrationProducts[i].getName() + ": ";
					logString2 += "value["+i+"]: " + formatterSci3.format(bestValues[i]);
				}
				logger.fine(logString2);
			}

			// Get covariance model corresponding to the best parameter set.
			final double[] bestParameters = optimizer.getBestFitParameters();
			final AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = this.getCloneWithModifiedParameters(bestParameters);

			return calibrationCovarianceModel;
		}
		catch(final SolverException e) {
			throw new CalculationException(e);
		}
		catch(final Exception e) {
			e.printStackTrace();
			throw e;
		}
		finally {
			if(executorForProductValuation != null) {
				executorForProductValuation.shutdown();
			}
		}

	}

	@Override
	public String toString() {
		return "AbstractLIBORCovarianceModelParametric [getParameter()="
				+ Arrays.toString(getParameterAsDouble()) + "]";
	}
}
