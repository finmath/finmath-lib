/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 14.06.2015
 */

package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.daycount.DayCountConvention_30E_360;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * A collection of convenient methods constructing some more specialized curves.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class CurveFactory {

	private static DayCountConvention_ACT_365 modelDcc = new DayCountConvention_ACT_365();

	private CurveFactory() {
	}

	/**
	 * Creates a monthly index curve with seasonality and past fixings.
	 *
	 * This methods creates an index curve (e.g. for a CPI index) using provided <code>annualizedZeroRates</code>
	 * for the forwards (expected future CPI values) and <code>indexFixings</code> for the past
	 * fixings.
	 *
	 * It may also "overlay" the future values with a seasonality adjustment. The seasonality adjustment
	 * is either taken from adjustment factors provided in <code>seasonalityAdjustments</code> or
	 * (if that argument is null) estimated from the <code>indexFixings</code>. The the latter case
	 * use <code>seasonalAveragingNumerOfYears</code> to specify the number of years which should be used
	 * to estimate the seasonality adjustments.
	 *
	 * @param name The name of the curve.
	 * @param referenceDate The reference date of the curve.
	 * @param indexFixings A Map&lt;LocalDate, Double&gt; of past fixings.
	 * @param seasonalityAdjustments A Map&lt;String, Double&gt; of seasonality adjustments (annualized continuously compounded rates for the given month, i.e., the seasonality factor is exp(seasonalityAdjustment/12)), where the String keys are "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december".
	 * @param seasonalAveragingNumberOfYears If seasonalityAdjustments is null you may provide an integer representing a number of years to have the seasonality estimated from the past fixings in <code>indexFixings</code>.
	 * @param annualizedZeroRates Map&lt;LocalDate, Double&gt; of annualized zero rates for given maturities.
	 * @param forwardsFixingLag The fixing lag (e.g. "-3M" for -3 month)
	 * @param forwardsFixingType The fixing type (e.g. "endOfMonth")
	 * @return An index curve.
	 */
	public static Curve createIndexCurveWithSeasonality(final String name, final LocalDate referenceDate, final Map<LocalDate, Double> indexFixings, final Map<String, Double> seasonalityAdjustments, final Integer seasonalAveragingNumberOfYears, final Map<LocalDate, Double> annualizedZeroRates, final String forwardsFixingLag, final String forwardsFixingType) {

		/*
		 * Create a curve containing past fixings (using picewise constant interpolation)
		 */
		final double[] fixingTimes = new double[indexFixings.size()];
		final double[] fixingValue = new double[indexFixings.size()];
		int i = 0;
		final List<LocalDate> fixingDates = new ArrayList<>(indexFixings.keySet());
		Collections.sort(fixingDates);
		for(final LocalDate fixingDate : fixingDates) {
			fixingTimes[i] = modelDcc.getDaycountFraction(referenceDate, fixingDate);
			fixingValue[i] = indexFixings.get(fixingDate).doubleValue();
			i++;
		}
		final Curve curveOfFixings = new CurveInterpolation(name, referenceDate, InterpolationMethod.PIECEWISE_CONSTANT_RIGHTPOINT, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE, fixingTimes, fixingValue);

		/*
		 * Create a curve modeling the seasonality
		 */
		Curve seasonCurve = null;
		if(seasonalityAdjustments != null && seasonalityAdjustments.size() > 0 && seasonalAveragingNumberOfYears == null) {
			final String[] monthList = { "january", "february", "march", "april", "may", "june", "july", "august", "september", "october", "november", "december" };
			final double[] seasonTimes = new double[12];
			final double[] seasonValue = new double[12];
			double seasonValueCummulated = 1.0;
			for(int j=0; j<12; j++) {
				seasonValueCummulated *= Math.exp(seasonalityAdjustments.get(monthList[j]));
				seasonTimes[j] = j/12.0;
				seasonValue[j] = seasonValueCummulated;
			}
			seasonCurve = new SeasonalCurve(name + "-seasonal", referenceDate,new CurveInterpolation(name + "-seasonal-base", referenceDate, InterpolationMethod.PIECEWISE_CONSTANT_LEFTPOINT, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE, seasonTimes, seasonValue));
		}
		else if(seasonalAveragingNumberOfYears != null && seasonalityAdjustments == null) {
			seasonCurve = new SeasonalCurve(name + "-seasonal", referenceDate, indexFixings, seasonalAveragingNumberOfYears);
		}
		else if(seasonalAveragingNumberOfYears != null && seasonalityAdjustments != null) {
			throw new IllegalArgumentException("Specified seasonal factors and seasonal averaging at the same time.");
		}

		/*
		 * Create the index curve from annualized zero rates.
		 */
		final double[] times = new double[annualizedZeroRates.size()];
		final double[] givenDiscountFactors = new double[annualizedZeroRates.size()];


		int index = 0;
		final List<LocalDate> dates = new ArrayList<>(annualizedZeroRates.keySet());
		Collections.sort(dates);
		for(final LocalDate forwardDate : dates) {
			LocalDate cpiDate = forwardDate;
			if(forwardsFixingType != null && forwardsFixingLag != null) {
				if(forwardsFixingType.equals("endOfMonth")) {
					cpiDate = cpiDate.withDayOfMonth(1);
					if(forwardsFixingLag.equals("-2M")) {
						cpiDate = cpiDate.minusMonths(2);
					} else if(forwardsFixingLag.equals("-3M")) {
						cpiDate = cpiDate.minusMonths(3);
					} else if(forwardsFixingLag.equals("-4M")) {
						cpiDate = cpiDate.minusMonths(4);
					} else {
						throw new IllegalArgumentException("Unsupported fixing type for forward in curve " + name);
					}
					cpiDate = cpiDate.withDayOfMonth(cpiDate.lengthOfMonth());
				}
				else {
					throw new IllegalArgumentException("Unsupported fixing type for forward in curve " + name);
				}
			}
			times[index] = modelDcc.getDaycountFraction(referenceDate, cpiDate);
			final double rate = annualizedZeroRates.get(forwardDate).doubleValue();
			givenDiscountFactors[index] = 1.0/Math.pow(1 + rate, (new DayCountConvention_30E_360()).getDaycountFraction(referenceDate, forwardDate));
			index++;
		}
		final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(name, referenceDate, times, givenDiscountFactors, null, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE);

		LocalDate baseDate = referenceDate;
		if(forwardsFixingType != null && forwardsFixingType.equals("endOfMonth") && forwardsFixingLag != null) {
			baseDate = baseDate.withDayOfMonth(1);
			if(forwardsFixingLag.equals("-2M")) {
				baseDate = baseDate.minusMonths(2);
			} else if(forwardsFixingLag.equals("-3M")) {
				baseDate = baseDate.minusMonths(3);
			} else if(forwardsFixingLag.equals("-4M")) {
				baseDate = baseDate.minusMonths(4);
			} else {
				throw new IllegalArgumentException("Unsupported fixing type for forward in curve " + name);
			}
			baseDate = baseDate.withDayOfMonth(baseDate.lengthOfMonth());
		}

		/*
		 * Index base value
		 */
		final Double baseValue	= indexFixings.get(baseDate);
		if(baseValue == null) {
			throw new IllegalArgumentException("CurveFromInterpolationPoints " + name + " has missing index value for base date " + baseDate);
		}
		final double baseTime		= FloatingpointDate.getFloatingPointDateFromDate(referenceDate, baseDate);

		/*
		 * Combine all three curves.
		 */
		double currentProjectedIndexValue = baseValue;
		if(seasonCurve != null) {
			// Rescale initial value of with seasonality
			currentProjectedIndexValue /= seasonCurve.getValue(baseTime);

			final Curve indexCurve = new IndexCurveFromDiscountCurve(name, currentProjectedIndexValue, discountCurve);
			final Curve indexCurveWithSeason = new CurveFromProductOfCurves(name, referenceDate, indexCurve, seasonCurve);
			final PiecewiseCurve indexCurveWithFixing = new PiecewiseCurve(indexCurveWithSeason, curveOfFixings, -Double.MAX_VALUE, fixingTimes[fixingTimes.length-1] + 1.0/365.0);
			return indexCurveWithFixing;
		}
		else {
			final Curve indexCurve = new IndexCurveFromDiscountCurve(name, currentProjectedIndexValue, discountCurve);
			final PiecewiseCurve indexCurveWithFixing = new PiecewiseCurve(indexCurve, curveOfFixings, -Double.MAX_VALUE, fixingTimes[fixingTimes.length-1]);
			return indexCurveWithFixing;
		}
	}
}
