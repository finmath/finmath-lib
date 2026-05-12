package net.finmath.finitedifference.solvers;

/**
 * Activation policy for two-state finite-difference products in which the
 * active regime
 * continues with its own continuation value once the barrier event has
 * occurred.
 *
 * <p>
 * In a two-state barrier formulation, the contract value is decomposed into an
 * inactive
 * no-hit regime and an active already-hit regime. Let
 * <i>V^{\mathrm{act}}(t,x)</i> denote the value in the active regime, where
 * <i>x</i> is the state variable. Under a continuation activation rule, barrier
 * activation
 * does not trigger an immediate replacement payoff other than the active branch
 * itself.
 * Hence the already-hit value is simply identified with the active continuation
 * value.
 * </p>
 *
 * <p>
 * At maturity this means
 * </p>
 *
 * <p>
 * <i>V^{\mathrm{hit}}(T,x) = V^{\mathrm{act}}(T,x)</i>,
 * </p>
 *
 * <p>
 * independently of the inactive no-hit terminal value. Likewise, at
 * intermediate times
 * and at the activation interface,
 * </p>
 *
 * <p>
 * <i>V^{\mathrm{hit}}(t,x) = V^{\mathrm{act}}(t,x)</i>.
 * </p>
 *
 * <p>
 * This policy is appropriate for contracts where hitting the barrier switches
 * the product
 * into an active continuation problem rather than causing an immediate cash
 * settlement.
 * A typical example is an expiry-settled knock-in structure, where the contract
 * remains
 * alive after activation and evolves according to the active branch PDE until
 * maturity.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class ContinuationActivationPolicy implements TwoStateActivationPolicy {

	/**
	 * Returns the already-hit value at maturity.
	 *
	 * @param stateVariable State variable.
	 * @param activePayoffValue Active-regime payoff value at maturity.
	 * @param inactiveNoHitValue Inactive no-hit value at maturity.
	 * @return The already-hit value at maturity.
	 */
	@Override
	public double getAlreadyHitValueAtMaturity(
			final double stateVariable,
			final double activePayoffValue,
			final double inactiveNoHitValue) {
		return activePayoffValue;
	}

	/**
	 * Returns the already-hit value before maturity.
	 *
	 * @param currentTime Current running time.
	 * @param stateVariable State variable.
	 * @param activeValue Active-regime continuation value.
	 * @return The already-hit value.
	 */
	@Override
	public double getAlreadyHitValue(
			final double currentTime,
			final double stateVariable,
			final double activeValue) {
		return activeValue;
	}

	/**
	 * Returns the interface value at the activation boundary.
	 *
	 * @param currentTime Current running time.
	 * @param barrierStateVariable State variable at the barrier.
	 * @param activeValueAtBarrier Active-regime value at the barrier.
	 * @return The interface value.
	 */
	@Override
	public double getInterfaceValue(
			final double currentTime,
			final double barrierStateVariable,
			final double activeValueAtBarrier) {
		return activeValueAtBarrier;
	}
}
