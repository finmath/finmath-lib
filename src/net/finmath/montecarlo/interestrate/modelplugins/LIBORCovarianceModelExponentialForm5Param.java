/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

public class LIBORCovarianceModelExponentialForm5Param extends AbstractLIBORCovarianceModelParametric {

	double[] parameter = new double[5];

	LIBORVolatilityModelMaturityDependentFourParameterExponentialForm	volatilityModel;
	LIBORCorrelationModelExponentialDecay				correlationModel;
	
	public LIBORCovarianceModelExponentialForm5Param(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, int numberOfFactors) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);
		
		parameter[0] = 0.20;
		parameter[1] = 0.05;
		parameter[2] = 0.10;
		parameter[3] = 0.20;
		parameter[4] = 0.10;
		
		setParameter(parameter);
	}

	@Override
	public Object clone() {
		LIBORCovarianceModelExponentialForm5Param model = new LIBORCovarianceModelExponentialForm5Param(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), this.getNumberOfFactors());
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

		volatilityModel		= new LIBORVolatilityModelMaturityDependentFourParameterExponentialForm(liborPeriodDiscretization, liborPeriodDiscretization, parameter[0], parameter[1], parameter[2], parameter[3]);
		correlationModel	= new LIBORCorrelationModelExponentialDecay(liborPeriodDiscretization, liborPeriodDiscretization, numberOfFactors, parameter[4], false);
	}

	@Override
	public RandomVariableInterface getFactorLoading(int timeIndex, int factor, int component) {
        RandomVariableInterface volatilityInstanteaneous	= this.volatilityModel.getVolatility(timeIndex, component);
        double factorLoading			= this.correlationModel.getFactorLoading(timeIndex, factor, component);

        return volatilityInstanteaneous.mult(factorLoading);
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor) {
		throw new UnsupportedOperationException();
	}

}
