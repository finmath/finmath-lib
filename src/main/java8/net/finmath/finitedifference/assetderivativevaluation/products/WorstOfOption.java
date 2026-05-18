package net.finmath.finitedifference.assetderivativevaluation.products;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.CallOrPut;

/**
 * Finite-difference valuation of a European option on the worst of two assets.
 *
 * <p>
 * Let {@code S1} and {@code S2} denote the two underlying assets and let
 * {@code T} be the maturity. The worst-of underlying is
 * </p>
 *
 * <p>
 * <i>
 * W(T) = min(S_1(T), S_2(T)).
 * </i>
 * </p>
 *
 * <p>
 * The payoff is
 * </p>
 *
 * <p>
 * <i>
 * \left( \omega \left( W(T) - K \right) \right)^{+},
 * </i>
 * </p>
 *
 * <p>
 * where {@code \omega = +1} for a call and {@code \omega = -1} for a put. Hence
 * the product pays
 * </p>
 *
 * <p>
 * <i>
 * \left( min(S_1(T), S_2(T)) - K \right)^{+}
 * </i>
 * </p>
 *
 * <p>
 * for a call, and
 * </p>
 *
 * <p>
 * <i>
 * \left( K - min(S_1(T), S_2(T)) \right)^{+}
 * </i>
 * </p>
 *
 * <p>
 * for a put.
 * </p>
 *
 * <p>
 * This product is genuinely non-linear in the two assets and cannot be reduced
 * to the linear-combination basket class. The current implementation requires a
 * two-dimensional model state and supports European exercise only.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class WorstOfOption implements FiniteDifferenceEquityProduct {

	/**
	 * The first underlying name.
	 */
	private final String firstUnderlyingName;
	/**
	 * The second underlying name.
	 */
	private final String secondUnderlyingName;
	/**
	 * The maturity.
	 */
	private final double maturity;
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
	 * Creates a European worst-of option for two named underlyings.
	 *
	 * @param firstUnderlyingName Name of the first underlying.
	 * @param secondUnderlyingName Name of the second underlying.
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPut Option type.
	 */
	public WorstOfOption(
			final String firstUnderlyingName,
			final String secondUnderlyingName,
			final double maturity,
			final double strike,
			final CallOrPut callOrPut) {

		if (callOrPut == null) {
			throw new IllegalArgumentException("callOrPut must not be null.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("maturity must be non-negative.");
		}

		this.firstUnderlyingName = firstUnderlyingName;
		this.secondUnderlyingName = secondUnderlyingName;
		this.maturity = maturity;
		this.strike = strike;
		this.callOrPut = callOrPut;
		this.exercise = new EuropeanExercise(maturity);
	}

	/**
	 * Creates a European worst-of option for two named underlyings.
	 *
	 * @param firstUnderlyingName Name of the first underlying.
	 * @param secondUnderlyingName Name of the second underlying.
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPutSign Payoff sign, where {@code 1.0} corresponds to a call
	 *        and {@code -1.0} corresponds to a put.
	 */
	public WorstOfOption(
			final String firstUnderlyingName,
			final String secondUnderlyingName,
			final double maturity,
			final double strike,
			final double callOrPutSign) {
		this(
				firstUnderlyingName,
				secondUnderlyingName,
				maturity,
				strike,
				mapCallOrPut(callOrPutSign)
				);
	}

	/**
	 * Creates a European worst-of option with unnamed underlyings.
	 *
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPut Option type.
	 */
	public WorstOfOption(
			final double maturity,
			final double strike,
			final CallOrPut callOrPut) {
		this(null, null, maturity, strike, callOrPut);
	}

	/**
	 * Creates a European worst-of option with unnamed underlyings.
	 *
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPutSign Payoff sign, where {@code 1.0} corresponds to a call
	 *        and {@code -1.0} corresponds to a put.
	 */
	public WorstOfOption(
			final double maturity,
			final double strike,
			final double callOrPutSign) {
		this(null, null, maturity, strike, mapCallOrPut(callOrPutSign));
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
		final double worstOfValue = Math.min(firstAssetValue, secondAssetValue);
		return Math.max(callOrPut.toInteger() * (worstOfValue - strike), 0.0);
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
					"WorstOfOption currently requires a two-dimensional space discretization.");
		}

		if (model.getInitialValue() == null || model.getInitialValue().length != 2) {
			throw new IllegalArgumentException(
					"WorstOfOption currently requires a two-dimensional model state.");
		}
	}

	/**
	 * Maps the sign convention to the option type.
	 *
	 * @param sign Option sign.
	 * @return The corresponding option type.
	 */
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
	 * Returns the name of the first underlying.
	 *
	 * @return The name of the first underlying, or {@code null} if unspecified.
	 */
	public String getFirstUnderlyingName() {
		return firstUnderlyingName;
	}

	/**
	 * Returns the name of the second underlying.
	 *
	 * @return The name of the second underlying, or {@code null} if
	 *     unspecified.
	 */
	public String getSecondUnderlyingName() {
		return secondUnderlyingName;
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
}
