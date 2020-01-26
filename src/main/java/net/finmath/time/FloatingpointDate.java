/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 16.05.2017
 */
package net.finmath.time;


import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * This class provides the library wide conversion from a floating point number to a LocalDate.
 *
 * While in practical applications a date or time should always be represented with a proper
 * date class, e.g. <code>LocalDate</code> or <code>LocalDateTime</code>. In financial applications the measurement
 * of time distances has to be based on a solid definition, e.g., daycount conventions to calculate
 * daycount fractions.
 *
 * However, many mathematical models described in text books rely on time being modeled as some
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
 * The specification of this internal time representation is to some extend just a convention which defines the unit of internal model parameters.
 * A constant volatility measured against a floating point time ACT/365 corresponds 1:1: to a constant volatility measure again a floating point time ACT/360
 * with a conversion factor of 360/365. However, the situation is not always trivial, since a constant volatility measured against a floating point time ACT/365
 * would correspond to a weakly time-dependent volatility volatility measured against ACT/ACT.
 *
 * Such nuances represent differences in the model, a model calibration will ensure that the parameters are choosen such that the model matches observed quantities
 * independent from the convention used to express internal model parameters.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class FloatingpointDate {

	private static final long SECONDS_PER_DAY = 365*24*60*60;

	private static DayCountConvention	internalDayCounting = new DayCountConvention_ACT_365();

	private FloatingpointDate() {
	}

	/**
	 * Convert a floating point date to a LocalDateTime.
	 *
	 * Note: This method currently performs a rounding to the next second.
	 *
	 * If referenceDate is null, the method returns null.
	 *
	 * @param referenceDate The reference date associated with \( t=0 \).
	 * @param floatingPointDate The value to the time offset \( t \).
	 * @return The date resulting from adding Math.round(fixingTime*SECONDS_PER_DAY) seconds to referenceDate, where one day has SECONDS_PER_DAY seconds and SECONDS_PER_DAY is a constant 365*24*60*60
	 */
	public static LocalDateTime getDateFromFloatingPointDate(final LocalDateTime referenceDate, final double floatingPointDate) {
		if(referenceDate == null) {
			return null;
		}

		final Duration duration = Duration.ofSeconds(Math.round(floatingPointDate * SECONDS_PER_DAY));
		return referenceDate.plus(duration);
	}

	/**
	 * Convert a given date to a floating point date using a given reference date.
	 *
	 * @param referenceDate The reference date associated with \( t=0 \).
	 * @param date The given date to be associated with the return value \( T \).
	 * @return The value T measuring the distance of reference date and date by ACT/365 with SECONDS_PER_DAY seconds used as the smallest time unit and SECONDS_PER_DAY is a constant 365*24*60*60.
	 */
	public static double getFloatingPointDateFromDate(final LocalDateTime referenceDate, final LocalDateTime date) {
		final Duration duration = Duration.between(referenceDate, date);
		return ((double)duration.getSeconds()) / SECONDS_PER_DAY;
	}

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
	public static LocalDate getDateFromFloatingPointDate(final LocalDate referenceDate, final double floatingPointDate) {
		if(referenceDate == null) {
			return null;
		}
		return referenceDate.plusDays((int)Math.round(floatingPointDate*365.0));
	}

	/**
	 * Convert a given date to a floating point date using a given reference date.
	 *
	 * @param referenceDate The reference date associated with \( t=0 \).
	 * @param date The given daten to be associated with the return value \( T \).
	 * @return The value T measuring the distance of reference date and date by ACT/365.
	 */
	public static double getFloatingPointDateFromDate(final LocalDate referenceDate, final LocalDate date) {
		return internalDayCounting.getDaycountFraction(referenceDate, date);
	}
}
