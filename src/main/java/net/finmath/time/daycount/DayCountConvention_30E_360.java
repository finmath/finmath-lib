/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;

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
 * 		The method {@link #getDaycount(Calendar, Calendar) getDaycount} corresponds to the implementation of the "European method" of Excel function DAYS360, i.e., DAYS360(startDate,endDate,TRUE).
 * 	</li>
 * 	<li>
 * 		The method {@link #getDaycountFraction(Calendar, Calendar) getDaycountFraction} corresponds to the implementation of the "30E/360 method" of Excel function YEARFRAC, i.e., YEARFRAC(startDate,endDate,4).
 * 	</li>
 * </ul>
 * 
 * The day count of 30E+/360 is that of 30E/360 whenever endDateDay is &leq; 30, otherwise it is that of 30E/360 plus one.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30E_360 implements DayCountConventionInterface {

	private final boolean is30Eplus360;
	
	/**
	 * Create a 30E/360 or 30E+/360 daycount convention.
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
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public double getDaycount(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		int startDateDay 	= startDate.get(Calendar.DAY_OF_MONTH);
		int startDateMonth 	= startDate.get(Calendar.MONTH);
		int startDateYear 	= startDate.get(Calendar.YEAR);
		
		int endDateDay 		= endDate.get(Calendar.DAY_OF_MONTH);
		int endDateMonth 	= endDate.get(Calendar.MONTH);
		int endDateYear 	= endDate.get(Calendar.YEAR);

		double daycount = (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay, 30.0) - Math.min(startDateDay, 30.0));

		if(is30Eplus360 && endDateDay == 31) daycount +=1.0;
		
		return daycount;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		return getDaycount(startDate, endDate) / 360.0;
	}
}
