/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.covariancemodels;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORCorrelationModelExponentialDecay;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModelMaturityDependentFourParameterExponentialForm;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * The five parameter covariance model consisting of an
 * {@link LIBORVolatilityModelMaturityDependentFourParameterExponentialForm}
 * and an
 * {@link LIBORCorrelationModelExponentialDecay}.
 *
 * @author Christian Fries
 */
public class LIBORCovarianceModelExponentialForm5Param extends AbstractLIBORCovarianceModelParametric {

	private RandomVariableInterface[] parameter = new RandomVariableInterface[5];

	private LIBORVolatilityModel		volatilityModel;
	private LIBORCorrelationModel	correlationModel;

	public LIBORCovarianceModelExponentialForm5Param(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors, RandomVariableInterface[] parameters) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);

		this.parameter = parameters.clone();
		volatilityModel	= new LIBORVolatilityModelFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameter[0], parameter[1], parameter[2], parameter[3], false);
		correlationModel	= new LIBORCorrelationModelExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameter[4].doubleValue(), false);
	}

	@Override
	public Object clone() {
		LIBORCovarianceModelExponentialForm5Param model = new LIBORCovarianceModelExponentialForm5Param(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), this.getNumberOfFactors(), this.getParameter());
		model.parameter = this.parameter;
		model.volatilityModel = this.volatilityModel;
		model.correlationModel = this.correlationModel;
		return model;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariableInterface[] parameters) {
		LIBORCovarianceModelExponentialForm5Param model = (LIBORCovarianceModelExponentialForm5Param)this.clone();

		model.parameter = parameters;
		if(parameters[0] != this.parameter[0] || parameters[1] != this.parameter[1] || parameters[2] != this.parameter[2] || parameters[3] != this.parameter[3]) {
			model.volatilityModel	= new LIBORVolatilityModelFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameters[0], parameters[1], parameters[2], parameters[3], false);
		}
		if(parameters[4] != this.parameter[4]) {
			model.correlationModel	= new LIBORCorrelationModelExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameters[4].doubleValue(), false);
		}

		return model;
	}

	@Override
	public RandomVariableInterface[] getParameter() {
		return parameter.clone();
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		RandomVariableInterface[] factorLoading = new RandomVariableInterface[correlationModel.getNumberOfFactors()];
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			RandomVariableInterface volatility = volatilityModel.getVolatility(timeIndex, component);
			factorLoading[factorIndex] = volatility
					.mult(correlationModel.getFactorLoading(timeIndex, factorIndex, component));
		}

		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariableInterface[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}
}
