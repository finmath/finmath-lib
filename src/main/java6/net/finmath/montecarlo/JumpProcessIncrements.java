/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import java.io.Serializable;
import java.util.Arrays;

import net.finmath.functions.PoissonDistribution;
import net.finmath.randomnumbers.MersenneTwister;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implementation of a time-discrete n-dimensional jump process
 * <i>J = (J<sub>1</sub>,...,J<sub>n</sub>)</i> where <i>J<sub>i</sub></i> is
 * a Poisson jump process and <i>J<sub>i</sub></i>, <i>J<sub>j</sub></i> are
 * independent for <i>i</i> not equal <i>j</i>.
 * 
 * Here the dimension <i>n</i> is called factors since the increments are used to
 * generate multi-dimensional multi-factor processes and there one might
 * use a different number of factors to generate processes of different
 * dimension. 
 *
 * The quadruppel (time discretization, jumpIntensities, number of paths, seed)
 * defines the state of an object of this class.
 *
 * The class is immutable and thread safe. It uses lazy initialization.
 * 
 * @author Christian Fries
 * @version 1.6
 */
public class JumpProcessIncrements implements IndependentIncrementsInterface, Serializable {

	private static final long serialVersionUID = -5430067621669213475L;

	private final TimeDiscretizationInterface						timeDiscretization;

	private final int			numberOfPaths;
	private final int			seed;

	private final double[]		jumpIntensities;

	private final AbstractRandomVariableFactory randomVariableFactory;

	private transient	RandomVariableInterface[][]	increments;
	private final		Object						incrementsLazyInitLock = new Object();

	/**
	 * Construct a jump process.
	 * 
	 * The constructor allows to set the factory to be used for the construction of
	 * random variables. This allows to generate increments represented
	 * by different implementations of the RandomVariableInterface (e.g. the RandomVariableLowMemory internally
	 * using float representations).
	 * 
	 * @param timeDiscretization The time discretization used for the increments.
	 * @param jumpIntensities The jump intensities, one for each factor.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param randomVariableFactory Factory to be used to create random variable.
	 */
	public JumpProcessIncrements(
			TimeDiscretizationInterface timeDiscretization,
			double[] jumpIntensities,
			int numberOfPaths,
			int seed,
			AbstractRandomVariableFactory randomVariableFactory) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.jumpIntensities	= jumpIntensities;
		this.numberOfPaths		= numberOfPaths;
		this.seed				= seed;

		this.randomVariableFactory = randomVariableFactory;

		this.increments	= null; 	// Lazy initialization
	}

	/**
	 * Construct a jump process.
	 * 
	 * @param timeDiscretization The time discretization used for the Brownian increments.
	 * @param jumpIntensities The vector of jump intensities, one intensity for each factor.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 */
	public JumpProcessIncrements(
			TimeDiscretizationInterface timeDiscretization,
			double[] jumpIntensities,
			int numberOfPaths,
			int seed) {
		this(timeDiscretization, jumpIntensities, numberOfPaths, seed, new RandomVariableFactory());
	}

	@Override
	public JumpProcessIncrements getCloneWithModifiedSeed(int seed) {
		return new JumpProcessIncrements(getTimeDiscretization(), jumpIntensities, getNumberOfPaths(), seed);
	}

	@Override
	public JumpProcessIncrements getCloneWithModifiedTimeDiscretization(TimeDiscretizationInterface newTimeDiscretization) {
		/// @TODO This can be improved: a complete recreation of the Brownian motion wouldn't be necessary!
		return new JumpProcessIncrements(newTimeDiscretization, jumpIntensities, getNumberOfPaths(), getSeed());
	}

	@Override
	public RandomVariableInterface[] getIncrement(int timeIndex)
	{
		RandomVariableInterface[] increment = new RandomVariableInterface[getNumberOfFactors()];
		for(int factorIndex = 0; factorIndex<getNumberOfFactors(); factorIndex++) {
			increment[factorIndex] = getIncrement(timeIndex, factorIndex);
		}
		return increment;
	}

	@Override
	public RandomVariableInterface getIncrement(int timeIndex, int factor) {

		// Thread safe lazy initialization
		synchronized(incrementsLazyInitLock) {
			if(increments == null) doGenerateIncrements();
		}

		/*
		 *  We return an immutable object which ensures that the receiver does not alter the data.
		 */
		return increments[timeIndex][factor];
	}

	/**
	 * Lazy initialization of brownianIncrement. Synchronized to ensure thread safety of lazy init.
	 */
	private void doGenerateIncrements() {
		if(increments != null) return;	// Nothing to do

		// Create random number sequence generator
		MersenneTwister mersenneTwister = new MersenneTwister(seed);

		// Allocate memory
		double[][][] incrementsArray = new double[timeDiscretization.getNumberOfTimeSteps()][jumpIntensities.length][numberOfPaths];

		// Pre-calculate Poisson distributions
		PoissonDistribution[][] poissonDistribution = new PoissonDistribution[timeDiscretization.getNumberOfTimeSteps()][jumpIntensities.length];
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			for(int factorIndex=0; factorIndex<jumpIntensities.length; factorIndex++) {
				poissonDistribution[timeIndex][factorIndex] = new PoissonDistribution(timeDiscretization.getTimeStep(timeIndex)*jumpIntensities[factorIndex]);
			}
		} 


		/*
		 * Generate independent increments.
		 * 
		 * The inner loop goes over time and factors.
		 * MersenneTwister is known to generate "independent" increments in 623 dimensions.
		 * Since we want to generate independent streams (paths), the loop over path is the outer loop.
		 */
		for(int pathIndex=0; pathIndex<numberOfPaths; pathIndex++) {
			for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
				for(int factorIndex=0; factorIndex<jumpIntensities.length; factorIndex++) {
					double uniformIncrement = mersenneTwister.nextDouble();
					double numberOfJumps = poissonDistribution[timeIndex][factorIndex].inverseCumulativeDistribution(uniformIncrement);

					incrementsArray[timeIndex][factorIndex][pathIndex] = numberOfJumps;
				}
			}
		}

		// Allocate memory for RandomVariable wrapper objects.
		increments = new RandomVariableInterface[timeDiscretization.getNumberOfTimeSteps()][jumpIntensities.length];

		// Wrap the values in RandomVariable objects
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			double time = timeDiscretization.getTime(timeIndex+1);
			for(int factor=0; factor<jumpIntensities.length; factor++) {
				increments[timeIndex][factor] =
						randomVariableFactory.createRandomVariable(time, incrementsArray[timeIndex][factor]);
			}
		}
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public int getNumberOfFactors() {
		return jumpIntensities.length;
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

	@Override
	public String toString() {
		return "JumpProcessIncrements [timeDiscretization=" + timeDiscretization + ", numberOfPaths=" + numberOfPaths
				+ ", seed=" + seed + ", jumpIntensities=" + Arrays.toString(jumpIntensities)
				+ ", randomVariableFactory=" + randomVariableFactory + "]";
	}
}
