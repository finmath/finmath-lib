/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.06.2014
 */

package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;

/**
 * Interface for classes representing a volatility surface,
 * i.e. European option prices.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface VolatilitySurface {

	/**
	 * Quoting conventions.
	 * It may be that the implementing class does not support all quoting conventions.
	 *
	 * @author Christian Fries
	 */
	enum QuotingConvention {
		VOLATILITYLOGNORMAL,
		VOLATILITYNORMAL,
		PRICE
	}

	/**
	 * Returns the name of the volatility surface.
	 *
	 * @return The name of the volatility surface.
	 */
	String getName();

	/**
	 * Return the reference date of this surface, i.e. the date
	 * associated with t=0.
	 *
	 * @return The date identified as t=0.
	 */
	LocalDate getReferenceDate();

	/**
	 * Returns the price or implied volatility for the corresponding maturity and strike.
	 *
	 * @param maturity The option maturity for which the price or implied volatility is requested.
	 * @param strike The option strike for which the price or implied volatility is requested.
	 * @param quotingConvention The quoting convention to be used for the return value.
	 * @return The price or implied volatility depending on the quoting convention.
	 */
	double getValue(double maturity, double strike, QuotingConvention quotingConvention);

	/**
	 * Returns the price or implied volatility for the corresponding maturity and strike.
	 *
	 * @param model An analytic model providing a context. Some curves do not need this (may be null).
	 * @param maturity The option maturity for which the price or implied volatility is requested.
	 * @param strike The option strike for which the price or implied volatility is requested.
	 * @param quotingConvention The quoting convention to be used for the return value.
	 * @return The price or implied volatility depending on the quoting convention.
	 */
	double getValue(AnalyticModel model, double maturity, double strike, QuotingConvention quotingConvention);

	/**
	 * Return the default quoting convention of this surface.
	 *
	 * @return the quotingConvention
	 */
	QuotingConvention getQuotingConvention();
}
