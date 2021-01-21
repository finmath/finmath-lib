package net.finmath.singleswaprate.data;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.BivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.AkimaSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.BivariateGridInterpolator;
import org.apache.commons.math3.analysis.interpolation.PiecewiseBicubicSplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.time.SchedulePrototype;


/**
 * Extends {@link net.finmath.singleswaprate.data.DataTableBasic} with the capacity to interpolate values between tenor grid nodes. Note that the interpolation is done to the accuracy of the table convention.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class DataTableInterpolated extends DataTableBasic implements DataTable, Cloneable {

	private static final long serialVersionUID = -6852590286897952990L;

	/**
	 * Create an interpolated table from a basic table.
	 *
	 * @param baseTable The table to receive interpolation.
	 * @return The table with interpolation.
	 */
	public static DataTableInterpolated interpolateDataTable(final DataTableBasic baseTable) {

		final int[] maturities = new int[baseTable.size()];
		final int[] terminations = new int[baseTable.size()];
		final double[] values = new double[baseTable.size()];

		int i = 0;
		for(final int maturity : baseTable.getMaturities()) {
			for(final int termination : baseTable.getTerminationsForMaturity(maturity)) {
				maturities[i] = maturity;
				terminations[i] = termination;
				values[i++] = baseTable.getValue(maturity, termination);
			}
		}

		return new DataTableInterpolated(baseTable.getName(), baseTable.getConvention(), baseTable.getReferenceDate(), baseTable.getScheduleMetaData(),
				maturities, terminations, values);
	}

	// sliceInterpolator is used when either only one row or column is given. AkimaSplineInterpolator corresponds to the underlying interpolator of PiecewiseBicubicSplineInterpolator in Version 3.6 of commons.math3
	private static final BivariateGridInterpolator interpolator = new PiecewiseBicubicSplineInterpolator();
	private static final UnivariateInterpolator sliceInterpolator = new AkimaSplineInterpolator();

	/**
	 * Create an empty table.
	 *
	 * @param name The name of the table.
	 * @param convention The convention of the table.
	 * @param referenceDate The referenceDate of the table.
	 * @param scheduleMetaData The schedule meta data of the table.
	 */
	public DataTableInterpolated(final String name, final TableConvention convention, final LocalDate referenceDate,
			final SchedulePrototype scheduleMetaData) {
		super(name, convention, referenceDate, scheduleMetaData);
	}

	/**
	 * Create a table.
	 *
	 * @param name The name of the table.
	 * @param convention The convention of the table.
	 * @param referenceDate The referenceDate of the table.
	 * @param scheduleMetaData The schedule meta data of the table.
	 * @param maturities The maturities of the points as offset with respect to the reference date.
	 * @param terminations The terminations of the points as offset with respect to the maturity date.
	 * @param values The values at the points.
	 */
	public DataTableInterpolated(final String name, final TableConvention convention, final LocalDate referenceDate,
			final SchedulePrototype scheduleMetaData, final int[] maturities, final int[] terminations, final double[] values) {
		super(name, convention, referenceDate, scheduleMetaData, maturities, terminations, values);
	}

	/**
	 * Create a table.
	 *
	 * @param name The name of the table.
	 * @param convention The convention of the table.
	 * @param referenceDate The referenceDate of the table.
	 * @param scheduleMetaData The schedule meta data of the table.
	 * @param maturities The maturities of the points as offset with respect to the reference date.
	 * @param terminations The terminations of the points as offset with respect to the maturity date.
	 * @param values The values at the points.
	 */
	public DataTableInterpolated(final String name, final TableConvention convention, final LocalDate referenceDate,
			final SchedulePrototype scheduleMetaData, final List<Integer> maturities, final List<Integer> terminations, final List<Double> values) {
		super(name, convention, referenceDate, scheduleMetaData, maturities, terminations, values);
	}

	@Override
	public double getValue(final int maturity, final int termination) {
		if(containsEntryFor(maturity, termination)) {
			return super.getValue(maturity, termination);
		}

		// check if either of the table dimensions is one and fits the input, otherwise default to bivariate interpolation.
		if(getMaturities().contains(maturity)) {

			final int[] terminations = getTerminationsForMaturity(maturity).stream().mapToInt(Integer::intValue).toArray();
			final double[] values = new double[terminations.length];

			for(int i = 0; i < values.length; i++) {
				values[i] = super.getValue(maturity, terminations[i]);
			}

			final UnivariateFunction curve = sliceInterpolator.interpolate(Arrays.stream(terminations).asDoubleStream().toArray(), values);
			return curve.value(termination);

		} else if(getTerminations().contains(termination)){

			final int[] maturities = getMaturitiesForTermination(termination).stream().mapToInt(Integer::intValue).toArray();
			final double[] values = new double[maturities.length];

			for(int i = 0; i< maturities.length; i++) {
				values[i] = super.getValue(maturities[i], termination);
			}

			final UnivariateFunction curve = sliceInterpolator.interpolate(Arrays.stream(maturities).asDoubleStream().toArray(), values);
			return curve.value(maturity);
		} else {

			if(size() != getMaturities().size() * getTerminations().size()) {
				throw new RuntimeException("For interpolation " +getName()+ " requires a regular grid of values.");
			}

			final int[] maturities = getMaturities().stream().mapToInt(Integer::intValue).toArray();
			final int[] terminations = getTerminations().stream().mapToInt(Integer::intValue).toArray();
			final double[][] values = new double[maturities.length][terminations.length];

			for(int i = 0; i< maturities.length; i++) {
				for(int j = 0; j < terminations.length; j++) {
					values[i][j] = getValue(maturities[i], terminations[j]);
				}
			}

			final BivariateFunction surface = interpolator.interpolate(Arrays.stream(maturities).asDoubleStream().toArray(),
					Arrays.stream(terminations).asDoubleStream().toArray(), values);
			return surface.value(maturity, termination);
		}

	}

	@Override
	public double getValue(final double maturity, final double termination) {
		if(containsEntryFor(maturity, termination)) {
			return super.getValue(maturity, termination);
		}

		// round to make regular grid
		int roundingMultiplier;
		switch (getConvention()) {
		case YEARS: roundingMultiplier = 1; break;
		case MONTHS: roundingMultiplier = 12; break;
		case DAYS: roundingMultiplier = 365; break;
		case WEEKS: roundingMultiplier = 52; break;
		default: throw new RuntimeException("No tableConvention specified");
		}


		final int roundedMaturity = Math.toIntExact(Math.round(maturity * roundingMultiplier));
		final int roundedTermination = Math.toIntExact(Math.round(termination * roundingMultiplier)) - roundedMaturity;

		return getValue(roundedMaturity, roundedTermination);
	}

	@Override
	public DataTableInterpolated clone() {

		final int[] maturities = new int[size()];
		final int[] terminations = new int[size()];
		final double[] values = new double[size()];

		int i = 0;
		for(final int maturity : getMaturities()) {
			for(final int termination : getTerminationsForMaturity(maturity)) {
				maturities[i] = maturity;
				terminations[i] = termination;
				values[i++] = getValue(maturity, termination);
			}
		}

		return new DataTableInterpolated(getName(), getConvention(), getReferenceDate(), getScheduleMetaData(), maturities, terminations, values);
	}

	@Override
	public String toString() {
		return toString(1.0);
	}

	@Override
	public String toString(final double unit) {
		final StringBuilder builder = new StringBuilder();
		builder.append("DataTableInterpolated with interpolator "+ interpolator.getClass()+ " and base table: ");
		builder.append(super.toString(unit));

		return builder.toString();
	}

}
