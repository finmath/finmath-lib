/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.12.2016
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import net.finmath.montecarlo.interestrate.TermStructureModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class TermStructCovarianceModelFromLIBORCovarianceModelParametric extends TermStructureCovarianceModelParametric {

	private final TermStructureTenorTimeScaling tenorTimeScalingModel;
	private final AbstractLIBORCovarianceModelParametric covarianceModel;

	/**
	 * @param tenorTimeScalingModel The model used for the tenor time re-scaling (providing the scaling coefficients).
	 * @param covarianceModel The model implementing AbstractLIBORCovarianceModelParametric.
	 */
	public TermStructCovarianceModelFromLIBORCovarianceModelParametric(final TermStructureTenorTimeScaling tenorTimeScalingModel, final AbstractLIBORCovarianceModelParametric covarianceModel) {
		this.tenorTimeScalingModel = tenorTimeScalingModel;
		this.covarianceModel = covarianceModel;
	}

	@Override
	public double getScaledTenorTime(final double periodStart, final double periodEnd) {
		if(tenorTimeScalingModel == null) {
			return periodEnd-periodStart;
		}
		return tenorTimeScalingModel.getScaledTenorTime(periodStart, periodEnd);
	}

	@Override
	public RandomVariable[] getFactorLoading(final double time, final double periodStart, final double periodEnd, final TimeDiscretization periodDiscretization, final RandomVariable[] realizationAtTimeIndex, final TermStructureModel model) {
		final TimeDiscretization liborPeriodDiscretization = covarianceModel.getLiborPeriodDiscretization();

		final int periodStartIndex = liborPeriodDiscretization.getTimeIndex(periodStart);
		final int periodEndIndex = liborPeriodDiscretization.getTimeIndex(periodEnd);
		final RandomVariable[] factorLoadings = covarianceModel.getFactorLoading(time, periodStartIndex, null);
		if(periodEndIndex > periodStartIndex+1) {
			// Need to sum factor loadings
			for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
				factorLoadings[factorIndex] = factorLoadings[factorIndex].mult(liborPeriodDiscretization.getTimeStep(periodStartIndex));
			}

			for(int periodIndex = periodStartIndex+1; periodIndex<periodEndIndex; periodIndex++) {
				final RandomVariable[] factorLoadingsForPeriod = covarianceModel.getFactorLoading(time, periodStartIndex, null);
				final double periodLength = liborPeriodDiscretization.getTimeStep(periodIndex);
				for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
					factorLoadings[factorIndex] = factorLoadings[factorIndex].addProduct(factorLoadingsForPeriod[factorIndex], periodLength);
				}
			}

			for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
				factorLoadings[factorIndex] = factorLoadings[factorIndex].div(periodEnd-periodStart);
			}
		}

		int componentIndex = periodDiscretization.getTimeIndex(periodStart);
		if(componentIndex < 0) {
			componentIndex = -componentIndex-1-1;
		}
		int componentIndex2 = periodDiscretization.getTimeIndex(periodEnd);
		if(componentIndex2 < 0) {
			componentIndex2 = -componentIndex2-1;
		}
		if(componentIndex != componentIndex2-1) {
			throw new IllegalArgumentException();
		}

		/*
		 * 		for(int factorIndex = 0; factorIndex<factorLoadings.length; factorIndex++) {
			factorLoadings[factorIndex] = factorLoadings[factorIndex].div(realizationAtTimeIndex[componentIndex].mult(periodDiscretization.getTimeStep(componentIndex)).exp());
		}
		 */

		return factorLoadings;
	}

	@Override
	public int getNumberOfFactors() {
		return covarianceModel.getNumberOfFactors();
	}

	@Override
	public double[] getParameter() {
		if(tenorTimeScalingModel == null) {
			return covarianceModel.getParameterAsDouble();
		}

		final double[] tenorTimeScalingParameter = tenorTimeScalingModel.getParameter();
		final double[] covarianceParameter = covarianceModel.getParameterAsDouble();
		final double[] parameter = new double[tenorTimeScalingParameter.length + covarianceParameter.length];
		System.arraycopy(tenorTimeScalingParameter, 0, parameter, 0, tenorTimeScalingParameter.length);
		System.arraycopy(covarianceParameter, 0, parameter, tenorTimeScalingParameter.length, covarianceParameter.length);
		return parameter;
	}

	@Override
	public TermStructureCovarianceModelParametric getCloneWithModifiedParameters(final double[] parameters) {
		if(tenorTimeScalingModel == null) {
			return new TermStructCovarianceModelFromLIBORCovarianceModelParametric(null, covarianceModel.getCloneWithModifiedParameters(parameters));
		}

		final double[] tenorTimeScalingParameter = tenorTimeScalingModel.getParameter();
		final double[] covarianceParameter = covarianceModel.getParameterAsDouble();
		System.arraycopy(parameters, 0, tenorTimeScalingParameter, 0, tenorTimeScalingParameter.length);
		System.arraycopy(parameters, tenorTimeScalingParameter.length, covarianceParameter, 0, covarianceParameter.length);

		return new TermStructCovarianceModelFromLIBORCovarianceModelParametric(tenorTimeScalingModel.getCloneWithModifiedParameters(tenorTimeScalingParameter), covarianceModel.getCloneWithModifiedParameters(covarianceParameter));
	}

	@Override
	public TermStructureCovarianceModelParametric clone() {
		return new TermStructCovarianceModelFromLIBORCovarianceModelParametric(tenorTimeScalingModel.clone(), (AbstractLIBORCovarianceModelParametric) covarianceModel.clone());
	}
}
