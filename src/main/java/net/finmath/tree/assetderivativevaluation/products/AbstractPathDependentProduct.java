package net.finmath.tree.assetderivativevaluation.products;

import java.util.Arrays;

import net.finmath.stochastic.RandomVariable;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.tree.AbstractTreeProduct;
import net.finmath.tree.TreeModel;

/**
 * Base class for full (non-recombining) path-dependent products on a tree.
 *
 * Key property:
 *  - The number of nodes grows exponentially with the number of time steps:
 *      level j has branchingFactor^j nodes.
 *  - NO recombination / compression / bucketing.
 *
 * This class builds a "full path tree" of:
 *  - an underlying recombining state index (for reading spot from the model's spot level),
 *  - plus product-specific path state variables (e.g. running sum/count for Asian). We build the 
 *   full (non-recombining) path tree (size branchingFactor^j after j steps)
 *
 * After the forward loop, it then prices by backward induction using TreeModel's transition API:
 *  - p = model.getTransitionProbability(timeIndex, recombStateIndex, branchIndex)
 *  - df = model.getOneStepDiscountFactor(timeIndex)
 *
 * Design choices:
 *  - We keep state variables in primitive arrays for speed and to avoid per-node objects.
 *  - We treat the model's spot as a function of (timeIndex, recombStateIndex) so we don't
 *    need the model to expose explicit "spot multipliers".
 *
 * IMPORTANT:
 *  - To evolve the recombining state index along a branch we require a "child state shift"
 *    convention. For the existing models (CRR/JR/Boyle) this matches their internal layout:
 *      Binomial: childShift = {0, 1}  (up keeps index, down increases index)
 *      Trinomial (Boyle): childShift = {0, 1, 2} (up, mid, down)
 *    For a general multinomial model, one has to provide childShift explicitly.
 *    
 *    @author Alessandro Gnoatto
 */
public abstract class AbstractPathDependentProduct extends AbstractTreeProduct {

    /**
     * Fixing time indices on the model grid, e.g. {1,2,3,4,5}.
     * These determine when path-state should be updated with spot.
     */
    private final int[] fixingTimeIndices;


    protected AbstractPathDependentProduct(
            final double maturity,
            final int[] fixingTimeIndices
    ) {
        super(maturity);
        this.fixingTimeIndices = fixingTimeIndices != null ? fixingTimeIndices.clone() : new int[0];
        Arrays.sort(this.fixingTimeIndices);
    }

    /**
     * Number of (double-valued) path-state variables required by the product.
     * Examples:
     *  - Fixed-strike Asian: 2 variables (sum, count)
     *  - Lookback (min): 1 variable (runningMin)
     *  - Lookback (min/max): 2 variables
     */
    protected abstract int getNumberOfStateVariables();

    /**
     * Initialize product path-state at the root node.
     *
     * @param spotAtNode spot at (timeIndex, recombStateIndex)
     * @param timeIndex  time index for root (typically k0)
     * @param isFixing   whether this time index is part of averaging/fixing set
     * @param stateOut   output array length = getNumberOfStateVariables()
     */
    protected abstract void initializeState(
            double spotAtNode,
            int timeIndex,
            boolean isFixing,
            double[] stateOut
    );

    /**
     * Evolve product path-state from parent to a child node.
     *
     * @param parentState state at parent node
     * @param spotAtChild spot at child node
     * @param timeIndexChild time index of child
     * @param isFixingChild whether child time is fixing
     * @param childStateOut output state array (length = getNumberOfStateVariables())
     */
    protected abstract void evolveState(
            double[] parentState,
            double spotAtChild,
            int timeIndexChild,
            boolean isFixingChild,
            double[] childStateOut
    );

    /**
     * Terminal payoff at maturity.
     *
     * @param terminalState state variables at maturity
     * @param spotAtMaturity spot at maturity node (often needed e.g. floating-strike Asian, lookback)
     * @return payoff
     */
    protected abstract double payoff(
            double[] terminalState,
            double spotAtMaturity
    );

    /**
     * Converts a double time to a time index by rounding.
     */
    protected final int timeToIndex(double time, TreeModel model) {
        return (int) Math.round(time / model.getTimeStep());
    }

    /**
     * Returns whether a given time index is a fixing time.
     */
    protected final boolean isFixingTimeIndex(int timeIndex) {
        return Arrays.binarySearch(fixingTimeIndices, timeIndex) >= 0;
    }

    /**
     * Determine branching factor and childStateShift convention.
     */
    private int[] resolveChildShift(TreeModel model, int timeIndex, int stateIndex) {
        int branchingFactor = model.getNumberOfBranches(timeIndex, stateIndex);
        int[] childStateShift = model.getChildStateIndexShift();
        if(childStateShift != null) {
            if(childStateShift.length != branchingFactor) {
                throw new IllegalArgumentException(
                        "childStateShift length (" + childStateShift.length + ") "
                        + "does not match branching factor (" + branchingFactor + ")."
                );
            }
            return childStateShift;
        }

        throw new IllegalArgumentException(
                "No childStateShift provided for branching factor " + branchingFactor
                + "."
        );
    }

    /**
     * Full non-recombining valuation.
     *
     * Notes about evaluationTime:
     *  - Path dependent contracts generally require the past path-state at evaluationTime.
     *  - This implementation assumes the path-state STARTS at evaluationTime (i.e. no past).
     *    This is exactly what you want when evaluationTime = 0.
     *    
     * IMPORTANT WARNING:
     * 
     *    In the current structure of the library this method will always be called with evaluationTime = 0.
     *    When evaluationTime > 0. the logic contained in AbstractTreeProduct guarantees that the whole tree of the 
     *    path dependent functional is reconstructed starting from time zero. After this the time instants before the
     *    requested time are discarded and the RandomVariable at the selected time is returned. Remember that we enjoy the 
     *    Markov property only by extending the state space, i.e. with respect to the couple (stock, pathDependentFunctional)
     */
    @Override
    public final RandomVariable[] getValues(final double evaluationTime, final TreeModel model) {

        // Ensure transition API is supported
        // (if not, TreeModel default methods will throw).
        final int k0 = timeToIndex(evaluationTime, model);
        final int n  = timeToIndex(getMaturity(), model);
        if(n < k0) throw new IllegalArgumentException("Maturity is before evaluation time.");

        // Root uses recombining state index 0 (consistent with European products).
        final int rootRecombState = 0;

        // Determine branching and child shift convention at the root step.
        // We assume constant branching factor.
        final int[] childShift = resolveChildShift(model, k0, rootRecombState);
        final int B = childShift.length;

        final int steps = n - k0;

        // levels[j] corresponds to timeIndex = k0 + j, and has B^j nodes.
        final RandomVariable[] levels = new RandomVariable[steps + 1];

        // Store recombining index per full node, and product state variables per node.
        // recombIndex[j][node] = recombining state index at model time k0+j.
        final int[][] recombIndex = new int[steps + 1][];
        final double[][] state = new double[steps + 1][]; // flattened: node-major, then var-major

        final int m = getNumberOfStateVariables();

        // Allocate level 0
        recombIndex[0] = new int[] { rootRecombState };
        state[0] = new double[m]; // 1 node * m vars

        // Spot at root
        final double spot0 = model.getSpotAtGivenTimeIndexRV(k0).get(rootRecombState);

        initializeState(spot0, k0, isFixingTimeIndex(k0), state[0]);

        // Forward expansion of path-state (full tree)
        for(int j = 1; j <= steps; j++) {
            final int timeIndex = k0 + j;

            final int nodes = powInt(B, j);
            final int parentNodes = powInt(B, j - 1);

            recombIndex[j] = new int[nodes];
            state[j] = new double[nodes * m];

            // Pre-fetch the recombining spot vector at this time index.
            // We will index into it using recombIndex[j][node].
            final var spotRV = model.getSpotAtGivenTimeIndexRV(timeIndex);

            for(int parent = 0; parent < parentNodes; parent++) {

                final int parentRecomb = recombIndex[j - 1][parent];

                // parent state slice
                final int parentStateOffset = parent * m;

                for(int b = 0; b < B; b++) {

                    final int child = parent * B + b;

                    final int childRecomb = parentRecomb + childShift[b];
                    recombIndex[j][child] = childRecomb;

                    final double spotChild = spotRV.get(childRecomb);

                    // child state slice
                    final int childStateOffset = child * m;

                    // evolve state
                    evolveState(
                            state[j - 1],
                            parentStateOffset,
                            spotChild,
                            timeIndex,
                            isFixingTimeIndex(timeIndex),
                            state[j],
                            childStateOffset
                    );
                }
            }
        }

        // Terminal payoff values at maturity level
        final int terminalNodes = powInt(B, steps);
        final double[] vTerminal = new double[terminalNodes];

        final var spotN = model.getSpotAtGivenTimeIndexRV(n);

        for(int node = 0; node < terminalNodes; node++) {
            final int recomb = recombIndex[steps][node];
            final double spotAtMaturity = spotN.get(recomb);

            final double[] tmp = new double[m];
            System.arraycopy(state[steps], node * m, tmp, 0, m);

            vTerminal[node] = payoff(tmp, spotAtMaturity);
        }

        levels[steps] = new RandomVariableFromDoubleArray(n * model.getTimeStep(), vTerminal);

        // Backward induction on full node set
        double[] vNext = vTerminal;

        for(int j = steps - 1; j >= 0; j--) {

            final int timeIndex = k0 + j;
            final int nodesHere = powInt(B, j);

            final double[] vHere = new double[nodesHere];

            final double df = model.getOneStepDiscountFactor(timeIndex);

            for(int node = 0; node < nodesHere; node++) {

                final int parentRecomb = recombIndex[j][node];

                double sum = 0.0;

                for(int b = 0; b < B; b++) {

                    final int child = node * B + b;

                    final double p = model.getTransitionProbability(timeIndex, parentRecomb, b);

                    sum += p * vNext[child];
                }

                vHere[node] = df * sum;
            }

            levels[j] = new RandomVariableFromDoubleArray(timeIndex * model.getTimeStep(), vHere);
            vNext = vHere;
        }

        return levels;
    }

    /**
     * Helper: evolveState where parent/child arrays are flattened.
     * This avoids allocating per-node double[] objects.
     */
    private void evolveState(
            final double[] parentStateFlat,
            final int parentOffset,
            final double spotChild,
            final int timeIndexChild,
            final boolean isFixingChild,
            final double[] childStateFlat,
            final int childOffset
    ) {
        final int m = getNumberOfStateVariables();
        final double[] parent = new double[m];
        System.arraycopy(parentStateFlat, parentOffset, parent, 0, m);

        final double[] child = new double[m];
        evolveState(parent, spotChild, timeIndexChild, isFixingChild, child);

        System.arraycopy(child, 0, childStateFlat, childOffset, m);
    }

    private static int powInt(int base, int exp) {
        if(exp < 0) throw new IllegalArgumentException("exp < 0");
        int r = 1;
        for(int i = 0; i < exp; i++) {
            r = Math.multiplyExact(r, base); // throws if overflow
        }
        return r;
    }
}
