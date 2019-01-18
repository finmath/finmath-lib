/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * The five parameter covariance model consisting of an
 * {@link LIBORVolatilityModelMaturityDependentFourParameterExponentialForm}
 * and an
 * {@link LIBORCorrelationModelExponentialDecay}.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORCovarianceModelExponentialForm5Param extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = -2866966535617565970L;

	private double[] parameter = new double[5];

	private LIBORVolatilityModel	volatilityModel;
	private LIBORCorrelationModel	correlationModel;

	public LIBORCovarianceModelExponentialForm5Param(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, int numberOfFactors, double[] parameters) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);

		this.parameter = parameters.clone();
		volatilityModel		= new LIBORVolatilityModelFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameter[0], parameter[1], parameter[2], parameter[3], false);
		correlationModel	= new LIBORCorrelationModelExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameter[4], false);
	}

	public LIBORCovarianceModelExponentialForm5Param(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, int numberOfFactors) {
		this(timeDiscretization, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20, 0.05, 0.10, 0.20, 0.10});
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
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		LIBORCovarianceModelExponentialForm5Param model = (LIBORCovarianceModelExponentialForm5Param)this.clone();

		model.parameter = parameters;
		if(parameters[0] != this.parameter[0] || parameters[1] != this.parameter[1] || parameters[2] != this.parameter[2] || parameters[3] != this.parameter[3]) {
			model.volatilityModel	= new LIBORVolatilityModelFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameters[0], parameters[1], parameters[2], parameters[3], false);
		}
		if(parameters[4] != this.parameter[4]) {
			model.correlationModel	= new LIBORCorrelationModelExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameters[4], false);
		}

		return model;
	}

	@Override
	public double[] getParameter() {
		return parameter.clone();
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		RandomVariable[] factorLoading = new RandomVariable[correlationModel.getNumberOfFactors()];
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			RandomVariable volatility = volatilityModel.getVolatility(timeIndex, component);
			factorLoading[factorIndex] = volatility
					.mult(correlationModel.getFactorLoading(timeIndex, factorIndex, component));
		}

		return factorLoading;
	}

	@Override
	public RandomVariableFromDoubleArray getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}
}
