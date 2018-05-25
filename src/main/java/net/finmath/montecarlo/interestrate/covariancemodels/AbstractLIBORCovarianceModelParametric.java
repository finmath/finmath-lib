/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.covariancemodels;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.optimizer.SolverException;
import net.finmath.optimizer.StochasticOptimizerFactoryInterface;
import net.finmath.optimizer.StochasticOptimizerInterface;
import net.finmath.optimizer.StochasticPathwiseOptimizerFactoryLevenbergMarquardt;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public abstract class AbstractLIBORCovarianceModelParametric extends AbstractLIBORCovarianceModel {

	private static final Logger logger = Logger.getLogger("net.finmath");

	/**
	 * Constructor consuming time discretizations, which are handled by the super class.
	 * 
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 * @param numberOfFactors The number of factors to use (a factor reduction is performed)
	 */
	public AbstractLIBORCovarianceModelParametric(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);
	}

	/**
	 * Get the parameters of determining this parametric
	 * covariance model. The parameters are usually free parameters
	 * which may be used in calibration.
	 * 
	 * @return Parameter vector.
	 */
	public abstract RandomVariableInterface[]	getParameter();

	@Override
	public abstract Object clone();

	/**
	 * Return an instance of this model using a new set of parameters.
	 * Note: To improve performance it is admissible to return the same instance of the object given that the parameters have not changed. Models should be immutable.
	 * 
	 * @param parameters The new set of parameters.
	 * @return An instance of AbstractLIBORCovarianceModelParametric with modified parameters.
	 */
	public abstract AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariableInterface[] parameters);
	
	public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModelInterface calibrationModel, final AbstractLIBORMonteCarloProduct[] calibrationProducts, RandomVariableInterface[] calibrationTargetValues, double[] calibrationWeights) throws CalculationException {
		return getCloneCalibrated(calibrationModel, calibrationProducts, calibrationTargetValues, calibrationWeights, null);
	}

	/**
	 * Performs a generic calibration of the parametric model by
	 * trying to match a given vector of calibration product to a given vector of target values
	 * using a given vector of weights.
	 * 
	 * Optional calibration parameters may be passed using the map calibrationParameters. The keys are (<code>String</code>s):
	 * <ul>
	 * 	<li><tt>brownianMotion</tt>: Under this key an object implementing {@link net.finmath.montecarlo.BrownianMotionInterface} may be provided. If so, this Brownian motion is used to build the valuation model.</li>
	 * 	<li><tt>maxIterations</tt>: Under this key an object of type Integer may be provided specifying the maximum number of iterations.</li>
	 * 	<li><tt>accuracy</tt>: Under this key an object of type Double may be provided specifying the desired accuracy. Note that this is understood in the sense that the solver will stop if the iteration does not improve by more than this number.</li>
	 * </ul>
	 * 
	 * @param calibrationModel The LIBOR market model to be used for calibrations (specifies forward curve and tenor discretization).
	 * @param calibrationProducts The array of calibration products.
	 * @param calibrationTargetValues The array of target values.
	 * @param calibrationWeights The array of weights.
	 * @param calibrationParameters A map of type Map&lt;String, Object&gt; specifying some (optional) calibration parameters.
	 * @return A new parametric model of the same type than <code>this</code> one, but with calibrated parameters.
	 * @throws CalculationException Thrown if calibration has failed.
	 */
	public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModelInterface calibrationModel, final AbstractLIBORMonteCarloProduct[] calibrationProducts, final RandomVariableInterface[] calibrationTargetValues, double[] calibrationWeights, Map<String,Object> calibrationParameters) throws CalculationException {

		if(calibrationParameters == null) calibrationParameters = new HashMap<String,Object>();
		Integer numberOfPathsParameter	= (Integer)calibrationParameters.get("numberOfPaths");
		Integer seedParameter			= (Integer)calibrationParameters.get("seed");
		Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
		Double	parameterStepParameter	= (Double)calibrationParameters.get("parameterStep");
		Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
		BrownianMotionInterface brownianMotionParameter	= (BrownianMotionInterface)calibrationParameters.get("brownianMotion");

		RandomVariableInterface[] initialParameters = this.getParameter();
		RandomVariableInterface[] lowerBound = new RandomVariableInterface[initialParameters.length];
		RandomVariableInterface[] upperBound = new RandomVariableInterface[initialParameters.length];
		RandomVariableInterface[] parameterStep = new RandomVariableInterface[initialParameters.length];
		Arrays.fill(lowerBound, new RandomVariable(Double.NEGATIVE_INFINITY));
		Arrays.fill(upperBound, new RandomVariable(Double.POSITIVE_INFINITY));
		Arrays.fill(parameterStep,  new RandomVariable(parameterStepParameter != null ? parameterStepParameter.doubleValue() : 1E-4));

		/*
		 * We allow for 2 simultaneous calibration models.
		 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
		 * one model with 2 times the number of paths. In the case of an analytic calibration
		 * memory requirement is not the limiting factor.
		 */
		int numberOfThreads = 2;
		StochasticOptimizerFactoryInterface optimizerFactoryParameter = (StochasticOptimizerFactoryInterface)calibrationParameters.get("optimizerFactory");

		int numberOfPaths	= numberOfPathsParameter != null ? numberOfPathsParameter.intValue() : 2000;
		int seed			= seedParameter != null ? seedParameter.intValue() : 31415;
		int maxIterations	= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 400;
		double accuracy		= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-7;
		final BrownianMotionInterface brownianMotion = brownianMotionParameter != null ? brownianMotionParameter : new BrownianMotion(getTimeDiscretization(), getNumberOfFactors(), numberOfPaths, seed);
		StochasticOptimizerFactoryInterface optimizerFactory = optimizerFactoryParameter != null ? optimizerFactoryParameter : new StochasticPathwiseOptimizerFactoryLevenbergMarquardt(maxIterations, accuracy, numberOfThreads);

		int numberOfThreadsForProductValuation = 2 * Math.max(2, Runtime.getRuntime().availableProcessors());
		final ExecutorService executor = null;//Executors.newFixedThreadPool(numberOfThreadsForProductValuation);

		StochasticOptimizerInterface.ObjectiveFunction calibrationError = new StochasticOptimizerInterface.ObjectiveFunction() {			
			// Calculate model values for given parameters
			@Override
			public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) throws SolverException {

				AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = AbstractLIBORCovarianceModelParametric.this.getCloneWithModifiedParameters(parameters);

				// Create a LIBOR market model with the new covariance structure.
				LIBORMarketModelInterface model = calibrationModel.getCloneWithModifiedCovarianceModel(calibrationCovarianceModel);
				ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
				final LIBORModelMonteCarloSimulation liborMarketModelMonteCarloSimulation =  new LIBORModelMonteCarloSimulation(model, process);

				ArrayList<Future<RandomVariableInterface>> valueFutures = new ArrayList<Future<RandomVariableInterface>>(calibrationProducts.length);
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					final int workerCalibrationProductIndex = calibrationProductIndex;
					Callable<RandomVariableInterface> worker = new  Callable<RandomVariableInterface>() {
						public RandomVariableInterface call() {
							try {
								return calibrationProducts[workerCalibrationProductIndex].getValue(0.0, liborMarketModelMonteCarloSimulation);
							} catch (CalculationException e) {
								// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
								return calibrationTargetValues[workerCalibrationProductIndex];
							} catch (Exception e) {
								// We do not signal exceptions to keep the solver working and automatically exclude non-working calibration products.
								return calibrationTargetValues[workerCalibrationProductIndex];
							}
						}
					};
					if(executor != null) {
						Future<RandomVariableInterface> valueFuture = executor.submit(worker);
						valueFutures.add(calibrationProductIndex, valueFuture);
					}
					else {
						FutureTask<RandomVariableInterface> valueFutureTask = new FutureTask<RandomVariableInterface>(worker);
						valueFutureTask.run();
						valueFutures.add(calibrationProductIndex, valueFutureTask);
					}
				}
				for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
					try {
						RandomVariableInterface value = valueFutures.get(calibrationProductIndex).get();
						values[calibrationProductIndex] = value;
					}
					catch (InterruptedException e) {
						throw new SolverException(e);
					} catch (ExecutionException e) {
						throw new SolverException(e);
					}
				}
			}
		};

		StochasticOptimizerInterface optimizer = optimizerFactory.getOptimizer(calibrationError, initialParameters, lowerBound, upperBound, parameterStep, calibrationTargetValues);
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
		RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
		AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = this.getCloneWithModifiedParameters(bestParameters);

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

	@Override
	public String toString() {
		return "AbstractLIBORCovarianceModelParametric [getParameter()="
				+ Arrays.toString(getParameter()) + "]";
	}
}
