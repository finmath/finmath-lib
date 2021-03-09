/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
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

	/**
	 *
	 */
	private static final long serialVersionUID = -6538642489767323201L;

	private RandomVariable[] parameter = new RandomVariable[5];

	private LIBORVolatilityModel		volatilityModel;
	private LIBORCorrelationModel	correlationModel;

	public LIBORCovarianceModelExponentialForm5Param(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors, final RandomVariable[] parameters) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);

		parameter = parameters.clone();
		volatilityModel	= new LIBORVolatilityModelFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameter[0], parameter[1], parameter[2], parameter[3], false);
		correlationModel	= new LIBORCorrelationModelExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameter[4].doubleValue(), false);
	}

	public LIBORCovarianceModelExponentialForm5Param(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors, final double[] parameters) {
		this(timeDiscretization, liborPeriodDiscretization, numberOfFactors, Scalar.arrayOf(parameters));
	}

	public LIBORCovarianceModelExponentialForm5Param(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors) {
		this(timeDiscretization, liborPeriodDiscretization, numberOfFactors, new double[] { 0.20, 0.05, 0.10, 0.20, 0.10});
	}
	@Override
	public Object clone() {
		final LIBORCovarianceModelExponentialForm5Param model = new LIBORCovarianceModelExponentialForm5Param(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), this.getNumberOfFactors(), this.getParameter());
		model.parameter = parameter;
		model.volatilityModel = volatilityModel;
		model.correlationModel = correlationModel;
		return model;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(final RandomVariable[] parameters) {
		final LIBORCovarianceModelExponentialForm5Param model = (LIBORCovarianceModelExponentialForm5Param)this.clone();

		model.parameter = parameters;
		if(parameters[0] != parameter[0] || parameters[1] != parameter[1] || parameters[2] != parameter[2] || parameters[3] != parameter[3]) {
			model.volatilityModel	= new LIBORVolatilityModelFourParameterExponentialForm(getTimeDiscretization(), getLiborPeriodDiscretization(), parameters[0], parameters[1], parameters[2], parameters[3], false);
		}
		if(parameters[4] != parameter[4]) {
			model.correlationModel	= new LIBORCorrelationModelExponentialDecay(getLiborPeriodDiscretization(), getLiborPeriodDiscretization(), getNumberOfFactors(), parameters[4].doubleValue(), false);
		}

		return model;
	}

	@Override
	public RandomVariable[] getParameter() {
		return parameter.clone();
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(final double[] parameters) {
		return getCloneWithModifiedParameters(Scalar.arrayOf(parameters));
	}

	@Override
	public double[] getParameterAsDouble() {
		final RandomVariable[] parameters = getParameter();
		final double[] parametersAsDouble = new double[parameters.length];
		for(int i=0; i<parameters.length; i++) {
			parametersAsDouble[i] = parameters[i].doubleValue();
		}
		return parametersAsDouble;
	}

	@Override
	public RandomVariable[] getFactorLoading(final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		final RandomVariable[] factorLoading = new RandomVariable[correlationModel.getNumberOfFactors()];
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			final RandomVariable volatility = volatilityModel.getVolatility(timeIndex, component);
			factorLoading[factorIndex] = volatility
					.mult(correlationModel.getFactorLoading(timeIndex, factorIndex, component));
		}

		return factorLoading;
	}

	@Override
	public RandomVariableFromDoubleArray getFactorLoadingPseudoInverse(final int timeIndex, final int component, final int factor, final RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(final Map<String, Object> dataModified)
			throws CalculationException {
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		int numberOfFactors = this.getNumberOfFactors();
		RandomVariable[] parameter = this.parameter;
		RandomVariableFactory randomVariableFactory = null;

		if(dataModified != null) {
			if(dataModified.containsKey("randomVariableFactory")) {
				randomVariableFactory = (RandomVariableFactory)dataModified.get("randomVariableFactory");
				parameter = randomVariableFactory.createRandomVariableArray(Arrays.stream(parameter).mapToDouble(new ToDoubleFunction<RandomVariable>() {
					@Override
					public double applyAsDouble(final RandomVariable para) {
						return para.doubleValue();
					}
				}).toArray());
			}

			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			numberOfFactors = (int)dataModified.getOrDefault("numberOfFactors", numberOfFactors);

			if(dataModified.getOrDefault("parameter", parameter) instanceof RandomVariable[]) {
				parameter = (RandomVariable[])dataModified.getOrDefault("parameter", parameter);
			}else if(randomVariableFactory==null){
				parameter = Scalar.arrayOf((double[])dataModified.get("parameter"));
			}else {
				parameter = randomVariableFactory.createRandomVariableArray((double[])dataModified.get("parameter"));
			}
		}

		final AbstractLIBORCovarianceModelParametric newModel = new LIBORCovarianceModelExponentialForm5Param(timeDiscretization, liborPeriodDiscretization, numberOfFactors, parameter);
		return newModel;
	}
}
