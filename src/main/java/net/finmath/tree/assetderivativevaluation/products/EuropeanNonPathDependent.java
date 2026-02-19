package net.finmath.tree.assetderivativevaluation.products;

import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.TreeModel;

import java.util.function.DoubleUnaryOperator;

/**
 * European (non–path-dependent) option priced on a recombining TreeModel.
 * The payoff f(S_T) is applied only at maturity and the price is obtained
 * by backward induction using the model’s discounted conditional expectation.
 * pricing uses evaluationTime = 0.0 => k0 = 0.
 */
public class EuropeanNonPathDependent extends AbstractNonPathDependentProduct {

	/**
	 * Creates a European option with given maturity and payoff.
	 *
	 * @param maturity        Contract maturity (model time units).
	 * @param payOffFunction  Payoff function f(S) applied at T
	 *                        (e.g., s -> Math.max(s-K,0.0) for a call).
	 */
	public EuropeanNonPathDependent(double maturity, DoubleUnaryOperator payOffFunction){
		super(maturity,payOffFunction);
	}

	/**
	 * Performs backward induction under the given tree model:
	 * @param evaluationTime The time at which the value is requested (usually 0.0).
	 * @param model           The tree model providing spot lattice and CE/discounting.
	 * @return An array of RandomVariable with values from time index k0 to n.
	 *         By convention, values[0] corresponds to k0 and
	 *         values[n-k0] corresponds to maturity.
	 */
	@Override
	public RandomVariable[] getValues(double evaluationTime, TreeModel model) {
		final int k0  = timeToIndex(evaluationTime,model);
		final int n = model.getNumberOfTimes()-1;
		final RandomVariable[] values = new RandomVariable[n -k0 +1];
		values[n-k0] = model.getTransformedValuesAtGivenTimeRV(model.getLastTime(),getPayOffFunction());

		for (int timeIndex = n - 1; timeIndex >= k0; timeIndex--) {
			values[timeIndex - k0] = model.getConditionalExpectationRV(values[(timeIndex+1)], timeIndex);
		}
		return values;
	}

	}

