package net.finmath.equities.marketdata;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;


/**
 * Class to store and handle a stream of affine dividends
 *
 * @author Andreas Grotz
 */

public class AffineDividendStream {
	private final AffineDividend[] dividendStream;

	public AffineDividendStream(
			final AffineDividend[] dividendStream)
	{
		final var diviList = Arrays.asList(dividendStream);
		diviList.sort(Comparator.comparing(pt -> pt.getDate()));
		this.dividendStream = diviList.toArray(new AffineDividend[0]);
	}

	public ArrayList<LocalDate> getDividendDates()
	{
		final var dates = new ArrayList<LocalDate>();
		for (final AffineDividend divi : dividendStream) {
			dates.add(divi.getDate());
		}
		return dates;
	}

	public double getDividend(
			final LocalDate date,
			final double stockPrice)
	{
		for (final AffineDividend divi : dividendStream)
		{
			if (divi.getDate() == date) {
				return divi.getDividend(stockPrice);
			}
		}
		return 0.0;
	}

	public double getProportionalDividendFactor(
			final LocalDate date)
	{
		for (final AffineDividend divi : dividendStream)
		{
			if (divi.getDate() == date) {
				return divi.getProportionalDividendFactor();
			}
		}
		return 1.0;
	}

	public double getCashDividend(
			final LocalDate date)
	{
		for (final AffineDividend divi : dividendStream)
		{
			if (divi.getDate() == date) {
				return divi.getCashDividend();
			}
		}
		return 0.0;
	}

	public static AffineDividendStream getAffineDividendsFromCashDividends(
			AffineDividendStream cashDividends,
			HashMap<LocalDate, Double> transformationFactors,
			LocalDate valDate,
			double spot,
			FlatYieldCurve repoCurve)
	{
		// This method takes a stream of cash dividends and converts them to affine dividends,
		// by transforming a part of each cash dividend to a proportional dividend.
		// The percentage of each cash dividend to be transformed to a proportional dividend
		// is specified in the member propDividendFactor of the dividend.
		// The transformation is done in an arbitrage-free way, i.e. the forward structure is preserved.
		// This method is usefull in practice, where traders use dividend futures as input, and transform
		// a part to a proportional dividend (the further away the dividend, the higher the proportional part
		// and the lower the cash part.

		final var dates = cashDividends.getDividendDates();

		final var affineDividends = new ArrayList<AffineDividend>();

		for (final var date : dates)
		{
			if (date.isBefore(valDate)) {
				continue;
			}
			assert cashDividends.getProportionalDividendFactor(date) == 0.0 :
				"Proportional dividend different from zero for date " + date;
			final var cashDividend = cashDividends.getCashDividend(date);
			var fwd = spot;
			for (final var otherDate : dates)
			{
				if (otherDate.isBefore(date) && !otherDate.isBefore(valDate)) {
					fwd -= cashDividends.getCashDividend(otherDate)
							* repoCurve.getForwardDiscountFactor(valDate, otherDate);
				}
			}
			final var q = transformationFactors.get(date) * cashDividend
					* repoCurve.getForwardDiscountFactor(valDate, date)
					/ fwd;
			affineDividends.add(
					new AffineDividend(
							date,
							(1.0 - transformationFactors.get(date)) * cashDividend,
							q));
		}

		return new AffineDividendStream(affineDividends.toArray(new AffineDividend[0]));
	}
}
