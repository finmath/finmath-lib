/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.12.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.interestrate.LIBORMarketModelWithTenorRefinement;
import net.finmath.montecarlo.interestrate.TermStructureModelInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 *
 */
public class TermStructCovarianceModelFromLIBORCovarianceModel implements TermStructureFactorLoadingsModelInterface {

	private final AbstractLIBORCovarianceModelParametric covarianceModel;
	
	/**
	 * Create a term structure covariance model model implementing TermStructureCovarianceModelInterface
	 * using a given model implementing AbstractLIBORCovarianceModelParametric.
	 * 
	 * @param covarianceModel The model implementing AbstractLIBORCovarianceModelParametric.
	 */
	public TermStructCovarianceModelFromLIBORCovarianceModel(AbstractLIBORCovarianceModelParametric covarianceModel) {
		this.covarianceModel = covarianceModel;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(double time, double periodStart, double periodEnd, TimeDiscretizationInterface periodDiscretization, RandomVariableInterface[] realizationAtTimeIndex, TermStructureModelInterface model) {
		TimeDiscretizationInterface liborPeriodDiscretization = covarianceModel.getLiborPeriodDiscretization();

		// Cache is really needed.
		RandomVariableInterface[] liborAtTimeIndex = new RandomVariableInterface[liborPeriodDiscretization.getNumberOfTimeSteps()];
		for(int componentIndex=0; componentIndex<liborPeriodDiscretization.getNumberOfTimeSteps(); componentIndex++) {
			if(liborPeriodDiscretization.getTime(componentIndex) < time) {
				liborAtTimeIndex[componentIndex] = null;
			}
			else {
				liborAtTimeIndex[componentIndex] = ((LIBORMarketModelWithTenorRefinement)model).getLIBORForStateVariable(periodDiscretization, realizationAtTimeIndex, liborPeriodDiscretization.getTime(componentIndex), liborPeriodDiscretization.getTime(componentIndex+1));
			}
		}

		int periodStartIndex = liborPeriodDiscretization.getTimeIndex(periodStart);
		int periodEndIndex = liborPeriodDiscretization.getTimeIndex(periodEnd);
		RandomVariableInterface[] factorLoadings = covarianceModel.getFactorLoading(time, periodStartIndex, liborAtTimeIndex);
		if(periodEndIndex > periodStartIndex+1) {
			// Need to sum factor loadings
			for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
				factorLoadings[factorIndex] = factorLoadings[factorIndex].mult(liborPeriodDiscretization.getTimeStep(periodStartIndex));
			}
			
			for(int periodIndex = periodStartIndex+1; periodIndex<periodEndIndex; periodIndex++) {
				RandomVariableInterface[] factorLoadingsForPeriod = covarianceModel.getFactorLoading(time, periodStartIndex, liborAtTimeIndex);
				double periodLength = liborPeriodDiscretization.getTimeStep(periodIndex);
				for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
					factorLoadings[factorIndex] = factorLoadings[factorIndex].addProduct(factorLoadingsForPeriod[factorIndex], periodLength);
				}
			}

			for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
				factorLoadings[factorIndex] = factorLoadings[factorIndex].div(periodEnd-periodStart);
			}
		}
		return factorLoadings;
	}

	@Override
	public int getNumberOfFactors() {
		return covarianceModel.getNumberOfFactors();
	}
}
