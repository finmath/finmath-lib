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
 */
public class MarketData {

	private Calendar			date;
	private Map<String,Double>	valuesForSymbols;

	public MarketData(Calendar date, Map<String,Double> valuesForSymbols) {
		super();
		this.date				= date;
		this.valuesForSymbols	= valuesForSymbols;
	}

	public Calendar getDate() {
		return date;
	}

	public double getValue(String symbol) {
		return valuesForSymbols.get(symbol);
	}

}
