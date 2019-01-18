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
 * @author Christian Fries
 *
 * @version 1.0
 */
public class LIBORCovarianceModelExponentialForm7Param extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = -7980504204664006566L;

	private double[] parameter = new double[7];

	private LIBORVolatilityModelMaturityDependentFourParameterExponentialForm	volatilityModel;
	private LIBORCorrelationModelThreeParameterExponentialDecay					correlationModel;

	public LIBORCovarianceModelExponentialForm7Param(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, int numberOfFactors) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);

		parameter[0] = 0.1;
		parameter[1] = 0.1;
		parameter[2] = 0.1;
		parameter[3] = 0.2;
		parameter[4] = 0.1;
		parameter[5] = 0.1;
		parameter[6] = 0.1;

		volatilityModel	= new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameter[0], parameter[1], parameter[2], parameter[3]);
		correlationModel	= new LIBORCorrelationModelThreeParameterExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameter[4], parameter[5], parameter[6], false);
	}

	@Override
	public Object clone() {
		LIBORCovarianceModelExponentialForm7Param model = new LIBORCovarianceModelExponentialForm7Param(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), this.getNumberOfFactors());
		model.parameter = parameter;
		model.volatilityModel = volatilityModel;
		model.correlationModel = correlationModel;
		return model;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		LIBORCovarianceModelExponentialForm7Param model = (LIBORCovarianceModelExponentialForm7Param)this.clone();

		model.parameter = parameters;
		if(parameters[0] != this.parameter[0] || parameters[1] != this.parameter[1] || parameters[2] != this.parameter[2] || parameters[3] != this.parameter[3]) {
			model.volatilityModel	= new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameters[0], parameters[1], parameters[2], parameters[3]);
		}
		if(parameters[4] != this.parameter[4] || parameters[5] != this.parameter[5] || parameters[6] != this.parameter[6]) {
			model.correlationModel	= new LIBORCorrelationModelThreeParameterExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameters[4], parameters[5], parameters[6], false);
		}

		return model;
	}

	@Override
	public double[] getParameter() {
		return parameter;
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		RandomVariable factorLoading[] = new RandomVariable[correlationModel.getNumberOfFactors()];
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			RandomVariable volatility = volatilityModel.getVolatility(timeIndex, component);
			factorLoading[factorIndex] = volatility;
			factorLoading[factorIndex] = factorLoading[factorIndex].mult(correlationModel.getFactorLoading(timeIndex, factorIndex, component));
		}

		return factorLoading;
	}

	@Override
	public RandomVariableFromDoubleArray getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}

}
