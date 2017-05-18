/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 16.05.2017
 */
package net.finmath.time;


import java.time.LocalDate;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * This class provides the library wide conversion from a floating point number to a LocalDate.
 * 
 * While in practical applications a date or time should always be represented with a proper
 * date class, e.g. <code>LocalDate</code>. In financial applications the measurement
 * of time distances has to be based on a solid definition, e.g., daycount conventions to calculate
 * daycount fractions.
 * 
 * However, many mathematical models described in text books rely on time being model as some
 * real value \( t \).
 * 
 * To allow for both approaches to co-exists this class fixes the interpretation of a floating
 * point number representing time, <i>unless otherwise specified</i>.
 * 
 * So it still possible that models use their own "conversion".
 * 
 * Examples where the specification of this contract is important:
 * - the way of measuring time in an NSS curve determines the interpretation of the NSS parameters.
 * - in the textbook Black-Scholes models, multiplying volatility by W(t), changing from an ACT/365 to ACT/360 would represent a re-scaling of the volatilities.
 * 
 * @author Christian Fries
 */
public class FloatingpointDate {

	private static	DayCountConventionInterface	internalDayCounting = new DayCountConvention_ACT_365();
	
	/**
	 * Convert a floating point date to a LocalDate.
	 * 
	 * Note: This method currently performs a rounding to the next day.
	 * In a future extension intra-day time offsets may be considered.
	 * 
	 * If referenceDate is null, the method returns null.
	 * 
	 * @param referenceDate The reference date associated with \( t=0 \).
	 * @param floatingPointDate The value to the time offset \( t \).
	 * @return The date resulting from adding Math.round(fixingTime*365.0) days to referenceDate.
	 */
	public static LocalDate getDateFromFloatingPointDate(LocalDate referenceDate, double floatingPointDate) {
		if(referenceDate == null) return null;
		return referenceDate.plusDays((int)Math.round(floatingPointDate*365.0));
	}
	
	/**
	 * Convert a given date to a floating point date using a given reference date.
	 * 
	 * @param referenceDate The reference date associated with \( t=0 \).
	 * @param date The given daten to be associated with the return value \( T \).
	 * @return The value T measuring the distance of reference date and date by ACT/365.
	 */
	public static double getFloatingPointDateFromDate(LocalDate referenceDate, LocalDate date) {
		return internalDayCounting.getDaycountFraction(referenceDate, date);
	}
}
