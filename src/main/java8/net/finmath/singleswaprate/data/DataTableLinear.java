package net.finmath.singleswaprate.data;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;

import net.finmath.interpolation.BiLinearInterpolation;
import net.finmath.time.SchedulePrototype;

/**
 * Extends {@link DataTableBasic} with the capacity to interpolate values between tenor grid nodes, using {@link BiLinearInterpolation}
 * Note that the interpolation is done to the accuracy of the table convention.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class DataTableLinear extends DataTableBasic implements DataTable, Cloneable {

	private static final long serialVersionUID = -2406767129264582719L;

	private transient BiLinearInterpolation interpolator = null;
	private static final UnivariateInterpolator sliceInterpolator = new LinearInterpolator();

	/**
	 * Create an interpolated table from a basic table.
	 *
	 * @param baseTable The table to receive interpolation.
	 * @return The table with interpolation.
	 */
	public static DataTableLinear interpolateDataTable(final DataTableBasic baseTable) {

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

		return new DataTableLinear(baseTable.getName(), baseTable.getConvention(), baseTable.getReferenceDate(), baseTable.getScheduleMetaData(),
				maturities, terminations, values);
	}

	/**
	 * Create an empty table.
	 *
	 * @param name The name of the table.
	 * @param convention The convention of the table.
	 * @param referenceDate The referenceDate of the table.
	 * @param scheduleMetaData The schedule meta data of the table.
	 */
	public DataTableLinear(final String name, final TableConvention convention, final LocalDate referenceDate,
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
	public DataTableLinear(final String name, final TableConvention convention, final LocalDate referenceDate,
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
	public DataTableLinear(final String name, final TableConvention convention, final LocalDate referenceDate,
			final SchedulePrototype scheduleMetaData, final List<Integer> maturities, final List<Integer> terminations, final List<Double> values) {
		super(name, convention, referenceDate, scheduleMetaData, maturities, terminations, values);
	}

	private void initInterpolator() {
		if(interpolator != null) {
			return;
		}

		final double[] maturities		= getMaturities().stream().mapToDouble(Integer::doubleValue).toArray();
		final double[] terminations	= getTerminations().stream().mapToDouble(Integer::doubleValue).toArray();
		final double[][] values		= new double[maturities.length][terminations.length];
		for(int iMat = 0; iMat < maturities.length; iMat++) {
			for(int iTer = 0; iTer < terminations.length; iTer++) {
				values[iMat][iTer] = super.getValue((int) maturities[iMat], (int) terminations[iTer]);
			}
		}
		interpolator = new BiLinearInterpolation(maturities, terminations, values);
	}

	@Override
	public double getValue(final int maturity, final int termination) {
		if(containsEntryFor(maturity, termination)) {
			return super.getValue(maturity, termination);
		}

		// check if either of the table dimensions is one and fits the input, otherwise default to bivariate interpolation.
		if(getMaturities().size() == 1 && getMaturities().contains(maturity)) {

			final int[] terminations = getTerminationsForMaturity(maturity).stream().mapToInt(Integer::intValue).toArray();
			final double[] values = new double[terminations.length];

			for(int i = 0; i < values.length; i++) {
				values[i] = super.getValue(maturity, terminations[i]);
			}

			final UnivariateFunction curve = sliceInterpolator.interpolate(Arrays.stream(terminations).asDoubleStream().toArray(), values);
			return curve.value(termination);

		} else if(getTerminations().size() == 1 && getTerminations().contains(termination)){

			final int[] maturities = getMaturitiesForTermination(termination).stream().mapToInt(Integer::intValue).toArray();
			final double[] values = new double[maturities.length];

			for(int i = 0; i< maturities.length; i++) {
				values[i] = super.getValue(maturities[i], termination);
			}

			final UnivariateFunction curve = sliceInterpolator.interpolate(Arrays.stream(maturities).asDoubleStream().toArray(), values);
			return curve.value(maturity);
		}
		if(size() != getMaturities().size() * getTerminations().size()) {
			throw new RuntimeException("For interpolation " +getName()+ " requires a regular grid of values.");
		}

		initInterpolator();
		return interpolator.apply(new Double(maturity), new Double(termination));

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
	public DataTableLinear clone() {

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

		return new DataTableLinear(getName(), getConvention(), getReferenceDate(), getScheduleMetaData(), maturities, terminations, values);
	}

	@Override
	public String toString() {
		return toString(1.0);
	}

	@Override
	public String toString(final double unit) {
		final StringBuilder builder = new StringBuilder();
		builder.append("DataTableLinear with base table: ");
		builder.append(super.toString(unit));

		return builder.toString();
	}
}
