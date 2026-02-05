package net.finmath.tree.assetderivativevaluation.products;

/**
 * Fixed-strike Asian option using FULL non-recombining tree expansion.
 *
 * State variables:
 *  - state[0] = runningSum of fixing spots
 *  - state[1] = fixingCount
 *
 * Payoff at maturity:
 *  - max( (sum/count) - K, 0 )
 *
 * The averaging can be performed on any subset of the time grid by specifying fixingTimeIndices.
 *
 * No path compression: number of nodes at level j is B^j.
 */
public class AsianOption extends AbstractPathDependentProduct {

    private final double strike;

    /**
     * @param maturity maturity in model time units
     * @param strike strike K
     * @param fixingTimeIndices subset of model time indices included in the average
     * @param childStateShift optional mapping for recombining index evolution;
     *                       pass null for defaults (binomial: {0,1}, trinomial: {0,1,2})
     */
    public AsianOption(
            final double maturity,
            final double strike,
            final int[] fixingTimeIndices
    ) {
        super(maturity, fixingTimeIndices);
        this.strike = strike;
    }

    @Override
    protected int getNumberOfStateVariables() {
        return 2; // sum, count
    }

    @Override
    protected void initializeState(
            final double spotAtNode,
            final int timeIndex,
            final boolean isFixing,
            final double[] stateOut
    ) {
        stateOut[0] = isFixing ? spotAtNode : 0.0;  // sum
        stateOut[1] = isFixing ? 1.0 : 0.0;         // count stored as double for simplicity
    }

    @Override
    protected void evolveState(
            final double[] parentState,
            final double spotAtChild,
            final int timeIndexChild,
            final boolean isFixingChild,
            final double[] childStateOut
    ) {
        childStateOut[0] = parentState[0] + (isFixingChild ? spotAtChild : 0.0);
        childStateOut[1] = parentState[1] + (isFixingChild ? 1.0 : 0.0);
    }

    @Override
    protected double payoff(final double[] terminalState, final double spotAtMaturity) {
        final double sum = terminalState[0];
        final double cnt = terminalState[1];

        final double avg = (cnt > 0.0) ? (sum / cnt) : 0.0;

        return Math.max(avg - strike, 0.0);
    }
}
