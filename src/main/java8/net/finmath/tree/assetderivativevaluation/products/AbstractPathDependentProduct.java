package net.finmath.tree.assetderivativevaluation.products;

import java.util.Arrays;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.AbstractTreeProduct;
import net.finmath.tree.TreeModel;

/**
 * Base class for full (non-recombining) path-dependent products on a tree.
 *
 * Key property:
 * <ul>
 * 	<li>The number of nodes grows exponentially with the number of time steps:
 * 		level j has branchingFactor^j nodes.</li>
 * 	<li>No recombination / compression / bucketing.</li>
 * </ul>
 *
 * This class builds a full path tree of:
 * <ul>
 * 	<li>An underlying recombining state index (for reading spot from the model's spot level).</li>
 * 	<li>Product-specific path state variables (e.g. running sum/count for Asian).</li>
 * </ul>
 *
 * After the forward loop, it prices by backward induction using the {@link TreeModel}'s transition API:
 * <ul>
 * 	<li>p = model.getTransitionProbability(timeIndex, recombStateIndex, branchIndex)</li>
 * 	<li>df = model.getOneStepDiscountFactor(timeIndex)</li>
 * </ul>
 *
 * Design choices:
 * <ul>
 * 	<li>State variables are kept in primitive arrays for speed and to avoid per-node objects.</li>
 * 	<li>The model's spot is treated as a function of (timeIndex, recombStateIndex), so no explicit
 * 		spot multipliers are required.</li>
 * </ul>
 *
 * IMPORTANT:
 * <ul>
 * 	<li>To evolve the recombining state index along a branch, we require a "child state shift" convention.
 * 		For the existing models (CRR/JR/Boyle) this matches their internal layout:
 * 		<ul>
 * 			<li>Binomial: childShift = {0, 1}</li>
 * 			<li>Trinomial (Boyle): childShift = {0, 1, 2}</li>
 * 		</ul>
 * 		For a general multinomial model, childShift must be provided explicitly.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public abstract class AbstractPathDependentProduct extends AbstractTreeProduct {

	/**
	 * Fixing time indices on the model grid, e.g. {1,2,3,4,5}.
	 * These determine when path-state should be updated with spot.
	 */
	private final int[] fixingTimeIndices;

	protected AbstractPathDependentProduct(
			final double maturity,
			final int[] fixingTimeIndices) {
		super(maturity);
		this.fixingTimeIndices = fixingTimeIndices != null ? fixingTimeIndices.clone() : new int[0];
		Arrays.sort(this.fixingTimeIndices);
	}

	/**
	 * Number of (double-valued) path-state variables required by the product.
	 */
	protected abstract int getNumberOfStateVariables();

	/**
	 * Initialize product path-state at the root node.
	 */
	protected abstract void initializeState(
			double spotAtNode,
			int timeIndex,
			boolean isFixing,
			double[] stateOut);

	/**
	 * Evolve product path-state from parent to a child node.
	 */
	protected abstract void evolveState(
			double[] parentState,
			double spotAtChild,
			int timeIndexChild,
			boolean isFixingChild,
			double[] childStateOut);

	/**
	 * Terminal payoff at maturity.
	 */
	protected abstract double payoff(
			double[] terminalState,
			double spotAtMaturity);

	/**
	 * Converts a double time to a time index by rounding.
	 */
	protected final int timeToIndex(final double time, final TreeModel model) {
		return (int) Math.round(time / model.getTimeStep());
	}

	/**
	 * Returns whether a given time index is a fixing time.
	 */
	protected final boolean isFixingTimeIndex(final int timeIndex) {
		return Arrays.binarySearch(fixingTimeIndices, timeIndex) >= 0;
	}

	/**
	 * Determine branching factor and childStateShift convention.
	 */
	private int[] resolveChildShift(final TreeModel model, final int timeIndex, final int stateIndex) {
		final int branchingFactor = model.getNumberOfBranches(timeIndex, stateIndex);
		final int[] childStateShift = model.getChildStateIndexShift();

		if(childStateShift != null) {
			if(childStateShift.length != branchingFactor) {
				throw new IllegalArgumentException(
						"childStateShift length (" + childStateShift.length + ") "
						+ "does not match branching factor (" + branchingFactor + ").");
			}
			return childStateShift;
		}

		throw new IllegalArgumentException(
				"No childStateShift provided for branching factor " + branchingFactor + ".");
	}

	@Override
	public final RandomVariable[] getValues(final double evaluationTime, final TreeModel model) {

		final int k0 = timeToIndex(evaluationTime, model);
		final int n = timeToIndex(getMaturity(), model);
		if(n < k0) {
			throw new IllegalArgumentException("Maturity is before evaluation time.");
		}

		final int rootRecombState = 0;

		final int[] childShift = resolveChildShift(model, k0, rootRecombState);
		final int B = childShift.length;

		final int steps = n - k0;

		final RandomVariable[] levels = new RandomVariable[steps + 1];

		final int[][] recombIndex = new int[steps + 1][];
		final double[][] state = new double[steps + 1][];

		final int m = getNumberOfStateVariables();

		recombIndex[0] = new int[] { rootRecombState };
		state[0] = new double[m];

		final double spot0 = model.getSpotAtGivenTimeIndexRV(k0).get(rootRecombState);
		initializeState(spot0, k0, isFixingTimeIndex(k0), state[0]);

		for(int j = 1; j <= steps; j++) {

			final int timeIndex = k0 + j;

			final int nodes = powInt(B, j);
			final int parentNodes = powInt(B, j - 1);

			recombIndex[j] = new int[nodes];
			state[j] = new double[nodes * m];

			final RandomVariable spotRV = model.getSpotAtGivenTimeIndexRV(timeIndex);

			for(int parent = 0; parent < parentNodes; parent++) {

				final int parentRecomb = recombIndex[j - 1][parent];
				final int parentStateOffset = parent * m;

				for(int b = 0; b < B; b++) {

					final int child = parent * B + b;
					final int childRecomb = parentRecomb + childShift[b];

					recombIndex[j][child] = childRecomb;

					final double spotChild = spotRV.get(childRecomb);
					final int childStateOffset = child * m;

					evolveState(
							state[j - 1],
							parentStateOffset,
							spotChild,
							timeIndex,
							isFixingTimeIndex(timeIndex),
							state[j],
							childStateOffset);
				}
			}
		}

		final int terminalNodes = powInt(B, steps);
		final double[] vTerminal = new double[terminalNodes];

		final RandomVariable spotN = model.getSpotAtGivenTimeIndexRV(n);

		for(int node = 0; node < terminalNodes; node++) {

			final int recomb = recombIndex[steps][node];
			final double spotAtMaturity = spotN.get(recomb);

			final double[] tmp = new double[m];
			System.arraycopy(state[steps], node * m, tmp, 0, m);

			vTerminal[node] = payoff(tmp, spotAtMaturity);
		}

		levels[steps] = new RandomVariableFromDoubleArray(n * model.getTimeStep(), vTerminal);

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

	private void evolveState(
			final double[] parentStateFlat,
			final int parentOffset,
			final double spotChild,
			final int timeIndexChild,
			final boolean isFixingChild,
			final double[] childStateFlat,
			final int childOffset) {

		final int m = getNumberOfStateVariables();

		final double[] parent = new double[m];
		System.arraycopy(parentStateFlat, parentOffset, parent, 0, m);

		final double[] child = new double[m];
		evolveState(parent, spotChild, timeIndexChild, isFixingChild, child);

		System.arraycopy(child, 0, childStateFlat, childOffset, m);
	}

	private static int powInt(final int base, final int exp) {
		if(exp < 0) {
			throw new IllegalArgumentException("exp < 0");
		}
		int r = 1;
		for(int i = 0; i < exp; i++) {
			r = Math.multiplyExact(r, base);
		}
		return r;
	}
}
