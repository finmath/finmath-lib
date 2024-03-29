/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.09.2013
 */

package net.finmath.time.daycount;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * Factory methods for day count conventions.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DayCountConventionFactory implements Serializable {

	private static final long serialVersionUID = -3477824315144792988L;

	/**
	 * Factory methods for day count conventions.
	 */
	private DayCountConventionFactory() {
	}

	/**
	 * Create a day count convention base on a convention string.
	 * The follwoing convention strings are supported
	 * <ul>
	 * 	<li>act/act isda</li>
	 * 	<li>30/360</li>
	 * 	<li>30E/360</li>
	 * 	<li>30U/360</li>
	 * 	<li>act/360</li>
	 * 	<li>act/365</li>
	 * 	<li>act/act yearfrac</li>
	 * </ul>
	 *
	 * @param convention A convention string.
	 * @return A day count convention object.
	 */
	public static DayCountConvention getDayCountConvention(String convention) {
		if(convention == null) {
			throw new IllegalArgumentException();
		}

		convention = convention.replace('.', ' ').toLowerCase();

		switch(convention) {
			case "30e/360 isda":
			case "e30/360 isda":
			case "30/360":
				return new DayCountConvention_30E_360_ISDA();
			case "30e/360":
			case "e30/360":
				return new DayCountConvention_30E_360();
			case "30u/360":
			case "u30/360":
				return new DayCountConvention_30U_360();
			case "act/360":
				return new DayCountConvention_ACT_360();
			case "act/365":
			case "act/365 fixed":
				return new DayCountConvention_ACT_365();
			case "act/act":
			case "act/act isda":
				return new DayCountConvention_ACT_ACT_ISDA();
			case "act/act yearfrac":
				return new DayCountConvention_ACT_ACT_YEARFRAC();
			case "act/act afb":
				return new DayCountConvention_ACT_ACT_AFB();
			default:
		}
		throw new IllegalArgumentException("Unknow day count convention: " + convention);
	}

	/**
	 * Return the number of days between startDate and endDate given the
	 * specific daycount convention.
	 *
	 * @param startDate The start date given as a {@link java.time.LocalDate}.
	 * @param endDate The end date given as a {@link java.time.LocalDate}.
	 * @param convention A convention string.
	 * @return The number of days within the given period.
	 */
	public static double getDaycount(final LocalDate startDate, final LocalDate endDate, final String convention) {
		final DayCountConvention daycountConvention = getDayCountConvention(convention);
		return daycountConvention.getDaycount(startDate, endDate);
	}

	/**
	 * Return the daycount fraction corresponding to the period from startDate to endDate given the
	 * specific daycount convention.
	 *
	 * @param startDate The start date given as a {@link java.time.LocalDate}.
	 * @param endDate The end date given as a {@link java.time.LocalDate}.
	 * @param convention A convention string.
	 * @return The daycount fraction corresponding to the given period.
	 */
	public static double getDaycountFraction(final LocalDate startDate, final LocalDate endDate, final String convention) {
		final DayCountConvention daycountConvention = getDayCountConvention(convention);
		return daycountConvention.getDaycountFraction(startDate, endDate);
	}

}
