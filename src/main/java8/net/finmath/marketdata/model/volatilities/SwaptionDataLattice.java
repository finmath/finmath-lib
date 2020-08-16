package net.finmath.marketdata.model.volatilities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import org.apache.commons.math3.util.Pair;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * Saves market data of swaption on a lattice of option maturity x swap tenor x option moneyness.
 * Moneyness in terms of basispoints relative to par swap rate.
 * Access to the tenor grid is given via a variety of options:
 * <ul>
 * <li>double, as year fraction, rounded to a monthly precision.</li>
 * <li>int, as an offset in months.</li>
 * <li>String, as code for the offset in the format '6M10Y'.</li>
 * </ul>
 * Moreover, the stored values can be requested in different quoting conventions.
 * For the conversion to work, ScheduleMetaData and curves need to be supplied.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SwaptionDataLattice implements Serializable {

	private static final long serialVersionUID = 5041960065072626043L;

	/**
	 * Quoting convention for swaption data in a lattice.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	public enum QuotingConvention {
		PAYERVOLATILITYLOGNORMAL,
		PAYERVOLATILITYNORMAL,
		PAYERPRICE,
		RECEIVERPRICE
	}

	private final LocalDate							referenceDate;
	private final QuotingConvention					quotingConvention;
	private final double							displacement;

	private final String							forwardCurveName;
	private final String							discountCurveName;

	private final SchedulePrototype					floatMetaSchedule;
	private final SchedulePrototype 					fixMetaSchedule;

	private final		Map<DataKey, Double>		entryMap = new HashMap<>();
	private transient	Map<Integer, int[][]>		keyMap;
	private transient	Map<Pair<Integer, Integer>, int[]>		reverseKeyMap;

	/**
	 * Create the lattice.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param maturities The maturities of the options as year fraction from the reference date.
	 * @param tenors The tenors of the swaps as year fraction from the reference date.
	 * @param moneynesss The moneyness' as actual difference of strike to par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(final LocalDate referenceDate, final QuotingConvention quotingConvention,
			final String forwardCurveName, final String discountCurveName, final SchedulePrototype floatMetaSchedule, final SchedulePrototype fixMetaSchedule,
			final double[] maturities, final double[] tenors, final double[] moneynesss, final double[] values) {
		this(referenceDate, quotingConvention, 0, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < maturities.length; i++) {
			entryMap.put(new DataKey(maturities[i], tenors[i], moneynesss[i]), values[i]);
		}
	}

	/**
	 * Create the lattice.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param maturitiesInMonths The maturities of the options as offset in months from the reference date.
	 * @param tenorsInMonths The tenors of the swaps as offset in months from the option maturity.
	 * @param moneynessBP The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(final LocalDate referenceDate, final QuotingConvention quotingConvention,
			final String forwardCurveName, final String discountCurveName, final SchedulePrototype floatMetaSchedule, final SchedulePrototype fixMetaSchedule,
			final int[] maturitiesInMonths, final int[] tenorsInMonths, final int[] moneynessBP, final double[] values) {
		this(referenceDate, quotingConvention, 0, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < maturitiesInMonths.length; i++) {
			entryMap.put(new DataKey(maturitiesInMonths[i], tenorsInMonths[i], moneynessBP[i]), values[i]);
		}
	}

	/**
	 * Create the lattice.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param tenorCodes The schedules of the swaptions encoded in the format '6M10Y'
	 * @param moneynessBP The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(final LocalDate referenceDate, final QuotingConvention quotingConvention,
			final String forwardCurveName, final String discountCurveName, final SchedulePrototype floatMetaSchedule, final SchedulePrototype fixMetaSchedule,
			final String[] tenorCodes, final int[] moneynessBP, final double[] values) {
		this(referenceDate, quotingConvention, 0, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < tenorCodes.length; i++) {
			entryMap.put(new DataKey(tenorCodes[i], moneynessBP[i]), values[i]);
		}
	}

	/**
	 * Create the lattice with {@link QuotingConvention}{@code .PAYERVOLATILITYLOGNORMAL}.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param displacement The displacement used the implied lognormal volatilities.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param maturitiesInMonths The maturities of the options as offset in months from the reference date.
	 * @param tenorsInMonths The tenors of the swaps as offset in months from the option maturity.
	 * @param moneynessBP The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(final LocalDate referenceDate, final QuotingConvention quotingConvention, final double displacement, final String forwardCurveName, final String discountCurveName,
			final SchedulePrototype floatMetaSchedule, final SchedulePrototype fixMetaSchedule, final int[] maturitiesInMonths, final int[] tenorsInMonths, final int[] moneynessBP, final double[] values) {
		this(referenceDate, quotingConvention, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < maturitiesInMonths.length; i++) {
			entryMap.put(new DataKey(maturitiesInMonths[i], tenorsInMonths[i], moneynessBP[i]), values[i]);
		}
	}

	/**
	 * Create the lattice with {@link QuotingConvention}{@code .PAYERVOLATILITYLOGNORMAL}.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param displacement The displacement used the implied lognormal volatilities.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param maturities The maturities of the options as year fraction from the reference date.
	 * @param tenors The tenors of the swaps as year fraction from the reference date.
	 * @param moneynesss The moneyness' as actual difference of strike to par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(final LocalDate referenceDate, final QuotingConvention quotingConvention, final double displacement, final String forwardCurveName, final String discountCurveName,
			final SchedulePrototype floatMetaSchedule, final SchedulePrototype fixMetaSchedule, final double[] maturities, final double[] tenors, final double[] moneynesss, final double[] values) {
		this(referenceDate, quotingConvention, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < maturities.length; i++) {
			entryMap.put(new DataKey(maturities[i], tenors[i], moneynesss[i]), values[i]);
		}
	}

	/**
	 * Create the lattice with {@link QuotingConvention}{@code .PAYERVOLATILITYLOGNORMAL}.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param displacement The displacement used the implied lognormal volatilities.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param tenorCodes The schedules of the swaptions encoded in the format '6M10Y'
	 * @param moneynessBP The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(final LocalDate referenceDate, final QuotingConvention quotingConvention, final double displacement, final String forwardCurveName, final String discountCurveName,
			final SchedulePrototype floatMetaSchedule, final SchedulePrototype fixMetaSchedule, final String[] tenorCodes, final int[] moneynessBP, final double[] values) {
		this(referenceDate, quotingConvention, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < tenorCodes.length; i++) {
			entryMap.put(new DataKey(tenorCodes[i], moneynessBP[i]), values[i]);
		}
	}

	/**
	 * Private constructor to initialize all field without actually filling the map.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param displacement The displacement, used in case of lognormal volatility.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 */
	private SwaptionDataLattice(final LocalDate referenceDate, final QuotingConvention quotingConvention, final double displacement,
			final String forwardCurveName, final String discountCurveName, final SchedulePrototype floatMetaSchedule, final SchedulePrototype fixMetaSchedule) {
		super();
		this.referenceDate		= referenceDate;
		this.quotingConvention	= quotingConvention;
		this.displacement		= displacement;
		this.forwardCurveName	= forwardCurveName;
		this.discountCurveName	= discountCurveName;
		this.floatMetaSchedule	= floatMetaSchedule;
		this.fixMetaSchedule	= fixMetaSchedule;
	}

	/**
	 * Convert this lattice to store data in the given convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param targetConvention The convention to store the data in.
	 * @param model The model for context.
	 *
	 * @return The converted lattice.
	 */
	public SwaptionDataLattice convertLattice(final QuotingConvention targetConvention, final AnalyticModel model) {
		return convertLattice(targetConvention, 0, model);
	}

	/**
	 * Convert this lattice to store data in the given convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param targetConvention The convention to store the data in.
	 * @param displacement The displacement to use, if applicable.
	 * @param model The model for context.
	 *
	 * @return The converted lattice.
	 */
	public SwaptionDataLattice convertLattice(final QuotingConvention targetConvention, final double displacement, final AnalyticModel model) {

		if(displacement != 0 && targetConvention != QuotingConvention.PAYERVOLATILITYLOGNORMAL) {
			throw new IllegalArgumentException("SwaptionDataLattice only supports displacement, when using QuotingCOnvention.PAYERVOLATILITYLOGNORMAL.");
		}

		//Reverse sign of moneyness, if switching between payer and receiver convention.
		final int reverse = ((targetConvention == QuotingConvention.RECEIVERPRICE) ^ (quotingConvention == QuotingConvention.RECEIVERPRICE)) ? -1 : 1;

		final List<Integer> maturities	= new ArrayList<>();
		final List<Integer> tenors		= new ArrayList<>();
		final List<Integer> moneynesss	= new ArrayList<>();
		final List<Double> values		= new ArrayList<>();

		for(final DataKey key : entryMap.keySet()) {
			maturities.add(key.maturity);
			tenors.add(key.tenor);
			moneynesss.add(key.moneyness * reverse);
			values.add(getValue(key.maturity, key.tenor, key.moneyness, targetConvention, displacement, model));
		}

		return new SwaptionDataLattice(referenceDate, targetConvention, displacement,
				forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule,
				maturities.stream().mapToInt(Integer::intValue).toArray(),
				tenors.stream().mapToInt(Integer::intValue).toArray(),
				moneynesss.stream().mapToInt(Integer::intValue).toArray(),
				values.stream().mapToDouble(Double::doubleValue).toArray());
	}

	/**
	 * Append the data of another lattice to this lattice. If the other lattice follows a different quoting convention, it is automatically converted.
	 * However, this method does not check, whether the two lattices are aligned in terms of reference date, curve names and meta schedules.
	 * If the two lattices have shared data points, the data from this lattice will be overwritten.
	 *
	 * @param other The lattice containing the data to be appended.
	 * @param model The model to use for context, in case the other lattice follows a different convention.
	 *
	 * @return The lattice with the combined swaption entries.
	 */
	public SwaptionDataLattice append(final SwaptionDataLattice other, final AnalyticModel model) {

		final SwaptionDataLattice combined = new SwaptionDataLattice(referenceDate, quotingConvention, displacement,
				forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);
		combined.entryMap.putAll(entryMap);

		if(quotingConvention == other.quotingConvention && displacement == other.displacement) {
			combined.entryMap.putAll(other.entryMap);
		} else {
			final SwaptionDataLattice converted = other.convertLattice(quotingConvention, displacement, model);
			combined.entryMap.putAll(converted.entryMap);
		}

		return combined;
	}

	/**
	 * Get a view of the locations of swaptions in this lattice.
	 * The keys of the map are the levels of moneyness for which there are swaptions, sorted in ascending order.
	 * The entries for each moneyness consist of an array of arrays { maturities, tenors }, each sorted in ascending order.
	 * Note, there is no guarantee for the grid per moneyness to be regular.
	 * Hence, getValue may still throw a NullPointerException, even when using entries from this view.
	 *
	 * @return The view of recorded swaptions.
	 */
	public Map<Integer, int[][]> getGridNodesPerMoneyness() {

		//See if the map has already been instantiated.
		if(keyMap != null) {
			return Collections.unmodifiableMap(keyMap);
		}

		//Otherwise create the map and return it.
		final Map<Integer, List<Set<Integer>>> newMap = new HashMap<>();

		for(final DataKey key : entryMap.keySet()) {
			if(! newMap.containsKey(key.moneyness)) {
				newMap.put(key.moneyness, new ArrayList<Set<Integer>>());
				newMap.get(key.moneyness).add(new HashSet<Integer>());
				newMap.get(key.moneyness).add(new HashSet<Integer>());
			}
			newMap.get(key.moneyness).get(0).add(key.maturity);
			newMap.get(key.moneyness).get(1).add(key.tenor);
		}

		final Map<Integer, int[][]> keyMap = new TreeMap<>();
		for(final int moneyness : newMap.keySet()) {
			final int[][] values = new int[2][];

			values[0] = newMap.get(moneyness).get(0).stream().sorted().mapToInt(Integer::intValue).toArray();
			values[1] = newMap.get(moneyness).get(1).stream().sorted().mapToInt(Integer::intValue).toArray();

			keyMap.put(moneyness, values);
		}
		this.keyMap = keyMap;
		return Collections.unmodifiableMap(keyMap);
	}

	/**
	 * Get a view of the locations of swaptions in this lattice.
	 * The keys of the map are pairs of maturities and tenors for which there are swaptions.
	 * The entries for each pair consist of an array of possible moneyness values, sorted in ascending order.
	 *
	 * @return The view of recorded swaptions.
	 */
	public Map<Pair<Integer, Integer>, int[]> getMoneynessPerGridNode() {

		//See if the map has already been instantiated.
		if(reverseKeyMap != null) {
			return Collections.unmodifiableMap(reverseKeyMap);
		}

		//Otherwise create the map and return it.
		final Map<Pair<Integer, Integer>, Set<Integer>> newMap = new HashMap<>();

		for(final DataKey key : entryMap.keySet()) {
			final Pair<Integer, Integer> maturityTenorPair = new Pair<>(key.maturity, key.tenor);
			if(! newMap.containsKey(maturityTenorPair)) {
				newMap.put(maturityTenorPair, new HashSet<Integer>());
			}
			newMap.get(maturityTenorPair).add(key.moneyness);
		}

		final Map<Pair<Integer, Integer>, int[]> reverseKeyMap = new TreeMap<>();
		for(final Pair<Integer, Integer> maturityTenorPair : newMap.keySet()) {
			final int[] values = newMap.get(maturityTenorPair).stream().sorted().mapToInt(Integer::intValue).toArray();

			reverseKeyMap.put(maturityTenorPair, values);
		}
		this.reverseKeyMap = reverseKeyMap;
		return Collections.unmodifiableMap(reverseKeyMap);
	}

	/**
	 * Return all levels of moneyness for which data exists.
	 *
	 * @return The levels of moneyness in bp.
	 */
	public int[] getMoneyness() {
		return getGridNodesPerMoneyness().keySet().stream().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Return all levels of moneyness for which data exists.
	 * Moneyness is returned as actual difference strike - par swap rate.
	 *
	 * @return The levels of moneyness as difference of strike to par swap rate.
	 */
	public double[] getMoneynessAsOffsets() {
		DoubleStream moneyness = getGridNodesPerMoneyness().keySet().stream().mapToDouble(Integer::doubleValue);
		if(quotingConvention == QuotingConvention.PAYERVOLATILITYLOGNORMAL) {
			moneyness = moneyness.map(new DoubleUnaryOperator() {
				@Override
				public double applyAsDouble(final double x) {
					return x * 0.01;
				}
			});
		} else if(quotingConvention == QuotingConvention.RECEIVERPRICE) {
			moneyness = moneyness.map(new DoubleUnaryOperator() {
				@Override
				public double applyAsDouble(final double x) {
					return - x * 0.0001;
				}
			});
		} else {
			moneyness = moneyness.map(new DoubleUnaryOperator() {
				@Override
				public double applyAsDouble(final double x) {
					return x * 0.0001;
				}
			});
		}
		return moneyness.toArray();
	}

	/**
	 * Return all maturities for which data exists.
	 *
	 * @return The maturities in months.
	 */
	public int[] getMaturities() {
		final Set<Integer> setMaturities	= new HashSet<>();

		for(final int moneyness : getGridNodesPerMoneyness().keySet()) {
			setMaturities.addAll(Arrays.asList((IntStream.of(keyMap.get(moneyness)[0]).boxed().toArray(Integer[]::new))));
		}
		return setMaturities.stream().sorted().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Return all valid maturities for a given moneyness.
	 *
	 * @param moneynessBP The moneyness in bp for which to get the maturities.
	 * @return The maturities in months.
	 */
	public int[] getMaturities(final int moneynessBP) {
		try {
			return getGridNodesPerMoneyness().get(moneynessBP)[0];
		} catch (final NullPointerException e) {
			return new int[0];
		}
	}

	/**
	 * Return all valid maturities for a given moneyness.
	 * Uses the fixing times of the fix schedule to determine fractions.
	 *
	 * @param moneyness The moneyness as actual offset from par swap rate for which to get the maturities.
	 * @return The maturities as year fraction from reference date.
	 */
	public double[] getMaturities(final double moneyness) {
		final int[] maturitiesInMonths	= getMaturities(convertMoneyness(moneyness));
		final double[] maturities			= new double[maturitiesInMonths.length];

		for(int index = 0; index < maturities.length; index++) {
			maturities[index] = convertMaturity(maturitiesInMonths[index]);
		}
		return maturities;
	}

	/**
	 * Return all tenors for which data exists.
	 *
	 * @return The tenors in months.
	 */
	public int[] getTenors() {
		final Set<Integer> setTenors	= new HashSet<>();

		for(final int moneyness : getGridNodesPerMoneyness().keySet()) {
			setTenors.addAll(Arrays.asList((IntStream.of(keyMap.get(moneyness)[1]).boxed().toArray(Integer[]::new))));
		}
		return setTenors.stream().sorted().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Return all valid tenors for a given moneyness and maturity.
	 *
	 * @param moneynessBP The moneyness in bp for which to get the tenors.
	 * @param maturityInMonths The maturities in months for which to get the tenors.
	 * @return The tenors in months.
	 */
	public int[] getTenors(final int moneynessBP, final int maturityInMonths) {

		try {
			final List<Integer> ret = new ArrayList<>();
			for(final int tenor : getGridNodesPerMoneyness().get(moneynessBP)[1]) {
				if(containsEntryFor(maturityInMonths, tenor, moneynessBP)) {
					ret.add(tenor);
				}
			}
			return ret.stream().mapToInt(Integer::intValue).toArray();
		} catch (final NullPointerException e) {
			return new int[0];
		}
	}

	/**
	 * Return all valid tenors for a given moneyness and maturity.
	 * Uses the payment times of the fix schedule to determine fractions.
	 *
	 * @param moneyness The moneyness as actual offset from par swap rate for which to get the maturities.
	 * @param maturity The maturities as year fraction from the reference date.
	 * @return The tenors as year fraction from reference date.
	 */
	public double[] getTenors(final double moneyness, final double maturity) {
		final int maturityInMonths	= (int) Math.round(maturity * 12);
		final int[] tenorsInMonths	= getTenors(convertMoneyness(moneyness), maturityInMonths);
		final double[] tenors			= new double[tenorsInMonths.length];

		for(int index = 0; index < tenors.length; index++) {
			tenors[index] = convertTenor(maturityInMonths, tenorsInMonths[index]);
		}
		return tenors;
	}

	/**
	 * Convert moneyness given as difference to par swap rate to moneyness in bp.
	 * Uses the fixing times of the fix schedule to determine fractions.
	 *
	 * @param moneyness as offset.
	 * @return Moneyness in bp.
	 */
	private int convertMoneyness(final double moneyness) {
		if(quotingConvention == QuotingConvention.PAYERVOLATILITYLOGNORMAL) {
			return (int) Math.round(moneyness * 100);
		} else if(quotingConvention == QuotingConvention.RECEIVERPRICE) {
			return - (int) Math.round(moneyness * 10000);
		} else {
			return (int) Math.round(moneyness * 10000);
		}
	}

	/**
	 * Convert maturity given as offset in months to year fraction.
	 *
	 * @param maturityInMonths The maturity as offset in months.
	 * @return The maturity as year fraction.
	 */
	private double convertMaturity(final int maturityInMonths) {
		final Schedule schedule = fixMetaSchedule.generateSchedule(referenceDate, maturityInMonths, 12);
		return schedule.getFixing(0);
	}

	/**
	 * Convert tenor given as offset in months to year fraction.
	 *
	 * @param maturityInMonths The maturity as offset in months.
	 * @param tenorInMonths The tenor as offset in months.
	 * @return THe tenor as year fraction.
	 */
	private double convertTenor(final int maturityInMonths, final int tenorInMonths) {
		final Schedule schedule = fixMetaSchedule.generateSchedule(referenceDate, maturityInMonths, tenorInMonths);
		return schedule.getPayment(schedule.getNumberOfPeriods()-1);
	}

	/**
	 * Returns true if the lattice contains an entry at the specified location.
	 *
	 * @param maturityInMonths The maturity in months to check.
	 * @param tenorInMonths The tenor in months to check.
	 * @param moneynessBP The moneyness in bp to check.
	 * @return True iff there is an entry at the specified location.
	 */
	public boolean containsEntryFor(final int maturityInMonths, final int tenorInMonths, final int moneynessBP) {
		return entryMap.containsKey(new DataKey(maturityInMonths, tenorInMonths, moneynessBP));
	}

	/**
	 * Return the value in the quoting convention of this lattice.
	 *
	 * @param maturity The maturity of the option as year fraction from the reference date.
	 * @param tenor The tenor of the swap as year fraction from the reference date.
	 * @param moneyness The moneyness as actual difference of strike to par swap rate.
	 *
	 * @return The value as stored in the lattice.
	 */
	public double getValue(final double maturity, final double tenor, final double moneyness) {
		return getValue(new DataKey(maturity, tenor, moneyness));
	}

	/**
	 * Return the value in the quoting convention of this lattice.
	 *
	 * @param maturityInMonths The maturity of the option as offset in months from the reference date.
	 * @param tenorInMonths The tenor of the swap as offset in months from the option maturity.
	 * @param moneynessBP The moneyness in basis points on the par swap rate.
	 *
	 * @return The value as stored in the lattice.
	 */
	public double getValue(final int maturityInMonths, final int tenorInMonths, final int moneynessBP) {
		return getValue(new DataKey(maturityInMonths, tenorInMonths, moneynessBP));
	}

	/**
	 * Return the value in the quoting convention of this lattice.
	 *
	 * @param tenorCode The schedule of the swaption encoded in the format '6M10Y'
	 * @param moneynessBP The moneyness in basis points on the par swap rate.
	 *
	 * @return The value as stored in the lattice.
	 */
	public double getValue(final String tenorCode, final int moneynessBP) {
		return getValue(new DataKey(tenorCode, moneynessBP));
	}

	/**
	 * Internal getValue method for other methods to call.
	 *
	 * @param key
	 * @return The value as stored in the lattice.
	 */
	private double getValue(final DataKey key) {
		return entryMap.get(key);
	}

	/**
	 * Return the value in the given quoting convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param maturity The maturity of the option as year fraction from the reference date.
	 * @param tenor The tenor of the swap as year fraction from the reference date.
	 * @param moneyness The moneyness as actual difference of strike to par swap rate.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(final double maturity, final double tenor, final double moneyness, final QuotingConvention convention, final double displacement, final AnalyticModel model) {
		final DataKey key = new DataKey(maturity, tenor, moneyness);
		return convertToConvention(getValue(key), key, convention, displacement, quotingConvention, this.displacement, model);
	}

	/**
	 * Return the value in the given quoting convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param maturityInMonths The maturity of the option as offset in months from the reference date.
	 * @param tenorInMonths The tenor of the swap as offset in months from the option maturity.
	 * @param moneynessBP The moneyness in basis points on the par swap rate, as understood in the original convention.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(final int maturityInMonths, final int tenorInMonths, final int moneynessBP, final QuotingConvention convention, final double displacement, final AnalyticModel model) {
		final DataKey key = new DataKey(maturityInMonths, tenorInMonths, moneynessBP);
		return convertToConvention(getValue(key), key, convention, displacement, quotingConvention, this.displacement, model);
	}

	/**
	 * Return the value in the given quoting convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param tenorCode The schedule of the swaption encoded in the format '6M10Y'
	 * @param moneynessBP The moneyness in basis points on the par swap rate, as understood in the original convention.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(final String tenorCode, final int moneynessBP, final QuotingConvention convention, final double displacement, final AnalyticModel model) {
		final DataKey key = new DataKey(tenorCode, moneynessBP);
		return convertToConvention(getValue(key), key, convention, displacement, quotingConvention, this.displacement, model);
	}

	/**
	 * Convert the value to requested quoting convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param value The value to convert.
	 * @param key The key of the value.
	 * @param toConvention The convention to convert to.
	 * @param toDisplacement The displacement to be used, if converting to log normal implied volatility.
	 * @param fromConvention The current convention of the value.
	 * @param fromDisplacement The current displacement.
	 * @param model The model for context.
	 *
	 * @return The converted value.
	 */
	private double convertToConvention(final double value, final DataKey key, final QuotingConvention toConvention, final double toDisplacement,
			final QuotingConvention fromConvention, final double fromDisplacement, final AnalyticModel model) {

		if(toConvention == fromConvention) {
			if(toConvention != QuotingConvention.PAYERVOLATILITYLOGNORMAL) {
				return value;
			} else {
				if(toDisplacement == fromDisplacement) {
					return value;
				} else {
					return convertToConvention(convertToConvention(value, key, QuotingConvention.PAYERPRICE, 0, fromConvention, fromDisplacement, model),
							key, toConvention, toDisplacement, QuotingConvention.PAYERPRICE, 0, model);
				}
			}
		}

		final Schedule floatSchedule	= floatMetaSchedule.generateSchedule(getReferenceDate(), key.maturity, key.tenor);
		final Schedule fixSchedule	= fixMetaSchedule.generateSchedule(getReferenceDate(), key.maturity, key.tenor);

		final double forward = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		final double optionMaturity = floatSchedule.getFixing(0);
		final double offset = key.moneyness /10000.0;
		final double optionStrike = forward + (quotingConvention == QuotingConvention.RECEIVERPRICE ? -offset : offset);
		final double payoffUnit = SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);

		if(toConvention.equals(QuotingConvention.PAYERPRICE) && fromConvention.equals(QuotingConvention.PAYERVOLATILITYLOGNORMAL)) {
			return AnalyticFormulas.blackScholesGeneralizedOptionValue(forward + fromDisplacement, value, optionMaturity, optionStrike + fromDisplacement, payoffUnit);
		}
		else if(toConvention.equals(QuotingConvention.PAYERPRICE) && fromConvention.equals(QuotingConvention.PAYERVOLATILITYNORMAL)) {
			return AnalyticFormulas.bachelierOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(toConvention.equals(QuotingConvention.PAYERPRICE) && fromConvention.equals(QuotingConvention.RECEIVERPRICE)) {
			return value + (forward - optionStrike) * payoffUnit;
		}
		else if(toConvention.equals(QuotingConvention.PAYERVOLATILITYLOGNORMAL) && fromConvention.equals(QuotingConvention.PAYERPRICE)) {
			return AnalyticFormulas.blackScholesOptionImpliedVolatility(forward + toDisplacement, optionMaturity, optionStrike + toDisplacement, payoffUnit, value);
		}
		else if(toConvention.equals(QuotingConvention.PAYERVOLATILITYNORMAL) && fromConvention.equals(QuotingConvention.PAYERPRICE)) {
			return AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else if(toConvention.equals(QuotingConvention.RECEIVERPRICE) && fromConvention.equals(QuotingConvention.PAYERPRICE)) {
			return value - (forward - optionStrike) * payoffUnit;
		}
		else {
			return convertToConvention(convertToConvention(value, key, QuotingConvention.PAYERPRICE, 0, fromConvention, fromDisplacement, model),
					key, toConvention, toDisplacement, QuotingConvention.PAYERPRICE, 0, model);
		}
	}

	/**
	 * @return The number of entries in the lattice.
	 */
	public int size() {
		return entryMap.size();
	}

	/**
	 * @return The reference date of the swaptions.
	 */
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	/**
	 * @return The quoting convention used to store data.
	 */
	public QuotingConvention getQuotingConvention() {
		return quotingConvention;
	}

	/**
	 * @return The displacement, used in case of {@link QuotingConvention}{@code .PAYERVOLATILITYLOGNORMAL}.
	 */
	public double getDisplacement() {
		return displacement;
	}

	/**
	 * @return The name of the forward curve for these swaptions.
	 */
	public String getForwardCurveName() {
		return forwardCurveName;
	}

	/**
	 * @return The name of the discount curve for these swaptions.
	 */
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	/**
	 * @return The schedule meta data for the float leg of the swaptions.
	 */
	public SchedulePrototype getFloatMetaSchedule() {
		return floatMetaSchedule;
	}

	/**
	 * @return The schedule meta data for the fix leg of the swaptions.
	 */
	public SchedulePrototype getFixMetaSchedule() {
		return fixMetaSchedule;
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder(super.toString());
		builder.append("[referenceDate= " + getReferenceDate() + ", quotingConvention= " + getQuotingConvention() + ", displacement= " + getDisplacement() +
				", forwardCurveName= " + getForwardCurveName() + ", discountCurveName= " + getDiscountCurveName() + ", floatMetaSchedule= " + getFloatMetaSchedule() +
				", fixMetaSchedule= " + getFixMetaSchedule() +", Entries:\n");
		builder.append("Maturity\tTenor\tMoneyness\tValue\n");
		for(final DataKey key : entryMap.keySet()) {
			builder.append(key.maturity + "\t" + key.tenor + "\t" + key.moneyness + "\t" + getValue(key) + "\n");
		}
		builder.append("]");

		return builder.toString();
	}

	/**
	 * A key used to reference swaption data in an map.
	 * Key overrides {@code equals} and {@code hashCode} for quick data recovery.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private class DataKey implements Serializable {

		private static final long serialVersionUID = -8284316295640713492L;

		private final int maturity;
		private final int tenor;
		private final int moneyness;

		DataKey(final int maturity, final int tenor, final int moneyness) {
			super();
			this.maturity	= maturity;
			this.tenor		= tenor;
			this.moneyness	= moneyness;
		}

		DataKey(final double maturity, final double tenor, final double moneyness) {
			super();
			this.maturity	= (int) Math.round(maturity * 12);
			this.tenor		= (int) Math.round((tenor-maturity) * 12);
			this.moneyness	= convertMoneyness(moneyness);
		}

		DataKey(final String tenorCode, final int moneyness) {
			super();
			final String[] inputs = tenorCode.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)", 4);
			maturity	= Integer.parseInt(inputs[0]) * (inputs[1].equalsIgnoreCase("Y")? 12 : inputs[1].equalsIgnoreCase("M")? 1 : 0 );
			tenor		= Integer.parseInt(inputs[2]) * (inputs[3].equalsIgnoreCase("Y")? 12 : inputs[3].equalsIgnoreCase("M")? 1 : 0 );
			this.moneyness	= moneyness;
		}

		@Override
		public boolean equals(final Object other) {
			if(this == other) {
				return true;
			}

			if(other == null || other.getClass() != getClass()) {
				return false;
			}

			if(maturity 	!= ((DataKey) other).maturity || tenor != ((DataKey) other).tenor) {
				return false;
			}
			return moneyness == ((DataKey) other).moneyness;
		}

		@Override
		public int hashCode() {
			return maturity + 100* tenor + 10000* moneyness;
		}
	}
}
