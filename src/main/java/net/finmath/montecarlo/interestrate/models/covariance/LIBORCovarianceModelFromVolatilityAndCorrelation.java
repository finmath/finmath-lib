/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.12.2007
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Arrays;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

/**
 * A covariance model build from a volatility model implementing
 * <code>LIBORVolatilityModel</code> and a correlation model
 * implementing <code>LIBORCorrelationModel</code>.
 *
 * <p>
 * The model parameters are given by the concatenation of the
 * parameters of the <code>LIBORVolatilityModel</code> and
 * the parameters of the <code>LIBORCorrelationModel</code>,
 * in this ordering
 * </p>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LIBORCovarianceModelFromVolatilityAndCorrelation extends AbstractLIBORCovarianceModelParametric {

	/**
	 *
	 */
	private static final long serialVersionUID = -8782024526695367005L;
	private LIBORVolatilityModel	volatilityModel;
	private LIBORCorrelationModel	correlationModel;

	public LIBORCovarianceModelFromVolatilityAndCorrelation(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization, LIBORVolatilityModel volatilityModel, LIBORCorrelationModel correlationModel) {
		super(timeDiscretization, liborPeriodDiscretization, correlationModel.getNumberOfFactors());

		this.volatilityModel = volatilityModel;
		this.correlationModel = correlationModel;
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		RandomVariable[] factorLoading = new RandomVariable[correlationModel.getNumberOfFactors()];

		RandomVariable volatility	= volatilityModel.getVolatility(timeIndex, component);
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			factorLoading[factorIndex] = volatility.mult(correlationModel.getFactorLoading(timeIndex, factorIndex, component));
		}

		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex) {
		// Note that we assume that the correlation model getFactorLoading gives orthonormal vectors
		RandomVariable factorLoadingPseudoInverse = volatilityModel.getVolatility(timeIndex, component).invert()
				.mult(correlationModel.getFactorLoading(timeIndex, factor, component));

		// @todo numberOfComponents should be stored as a member?!
		int numberOfComponents = getLiborPeriodDiscretization().getNumberOfTimeSteps();

		double factorWeight = 0.0;
		for(int componentIndex=0; componentIndex<numberOfComponents; componentIndex++) {
			double factorElement = correlationModel.getFactorLoading(timeIndex, factor, componentIndex);
			factorWeight +=  factorElement*factorElement;
		}

		factorLoadingPseudoInverse = factorLoadingPseudoInverse.mult(1/factorWeight);

		return factorLoadingPseudoInverse;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.models.covariance.AbstractLIBORCovarianceModel#getCovariance(int, int, int)
	 */
	@Override
	public RandomVariable getCovariance(int timeIndex, int component1, int component2, RandomVariable[] realizationAtTimeIndex) {

		RandomVariable volatilityOfComponent1 = volatilityModel.getVolatility(timeIndex, component1);
		RandomVariable volatilityOfComponent2 = volatilityModel.getVolatility(timeIndex, component2);

		double					correlationOfComponent1And2 = correlationModel.getCorrelation(timeIndex, component1, component2);

		RandomVariable covariance = volatilityOfComponent1.mult(volatilityOfComponent2).mult(correlationOfComponent1And2);

		return covariance;
	}

	@Override
	public RandomVariable[] getParameter() {
		RandomVariable[] volatilityParameter	= volatilityModel.getParameter();
		RandomVariable[] correlationParameter	= correlationModel.getParameter();

		int parameterLength = 0;
		parameterLength += volatilityParameter	!= null ? volatilityParameter.length : 0;
		parameterLength += correlationParameter != null ? correlationParameter.length : 0;

		RandomVariable[] parameter = new RandomVariable[parameterLength];

		int parameterIndex = 0;
		if(volatilityParameter != null) {
			System.arraycopy(volatilityParameter, 0, parameter, parameterIndex, volatilityParameter.length);
			parameterIndex += volatilityParameter.length;
		}
		if(correlationParameter != null) {
			System.arraycopy(correlationParameter, 0, parameter, parameterIndex, correlationParameter.length);
			parameterIndex += correlationParameter.length;
		}

		return parameter;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		return getCloneWithModifiedParameters(Scalar.arrayOf(parameters));
	}

	@Override
	public double[] getParameterAsDouble() {
		RandomVariable[] parameters = getParameter();
		double[] parametersAsDouble = new double[parameters.length];
		for(int i=0; i<parameters.length; i++) parametersAsDouble[i] = parameters[i].doubleValue();
		return parametersAsDouble;
	}

	@Override
	public Object clone() {
		return new LIBORCovarianceModelFromVolatilityAndCorrelation(
				this.getTimeDiscretization(),
				this.getLiborPeriodDiscretization(),
				(LIBORVolatilityModel)volatilityModel.clone(), (LIBORCorrelationModel)correlationModel.clone());
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariable[] parameters) {
		LIBORVolatilityModel volatilityModel = this.volatilityModel;
		LIBORCorrelationModel correlationModel = this.correlationModel;

		RandomVariable[] volatilityParameter = volatilityModel.getParameter();
		RandomVariable[] correlationParameter = correlationModel.getParameter();

		int parameterIndex = 0;
		if(volatilityParameter != null) {
			RandomVariable[] newVolatilityParameter = new RandomVariable[volatilityParameter.length];
			System.arraycopy(parameters, parameterIndex, newVolatilityParameter, 0, newVolatilityParameter.length);
			parameterIndex += newVolatilityParameter.length;
			if(!Arrays.equals(newVolatilityParameter, volatilityModel.getParameter())) {
				volatilityModel = volatilityModel.getCloneWithModifiedParameter(newVolatilityParameter);
			}
		}

		if(correlationParameter != null) {
			RandomVariable[] newCorrelationParameter = new RandomVariable[correlationParameter.length];
			System.arraycopy(parameters, parameterIndex, newCorrelationParameter, 0, newCorrelationParameter.length);
			parameterIndex += newCorrelationParameter.length;
			if(!Arrays.equals(newCorrelationParameter, correlationModel.getParameter())) {
				correlationModel = correlationModel.getCloneWithModifiedParameter(newCorrelationParameter);
			}
		}
		return new LIBORCovarianceModelFromVolatilityAndCorrelation(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), volatilityModel, correlationModel);
	}

	public LIBORVolatilityModel getVolatilityModel() {
		return volatilityModel;
	}

	public LIBORCorrelationModel getCorrelationModel() {
		return correlationModel;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(Map<String, Object> dataModified)
			throws CalculationException {
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		LIBORVolatilityModel volatilityModel = this.volatilityModel;
		LIBORCorrelationModel correlationModel = this.correlationModel;


		if(dataModified != null) {
			if((dataModified.containsKey("timeDiscretization")||dataModified.containsKey("liborPeriodDiscretization")||dataModified.containsKey("randomVariableFactory"))) {
				if(!dataModified.containsKey("volatilityModel")) {
					volatilityModel = volatilityModel.getCloneWithModifiedData(dataModified);
				}
				if(!dataModified.containsKey("correlationModel")) {
					correlationModel = correlationModel.getCloneWithModifiedData(dataModified);
				}
			}

			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);

			// the discretizations have to be compatible to the volatilityModels!
			volatilityModel = (LIBORVolatilityModel)dataModified.getOrDefault("volatilityModel", volatilityModel);
			correlationModel = (LIBORCorrelationModel)dataModified.getOrDefault("correlationModel", correlationModel);
		}

		AbstractLIBORCovarianceModelParametric newModel = new LIBORCovarianceModelFromVolatilityAndCorrelation(timeDiscretization, liborPeriodDiscretization, volatilityModel, correlationModel);
		return newModel;
	}
}
