/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 30.03.2014
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORMarketModelInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements the calculation of the
 * curvature of the volatility surface of the forward rates.
 * 
 * While this is not a common financial product, this class can be helpful in calibration procedures, e.g.
 * to put an additional constrain on the smoothness / curvature of the model surface. 
 *
 * @author Christian Fries
 * @date 12.04.2014.
 */
public class ForwardRateVolatilitySurfaceCurvature extends AbstractLIBORMonteCarloProduct {

    /**
     * Create the calculation of the curvature of the volatility surface of the forward rates
     */
    public ForwardRateVolatilitySurfaceCurvature() {
        super();
    }

    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) {
    	return getValues(evaluationTime, model.getModel());
    }
    
    /**
     * Calculates the squared curvature of the LIBOR volatility.
     * 
     * @param evaluationTime Time at which the product is evaluated.
     * @param model A model implementing the LIBORModelMonteCarloSimulationInterface
     * @return Depending on the value of value unit, the method returns either
     * the approximated integrated instantaneous variance of the swap rate (ValueUnit.INTEGRATEDVARIANCE)
     * or the value using the Black formula (ValueUnit.VALUE).
     * @TODO make initial values an arg and use evaluation time.
     */
    public RandomVariableInterface getValues(double evaluationTime, LIBORMarketModelInterface model) {
    	if(evaluationTime > 0) throw new RuntimeException("Forward start evaluation currently not supported.");

    	// Fetch the covariance model of the model
    	AbstractLIBORCovarianceModel covarianceModel = model.getCovarianceModel();

    	// We sum over all simulation time steps
    	int numberOfTimeSteps = covarianceModel.getTimeDiscretization().getNumberOfTimes();

    	// Accumulator
        RandomVariableInterface integratedLIBORCurvature = new RandomVariable(0.0);
        for(int timeIndex = 0; timeIndex < numberOfTimeSteps-2-1; timeIndex++) {
            double time				    = covarianceModel.getTimeDiscretization().getTime(timeIndex);
            int simulationTimeIndex		= covarianceModel.getTimeDiscretization().getTimeIndex(time);
            int componentStartIndex		= covarianceModel.getLiborPeriodDiscretization().getTimeIndex(time);
            int componentEndIndex		= covarianceModel.getLiborPeriodDiscretization().getNumberOfTimes()-1;

            // Sum squared second derivative of the variance for all components at this time step
            RandomVariableInterface integratedLIBORCurvaturePerTime = new RandomVariable(0.0);
            for(int componentIndex = componentStartIndex; componentIndex < componentEndIndex; componentIndex++) {
                RandomVariableInterface covarianceLeft		= covarianceModel.getCovariance(simulationTimeIndex+0, componentIndex, componentIndex, null);
                RandomVariableInterface covarianceCenter	= covarianceModel.getCovariance(simulationTimeIndex+1, componentIndex, componentIndex, null);
                RandomVariableInterface covarianceRight		= covarianceModel.getCovariance(simulationTimeIndex+2, componentIndex, componentIndex, null);

                RandomVariableInterface curvatureSquared = covarianceRight.sub(covarianceCenter.mult(2.0)).add(covarianceLeft).squared();

                integratedLIBORCurvaturePerTime = integratedLIBORCurvaturePerTime.add(curvatureSquared);
            }
            integratedLIBORCurvaturePerTime = integratedLIBORCurvaturePerTime.div(componentEndIndex-componentStartIndex);

            integratedLIBORCurvature = integratedLIBORCurvature.add(integratedLIBORCurvaturePerTime);
        }

         return integratedLIBORCurvature.div(numberOfTimeSteps-3);
    }
}
