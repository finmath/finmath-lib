package net.finmath.finitedifference.solvers;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * Interface for finite difference solvers.
 *
 * <p>
 * Implementations provide methods to compute the solution of a PDE
 * on a space-time grid, either at a specific evaluation time or
 * over the full time history.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FDMSolver {

	/**
	 * Returns the solution at a given evaluation time.
	 *
	 * <p>
	 * This is a legacy return type. Typical shapes are:
	 * </p>
	 * <ul>
	 *   <li>1D: {@code [nS]} representing values at the evaluation time.</li>
	 * <li>2D: {@code [nS * nV]} representing values at the evaluation
	 * time.</li>
	 * </ul>
	 *
	 * @param evaluationTime   The evaluation time.
	 * @param time             The maturity (time to maturity).
	 * @param valueAtMaturity  The payoff function applied at maturity.
	 * @return The solution at the specified evaluation time.
	 */
	double[] getValue(
			double evaluationTime,
			double time,
			DoubleUnaryOperator valueAtMaturity);

	/**
	 * Returns the full time history of the solution on the space-time grid.
	 *
	 * <p>
	 * Typical shapes are:
	 * </p>
	 * <ul>
	 *   <li>1D: {@code [nT][nS]}</li>
	 *   <li>2D: {@code [nT][nS * nV]}</li>
	 * </ul>
	 *
	 * @param time            The maturity (time to maturity).
	 * @param valueAtMaturity The payoff function applied at maturity.
	 * @return The full time-space solution.
	 */
	double[][] getValues(
			double time,
			DoubleUnaryOperator valueAtMaturity);

	/**
	 * Returns the solution at a given evaluation time using a precomputed
	 * terminal value vector on the spatial grid.
	 *
	 * <p>
	 * This overload is useful for products that require a non-pointwise
	 * terminal initialization, for example cell-averaged digitals.
	 * </p>
	 *
	 * <p>
	 * The supplied array is interpreted as the maturity layer ordered
	 * consistently with the solver's spatial grid.
	 * </p>
	 *
	 * @param evaluationTime The evaluation time.
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @return The value vector at the requested evaluation time.
	 */
	default double[] getValue(
			final double evaluationTime,
			final double time,
			final double[] terminalValues) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support direct terminal-vector initialization.");
	}

	/**
	 * Returns the full time history of the solution using a precomputed
	 * terminal value vector on the spatial grid.
	 *
	 * <p>
	 * This overload is useful for products that require a non-pointwise
	 * terminal initialization, for example cell-averaged digitals.
	 * </p>
	 *
	 * @param time The maturity time.
	 * @param terminalValues The terminal values on the spatial grid.
	 * @return The full time-space solution.
	 */
	default double[][] getValues(
			final double time,
			final double[] terminalValues) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support direct terminal-vector initialization.");
	}

	/**
	 * Default binary-payoff version.
	 * For solvers that are effectively 1D, the second state variable is
	 * ignored.
	 * @param evaluationTime The value.
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @return The value.
	 */
	default double[] getValue(
			final double evaluationTime,
			final double time,
			final DoubleBinaryOperator valueAtMaturity) {
		return getValue(evaluationTime, time, x -> valueAtMaturity.applyAsDouble(x, 0.0));
	}

	/**
	 * Default binary-payoff version.
	 * For solvers that are effectively 1D, the second state variable is
	 * ignored.
	 * @param time The value.
	 * @param valueAtMaturity The value.
	 * @return The value.
	 */
	default double[][] getValues(
			final double time,
			final DoubleBinaryOperator valueAtMaturity) {
		return getValues(time, x -> valueAtMaturity.applyAsDouble(x, 0.0));
	}

	/**
	 * Default binary-payoff version with early exercise
	 * For solvers that are effectively 1D, the second state variable is
	 * ignored.
	 * @param evaluationTime The value.
	 * @param time The value.
	 * @param terminalValues The value.
	 * @param exerciseValue The value.
	 * @return The value.
	 */
	default double[] getValue(
			final double evaluationTime,
			final double time,
			final double[] terminalValues,
			final DoubleUnaryOperator exerciseValue) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support terminal-vector initialization with pointwise exercise payoff.");
	}

	/**
	 * Default binary-payoff version with early exercise
	 * For solvers that are effectively 1D, the second state variable is
	 * ignored.
	 * @param time The value.
	 * @param terminalValues The value.
	 * @param exerciseValue The value.
	 * @return The value.
	 */
	default double[][] getValues(
			final double time,
			final double[] terminalValues,
			final DoubleUnaryOperator exerciseValue) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " does not support terminal-vector initialization with pointwise exercise payoff.");
	}
}
