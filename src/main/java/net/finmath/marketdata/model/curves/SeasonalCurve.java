/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.03.2015
 */

package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * The curve returns a value depending on the month of the time argument, that is,
 * a call <code>getValue(model, time)</code> will map time to a 30/360 value using
 * the day and month only and delegate the call to a given base curve.
 *
 * The value returned then is <code>baseCurve.getValue(model, season)</code>
 * where
 * <code>season = (month-1) / 12.0 + (day-1) / (double)numberOfDays / 12.0;</code>
 *
 * The base curve has to be constructed according to this time convention (e.g.,
 * as a piecewise constant curve with values at i / 12 for i=1,...,12 using
 * {@link Curve.InterpolationMethod} with <code>PIECEWISE_CONSTANT_RIGHTPOINT</code>.
 *
 * @author Christian Fries
 */
public class SeasonalCurve extends AbstractCurve implements CurveInterface {

	private static final long serialVersionUID = 4021745191829488593L;

	private CurveInterface baseCurve;

	/**
	 * A builder (following the builder pattern) for SeasonalCurve objects.
	 * Allows to successively construct a curve object by adding points to its base points.
	 *
	 * @author Christian Fries
	 */
	public static class CurveBuilder extends Curve.CurveBuilder implements CurveBuilderInterface {

		private SeasonalCurve			curve = null;

		/**
		 * Create a CurveBuilder from a given seasonalCurve.
		 *
		 * @param seasonalCurve The seasonal curve from which to copy the fixed part upon build().
		 * @throws CloneNotSupportedException Thrown, when the base curve could not be cloned.
		 */
		public CurveBuilder(SeasonalCurve seasonalCurve) throws CloneNotSupportedException {
			super((Curve)(seasonalCurve.baseCurve));
			this.curve = seasonalCurve;
		}

		@Override
		public CurveInterface build() throws CloneNotSupportedException {
			SeasonalCurve buildCurve = curve.clone();
			buildCurve.baseCurve = super.build();
			curve = null;
			return buildCurve;
		}
	}

	/**
	 * Create a monthly seasonality adjustment curve by estimating historic log-returns from monthly index fixings.
	 *
	 * @param name The name of this curve.
	 * @param referenceDate The reference date for this curve (i.e. t=0).
	 * @param indexFixings A <code>Map&lt;Date, Double&gt;</code> of consecutive monthly index fixings.
	 * @param numberOfYearsToAverage The number of years over which monthly log returns should be averaged.
	 */
	public SeasonalCurve(String name, LocalDate referenceDate, Map<LocalDate, Double> indexFixings, int numberOfYearsToAverage) {
		super(name, referenceDate);

		double[] seasonalAdjustmentCalculated = SeasonalCurve.computeSeasonalAdjustments(referenceDate, indexFixings, numberOfYearsToAverage);

		double[] seasonTimes = new double[12];
		double[] seasonValue = new double[12];
		double seasonValueCummulated = 1.0;
		for(int j=0; j<12; j++) {
			seasonValueCummulated *= Math.exp(seasonalAdjustmentCalculated[j]/12.0);
			seasonTimes[j] = j/12.0;
			seasonValue[j] = seasonValueCummulated;
		}
		this.baseCurve = new Curve(name + "-seasonal-base", referenceDate, InterpolationMethod.PIECEWISE_CONSTANT_LEFTPOINT, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE, seasonTimes, seasonValue);
	}

	/**
	 * @param name The name of this curve.
	 * @param referenceDate The reference date for this curve (i.e. t=0).
	 * @param baseCurve The base curve, i.e., the discount curve used to calculate the seasonal adjustment factors.
	 */
	public SeasonalCurve(String name, LocalDate referenceDate, CurveInterface baseCurve) {
		super(name, referenceDate);
		this.baseCurve = baseCurve;
	}

	@Override
	public double[] getParameter() {
		return baseCurve.getParameter();
	}

	@Override
	public void setParameter(double[] parameter) {
		baseCurve.setParameter(parameter);
	}

	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		LocalDate calendar = getReferenceDate().plusDays((int) Math.round(time*365));

		int month = calendar.getMonthValue();				// Note: month = 1,2,3,...,12
		int day   = calendar.getDayOfMonth(); 				// Note: day = 1,2,3,...,numberOfDays
		int numberOfDays = calendar.lengthOfMonth();
		double season = (month-1) / 12.0 + (day-1) / (double)numberOfDays / 12.0;

		return baseCurve.getValue(model, season);
	}

	@Override
	public CurveInterface getCloneForParameter(double[] value) throws CloneNotSupportedException {
		SeasonalCurve newCurve = clone();
		newCurve.baseCurve = baseCurve.getCloneForParameter(value);

		return newCurve;
	}

	@Override
	public SeasonalCurve clone() throws CloneNotSupportedException {
		return new SeasonalCurve(this.getName(), this.getReferenceDate(), (CurveInterface) baseCurve.clone());
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		return new CurveBuilder(this);
	}

	public static double[] computeSeasonalAdjustments(LocalDate referenceDate, Map<LocalDate, Double> indexFixings, int numberOfYearsToAverage) {
		DayCountConventionInterface modelDcc = new DayCountConvention_ACT_365();			// Not needed: remove

		double[] fixingTimes = new double[indexFixings.size()];								// Not needed: remove
		double[] realizedCPIValues = new double[indexFixings.size()];
		int i = 0;
		List<LocalDate> fixingDates = new ArrayList<>(indexFixings.keySet());
		Collections.sort(fixingDates);
		for(LocalDate fixingDate : fixingDates) {
			fixingTimes[i] = modelDcc.getDaycountFraction(referenceDate, fixingDate);
			realizedCPIValues[i] = indexFixings.get(fixingDate).doubleValue();
			i++;
		}

		LocalDate lastMonth = fixingDates.get(fixingDates.size()-1);

		return computeSeasonalAdjustments(realizedCPIValues, lastMonth.getMonthValue(), numberOfYearsToAverage);
	}

	/**
	 * Computes annualized seasonal adjustments from given monthly realized CPI values.
	 *
	 * @param realizedCPIValues An array of consecutive monthly CPI values (minimum size is 12*numberOfYearsToAverage))
	 * @param lastMonth The index of the last month in the sequence of realizedCPIValues (corresponding to the enums in <code>{@link java.time.Month}</code>).
	 * @param numberOfYearsToAverage The number of years to go back in the array of realizedCPIValues.
	 * @return Array of annualized seasonal adjustments, where [0] corresponds to the adjustment for from December to January.
	 */
	public static double[] computeSeasonalAdjustments(double[] realizedCPIValues, int lastMonth, int numberOfYearsToAverage) {

		/*
		 * Cacluate average log returns
		 */
		double[] averageLogReturn = new double[12];
		Arrays.fill(averageLogReturn, 0.0);
		for(int arrayIndex = 0; arrayIndex < 12*numberOfYearsToAverage; arrayIndex++){

			int month = (((((lastMonth-1 - arrayIndex) % 12) + 12) % 12));

			double logReturn = Math.log(realizedCPIValues[realizedCPIValues.length - 1 - arrayIndex] / realizedCPIValues[realizedCPIValues.length - 2 - arrayIndex]);
			averageLogReturn[month] += logReturn/numberOfYearsToAverage;
		}

		/*
		 * Normalize
		 */
		double sum = 0.0;
		for(int index = 0; index < averageLogReturn.length; index++){
			sum += averageLogReturn[index];
		}
		double averageSeasonal = sum / averageLogReturn.length;

		double[] seasonalAdjustments = new double[averageLogReturn.length];
		for(int index = 0; index < seasonalAdjustments.length; index++){
			seasonalAdjustments[index] = averageLogReturn[index] - averageSeasonal;
		}

		// Annualize seasonal adjustments
		for(int index = 0; index < seasonalAdjustments.length; index++){
			seasonalAdjustments[index] = seasonalAdjustments[index] * 12;
		}

		return seasonalAdjustments;
	}
}
