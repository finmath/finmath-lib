/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Implements a placeholder object for an unknown day count convention, throwing an exception,
 * whenever a day count or day count fraction is requested.
 *
 * This class may become handy, if you like to construct products based on possibly incomplete data
 * where an exception is thrown late, i.e., not at construction time, but at evaluation time.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DayCountConvention_UNKNOWN implements DayCountConvention, Serializable {

	private static final long serialVersionUID = -3204506898077711668L;

	private final String errorMessage;

	/**
	 * Create the unknown day count convention.
	 */
	public DayCountConvention_UNKNOWN() {
		errorMessage = "Requesting day count on an unknown day count convention.";
	}

	/**
	 * Create the unknown day count convention.
	 *
	 * @param name A given name, which will be shown in the {@link IllegalArgumentException} when any method is called on this object.
	 */
	public DayCountConvention_UNKNOWN(final String name) {
		errorMessage = "Requesting day count on an unknown day count convention (name=" + name + ").";
	}

	@Override
	public double getDaycount(final LocalDate startDate, final LocalDate endDate) {
		throw new IllegalArgumentException(errorMessage);
	}

	@Override
	public double getDaycountFraction(final LocalDate startDate, final LocalDate endDate) {
		throw new IllegalArgumentException(errorMessage);
	}
}
