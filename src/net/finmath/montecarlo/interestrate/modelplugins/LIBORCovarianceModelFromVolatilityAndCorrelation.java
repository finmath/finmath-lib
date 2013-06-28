/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 15.12.2007
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * A covariance model build from a volatility model implementing
 * <code>LIBORVolatilityModel</code> and a correlation model
 * implementing <code>LIBORCorrelationModel</code>.
 * 
 * The model parameters are given by the concatenation of the
 * parameters of the <code>LIBORVolatilityModel</code> and
 * the parameters of the <code>LIBORCorrelationModel</code>,
 * in this ordering
 * 
 * @author Christian Fries
 */
public class LIBORCovarianceModelFromVolatilityAndCorrelation extends AbstractLIBORCovarianceModelParametric {

	public final LIBORVolatilityModel	volatilityModel;
	public final LIBORCorrelationModel	correlationModel;
	
	public LIBORCovarianceModelFromVolatilityAndCorrelation(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization, LIBORVolatilityModel volatilityModel, LIBORCorrelationModel correlationModel) {
		super(timeDiscretization, liborPeriodDiscretization, correlationModel.getNumberOfFactors());

		this.volatilityModel = volatilityModel;
		this.correlationModel = correlationModel;
	}

	@Override
    public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, ImmutableRandomVariableInterface[] realizationAtTimeIndex) {
		RandomVariableInterface[] factorLoading = new RandomVariableInterface[correlationModel.getNumberOfFactors()];

		RandomVariableInterface volatility	= volatilityModel.getVolatility(timeIndex, component);
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			factorLoading[factorIndex] = volatility.mult(correlationModel.getFactorLoading(timeIndex, factorIndex, component));
		}
		
		return factorLoading;
	}
	
	@Override
    public RandomVariableInterface getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, ImmutableRandomVariableInterface[] realizationAtTimeIndex) {
		// Note that we assume that the correlation model getFactorLoading gives orthonormal vectors
		RandomVariableInterface factorLoadingPseudoInverse = volatilityModel.getVolatility(timeIndex, component).invert()
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
     * @see net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel#getCovariance(int, int, int)
     */
    @Override
    public RandomVariableInterface getCovariance(int timeIndex, int component1, int component2, ImmutableRandomVariableInterface[] realizationAtTimeIndex) {
        RandomVariableInterface covariance = new RandomVariable(0.0, correlationModel.getCorrelation(timeIndex, component1, component2));
        covariance = covariance.mult(volatilityModel.getVolatility(timeIndex, component1))
                .mult(volatilityModel.getVolatility(timeIndex, component2));

        return covariance;
    }

	@Override
	public double[] getParameter() {
		double[] volatilityParameter	= volatilityModel.getParameter();
		double[] correlationParameter	= correlationModel.getParameter();
		
		int parameterLength = 0;
		parameterLength += volatilityParameter	!= null ? volatilityParameter.length : 0;
		parameterLength += correlationParameter != null ? correlationParameter.length : 0;
		
		double[] parameter = new double[parameterLength];
		
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
	public void setParameter(double[] parameter) {
		double[] volatilityParameter = volatilityModel.getParameter();
		double[] correlationParameter = correlationModel.getParameter();

		int parameterIndex = 0;
		if(volatilityParameter != null) {
			System.arraycopy(parameter, parameterIndex, volatilityParameter, 0, volatilityParameter.length);
			parameterIndex += volatilityParameter.length;
			volatilityModel.setParameter(volatilityParameter);
		}
		if(correlationParameter != null) {
			System.arraycopy(parameter, parameterIndex, correlationParameter, 0, correlationParameter.length);
			parameterIndex += correlationParameter.length;
			correlationModel.setParameter(correlationParameter);
		}
	}

	@Override
	public Object clone() {
		return new LIBORCovarianceModelFromVolatilityAndCorrelation(
				this.getTimeDiscretization(),
				this.getLiborPeriodDiscretization(),
				(LIBORVolatilityModel)volatilityModel.clone(), (LIBORCorrelationModel)correlationModel.clone());
	}

	public LIBORVolatilityModel getVolatilityModel() {
		return volatilityModel;
	}

	public LIBORCorrelationModel getCorrelationModel() {
		return correlationModel;
	}
}
