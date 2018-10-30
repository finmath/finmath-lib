package net.finmath.marketdata.model.volatilities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
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

	private static final long serialVersionUID = -3928400314495290603L;

	private final LocalDate							referenceDate;
	private final QuotingConvention					quotingConvention;
	private final double							displacement;

	private final String							forwardCurveName;
	private final String							discountCurveName;

	private final ScheduleMetaData					floatMetaSchedule;
	private final ScheduleMetaData 					fixMetaSchedule;

	private final Map<DataKey, Double>				entryMap = new HashMap<>();

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
		this(referenceDate, QuotingConvention.VOLATILITYLOGNORMAL, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

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
		this(referenceDate, QuotingConvention.VOLATILITYLOGNORMAL, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

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
		this(referenceDate,QuotingConvention.VOLATILITYLOGNORMAL, displacement, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule);

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
	 *
	 * @param targetConvention The convention to store the data in.
	 * @param displacement The displacement to use, if applicable.
	 * @param model The model for context.
	 *
	 * @return The converted lattice.
	 */
	public SwaptionDataLattice convertLattice(QuotingConvention targetConvention, double displacement, AnalyticModelInterface model) {

		List<Integer> maturities	= new ArrayList<>();
		List<Integer> tenors		= new ArrayList<>();
		List<Integer> moneynesss	= new ArrayList<>();
		List<Double> values		= new ArrayList<>();

		for(DataKey key : entryMap.keySet()) {
			maturities.add(key.maturity);
			tenors.add(key.tenor);
			moneynesss.add(key.moneyness);
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
	 * @return The lattice with the combined swpation entries.
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
	 *
	 * @param maturity The maturity of the option as year fraction from the reference date.
	 * @param tenor The tenor of the swap as year fraction from the reference date.
	 * @param moneyness The moneyness in basis points on the par swap rate.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(double maturity, double tenor, int moneyness, QuotingConvention convention, double displacement, AnalyticModelInterface model) {
		DataKey key = new DataKey(maturity, tenor, moneyness);
		return convertToConvention(getValue(key), key, convention, displacement, model);
	}

	/**
	 * Return the value in the given quoting convention.
	 *
	 * @param maturity The maturity of the option as offset in months from the reference date.
	 * @param tenor The tenor of the swap as offset in months from the option maturity.
	 * @param moneyness The moneyness in basis points on the par swap rate.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(int maturity, int tenor, int moneyness, QuotingConvention convention, double displacement, AnalyticModelInterface model) {
		DataKey key = new DataKey(maturity, tenor, moneyness);
		return convertToConvention(getValue(key), key, convention, displacement, model);
	}

	/**
	 * Return the value in the given quoting convention.
	 *
	 * @param tenorCode The schedule of the swaption encoded in the format '6M10Y'
	 * @param moneyness The moneyness in basis points on the par swap rate.
	 * @param convention The desired quoting convention.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The value converted to the convention.
	 */
	public double getValue(String tenorCode, int moneyness, QuotingConvention convention, double displacement, AnalyticModelInterface model) {
		DataKey key = new DataKey(tenorCode, moneyness);
		return convertToConvention(getValue(key), key, convention, displacement, model);
	}

	/**
	 * Convert the value to requested quoting convention.
	 *
	 * @param value The value to convert.
	 * @param key The key of the value.
	 * @param convention The convention to convert to.
	 * @param displacement The displacement to be used, if converting to log normal implied volatility.
	 * @param model The model for context.
	 *
	 * @return The converted value.
	 */
	private double convertToConvention(double value, DataKey key, QuotingConvention convention, double displacement, AnalyticModelInterface model) {

		if(convention == quotingConvention) {
			if(convention != QuotingConvention.VOLATILITYLOGNORMAL) {
				return value;
			} else {
				if(displacement == this.displacement) {
					return value;
				} else {
					return convertToConvention(convertToConvention(value, key, QuotingConvention.PRICE, displacement, model), key, convention, displacement, model);
				}
			}
		}

		ScheduleInterface floatSchedule	= floatMetaSchedule.generateSchedule(key.maturity, key.tenor);
		ScheduleInterface fixSchedule	= fixMetaSchedule.generateSchedule(key.maturity, key.tenor);

		double forward = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model) + this.displacement;
		double optionMaturity = floatSchedule.getFixing(0);
		double optionStrike = forward + key.moneyness /10000.0 + this.displacement;
		double payoffUnit = SwapAnnuity.getSwapAnnuity(0, fixSchedule, model.getDiscountCurve(discountCurveName), model);

		if(convention.equals(QuotingConvention.PRICE) && quotingConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
			return AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(convention.equals(QuotingConvention.PRICE) && quotingConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
			return AnalyticFormulas.bachelierOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(convention.equals(QuotingConvention.VOLATILITYLOGNORMAL) && quotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.blackScholesOptionImpliedVolatility(forward -this.displacement+displacement, optionMaturity, optionStrike-this.displacement+displacement, payoffUnit, value);
		}
		else if(convention.equals(QuotingConvention.VOLATILITYNORMAL) && quotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else {
			return convertToConvention(convertToConvention(value, key, QuotingConvention.PRICE, displacement, model), key, convention, displacement, model);
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
