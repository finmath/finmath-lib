/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 24.12.2016
 */
package net.finmath.montecarlo.interestrate.modelplugins;

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
import net.finmath.math.optimizer.OptimizerFactoryInterface;
import net.finmath.math.optimizer.OptimizerFactoryLevenbergMarquardt;
import net.finmath.math.optimizer.OptimizerInterface;
import net.finmath.math.optimizer.SolverException;
import net.finmath.math.optimizer.OptimizerInterface.ObjectiveFunction;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.TermStructureModelInterface;
import net.finmath.montecarlo.interestrate.TermStructureModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.process.ProcessEulerScheme;

/**
 * A base class and interface description for the instantaneous covariance of
 * an forward rate interest rate model.
 * 
 * @author Christian Fries
 */
public abstract class TermStructureCovarianceModelParametric implements TermStructureCovarianceModelInterface, TermStructureTenorTimeScalingInterface, TermStructureFactorLoadingsModelParametricInterface {

	private static final Logger logger = Logger.getLogger("net.finmath");

	/**
	 * Get the parameters of determining this parametric
	 * covariance model. The parameters are usually free parameters
	 * which may be used in calibration.
	 * 
	 * @return Parameter vector.
	 */
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
	public abstract TermStructureCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters);

	/**
	 * Return a calibrated clone of the covariance model.
	 * 
	 * @param calibrationModel Model to be used for the calibration.
	 * @param calibrationProducts Vector of calibration products.
	 * @param calibrationTargetValues Vector of corresponding target values.
	 * @param calibrationWeights Vector of corresponding weights.
	 * @param calibrationParameters Property map of calibration parameters.
	 * @return A clone of this model, using the calibrated parameters.
	 * @throws CalculationException Exception indicating failure in calibration.
	 */
	public TermStructureCovarianceModelParametric getCloneCalibrated(final TermStructureModelInterface calibrationModel, final AbstractLIBORMonteCarloProduct[] calibrationProducts, final double[] calibrationTargetValues, final double[] calibrationWeights, Map<String, Object> calibrationParameters) throws CalculationException {

		if(calibrationParameters == null) calibrationParameters = new HashMap<String,Object>();
		Integer numberOfPathsParameter	= (Integer)calibrationParameters.get("numberOfPaths");
		Integer seedParameter			= (Integer)calibrationParameters.get("seed");
		Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		Double	parameterStepParameter	= (Double)calibrationParameters.get("parameterStep");
		Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		BrownianMotionInterface brownianMotionParameter	= (BrownianMotionInterface)calibrationParameters.get("brownianMotion");

		double[] initialParameters = this.getParameter();
		double[] lowerBound = new double[initialParameters.length];
		double[] upperBound = new double[initialParameters.length];
		double[] parameterStep = new double[initialParameters.length];
		double[] zero = new double[calibrationTargetValues.length];
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
		int numberOfThreads = 2;
		OptimizerFactoryInterface optimizerFactoryParameter = (OptimizerFactoryInterface)calibrationParameters.get("optimizerFactory");
		
		int numberOfPaths	= numberOfPathsParameter != null ? numberOfPathsParameter.intValue() : 2000;
		int seed			= seedParameter != null ? seedParameter.intValue() : 31415;
		int maxIterations	= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 400;
		double accuracy		= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-7;
		final BrownianMotionInterface brownianMotion = brownianMotionParameter != null ? brownianMotionParameter : new BrownianMotion(calibrationModel.getProcess().getStochasticDriver().getTimeDiscretization(), getNumberOfFactors(), numberOfPaths, seed);
		OptimizerFactoryInterface optimizerFactory = optimizerFactoryParameter != null ? optimizerFactoryParameter : new OptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

		int numberOfThreadsForProductValuation = 2 * Math.max(2, Runtime.getRuntime().availableProcessors());
		final ExecutorService executor = null;//Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

		ObjectiveFunction calibrationError = new ObjectiveFunction() {			
			// Calculate model values for given parameters
			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {

				TermStructureCovarianceModelParametric calibrationCovarianceModel = TermStructureCovarianceModelParametric.this.getCloneWithModifiedParameters(parameters);

				// Create a term structure model with the new covariance structure.
				HashMap<String, Object> data = new HashMap<String, Object>();
				data.put("covarianceModel", calibrationCovarianceModel);
				TermStructureModelInterface model;
				try {
					model = calibrationModel.getCloneWithModifiedData(data);
				} catch (CalculationException e) {
					throw new SolverException(e);
				}
				ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
				final TermStructureModelMonteCarloSimulation termStructureModelMonteCarloSimulation =  new TermStructureModelMonteCarloSimulation(model, process);

				ArrayList<Future<Double>> valueFutures = new ArrayList<Future<Double>>(calibrationProducts.length);
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					final int workerCalibrationProductIndex = calibrationProductIndex;
					Callable<Double> worker = new  Callable<Double>() {
						public Double call() throws SolverException {
							try {
								return calibrationWeights[workerCalibrationProductIndex] * (calibrationProducts[workerCalibrationProductIndex].getValue(termStructureModelMonteCarloSimulation) - calibrationTargetValues[workerCalibrationProductIndex]);
							} catch (CalculationException e) {
								// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
								return new Double(0.0);
							} catch (Exception e) {
								// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
								return new Double(0.0);
							}
						}
					};
					if(executor != null) {
						Future<Double> valueFuture = executor.submit(worker);
						valueFutures.add(calibrationProductIndex, valueFuture);
					}
					else {
						FutureTask<Double> valueFutureTask = new FutureTask<Double>(worker);
						valueFutureTask.run();
						valueFutures.add(calibrationProductIndex, valueFutureTask);
					}
				}
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					try {
						double value = valueFutures.get(calibrationProductIndex).get();
						values[calibrationProductIndex] = value;
					}
					catch (InterruptedException e) {
						throw new SolverException(e);
					} catch (ExecutionException e) {
						throw new SolverException(e);
					}
				}

				double error = 0.0;

				for (int valueIndex = 0; valueIndex < values.length; valueIndex++) {
					double deviation = values[valueIndex];
					error += deviation * deviation;
				}

				System.out.println(Math.sqrt(error/values.length));
			}
		};

		OptimizerInterface optimizer = optimizerFactory.getOptimizer(calibrationError, initialParameters, lowerBound, upperBound, parameterStep, zero);
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
		TermStructureCovarianceModelParametric calibrationCovarianceModel = this.getCloneWithModifiedParameters(bestParameters);

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
