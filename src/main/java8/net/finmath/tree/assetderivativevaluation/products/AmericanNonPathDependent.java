package net.finmath.tree.assetderivativevaluation.products;


import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.TreeModel;

import java.util.function.DoubleUnaryOperator;

/**
 * American (non–path-dependent) option priced on a recombining TreeModel.
 * The product allows early exercise at every lattice time prior to maturity.
 * At each step k it compares:
 * Continuation value: discounted conditional expectation
 *         E^Q[V_{k+1} | F_k] provided by the model, and
 *         Immediate exercise value: f(S_k).
 * The option value is the pointwise maximum of the two (early exercise rule).
 */
public class AmericanNonPathDependent extends AbstractNonPathDependentProduct {

	private final DoubleUnaryOperator payOffFunction;

	/**
	 * Creates an American option with given maturity and payoff.
	 *
	 * @param maturity        Contract maturity (model time units).
	 * @param payOffFunction  Payoff function f(S) (e.g.,s -> Math.max(K - s, 0.0) for a put).
	 */
	public AmericanNonPathDependent(double maturity, DoubleUnaryOperator payOffFunction) {
		super(maturity);
		this.payOffFunction = payOffFunction;
	}

	/**
	 * Performs backward induction with early exercise:
	 * Set terminal values at maturity: levels[n-k0] = f(S_T).
	 *   For k = n-1,...,k0:
	 *      Continuation: continuation = DF * E^Q[ levels[k+1-k0] | F_k ] via getConditionalExpectationRV(RandomVariable, int)
	 *      Exercise: exercise = f(S_k) via getSpotAtGivenTimeIndexRV(int).
	 *      American rule: levels[k-k0] = max(continuation, exercise).
	 *
	 * @param evaluationTime The time at which the value is requested (typically 0.0).
	 * @param model          The tree model (spot lattice, discounting, conditional expectations).
	 * @return Array of value random variables from index k0 to n;
	 */
	@Override
	public RandomVariable[] getValues(double evaluationTime, TreeModel model) {
		final int k0 = timeToIndex(evaluationTime, model);
		final int n  = model.getNumberOfTimes() - 1;
		final RandomVariable[] levels = new RandomVariable[n - k0 + 1];
		levels[n - k0] = model.getTransformedValuesAtGivenTimeRV(model.getLastTime(), getPayOffFunction());

		for (int k = n - 1; k >= k0; --k) {

			final RandomVariable continuation = model.getConditionalExpectationRV(levels[(k + 1) - k0], k);

			final RandomVariable spotK   = model.getSpotAtGivenTimeIndexRV(k);
			final RandomVariable exercise = spotK.apply(getPayOffFunction());

			levels[k -k0] = continuation.floor(exercise);

		}

		return levels;

	}

	@Override
	public DoubleUnaryOperator getPayOffFunction() {
		return this.payOffFunction;
	}
}

