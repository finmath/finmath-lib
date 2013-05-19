/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata;

import net.finmath.time.TimeDiscretizationInterface;

public interface AbstractSwaptionMarketData {

	public TimeDiscretizationInterface	getOptionMaturities();
	public TimeDiscretizationInterface	getTenor();
	public double						getSwapPeriodLength();

	/**
	 * Returns the option price of a swaption for a given option maturity and tenor length.
	 * @param optionMaturity The option maturity.
	 * @param tenorLength The tenor length.
	 * @param periodLength The period length of the floating rate period.
	 * @param strike The strike (swap) rate.
	 * @return The option price.
	 */
	public double getValue(double optionMaturity, double tenorLength, double periodLength, double strike);

	/**
	 * Returns the option implied volatility of a swaption for a given option maturity and tenor length.
	 * @param optionMaturity The option maturity.
	 * @param tenorLength The tenor length.
	 * @param periodLength The period length of the floating rate period.
	 * @param strike The strike (swap) rate.
	 * @return The implied volatility.
	 */
	public double getVolatility(double optionMatruity, double tenorLength, double periodLength, double strike);
}
