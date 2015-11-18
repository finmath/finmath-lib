/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import org.joda.time.LocalDate;

/**
 * Implementation of 30E/360 and 30E+/360.
 * 
 * Calculates the day count of 30E/360 as
 * <code>
 *		(endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay, 30.0) - Math.min(startDateDay, 30.0))
 * </code>
 * 
 * This day count convention is sometime called <i>Eurobond basis</i> or <i>30/360 ISMA</i>.
 * 
 * For 30E/360 we have that:
 * <ul>
 * 	<li>
 * 		The method {@link #getDaycount(LocalDate, LocalDate) getDaycount} corresponds to the implementation of the "European method" of Excel function DAYS360, i.e., DAYS360(startDate,endDate,TRUE).
 * 	</li>
 * 	<li>
 * 		The method {@link #getDaycountFraction(LocalDate, LocalDate) getDaycountFraction} corresponds to the implementation of the "30E/360 method" of Excel function YEARFRAC, i.e., YEARFRAC(startDate,endDate,4).
 * 	</li>
 * </ul>
 * 
 * The day count of 30E+/360 is that of 30E/360 whenever endDateDay is &le; 30, otherwise it is that of 30E/360 plus one.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30E_360 implements DayCountConventionInterface {

	private final boolean is30Eplus360;
	
	/**
	 * Create a 30E/360 or 30E+/360 day count convention.
	 * 
	 * @param is30Eplus360 If true, then 30E+/360 is constructed, otherwise 30E/360 is constructed.
	 */
	public DayCountConvention_30E_360(boolean is30Eplus360) {
		this.is30Eplus360 = is30Eplus360;
	}

	/**
	 * Create a 30E/360 daycount convention.
	 */
	public DayCountConvention_30E_360() {
		this(false);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycount(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) return -getDaycount(endDate,startDate);

		int startDateDay 	= startDate.getDayOfMonth();
		int startDateMonth 	= startDate.getMonthOfYear();
		int startDateYear 	= startDate.getYear();
		
		int endDateDay 		= endDate.getDayOfMonth();
		int endDateMonth 	= endDate.getMonthOfYear();
		int endDateYear 	= endDate.getYear();

		double daycount = (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay, 30.0) - Math.min(startDateDay, 30.0));

		if(is30Eplus360 && endDateDay == 31) daycount +=1.0;
		
		return daycount;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycountFraction(LocalDate startDate, LocalDate endDate) {
		return getDaycount(startDate, endDate) / 360.0;
	}
}
