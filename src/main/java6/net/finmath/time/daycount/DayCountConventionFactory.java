/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 20.09.2013
 */

package net.finmath.time.daycount;

import java.util.Date;

import org.joda.time.LocalDate;

/**
 * Factory methods for day count conventions.
 * 
 * @author Christian Fries
 */
public class DayCountConventionFactory {

	/**
	 * Factory methods for day count conventions.
	 */
	public DayCountConventionFactory() {
		// TODO Auto-generated constructor stub
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
	public static DayCountConventionInterface getDayCountConvention(String convention) {
		if(convention.compareToIgnoreCase("act/act isda") == 0) {
			return new DayCountConvention_ACT_ACT_ISDA();
		}
		else if(convention.compareToIgnoreCase("30/360") == 0) {
			return new DayCountConvention_30E_360_ISDA();
		}
		else if(convention.compareToIgnoreCase("30e/360") == 0) {
			return new DayCountConvention_30E_360();
		}
		else if(convention.compareToIgnoreCase("30u/360") == 0) {
			return new DayCountConvention_30U_360();
		}
		else if(convention.compareToIgnoreCase("act/360") == 0) {
			return new DayCountConvention_ACT_360();
		}
		else if(convention.compareToIgnoreCase("act/365") == 0) {
			return new DayCountConvention_ACT_365();
		}
		else if(convention.compareToIgnoreCase("act/act yearfrac") == 0) {
			return new DayCountConvention_ACT_ACT_YEARFRAC();
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
	public static double getDaycount(LocalDate startDate, LocalDate endDate, String convention) {
		DayCountConventionInterface daycountConvention = getDayCountConvention(convention);
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
	public static double getDaycountFraction(LocalDate startDate, LocalDate endDate, String convention) {
		DayCountConventionInterface daycountConvention = getDayCountConvention(convention);
		return daycountConvention.getDaycountFraction(startDate, endDate);
	}	
	
	/**
	 * Return the daycount fraction corresponding to the period from startDate to endDate given the
	 * specific daycount convention.
	 * 
	 * Method using <code>java.util.Date</code> instead of <code>LocalDate</code> for backward compatibility.
	 * 
	 * @param startDate The start date given as a {@link java.time.LocalDate}.
	 * @param endDate The end date given as a {@link java.time.LocalDate}.
	 * @param convention A convention string.
	 * @return The daycount fraction corresponding to the given period.
	 */
	public static double getDaycountFraction(Date startDate, Date endDate, String convention) {
		return getDaycountFraction(new LocalDate(startDate), new LocalDate(endDate), convention);
	}	
}
