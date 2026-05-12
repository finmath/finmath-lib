package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.Arrays;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.CallOrPut;

/**
 * Finite-difference valuation of a European cash-or-nothing digital option on
 * two assets.
 *
 * <p>
 * The class unifies three practically relevant two-asset digital payoffs:
 * </p>
 *
 * <ul>
 *   <li>linear-combination digital,</li>
 *   <li>best-of digital,</li>
 *   <li>worst-of digital.</li>
 * </ul>
 *
 * <p>
 * Let {@code S1} and {@code S2} denote the two asset values at maturity
 * {@code T}, let {@code K} be the strike, let {@code C &gt;= 0} be the cash
 * payoff, and let {@code \omega \in \{+1,-1\}} denote the call/put sign.
 * </p>
 *
 * <p>
 * In {@link BasketDigitalType#LINEAR_COMBINATION} mode, with signed quantities
 * {@code q1} and {@code q2}, the payoff is
 * </p>
 *
 * <p>
 * <i>
 * C \mathbf{1}_{\{\omega (q_1 S_1(T) + q_2 S_2(T) - K) &gt; 0\}}.
 * </i>
 * </p>
 *
 * <p>
 * In {@link BasketDigitalType#BEST_OF} mode, the payoff is
 * </p>
 *
 * <p>
 * <i>
 * C \mathbf{1}_{\{\omega (\max(S_1(T),S_2(T)) - K) &gt; 0\}}.
 * </i>
 * </p>
 *
 * <p>
 * In {@link BasketDigitalType#WORST_OF} mode, the payoff is
 * </p>
 *
 * <p>
 * <i>
 * C \mathbf{1}_{\{\omega (\min(S_1(T),S_2(T)) - K) &gt; 0\}}.
 * </i>
 * </p>
 *
 * <p>
 * The current implementation supports only European exercise and requires a
 * two-dimensional model state.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DigitalBasketOption implements FiniteDifferenceEquityProduct {

	/**
	 * Type of two-asset digital payoff.
	 */
	public enum BasketDigitalType {
		/**
		 * The linear combination.
		 */
		LINEAR_COMBINATION,
		/**
		 * The best of.
		 */
		BEST_OF,
		/**
		 * The worst of.
		 */
		WORST_OF
	}

	/**
	 * The underlying names.
	 */
	private final String[] underlyingNames;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The basket digital type.
	 */
	private final BasketDigitalType basketDigitalType;
	/**
	 * The quantities.
	 */
	private final double[] quantities;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The cash payoff.
	 */
	private final double cashPayoff;
	/**
	 * The call or put.
	 */
	private final CallOrPut callOrPut;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	/**
	 * Creates a digital basket option.
	 *
	 * @param underlyingNames Names of the underlyings. May be {@code null}. If
	 *        provided, the array length must be 2.
	 * @param maturity Maturity.
	 * @param basketDigitalType Payoff type.
	 * @param quantities Signed quantities for
	 * {@link BasketDigitalType#LINEAR_COMBINATION}. Ignored otherwise and
	 *        may be {@code null}.
	 * @param strike Strike.
	 * @param cashPayoff Cash payoff.
	 * @param callOrPut Option type.
	 */
	public DigitalBasketOption(
			final String[] underlyingNames,
			final double maturity,
			final BasketDigitalType basketDigitalType,
			final double[] quantities,
			final double strike,
			final double cashPayoff,
			final CallOrPut callOrPut) {

		if (basketDigitalType == null) {
			throw new IllegalArgumentException("basketDigitalType must not be null.");
		}
		if (callOrPut == null) {
			throw new IllegalArgumentException("callOrPut must not be null.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("maturity must be non-negative.");
		}
		if (cashPayoff < 0.0) {
			throw new IllegalArgumentException("cashPayoff must be non-negative.");
		}
		if (underlyingNames != null && underlyingNames.length != 2) {
			throw new IllegalArgumentException("underlyingNames must have length 2.");
		}

		if (basketDigitalType == BasketDigitalType.LINEAR_COMBINATION) {
			if (quantities == null || quantities.length != 2) {
				throw new IllegalArgumentException(
						"quantities must have length 2 for LINEAR_COMBINATION mode.");
			}

			boolean allZero = true;
			for (final double quantity : quantities) {
				if (Math.abs(quantity) > 0.0) {
					allZero = false;
					break;
				}
			}
			if (allZero) {
				throw new IllegalArgumentException("At least one quantity must be non-zero.");
			}
		}

		this.underlyingNames = underlyingNames != null ? underlyingNames.clone() : null;
		this.maturity = maturity;
		this.basketDigitalType = basketDigitalType;
		this.quantities = quantities != null ? quantities.clone() : null;
		this.strike = strike;
		this.cashPayoff = cashPayoff;
		this.callOrPut = callOrPut;
		this.exercise = new EuropeanExercise(maturity);
	}

	/**
	 * Creates a digital basket option.
	 *
	 * @param maturity Maturity.
	 * @param basketDigitalType Payoff type.
	 * @param quantities Signed quantities for
	 * {@link BasketDigitalType#LINEAR_COMBINATION}. Ignored otherwise and
	 *        may be {@code null}.
	 * @param strike Strike.
	 * @param cashPayoff Cash payoff.
	 * @param callOrPut Option type.
	 */
	public DigitalBasketOption(
			final double maturity,
			final BasketDigitalType basketDigitalType,
			final double[] quantities,
			final double strike,
			final double cashPayoff,
			final CallOrPut callOrPut) {
		this(null, maturity, basketDigitalType, quantities, strike, cashPayoff, callOrPut);
	}

	/**
	 * Creates a linear-combination digital option.
	 *
	 * @param underlyingNames Names of the underlyings.
	 * @param maturity Maturity.
	 * @param quantities Signed quantities.
	 * @param strike Strike.
	 * @param cashPayoff Cash payoff.
	 * @param callOrPut Option type.
	 * @return The product.
	 */
	public static DigitalBasketOption linearCombination(
			final String[] underlyingNames,
			final double maturity,
			final double[] quantities,
			final double strike,
			final double cashPayoff,
			final CallOrPut callOrPut) {
		return new DigitalBasketOption(
				underlyingNames,
				maturity,
				BasketDigitalType.LINEAR_COMBINATION,
				quantities,
				strike,
				cashPayoff,
				callOrPut
		);
	}

	/**
	 * Creates a best-of digital option.
	 *
	 * @param underlyingNames Names of the underlyings.
	 * @param maturity Maturity.
	 * @param strike Strike.
	 * @param cashPayoff Cash payoff.
	 * @param callOrPut Option type.
	 * @return The product.
	 */
	public static DigitalBasketOption bestOf(
			final String[] underlyingNames,
			final double maturity,
			final double strike,
			final double cashPayoff,
			final CallOrPut callOrPut) {
		return new DigitalBasketOption(
				underlyingNames,
				maturity,
				BasketDigitalType.BEST_OF,
				null,
				strike,
				cashPayoff,
				callOrPut
		);
	}

	/**
	 * Creates a worst-of digital option.
	 *
	 * @param underlyingNames Names of the underlyings.
	 * @param maturity Maturity.
	 * @param strike Strike.
	 * @param cashPayoff Cash payoff.
	 * @param callOrPut Option type.
	 * @return The product.
	 */
	public static DigitalBasketOption worstOf(
			final String[] underlyingNames,
			final double maturity,
			final double strike,
			final double cashPayoff,
			final CallOrPut callOrPut) {
		return new DigitalBasketOption(
				underlyingNames,
				maturity,
				BasketDigitalType.WORST_OF,
				null,
				strike,
				cashPayoff,
				callOrPut
		);
	}

	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {
		validateModel(model);

		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, exercise);
		return solver.getValue(
				evaluationTime,
				maturity,
				this::terminalPayoff
		);
	}

	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {
		validateModel(model);

		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, exercise);
		return solver.getValues(
				maturity,
				this::terminalPayoff
		);
	}

	/**
	 * Returns the terminal payoff.
	 *
	 * @param firstAssetValue Value of the first asset.
	 * @param secondAssetValue Value of the second asset.
	 * @return The terminal payoff.
	 */
	private double terminalPayoff(final double firstAssetValue, final double secondAssetValue) {
		final double underlyingValue;

		switch (basketDigitalType) {
		case LINEAR_COMBINATION:
			underlyingValue = quantities[0] * firstAssetValue + quantities[1] * secondAssetValue;
			break;
		case BEST_OF:
			underlyingValue = Math.max(firstAssetValue, secondAssetValue);
			break;
		case WORST_OF:
			underlyingValue = Math.min(firstAssetValue, secondAssetValue);
			break;
		default:
			throw new IllegalStateException("Unsupported basket digital type.");
		}

		return callOrPut.toInteger() * (underlyingValue - strike) > 0.0 ? cashPayoff : 0.0;
	}

	/**
	 * Validates model compatibility.
	 *
	 * @param model The finite-difference model.
	 */
	private void validateModel(final FiniteDifferenceEquityModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}

		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 2) {
			throw new IllegalArgumentException(
					"DigitalBasketOption currently requires a two-dimensional space discretization.");
		}

		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"DigitalBasketOption currently requires a two-dimensional model state.");
		}
	}

	/**
	 * Returns the names of the underlyings.
	 *
	 * @return The names of the underlyings, or {@code null} if unspecified.
	 */
	public String[] getUnderlyingNames() {
		return underlyingNames != null ? underlyingNames.clone() : null;
	}

	/**
	 * Returns the maturity.
	 *
	 * @return The maturity.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the payoff type.
	 *
	 * @return The payoff type.
	 */
	public BasketDigitalType getBasketDigitalType() {
		return basketDigitalType;
	}

	/**
	 * Returns the signed quantities.
	 *
	 * @return The signed quantities, or {@code null} if not applicable.
	 */
	public double[] getQuantities() {
		return quantities != null ? quantities.clone() : null;
	}

	/**
	 * Returns the strike.
	 *
	 * @return The strike.
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * Returns the cash payoff.
	 *
	 * @return The cash payoff.
	 */
	public double getCashPayoff() {
		return cashPayoff;
	}

	/**
	 * Returns the option type.
	 *
	 * @return The option type.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPut;
	}

	/**
	 * Returns the exercise specification.
	 *
	 * @return The exercise specification.
	 */
	public Exercise getExercise() {
		return exercise;
	}

	@Override
	public String toString() {
		return "DigitalBasketOption [maturity=" + maturity
				+ ", basketDigitalType=" + basketDigitalType
				+ ", quantities=" + Arrays.toString(quantities)
				+ ", strike=" + strike
				+ ", cashPayoff=" + cashPayoff
				+ ", callOrPut=" + callOrPut
				+ "]";
	}
}
