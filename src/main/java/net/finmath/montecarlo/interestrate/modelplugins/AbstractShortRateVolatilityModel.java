/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import java.io.Serializable;

import net.finmath.time.TimeDiscretization;

/**
 * A base class and interface description for the instantaneous volatility of
 * an short rate model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractShortRateVolatilityModel implements ShortRateVolatilityModelInterface, Serializable {

	private static final long serialVersionUID = 5364544247367259329L;

	private	TimeDiscretization		timeDiscretization;

	/**
	 * Constructor consuming time discretizations, which are handled by the super class.
	 *
	 * @param timeDiscretizationFromArray The vector of simulation time discretization points.
	 */
	public AbstractShortRateVolatilityModel(TimeDiscretization timeDiscretization) {
		super();
		this.timeDiscretization			= timeDiscretization;
	}

	/**
	 * The simulation time discretization associated with this model.
	 *
	 * @return the timeDiscretizationFromArray
	 */
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}
}
