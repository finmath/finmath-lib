package net.finmath.equities.marketdata;

import java.time.LocalDate;

/**
 * Class to store and handle an affine dividend, i.e. a dividend consisting of a cash part (fixed amount)
 * and a proportional part (fixed proportion of the stock price prevailing at the dividend date.
 * The convention follows Buehler's 2007 paper, i.e. the dividend amount paid such that the stock price
 * after dividend payment at t is given by
 *   S(t) = S(t-eps) * (1 - propDividendFactor) - cashDividend
 *
 * @author Andreas Grotz
 */

public class AffineDividend {

	private final LocalDate date;
	private final double cashDividend;
	private final double propDividendFactor;

	public AffineDividend(
			final LocalDate date,
			final double cashDividend,
			final double propDividendFactor)
	{
		this.date = date;
		this.cashDividend = cashDividend;
		this.propDividendFactor = propDividendFactor;
	}

	public LocalDate getDate()
	{
		return date;
	}

	public double getProportionalDividendFactor()
	{
		return propDividendFactor;
	}

	public double getCashDividend()
	{
		return cashDividend;
	}

	public double getDividend(final double stockPrice)
	{
		return propDividendFactor * stockPrice + cashDividend;
	}
}
