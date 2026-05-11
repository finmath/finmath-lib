package net.finmath.finitedifference.assetderivativevaluation.products;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.products.CallOrPut;

/**
 * Finite difference valuation of a European option on a single asset.
 *
 * <p>
 * Given an underlying price {@code S} with strike {@code K} and maturity {@code
 * T}, the payoff is
 * </p>
 * <p>
 * {@code max(sign * (S(T) - K), 0)},
 * </p>
 * <p>
 * where {@code sign} corresponds to {@link CallOrPut#CALL} or {@link
 * CallOrPut#PUT}.
 * </p>
 *
 * @author Christian Fries
 * @author Ralph Rudd
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class EuropeanOption implements FiniteDifferenceEquityProduct {

	/**
	 * The underlying name.
	 */
	private final String underlyingName;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The call or put sign.
	 */
	private final CallOrPut callOrPutSign;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	/**
	 * Creates a European option for a named underlying.
	 *
	 * @param underlyingName Name of the underlying.
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPutSign Payoff sign, where {@code 1.0} corresponds to a call
	 *     and
	 *        {@code -1.0} corresponds to a put.
	 */
	public EuropeanOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double callOrPutSign) {

		super();
		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;

		if (callOrPutSign == 1.0) {
			this.callOrPutSign = CallOrPut.CALL;
		} else if (callOrPutSign == -1.0) {
			this.callOrPutSign = CallOrPut.PUT;
		} else {
			throw new IllegalArgumentException("Unknown option type");
		}

		this.exercise = new EuropeanExercise(maturity);
	}

	/**
	 * Creates a European option for a named underlying.
	 *
	 * @param underlyingName Name of the underlying.
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPutSign Option type.
	 */
	public EuropeanOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign) {

		super();
		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
		this.callOrPutSign = callOrPutSign;
		this.exercise = new EuropeanExercise(maturity);
	}

	/**
	 * Creates a European option (single-asset case, unnamed underlying).
	 *
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPutSign Payoff sign, where {@code 1.0} corresponds to a call
	 *     and
	 *        {@code -1.0} corresponds to a put.
	 */
	public EuropeanOption(final double maturity, final double strike, final double callOrPutSign) {

		super();
		this.maturity = maturity;
		this.strike = strike;

		if (callOrPutSign == 1.0) {
			this.callOrPutSign = CallOrPut.CALL;
		} else if (callOrPutSign == -1.0) {
			this.callOrPutSign = CallOrPut.PUT;
		} else {
			throw new IllegalArgumentException("Unknown option type");
		}

		this.underlyingName = null;
		this.exercise = new EuropeanExercise(maturity);
	}

	/**
	 * Creates a European option (single-asset case, unnamed underlying).
	 *
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 * @param callOrPutSign Option type.
	 */
	public EuropeanOption(final double maturity, final double strike, final CallOrPut callOrPutSign) {

		super();
		this.maturity = maturity;
		this.strike = strike;
		this.callOrPutSign = callOrPutSign;
		this.underlyingName = null;
		this.exercise = new EuropeanExercise(maturity);
	}

	/**
	 * Creates a European call option for a named underlying.
	 *
	 * @param underlyingName Name of the underlying.
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike) {
		this(underlyingName, maturity, strike, 1.0);
	}

	/**
	 * Creates a European call option (single-asset case, unnamed underlying).
	 *
	 * @param maturity Maturity {@code T}.
	 * @param strike Strike {@code K}.
	 */
	public EuropeanOption(final double maturity, final double strike) {
		this(maturity, strike, 1.0);
	}

	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {
		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, exercise);

		if (callOrPutSign == CallOrPut.CALL) {
			return solver.getValue(evaluationTime, maturity, assetValue -> Math.max(assetValue - strike, 0.0));
		} else {
			return solver.getValue(evaluationTime, maturity, assetValue -> Math.max(strike - assetValue, 0.0));
		}
	}

	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {
		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, exercise);

		if (callOrPutSign == CallOrPut.CALL) {
			return solver.getValues(maturity, assetValue -> Math.max(assetValue - strike, 0.0));
		} else {
			return solver.getValues(maturity, assetValue -> Math.max(strike - assetValue, 0.0));
		}
	}

	/**
	 * Returns the name of the underlying.
	 *
	 * @return The underlying name, or {@code null} if unspecified.
	 */
	public String getUnderlyingName() {
		return underlyingName;
	}

	/**
	 * Returns the option maturity.
	 *
	 * @return The maturity.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the option strike.
	 *
	 * @return The strike.
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * Returns whether the option is a call or put.
	 *
	 * @return The option type.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPutSign;
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
