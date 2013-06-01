/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.logging.Level;
import java.util.logging.Logger;

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
 * Base class for parametric covariance models.
 * Parametric models feature a parameter vector which can be inspected
 * and modified for calibration purposes.
 * 
 * The parameter vector may have zero length, which indicated that the model
 * is not calibrateable.
 * 
 * @author Christian Fries
 */
public abstract class AbstractLIBORCovarianceModelParametric extends AbstractLIBORCovarianceModel {

	private static Logger logger = Logger.getLogger("net.finmath");

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

    public abstract Object clone();
    
    public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
    	AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = (AbstractLIBORCovarianceModelParametric)AbstractLIBORCovarianceModelParametric.this.clone();
		calibrationCovarianceModel.setParameter(parameters);
		
		return calibrationCovarianceModel;
    }
    
    public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModelInterface calibrationModel, final AbstractLIBORMonteCarloProduct[] calibrationProducts, double[] calibrationTargetValues, double[] calibrationWeights) throws CalculationException {

    	double[] initialParameters = this.getParameter();

    	// @TODO: These constants should become parameters. The numberOfPaths and seed is only relevant if Monte-Carlo products are used for calibration.
		final int numberOfPaths	= 5000;
		final int seed			= 31415;
		final int maxIterations	= 1000;

		// We do not allocate more threads the twice the number of processors.
		int numberOfThreads = Math.min(Math.max(2 * Runtime.getRuntime().availableProcessors(),1), calibrationProducts.length);
		
    	LevenbergMarquardt optimizer = new net.finmath.optimizer.LevenbergMarquardt(
			initialParameters,
			calibrationTargetValues,
			maxIterations,
			numberOfThreads)
    	{
			// Calculate model values for given parameters
			@Override
            public void setValues(double[] parameters, double[] values) throws SolverException {
		        
		    	AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = (AbstractLIBORCovarianceModelParametric)AbstractLIBORCovarianceModelParametric.this.getCloneWithModifiedParameters(parameters);

		    	LIBORMarketModelInterface model = calibrationModel.getCloneWithModifiedCovarianceModel(calibrationCovarianceModel);

				BrownianMotion brownianMotion = new BrownianMotion(getTimeDiscretization(), getNumberOfFactors(), numberOfPaths, seed);

				ProcessEulerScheme process = new ProcessEulerScheme(brownianMotion);
		        final LIBORModelMonteCarloSimulation liborMarketModelMonteCarloSimulation =  new LIBORModelMonteCarloSimulation(model, process);


		        for(int calibrationProductIndex=0; calibrationProductIndex<calibrationProducts.length; calibrationProductIndex++) {
		        	try {
		        		values[calibrationProductIndex] = calibrationProducts[calibrationProductIndex].getValue(liborMarketModelMonteCarloSimulation);
					} catch (CalculationException e) {
		    			throw new SolverException(e);
					}
		        }
			}			
		};

		// Set solver parameters
		optimizer.setWeights(calibrationWeights);
		
		try {
			optimizer.run();
		}
		catch(SolverException e) {
			throw new CalculationException(e);
		}

		// Get covariance model corresponding to the best parameter set.
		double[] bestParameters = optimizer.getBestFitParameters();
    	final AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = (AbstractLIBORCovarianceModelParametric)this.getCloneWithModifiedParameters(bestParameters);
		
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
