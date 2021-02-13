package net.finmath.equities.models;

import java.time.LocalDate;
import java.util.HashMap;

import net.finmath.equities.marketdata.AffineDividendStream;
import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.time.daycount.DayCountConvention;

/**
 * This class implements the forward structure for a stock with affine dividends
 * according to Buehler's 2007 paper.
 *
 * @author Andreas Grotz
 */

public class BuehlerDividendForwardStructure implements EquityForwardStructure {
	private final LocalDate valuationDate;
	private final double spot;
	private final FlatYieldCurve repoCurve;
	private final AffineDividendStream dividendStream;
	private final DayCountConvention dayCounter;
	private final HashMap<LocalDate,Double> dividendTimes;


	public BuehlerDividendForwardStructure(
			final LocalDate valuationDate,
			final double spot,
			final FlatYieldCurve repoCurve,
			final AffineDividendStream dividendStream,
			final DayCountConvention dayCounter)
	{
		this.valuationDate = valuationDate;
		this.spot = spot;
		this.repoCurve = repoCurve;
		this.dividendStream = dividendStream;
		this.dayCounter = dayCounter;
		dividendTimes = new HashMap<LocalDate,Double>();
		for (final var date : dividendStream.getDividendDates())
		{
			dividendTimes.put(date, dayCounter.getDaycountFraction(valuationDate, date));
		}
		validate();
	}

	public void validate()
	{
		assert getFutureDividendFactor(valuationDate) <= spot : "PV of future dividends is larger than spot.";
	}

	@Override
	public BuehlerDividendForwardStructure cloneWithNewSpot(double newSpot)
	{
		return new BuehlerDividendForwardStructure(
				this.valuationDate,
				newSpot,
				this.repoCurve,
				this.dividendStream,
				this.dayCounter);
	}

	@Override
	public BuehlerDividendForwardStructure cloneWithNewDate(LocalDate newDate)
	{
		return new BuehlerDividendForwardStructure(
				newDate,
				this.spot,
				this.repoCurve,
				this.dividendStream,
				this.dayCounter);
	}

	@Override
	public DividendModelType getDividendModel()
	{
		return DividendModelType.Buehler;
	}

	@Override
	public LocalDate getValuationDate()
	{
		return valuationDate;
	}

	@Override
	public double getSpot()
	{
		return spot;
	}

	@Override
	public FlatYieldCurve getRepoCurve()
	{
		return repoCurve;
	}

	@Override
	public AffineDividendStream getDividendStream()
	{
		return dividendStream;
	}

	@Override
	public double getGrowthDiscountFactor(double startTime, double endTime)
	{
		var df = 1.0;
		for (final var date : dividendStream.getDividendDates())
		{
			final var dividendTime = dividendTimes.get(date);
			if (dividendTime > startTime && dividendTime <= endTime) {
				df *= (1.0 - dividendStream.getProportionalDividendFactor(date));
			}
		}

		return df / repoCurve.getForwardDiscountFactor(startTime, endTime);
	}

	@Override
	public double getGrowthDiscountFactor(
			LocalDate startDate,
			LocalDate endDate)
	{
		final var startTime = dayCounter.getDaycountFraction(valuationDate, startDate);
		final var endTime = dayCounter.getDaycountFraction(valuationDate, endDate);
		return getGrowthDiscountFactor(startTime, endTime);
	}

	@Override
	public double getFutureDividendFactor(double valTime)
	{
		var df = 0.0;
		for (final var date : dividendStream.getDividendDates())
		{
			final var dividendTime = dividendTimes.get(date);
			if (dividendTime > valTime) {
				df += dividendStream.getCashDividend(date) / getGrowthDiscountFactor(valTime, dividendTime);
			}
		}
		return df;
	}

	@Override
	public double getFutureDividendFactor(LocalDate valDate)
	{
		final var valTime = dayCounter.getDaycountFraction(valuationDate, valDate);
		return getFutureDividendFactor(valTime);
	}

	@Override
	public double getForward(double expiryTime)
	{
		var forward = spot * getGrowthDiscountFactor(0.0, expiryTime);
		for (final var date : dividendStream.getDividendDates())
		{
			final var dividendTime = dividendTimes.get(date);
			if (dividendTime <= expiryTime) {
				forward -= dividendStream.getCashDividend(date) * getGrowthDiscountFactor(dividendTime, expiryTime);
			}
		}
		return forward;
	}

	@Override
	public double getForward(LocalDate expiryDate)
	{
		final var expiryTime = dayCounter.getDaycountFraction(valuationDate, expiryDate);
		return getForward(expiryTime);
	}

	@Override
	public double getDividendAdjustedStrike(
			double strike,
			double expiryTime)
	{
		return strike - getFutureDividendFactor(expiryTime);
	}

	@Override
	public double getDividendAdjustedStrike(
			double strike,
			LocalDate expiryDate)
	{
		return strike - getFutureDividendFactor(expiryDate);
	}

	@Override
	public double getLogMoneyness(
			double strike,
			double expiryTime)
	{
		return Math.log(getDividendAdjustedStrike(strike, expiryTime)
				/ getDividendAdjustedStrike(getForward(expiryTime), expiryTime));
	}

	@Override
	public double getLogMoneyness(
			double strike,
			LocalDate expiryDate)
	{
		return Math.log(getDividendAdjustedStrike(strike, expiryDate)
				/ getDividendAdjustedStrike(getForward(expiryDate), expiryDate));
	}
}
