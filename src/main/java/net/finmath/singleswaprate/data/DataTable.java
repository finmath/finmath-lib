package net.finmath.singleswaprate.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import net.finmath.time.SchedulePrototype;


/**
 * An interface for storing double values in a tenor grid. Access is provided either as int, with maturity as offset from the reference date and termination as offset
 * from the maturity date, both with regards to the table convention, or as double, with maturity and termination being year fractions from the reference date.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface DataTable extends Serializable, Cloneable {

	/**
	 * Possible conventions for the table. Specifies the translation of table coordinates.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	enum TableConvention {
		MONTHS,
		YEARS,
		DAYS,
		WEEKS
	}

	/**
	 * Provides an overview of the contents of this table as basic java objects sorted in an unmodifiable map.
	 * Values are provided in lists, finmath components are converted to Strings.
	 *
	 * @param table The table to be exported.
	 * @return Map of the table contents.
	 */
	static Map<String, Object> exportTable(final DataTable table) {
		final Map<String, Object> map = new HashMap<>();
		map.put("name", table.getName());
		map.put("tableConvention", table.getConvention().toString());
		map.put("referenceDate", table.getReferenceDate());
		map.put("scheduleMetaData", table.getScheduleMetaData().toString());

		final List<Integer> maturities	= new ArrayList<>();
		final List<Integer> terminations	= new ArrayList<>();
		final List<Double> values			= new ArrayList<>();

		for(final int mat : table.getMaturities()) {
			for(final int ter : table.getTerminationsForMaturity(mat)) {
				maturities.add(mat);
				terminations.add(ter);
				values.add(table.getValue(mat, ter));
			}
		}

		map.put("maturities", Collections.unmodifiableList(maturities));
		map.put("terminations", Collections.unmodifiableList(terminations));
		map.put("values", Collections.unmodifiableList(values));
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Add a point to the grid of the table.
	 *
	 * @param maturity The maturity of the point as offset with respect to the reference date.
	 * @param termination The termination of the point as offset with respect to the maturity date.
	 * @param value The value at the point.
	 * @return The table with the point added.
	 */
	DataTable addPoint(int maturity, int termination, double value);

	/**
	 * Add an array of points to the table.
	 *
	 * @param maturities The maturities of the points as offset with respect to the reference date.
	 * @param terminations The terminations of the points as offset with respect to the maturity date.
	 * @param values The values at the points.
	 * @return The table with the point added.
	 */
	DataTable addPoints(int[] maturities, int[] terminations, double[] values);

	/**
	 * Returns the value of the table at a given time. Interpolates if the table is set up to do so.
	 *
	 * @param maturity The maturity as offset with respect to the reference date.
	 * @param termination The termination as offset with respect to the maturity date.
	 * @return Value at the given time.
	 */
	double getValue(int maturity, int termination);

	/**
	 * Returns the value of the table at a given time. Interpolates if the table is set up to do so.
	 *
	 * @param maturity Maturity in double as year fraction with respect to reference date.
	 * @param termination Termination in double as year fraction with respect to reference date.
	 * @return Value at the given time.
	 */
	double getValue(double maturity, double termination);

	/**
	 * Checks whether the table has an actual entry at the specified coordinates. Note that even if this returns false the table may provide a value when calling
	 * <code>getValue</code> on these coordinates as the table may interpolate/extrapolate.
	 *
	 * @param maturity The maturity as offset with respect to the reference date.
	 * @param termination The termination as offset with respect to the maturity date.
	 * @return True if the table contains a data point at the coordinates, false otherwise.
	 */
	boolean containsEntryFor(int maturity, int termination);

	/**
	 * Checks whether the table has an actual entry at the specified coordinates. Note that even if this returns false the table may provide a value when calling
	 * <code>getValue</code> on these coordinates as the table may interpolate/extrapolate.
	 *
	 * @param maturity Maturity in double as year fraction with respect to reference date.
	 * @param termination Termination in double as year fraction with respect to reference date.
	 * @return True if the table contains a data point at the coordinates, false otherwise.
	 */
	boolean containsEntryFor(double maturity, double termination);

	/**
	 * Get a sorted set view of all maturities in the table.
	 *
	 * @return The maturities as sorted set.
	 */
	TreeSet<Integer> getMaturities();

	/**
	 * Get a sorted set view of all terminations in the table.
	 *
	 * @return The terminations as sorted set.
	 */
	TreeSet<Integer> getTerminations();

	/**
	 * Get a sorted set view of all terminations for a specific maturity in the table.
	 *
	 * @param maturity The maturity for which to get the terminations.
	 * @return The terminations as sorted set.
	 */
	TreeSet<Integer> getTerminationsForMaturity(int maturity);

	/**
	 * Get a sorted set view of all maturities for a speceific termination in the table.
	 *
	 * @param termination The termination for which to get the maturities.
	 * @return The maturities as sorted set.
	 */
	TreeSet<Integer> getMaturitiesForTermination(int termination);

	/**
	 * @return The name of the table.
	 */
	String getName();

	/**
	 * Returns the convention the table understands its coordinates in. As offset form the reference date.
	 *
	 * @return The convention of the table.
	 */
	TableConvention getConvention();

	/**
	 * The reference date of the table.
	 *
	 * @return The reference date.
	 */
	LocalDate getReferenceDate();

	/**
	 * @return A copy of the table.
	 */
	DataTable clone();

	/**
	 * @return The number of data points in the table.
	 */
	int size();

	/**
	 * @return The meta data as {@link SchedulePrototype} used by the table to convert int and double representations.
	 */
	SchedulePrototype getScheduleMetaData();

}
