/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.11.2007
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements an analytic approximation of the integrated instantaneous covariance
 * of two swap rates under a LIBOR market model.
 * 
 * @author Christian Fries
 */
public class SwaprateCovarianceAnalyticApproximation extends AbstractMonteCarloProduct {

    final double[]    swapTenor1;       // Vector of swap tenor (period start and end dates).
    final double[]    swapTenor2;       // Vector of swap tenor (period start and end dates).
    
    /**
     * @param swapTenor1 The swap tenor of the first rate in doubles.
     * @param swapTenor2 The swap tenor of the second rate in doubles.
     */
    public SwaprateCovarianceAnalyticApproximation(double[] swapTenor1, double[] swapTenor2) {
        super();
        this.swapTenor1 = swapTenor1;
        this.swapTenor2 = swapTenor2;
    }

    @Override
    public RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException {
    	return getValue(evaluationTime, (LIBORMarketModel)((LIBORModelMonteCarloSimulationInterface) model).getModel());
    }

    /**
     * Calculates the approximated integrated instantaneous covariance of two swap rates,
     * using the approximation d log(S(t))/d log(L(t)) = d log(S(0))/d log(L(0)).
     * 
     * @param model A model implementing the LIBORMarketModel
     * @return Returns the approximated integrated instantaneous covariance of two swap rates.
     */
	public RandomVariableInterface getValue(double evaluationTime, LIBORMarketModel model) throws CalculationException {

        int swapStartIndex1  = model.getLiborPeriodIndex(swapTenor1[0]);
        int swapEndIndex1    = model.getLiborPeriodIndex(swapTenor1[swapTenor1.length-1]);

        int swapStartIndex2  = model.getLiborPeriodIndex(swapTenor2[0]);
        int swapEndIndex2    = model.getLiborPeriodIndex(swapTenor2[swapTenor2.length-1]);

        int optionMaturityIndex = model.getTimeIndex(Math.min(swapTenor1[0], swapTenor2[0]));
        
        double[]  swapCovarianceWeights1  = SwaptionAnalyticApproximation.getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getForwardRateCurve(), swapTenor1).get("values");
        double[]  swapCovarianceWeights2  = SwaptionAnalyticApproximation.getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getForwardRateCurve(), swapTenor2).get("values");

        // Get the integrated libor covariance from the model
        double[][]	integratedLIBORCovariance = model.getIntegratedLIBORCovariance()[optionMaturityIndex];

        // Calculate integrated swap rate covariance
        double integratedSwapRateCovariance = 0.0;

        for(int componentIndex1 = swapStartIndex1; componentIndex1 < swapEndIndex1; componentIndex1++) {
            // Sum the libor cross terms (use symmetry)
            for(int componentIndex2 = swapStartIndex2; componentIndex2 < swapEndIndex2; componentIndex2++) {
                integratedSwapRateCovariance += swapCovarianceWeights1[componentIndex1-swapStartIndex1] * swapCovarianceWeights2[componentIndex2-swapStartIndex2] * integratedLIBORCovariance[componentIndex1][componentIndex2];
            }
        }

        return new RandomVariable(evaluationTime, integratedSwapRateCovariance);
    }
}
