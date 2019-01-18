/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.time.LocalDate;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface Tenor {

	/**
	 * @return The reference date of this tenor, i.e., the date mapped to 0.0
	 */
	LocalDate getReferenceDate();

	/**
	 * Returns the date for the given time index.
	 *
	 * @param timeIndex Time index
	 * @return Returns the date for a given time index.
	 */
	LocalDate getDate(int timeIndex);

	/**
	 * Returns the day count fraction for the period form timeIndex to to timeIndex+1.
	 *
	 * @param timeIndex Time index
	 * @return Returns the day count fraction.
	 */
	double getDaycountFraction(int timeIndex);
}
