package net.finmath.tree;

import net.finmath.modelling.Model;
import net.finmath.stochastic.RandomVariable;
import java.util.function.DoubleUnaryOperator;

/**
 * General interface rappresenting a tree model with all the common methods
 *
 * @author Carlo Andrea Tramentozzi
 * @version 1.0
 */

public interface TreeModel extends Model {

	double getTimeStep();
	int getNumberOfTimes();


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


	/** Getter */
	//double getInitialPrice();
	//double getRiskFreeRate();
	//double getVolatility();
	double getLastTime();


}
