/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 25.05.2013
 */

package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Provides a correlated Brownian motion from given (independent) increments
 * and a given matrix of factor loadings.
 *
 * The i-th factor of this BrownianMotionLazyInit is <i>dW<sub>i</sub></i> where
 * <i>dW<sub>i</sub> = f<sub>i,1</sub> dU<sub>1</sub> + ... + f<sub>i,m</sub> dU<sub>m</sub></i>
 * for <i>i = 1, ..., n</i>.
 *
 * Here <i>f<sub>i,j</sub></i> are the factor loadings, an <i>n &times; m</i>-matrix.
 *
 * If <i>dU<sub>j</sub></i> are independent, then <i>dW<sub>i</sub> dW<sub>k</sub> = &rho;<sub>i,k</sub> dt</i>
 * where <i>&rho;<sub>i,k</sub> = f<sub>i</sub> &middot; f<sub>j</sub></i>.
 *
 * Note: It is possible to create this class with a Brownian motion <i>U</i> which is
 * already correlated. The factors loadings will be applied accordingly.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class CorrelatedBrownianMotion implements BrownianMotion {

	private final BrownianMotion	uncollelatedFactors;
	private final double[][]				factorLoadings;

	/**
	 * Create a correlated Brownian motion from given independent increments
	 * and a given matrix of factor loadings.
	 *
	 * The i-th factor of this BrownianMotionLazyInit is <i>dW<sub>i</sub></i> where
	 * <i>dW<sub>i</sub> = f<sub>i,1</sub> dU<sub>1</sub> + ... + f<sub>i,m</sub> dU<sub>m</sub></i>
	 * for <i>i = 1, ..., n</i>.
	 *
	 * Here <i>f<sub>i,j</sub></i> are the factor loadings, an <i>n &times; m</i>-matrix.
	 *
	 * If <i>dU<sub>j</sub></i> are independent, then <i>dW<sub>i</sub> dW<sub>k</sub> = &rho;<sub>i,k</sub> dt</i>
	 * where <i>&rho;<sub>i,k</sub> = f<sub>i</sub> &middot; f<sub>j</sub></i>.
	 *
	 * @param uncollelatedFactors The Brownian motion providing the (uncorrelated) factors <i>dU<sub>j</sub></i>.
	 * @param factorLoadings The factor loadings <i>f<sub>i,j</sub></i>.
	 */
	public CorrelatedBrownianMotion(final BrownianMotion uncollelatedFactors,
			final double[][] factorLoadings) {
		super();
		this.uncollelatedFactors	= uncollelatedFactors;
		this.factorLoadings			= factorLoadings;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotion#getBrownianIncrement(int, int)
	 */
	@Override
	public RandomVariable getBrownianIncrement(final int timeIndex, final int factor) {
		RandomVariable brownianIncrement = new RandomVariableFromDoubleArray(0.0);
		for(int factorIndex=0; factorIndex<factorLoadings[factor].length; factorIndex++) {
			if(factorLoadings[factor][factorIndex] != 0) {
				final RandomVariable independentFactor = uncollelatedFactors.getBrownianIncrement(timeIndex, factorIndex);
				brownianIncrement = brownianIncrement.addProduct(independentFactor, factorLoadings[factor][factorIndex]);
			}
		}
		return brownianIncrement;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotion#getTimeDiscretization()
	 */
	@Override
	public TimeDiscretization getTimeDiscretization() {
		return uncollelatedFactors.getTimeDiscretization();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotion#getNumberOfFactors()
	 */
	@Override
	public int getNumberOfFactors() {
		return factorLoadings.length;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotion#getNumberOfPaths()
	 */
	@Override
	public int getNumberOfPaths() {
		return uncollelatedFactors.getNumberOfPaths();
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return uncollelatedFactors.getRandomVariableForConstant(value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotion#getCloneWithModifiedSeed(int)
	 */
	@Override
	public BrownianMotion getCloneWithModifiedSeed(final int seed) {
		return new CorrelatedBrownianMotion(uncollelatedFactors.getCloneWithModifiedSeed(seed), factorLoadings);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotion#getCloneWithModifiedTimeDiscretization(net.finmath.time.TimeDiscretization)
	 */
	@Override
	public BrownianMotion getCloneWithModifiedTimeDiscretization(final TimeDiscretization newTimeDiscretization) {
		return new CorrelatedBrownianMotion(uncollelatedFactors.getCloneWithModifiedTimeDiscretization(newTimeDiscretization), factorLoadings);
	}

	@Override
	public RandomVariable getIncrement(final int timeIndex, final int factor) {
		return getBrownianIncrement(timeIndex, factor);
	}
}
