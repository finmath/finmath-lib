/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 13.04.2014
 */

package net.finmath.timeseries;

import java.util.Calendar;
import java.util.Map;

/**
 * A set of raw data associated with a given date.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class MarketData {

	private final Calendar			date;
	private final Map<String,Double>	valuesForSymbols;

	public MarketData(final Calendar date, final Map<String,Double> valuesForSymbols) {
		super();
		this.date				= date;
		this.valuesForSymbols	= valuesForSymbols;
	}

	public Calendar getDate() {
		return date;
	}

	public double getValue(final String symbol) {
		return valuesForSymbols.get(symbol);
	}

}
