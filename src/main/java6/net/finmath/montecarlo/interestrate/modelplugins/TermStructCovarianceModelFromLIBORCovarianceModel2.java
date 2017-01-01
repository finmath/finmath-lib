/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 22.12.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.interestrate.TermStructureModelInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 *
 */
public class TermStructCovarianceModelFromLIBORCovarianceModel2 implements TermStructureCovarianceModelInterface {

	private final AbstractLIBORCovarianceModelParametric covarianceModel;
	
	/**
	 * 
	 */
	public TermStructCovarianceModelFromLIBORCovarianceModel2(AbstractLIBORCovarianceModelParametric covarianceModel) {
		this.covarianceModel = covarianceModel;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(double time, double periodStart, double periodEnd, TimeDiscretizationInterface periodDiscretization, RandomVariableInterface[] realizationAtTimeIndex, TermStructureModelInterface model) {
		TimeDiscretizationInterface liborPeriodDiscretization = covarianceModel.getLiborPeriodDiscretization();

		int periodStartIndex = liborPeriodDiscretization.getTimeIndex(periodStart);
		int periodEndIndex = liborPeriodDiscretization.getTimeIndex(periodEnd);
		RandomVariableInterface[] factorLoadings = covarianceModel.getFactorLoading(time, periodStartIndex, null);
		if(periodEndIndex > periodStartIndex+1) {
			// Need to sum factor loadings
			for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
				factorLoadings[factorIndex] = factorLoadings[factorIndex].mult(liborPeriodDiscretization.getTimeStep(periodStartIndex));
			}
			
			for(int periodIndex = periodStartIndex+1; periodIndex<periodEndIndex; periodIndex++) {
				RandomVariableInterface[] factorLoadingsForPeriod = covarianceModel.getFactorLoading(time, periodStartIndex, null);
				double periodLength = liborPeriodDiscretization.getTimeStep(periodIndex);
				for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
					factorLoadings[factorIndex] = factorLoadings[factorIndex].addProduct(factorLoadingsForPeriod[factorIndex], periodLength);
				}
			}

			for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
				factorLoadings[factorIndex] = factorLoadings[factorIndex].div(periodEnd-periodStart);
			}
		}

		int componentIndex = periodDiscretization.getTimeIndex(periodStart);
		if(componentIndex < 0) componentIndex = -componentIndex-1-1;
		int componentIndex2 = periodDiscretization.getTimeIndex(periodEnd);
		if(componentIndex2 < 0) componentIndex2 = -componentIndex2-1;
		if(componentIndex != componentIndex2-1) {
			throw new IllegalArgumentException();
		}
		
		for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
			factorLoadings[factorIndex] = factorLoadings[factorIndex].div(realizationAtTimeIndex[componentIndex].mult(periodDiscretization.getTimeStep(componentIndex)).exp());
		}

		return factorLoadings;
	}

	@Override
	public int getNumberOfFactors() {
		return covarianceModel.getNumberOfFactors();
	}

}
