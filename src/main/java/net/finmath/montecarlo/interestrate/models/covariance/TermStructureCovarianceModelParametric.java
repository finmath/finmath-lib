/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.12.2016
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.TermStructureModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationFromTermStructureModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.optimizer.Optimizer;
import net.finmath.optimizer.Optimizer.ObjectiveFunction;
import net.finmath.optimizer.OptimizerFactory;
import net.finmath.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.optimizer.SolverException;

/**
 * A base class and interface description for the instantaneous covariance of
 * an forward rate interest rate model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class TermStructureCovarianceModelParametric implements TermStructureCovarianceModel, TermStructureTenorTimeScaling, TermStructureFactorLoadingsModelParametric {

	private static final Logger logger = Logger.getLogger("net.finmath");

	/**
	 * Get the parameters of determining this parametric
	 * covariance model. The parameters are usually free parameters
	 * which may be used in calibration.
	 *
	 * @return Parameter vector.
	 */
	@Override
	public abstract double[]	getParameter();

	@Override
	public abstract TermStructureCovarianceModelParametric clone();

	/**
	 * Return an instance of this model using a new set of parameters.
	 * Note: To improve performance it is admissible to return the same instance of the object given that the parameters have not changed. Models should be immutable.
	 *
	 * @param parameters The new set of parameters.
	 * @return An instance of AbstractLIBORCovarianceModelParametric with modified parameters.
	 */
	@Override
	public abstract TermStructureCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters);

	/**
	 * Return a calibrated clone of the covariance model.
	 *
	 * @param calibrationModel Model to be used for the calibration.
	 * @param calibrationProducts Vector of calibration products.
	 * @param calibrationParameters Property map of calibration parameters.
	 * @return A clone of this model, using the calibrated parameters.
	 * @throws CalculationException Exception indicating failure in calibration.
	 */
	public TermStructureCovarianceModelParametric getCloneCalibrated(final TermStructureModel calibrationModel, final CalibrationProduct[] calibrationProducts, Map<String, Object> calibrationParameters) throws CalculationException {

		if(calibrationParameters == null) {
			calibrationParameters = new HashMap<>();
		}
		final Integer numberOfPathsParameter	= (Integer)calibrationParameters.get("numberOfPaths");
		final Integer seedParameter			= (Integer)calibrationParameters.get("seed");
		final Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		final Double	parameterStepParameter	= (Double)calibrationParameters.get("parameterStep");
		final Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		final BrownianMotion brownianMotionParameter	= (BrownianMotion)calibrationParameters.get("brownianMotion");
		if(brownianMotionParameter == null) {
			throw new IllegalArgumentException("Calibration requires a Brownian motion to be specified under the key 'brownianMotion'.");
		}

		final double[] initialParameters = this.getParameter();
		final double[] lowerBound = new double[initialParameters.length];
		final double[] upperBound = new double[initialParameters.length];
		final double[] parameterStep = new double[initialParameters.length];
		final double[] zero = new double[calibrationProducts.length];
		Arrays.fill(lowerBound, 0);
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
		final BrownianMotion brownianMotion = brownianMotionParameter;
		final OptimizerFactory optimizerFactory = optimizerFactoryParameter != null ? optimizerFactoryParameter : new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

		final int numberOfThreadsForProductValuation = 2 * Math.max(2, Runtime.getRuntime().availableProcessors());
		final ExecutorService executor = null;//Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

		final ObjectiveFunction calibrationError = new ObjectiveFunction() {
			// Calculate model values for given parameters
			@Override
			public void setValues(final double[] parameters, final double[] values) throws SolverException {

				final TermStructureCovarianceModelParametric calibrationCovarianceModel = TermStructureCovarianceModelParametric.this.getCloneWithModifiedParameters(parameters);

				// Create a term structure model with the new covariance structure.
				final HashMap<String, Object> data = new HashMap<>();
				data.put("covarianceModel", calibrationCovarianceModel);
				TermStructureModel model;
				try {
					model = calibrationModel.getCloneWithModifiedData(data);
				} catch (final CalculationException e) {
					throw new SolverException(e);
				}
				final EulerSchemeFromProcessModel process = new EulerSchemeFromProcessModel(model, brownianMotion);
				final TermStructureMonteCarloSimulationFromTermStructureModel lIBORMonteCarloSimulationFromTermStructureModel =  new TermStructureMonteCarloSimulationFromTermStructureModel(model, process);

				final ArrayList<Future<Double>> valueFutures = new ArrayList<>(calibrationProducts.length);
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					final int workerCalibrationProductIndex = calibrationProductIndex;
					final Callable<Double> worker = new  Callable<Double>() {
						@Override
						public Double call() {
							try {
								return calibrationProducts[workerCalibrationProductIndex].getProduct().getValue(0.0,lIBORMonteCarloSimulationFromTermStructureModel).sub(calibrationProducts[workerCalibrationProductIndex].getTargetValue()).mult(calibrationProducts[workerCalibrationProductIndex].getWeight()).getAverage();
							} catch (final Exception e) {
								// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
								return 0.0;
							}
						}
					};
					if(executor != null) {
						final Future<Double> valueFuture = executor.submit(worker);
						valueFutures.add(calibrationProductIndex, valueFuture);
					}
					else {
						final FutureTask<Double> valueFutureTask = new FutureTask<>(worker);
						valueFutureTask.run();
						valueFutures.add(calibrationProductIndex, valueFutureTask);
					}
				}
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					try {
						final double value = valueFutures.get(calibrationProductIndex).get();
						values[calibrationProductIndex] = value;
					}
					catch (final InterruptedException | ExecutionException e) {
						throw new SolverException(e);
					}
				}

				double error = 0.0;

				for (int valueIndex = 0; valueIndex < values.length; valueIndex++) {
					final double deviation = values[valueIndex];
					error += deviation * deviation;
				}

				System.out.println(Math.sqrt(error/values.length));
			}
		};

		final Optimizer optimizer = optimizerFactory.getOptimizer(calibrationError, initialParameters, lowerBound, upperBound, parameterStep, zero);
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
		final double[] bestParameters = optimizer.getBestFitParameters();
		final TermStructureCovarianceModelParametric calibrationCovarianceModel = this.getCloneWithModifiedParameters(bestParameters);

		// Diagnostic output
		if (logger.isLoggable(Level.FINE)) {
			logger.fine("The solver required " + optimizer.getIterations() + " iterations. The best fit parameters are:");

			String logString = "Best parameters:";
			for(int i=0; i<bestParameters.length; i++) {
				logString += "\tparameter["+i+"]: " + bestParameters[i];
			}
			logger.fine(logString);
		}

		return calibrationCovarianceModel;
	}
}
