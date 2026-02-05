package net.finmath.tree;

import net.finmath.modelling.Model;
import net.finmath.stochastic.RandomVariable;
import java.util.function.DoubleUnaryOperator;

/**
 * General interface rappresenting a tree model with all the common methods
 *
 * @author Carlo Andrea Tramentozzi
 * @author Alessandro Gnoatto
 * @author Andrea Mazzon
 * @version 1.0
 */
public interface TreeModel extends Model {

	double getTimeStep();
	int getNumberOfTimes();
	double getLastTime();

	/**
	 * Returns the spot process at a given time transformed pointwise by a function.
	 * Equivalent to getSpotAtGivenTimeIndexRV(k).apply(transformFunction), but accepts a physical time.
	 *
	 * @param timeindex  Physical time t; the model maps it to the nearest lattice index k (e.g. round(t/dt)).
	 * @param transformFunction  Function f(s) applied element-wise to S_k.
	 * @return A RandomVariable at time k with length = number of states at level k
	 *         (binomial: k+1; trinomial: 2k+1), containing f(S_k) pathwise.
	 */
	RandomVariable getTransformedValuesAtGivenTimeRV(double timeindex, DoubleUnaryOperator transformFunction);

	/**
	 * One-step risk-neutral discounted conditional expectation operator.
	 * Takes values defined at level k+1 and projects them back to level k:
	 *   V_k = E^Q[ V_{k+1} | F_k ] * DF  (DF is the one-step discount factor).
	 *
	 * @param optionValues  RandomVariable living at level k+1.
	 * @param timeindex     Level k to which the conditional expectation is taken (0 ≤ k < n).
	 * @return A RandomVariable at level k with length matching the number of states at k.
	 */
	RandomVariable getConditionalExpectationRV(RandomVariable optionValues, int timeindex);

	/**
	 * Returns the spot vector S_k as a RandomVariable at lattice level k.
	 *
	 * @param timeindex  Level index k (0…n).
	 * @return A RandomVariable carrying S_k pathwise; length = number of states at level k
	 *         (binomial: k+1; trinomial: 2k+1). The time stamp is typically t_k = k*dt.
	 */
	RandomVariable getSpotAtGivenTimeIndexRV(int timeindex);

	/**
	 * Returns the number of outgoing branches from a node at time index k.
	 * <p>
	 * This method is intentionally defined per node to allow state-dependent branching
	 * (e.g. for trinomial with pruning, multinomial with barriers, etc.). For standard
	 * binomial / trinomial recombining trees this is constant (2 / 3) and independent
	 * of {@code stateIndex}.
	 *
	 * @param timeIndex  The time index k (0 ≤ k < n).
	 * @param stateIndex The node / state index at time k (model-defined).
	 * @return The number of branches from that node to time k+1.
	 */
	default int getNumberOfBranches(int timeIndex, int stateIndex) {
		throw new UnsupportedOperationException("Transition API not implemented by this TreeModel.");
	}

	/**
	 * Returns the risk-neutral transition probability for a branch.
	 * Branch indices are model-defined but should be consistent with how the model
	 * evolves the risk factor (e.g. for binomial: 0=up,1=down; for trinomial: 0=up,1=mid,2=down).
	 *
	 * @param timeIndex   The time index k (0 ≤ k < n).
	 * @param stateIndex  The node / state index at time k (model-defined).
	 * @param branchIndex The branch index in {@code [0, getNumberOfBranches(k,i))}.
	 * @return Transition probability to the corresponding child on level k+1.
	 */
	default double getTransitionProbability(int timeIndex, int stateIndex, int branchIndex) {
		throw new UnsupportedOperationException("Transition API not implemented by this TreeModel.");
	}

	/**
	 * One-step discount factor from time k to k+1 (applied when taking the conditional expectation).
	 * For example, for a constant short rate r and step dt, DF = exp(-r*dt).
	 *
	 * @param timeIndex The time index k (0 ≤ k < n).
	 * @return One-step discount factor DF(k,k+1).
	 */
	default double getOneStepDiscountFactor(int timeIndex) {
		throw new UnsupportedOperationException("Transition API not implemented by this TreeModel.");
	}


	/**
	* Returns the model's child-index shift convention for recombining state indices.
	*
	* Convention: childIndex = parentIndex + shift[branchIndex].
	*
	* Examples:
	*  - Binomial CRR/JR: {0, 1} interpreted as {up, down}
	*  - Trinomial Boyle: {0, 1, 2} interpreted as {up, mid, down}
	*
	* Path-dependent products that build a full non-recombining tree (exponential growth)
	* can use this to map their parent recombining state index to the child's recombining
	* state index when querying model spots.
	*
	* Default throws: models supporting path-dependent products should override this.
	*
	* @return shift array of length = number of branches.
	*/
	default int[] getChildStateIndexShift() {
		throw new UnsupportedOperationException("Child state index shift not implemented by this TreeModel.");
	}

}
