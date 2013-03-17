/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import java.io.Serializable;

import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;
import cern.jet.random.engine.MersenneTwister64;

/**
 * Implementation of <code>BrownianMotionInterface</code>.
 * See <code>BrownianMotionInterface</code> for further comments. 
 * 
 * The class is immutable and thread safe. It uses lazy initialization.
 * 
 * @author Christian Fries
 * @version 1.5
 */
public class BrownianMotion implements BrownianMotionInterface, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5430067621669213475L;

	private TimeDiscretizationInterface						timeDiscretization;

	private int			numberOfFactors;
	private int			numberOfPaths;
	private int			seed;

	private transient ImmutableRandomVariableInterface[][]	brownianIncrements;	

	/**
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
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;
		this.seed				= seed;

		this.brownianIncrements	= null; 	// Lazy initialization
	}

	public Object getCloneWithModifiedSeed(int seed) {
		return new BrownianMotion(getTimeDiscretization(), getNumberOfFactors(), getNumberOfPaths(), seed);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getBrownianIncrement(int, int)
	 */
	public ImmutableRandomVariableInterface getBrownianIncrement(int timeIndex, int factor) {
		// Thread safe lazy initialization
		synchronized(this) {
			if(brownianIncrements == null) doGenerateBrownianMotion();
		}

		/*
		 *  For performance reasons we return directly the stored data (no defensive copy).
		 *  We return an immutable object to ensure that the receiver does not alter the data.
		 */
		return brownianIncrements[timeIndex][factor];
	}

	/**
	 * Lazy initialization of brownianIncrement. Synchronized to ensure thread safety of lazy init.
	 */
	private void doGenerateBrownianMotion() {
		if(brownianIncrements != null) return;	// Nothing to do

		// Create random number sequence generator (we use MersenneTwister64 from colt)
		MersenneTwister64		mersenneTwister		= new MersenneTwister64(seed);

		// Allocate memory
		double[][][] brownianIncrementsArray = new double[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors][numberOfPaths];

		// Pre-calculate square roots of deltaT
		double[] sqrtOfTimeStep = new double[timeDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<sqrtOfTimeStep.length; timeIndex++) {
			sqrtOfTimeStep[timeIndex] = Math.sqrt(timeDiscretization.getTimeStep(timeIndex));
		}   

		/*
		 * Generate normal distribued independent increments.
		 * 
		 * The inner loop goes over time and factors.
		 * MersenneTwister is known to generate "independent" increments in 623 dimensions.
		 * Since we want to generate indepentent streams (paths), the loop over path is the outer loop.
		 */
		for(int path=0; path<numberOfPaths; path++) {
			for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
				double sqrtDeltaT = sqrtOfTimeStep[timeIndex];
				// Generate uncorrelated Brownian increment
				for(int factor=0; factor<numberOfFactors; factor++) {
					double uniformIncement = mersenneTwister.nextDouble();
					brownianIncrementsArray[timeIndex][factor][path] = net.finmath.functions.NormalDistribution.inverseCumulativeDistribution(uniformIncement) * sqrtDeltaT;
				}				
			}
		}

		// Allocate memory for RandomVariable wrapper objects.
		brownianIncrements = new RandomVariable[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];

		// Wrap the values in RandomVariable objects
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			for(int factor=0; factor<numberOfFactors; factor++) {
				brownianIncrements[timeIndex][factor] = new RandomVariable(timeDiscretization.getTime(timeIndex+1), brownianIncrementsArray[timeIndex][factor]);			
			}
		}
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getTimeDiscretization()
	 */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getNumberOfFactors()
	 */
	public int getNumberOfFactors() {
		return numberOfFactors;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.BrownianMotionInterface#getNumberOfPaths()
	 */
	public int getNumberOfPaths() {
		return numberOfPaths;
	}

	/**
	 * @return Returns the seed.
	 */
	public int getSeed() {
		return seed;
	}
}
