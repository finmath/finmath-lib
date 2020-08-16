/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.io.Serializable;
import java.util.Map;

import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.RandomVariableArrayImplementation;
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
	 * @param timeDiscretization The vector of simulation time discretization points.
	 * @param liborPeriodDiscretization The vector of tenor discretization points.
	 */
	public LIBORVolatilityModel(final TimeDiscretization timeDiscretization, final TimeDiscretization liborPeriodDiscretization) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.liborPeriodDiscretization = liborPeriodDiscretization;
	}

	public abstract RandomVariable[] getParameter();
	public abstract LIBORVolatilityModel getCloneWithModifiedParameter(RandomVariable[] parameter);

	/**
	 * Implement this method to complete the implementation.
	 * @param timeIndex The time index (for timeDiscretizationFromArray)
	 * @param component The libor index (for liborPeriodDiscretization)
	 * @return A random variable (e.g. as a vector of doubles) representing the volatility for each path.
	 */
	public abstract RandomVariable getVolatility(int timeIndex, int component);

	public double[] getParameterAsDouble() {
		return (double[])(RandomVariableArrayImplementation.of(getParameter())).toDoubleArray();
	}

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

	/**
	 * Returns a clone of this model where the specified properties have been modified.
	 *
	 * Note that there is no guarantee that a model reacts on a specification of a properties in the
	 * parameter map <code>dataModified</code>. If data is provided which is ignored by the model
	 * no exception may be thrown.
	 *
	 * Furthermore the structure of the correlation model has to match changed data.
	 * A change of the time discretizations may requires a change in the parameters
	 * but this function will just insert the new time discretization without
	 * changing the parameters. An exception may not be thrown.
	 *
	 * @param dataModified Key-value-map of parameters to modify.
	 * @return A clone of this model (or a new instance of this model if no parameter was modified).
	 */
	public abstract LIBORVolatilityModel getCloneWithModifiedData(Map<String, Object> dataModified);
}
