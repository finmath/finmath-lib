package net.finmath.finitedifference.assetderivativevaluation.models;

/**
 * Minimal abstract base class for a state-independent jump component.
 *
 * <p>
 * This class provides boilerplate for jump components whose Levy density does
 * not
 * depend on the current PDE state variables. It is intended as a convenient
 * base
 * for models such as Bates and Variance Gamma, where the jump law is typically
 * specified directly in terms of the log-jump size.
 * </p>
 *
 * <p>
 * The jump contribution is understood in the compensated form
 * </p>
 *
 * <pre>
 * integral [ u(..., x exp(y), ...) - u(..., x, ...)
 *          - x (exp(y) - 1) partial_x u(..., x, ...) ] nu(dy),
 * </pre>
 *
 * <p>
 * where {@code y} denotes the log-relative jump size and {@code x} is the
 * affected
 * state variable.
 * </p>
 *
 * <p>
 * The integration bounds returned by this class are interpreted as numerical
 * truncation bounds for the jump variable. For finite-support models they may
 * coincide with the true support; for infinite-support models they are
 * numerical
 * cutoffs chosen by the implementation.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public abstract class AbstractStateIndependentJumpComponent implements JumpComponent {

	private static final long serialVersionUID = 1L;

	/**
	 * The state variable index.
	 */
	private final int stateVariableIndex;
	/**
	 * The lower integration bound.
	 */
	private final double lowerIntegrationBound;
	/**
	 * The upper integration bound.
	 */
	private final double upperIntegrationBound;
	/**
	 * The finite activity.
	 */
	private final boolean finiteActivity;
	/**
	 * The finite variation.
	 */
	private final boolean finiteVariation;

	/**
	 * Creates a state-independent jump component acting on the first state
	 * variable.
	 *
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param finiteActivity {@code true} if the jump measure has finite
	 *     activity.
	 * @param finiteVariation {@code true} if the jump part has finite
	 *     variation.
	 */
	protected AbstractStateIndependentJumpComponent(
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final boolean finiteActivity,
			final boolean finiteVariation) {
		this(0, lowerIntegrationBound, upperIntegrationBound, finiteActivity, finiteVariation);
	}

	/**
	 * Creates a state-independent jump component.
	 *
	 * @param stateVariableIndex Index of the affected state variable.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param finiteActivity {@code true} if the jump measure has finite
	 *     activity.
	 * @param finiteVariation {@code true} if the jump part has finite
	 *     variation.
	 */
	protected AbstractStateIndependentJumpComponent(
			final int stateVariableIndex,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final boolean finiteActivity,
			final boolean finiteVariation) {

		if (stateVariableIndex < 0) {
			throw new IllegalArgumentException("State variable index must be non-negative.");
		}
		if (lowerIntegrationBound >= upperIntegrationBound) {
			throw new IllegalArgumentException(
					"Lower integration bound must be strictly smaller than upper integration bound.");
		}

		this.stateVariableIndex = stateVariableIndex;
		this.lowerIntegrationBound = lowerIntegrationBound;
		this.upperIntegrationBound = upperIntegrationBound;
		this.finiteActivity = finiteActivity;
		this.finiteVariation = finiteVariation;
	}

	@Override
	public int getStateVariableIndex() {
		return stateVariableIndex;
	}

	@Override
	public double getLowerIntegrationBound(final double time, final double... stateVariables) {
		return lowerIntegrationBound;
	}

	@Override
	public double getUpperIntegrationBound(final double time, final double... stateVariables) {
		return upperIntegrationBound;
	}

	@Override
	public final double getLevyDensity(
			final double time,
			final double jumpSize,
			final double... stateVariables) {
		return getLevyDensity(time, jumpSize);
	}

	@Override
	public boolean isStateDependent() {
		return false;
	}

	@Override
	public boolean isFiniteActivity() {
		return finiteActivity;
	}

	@Override
	public boolean isFiniteVariation() {
		return finiteVariation;
	}

	/**
	 * Returns the Levy density at the given log-jump size.
	 *
	 * <p>
	 * Implementations only need to provide the state-independent density
	 * {@code nu(y)}. Dependence on the PDE state variables is intentionally
	 * excluded
	 * from this base class.
	 * </p>
	 *
	 * @param time The evaluation time.
	 * @param jumpSize The log-relative jump size.
	 * @return The Levy density at the given jump size.
	 */
	protected abstract double getLevyDensity(double time, double jumpSize);
}
