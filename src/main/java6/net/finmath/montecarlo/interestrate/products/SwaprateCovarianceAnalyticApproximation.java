/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.11.2007
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
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
public class SwaprateCovarianceAnalyticApproximation extends AbstractLIBORMonteCarloProduct {

    final double[]    swapTenor1;       // Vector of swap tenor (period start and end dates).
    final double[]    swapTenor2;       // Vector of swap tenor (period start and end dates).
    
    /**
     * Create the product implementing the analytic approximation of a swap rate covariance in a forward rate model.
     * 
     * @param swapTenor1 The swap tenor of the first rate in doubles.
     * @param swapTenor2 The swap tenor of the second rate in doubles.
     */
    public SwaprateCovarianceAnalyticApproximation(double[] swapTenor1, double[] swapTenor2) {
        super();
        this.swapTenor1 = swapTenor1;
        this.swapTenor2 = swapTenor2;
    }

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
    	// This product requires an LIBORModelMonteCarloSimulationInterface model, otherwise there will be a class cast exception
		if(model.getModel() instanceof LIBORMarketModel) {
	    	return getValue(evaluationTime, (LIBORMarketModel)model.getModel());
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getModel() + "."
			+ "It requires a model of type constructed from a " + LIBORMarketModel.class + ".");
		}
	}

	/**
     * Calculates the approximated integrated instantaneous covariance of two swap rates,
     * using the approximation d log(S(t))/d log(L(t)) = d log(S(0))/d log(L(0)).
     * 
	 * @param evaluationTime The evaluation time.
     * @param model A model implementing the LIBORMarketModel
     * @return Returns the approximated integrated instantaneous covariance of two swap rates.
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
	public RandomVariableInterface getValue(double evaluationTime, LIBORMarketModel model) throws CalculationException {

        int swapStartIndex1  = model.getLiborPeriodIndex(swapTenor1[0]);
        int swapEndIndex1    = model.getLiborPeriodIndex(swapTenor1[swapTenor1.length-1]);

        int swapStartIndex2  = model.getLiborPeriodIndex(swapTenor2[0]);
        int swapEndIndex2    = model.getLiborPeriodIndex(swapTenor2[swapTenor2.length-1]);

        int optionMaturityIndex = model.getTimeIndex(Math.min(swapTenor1[0], swapTenor2[0]));
        
        double[]  swapCovarianceWeights1  = SwaptionSingleCurveAnalyticApproximation.getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getForwardRateCurve(), swapTenor1).get("values");
        double[]  swapCovarianceWeights2  = SwaptionSingleCurveAnalyticApproximation.getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getForwardRateCurve(), swapTenor2).get("values");

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
