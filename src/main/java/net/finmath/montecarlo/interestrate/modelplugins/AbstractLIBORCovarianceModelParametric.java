/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.time.TimeDiscretizationInterface;

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
    public abstract double[]	getParameter();
    
    public abstract void		setParameter(double[] parameter);

    @Override
    public abstract Object clone();
    
    public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
    	AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = (AbstractLIBORCovarianceModelParametric)AbstractLIBORCovarianceModelParametric.this.clone();
		calibrationCovarianceModel.setParameter(parameters);
		
		return calibrationCovarianceModel;
    }
    
    public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModelInterface calibrationModel, final AbstractLIBORMonteCarloProduct[] calibrationProducts, double[] calibrationTargetValues, double[] calibrationWeights) throws CalculationException {
    	return getCloneCalibrated(calibrationModel, calibrationProducts, calibrationTargetValues, calibrationWeights, null);
    }
    
    public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModelInterface calibrationModel, final AbstractLIBORMonteCarloProduct[] calibrationProducts, double[] calibrationTargetValues, double[] calibrationWeights, Map<String,Object> calibrationParameters) throws CalculationException {

    	double[] initialParameters = this.getParameter();

    	if(calibrationParameters == null) calibrationParameters = new HashMap<String,Object>();
    	Integer numberOfPathsParameter	= (Integer)calibrationParameters.get("numberOfPaths");
    	Integer seedParameter			= (Integer)calibrationParameters.get("seed");
    	Integer maxIterationsParameter	= (Integer)calibrationParameters.get("maxIterations");
    	Double	accuracyParameter		= (Double)calibrationParameters.get("accuracy");
    	
    	// @TODO: These constants should become parameters. The numberOfPaths and seed is only relevant if Monte-Carlo products are used for calibration.
		int numberOfPaths	= numberOfPathsParameter != null ? numberOfPathsParameter.intValue() : 2000;
		int seed			= seedParameter != null ? seedParameter.intValue() : 31415;
		int maxIterations	= maxIterationsParameter != null ? maxIterationsParameter.intValue() : 400;
		double accuracy		= accuracyParameter != null ? accuracyParameter.doubleValue() : 1E-6;

		final BrownianMotion brownianMotion = new BrownianMotion(getTimeDiscretization(), getNumberOfFactors(), numberOfPaths, seed);

		/*
		 * We allow for 5 simultaneous calibration models.
		 * Note: In the case of a Monte-Carlo calibration, the memory requirement is that of
		 * one model with 5 times the number of paths. In the case of an analytic calibration
		 * memory requirement is not the limiting factor.
		 */
		int numberOfThreads = 5;			
    	LevenbergMarquardt optimizer = new LevenbergMarquardt(initialParameters, calibrationTargetValues, maxIterations, numberOfThreads)
    	{
			// Calculate model values for given parameters
			@Override
            public void setValues(double[] parameters, double[] values) throws SolverException {
		        
		    	AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = AbstractLIBORCovarianceModelParametric.this.getCloneWithModifiedParameters(parameters);

		    	// Create a LIBOR market model with the new covariance structure.
		    	LIBORMarketModelInterface model = calibrationModel.getCloneWithModifiedCovarianceModel(calibrationCovarianceModel);
				ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
		        final LIBORModelMonteCarloSimulation liborMarketModelMonteCarloSimulation =  new LIBORModelMonteCarloSimulation(model, process);

                IntStream.range(0,calibrationProducts.length).parallel().forEach(
                        calibrationProductIndex -> {
                            try {
                                values[calibrationProductIndex] = calibrationProducts[calibrationProductIndex].getValue(liborMarketModelMonteCarloSimulation);
                            } catch (net.finmath.exception.CalculationException e) {
                               throw new RuntimeException(e);
                            }
                        }
                        );
			}
		};

		// Set solver parameters
		optimizer.setWeights(calibrationWeights);
		optimizer.setErrorTolerance(accuracy);

		try {
			optimizer.run();
		}
		catch(SolverException e) {
			throw new CalculationException(e);
		}

		// Get covariance model corresponding to the best parameter set.
		double[] bestParameters = optimizer.getBestFitParameters();
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
}
