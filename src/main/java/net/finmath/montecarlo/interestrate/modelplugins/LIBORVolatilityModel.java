/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.io.Serializable;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Abstract base class and interface description of a volatility model
 * (as it is used in {@link LIBORCovarianceModelFromVolatilityAndCorrelation}).
 *
 * Derive from this class and implement the <code>getVolatlity</code> method.
 * You have to call the constructor of this class to set the time
 * discretizations.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class LIBORVolatilityModel implements Serializable {

	private static final long serialVersionUID = 5481713000841480672L;

	private TimeDiscretization	timeDiscretization;
	private TimeDiscretization	liborPeriodDiscretization;

	// You cannot instantiate the class empty
	@SuppressWarnings("unused")
	private LIBORVolatilityModel() {
	}

	/**
	 * @param timeDiscretizationFromArray The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 */
	public LIBORVolatilityModel(TimeDiscretization timeDiscretization, TimeDiscretization liborPeriodDiscretization) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.liborPeriodDiscretization = liborPeriodDiscretization;
	}

	public abstract double[]	getParameter();
	public abstract LIBORVolatilityModel	getCloneWithModifiedParameter(double[] parameter);

	/**
	 * Implement this method to complete the implementation.
	 * @param timeIndex The time index (for timeDiscretizationFromArray)
	 * @param component The libor index (for liborPeriodDiscretization)
	 * @return A random variable (e.g. as a vector of doubles) representing the volatility for each path.
	 */
	public abstract RandomVariable getVolatility(int timeIndex, int component);

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
