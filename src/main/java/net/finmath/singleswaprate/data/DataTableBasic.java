package net.finmath.singleswaprate.data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.TreeSet;

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
	public static DataTableBasic upgradeDataTableLight(DataTableLight baseTable, LocalDate referenceDate, SchedulePrototype scheduleMetaData) {

		int[] maturities = new int[baseTable.size()];
		int[] terminations = new int[baseTable.size()];
		double[] values = new double[baseTable.size()];

		int i = 0;
		for(int maturity : baseTable.getMaturities()) {
			for(int termination : baseTable.getTerminationsForMaturity(maturity)) {
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

	private final TreeSet<Integer> maturitySet = new TreeSet<Integer>();
	private final TreeSet<Integer> terminationSet = new TreeSet<Integer>();

	private final HashMap<DoubleKey, Double> entries = new HashMap<DoubleKey, Double>();

	/**
	 * Create an empty table.
	 *
	 * @param name The name of the table.
	 * @param convention The convention of the table.
	 * @param referenceDate The referenceDate of the table.
	 * @param scheduleMetaData The schedule meta data of the table.
	 */
	public DataTableBasic(String name, TableConvention convention, LocalDate referenceDate, SchedulePrototype scheduleMetaData){
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
	public DataTableBasic(String name, TableConvention convention, LocalDate referenceDate, SchedulePrototype scheduleMetaData,
			int[] maturities, int[] terminations, double[] values){
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
	public DataTableBasic(String name, TableConvention convention, LocalDate referenceDate, SchedulePrototype scheduleMetaData,
			List<Integer> maturities, List<Integer> terminations, List<Double> values){
		this(name, convention, referenceDate, scheduleMetaData);
		for(int index = 0; index < maturities.size(); index++) {
			int mat = maturities.get(index);
			int term = terminations.get(index);
			double val =values.get(index);

			entries.put(new DoubleKey(mat, term), val);
			maturitySet.add(mat);
			terminationSet.add(term);
		}
	}

	@Override
	public DataTable addPoint(int maturity, int termination, double value) {
		DataTableBasic newTable = clone();
		newTable.entries.put(new DoubleKey(maturity, termination),value);
		newTable.maturitySet.add(maturity);
		newTable.terminationSet.add(termination);
		return newTable;
	}

	@Override
	public DataTable addPoints(int[] maturities, int[] terminations, double[] values) {
		DataTableBasic newTable = clone();
		for(int index = 0; index < maturities.length; index++) {
			newTable.entries.put(new DoubleKey(maturities[index], terminations[index]), values[index]);
			newTable.maturitySet.add(maturities[index]);
			newTable.terminationSet.add(terminations[index]);
		}
		return newTable;
	}

	@Override
	public double getValue(int maturity, int termination) {
		DoubleKey key = new DoubleKey(maturity, termination);
		if(entries.containsKey(key)) return entries.get(new DoubleKey(maturity,termination));
		else throw new NullPointerException("Key not found.");
	}

	@Override
	public double getValue(double maturity, double termination) {
		DoubleKey key = new DoubleKey(maturity, termination);
		if(entries.containsKey(key)) return entries.get(new DoubleKey(maturity,termination));
		else throw new NullPointerException("Key not found.");
	}

	@Override
	public int size() {
		return entries.size();
	}

	@Override
	public boolean containsEntryFor(int maturity, int termination) {
		return entries.containsKey(new DoubleKey(maturity, termination));
	}

	@Override
	public boolean containsEntryFor(double maturity, double termination) {
		return entries.containsKey(new DoubleKey(maturity, termination));
	}

	@Override
	public TreeSet<Integer> getMaturities(){
		return new TreeSet<Integer>(maturitySet);
	}

	@Override
	public TreeSet<Integer> getTerminations(){
		return new TreeSet<Integer>(terminationSet);
	}

	@Override
	public TreeSet<Integer> getTerminationsForMaturity(int maturity){
		if(maturitySet.contains(maturity)) {
			TreeSet<Integer> returnSet = new TreeSet<Integer>();
			for(int termination:terminationSet) if(entries.containsKey(new DoubleKey(maturity,termination))) {
				returnSet.add(termination);
			}
			return returnSet;
		}
		else throw new NullPointerException("This data table does not contain entries for maturity "+maturity);
	}

	@Override
	public TreeSet<Integer> getMaturitiesForTermination(int termination) {
		if(terminationSet.contains(termination)) {
			TreeSet<Integer> returnSet = new TreeSet<Integer>();
			for(int maturity: maturitySet) if(entries.containsKey(new DoubleKey(maturity,termination))) {
				returnSet.add(maturity);
			}
			return returnSet;
		}
		else throw new NullPointerException("This data table does not contain entries for termination " +termination);
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
		DataTableBasic newTable = new DataTableBasic(name, convention, referenceDate, metaSchedule);
		newTable.entries.putAll(entries);
		newTable.maturitySet.addAll(maturitySet);
		newTable.terminationSet.addAll(terminationSet);
		return newTable;
	}

	@Override
	public String toString() {
		return toString(1.0);
	}

	public String toString(double unit) {
		StringBuilder builder = new StringBuilder();
		builder.append("DataTableBasic [name="+name+", referenceDate="+referenceDate+", tableConvention=" +convention+", scheduleMetaData="+ metaSchedule+"values:\n");
		for(int termination : terminationSet) {
			builder.append("\t"+termination);
		}
		for(int maturity: maturitySet) {
			builder.append("\n"+maturity);
			for(int termination:terminationSet) {
				DoubleKey key = new DoubleKey(maturity, termination);
				builder.append('\t');
				if(entries.containsKey(key)) {
					builder.append(entries.get(key) * unit);
				}
			}
		}

		return builder.toString();
	}

	protected double getValue(DoubleKey key) {
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
		protected DoubleKey(int maturity, int termination){
			LocalDate startDate = dateFromOffset(referenceDate, maturity);
			LocalDate endDate = dateFromOffset(startDate, termination);
			Schedule schedule = metaSchedule.generateSchedule(referenceDate, startDate, endDate);
			this.maturity = schedule.getFixing(0);
			this.termination = schedule.getPayment(schedule.getNumberOfPeriods()-1);
		}

		/**
		 * Create key from double.
		 *
		 * @param maturity The maturtiy.
		 * @param termination The termination.
		 */
		protected DoubleKey(double maturity, double termination){
			this.maturity = maturity;
			this.termination = termination;
		}

		private LocalDate dateFromOffset(LocalDate startDate, int offset) {
			LocalDate date = null;
			switch(convention) {
			case YEARS: date = startDate.plusYears(offset); break;
			case MONTHS: date = startDate.plusMonths(offset); break;
			case DAYS: date = startDate.plusDays(offset); break;
			case WEEKS: date = startDate.plusWeeks(offset); break;
			}
			return date;
		}

		@Override
		public boolean equals(Object other) {
			if(this == other) return true;

			if(other == null) return false;
			if(other.getClass() != getClass()) return false;

			if(maturity 	!= ((DoubleKey) other).maturity )		return false;
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
