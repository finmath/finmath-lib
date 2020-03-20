/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import net.finmath.time.daycount.DayCountConvention;

/**
 * A schedule of interest rate periods with
 * a fixing and payment.
 *
 * The periods have two representations: one a {@link net.finmath.time.Period}
 * which contains {@link java.time.LocalDate} dates and
 * an alternative representation using doubles.
 *
 * Within a schedule, the mapping from doubles to dates is one to one.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class ScheduleFromPeriods implements Schedule, Serializable {

	private static final long serialVersionUID = 4460864939790714361L;

	private final			LocalDate					referenceDate;

	private final List<Period>			periods;
	private final DayCountConvention	daycountconvention;

	private final double[] fixingTimes;
	private final double[] paymentTimes;
	private final double[] periodStartTimes;
	private final double[] periodEndTimes;
	private final double[] periodLength;

	public ScheduleFromPeriods(final LocalDate referenceDate, final DayCountConvention daycountconvention, final Period... periods) {
		this(referenceDate, Arrays.asList(periods), daycountconvention);
	}

	public ScheduleFromPeriods(final LocalDate referenceDate, final List<Period> periods, final DayCountConvention daycountconvention) {
		super();
		if(referenceDate == null) {
			throw new IllegalArgumentException("referenceDate must not be null.");
		}

		this.referenceDate = referenceDate;
		this.periods = periods;
		this.daycountconvention = daycountconvention;

		// Precalculate dates to yearfrac doubles
		fixingTimes = new double[periods.size()];
		paymentTimes = new double[periods.size()];
		periodStartTimes = new double[periods.size()];
		periodEndTimes = new double[periods.size()];
		periodLength = new double[periods.size()];
		for(int periodIndex=0; periodIndex < periods.size(); periodIndex++) {
			fixingTimes[periodIndex] = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, periods.get(periodIndex).getFixing());
			paymentTimes[periodIndex] = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, periods.get(periodIndex).getPayment());
			periodStartTimes[periodIndex] = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, periods.get(periodIndex).getPeriodStart());
			periodEndTimes[periodIndex] = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, periods.get(periodIndex).getPeriodEnd());
			periodLength[periodIndex] = daycountconvention.getDaycountFraction(periods.get(periodIndex).getPeriodStart(), periods.get(periodIndex).getPeriodEnd());
		}
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	@Override
	public List<Period> getPeriods() {
		return periods;
	}

	@Override
	public DayCountConvention getDaycountconvention() {
		return daycountconvention;
	}

	@Override
	public int getNumberOfPeriods() {
		return periods.size();
	}

	@Override
	public Period getPeriod(final int periodIndex) {
		return periods.get(periodIndex);
	}

	@Override
	public double getFixing(final int periodIndex) {
		return fixingTimes[periodIndex];
	}

	@Override
	public double getPayment(final int periodIndex) {
		return paymentTimes[periodIndex];
	}

	@Override
	public double getPeriodStart(final int periodIndex) {
		return periodStartTimes[periodIndex];
	}

	@Override
	public double getPeriodEnd(final int periodIndex) {
		return periodEndTimes[periodIndex];
	}

	@Override
	public double getPeriodLength(final int periodIndex) {
		return periodLength[periodIndex];
	}

	@Override
	public Iterator<Period> iterator() {
		return periods.iterator();
	}

	@Override
	public int getPeriodIndex(final double time) {
		if(time< getPeriodStart(0)|| time>= getPeriodEnd(getNumberOfPeriods()-1)) {
			throw new IllegalArgumentException("Time point not included");
		}
		for(int i=0; i<getNumberOfPeriods()-1;i++) {
			if(time<=getPeriodEnd(i)) {
				return i;
			}
		}
		return getNumberOfPeriods()-1;
	}

	@Override
	public int getPeriodIndex(final LocalDate date) {
		final double floatingDate=FloatingpointDate.getFloatingPointDateFromDate(getReferenceDate(),date);
		return getPeriodIndex(floatingDate);
	}

	@Override
	public String toString() {
		String periodOutputString = "Periods (fixing, periodStart, periodEnd, payment):";
		for(int periodIndex=0; periodIndex<periods.size(); periodIndex++) {
			periodOutputString += "\n" + periods.get(periodIndex).getFixing() + ", " +
					periods.get(periodIndex).getPeriodStart() + ", " +
					periods.get(periodIndex).getPeriodEnd() + ", " +
					periods.get(periodIndex).getPayment();
		}
		return "ScheduleFromPeriods [referenceDate=" + referenceDate + ", daycountconvention=" + daycountconvention + "\n" + periodOutputString + "]";
	}
}
