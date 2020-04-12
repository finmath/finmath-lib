/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.volatilities;

import net.finmath.time.TimeDiscretization;

/**
 * Basic interface to be implemented by classes
 * providing swaption market data.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface SwaptionMarketData {

	TimeDiscretization	getOptionMaturities();
	TimeDiscretization	getTenor();
	double						getSwapPeriodLength();

	/**
	 * Returns the option price of a swaption for a given option maturity and tenor length.
	 * @param optionMaturity The option maturity.
	 * @param tenorLength The tenor length.
	 * @param periodLength The period length of the floating rate period.
	 * @param strike The strike (swap) rate.
	 * @return The option price.
	 */
	double getValue(double optionMaturity, double tenorLength, double periodLength, double strike);

	/**
	 * Returns the option implied volatility of a swaption for a given option maturity and tenor length.
	 *
	 * @param optionMaturity The option maturity.
	 * @param tenorLength The tenor length.
	 * @param periodLength The period length of the floating rate period.
	 * @param strike The strike (swap) rate.
	 * @return The implied volatility.
	 */
	double getVolatility(double optionMaturity, double tenorLength, double periodLength, double strike);
}
