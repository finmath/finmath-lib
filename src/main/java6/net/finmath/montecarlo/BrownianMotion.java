/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import java.io.Serializable;

import net.finmath.math.randomnumbers.MersenneTwister;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

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
 * defines the state of an object of this class, i.e., BrownianMotion for which
 * there parameters agree, generate the same random numbers.
 *
 * The class is immutable and thread safe. It uses lazy initialization.
 * 
 * @author Christian Fries
 * @version 1.6
 */
public class BrownianMotion implements BrownianMotionInterface, Serializable {

	private static final long serialVersionUID = -5430067621669213475L;

	private final TimeDiscretizationInterface						timeDiscretization;

	private final int			numberOfFactors;
	private final int			numberOfPaths;
	private final int			seed;

	private final AbstractRandomVariableFactory randomVariableFactory;

	private transient	RandomVariableInterface[][]	brownianIncrements;
	private final		Object						brownianIncrementsLazyInitLock = new Object();

	/**
	 * Construct a Brownian motion.
	 * 
	 * The constructor allows to set the factory to be used for the construction of
	 * random variables. This allows to generate Brownian increments represented
	 * by different implementations of the RandomVariableInterface (e.g. the RandomVariableLowMemory internally
	 * using float representations).
	 * 
	 * @param timeDiscretization The time discretization used for the Brownian increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param randomVariableFactory Factory to be used to create random variable.
	 */
	public BrownianMotion(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfFactors,
			int numberOfPaths,
			int seed,
			AbstractRandomVariableFactory randomVariableFactory) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;
		this.seed				= seed;

		this.randomVariableFactory = randomVariableFactory;

		this.brownianIncrements	= null; 	// Lazy initialization
	}

	/**
	 * Construct a Brownian motion.
	 * 
	 * @param timeDiscretization The time discretization used for the Brownian increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 */
	public BrownianMotion(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfFactors,
			int numberOfPaths,
			int seed) {
		this(timeDiscretization, numberOfFactors, numberOfPaths, seed, new RandomVariableFactory());
	}

	@Override
	public BrownianMotionInterface getCloneWithModifiedSeed(int seed) {
		return new BrownianMotion(getTimeDiscretization(), getNumberOfFactors(), getNumberOfPaths(), seed);
	}

	@Override
	public BrownianMotionInterface getCloneWithModifiedTimeDiscretization(TimeDiscretizationInterface newTimeDiscretization) {
		/// @TODO This can be improved: a complete recreation of the Brownian motion wouldn't be necessary!
		return new BrownianMotion(newTimeDiscretization, getNumberOfFactors(), getNumberOfPaths(), getSeed());
	}

	@Override
	public RandomVariableInterface getBrownianIncrement(int timeIndex, int factor) {

		// Thread safe lazy initialization
		synchronized(brownianIncrementsLazyInitLock) {
			if(brownianIncrements == null) doGenerateBrownianMotion();
		}

		/*
		 *  We return an immutable object which ensures that the receiver does not alter the data.
		 */
		return brownianIncrements[timeIndex][factor];
	}

	/**
	 * Lazy initialization of brownianIncrement. Synchronized to ensure thread safety of lazy init.
	 */
	private void doGenerateBrownianMotion() {
		if(brownianIncrements != null) return;	// Nothing to do

		// Create random number sequence generator
		MersenneTwister mersenneTwister = new MersenneTwister(seed);

		// Allocate memory
		double[][][] brownianIncrementsArray = new double[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors][numberOfPaths];

		// Pre-calculate square roots of deltaT
		double[] sqrtOfTimeStep = new double[timeDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<sqrtOfTimeStep.length; timeIndex++) {
			sqrtOfTimeStep[timeIndex] = Math.sqrt(timeDiscretization.getTimeStep(timeIndex));
		}   

		/*
		 * Generate normal distributed independent increments.
		 * 
		 * The inner loop goes over time and factors.
		 * MersenneTwister is known to generate "independent" increments in 623 dimensions.
		 * Since we want to generate independent streams (paths), the loop over path is the outer loop.
		 */
		for(int path=0; path<numberOfPaths; path++) {
			for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
				double sqrtDeltaT = sqrtOfTimeStep[timeIndex];
				// Generate uncorrelated Brownian increment
				for(int factor=0; factor<numberOfFactors; factor++) {
					double uniformIncrement = mersenneTwister.nextDouble();
					brownianIncrementsArray[timeIndex][factor][path] = net.finmath.math.functions.NormalDistribution.inverseCumulativeDistribution(uniformIncrement) * sqrtDeltaT;
				}				
			}
		}

		// Allocate memory for RandomVariable wrapper objects.
		brownianIncrements = new RandomVariableInterface[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];

		// Wrap the values in RandomVariable objects
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			double time = timeDiscretization.getTime(timeIndex+1);
			for(int factor=0; factor<numberOfFactors; factor++) {
				brownianIncrements[timeIndex][factor] =
						randomVariableFactory.createRandomVariable(time, brownianIncrementsArray[timeIndex][factor]);
			}
		}
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public int getNumberOfFactors() {
		return numberOfFactors;
	}

	@Override
	public int getNumberOfPaths() {
		return numberOfPaths;
	}

	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	/**
	 * @return Returns the seed.
	 */
	public int getSeed() {
		return seed;
	}

	public String toString() {
		return super.toString()
				+ "\n" + "timeDiscretization: " + timeDiscretization.toString()
				+ "\n" + "numberOfPaths: " + numberOfPaths
				+ "\n" + "numberOfFactors: " + numberOfFactors
				+ "\n" + "seed: " + seed;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		BrownianMotion that = (BrownianMotion) o;

		if (numberOfFactors != that.numberOfFactors) return false;
		if (numberOfPaths != that.numberOfPaths) return false;
		if (seed != that.seed) return false;
		if (!timeDiscretization.equals(that.timeDiscretization)) return false;

		return true;
	}

	@Override
	public RandomVariableInterface getIncrement(int timeIndex, int factor) {
		return getBrownianIncrement(timeIndex, factor);
	}

	@Override
	public int hashCode() {
		int result = timeDiscretization.hashCode();
		result = 31 * result + numberOfFactors;
		result = 31 * result + numberOfPaths;
		result = 31 * result + seed;
		return result;
	}
}
