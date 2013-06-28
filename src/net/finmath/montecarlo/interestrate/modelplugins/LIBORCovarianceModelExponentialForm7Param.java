/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

public class LIBORCovarianceModelExponentialForm7Param extends AbstractLIBORCovarianceModelParametric {

	private double[] parameter = new double[7];

	private LIBORVolatilityModelMaturityDependentFourParameterExponentialForm	volatilityModel;
	private LIBORCorrelationModelThreeParameterExponentialDecay					correlationModel;
	
	public LIBORCovarianceModelExponentialForm7Param(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);
		
		parameter[0] = 0.1;
		parameter[1] = 0.1;
		parameter[2] = 0.1;
		parameter[3] = 0.2;
		parameter[4] = 0.1;
		parameter[5] = 0.1;
		parameter[6] = 0.1;
		
		setParameter(parameter);
	}

	@Override
	public Object clone() {
		LIBORCovarianceModelExponentialForm7Param model = new LIBORCovarianceModelExponentialForm7Param(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), this.getNumberOfFactors());
		model.setParameter(this.getParameter());
		return model;
	}
	
	@Override
	public double[] getParameter() {
		return parameter;
	}

	@Override
	public void setParameter(double[] parameter) {
		if(parameter[4] < 0) parameter[4] = Math.max(parameter[4], 0.0);
		
		this.parameter = parameter;

		volatilityModel		= new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameter[0], parameter[1], parameter[2], parameter[3]);
		correlationModel	= new LIBORCorrelationModelThreeParameterExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameter[4], parameter[5], parameter[6], false);
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, ImmutableRandomVariableInterface[] realizationAtTimeIndex) {
		RandomVariableInterface factorLoading[] = new RandomVariableInterface[correlationModel.getNumberOfFactors()];
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			RandomVariableInterface volatility = volatilityModel.getVolatility(timeIndex, component);
			factorLoading[factorIndex] = volatility;
			factorLoading[factorIndex] = factorLoading[factorIndex].mult(correlationModel.getFactorLoading(timeIndex, factorIndex, component));
		}
		
		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, ImmutableRandomVariableInterface[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}

}
