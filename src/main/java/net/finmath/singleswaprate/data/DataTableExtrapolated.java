package net.finmath.singleswaprate.data;

import java.time.LocalDate;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;

import net.finmath.time.SchedulePrototype;

/**
 * Extends {@link net.finmath.singleswaprate.data.DataTableBasic} with the capacity to inter- and extrapolate values off the tenor grid.
 * Note that the interpolation is done to the accuracy of the table convention.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class DataTableExtrapolated extends DataTableInterpolated implements DataTable, Cloneable {

	private static final long serialVersionUID = 1237834784985091980L;

	/**
	 * Create an empty table.
	 *
	 * @param name The name of the table.
	 * @param convention The convention of the table.
	 * @param referenceDate The referenceDate of the table.
	 * @param scheduleMetaData The schedule meta data of the table.
	 */
	public DataTableExtrapolated(String name, TableConvention convention, LocalDate referenceDate,
			SchedulePrototype scheduleMetaData) {
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
	public DataTableExtrapolated(String name, TableConvention convention, LocalDate referenceDate,
			SchedulePrototype scheduleMetaData, int[] maturities, int[] terminations, double[] values) {
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
	public DataTableExtrapolated(String name, TableConvention convention, LocalDate referenceDate,
			SchedulePrototype scheduleMetaData, List<Integer> maturities, List<Integer> terminations, List<Double> values) {
		super(name, convention, referenceDate, scheduleMetaData, maturities, terminations, values);
	}

	@Override
	public double getValue(int maturity, int termination) {

		int[] maturities = ArrayUtils.toPrimitive(getMaturities().toArray(new Integer[0]));
		int[] terminations = ArrayUtils.toPrimitive(getTerminations().toArray(new Integer[0]));

		// constant extrapolation (assuming arrays are provided via TreeSet, i.e. sorted)
		int extraMat = Math.min(Math.max(maturity, maturities[0]), maturities[maturities.length-1]);
		int extraTer = Math.min(Math.max(termination, terminations[0]), terminations[terminations.length-1]);

		return super.getValue(extraMat, extraTer);
	}

	@Override
	public double getValue(double maturity, double termination) {
		if(containsEntryFor(maturity, termination)) return super.getValue(maturity, termination);

		// round to make regular grid
		int roundingMultiplier;
		switch (getConvention()) {
		case YEARS: roundingMultiplier = 1; break;
		case MONTHS: roundingMultiplier = 12; break;
		case DAYS: roundingMultiplier = 365; break;
		case WEEKS: roundingMultiplier = 52; break;
		default: throw new RuntimeException("No tableConvention specified");
		}


		int roundedMaturity = Math.toIntExact(Math.round(maturity * roundingMultiplier));
		int roundedTermination = Math.toIntExact(Math.round(termination * roundingMultiplier)) - roundedMaturity;

		return getValue(roundedMaturity, roundedTermination);
	}

	@Override
	public DataTableExtrapolated clone() {

		int[] maturities = new int[size()];
		int[] terminations = new int[size()];
		double[] values = new double[size()];

		int i = 0;
		for(int maturity : getMaturities()) {
			for(int termination : getTerminationsForMaturity(maturity)) {
				maturities[i] = maturity;
				terminations[i] = termination;
				values[i++] = getValue(maturity, termination);
			}
		}

		return new DataTableExtrapolated(getName(), getConvention(), getReferenceDate(), getScheduleMetaData(), maturities, terminations, values);
	}

	@Override
	public String toString() {
		return toString(1.0);
	}

	@Override
	public String toString(double unit) {
		StringBuilder builder = new StringBuilder();
		builder.append("DataTableExtrapolated with constant extrapolation and base table: ");
		builder.append(super.toString(unit));

		return builder.toString();
	}
}
