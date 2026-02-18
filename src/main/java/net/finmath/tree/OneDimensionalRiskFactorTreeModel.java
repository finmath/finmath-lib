package net.finmath.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

/**
 * This abstract class encapsulates the logic of one-dimensional trees for the simulation of a risk factor.
 * At this level, the class could be used for equities or interest rates (e.g. the Hull-White short rate model).
 *
 * @author Carlo Andrea Tramentozzi
 * @author Alessandro Gnoatto
 */
public abstract class OneDimensionalRiskFactorTreeModel implements TreeModel {

	/** Cache of level spot S_k. */
	private final List<RandomVariable> riskFactorLevels = new ArrayList<>();

	/**
	 * Returns the number of states at level {@code k} (binomial: {@code k + 1}; trinomial: {@code 2k + 1}).
	 *
	 * @param k The level index.
	 * @return The number of states at level {@code k}.
	 */
	public abstract int statesAt(int k);

	/**
	 * Builds all the realizations {@code X_k[i]}.
	 *
	 * @param k The level index.
	 * @return The spot (risk factor) values at level {@code k}.
	 */
	protected abstract RandomVariable buildSpotLevel(int k);

	/**
	 * Discounted conditional expectation: from {@code v_{k+1}} to {@code v_k}.
	 *
	 * @param vNext The values at the next level.
	 * @param k The current level index.
	 * @return The conditional expectation.
	 */
	protected abstract RandomVariable conditionalExpectation(RandomVariable vNext, int k);

	/**
	 * Builds (if missing) and returns {@code X_k} using the cache.
	 *
	 * @param k The level index.
	 * @return The risk factor at level {@code k}.
	 */
	protected final RandomVariable ensureRiskFactorLevelArray(int k) {
		while(riskFactorLevels.size() <= k) {
			final int next = riskFactorLevels.size();
			riskFactorLevels.add(buildSpotLevel(next));
		}
		return riskFactorLevels.get(k);
	}

	@Override
	public RandomVariable getTransformedValuesAtGivenTimeRV(final double time, final DoubleUnaryOperator transformFunction) {
		int k = (int) Math.round(time / getTimeStep());

		// Checking param to avoid out of bound exception.
		if(k < 0) {
			k = 0;
		}
		if(k > getNumberOfTimes() - 1) {
			k = getNumberOfTimes() - 1;
		}

		final RandomVariable sRV = ensureRiskFactorLevelArray(k);
		final double[] s = sRV.getRealizations();
		final double[] v = new double[s.length];
		for(int i = 0; i < s.length; i++) {
			v[i] = transformFunction.applyAsDouble(s[i]);
		}
		return new RandomVariableFromDoubleArray(k * getTimeStep(), v);
	}

	@Override
	public RandomVariable getConditionalExpectationRV(final RandomVariable vNext, final int k) {
		final RandomVariable vK = conditionalExpectation(vNext, k); // hook
		return new RandomVariableFromDoubleArray(k * getTimeStep(), vK.getRealizations());
	}

	@Override
	public RandomVariable getSpotAtGivenTimeIndexRV(final int k) {
		final RandomVariable sRV = ensureRiskFactorLevelArray(k);
		return new RandomVariableFromDoubleArray(k * getTimeStep(), sRV.getRealizations());
	}
}
