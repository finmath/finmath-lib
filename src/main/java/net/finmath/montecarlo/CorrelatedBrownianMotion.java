/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 25.05.2013
 */

package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Provides a correlated Brownian motion from given (independent) increments
 * and a given matrix of factor loadings.
 * 
 * The i-th factor of this BrownianMotion is <i>dW<sub>i</sub></i> where
 * <i>dW<sub>i</sub> = f<sub>i,1</sub> dU<sub>1</sub> + ... + f<sub>i,m</sub> dU<sub>m</sub></i>
 * for <i>i = 1, ..., n</i>.
 * 
 * Here <i>f<sub>i,j</sub></i> are the factor loadings, an <i>n &times; m</i>-matrix.
 * 
 * If <i>dU<sub>j</sub></i> are independent, then <i>dW<sub>i</sub> dW<sub>k</sub> = &rho;<sub>i,k</sub> dt</i>
 * where <i>&rho;<sub>i,k</sub> = f<sub>i</sub> &cdot; f<sub>j</sub></i>.
 * 
 * Note: It is possible to create this class with a Brownian motion <i>U</i> which is
 * already correlated. The factors loadings will be applied accordingly.
 * 
 * @author Christian Fries
 */
public class CorrelatedBrownianMotion implements BrownianMotionInterface {

	private BrownianMotionInterface	uncollelatedFactors;
	private double[][]				factorLoadings;

	/**
	 * Create a correlated Brownian motion from given independent increments
	 * and a given matrix of factor loadings.
	 * 
	 * The i-th factor of this BrownianMotion is <i>dW<sub>i</sub></i> where
	 * <i>dW<sub>i</sub> = f<sub>i,1</sub> dU<sub>1</sub> + ... + f<sub>i,m</sub> dU<sub>m</sub></i>
	 * for <i>i = 1, ..., n</i>.
	 * 
	 * Here <i>f<sub>i,j</sub></i> are the factor loadings, an <i>n &times; m</i>-matrix.
	 * 
	 * If <i>dU<sub>j</sub></i> are independent, then <i>dW<sub>i</sub> dW<sub>k</sub> = &rho;<sub>i,k</sub> dt</i>
	 * where <i>&rho;<sub>i,k</sub> = f<sub>i</sub> &cdot; f<sub>j</sub></i>.
	 * 
	 * @param uncollelatedFactors The Brownian motion providing the (uncorrelated) factors <i>dU<sub>j</sub></i>.
	 * @param factorLoadings The factor loadings <i>f<sub>i,j</sub></i>.
	 */
	public CorrelatedBrownianMotion(BrownianMotionInterface uncollelatedFactors,
			double[][] factorLoadings) {
		super();
		this.uncollelatedFactors	= uncollelatedFactors;
		this.factorLoadings			= factorLoadings;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getBrownianIncrement(int, int)
	 */
	@Override
	public RandomVariableInterface getBrownianIncrement(int timeIndex, int factor) {
		RandomVariableInterface brownianIncrement = new RandomVariable(0.0);
		for(int factorIndex=0; factorIndex<factorLoadings[factor].length; factorIndex++) {
			if(factorLoadings[factor][factorIndex] != 0) {
				RandomVariableInterface independentFactor = uncollelatedFactors.getBrownianIncrement(timeIndex, factorIndex);
				brownianIncrement = brownianIncrement.addProduct(independentFactor, factorLoadings[factor][factorIndex]);
			}
		}
		return brownianIncrement;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getTimeDiscretization()
	 */
	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return uncollelatedFactors.getTimeDiscretization();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getNumberOfFactors()
	 */
	@Override
	public int getNumberOfFactors() {
		return factorLoadings.length;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getNumberOfPaths()
	 */
	@Override
	public int getNumberOfPaths() {
		return uncollelatedFactors.getNumberOfFactors();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getCloneWithModifiedSeed(int)
	 */
	@Override
	public BrownianMotionInterface getCloneWithModifiedSeed(int seed) {
		return new CorrelatedBrownianMotion(uncollelatedFactors.getCloneWithModifiedSeed(seed), factorLoadings);
	}

}
