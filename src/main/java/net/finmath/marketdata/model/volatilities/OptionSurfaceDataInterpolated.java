package net.finmath.marketdata.model.volatilities;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * An interpolated equity option surface based on irregular option quotes.
 * <p>
 * The class extends {@link OptionSurfaceData} by adding finmath-native
 * interpolation
 * and quoting-convention conversion. The underlying market data may be supplied
 * either as individual {@link OptionData} quotes or as {@link OptionSmileData}
 * objects.
 * </p>
 * <p>
 * The surface does not assume a rectangular grid. Different maturities may have
 * different strike grids and different numbers of option quotes.
 * </p>
 * <p>
 * Internally, the market data are represented as nodal points
 * {@code (maturity, strike, value)}. The sorted representation is ordered by
 * maturity first and by strike within maturity. The interpolated value at
 * {@code (T,K)} is obtained by:
 * </p>
 * <ol>
 *     <li>interpolating each maturity smile in strike at {@code K};</li>
 *     <li>interpolating the resulting values in maturity at {@code T}.</li>
 * </ol>
 * <p>
 * This class is intentionally responsible only for interpolation and
 * quoting-convention conversion.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class OptionSurfaceDataInterpolated extends OptionSurfaceData implements VolatilitySurface {

	/**
	 * The default strike interpolation method.
	 */
	private static final InterpolationMethod DEFAULT_STRIKE_INTERPOLATION_METHOD = InterpolationMethod.CUBIC_SPLINE;
	/**
	 * The default strike extrapolation method.
	 */
	private static final ExtrapolationMethod DEFAULT_STRIKE_EXTRAPOLATION_METHOD = ExtrapolationMethod.CONSTANT;
	/**
	 * The default maturity interpolation method.
	 */
	private static final InterpolationMethod DEFAULT_MATURITY_INTERPOLATION_METHOD = InterpolationMethod.LINEAR;
	/**
	 * The default maturity extrapolation method.
	 */
	private static final ExtrapolationMethod DEFAULT_MATURITY_EXTRAPOLATION_METHOD = ExtrapolationMethod.LINEAR;

	/**
	 * The option quote comparator.
	 */
	private static final Comparator<OptionData> OPTION_QUOTE_COMPARATOR =
			Comparator.comparingDouble(OptionData::getMaturity)
					.thenComparingDouble(OptionData::getStrike);

	/**
	 * The option smile comparator.
	 */
	private static final Comparator<OptionSmileData> OPTION_SMILE_COMPARATOR =
			Comparator.comparingDouble(OptionSmileData::getMaturity);

	/**
	 * The smiles.
	 */
	private final OptionSmileData[] smiles;
	/**
	 * The node maturities.
	 */
	private final double[] nodeMaturities;
	/**
	 * The node strikes.
	 */
	private final double[] nodeStrikes;
	/**
	 * The node values.
	 */
	private final double[] nodeValues;
	/**
	 * The unique maturities.
	 */
	private final double[] uniqueMaturities;

	/**
	 * The strike interpolation method.
	 */
	private final InterpolationMethod strikeInterpolationMethod;
	/**
	 * The strike extrapolation method.
	 */
	private final ExtrapolationMethod strikeExtrapolationMethod;
	/**
	 * The maturity interpolation method.
	 */
	private final InterpolationMethod maturityInterpolationMethod;
	/**
	 * The maturity extrapolation method.
	 */
	private final ExtrapolationMethod maturityExtrapolationMethod;

	/**
	 * The smile interpolators.
	 */
	private final SmileInterpolator[] smileInterpolators;

	/**
	 * Creates an interpolated surface from individual option quotes.
	 * <p>
	 * The input quotes are required to be sorted by maturity and by strike
	 * within
	 * each maturity. Use {@link #ofUnsorted(OptionData[], DiscountCurve,
	 * DiscountCurve)}
	 * if the input quotes are not sorted.
	 * </p>
	 *
	 * @param optionQuotes The individual option quotes.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 */
	public OptionSurfaceDataInterpolated(
			final OptionData[] optionQuotes,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {

		this(
				prepareFromOptionQuotes(optionQuotes, false),
				discountCurve,
				equityForwardCurve,
				DEFAULT_STRIKE_INTERPOLATION_METHOD,
				DEFAULT_STRIKE_EXTRAPOLATION_METHOD,
				DEFAULT_MATURITY_INTERPOLATION_METHOD,
				DEFAULT_MATURITY_EXTRAPOLATION_METHOD
		);
	}

	/**
	 * Creates an interpolated surface from individual option quotes.
	 * <p>
	 * The input quotes are required to be sorted by maturity and by strike
	 * within
	 * each maturity. Use {@link #ofUnsorted(OptionData[], DiscountCurve,
	 * DiscountCurve, InterpolationMethod, ExtrapolationMethod,
	 * InterpolationMethod, ExtrapolationMethod)}
	 * if the input quotes are not sorted.
	 * </p>
	 *
	 * @param optionQuotes The individual option quotes.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @param strikeInterpolationMethod The interpolation method in strike
	 *     direction.
	 * @param strikeExtrapolationMethod The extrapolation method in strike
	 *     direction.
	 * @param maturityInterpolationMethod The interpolation method in maturity
	 *     direction.
	 * @param maturityExtrapolationMethod The extrapolation method in maturity
	 *     direction.
	 */
	public OptionSurfaceDataInterpolated(
			final OptionData[] optionQuotes,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve,
			final InterpolationMethod strikeInterpolationMethod,
			final ExtrapolationMethod strikeExtrapolationMethod,
			final InterpolationMethod maturityInterpolationMethod,
			final ExtrapolationMethod maturityExtrapolationMethod) {

		this(
				prepareFromOptionQuotes(optionQuotes, false),
				discountCurve,
				equityForwardCurve,
				strikeInterpolationMethod,
				strikeExtrapolationMethod,
				maturityInterpolationMethod,
				maturityExtrapolationMethod
		);
	}

	/**
	 * Creates an interpolated surface from option smiles.
	 * <p>
	 * The input smiles are required to be sorted by maturity. The strikes
	 * inside
	 * each smile are required to be sorted increasingly and to be unique. Use
	 * {@link #ofUnsorted(OptionSmileData[], DiscountCurve, DiscountCurve)} if
	 * the
	 * input smiles or strikes are not sorted.
	 * </p>
	 *
	 * @param smiles The option smiles.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 */
	public OptionSurfaceDataInterpolated(
			final OptionSmileData[] smiles,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {

		this(
				prepareFromSmiles(smiles, false),
				discountCurve,
				equityForwardCurve,
				DEFAULT_STRIKE_INTERPOLATION_METHOD,
				DEFAULT_STRIKE_EXTRAPOLATION_METHOD,
				DEFAULT_MATURITY_INTERPOLATION_METHOD,
				DEFAULT_MATURITY_EXTRAPOLATION_METHOD
		);
	}

	/**
	 * Creates an interpolated surface from option smiles.
	 * <p>
	 * The input smiles are required to be sorted by maturity. The strikes
	 * inside
	 * each smile are required to be sorted increasingly and to be unique. Use
	 * {@link #ofUnsorted(OptionSmileData[], DiscountCurve, DiscountCurve,
	 * InterpolationMethod, ExtrapolationMethod, InterpolationMethod,
	 * ExtrapolationMethod)}
	 * if the input smiles or strikes are not sorted.
	 * </p>
	 *
	 * @param smiles The option smiles.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @param strikeInterpolationMethod The interpolation method in strike
	 *     direction.
	 * @param strikeExtrapolationMethod The extrapolation method in strike
	 *     direction.
	 * @param maturityInterpolationMethod The interpolation method in maturity
	 *     direction.
	 * @param maturityExtrapolationMethod The extrapolation method in maturity
	 *     direction.
	 */
	public OptionSurfaceDataInterpolated(
			final OptionSmileData[] smiles,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve,
			final InterpolationMethod strikeInterpolationMethod,
			final ExtrapolationMethod strikeExtrapolationMethod,
			final InterpolationMethod maturityInterpolationMethod,
			final ExtrapolationMethod maturityExtrapolationMethod) {

		this(
				prepareFromSmiles(smiles, false),
				discountCurve,
				equityForwardCurve,
				strikeInterpolationMethod,
				strikeExtrapolationMethod,
				maturityInterpolationMethod,
				maturityExtrapolationMethod
		);
	}

	/**
	 * Internal constructor from a prepared, sorted surface.
	 *
	 * @param preparedSurface The prepared surface.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @param strikeInterpolationMethod The interpolation method in strike
	 *     direction.
	 * @param strikeExtrapolationMethod The extrapolation method in strike
	 *     direction.
	 * @param maturityInterpolationMethod The interpolation method in maturity
	 *     direction.
	 * @param maturityExtrapolationMethod The extrapolation method in maturity
	 *     direction.
	 */
	private OptionSurfaceDataInterpolated(
			final PreparedSurface preparedSurface,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve,
			final InterpolationMethod strikeInterpolationMethod,
			final ExtrapolationMethod strikeExtrapolationMethod,
			final InterpolationMethod maturityInterpolationMethod,
			final ExtrapolationMethod maturityExtrapolationMethod) {

		super(preparedSurface.smiles, discountCurve, equityForwardCurve);

		this.smiles = preparedSurface.smiles;
		this.nodeMaturities = preparedSurface.nodeMaturities;
		this.nodeStrikes = preparedSurface.nodeStrikes;
		this.nodeValues = preparedSurface.nodeValues;
		this.uniqueMaturities = preparedSurface.uniqueMaturities;

		this.strikeInterpolationMethod = Objects.requireNonNull(strikeInterpolationMethod, "strikeInterpolationMethod");
		this.strikeExtrapolationMethod = Objects.requireNonNull(strikeExtrapolationMethod, "strikeExtrapolationMethod");
		this.maturityInterpolationMethod = Objects.requireNonNull(maturityInterpolationMethod, "maturityInterpolationMethod");
		this.maturityExtrapolationMethod = Objects.requireNonNull(maturityExtrapolationMethod, "maturityExtrapolationMethod");

		this.smileInterpolators = buildSmileInterpolators(preparedSurface.smiles);
	}

	/**
	 * Creates an interpolated surface from sorted individual option quotes.
	 *
	 * @param optionQuotes The individual option quotes, sorted by maturity and
	 *     strike.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated of(
			final OptionData[] optionQuotes,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {

		return new OptionSurfaceDataInterpolated(optionQuotes, discountCurve, equityForwardCurve);
	}

	/**
	 * Creates an interpolated surface from sorted individual option quotes.
	 *
	 * @param optionQuotes The individual option quotes, sorted by maturity and
	 *     strike.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @param strikeInterpolationMethod The interpolation method in strike
	 *     direction.
	 * @param strikeExtrapolationMethod The extrapolation method in strike
	 *     direction.
	 * @param maturityInterpolationMethod The interpolation method in maturity
	 *     direction.
	 * @param maturityExtrapolationMethod The extrapolation method in maturity
	 *     direction.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated of(
			final OptionData[] optionQuotes,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve,
			final InterpolationMethod strikeInterpolationMethod,
			final ExtrapolationMethod strikeExtrapolationMethod,
			final InterpolationMethod maturityInterpolationMethod,
			final ExtrapolationMethod maturityExtrapolationMethod) {

		return new OptionSurfaceDataInterpolated(
				optionQuotes,
				discountCurve,
				equityForwardCurve,
				strikeInterpolationMethod,
				strikeExtrapolationMethod,
				maturityInterpolationMethod,
				maturityExtrapolationMethod
		);
	}

	/**
	 * Creates an interpolated surface from unsorted individual option quotes.
	 * <p>
	 * The input quotes are sorted by maturity and by strike within maturity.
	 * Duplicate nodes are not allowed.
	 * </p>
	 *
	 * @param optionQuotes The individual option quotes.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated ofUnsorted(
			final OptionData[] optionQuotes,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {

		return new OptionSurfaceDataInterpolated(
				prepareFromOptionQuotes(optionQuotes, true),
				discountCurve,
				equityForwardCurve,
				DEFAULT_STRIKE_INTERPOLATION_METHOD,
				DEFAULT_STRIKE_EXTRAPOLATION_METHOD,
				DEFAULT_MATURITY_INTERPOLATION_METHOD,
				DEFAULT_MATURITY_EXTRAPOLATION_METHOD
		);
	}

	/**
	 * Creates an interpolated surface from unsorted individual option quotes.
	 * <p>
	 * The input quotes are sorted by maturity and by strike within maturity.
	 * Duplicate nodes are not allowed.
	 * </p>
	 *
	 * @param optionQuotes The individual option quotes.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @param strikeInterpolationMethod The interpolation method in strike
	 *     direction.
	 * @param strikeExtrapolationMethod The extrapolation method in strike
	 *     direction.
	 * @param maturityInterpolationMethod The interpolation method in maturity
	 *     direction.
	 * @param maturityExtrapolationMethod The extrapolation method in maturity
	 *     direction.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated ofUnsorted(
			final OptionData[] optionQuotes,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve,
			final InterpolationMethod strikeInterpolationMethod,
			final ExtrapolationMethod strikeExtrapolationMethod,
			final InterpolationMethod maturityInterpolationMethod,
			final ExtrapolationMethod maturityExtrapolationMethod) {

		return new OptionSurfaceDataInterpolated(
				prepareFromOptionQuotes(optionQuotes, true),
				discountCurve,
				equityForwardCurve,
				strikeInterpolationMethod,
				strikeExtrapolationMethod,
				maturityInterpolationMethod,
				maturityExtrapolationMethod
		);
	}

	/**
	 * Creates an interpolated surface from sorted option smiles.
	 *
	 * @param smiles The option smiles, sorted by maturity and with sorted
	 *     strikes.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated of(
			final OptionSmileData[] smiles,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {

		return new OptionSurfaceDataInterpolated(smiles, discountCurve, equityForwardCurve);
	}

	/**
	 * Creates an interpolated surface from sorted option smiles.
	 *
	 * @param smiles The option smiles, sorted by maturity and with sorted
	 *     strikes.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @param strikeInterpolationMethod The interpolation method in strike
	 *     direction.
	 * @param strikeExtrapolationMethod The extrapolation method in strike
	 *     direction.
	 * @param maturityInterpolationMethod The interpolation method in maturity
	 *     direction.
	 * @param maturityExtrapolationMethod The extrapolation method in maturity
	 *     direction.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated of(
			final OptionSmileData[] smiles,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve,
			final InterpolationMethod strikeInterpolationMethod,
			final ExtrapolationMethod strikeExtrapolationMethod,
			final InterpolationMethod maturityInterpolationMethod,
			final ExtrapolationMethod maturityExtrapolationMethod) {

		return new OptionSurfaceDataInterpolated(
				smiles,
				discountCurve,
				equityForwardCurve,
				strikeInterpolationMethod,
				strikeExtrapolationMethod,
				maturityInterpolationMethod,
				maturityExtrapolationMethod
		);
	}

	/**
	 * Creates an interpolated surface from unsorted option smiles.
	 * <p>
	 * The smiles are sorted by maturity and the strikes inside each smile are
	 * sorted increasingly. Duplicate maturities or duplicate strikes inside a
	 * smile are not allowed.
	 * </p>
	 *
	 * @param smiles The option smiles.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated ofUnsorted(
			final OptionSmileData[] smiles,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve) {

		return new OptionSurfaceDataInterpolated(
				prepareFromSmiles(smiles, true),
				discountCurve,
				equityForwardCurve,
				DEFAULT_STRIKE_INTERPOLATION_METHOD,
				DEFAULT_STRIKE_EXTRAPOLATION_METHOD,
				DEFAULT_MATURITY_INTERPOLATION_METHOD,
				DEFAULT_MATURITY_EXTRAPOLATION_METHOD
		);
	}

	/**
	 * Creates an interpolated surface from unsorted option smiles.
	 * <p>
	 * The smiles are sorted by maturity and the strikes inside each smile are
	 * sorted increasingly. Duplicate maturities or duplicate strikes inside a
	 * smile are not allowed.
	 * </p>
	 *
	 * @param smiles The option smiles.
	 * @param discountCurve The discount curve.
	 * @param equityForwardCurve The equity forward curve.
	 * @param strikeInterpolationMethod The interpolation method in strike
	 *     direction.
	 * @param strikeExtrapolationMethod The extrapolation method in strike
	 *     direction.
	 * @param maturityInterpolationMethod The interpolation method in maturity
	 *     direction.
	 * @param maturityExtrapolationMethod The extrapolation method in maturity
	 *     direction.
	 * @return The interpolated option surface.
	 */
	public static OptionSurfaceDataInterpolated ofUnsorted(
			final OptionSmileData[] smiles,
			final DiscountCurve discountCurve,
			final DiscountCurve equityForwardCurve,
			final InterpolationMethod strikeInterpolationMethod,
			final ExtrapolationMethod strikeExtrapolationMethod,
			final InterpolationMethod maturityInterpolationMethod,
			final ExtrapolationMethod maturityExtrapolationMethod) {

		return new OptionSurfaceDataInterpolated(
				prepareFromSmiles(smiles, true),
				discountCurve,
				equityForwardCurve,
				strikeInterpolationMethod,
				strikeExtrapolationMethod,
				maturityInterpolationMethod,
				maturityExtrapolationMethod
		);
	}

	/**
	 * Returns the value in the default quoting convention of the surface.
	 *
	 * @param maturity The option maturity.
	 * @param strike The option strike.
	 * @return The interpolated value.
	 */
	@Override
	public double getValue(final double maturity, final double strike) {
		return getValue(maturity, strike, getQuotingConvention());
	}

	/**
	 * Returns the value in the requested quoting convention.
	 *
	 * @param maturity The option maturity.
	 * @param strike The option strike.
	 * @param quotingConvention The requested quoting convention.
	 * @return The interpolated and, if necessary, converted value.
	 */
	@Override
	public double getValue(
			final double maturity,
			final double strike,
			final QuotingConvention quotingConvention) {

		return getValue(null, maturity, strike, quotingConvention);
	}

	/**
	 * Returns the value in the requested quoting convention.
	 * <p>
	 * The model argument is currently not used, since the discount curve and
	 * the
	 * equity forward curve are stored in the surface.
	 * </p>
	 *
	 * @param model An analytic model.
	 * @param maturity The option maturity.
	 * @param strike The option strike.
	 * @param quotingConvention The requested quoting convention.
	 * @return The interpolated and, if necessary, converted value.
	 */
	@Override
	public double getValue(
			final AnalyticModel model,
			final double maturity,
			final double strike,
			final QuotingConvention quotingConvention) {

		final double valueInSurfaceConvention = getInterpolatedValue(maturity, strike);

		if (quotingConvention.equals(getQuotingConvention())) {
			return valueInSurfaceConvention;
		}

		return convertFromSurfaceConvention(maturity, strike, valueInSurfaceConvention, quotingConvention);
	}

	/**
	 * Returns the interpolated value in the surface's native quoting
	 * convention.
	 *
	 * @param maturity The option maturity.
	 * @param strike The option strike.
	 * @return The interpolated value in the surface's native quoting
	 *     convention.
	 */
	public double getInterpolatedValue(final double maturity, final double strike) {

		final double[] valuesAtStrike = new double[uniqueMaturities.length];

		for (int maturityIndex = 0; maturityIndex < uniqueMaturities.length; maturityIndex++) {
			valuesAtStrike[maturityIndex] = smileInterpolators[maturityIndex].getValue(strike);
		}

		if (uniqueMaturities.length == 1) {
			return valuesAtStrike[0];
		}

		final RationalFunctionInterpolation maturityInterpolator =
				new RationalFunctionInterpolation(
						uniqueMaturities,
						valuesAtStrike,
						maturityInterpolationMethod,
						maturityExtrapolationMethod
				);

		return maturityInterpolator.getValue(maturity);
	}

	/**
	 * Creates a clone of this surface with another quoting convention.
	 * <p>
	 * The conversion is performed at the original surface nodes. The
	 * interpolation
	 * settings are preserved.
	 * </p>
	 *
	 * @param newQuotingConvention The new quoting convention.
	 * @return A surface with the requested quoting convention.
	 */
	public OptionSurfaceDataInterpolated getCloneForQuotingConvention(final QuotingConvention newQuotingConvention) {

		Objects.requireNonNull(newQuotingConvention, "newQuotingConvention");

		if (newQuotingConvention.equals(getQuotingConvention())) {
			return this;
		}

		final List<OptionSmileData> convertedSmiles = new ArrayList<>();

		for (final OptionSmileData smile : smiles) {

			final double maturity = smile.getMaturity();
			final double[] strikes = smile.getStrikes().clone();
			final double[] convertedValues = new double[strikes.length];

			for (int strikeIndex = 0; strikeIndex < strikes.length; strikeIndex++) {

				final double strike = strikes[strikeIndex];
				final double originalValue = smile.getSmile().get(strike).getValue();

				convertedValues[strikeIndex] =
						convertFromSurfaceConvention(
								maturity,
								strike,
								originalValue,
								newQuotingConvention
						);
			}

			convertedSmiles.add(
					new OptionSmileData(
							smile.getUnderlying(),
							smile.getReferenceDate(),
							strikes,
							maturity,
							convertedValues,
							newQuotingConvention
					)
			);
		}

		return new OptionSurfaceDataInterpolated(
				convertedSmiles.toArray(new OptionSmileData[0]),
				getDiscountCurve(),
				getEquityForwardCurve(),
				strikeInterpolationMethod,
				strikeExtrapolationMethod,
				maturityInterpolationMethod,
				maturityExtrapolationMethod
		);
	}

	/**
	 * Returns the sorted smiles used by this surface.
	 *
	 * @return The sorted smiles.
	 */
	public OptionSmileData[] getSmiles() {
		return smiles.clone();
	}

	/**
	 * Returns the flattened node maturities.
	 *
	 * @return The node maturities.
	 */
	public double[] getNodeMaturities() {
		return nodeMaturities.clone();
	}

	/**
	 * Returns the flattened node strikes.
	 *
	 * @return The node strikes.
	 */
	public double[] getNodeStrikes() {
		return nodeStrikes.clone();
	}

	/**
	 * Returns the flattened node values in the native quoting convention.
	 *
	 * @return The node values.
	 */
	public double[] getNodeValues() {
		return nodeValues.clone();
	}

	/**
	 * Returns the sorted unique maturities.
	 *
	 * @return The sorted unique maturities.
	 */
	@Override
	public double[] getMaturities() {
		return uniqueMaturities.clone();
	}

	/**
	 * Returns the interpolation method in strike direction.
	 *
	 * @return The strike interpolation method.
	 */
	public InterpolationMethod getStrikeInterpolationMethod() {
		return strikeInterpolationMethod;
	}

	/**
	 * Returns the extrapolation method in strike direction.
	 *
	 * @return The strike extrapolation method.
	 */
	public ExtrapolationMethod getStrikeExtrapolationMethod() {
		return strikeExtrapolationMethod;
	}

	/**
	 * Returns the interpolation method in maturity direction.
	 *
	 * @return The maturity interpolation method.
	 */
	public InterpolationMethod getMaturityInterpolationMethod() {
		return maturityInterpolationMethod;
	}

	/**
	 * Returns the extrapolation method in maturity direction.
	 *
	 * @return The maturity extrapolation method.
	 */
	public ExtrapolationMethod getMaturityExtrapolationMethod() {
		return maturityExtrapolationMethod;
	}

	private double convertFromSurfaceConvention(
			final double maturity,
			final double strike,
			final double value,
			final QuotingConvention targetConvention) {

		final QuotingConvention sourceConvention = getQuotingConvention();

		if (sourceConvention.equals(targetConvention)) {
			return value;
		}

		final double forward = getEquityForwardCurve().getValue(maturity);
		final double discountFactor = getDiscountCurve().getValue(maturity);

		if (sourceConvention == QuotingConvention.VOLATILITYLOGNORMAL
				&& targetConvention == QuotingConvention.PRICE) {

			return AnalyticFormulas.blackScholesGeneralizedOptionValue(
					forward,
					value,
					maturity,
					strike,
					discountFactor
			);
		}

		if (sourceConvention == QuotingConvention.PRICE
				&& targetConvention == QuotingConvention.VOLATILITYLOGNORMAL) {

			return AnalyticFormulas.blackScholesOptionImpliedVolatility(
					forward,
					maturity,
					strike,
					discountFactor,
					value
			);
		}

		if (sourceConvention == QuotingConvention.VOLATILITYNORMAL
				&& targetConvention == QuotingConvention.PRICE) {

			return AnalyticFormulas.bachelierOptionValue(
					forward,
					value,
					maturity,
					strike,
					discountFactor
			);
		}

		if (sourceConvention == QuotingConvention.PRICE
				&& targetConvention == QuotingConvention.VOLATILITYNORMAL) {

			return AnalyticFormulas.bachelierOptionImpliedVolatility(
					forward,
					maturity,
					strike,
					discountFactor,
					value
			);
		}

		if (sourceConvention == QuotingConvention.VOLATILITYLOGNORMAL
				&& targetConvention == QuotingConvention.VOLATILITYNORMAL) {

			final double price = AnalyticFormulas.blackScholesGeneralizedOptionValue(
					forward,
					value,
					maturity,
					strike,
					discountFactor
			);

			return AnalyticFormulas.bachelierOptionImpliedVolatility(
					forward,
					maturity,
					strike,
					discountFactor,
					price
			);
		}

		if (sourceConvention == QuotingConvention.VOLATILITYNORMAL
				&& targetConvention == QuotingConvention.VOLATILITYLOGNORMAL) {

			final double price = AnalyticFormulas.bachelierOptionValue(
					forward,
					value,
					maturity,
					strike,
					discountFactor
			);

			return AnalyticFormulas.blackScholesOptionImpliedVolatility(
					forward,
					maturity,
					strike,
					discountFactor,
					price
			);
		}

		throw new IllegalArgumentException(
				"Unsupported quoting convention conversion from "
						+ sourceConvention + " to " + targetConvention
		);
	}

	private SmileInterpolator[] buildSmileInterpolators(final OptionSmileData[] smiles) {

		final SmileInterpolator[] interpolators = new SmileInterpolator[smiles.length];

		for (int maturityIndex = 0; maturityIndex < smiles.length; maturityIndex++) {

			final OptionSmileData smile = smiles[maturityIndex];
			final double[] strikes = smile.getStrikes().clone();
			final double[] values = new double[strikes.length];

			for (int strikeIndex = 0; strikeIndex < strikes.length; strikeIndex++) {
				values[strikeIndex] = smile.getSmile().get(strikes[strikeIndex]).getValue();
			}

			interpolators[maturityIndex] =
					new SmileInterpolator(
							strikes,
							values,
							strikeInterpolationMethod,
							strikeExtrapolationMethod
					);
		}

		return interpolators;
	}

	private static PreparedSurface prepareFromOptionQuotes(
			final OptionData[] optionQuotes,
			final boolean sort) {

		checkOptionQuotes(optionQuotes);

		final OptionData[] quotes = optionQuotes.clone();

		if (sort) {
			Arrays.sort(quotes, OPTION_QUOTE_COMPARATOR);
		} else {
			checkOptionQuotesSorted(quotes);
		}

		checkOptionQuotesConsistencyAndDuplicates(quotes);

		final List<OptionSmileData> smiles = new ArrayList<>();

		int startIndex = 0;

		while (startIndex < quotes.length) {

			final double maturity = quotes[startIndex].getMaturity();
			int endIndex = startIndex + 1;

			while (endIndex < quotes.length
					&& Double.compare(quotes[endIndex].getMaturity(), maturity) == 0) {
				endIndex++;
			}

			final int numberOfQuotesInSmile = endIndex - startIndex;
			final double[] strikes = new double[numberOfQuotesInSmile];
			final double[] values = new double[numberOfQuotesInSmile];

			for (int quoteIndex = startIndex; quoteIndex < endIndex; quoteIndex++) {
				final int smileIndex = quoteIndex - startIndex;
				strikes[smileIndex] = quotes[quoteIndex].getStrike();
				values[smileIndex] = quotes[quoteIndex].getValue();
			}

			smiles.add(
					new OptionSmileData(
							quotes[startIndex].getUnderlying(),
							quotes[startIndex].getReferenceDate(),
							strikes,
							maturity,
							values,
							quotes[startIndex].getConvention()
					)
			);

			startIndex = endIndex;
		}

		return prepareFromSmiles(smiles.toArray(new OptionSmileData[0]), false);
	}

	private static PreparedSurface prepareFromSmiles(
			final OptionSmileData[] inputSmiles,
			final boolean sort) {

		checkSmiles(inputSmiles);

		final OptionSmileData[] smiles = inputSmiles.clone();

		if (sort) {
			Arrays.sort(smiles, OPTION_SMILE_COMPARATOR);
		} else {
			checkSmilesSorted(smiles);
		}

		checkSmilesConsistencyAndDuplicates(smiles);

		final OptionSmileData[] normalizedSmiles = new OptionSmileData[smiles.length];

		for (int maturityIndex = 0; maturityIndex < smiles.length; maturityIndex++) {
			normalizedSmiles[maturityIndex] = normalizeSmile(smiles[maturityIndex], sort);
		}

		checkSmilesConsistencyAndDuplicates(normalizedSmiles);

		return flatten(normalizedSmiles);
	}

	private static OptionSmileData normalizeSmile(
			final OptionSmileData smile,
			final boolean sort) {

		final double[] strikes = smile.getStrikes().clone();

		if (sort) {
			Arrays.sort(strikes);
		} else {
			checkStrictlyIncreasing(strikes, "strikes");
		}

		checkStrictlyIncreasing(strikes, "strikes");

		final double[] values = new double[strikes.length];

		for (int strikeIndex = 0; strikeIndex < strikes.length; strikeIndex++) {

			final OptionData option = smile.getSmile().get(strikes[strikeIndex]);

			if (option == null) {
				throw new IllegalArgumentException(
						"Smile for maturity " + smile.getMaturity()
								+ " does not contain a quote for strike " + strikes[strikeIndex]
				);
			}

			values[strikeIndex] = option.getValue();
		}

		return new OptionSmileData(
				smile.getUnderlying(),
				smile.getReferenceDate(),
				strikes,
				smile.getMaturity(),
				values,
				smile.getSmile().get(strikes[0]).getConvention()
		);
	}

	private static PreparedSurface flatten(final OptionSmileData[] smiles) {

		int numberOfNodes = 0;

		for (final OptionSmileData smile : smiles) {
			numberOfNodes += smile.getStrikes().length;
		}

		if (numberOfNodes < 2) {
			throw new IllegalArgumentException("The surface must contain at least two option quotes.");
		}

		final double[] nodeMaturities = new double[numberOfNodes];
		final double[] nodeStrikes = new double[numberOfNodes];
		final double[] nodeValues = new double[numberOfNodes];
		final double[] uniqueMaturities = new double[smiles.length];

		int nodeIndex = 0;

		for (int maturityIndex = 0; maturityIndex < smiles.length; maturityIndex++) {

			final OptionSmileData smile = smiles[maturityIndex];
			final double maturity = smile.getMaturity();
			final double[] strikes = smile.getStrikes();

			uniqueMaturities[maturityIndex] = maturity;

			for (final double strike : strikes) {
				nodeMaturities[nodeIndex] = maturity;
				nodeStrikes[nodeIndex] = strike;
				nodeValues[nodeIndex] = smile.getSmile().get(strike).getValue();
				nodeIndex++;
			}
		}

		return new PreparedSurface(
				smiles,
				nodeMaturities,
				nodeStrikes,
				nodeValues,
				uniqueMaturities
		);
	}

	private static void checkOptionQuotes(final OptionData[] optionQuotes) {

		if (optionQuotes == null || optionQuotes.length == 0) {
			throw new IllegalArgumentException("The option quote array must not be null or empty.");
		}

		for (int quoteIndex = 0; quoteIndex < optionQuotes.length; quoteIndex++) {

			final OptionData quote = optionQuotes[quoteIndex];

			if (quote == null) {
				throw new IllegalArgumentException("The option quote at index " + quoteIndex + " is null.");
			}

			checkFinite(quote.getMaturity(), "maturity");
			checkFinite(quote.getStrike(), "strike");
			checkFinite(quote.getValue(), "value");

			if (quote.getMaturity() < 0.0) {
				throw new IllegalArgumentException("Option maturity must be non-negative.");
			}

			if (quote.getStrike() <= 0.0) {
				throw new IllegalArgumentException("Option strike must be positive.");
			}
		}
	}

	private static void checkOptionQuotesSorted(final OptionData[] quotes) {

		for (int quoteIndex = 1; quoteIndex < quotes.length; quoteIndex++) {

			if (OPTION_QUOTE_COMPARATOR.compare(quotes[quoteIndex - 1], quotes[quoteIndex]) > 0) {
				throw new IllegalArgumentException(
						"Option quotes must be sorted by maturity and strike. "
								+ "Use ofUnsorted(...) for unsorted input."
				);
			}
		}
	}

	private static void checkOptionQuotesConsistencyAndDuplicates(final OptionData[] quotes) {

		final OptionData firstQuote = quotes[0];

		for (int quoteIndex = 0; quoteIndex < quotes.length; quoteIndex++) {

			final OptionData quote = quotes[quoteIndex];

			if (!Objects.equals(quote.getUnderlying(), firstQuote.getUnderlying())) {
				throw new IllegalArgumentException("All option quotes must have the same underlying.");
			}

			if (!Objects.equals(quote.getReferenceDate(), firstQuote.getReferenceDate())) {
				throw new IllegalArgumentException("All option quotes must have the same reference date.");
			}

			if (!Objects.equals(quote.getConvention(), firstQuote.getConvention())) {
				throw new IllegalArgumentException("All option quotes must have the same quoting convention.");
			}

			if (quoteIndex > 0
					&& Double.compare(quotes[quoteIndex - 1].getMaturity(), quote.getMaturity()) == 0
					&& Double.compare(quotes[quoteIndex - 1].getStrike(), quote.getStrike()) == 0) {

				throw new IllegalArgumentException(
						"Duplicate option quote for maturity "
								+ quote.getMaturity()
								+ " and strike "
								+ quote.getStrike()
				);
			}
		}
	}

	private static void checkSmiles(final OptionSmileData[] smiles) {

		if (smiles == null || smiles.length == 0) {
			throw new IllegalArgumentException("The option smile array must not be null or empty.");
		}

		for (int smileIndex = 0; smileIndex < smiles.length; smileIndex++) {

			final OptionSmileData smile = smiles[smileIndex];

			if (smile == null) {
				throw new IllegalArgumentException("The option smile at index " + smileIndex + " is null.");
			}

			checkFinite(smile.getMaturity(), "maturity");

			if (smile.getMaturity() < 0.0) {
				throw new IllegalArgumentException("Option maturity must be non-negative.");
			}

			final double[] strikes = smile.getStrikes();

			if (strikes == null || strikes.length == 0) {
				throw new IllegalArgumentException("Each smile must contain at least one strike.");
			}

			for (final double strike : strikes) {
				checkFinite(strike, "strike");

				if (strike <= 0.0) {
					throw new IllegalArgumentException("Option strike must be positive.");
				}

				final OptionData option = smile.getSmile().get(strike);

				if (option == null) {
					throw new IllegalArgumentException(
							"Smile for maturity " + smile.getMaturity()
									+ " does not contain a quote for strike " + strike
					);
				}

				checkFinite(option.getValue(), "value");
			}
		}
	}

	private static void checkSmilesSorted(final OptionSmileData[] smiles) {

		for (int smileIndex = 1; smileIndex < smiles.length; smileIndex++) {

			if (smiles[smileIndex].getMaturity() < smiles[smileIndex - 1].getMaturity()) {
				throw new IllegalArgumentException(
						"Option smiles must be sorted by maturity. "
								+ "Use ofUnsorted(...) for unsorted input."
				);
			}
		}

		for (final OptionSmileData smile : smiles) {
			checkStrictlyIncreasing(smile.getStrikes(), "strikes");
		}
	}

	private static void checkSmilesConsistencyAndDuplicates(final OptionSmileData[] smiles) {

		final OptionSmileData firstSmile = smiles[0];
		final String underlying = firstSmile.getUnderlying();
		final LocalDate referenceDate = firstSmile.getReferenceDate();
		final QuotingConvention convention = firstSmile.getSmile().get(firstSmile.getStrikes()[0]).getConvention();

		for (int smileIndex = 0; smileIndex < smiles.length; smileIndex++) {

			final OptionSmileData smile = smiles[smileIndex];

			if (!Objects.equals(smile.getUnderlying(), underlying)) {
				throw new IllegalArgumentException("All smiles must have the same underlying.");
			}

			if (!Objects.equals(smile.getReferenceDate(), referenceDate)) {
				throw new IllegalArgumentException("All smiles must have the same reference date.");
			}

			if (smileIndex > 0
					&& Double.compare(smiles[smileIndex - 1].getMaturity(), smile.getMaturity()) == 0) {

				throw new IllegalArgumentException(
						"Duplicate option smile for maturity " + smile.getMaturity()
				);
			}

			final double[] strikes = smile.getStrikes();

			checkStrictlyIncreasing(strikes, "strikes");

			for (final double strike : strikes) {

				final OptionData option = smile.getSmile().get(strike);

				if (!Objects.equals(option.getConvention(), convention)) {
					throw new IllegalArgumentException("All smiles must have the same quoting convention.");
				}

				if (!Objects.equals(option.getUnderlying(), underlying)) {
					throw new IllegalArgumentException("All option quotes inside smiles must have the same underlying.");
				}

				if (!Objects.equals(option.getReferenceDate(), referenceDate)) {
					throw new IllegalArgumentException("All option quotes inside smiles must have the same reference date.");
				}

				if (Double.compare(option.getMaturity(), smile.getMaturity()) != 0) {
					throw new IllegalArgumentException(
							"Option quote maturity does not match smile maturity."
					);
				}
			}
		}
	}

	private static void checkStrictlyIncreasing(final double[] points, final String name) {

		if (points == null || points.length == 0) {
			throw new IllegalArgumentException(name + " must not be null or empty.");
		}

		for (int index = 1; index < points.length; index++) {
			if (points[index] <= points[index - 1]) {
				throw new IllegalArgumentException(
						name + " must be strictly increasing. Received: "
								+ Arrays.toString(points)
				);
			}
		}
	}

	private static void checkFinite(final double value, final String name) {

		if (Double.isNaN(value) || Double.isInfinite(value)) {
			throw new IllegalArgumentException(name + " must be finite.");
		}
	}

	private static final class PreparedSurface {

		/**
		 * The smiles.
		 */
		private final OptionSmileData[] smiles;
		/**
		 * The node maturities.
		 */
		private final double[] nodeMaturities;
		/**
		 * The node strikes.
		 */
		private final double[] nodeStrikes;
		/**
		 * The node values.
		 */
		private final double[] nodeValues;
		/**
		 * The unique maturities.
		 */
		private final double[] uniqueMaturities;

		private PreparedSurface(
				final OptionSmileData[] smiles,
				final double[] nodeMaturities,
				final double[] nodeStrikes,
				final double[] nodeValues,
				final double[] uniqueMaturities) {

			this.smiles = smiles;
			this.nodeMaturities = nodeMaturities;
			this.nodeStrikes = nodeStrikes;
			this.nodeValues = nodeValues;
			this.uniqueMaturities = uniqueMaturities;
		}
	}

	private static final class SmileInterpolator {

		/**
		 * The strikes.
		 */
		private final double[] strikes;
		/**
		 * The values.
		 */
		private final double[] values;
		/**
		 * The interpolation.
		 */
		private final RationalFunctionInterpolation interpolation;

		private SmileInterpolator(
				final double[] strikes,
				final double[] values,
				final InterpolationMethod interpolationMethod,
				final ExtrapolationMethod extrapolationMethod) {

			this.strikes = strikes.clone();
			this.values = values.clone();

			if (strikes.length != values.length) {
				throw new IllegalArgumentException("Number of strikes and values must agree.");
			}

			if (strikes.length == 1) {
				this.interpolation = null;
			} else {
				this.interpolation =
						new RationalFunctionInterpolation(
								this.strikes,
								this.values,
								interpolationMethod,
								extrapolationMethod
						);
			}
		}

		private double getValue(final double strike) {

			if (values.length == 1) {
				return values[0];
			}

			return interpolation.getValue(strike);
		}
	}
}
