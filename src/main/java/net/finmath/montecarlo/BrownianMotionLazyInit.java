/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import net.finmath.time.TimeDiscretization;

/**
 * Implementation of a time-discrete n-dimensional Brownian motion
 * <i>W = (W<sub>1</sub>,...,W<sub>n</sub>)</i> where <i>W<sub>i</sub></i> is
 * a Brownian motion and <i>W<sub>i</sub></i>, <i>W<sub>j</sub></i> are
 * independent for <i>i</i> not equal <i>j</i>.
 *
 * For a correlated Brownian motion with see
 * {@link net.finmath.montecarlo.CorrelatedBrownianMotion}.
 *
 * Here the dimension <i>n</i> is called factors since this Brownian motion is used to
 * generate multi-dimensional multi-factor Ito processes and there one might
 * use a different number of factors to generate Ito processes of different
 * dimension.
 *
 * The quadruppel (time discretization, number of factors, number of paths, seed)
 * defines the state of an object of this class, i.e., BrownianMotionLazyInit for which
 * there parameters agree, generate the same random numbers.
 *
 * The class is immutable and thread safe. It uses lazy initialization.
 *
 * @author Christian Fries
 * @version 1.6
 * @deprecated Refactor rename. Please use BrownianMotionFromMersenneRandomNumbers instead.
 */
@Deprecated
public class BrownianMotionLazyInit extends BrownianMotionFromMersenneRandomNumbers {

	private static final long serialVersionUID = -5430067621669213475L;

	/**
	 * Construct a Brownian motion.
	 *
	 * The constructor allows to set the factory to be used for the construction of
	 * random variables. This allows to generate Brownian increments represented
	 * by different implementations of the RandomVariable (e.g. the RandomVariableFromFloatArray internally
	 * using float representations).
	 *
	 * @param timeDiscretization The time discretization used for the Brownian increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param randomVariableFactory Factory to be used to create random variable.
	 */
	public BrownianMotionLazyInit(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final int seed,
			final RandomVariableFactory randomVariableFactory) {
		super(timeDiscretization, numberOfFactors, numberOfPaths, seed, randomVariableFactory);
	}

	/**
	 * Construct a Brownian motion.
	 *
	 * @param timeDiscretization The time discretization used for the Brownian increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 */
	public BrownianMotionLazyInit(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final int seed) {
		this(timeDiscretization, numberOfFactors, numberOfPaths, seed, new RandomVariableFromArrayFactory());
	}
}
