/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

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
 * @author Christian Fries
 *
 */
public abstract class AbstractLIBORCovarianceModelParametric extends AbstractLIBORCovarianceModel {

	/**
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 * @param numberOfPaths The number of paths (used only in a stochastic volatility model).
	 */
	public AbstractLIBORCovarianceModelParametric(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);
	}

    public abstract double[]	getParameter();
    public abstract void		setParameter(double[] parameter);
       

    public abstract Object clone();
    
    public AbstractLIBORCovarianceModelParametric getCloneCalibrated(final LIBORMarketModelInterface calibrationModel, final AbstractLIBORMonteCarloProduct[] calibrationProducts, double[] calibrationTargetValues, double[] calibrationWeights) throws CalculationException {

    	double[] initialParameters = this.getParameter();

    	// @TODO: These constants should become parameters. The numberOfPaths and seed is only relevant if Monte-Carlo products are used for calibration.
		final int numberOfPaths	= 5000;
		final int seed			= 31415;
		final int maxIterations	= 1000;
		final int numberOfThreads = 10;

		
    	LevenbergMarquardt optimizer = new net.finmath.optimizer.LevenbergMarquardt(
			initialParameters,
			calibrationTargetValues,
			maxIterations,
			numberOfThreads)
    	{
			// Calculate model values for given parameters
			@Override
            public void setValues(double[] parameters, double[] values) throws SolverException {
		        
		    	AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = (AbstractLIBORCovarianceModelParametric)AbstractLIBORCovarianceModelParametric.this.clone();
				calibrationCovarianceModel.setParameter(parameters);

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
		
    	final AbstractLIBORCovarianceModelParametric calibrationCovarianceModel = (AbstractLIBORCovarianceModelParametric)this.clone();
		calibrationCovarianceModel.setParameter(optimizer.getBestFitParameters());
		
		// Diagnostic output
		double[] bestParameters = optimizer.getBestFitParameters();
		System.out.println("The solver required " + optimizer.getIterations() + " iterations. The best fit parameters are:");
		for(int i=0; i<bestParameters.length; i++) {
			System.out.println("\tparameter["+i+"]: " + bestParameters[i]);
		}

        return calibrationCovarianceModel;    	
    }
}
