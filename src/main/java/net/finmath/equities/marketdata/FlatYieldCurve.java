package net.finmath.equities.marketdata;

import java.time.LocalDate;

import net.finmath.time.daycount.DayCountConvention;

/**
 * Class to provide methods of a flat yield curve.
 *
 * @author Andreas Grotz
 */
public class FlatYieldCurve extends YieldCurve {

	private static final int LONG_TIME = 100;

	public FlatYieldCurve(
			final LocalDate curveDate,
			final double rate,
			final DayCountConvention dayCounter) {

		super(
				"NONE",
				curveDate,
				dayCounter,
				new LocalDate[] { curveDate.plusYears(LONG_TIME) },
				new double[] {
						Math.exp(
								-rate * dayCounter.getDaycountFraction(
										curveDate,
										curveDate.plusYears(LONG_TIME)))
				});
	}

	public FlatYieldCurve rollToDate(LocalDate date) {
		assert date.isAfter(baseCurve.getReferenceDate()) : "can only roll to future dates";
		return new FlatYieldCurve(
				date,
				getRate(baseCurve.getReferenceDate().plusYears(LONG_TIME)),
				dayCounter);
	}
}
