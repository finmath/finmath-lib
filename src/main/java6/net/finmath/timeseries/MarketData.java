/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 13.04.2014
 */

package net.finmath.timeseries;

import java.util.Map;

import org.joda.time.LocalDate;

/**
 * A set of raw data associated with a given date.
 * 
 * @author Christian Fries
 */
public class MarketData {

	private LocalDate			date;
	private Map<String,Double>	valuesForSymbols;

	public MarketData(LocalDate date, Map<String,Double> valuesForSymbols) {
		super();
		this.date				= date;
		this.valuesForSymbols	= valuesForSymbols;
	}

	public LocalDate getDate() {
		return date;
	}

	public double getValue(String symbol) {
		return valuesForSymbols.get(symbol);
	}
	
}
