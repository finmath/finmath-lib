/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 13.04.2014
 */

package net.finmath.timeseries;

import java.util.Calendar;
import java.util.Map;

/**
 * @author Christian Fries
 *
 */
public class TimeSeries {

	private Map<Calendar, MarketData>	dataForDates;

	/**
	 * Create a time series.
	 * 
	 * @param dataForDates A map from dates to MarketData objects.
	 */
	public TimeSeries(Map<Calendar, MarketData> dataForDates) {
		super();
		this.dataForDates = dataForDates;
	}

	public MarketData getEntry(Calendar date) {
		return dataForDates.get(date);
	}
}
