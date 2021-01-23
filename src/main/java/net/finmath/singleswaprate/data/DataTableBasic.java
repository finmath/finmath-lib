package net.finmath.singleswaprate.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;


/**
 * A basic implementation of {@link net.finmath.singleswaprate.data.DataTable}, which provides no means of inter- or extrapolation.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class DataTableBasic implements DataTable, Cloneable {

	private static final long serialVersionUID = -529758680500367511L;

	/**
	 * Create a DataTableBasic by upgrading a {@link net.finmath.singleswaprate.data.DataTableLight} to allow access via double representation.
	 *
	 * @param baseTable The table to be upgraded.
	 * @param referenceDate The reference date.
	 * @param scheduleMetaData The schedule meta data of the table.
	 *
	 * @return The upgraded table.
	 */
	public static DataTableBasic upgradeDataTableLight(final DataTableLight baseTable, final LocalDate referenceDate, final SchedulePrototype scheduleMetaData) {

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

		return new DataTableBasic(baseTable.getName(), baseTable.getConvention(), referenceDate, scheduleMetaData,
				maturities, terminations, values);
	}

	private final String name;
	private final TableConvention convention;
	private final LocalDate referenceDate;
	private final SchedulePrototype metaSchedule;

	private final TreeSet<Integer> maturitySet = new TreeSet<>();
	private final TreeSet<Integer> terminationSet = new TreeSet<>();

	private final HashMap<DoubleKey, Double> entries = new HashMap<>();

	/**
	 * Create an empty table.
	 *
	 * @param name The name of the table.
	 * @param convention The convention of the table.
	 * @param referenceDate The referenceDate of the table.
	 * @param scheduleMetaData The schedule meta data of the table.
	 */
	public DataTableBasic(final String name, final TableConvention convention, final LocalDate referenceDate, final SchedulePrototype scheduleMetaData){
		this.name = name;
		this.convention = convention;
		this.referenceDate = referenceDate;
		this.metaSchedule = scheduleMetaData;
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
	public DataTableBasic(final String name, final TableConvention convention, final LocalDate referenceDate, final SchedulePrototype scheduleMetaData,
			final int[] maturities, final int[] terminations, final double[] values){
		this(name, convention, referenceDate, scheduleMetaData);
		for(int index = 0; index < maturities.length; index++) {
			entries.put(new DoubleKey(maturities[index], terminations[index]), values[index]);
			maturitySet.add(maturities[index]);
			terminationSet.add(terminations[index]);
		}
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
	public DataTableBasic(final String name, final TableConvention convention, final LocalDate referenceDate, final SchedulePrototype scheduleMetaData,
			final List<Integer> maturities, final List<Integer> terminations, final List<Double> values){
		this(name, convention, referenceDate, scheduleMetaData);
		for(int index = 0; index < maturities.size(); index++) {
			final int mat = maturities.get(index);
			final int term = terminations.get(index);
			final double val =values.get(index);

			entries.put(new DoubleKey(mat, term), val);
			maturitySet.add(mat);
			terminationSet.add(term);
		}
	}

	@Override
	public DataTable addPoint(final int maturity, final int termination, final double value) {
		final DataTableBasic newTable = clone();
		newTable.entries.put(new DoubleKey(maturity, termination),value);
		newTable.maturitySet.add(maturity);
		newTable.terminationSet.add(termination);
		return newTable;
	}

	@Override
	public DataTable addPoints(final int[] maturities, final int[] terminations, final double[] values) {
		final DataTableBasic newTable = clone();
		for(int index = 0; index < maturities.length; index++) {
			newTable.entries.put(new DoubleKey(maturities[index], terminations[index]), values[index]);
			newTable.maturitySet.add(maturities[index]);
			newTable.terminationSet.add(terminations[index]);
		}
		return newTable;
	}

	@Override
	public double getValue(final int maturity, final int termination) {
		final DoubleKey key = new DoubleKey(maturity, termination);
		if(entries.containsKey(key)) {
			return entries.get(new DoubleKey(maturity,termination));
		} else {
			throw new NullPointerException("Key not found.");
		}
	}

	@Override
	public double getValue(final double maturity, final double termination) {
		final DoubleKey key = new DoubleKey(maturity, termination);
		if(entries.containsKey(key)) {
			return entries.get(new DoubleKey(maturity,termination));
		} else {
			throw new NullPointerException("Key not found.");
		}
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Override
	public boolean containsEntryFor(final int maturity, final int termination) {
		return entries.containsKey(new DoubleKey(maturity, termination));
	}

	@Override
	public boolean containsEntryFor(final double maturity, final double termination) {
		return entries.containsKey(new DoubleKey(maturity, termination));
	}

	@Override
	public TreeSet<Integer> getMaturities(){
		return new TreeSet<>(maturitySet);
	}

	@Override
	public TreeSet<Integer> getTerminations(){
		return new TreeSet<>(terminationSet);
	}

	@Override
	public TreeSet<Integer> getTerminationsForMaturity(final int maturity){
		if(maturitySet.contains(maturity)) {
			final TreeSet<Integer> returnSet = new TreeSet<>();
			for(final int termination:terminationSet) {
				if(entries.containsKey(new DoubleKey(maturity,termination))) {
					returnSet.add(termination);
				}
			}
			return returnSet;
		} else {
			throw new NullPointerException("This data table does not contain entries for maturity "+maturity);
		}
	}

	@Override
	public TreeSet<Integer> getMaturitiesForTermination(final int termination) {
		if(terminationSet.contains(termination)) {
			final TreeSet<Integer> returnSet = new TreeSet<>();
			for(final int maturity: maturitySet) {
				if(entries.containsKey(new DoubleKey(maturity,termination))) {
					returnSet.add(maturity);
				}
			}
			return returnSet;
		} else {
			throw new NullPointerException("This data table does not contain entries for termination " +termination);
		}
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	@Override
	public TableConvention getConvention() {
		return convention;
	}

	@Override
	public SchedulePrototype getScheduleMetaData() {
		return metaSchedule;
	}

	@Override
	public DataTableBasic clone() {
		final DataTableBasic newTable = new DataTableBasic(name, convention, referenceDate, metaSchedule);
		newTable.entries.putAll(entries);
		newTable.maturitySet.addAll(maturitySet);
		newTable.terminationSet.addAll(terminationSet);
		return newTable;
	}

	@Override
	public String toString() {
		return toString(1.0);
	}

	public String toString(final double unit) {
		final StringBuilder builder = new StringBuilder();
		builder.append("DataTableBasic [name="+name+", referenceDate="+referenceDate+", tableConvention=" +convention+", scheduleMetaData="+ metaSchedule+"values:\n");
		for(final int termination : terminationSet) {
			builder.append("\t"+termination);
		}
		for(final int maturity: maturitySet) {
			builder.append("\n"+maturity);
			for(final int termination:terminationSet) {
				final DoubleKey key = new DoubleKey(maturity, termination);
				builder.append('\t');
				if(entries.containsKey(key)) {
					builder.append(entries.get(key) * unit);
				}
			}
		}

		return builder.toString();
	}

	protected double getValue(final DoubleKey key) {
		return entries.get(key);
	}

	/**
	 * Nested class to use as key in values map.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	protected class DoubleKey implements Serializable {

		private static final long serialVersionUID = -2372959679853584772L;
		private final double maturity;
		private final double termination;

		/**
		 * Create key from int.
		 *
		 * @param maturity The maturity.
		 * @param termination The termination.
		 */
		protected DoubleKey(final int maturity, final int termination){
			final LocalDate startDate = dateFromOffset(referenceDate, maturity);
			final LocalDate endDate = dateFromOffset(startDate, termination);
			final Schedule schedule = metaSchedule.generateSchedule(referenceDate, startDate, endDate);
			this.maturity = schedule.getFixing(0);
			this.termination = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		}

		/**
		 * Create key from double.
		 *
		 * @param maturity The maturtiy.
		 * @param termination The termination.
		 */
		protected DoubleKey(final double maturity, final double termination){
			this.maturity = maturity;
			this.termination = termination;
		}

		private LocalDate dateFromOffset(final LocalDate startDate, final int offset) {
			LocalDate date = null;
			switch(convention) {
			case YEARS:
				date = startDate.plusYears(offset);
				break;
			case MONTHS:
				date = startDate.plusMonths(offset);
				break;
			case DAYS:
				date = startDate.plusDays(offset);
				break;
			case WEEKS:
				date = startDate.plusWeeks(offset);
				break;
			default:
				throw new IllegalArgumentException("Unknown convention " + convention + ".");
			}
			return date;
		}

		@Override
		public boolean equals(final Object other) {
			if(this == other) {
				return true;
			}

			if(other == null) {
				return false;
			}
			if(other.getClass() != getClass()) {
				return false;
			}

			if(maturity 	!= ((DoubleKey) other).maturity ) {
				return false;
			}
			return (termination == ((DoubleKey) other).termination);
		}

		@Override
		public int hashCode() {
			return (int) (maturity*termination);
		}

		@Override
		public String toString() {
			return "DoubleKey [maturity=" + maturity + ", termination=" + termination + "]";
		}
	}

}
