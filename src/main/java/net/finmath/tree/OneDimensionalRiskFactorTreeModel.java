package net.finmath.tree;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

/**
 * This abstract class encapsulates the logic of one-dimensional trees for the simulation of a
 * risk factor. At this level, the class could be used for equities or interest rates (e.g.
 * the Hull-White short rate model).
 * 
 * @author Carlo Andrea Tramentozzi, Alessandro Gnoatto
 */
public abstract class OneDimensionalRiskFactorTreeModel implements TreeModel {
	
	/** Cache of level spot S_k */
	private final List<RandomVariable> riskFactorLevels = new ArrayList<>();

	/**
	 * Return number of states at level k (binomial: k+1; trinomial: 2k+1)
	 * @param k
	 * @return number of states at level k
	 */
	public abstract int statesAt(int k);

	/**
	 * Builds all the realizations X_k[i]
	 * @param k
	 * @return
	 */
	protected abstract RandomVariable buildSpotLevel(int k);


	/**
	 * Discounted Conditional expectation: from v_{k+1} (array) to v_k (array)
	 * @param vNext
	 * @param k
	 * @return the conditonal expectation
	 */
	protected abstract RandomVariable conditionalExpectation(RandomVariable vNext, int k);


	/**
	 * Builds (if missing) and return  X_k as array, using cache.
	 * @param k
	 * @return the risk factor at level k
	 */
	protected final RandomVariable ensureRiskFactorLevelArray(int k) {
		while(riskFactorLevels.size() <= k) {
			int next = riskFactorLevels.size();
			riskFactorLevels.add(buildSpotLevel(next));
		}
		return riskFactorLevels.get(k);
	}
	
	@Override
	public RandomVariable getTransformedValuesAtGivenTimeRV(double time, DoubleUnaryOperator transformFunction) {
		int k = (int) Math.round(time / getTimeStep());
		// Checking param to avoid out of bound exception
		if (k < 0) k = 0;
		if (k > getNumberOfTimes() - 1) k = getNumberOfTimes() - 1;

		RandomVariable sRV = ensureRiskFactorLevelArray(k);
		double[] s = sRV.getRealizations();
		double[] v = new double[s.length];
		for (int i = 0; i < s.length; i++) {
			v[i] = transformFunction.applyAsDouble(s[i]);
		}
		return new RandomVariableFromDoubleArray(k * getTimeStep(), v);
	}

	@Override
	public RandomVariable getConditionalExpectationRV(RandomVariable vNext, int k) {
		RandomVariable vK = conditionalExpectation(vNext, k); // hook
		return new RandomVariableFromDoubleArray(k * getTimeStep(), vK.getRealizations());
	}

	@Override
	public RandomVariable getSpotAtGivenTimeIndexRV(int k) {
		RandomVariable sRV = ensureRiskFactorLevelArray(k);
		return new RandomVariableFromDoubleArray(k * getTimeStep(), sRV.getRealizations());
	}

}
