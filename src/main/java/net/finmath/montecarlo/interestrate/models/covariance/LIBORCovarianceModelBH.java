/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 16.09.2007
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * A five parameter covariance model corresponding.
 *
 * The model is provided for analysis / illustration. It has some bad properties.
 * Use in production in not recommended.
 *
 * @author Christian Fries
 * @since finmath-lib 3.6.0
 * @version 1.0
 */
public class LIBORCovarianceModelBH extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = 2094266336585778694L;

	private double[] parameter = new double[5]; // sigma1,alpha1,sigma2,alpha2,rho

	/**
	 * Create model.
	 *
	 * @param timeDiscretization The simulation time discretization.
	 * @param liborPeriodDiscretization The fixed forward rate discretization.
	 * @param numberOfFactors The number of factors.
	 * @param parameter Vector of size 5.
	 */
	public LIBORCovarianceModelBH(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors, final double[] parameter) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);
		this.parameter = parameter;
	}

	/**
	 * Create model with default parameter.
	 *
	 * @param timeDiscretization The simulation time discretization.
	 * @param liborPeriodDiscretization The fixed forward rate discretization.
	 * @param numberOfFactors The number of factors.
	 */
	public LIBORCovarianceModelBH(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization, final int numberOfFactors) {
		super(timeDiscretization, liborPeriodDiscretization, numberOfFactors);
		parameter[0] =  0.4690;		// sigma1
		parameter[1] =  0.0452;		// alpha1
		parameter[2] =  0.3500;		// sigma2
		parameter[3] =  0.0100;		// alpha2
		parameter[4] = -0.8918;		// rho
	}

	@Override
	public Object clone() {
		final LIBORCovarianceModelBH model = new LIBORCovarianceModelBH(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), this.getNumberOfFactors(), this.getParameterAsDouble());
		return model;
	}

	@Override
	public double[] getParameterAsDouble() {
		return parameter;
	}

	@Override
	public RandomVariable[] getFactorLoading(final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		final double timeToMaturity = getLiborPeriodDiscretization().getTime(component) - getTimeDiscretization().getTime(timeIndex);

		final double s1 = timeToMaturity <= 0 ? 0 : parameter[0] * Math.exp(-parameter[1] * timeToMaturity);
		final double s2 = timeToMaturity <= 0 ? 0 : parameter[2] * Math.exp(-parameter[3] * timeToMaturity);
		final double rho = parameter[4];

		final RandomVariable[] factorLoading = new RandomVariable[2];
		factorLoading[0] = new RandomVariableFromDoubleArray(getTimeDiscretization().getTime(timeIndex), Math.sqrt(1-rho*rho) * s1);
		factorLoading[1] = new RandomVariableFromDoubleArray(getTimeDiscretization().getTime(timeIndex), rho * s1 + s2);

		return factorLoading;
	}

	@Override
	public RandomVariableFromDoubleArray getFactorLoadingPseudoInverse(final int timeIndex, final int component, final int factor, final RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(final double[] parameters) {
		final LIBORCovarianceModelBH model = new LIBORCovarianceModelBH(this.getTimeDiscretization(), this.getLiborPeriodDiscretization(), this.getNumberOfFactors(), parameters);
		return model;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(final Map<String, Object> dataModified)
			throws CalculationException {
		TimeDiscretization timeDiscretization = this.getTimeDiscretization();
		TimeDiscretization liborPeriodDiscretization = this.getLiborPeriodDiscretization();
		int numberOfFactors = this.getNumberOfFactors();
		double[] parameter = this.parameter;

		if(dataModified != null) {
			timeDiscretization = (TimeDiscretization)dataModified.getOrDefault("timeDiscretization", timeDiscretization);
			liborPeriodDiscretization = (TimeDiscretization)dataModified.getOrDefault("liborPeriodDiscretization", liborPeriodDiscretization);
			numberOfFactors = (int)dataModified.getOrDefault("numberOfFactors", numberOfFactors);
			parameter = (double[])dataModified.getOrDefault("parameter", parameter);
		}

		final AbstractLIBORCovarianceModelParametric newModel = new LIBORCovarianceModelBH(timeDiscretization, liborPeriodDiscretization, numberOfFactors, parameter);
		return newModel;
	}

}
