/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.io.Serializable;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Abstract base class and interface description of a volatility model
 * (as it is used in {@link LIBORCovarianceModelFromVolatilityAndCorrelation}).
 * 
 * Derive from this class and implement the <code>getVolatlity</code> method.
 * You have to call the constructor of this class to set the time
 * discretizations.
 * 
 * @author Christian Fries
 */
public abstract class LIBORVolatilityModel implements Serializable {

	private static final long serialVersionUID = 5481713000841480672L;

	private TimeDiscretizationInterface	timeDiscretization;
	private TimeDiscretizationInterface	liborPeriodDiscretization;

	// You cannot instantiate the class empty
	@SuppressWarnings("unused")
	private LIBORVolatilityModel() {
	}

	/**
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 */
	public LIBORVolatilityModel(TimeDiscretizationInterface timeDiscretization, TimeDiscretizationInterface liborPeriodDiscretization) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.liborPeriodDiscretization = liborPeriodDiscretization;
	}

	public abstract double[]	getParameter();
	public abstract LIBORVolatilityModel	getCloneWithModifiedParameter(double[] parameter);

	/**
	 * Implement this method to complete the implementation.
	 * @param timeIndex The time index (for timeDiscretization)
	 * @param component The libor index (for liborPeriodDiscretization)
	 * @return A random variable (e.g. as a vector of doubles) representing the volatility for each path.
	 */
	public abstract RandomVariableInterface getVolatility(int timeIndex, int component);

	/**
	 * @return Returns the liborPeriodDiscretization.
	 */
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return liborPeriodDiscretization;
	}

	/**
	 * @return Returns the timeDiscretization.
	 */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	public abstract Object clone();
}
