/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.io.Serializable;

import net.finmath.time.TimeDiscretization;


/**
 * Abstract base class and interface description of a correlation model
 * (as it is used in {@link LIBORCovarianceModelFromVolatilityAndCorrelation}).
 *
 * Derive from this class and implement the <code>getFactorLoading</code> method.
 * You have to call the constructor of this class to set the time
 * discretizations.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class LIBORCorrelationModel implements Serializable {

	private static final long serialVersionUID = -6780424108470638825L;

	final TimeDiscretization	timeDiscretization;
	final TimeDiscretization	liborPeriodDiscretization;

	public LIBORCorrelationModel(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.liborPeriodDiscretization = liborPeriodDiscretization;
	}

	public abstract double[]	getParameter();
	public abstract LIBORCorrelationModel		getCloneWithModifiedParameter(double[] parameter);

	public abstract	double	getFactorLoading(int timeIndex, int factor, int component);
	public abstract	double	getCorrelation(int timeIndex, int component1, int component2);
	public abstract int		getNumberOfFactors();

	/**
	 * @return Returns the liborPeriodDiscretization.
	 */
	public TimeDiscretization getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/**
	 * @return Returns the timeDiscretizationFromArray.
	 */
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public abstract Object clone();
}
