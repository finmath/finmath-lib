package net.finmath.finitedifference.solvers;

/**
 * Activation policy for two-state finite-difference products with immediate
 * cash settlement
 * upon barrier activation.
 *
 * <p>
 * In a two-state barrier formulation, the contract value is decomposed into an
 * inactive
 * no-hit regime and an already-hit regime. Under an immediate-cash activation
 * rule, once
 * the barrier event occurs, the contract is replaced by a deterministic cash
 * amount
 * <i>P</i>, independently of the current state variable and independently of
 * any active
 * continuation value.
 * </p>
 *
 * <p>
 * Hence the already-hit value is constant and given by
 * </p>
 *
 * <p>
 * <i>V^{\mathrm{hit}}(t,x) = P</i>
 * </p>
 *
 * <p>
 * for all times <i>t</i> after activation and all states <i>x</i>. In
 * particular, at
 * maturity, at intermediate times, and at the activation interface, the value
 * is always
 * the same cash amount.
 * </p>
 *
 * <p>
 * This policy is appropriate for products such as one-touch options with at-hit
 * settlement,
 * where the contract terminates immediately upon barrier contact and pays a
 * fixed cash
 * amount at that moment.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class ImmediateCashActivationPolicy implements TwoStateActivationPolicy {

	/**
	 * The payoff amount.
	 */
	private final double payoffAmount;

	/**
	 * Creates an immediate-cash activation policy.
	 *
	 * @param payoffAmount Cash amount paid immediately upon activation.
	 */
	public ImmediateCashActivationPolicy(final double payoffAmount) {
		this.payoffAmount = payoffAmount;
	}

	/**
	 * Returns the already-hit value at maturity.
	 *
	 * @param stateVariable State variable.
	 * @param activePayoffValue Active-regime payoff value at maturity.
	 * @param inactiveNoHitValue Inactive no-hit value at maturity.
	 * @return The immediate cash payoff amount.
	 */
	@Override
	public double getAlreadyHitValueAtMaturity(
			final double stateVariable,
			final double activePayoffValue,
			final double inactiveNoHitValue) {
		return payoffAmount;
	}

	/**
	 * Returns the already-hit value before maturity.
	 *
	 * @param currentTime Current running time.
	 * @param stateVariable State variable.
	 * @param activeValue Active-regime continuation value.
	 * @return The immediate cash payoff amount.
	 */
	@Override
	public double getAlreadyHitValue(
			final double currentTime,
			final double stateVariable,
			final double activeValue) {
		return payoffAmount;
	}

	/**
	 * Returns the interface value at the activation boundary.
	 *
	 * @param currentTime Current running time.
	 * @param barrierStateVariable State variable at the barrier.
	 * @param activeValueAtBarrier Active-regime value at the barrier.
	 * @return The immediate cash payoff amount.
	 */
	@Override
	public double getInterfaceValue(
			final double currentTime,
			final double barrierStateVariable,
			final double activeValueAtBarrier) {
		return payoffAmount;
	}
}
