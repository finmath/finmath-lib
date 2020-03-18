/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.11.2007
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class implements an analytic approximation of the integrated instantaneous covariance
 * of two swap rates under a LIBOR market model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SwaprateCovarianceAnalyticApproximation extends AbstractMonteCarloProduct {

	private final double[]    swapTenor1;       // Vector of swap tenor (period start and end dates).
	private final double[]    swapTenor2;       // Vector of swap tenor (period start and end dates).

	/**
	 * Create the product implementing the analytic approximation of a swap rate covariance in a forward rate model.
	 *
	 * @param swapTenor1 The swap tenor of the first rate in doubles.
	 * @param swapTenor2 The swap tenor of the second rate in doubles.
	 */
	public SwaprateCovarianceAnalyticApproximation(final double[] swapTenor1, final double[] swapTenor2) {
		super();
		this.swapTenor1 = swapTenor1;
		this.swapTenor2 = swapTenor2;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final MonteCarloSimulationModel model) throws CalculationException {
		if(!(model instanceof LIBORModelMonteCarloSimulationModel)) {
			throw new IllegalArgumentException("This product requires a simulation of type LIBORModelMonteCarloSimulationModel where the underlying model is of type LIBORMarketModel.");
		}

		final ProcessModel modelBase = ((LIBORModelMonteCarloSimulationModel)model).getModel();
		if(modelBase instanceof LIBORMarketModel) {
			return getValues(evaluationTime, model.getTimeDiscretization(), (LIBORMarketModel)modelBase);
		} else {
			throw new IllegalArgumentException("This product requires a simulation where the underlying model is of type LIBORMarketModel.");
		}
	}

	/**
	 * Calculates the approximated integrated instantaneous covariance of two swap rates,
	 * using the approximation d log(S(t))/d log(L(t)) = d log(S(0))/d log(L(0)).
	 *
	 * @param evaluationTime The evaluation time.
	 * @param timeDiscretization The time discretization used for integrating the covariance.
	 * @param model A model implementing the LIBORMarketModelFromCovarianceModel
	 * @return Returns the approximated integrated instantaneous covariance of two swap rates.
	 */
	public RandomVariable getValues(final double evaluationTime, final TimeDiscretization timeDiscretization, final LIBORMarketModel model) {

		final int swapStartIndex1  = model.getLiborPeriodIndex(swapTenor1[0]);
		final int swapEndIndex1    = model.getLiborPeriodIndex(swapTenor1[swapTenor1.length-1]);

		final int swapStartIndex2  = model.getLiborPeriodIndex(swapTenor2[0]);
		final int swapEndIndex2    = model.getLiborPeriodIndex(swapTenor2[swapTenor2.length-1]);

		final int optionMaturityIndex = model.getCovarianceModel().getTimeDiscretization().getTimeIndex(Math.min(swapTenor1[0], swapTenor2[0]));

		final double[]  swapCovarianceWeights1  = SwaptionSingleCurveAnalyticApproximation.getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getForwardRateCurve(), swapTenor1).get("values");
		final double[]  swapCovarianceWeights2  = SwaptionSingleCurveAnalyticApproximation.getLogSwaprateDerivative(model.getLiborPeriodDiscretization(), model.getForwardRateCurve(), swapTenor2).get("values");

		// Get the integrated libor covariance from the model
		final double[][]	integratedLIBORCovariance = model.getIntegratedLIBORCovariance(timeDiscretization)[optionMaturityIndex];

		// Calculate integrated swap rate covariance
		double integratedSwapRateCovariance = 0.0;

		for(int componentIndex1 = swapStartIndex1; componentIndex1 < swapEndIndex1; componentIndex1++) {
			// Sum the libor cross terms (use symmetry)
			for(int componentIndex2 = swapStartIndex2; componentIndex2 < swapEndIndex2; componentIndex2++) {
				integratedSwapRateCovariance += swapCovarianceWeights1[componentIndex1-swapStartIndex1] * swapCovarianceWeights2[componentIndex2-swapStartIndex2] * integratedLIBORCovariance[componentIndex1][componentIndex2];
			}
		}

		return new RandomVariableFromDoubleArray(evaluationTime, integratedSwapRateCovariance);
	}
}
