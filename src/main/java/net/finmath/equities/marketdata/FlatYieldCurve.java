package net.finmath.equities.marketdata;

import java.time.LocalDate;

import net.finmath.time.daycount.DayCountConvention;

/**
 * Class to provide methods of a flat yield curve.
 * TODO This class should be integrated into or replaced by finmat-lib's curve universe.
 *
 * @author Andreas Grotz
 */

public class FlatYieldCurve {
	private final LocalDate curveDate;
	private final double rate;
	private final DayCountConvention dayCounter;

	public FlatYieldCurve(
			final LocalDate curveDate,
			final double rate,
			final DayCountConvention dayCounter)
	{
		this.curveDate = curveDate;
		this.rate = rate;
		this.dayCounter = dayCounter;
	}

	public double getRate(double maturity)
	{
		assert maturity >= 0.0 : "maturity must be positive";
		return rate;
	}

	public double getRate(LocalDate date)
	{
		return getRate(dayCounter.getDaycountFraction(curveDate, date));
	}

	public double getDiscountFactor(double maturity)
	{
		assert maturity >= 0.0 : "maturity must be positive";
		return Math.exp(-maturity * rate);
	}

	public double getForwardDiscountFactor(double start, double expiry)
	{
		assert start >= 0.0 : "start must be positive";
		assert expiry >= start : "start must be before expiry";
		return getDiscountFactor(expiry) / getDiscountFactor(start);
	}

	public double getDiscountFactor(LocalDate date)
	{
		return getDiscountFactor(dayCounter.getDaycountFraction(curveDate, date));
	}

	public double getForwardDiscountFactor(LocalDate startDate, LocalDate endDate)
	{
		assert !startDate.isBefore(curveDate) : "start date must be after curve date";
		assert !endDate.isBefore(startDate) : "end date must be after start date";
		return getDiscountFactor(endDate) / getDiscountFactor(startDate);
	}
}
