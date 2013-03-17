/*
 * Created on 20.05.2005
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.marketdata;

import net.finmath.time.TimeDiscretizationInterface;

public interface AbstractSwaptionMarketData {

	public TimeDiscretizationInterface	getOptionMaturities();
	public TimeDiscretizationInterface	getTenor();
	public double getSwapPeriodLength();

	public double getValue(double optionMatruity, double tenorLength, double periodLength, double strike);
	public double getVolatility(double optionMatruity, double tenorLength, double periodLength, double strike);
}
