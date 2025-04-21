package net.finmath.equities.marketdata;

import java.time.LocalDate;
import java.util.Arrays;

import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Class to provide methods of a yield curve.
 *
 * @author Andreas Grotz
 */

public class YieldCurve {

    protected final LocalDate referenceDate;
    protected final LocalDate[] discountDates;
    protected final DayCountConvention dayCounter;
    protected final DiscountCurveInterpolation baseCurve;

    public YieldCurve(final String name, final LocalDate referenceDate, final DayCountConvention dayCounter,
            final LocalDate[] discountDates, final double[] discountFactors) {
        this.dayCounter = dayCounter;
        this.discountDates = discountDates;
        double[] times = new double[discountDates.length];
        boolean[] isParameter = new boolean[discountDates.length];
        for (int i = 0; i < times.length; i++) {
            times[i] = dayCounter.getDaycountFraction(referenceDate, discountDates[i]);
        }
        baseCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(name, referenceDate, times,
                discountFactors, isParameter, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT,
                InterpolationEntity.LOG_OF_VALUE_PER_TIME);

        this.referenceDate = referenceDate;
    }

    public YieldCurve rollToDate(LocalDate date) {
        assert date.isAfter(referenceDate) : "can only roll to future dates";
        LocalDate[] rolledDiscountDates = Arrays.stream(discountDates).filter(p -> p.isAfter(date))
                .toArray(LocalDate[]::new);
        double[] rolledDiscountFactors = new double[rolledDiscountDates.length];
        for (int i = 0; i < rolledDiscountDates.length; i++) {
            rolledDiscountFactors[i] = getForwardDiscountFactor(date, rolledDiscountDates[i]);
        }

        return new YieldCurve(baseCurve.getName(), date, dayCounter, rolledDiscountDates, rolledDiscountFactors);
    }

    public double getRate(double maturity) {
        assert maturity >= 0.0 : "maturity must be positive";
        return baseCurve.getZeroRate(maturity);
    }

    public double getRate(LocalDate date) {
        return baseCurve.getZeroRate(dayCounter.getDaycountFraction(referenceDate, date));
    }

    public double getDiscountFactor(double maturity) {
        assert maturity >= 0.0 : "maturity must be positive";
        return baseCurve.getDiscountFactor(maturity);
    }

    public double getForwardDiscountFactor(double start, double expiry) {
        assert start >= 0.0 : "start must be positive";
        assert expiry >= start : "start must be before expiry";
        return getDiscountFactor(expiry) / getDiscountFactor(start);
    }

    public double getDiscountFactor(LocalDate date) {
        return baseCurve.getDiscountFactor(dayCounter.getDaycountFraction(referenceDate, date));
    }

    public double getForwardDiscountFactor(LocalDate startDate, LocalDate endDate) {
        assert !startDate.isBefore(referenceDate) : "start date must be after curve date";
        assert !endDate.isBefore(startDate) : "end date must be after start date";
        return getDiscountFactor(endDate) / getDiscountFactor(startDate);
    }
}
