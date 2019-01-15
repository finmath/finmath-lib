package net.finmath.marketdata.model.volatilities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.ScheduleMetaData;

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

	private static final long serialVersionUID = -3106297186490797114L;

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

	private final ScheduleMetaData					floatMetaSchedule;
	private final ScheduleMetaData 					fixMetaSchedule;

	private final		Map<DataKey, Double>		entryMap = new HashMap<>();
	private transient	Map<Integer, int[][]>		keyMap;

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
	 * @param moneynesss The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(LocalDate referenceDate, QuotingConvention quotingConvention,
			String forwardCurveName, String discountCurveName, ScheduleMetaData floatMetaSchedule, ScheduleMetaData fixMetaSchedule,
			double[] maturities, double[] tenors, int[] moneynesss, double[] values) {
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
	 * @param maturities The maturities of the options as offset in months from the reference date.
	 * @param tenors The tenors of the swaps as offset in months from the option maturity.
	 * @param moneynesss The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(LocalDate referenceDate, QuotingConvention quotingConvention,
			String forwardCurveName, String discountCurveName, ScheduleMetaData floatMetaSchedule, ScheduleMetaData fixMetaSchedule,
			int[] maturities, int[] tenors, int[] moneynesss, double[] values) {
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
	 * @param tenorCodes The schedules of the swaptions encoded in the format '6M10Y'
	 * @param moneynesss The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(LocalDate referenceDate, QuotingConvention quotingConvention,
			String forwardCurveName, String discountCurveName, ScheduleMetaData floatMetaSchedule, ScheduleMetaData fixMetaSchedule,
			String[] tenorCodes, int[] moneynesss, double[] values) {
		this(referenceDate, quotingConvention, 0, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < tenorCodes.length; i++) {
			entryMap.put(new DataKey(tenorCodes[i], moneynesss[i]), values[i]);
		}
	}

	/**
	 * Create the lattice with {@link QuotingConvention}{@code .VOLATILITYLOGNORMAL}.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param displacement The displacement used the implied lognormal volatilities.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param maturities The maturities of the options as offset in months from the reference date.
	 * @param tenors The tenors of the swaps as offset in months from the option maturity.
	 * @param moneynesss The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(LocalDate referenceDate, QuotingConvention quotingConvention, double displacement, String forwardCurveName, String discountCurveName,
			ScheduleMetaData floatMetaSchedule, ScheduleMetaData fixMetaSchedule, int[] maturities, int[] tenors, int[] moneynesss, double[] values) {
		this(referenceDate, quotingConvention, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < maturities.length; i++) {
			entryMap.put(new DataKey(maturities[i], tenors[i], moneynesss[i]), values[i]);
		}
	}

	/**
	 * Create the lattice with {@link QuotingConvention}{@code .VOLATILITYLOGNORMAL}.
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
	 * @param moneynesss The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(LocalDate referenceDate, QuotingConvention quotingConvention, double displacement, String forwardCurveName, String discountCurveName,
			ScheduleMetaData floatMetaSchedule, ScheduleMetaData fixMetaSchedule, double[] maturities, double[] tenors, int[] moneynesss, double[] values) {
		this(referenceDate, quotingConvention, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < maturities.length; i++) {
			entryMap.put(new DataKey(maturities[i], tenors[i], moneynesss[i]), values[i]);
		}
	}

	/**
	 * Create the lattice with {@link QuotingConvention}{@code .VOLATILITYLOGNORMAL}.
	 *
	 * @param referenceDate The reference date of the swaptions.
	 * @param quotingConvention The quoting convention of the data.
	 * @param displacement The displacement used the implied lognormal volatilities.
	 * @param forwardCurveName The name of the forward curve associated with these swaptions.
	 * @param discountCurveName The name of the discount curve associated with these swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param tenorCodes The schedules of the swaptions encoded in the format '6M10Y'
	 * @param moneynesss The moneyness' in basis points on the par swap rate.
	 * @param values The values to be stored.
	 */
	public SwaptionDataLattice(LocalDate referenceDate, QuotingConvention quotingConvention, double displacement, String forwardCurveName, String discountCurveName,
			ScheduleMetaData floatMetaSchedule, ScheduleMetaData fixMetaSchedule, String[] tenorCodes, int[] moneynesss, double[] values) {
		this(referenceDate, quotingConvention, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

		for(int i = 0; i < tenorCodes.length; i++) {
			entryMap.put(new DataKey(tenorCodes[i], moneynesss[i]), values[i]);
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
	private SwaptionDataLattice(LocalDate referenceDate, QuotingConvention quotingConvention, double displacement,
			String forwardCurveName, String discountCurveName, ScheduleMetaData floatMetaSchedule, ScheduleMetaData fixMetaSchedule) {
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
	public SwaptionDataLattice convertLattice(QuotingConvention targetConvention, AnalyticModelInterface model) {
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
	public SwaptionDataLattice convertLattice(QuotingConvention targetConvention, double displacement, AnalyticModelInterface model) {

		if(displacement != 0 && targetConvention != QuotingConvention.PAYERVOLATILITYLOGNORMAL) {
			throw new IllegalArgumentException("SwaptionDataLattice only supports displacement, when using QuotingCOnvention.PAYERVOLATILITYLOGNORMAL.");
		}

		//Reverse sign of moneyness, if switching between payer and receiver convention.
		int reverse = ((targetConvention == QuotingConvention.RECEIVERPRICE) ^ (this.quotingConvention == QuotingConvention.RECEIVERPRICE)) ? -1 : 1;

		List<Integer> maturities	= new ArrayList<>();
		List<Integer> tenors		= new ArrayList<>();
		List<Integer> moneynesss	= new ArrayList<>();
		List<Double> values		= new ArrayList<>();

		for(DataKey key : entryMap.keySet()) {
			maturities.add(key.maturity);
			tenors.add(key.tenor);
			moneynesss.add(key.moneyness * reverse);
			values.add(getValue(key.maturity, key.tenor, key.moneyness, targetConvention, displacement, model));
		}

		return new SwaptionDataLattice(this.referenceDate, targetConvention, displacement,
				this.forwardCurveName, this.discountCurveName, this.floatMetaSchedule, this.fixMetaSchedule,
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
	public SwaptionDataLattice append(SwaptionDataLattice other, AnalyticModelInterface model) {

		SwaptionDataLattice combined = new SwaptionDataLattice(referenceDate, quotingConvention, displacement,
				forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);
		combined.entryMap.putAll(entryMap);

		if(quotingConvention == other.quotingConvention && displacement == other.displacement) {
			combined.entryMap.putAll(other.entryMap);
		} else {
			SwaptionDataLattice converted = other.convertLattice(quotingConvention, displacement, model);
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
		Map<Integer, List<Set<Integer>>> newMap = new TreeMap<>();

		for(DataKey key : entryMap.keySet()) {
			if(! newMap.containsKey(key.moneyness)) {
				newMap.put(key.moneyness, new ArrayList<Set<Integer>>());
				newMap.get(key.moneyness).add(new TreeSet<Integer>());
				newMap.get(key.moneyness).add(new TreeSet<Integer>());
			}
			newMap.get(key.moneyness).get(0).add(key.maturity);
			newMap.get(key.moneyness).get(1).add(key.tenor);
		}

		Map<Integer, int[][]> keyMap = new TreeMap<>();
		for(int moneyness : newMap.keySet()) {
			int[][] values = new int[2][];

			values[0] = newMap.get(moneyness).get(0).stream().mapToInt(Integer::intValue).toArray();
			values[1] = newMap.get(moneyness).get(1).stream().mapToInt(Integer::intValue).toArray();

			keyMap.put(moneyness, values);
		}
		this.keyMap = keyMap;
		return Collections.unmodifiableMap(keyMap);
	}

	/**
	 * Return all levels of moneyness for which data exists.
	 *
	 * @return The levels of moneyness.
	 */
	public int[] getMoneyness() {
		return getGridNodesPerMoneyness().keySet().stream().sorted().mapToInt(Integer::intValue).toArray();
	}

	/**
	 * Return all valid maturities for a given moneyness.
	 *
	 * @param moneyness The moneyness for which to get the maturities.
	 * @return The maturities.
	 */
	public int[] getMaturities(int moneyness) {
		try {
			return getGridNodesPerMoneyness().get(moneyness)[0];
		} catch (NullPointerException e) {
			return new int[0];
		}
	}

	/**
	 * Retrun all valid tenors for a given moneyness and maturity.
	 *
	 * @param moneyness The moneyness forwhich to get the tenors.
	 * @param maturity The maturities for which to get the tenors.
	 * @return The tenors.
	 */
	public int[] getTenors(int moneyness, int maturity) {

		try {
			Set<Integer> ret = new TreeSet<>();
			for(int tenor : getGridNodesPerMoneyness().get(moneyness)[1]) {
				if(containsEntryFor(maturity, tenor, moneyness)) {
					ret.add(tenor);
				}
			}
			return ret.stream().mapToInt(Integer::intValue).toArray();
		} catch (NullPointerException e) {
			return new int[0];
		}
	}

	/**
	 * Returns true if the lattice contains an entry at the specified location.
	 *
	 * @param maturity The maturity to check.
	 * @param tenor The tenor to check.
	 * @param moneyness The moneyness to check.
	 * @return True iff there is an entry at the specified location.
	 */
	public boolean containsEntryFor(int maturity, int tenor, int moneyness) {
		return entryMap.containsKey(new DataKey(maturity, tenor, moneyness));
	}

	/**
	 * Return the value in the quoting convention of this lattice.
	 *
	 * @param maturity The maturity of the option as year fraction from the reference date.
	 * @param tenor The tenor of the swap as year fraction from the reference date.
	 * @param moneyness The moneyness in basis points on the par swap rate.
	 *
	 * @return The value as stored in the lattice.
	 */
	public double getValue(double maturity, double tenor, int moneyness) {
		return getValue(new DataKey(maturity, tenor, moneyness));
	}

	/**
	 * Return the value in the quoting convention of this lattice.
	 *
	 * @param maturity The maturity of the option as offset in months from the reference date.
	 * @param tenor The tenor of the swap as offset in months from the option maturity.
	 * @param moneyness The moneyness in basis points on the par swap rate.
	 *
	 * @return The value as stored in the lattice.
	 */
	public double getValue(int maturity, int tenor, int moneyness) {
		return getValue(new DataKey(maturity, tenor, moneyness));
	}

	/**
	 * Return the value in the quoting convention of this lattice.
	 *
	 * @param tenorCode The schedule of the swaption encoded in the format '6M10Y'
	 * @param moneyness The moneyness in basis points on the par swap rate.
	 *
	 * @return The value as stored in the lattice.
	 */
	public double getValue(String tenorCode, int moneyness) {
		return getValue(new DataKey(tenorCode, moneyness));
	}

	/**
	 * Internal getValue method for other methods to call.
	 *
	 * @param key
	 * @return The value as stored in the lattice.
	 */
	private double getValue(DataKey key) {
		return entryMap.get(key);
	}

	/**
	 * Return the value in the given quoting convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param maturity The maturity of the option as year fraction from the reference date.
	 * @param tenor The tenor of the swap as year fraction from the reference date.
	 * @param moneyness The moneyness in basis points on the par swap rate, as understood in the original convention.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(double maturity, double tenor, int moneyness, QuotingConvention convention, double displacement, AnalyticModelInterface model) {
		DataKey key = new DataKey(maturity, tenor, moneyness);
		return convertToConvention(getValue(key), key, convention, displacement, this.quotingConvention, this.displacement, model);
	}

	/**
	 * Return the value in the given quoting convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param maturity The maturity of the option as offset in months from the reference date.
	 * @param tenor The tenor of the swap as offset in months from the option maturity.
	 * @param moneyness The moneyness in basis points on the par swap rate, as understood in the original convention.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(int maturity, int tenor, int moneyness, QuotingConvention convention, double displacement, AnalyticModelInterface model) {
		DataKey key = new DataKey(maturity, tenor, moneyness);
		return convertToConvention(getValue(key), key, convention, displacement, this.quotingConvention, this.displacement, model);
	}

	/**
	 * Return the value in the given quoting convention.
	 * Conversion involving receiver premium assumes zero wide collar.
	 *
	 * @param tenorCode The schedule of the swaption encoded in the format '6M10Y'
	 * @param moneyness The moneyness in basis points on the par swap rate, as understood in the original convention.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(String tenorCode, int moneyness, QuotingConvention convention, double displacement, AnalyticModelInterface model) {
		DataKey key = new DataKey(tenorCode, moneyness);
		return convertToConvention(getValue(key), key, convention, displacement, this.quotingConvention, this.displacement, model);
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
	private double convertToConvention(double value, DataKey key, QuotingConvention toConvention, double toDisplacement,
			QuotingConvention fromConvention, double fromDisplacement, AnalyticModelInterface model) {

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

		ScheduleInterface floatSchedule	= floatMetaSchedule.generateSchedule(getReferenceDate(), key.maturity, key.tenor);
		ScheduleInterface fixSchedule	= fixMetaSchedule.generateSchedule(getReferenceDate(), key.maturity, key.tenor);

		double forward = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
		double optionMaturity = floatSchedule.getFixing(0);
		double offset = key.moneyness /10000.0;
		double optionStrike = forward + (this.quotingConvention == QuotingConvention.RECEIVERPRICE ? -offset : offset);
		double payoffUnit = SwapAnnuity.getSwapAnnuity(0, fixSchedule, model.getDiscountCurve(discountCurveName), model);

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
	 * @return The displacement, used in case of {@code QuotingConvention.VOLATILITYLOGNORMAL}.
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
	public ScheduleMetaData getFloatMetaSchedule() {
		return floatMetaSchedule;
	}

	/**
	 * @return The schedule meta data for the fix leg of the swaptions.
	 */
	public ScheduleMetaData getFixMetaSchedule() {
		return fixMetaSchedule;
	}

	/**
	 * A key used to reference swaption data in an map.
	 * Key overrides {@code equals} and {@code hashCode} for quick data recovery.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private static class DataKey implements Serializable {

		private static final long serialVersionUID = -2768171106471398276L;

		final int maturity;
		final int tenor;
		final int moneyness;

		DataKey(int maturity, int tenor, int moneyness) {
			super();
			this.maturity	= maturity;
			this.tenor		= tenor;
			this.moneyness	= moneyness;
		}

		DataKey(double maturity, double tenor, int moneyness) {
			super();
			this.maturity	= (int) Math.round(maturity * 12);
			this.tenor		= (int) Math.round((tenor-maturity) * 12);
			this.moneyness	= moneyness;
		}

		DataKey(String tenorCode, int moneyness) {
			super();
			String[] inputs = tenorCode.split("(?<=\\D)(?=\\d)|(?<=\\d)(?=\\D)", 4);
			this.maturity	= Integer.parseInt(inputs[0]) * (inputs[1].equalsIgnoreCase("Y")? 12 : inputs[1].equalsIgnoreCase("M")? 1 : 0 );
			this.tenor		= Integer.parseInt(inputs[2]) * (inputs[3].equalsIgnoreCase("Y")? 12 : inputs[3].equalsIgnoreCase("M")? 1 : 0 );
			this.moneyness	= moneyness;
		}

		@Override
		public boolean equals(Object other) {
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
			return (int) (maturity + 100* tenor + 10000* moneyness);
		}
	}
}
