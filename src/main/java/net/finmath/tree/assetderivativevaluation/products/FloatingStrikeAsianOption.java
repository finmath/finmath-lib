package net.finmath.tree.assetderivativevaluation.products;

/**
 * Floating-strike Asian option (CALL) on a full non-recombining tree (exponential growth).
 *
 * Payoff at maturity:
 * <pre>
 *     max(S(T) - A, 0)
 * </pre>
 * where A is the arithmetic average of the spot over the fixingTimeIndices.
 *
 * State variables:
 * <ul>
 * 	<li>state[0] = running sum of fixing spots</li>
 * 	<li>state[1] = fixing count</li>
 * </ul>
 *
 * No recombination / compression:
 * <ul>
 * 	<li>Number of nodes at step j is B^j (B = branching factor of the model).</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class FloatingStrikeAsianOption extends AbstractPathDependentProduct {

	/**
	 * @param maturity Maturity in model time units.
	 * @param fixingTimeIndices Subset of model time indices included in the average.
	 */
	public FloatingStrikeAsianOption(
			final double maturity,
			final int[] fixingTimeIndices) {
		super(maturity, fixingTimeIndices);
	}

	@Override
	protected int getNumberOfStateVariables() {
		return 2; // running sum, fixing count
	}

	@Override
	protected void initializeState(
			final double spotAtNode,
			final int timeIndex,
			final boolean isFixing,
			final double[] stateOut) {

		stateOut[0] = isFixing ? spotAtNode : 0.0; // sum
		stateOut[1] = isFixing ? 1.0 : 0.0;        // count (stored as double)
	}

	@Override
	protected void evolveState(
			final double[] parentState,
			final double spotAtChild,
			final int timeIndexChild,
			final boolean isFixingChild,
			final double[] childStateOut) {

		childStateOut[0] = parentState[0] + (isFixingChild ? spotAtChild : 0.0);
		childStateOut[1] = parentState[1] + (isFixingChild ? 1.0 : 0.0);
	}

	@Override
	protected double payoff(
			final double[] terminalState,
			final double spotAtMaturity) {

		final double sum = terminalState[0];
		final double cnt = terminalState[1];

		// Defensive guard: empty fixing set.
		final double average = (cnt > 0.0) ? (sum / cnt) : 0.0;

		// Floating-strike CALL payoff.
		return Math.max(spotAtMaturity - average, 0.0);
	}
}
