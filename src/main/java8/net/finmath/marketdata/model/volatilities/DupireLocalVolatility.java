package net.finmath.marketdata.model.volatilities;

import java.util.Objects;

import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;

/**
 * Local volatility provider based on an interpolated option surface.
 *
 * <p>
 * The class supports two formulas:
 * </p>
 * <ul>
 *     <li>Dupire's formula from option prices,</li>
 * <li>Gatheral's formula from Black-Scholes implied lognormal
 * volatilities.</li>
 * </ul>
 *
 * <p>
 * The class is a concrete implementation of {@link LocalVolatility}. It is
 * intended to be framework-neutral and may be used by finite-difference models
 * and Monte Carlo models alike.
 * </p>
 *
 * <p>
 * The supplied {@link OptionSurfaceDataInterpolated} remains responsible only
 * for interpolation and quoting-convention conversion. This class is
 * responsible
 * only for computing the local volatility from that surface.
 * </p>
 *
 * <p>
 * If the formula is set to {@link LocalVolatilityFormula#AUTOMATIC}, then:
 * </p>
 * <ul>
 *     <li>{@link QuotingConvention#PRICE} uses Dupire's price formula,</li>
 * <li>{@link QuotingConvention#VOLATILITYLOGNORMAL} uses Gatheral's
 * formula.</li>
 * </ul>
 *
 * <p>
 * If a formula is selected explicitly, the constructor creates the appropriate
 * converted surface at the original nodes by using
 * {@link OptionSurfaceDataInterpolated#getCloneForQuotingConvention(QuotingConv
 * ention)}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DupireLocalVolatility implements LocalVolatility {

	/**
	 * Formula used to compute the local volatility.
	 *
	 * @author Alessandro Gnoatto
	 */
	public enum LocalVolatilityFormula {

		/**
		 * Infer the formula from the surface quoting convention.
		 */
		AUTOMATIC,

		/**
		 * Use Dupire's formula from option prices.
		 */
		DUPIRE_FROM_PRICES,

		/**
		 * Use Gatheral's formula from Black-Scholes implied lognormal
		 * volatilities.
		 */
		GATHERAL_FROM_IMPLIED_VOLATILITIES
	}

	/**
	 * Policy for negative or numerically unstable local variances.
	 *
	 * @author Alessandro Gnoatto
	 */
	public enum NegativeLocalVarianceHandling {

		/**
		 * Replace negative values by a variance floor.
		 */
		FLOOR_AT_ZERO,

		/**
		 * Take the absolute value. This reproduces the behavior of the original
		 * prototype but may hide arbitrage or interpolation issues.
		 */
		ABSOLUTE_VALUE,

		/**
		 * Throw an exception if the local variance is below the floor.
		 */
		THROW_EXCEPTION
	}

	/**
	 * The default time epsilon.
	 */
	private static final double DEFAULT_TIME_EPSILON = 1E-5;
	/**
	 * The default strike epsilon factor.
	 */
	private static final double DEFAULT_STRIKE_EPSILON_FACTOR = 1E-5;
	/**
	 * The default variance floor.
	 */
	private static final double DEFAULT_VARIANCE_FLOOR = 0.0;

	/**
	 * The quote surface.
	 */
	private final OptionSurfaceDataInterpolated quoteSurface;
	/**
	 * The formula surface.
	 */
	private final OptionSurfaceDataInterpolated formulaSurface;

	/**
	 * The requested formula.
	 */
	private final LocalVolatilityFormula requestedFormula;
	/**
	 * The effective formula.
	 */
	private final LocalVolatilityFormula effectiveFormula;

	/**
	 * The time epsilon.
	 */
	private final double timeEpsilon;
	/**
	 * The strike epsilon factor.
	 */
	private final double strikeEpsilonFactor;
	/**
	 * The variance floor.
	 */
	private final double varianceFloor;

	/**
	 * The negative local variance handling.
	 */
	private final NegativeLocalVarianceHandling negativeLocalVarianceHandling;

	/**
	 * Creates a Dupire local volatility provider using automatic formula
	 * selection.
	 *
	 * @param quoteSurface The interpolated option surface.
	 */
	public DupireLocalVolatility(final OptionSurfaceDataInterpolated quoteSurface) {
		this(
				quoteSurface,
				LocalVolatilityFormula.AUTOMATIC,
				DEFAULT_TIME_EPSILON,
				DEFAULT_STRIKE_EPSILON_FACTOR,
				DEFAULT_VARIANCE_FLOOR,
				NegativeLocalVarianceHandling.FLOOR_AT_ZERO
				);
	}

	/**
	 * Creates a Dupire local volatility provider using the selected formula.
	 *
	 * @param quoteSurface The interpolated option surface.
	 * @param formula The formula to be used.
	 */
	public DupireLocalVolatility(
			final OptionSurfaceDataInterpolated quoteSurface,
			final LocalVolatilityFormula formula) {

		this(
				quoteSurface,
				formula,
				DEFAULT_TIME_EPSILON,
				DEFAULT_STRIKE_EPSILON_FACTOR,
				DEFAULT_VARIANCE_FLOOR,
				NegativeLocalVarianceHandling.FLOOR_AT_ZERO
				);
	}

	/**
	 * Creates a Dupire local volatility provider.
	 *
	 * @param quoteSurface The interpolated option surface.
	 * @param formula The formula to be used.
	 * @param timeEpsilon The finite-difference bump in time.
	 * @param strikeEpsilonFactor The relative finite-difference bump in strike.
	 * @param varianceFloor The local variance floor.
	 * @param negativeLocalVarianceHandling The policy for negative local
	 *     variances.
	 */
	public DupireLocalVolatility(
			final OptionSurfaceDataInterpolated quoteSurface,
			final LocalVolatilityFormula formula,
			final double timeEpsilon,
			final double strikeEpsilonFactor,
			final double varianceFloor,
			final NegativeLocalVarianceHandling negativeLocalVarianceHandling) {

		this.quoteSurface = Objects.requireNonNull(quoteSurface, "quoteSurface");
		this.requestedFormula = Objects.requireNonNull(formula, "formula");
		this.effectiveFormula = resolveFormula(quoteSurface, formula);
		this.formulaSurface = createFormulaSurface(quoteSurface, effectiveFormula);

		if (timeEpsilon <= 0.0 || Double.isNaN(timeEpsilon) || Double.isInfinite(timeEpsilon)) {
			throw new IllegalArgumentException("timeEpsilon must be positive and finite.");
		}

		if (strikeEpsilonFactor <= 0.0
				|| Double.isNaN(strikeEpsilonFactor)
				|| Double.isInfinite(strikeEpsilonFactor)) {
			throw new IllegalArgumentException("strikeEpsilonFactor must be positive and finite.");
		}

		if (varianceFloor < 0.0 || Double.isNaN(varianceFloor) || Double.isInfinite(varianceFloor)) {
			throw new IllegalArgumentException("varianceFloor must be non-negative and finite.");
		}

		this.timeEpsilon = timeEpsilon;
		this.strikeEpsilonFactor = strikeEpsilonFactor;
		this.varianceFloor = varianceFloor;
		this.negativeLocalVarianceHandling =
				Objects.requireNonNull(negativeLocalVarianceHandling, "negativeLocalVarianceHandling");
	}

	@Override
	public double getValue(final double time, final double assetValue) {

		if (assetValue <= 0.0) {
			return 0.0;
		}

		final double localVariance;

		if (effectiveFormula == LocalVolatilityFormula.DUPIRE_FROM_PRICES) {
			localVariance = getLocalVarianceFromOptionPrices(time, assetValue);
		} else if (effectiveFormula == LocalVolatilityFormula.GATHERAL_FROM_IMPLIED_VOLATILITIES) {
			localVariance = getLocalVarianceFromImpliedVolatilities(time, assetValue);
		} else {
			throw new IllegalStateException("Unresolved local volatility formula.");
		}

		return Math.sqrt(handleLocalVariance(localVariance, time, assetValue));
	}

	/**
	 * Returns the local variance from Dupire's formula applied to option
	 * prices.
	 *
	 * @param time The option maturity.
	 * @param strike The strike, interpreted as the asset value at which local
	 *     volatility is evaluated.
	 * @return The local variance.
	 */
	public double getLocalVarianceFromOptionPrices(final double time, final double strike) {

		final double t = makeTimeSafe(time);
		final double k = makeStrikeSafe(strike);

		final double price = formulaSurface.getValue(t, k, QuotingConvention.PRICE);

		final double r = getRiskFreeRate(t);
		final double q = getDividendYieldRate(t);

		final double dT = getFirstTimeDerivative(formulaSurface, t, k, QuotingConvention.PRICE, price);
		final double dK = getFirstStrikeDerivative(formulaSurface, t, k, QuotingConvention.PRICE, price);
		final double dKK = getSecondStrikeDerivative(formulaSurface, t, k, QuotingConvention.PRICE, price);

		final double numerator = 2.0 * (dT + q * price + (r - q) * k * dK);
		final double denominator = k * k * dKK;

		if (Math.abs(denominator) < 1E-16) {
			throw new ArithmeticException(
					"Dupire denominator is numerically zero at time "
							+ t + " and strike " + k + "."
					);
		}

		return numerator / denominator;
	}

	/**
	 * Returns the local volatility from Dupire's formula applied to option
	 * prices.
	 *
	 * @param time The option maturity.
	 * @param strike The strike, interpreted as the asset value at which local
	 *     volatility is evaluated.
	 * @return The local volatility.
	 */
	public double getLocalVolatilityFromOptionPrices(final double time, final double strike) {
		return Math.sqrt(handleLocalVariance(getLocalVarianceFromOptionPrices(time, strike), time, strike));
	}

	/**
	 * Returns the local variance from Gatheral's implied-volatility formula.
	 *
	 * @param time The option maturity.
	 * @param strike The strike, interpreted as the asset value at which local
	 *     volatility is evaluated.
	 * @return The local variance.
	 */
	public double getLocalVarianceFromImpliedVolatilities(final double time, final double strike) {

		final double t = makeTimeSafe(time);
		final double k = makeStrikeSafe(strike);

		final double volatility =
				formulaSurface.getValue(t, k, QuotingConvention.VOLATILITYLOGNORMAL);

		if (volatility <= 0.0) {
			return 0.0;
		}

		final double forward = getForward(t);
		final double drift = Math.log(forward / getSpot()) / t;

		final double dT = getFirstTimeDerivative(
				formulaSurface,
				t,
				k,
				QuotingConvention.VOLATILITYLOGNORMAL,
				volatility
				);

		final double dK = getFirstStrikeDerivative(
				formulaSurface,
				t,
				k,
				QuotingConvention.VOLATILITYLOGNORMAL,
				volatility
				);

		final double dKK = getSecondStrikeDerivative(
				formulaSurface,
				t,
				k,
				QuotingConvention.VOLATILITYLOGNORMAL,
				volatility
				);

		final double h1 =
				(Math.log(getSpot() / k) + (drift + 0.5 * volatility * volatility) * t)
				/ volatility;

		final double h2 = h1 - volatility * t;

		final double numerator =
				volatility * volatility
				+ 2.0 * volatility * t * (dT + drift * k * dK);

		final double denominator =
				1.0
				+ 2.0 * h1 * k * dK
				+ k * k * (h1 * h2 * dK * dK + t * volatility * dKK);

		if (Math.abs(denominator) < 1E-16) {
			throw new ArithmeticException(
					"Gatheral denominator is numerically zero at time "
							+ t + " and strike " + k + "."
					);
		}

		return numerator / denominator;
	}

	/**
	 * Returns the local volatility from Gatheral's implied-volatility formula.
	 *
	 * @param time The option maturity.
	 * @param strike The strike, interpreted as the asset value at which local
	 *     volatility is evaluated.
	 * @return The local volatility.
	 */
	public double getLocalVolatilityFromImpliedVolatilities(final double time, final double strike) {
		return Math.sqrt(handleLocalVariance(getLocalVarianceFromImpliedVolatilities(time, strike), time, strike));
	}

	/**
	 * Returns the input quote surface.
	 *
	 * @return The input quote surface.
	 */
	public OptionSurfaceDataInterpolated getQuoteSurface() {
		return quoteSurface;
	}

	/**
	 * Returns the surface actually used by the formula.
	 * <p>
	 * For Dupire from prices, this is a price surface. For Gatheral, this is a
	 * lognormal implied-volatility surface.
	 * </p>
	 *
	 * @return The formula surface.
	 */
	public OptionSurfaceDataInterpolated getFormulaSurface() {
		return formulaSurface;
	}

	/**
	 * Returns the requested formula.
	 *
	 * @return The requested formula.
	 */
	public LocalVolatilityFormula getRequestedFormula() {
		return requestedFormula;
	}

	/**
	 * Returns the effective formula.
	 *
	 * @return The effective formula.
	 */
	public LocalVolatilityFormula getEffectiveFormula() {
		return effectiveFormula;
	}

	/**
	 * Returns the finite-difference bump in time.
	 *
	 * @return The time bump.
	 */
	public double getTimeEpsilon() {
		return timeEpsilon;
	}

	/**
	 * Returns the relative finite-difference bump in strike.
	 *
	 * @return The strike bump factor.
	 */
	public double getStrikeEpsilonFactor() {
		return strikeEpsilonFactor;
	}

	/**
	 * Returns the local variance floor.
	 *
	 * @return The variance floor.
	 */
	public double getVarianceFloor() {
		return varianceFloor;
	}

	/**
	 * Returns the negative local variance handling policy.
	 *
	 * @return The policy.
	 */
	public NegativeLocalVarianceHandling getNegativeLocalVarianceHandling() {
		return negativeLocalVarianceHandling;
	}

	private static LocalVolatilityFormula resolveFormula(
			final OptionSurfaceDataInterpolated surface,
			final LocalVolatilityFormula formula) {

		if (formula != LocalVolatilityFormula.AUTOMATIC) {
			return formula;
		}

		if (surface.getQuotingConvention() == QuotingConvention.PRICE) {
			return LocalVolatilityFormula.DUPIRE_FROM_PRICES;
		}

		if (surface.getQuotingConvention() == QuotingConvention.VOLATILITYLOGNORMAL) {
			return LocalVolatilityFormula.GATHERAL_FROM_IMPLIED_VOLATILITIES;
		}

		throw new IllegalArgumentException(
				"Automatic local volatility formula selection supports only PRICE "
						+ "and VOLATILITYLOGNORMAL surfaces."
				);
	}

	private static OptionSurfaceDataInterpolated createFormulaSurface(
			final OptionSurfaceDataInterpolated surface,
			final LocalVolatilityFormula formula) {

		if (formula == LocalVolatilityFormula.DUPIRE_FROM_PRICES) {
			return surface.getQuotingConvention() == QuotingConvention.PRICE
					? surface
							: surface.getCloneForQuotingConvention(QuotingConvention.PRICE);
		}

		if (formula == LocalVolatilityFormula.GATHERAL_FROM_IMPLIED_VOLATILITIES) {
			return surface.getQuotingConvention() == QuotingConvention.VOLATILITYLOGNORMAL
					? surface
							: surface.getCloneForQuotingConvention(QuotingConvention.VOLATILITYLOGNORMAL);
		}

		throw new IllegalArgumentException("Unsupported formula: " + formula);
	}

	private double getFirstTimeDerivative(
			final OptionSurfaceDataInterpolated surface,
			final double time,
			final double strike,
			final QuotingConvention quotingConvention,
			final double mid) {

		if (time <= timeEpsilon) {
			final double up = surface.getValue(time + timeEpsilon, strike, quotingConvention);
			return (up - mid) / timeEpsilon;
		}

		final double up = surface.getValue(time + timeEpsilon, strike, quotingConvention);
		final double down = surface.getValue(time - timeEpsilon, strike, quotingConvention);

		return (up - down) / (2.0 * timeEpsilon);
	}

	private double getFirstStrikeDerivative(
			final OptionSurfaceDataInterpolated surface,
			final double time,
			final double strike,
			final QuotingConvention quotingConvention,
			final double mid) {

		final double eps = getStrikeEpsilon();

		if (strike <= eps) {
			final double up = surface.getValue(time, strike + eps, quotingConvention);
			return (up - mid) / eps;
		}

		final double up = surface.getValue(time, strike + eps, quotingConvention);
		final double down = surface.getValue(time, strike - eps, quotingConvention);

		return (up - down) / (2.0 * eps);
	}

	private double getSecondStrikeDerivative(
			final OptionSurfaceDataInterpolated surface,
			final double time,
			final double strike,
			final QuotingConvention quotingConvention,
			final double mid) {

		final double eps = getStrikeEpsilon();

		final double center;
		final double offset;

		if (strike <= eps) {
			offset = eps - strike;
			center = surface.getValue(time, strike + offset, quotingConvention);
		} else {
			offset = 0.0;
			center = mid;
		}

		final double up = surface.getValue(time, strike + eps + offset, quotingConvention);
		final double down = surface.getValue(time, strike - eps + offset, quotingConvention);

		return (up + down - 2.0 * center) / (eps * eps);
	}

	private double handleLocalVariance(
			final double localVariance,
			final double time,
			final double strike) {

		if (Double.isNaN(localVariance) || Double.isInfinite(localVariance)) {
			throw new ArithmeticException(
					"Local variance is not finite at time "
							+ time + " and strike " + strike + "."
					);
		}

		if (localVariance >= varianceFloor) {
			return localVariance;
		}

		if (negativeLocalVarianceHandling == NegativeLocalVarianceHandling.FLOOR_AT_ZERO) {
			return varianceFloor;
		}

		if (negativeLocalVarianceHandling == NegativeLocalVarianceHandling.ABSOLUTE_VALUE) {
			return Math.max(Math.abs(localVariance), varianceFloor);
		}

		throw new ArithmeticException(
				"Negative local variance " + localVariance
				+ " at time " + time
				+ " and strike " + strike + "."
				);
	}

	private double makeTimeSafe(final double time) {

		if (Double.isNaN(time) || Double.isInfinite(time)) {
			throw new IllegalArgumentException("time must be finite.");
		}

		return Math.max(time, timeEpsilon);
	}

	private double makeStrikeSafe(final double strike) {

		if (Double.isNaN(strike) || Double.isInfinite(strike)) {
			throw new IllegalArgumentException("strike must be finite.");
		}

		return Math.max(strike, getStrikeEpsilon());
	}

	private double getStrikeEpsilon() {
		return Math.max(Math.abs(getSpot()) * strikeEpsilonFactor, strikeEpsilonFactor);
	}

	private double getSpot() {
		return formulaSurface.getEquityForwardCurve().getValue(0.0);
	}

	private double getForward(final double time) {
		return formulaSurface.getEquityForwardCurve().getValue(time);
	}

	private double getDiscountFactor(final double time) {
		return formulaSurface.getDiscountCurve().getDiscountFactor(time);
	}

	private double getRiskFreeRate(final double time) {
		return -Math.log(getDiscountFactor(time)) / time;
	}

	private double getDividendYieldRate(final double time) {

		final double spot = getSpot();
		final double forward = getForward(time);
		final double riskFreeRate = getRiskFreeRate(time);

		return riskFreeRate - Math.log(forward / spot) / time;
	}
}
