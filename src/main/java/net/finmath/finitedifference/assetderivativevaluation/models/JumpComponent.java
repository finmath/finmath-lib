package net.finmath.finitedifference.assetderivativevaluation.models;

import java.io.Serializable;

/**
 * Describes the jump part of the infinitesimal generator used by a finite-
 * difference
 * equity model.
 *
 * <p>
 * This interface is intentionally narrow: it does not attempt to expose a fully
 * generic non-local operator, but only the ingredients needed to assemble the
 * jump
 * term in the pricing PIDE.
 * </p>
 *
 * <p>
 * The current convention is that jumps act on one selected state variable
 * through a
 * log-relative jump size {@code y}. In the standard stock-coordinate case, this
 * means
 * the affected state variable {@code x} is mapped to {@code x * exp(y)}.
 * </p>
 *
 * <p>
 * For an affected state variable {@code x}, the jump contribution is
 * interpreted as
 * </p>
 *
 * <pre>
 * integral [ u(..., x exp(y), ...) - u(..., x, ...)
 *          - x (exp(y) - 1) partial_x u(..., x, ...) ] nu(dy),
 * </pre>
 *
 * <p>
 * where the compensation term is part of the integral definition.
 * </p>
 *
 * <p>
 * This interface therefore complements, but does not modify, the meaning of
 * {@link FiniteDifferenceEquityModel#getDrift(double, double...)}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface JumpComponent extends Serializable {

	/**
	 * Returns the index of the state variable affected by the jump operator.
	 *
	 * <p>
	 * The default is {@code 0}, corresponding to the first PDE state variable.
	 * </p>
	 *
	 * @return The index of the affected state variable.
	 */
	default int getStateVariableIndex() {
		return 0;
	}

	/**
	 * Returns the lower integration bound for the jump variable.
	 *
	 * <p>
	 * The jump variable is interpreted as a log-relative jump size {@code y}.
	 * For infinite-support models this bound will typically be a numerical
	 * truncation
	 * chosen by the implementation.
	 * </p>
	 *
	 * @param time The evaluation time.
	 * @param stateVariables The current PDE state variables.
	 * @return The lower integration bound.
	 */
	double getLowerIntegrationBound(double time, double... stateVariables);

	/**
	 * Returns the upper integration bound for the jump variable.
	 *
	 * <p>
	 * The jump variable is interpreted as a log-relative jump size {@code y}.
	 * For infinite-support models this bound will typically be a numerical
	 * truncation
	 * chosen by the implementation.
	 * </p>
	 *
	 * @param time The evaluation time.
	 * @param stateVariables The current PDE state variables.
	 * @return The upper integration bound.
	 */
	double getUpperIntegrationBound(double time, double... stateVariables);

	/**
	 * Returns the Levy density evaluated at the given jump size.
	 *
	 * <p>
	 * The argument {@code jumpSize} is interpreted as the log-relative jump
	 * {@code y}. In the standard stock-coordinate convention, the affected
	 * state
	 * variable {@code x} jumps to {@code x * exp(y)}.
	 * </p>
	 *
	 * @param time The evaluation time.
	 * @param jumpSize The log-relative jump size.
	 * @param stateVariables The current PDE state variables.
	 * @return The Levy density {@code nu(y)}.
	 */
	double getLevyDensity(double time, double jumpSize, double... stateVariables);

	/**
	 * Returns whether the jump density depends explicitly on the current state.
	 *
	 * <p>
	 * Bates and Variance Gamma in their standard forms are typically state-
	 * independent
	 * in the jump part, so the default is {@code false}.
	 * </p>
	 *
	 * @return {@code true} if the jump density depends on the current state
	 *     variables.
	 */
	default boolean isStateDependent() {
		return false;
	}

	/**
	 * Returns whether the jump component has finite activity.
	 *
	 * <p>
	 * This is metadata only and may be useful for later numerical choices.
	 * </p>
	 *
	 * @return {@code true} if the jump measure has finite total mass.
	 */
	default boolean isFiniteActivity() {
		return false;
	}

	/**
	 * Returns whether the jump component has finite variation.
	 *
	 * <p>
	 * This is metadata only and may be useful for later numerical choices.
	 * </p>
	 *
	 * @return {@code true} if the jump part has finite variation.
	 */
	default boolean isFiniteVariation() {
		return false;
	}
}
