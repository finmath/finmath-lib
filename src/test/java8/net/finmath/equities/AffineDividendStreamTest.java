package net.finmath.equities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import net.finmath.equities.models.EquityForwardStructure;
import net.finmath.equities.marketdata.YieldCurve;
import net.finmath.equities.marketdata.AffineDividend;
import net.finmath.equities.marketdata.AffineDividendStream;
import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.equities.models.BuehlerDividendForwardStructure;
import net.finmath.exception.CalculationException;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConventionFactory;


/**
 * Tests for the classes handling Buehle dividends.
 *
 * @author Andreas Grotz
 */

public class AffineDividendStreamTest {
	/*
	 */
	private static final DecimalFormat decform = new DecimalFormat("#0.00");
	private static final DayCountConvention dcc = DayCountConventionFactory.getDayCountConvention("act/365") ;

	@Test
	public void Test_sorting() throws CalculationException
	{
		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0),
						new AffineDividend(LocalDate.parse("2019-09-17"), 20.0, 0.0),
						new AffineDividend(LocalDate.parse("2020-09-17"), 30.0, 0.0)});
		final List<LocalDate> dates = dividends.getDividendDates();
		assertTrue(dates.get(0).isBefore(dates.get(1)) && dates.get(1).isBefore(dates.get(2)));
	}

	@Test
	public void Test_affineDividendConversion() throws CalculationException
	{
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double rate = 0.01;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);
		final AffineDividendStream cashDividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 10.0, 0.0),
						new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
						new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0)});

		final Map<LocalDate, Double> conversionRates = new HashMap<LocalDate, Double>() {
			private static final long serialVersionUID = 8454377222706940302L;
			{
				put(LocalDate.parse("2019-09-17"), 0.0);
				put(LocalDate.parse("2020-09-17"), 0.5);
				put(LocalDate.parse("2021-09-17"), 1.0);
			}};

			final AffineDividendStream affineDividends = AffineDividendStream.getAffineDividendsFromCashDividends(
					cashDividends, conversionRates, valDate, spot, curve);

			final EquityForwardStructure fsCash = new BuehlerDividendForwardStructure(valDate, spot, curve, cashDividends, dcc);
			final EquityForwardStructure fsAffine = new BuehlerDividendForwardStructure(valDate, spot, curve, affineDividends, dcc);

			final double tol = 1e-13;
			assertEquals(fsCash.getForward(LocalDate.parse("2019-09-16")),
					fsAffine.getForward(LocalDate.parse("2019-09-16")),
					tol);
			assertEquals(fsCash.getForward(LocalDate.parse("2019-09-17")),
					fsAffine.getForward(LocalDate.parse("2019-09-17")),
					tol);
			assertEquals(fsCash.getForward(LocalDate.parse("2020-09-16")),
					fsAffine.getForward(LocalDate.parse("2020-09-16")),
					tol);
			assertEquals(fsCash.getForward(LocalDate.parse("2020-09-17")),
					fsAffine.getForward(LocalDate.parse("2020-09-17")),
					tol);
			assertEquals(fsCash.getForward(LocalDate.parse("2021-09-16")),
					fsAffine.getForward(LocalDate.parse("2021-09-16")),
					tol);
			assertEquals(fsCash.getForward(LocalDate.parse("2021-09-17")),
					fsAffine.getForward(LocalDate.parse("2021-09-17")),
					tol);
	}
}
