package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.Arrays;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.CallOrPut;

/**
 * Finite-difference valuation of a European linear basket option on two assets.
 *
 * <p>
 * Let {@code S1} and {@code S2} denote the two underlying assets, let
 * {@code q1} and {@code q2} be the signed asset quantities, let {@code K} be
 * the strike, and let {@code \omega \in \{+1,-1\}} denote the call/put sign.
 * The payoff is
 * </p>
 *
 * <p>
 * <i>
 * \left( \omega \left( q_1 S_1(T) + q_2 S_2(T) - K \right) \right)^{+}.
 * </i>
 * </p>
 *
 * <p>
 * This single class covers a number of important special cases:
 * </p>
 * <ul>
 *   <li>arithmetic basket options, when both quantities are positive,</li>
 *   <li>spread options, for example with quantities {@code (1,-1)},</li>
 * <li>exchange options, for example with quantities {@code (1,-1)} and strike
 * {@code 0}.</li>
 * </ul>
 *
 * <p>
 * The class is written in a dimension-agnostic style through the quantity
 * vector,
 * but the current finite-difference implementation supports only the two-asset
 * case, that is, exactly two quantities and a two-dimensional model state.
 * </p>
 *
 * <p>
 * The current implementation supports only European exercise.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class BasketOption implements FiniteDifferenceEquityProduct {

	/**
	 * The underlying names.
	 */
	private final String[] underlyingNames;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The quantities.
	 */
	private final double[] quantities;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The call or put.
	 */
	private final CallOrPut callOrPut;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	/**
	 * Creates a European linear basket option.
	 *
	 * @param underlyingNames Names of the underlyings. May be {@code null}. If
	 *        provided, the array length must match the basket dimension.
	 * @param maturity Maturity {@code T}.
	 * @param quantities Signed asset quantities.
	 * @param strike Real strike {@code K}.
	 * @param callOrPut Option type.
	 */
	public BasketOption(
			final String[] underlyingNames,
			final double maturity,
			final double[] quantities,
			final double strike,
			final CallOrPut callOrPut) {

		if (quantities == null || quantities.length == 0) {
			throw new IllegalArgumentException("quantities must contain at least one entry.");
		}
		if (underlyingNames != null && underlyingNames.length != quantities.length) {
			throw new IllegalArgumentException(
					"underlyingNames must have the same length as quantities.");
		}
		if (callOrPut == null) {
			throw new IllegalArgumentException("callOrPut must not be null.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("maturity must be non-negative.");
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

		this.underlyingNames = underlyingNames != null ? underlyingNames.clone() : null;
		this.maturity = maturity;
		this.quantities = quantities.clone();
		this.strike = strike;
		this.callOrPut = callOrPut;
		this.exercise = new EuropeanExercise(maturity);
	}

	/**
	 * Creates a European linear basket option.
	 *
	 * @param maturity Maturity {@code T}.
	 * @param quantities Signed asset quantities.
	 * @param strike Real strike {@code K}.
	 * @param callOrPut Option type.
	 */
	public BasketOption(
			final double maturity,
			final double[] quantities,
			final double strike,
			final CallOrPut callOrPut) {
		this(null, maturity, quantities, strike, callOrPut);
	}

	/**
	 * Creates a European linear basket option.
	 *
	 * @param underlyingNames Names of the underlyings. May be {@code null}. If
	 *        provided, the array length must match the basket dimension.
	 * @param maturity Maturity {@code T}.
	 * @param quantities Signed asset quantities.
	 * @param strike Real strike {@code K}.
	 * @param callOrPutSign Payoff sign, where {@code 1.0} corresponds to a call
	 *        and {@code -1.0} corresponds to a put.
	 */
	public BasketOption(
			final String[] underlyingNames,
			final double maturity,
			final double[] quantities,
			final double strike,
			final double callOrPutSign) {
		this(
				underlyingNames,
				maturity,
				quantities,
				strike,
				mapCallOrPut(callOrPutSign)
				);
	}

	/**
	 * Creates a European linear basket option.
	 *
	 * @param maturity Maturity {@code T}.
	 * @param quantities Signed asset quantities.
	 * @param strike Real strike {@code K}.
	 * @param callOrPutSign Payoff sign, where {@code 1.0} corresponds to a call
	 *        and {@code -1.0} corresponds to a put.
	 */
	public BasketOption(
			final double maturity,
			final double[] quantities,
			final double strike,
			final double callOrPutSign) {
		this(null, maturity, quantities, strike, mapCallOrPut(callOrPutSign));
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

	private double terminalPayoff(final double firstAssetValue, final double secondAssetValue) {
		final double basketValue =
				quantities[0] * firstAssetValue + quantities[1] * secondAssetValue - strike;

		return Math.max(callOrPut.toInteger() * basketValue, 0.0);
	}

	private void validateModel(final FiniteDifferenceEquityModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}

		if (quantities.length != 2) {
			throw new IllegalArgumentException(
					"BasketOption currently supports only the two-asset case.");
		}

		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 2) {
			throw new IllegalArgumentException(
					"BasketOption currently requires a two-dimensional space discretization.");
		}

		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"BasketOption currently requires a two-dimensional model state.");
		}
	}

	private static CallOrPut mapCallOrPut(final double sign) {
		if (sign == 1.0) {
			return CallOrPut.CALL;
		}
		if (sign == -1.0) {
			return CallOrPut.PUT;
		}
		throw new IllegalArgumentException("Unknown option type.");
	}

	/**
	 * Returns the underlying names.
	 *
	 * @return The underlying names, or {@code null} if unspecified.
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
	 * Returns the signed asset quantities.
	 *
	 * @return The signed asset quantities.
	 */
	public double[] getQuantities() {
		return quantities.clone();
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
		return "BasketOption [maturity=" + maturity
				+ ", strike=" + strike
				+ ", callOrPut=" + callOrPut
				+ ", quantities=" + Arrays.toString(quantities)
				+ "]";
	}
}
