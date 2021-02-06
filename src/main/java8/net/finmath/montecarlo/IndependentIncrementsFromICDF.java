/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import java.io.IOException;
import java.io.Serializable;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import net.finmath.randomnumbers.MersenneTwister;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implementation of a time-discrete n-dimensional sequence of independent increments
 * <i>W = (W<sub>1</sub>,...,W<sub>n</sub>)</i> form a given set of inverse
 * cumulative distribution functions.
 *
 * Independent increments is a sequence of independent random variables index
 * by the time index associated with the time discretization. At each time step
 * the increment is a d-dimensional random variable \( Z(t_{i}) \), where d is <code>numberOfFactors</code>.
 * where each component of \( Z_{j}(t_{i}) \) is given by
 * \[
 * 	Z_{j}(t_{i}) = ICDF_{i,j}(U_{i,j})
 * \]
 * for a sequence of independent uniform distributes random variables U_{i,j}.
 *
 * The inverse cumulative distribution functions \( ICDF_{i,j} \) are given by
 * <code>inverseCumulativeDistributionFunctions</code> as the
 * map \( i \mapsto ( j \mapsto ICDF_{i,j} ) \) (here i is the time index and j is the factor (component).
 *
 * Each \( U_{i,j} \) is samples using <code>numberOfPaths</code>.
 *
 * The class is immutable and thread safe. It uses lazy initialization.
 *
 * @author Christian Fries
 * @version 1.6
 */
public class IndependentIncrementsFromICDF implements IndependentIncrements, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 6270884840989559532L;

	private final TimeDiscretization						timeDiscretization;

	private final int			numberOfFactors;
	private final int			numberOfPaths;
	private final int			seed;

	private final RandomVariableFactory randomVariableFactory;

	private transient	RandomVariable[][]	increments;
	private	transient	Object						incrementsLazyInitLock = new Object();

	private final IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions;

	/**
	 * Construct  the simulation of independent increments.
	 *
	 * Independent increments is a sequence of independent random variables index
	 * by the time index associated with the time discretization. At each time step
	 * the increment is a d-dimensional random variable \( Z(t_{i}) \), where d is <code>numberOfFactors</code>.
	 * where each component of \( Z_{j}(t_{i}) \) is given by
	 * \[
	 * 	Z_{j}(t_{i}) = ICDF_{i,j}(U_{i,j})
	 * \]
	 * for a sequence of independent uniform distributes random variables U_{i,j}.
	 *
	 * The inverse cumulative distribution functions \( ICDF_{i,j} \) are given by
	 * <code>inverseCumulativeDistributionFunctions</code> as the
	 * map \( i \mapsto ( j \mapsto ICDF_{i,j} ) \) (here i is the time index and j is the factor (component).
	 *
	 * Each \( U_{i,j} \) is samples using <code>numberOfPaths</code>.
	 *
	 * The constructor allows to set the factory to be used for the construction of
	 * random variables. This allows to generate increments represented
	 * by different implementations of the RandomVariable
	 * (e.g. the RandomVariableFromFloatArray internally using float representations).
	 *
	 * @param timeDiscretization The time discretization used for the increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param inverseCumulativeDistributionFunctions A map from the timeIndices to a map from the from the factors to the corresponding inverse cumulative distribution function.
	 * @param randomVariableFactory Factory to be used to create random variable.
	 */
	public IndependentIncrementsFromICDF(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final int seed,
			final IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions,
			final RandomVariableFactory randomVariableFactory) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;
		this.seed				= seed;

		this.inverseCumulativeDistributionFunctions = inverseCumulativeDistributionFunctions;
		this.randomVariableFactory = randomVariableFactory;

		increments	= null; 	// Lazy initialization
	}

	/**
	 * Construct  the simulation of independet increments.
	 *
	 * The independent increments is a sequence of independent random variables index
	 * by the time index associated with the time discretization. At each time step
	 * the increment is a d-dimensional random variable \( Z(t_{i}) \), where d is <code>numberOfFactors</code>.
	 * where each component of \( Z_{j}(t_{i}) \) is given by
	 * \[
	 * 	Z_{j}(t_{i}) = ICDF_{i,j}(U_{i,j})
	 * \]
	 * for a sequence of independent uniform distributes random variables U_{i,j}.
	 *
	 * The inverse cumulative distribution functions \( ICDF_{i,j} \) are given by
	 * <code>inverseCumulativeDistributionFunctions</code> as the
	 * map \( i \mapsto ( j \mapsto ICDF_{i,j} ) \) (here i is the time index and j is the factor (component).
	 *
	 * Each \( U_{i,j} \) is samples using <code>numberOfPaths</code>.
	 *
	 * @param timeDiscretization The time discretization used for the increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param inverseCumulativeDistributionFunctions A map from the timeIndices to a map from the from the factors to the corresponding inverse cumulative distribution function.
	 */
	public IndependentIncrementsFromICDF(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final int seed,
			final IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions) {
		this(timeDiscretization, numberOfFactors, numberOfPaths, seed, inverseCumulativeDistributionFunctions, new RandomVariableFromArrayFactory());
	}

	@Override
	public IndependentIncrements getCloneWithModifiedSeed(final int seed) {
		return new IndependentIncrementsFromICDF(getTimeDiscretization(), getNumberOfFactors(), getNumberOfPaths(), seed, inverseCumulativeDistributionFunctions, randomVariableFactory);
	}

	@Override
	public IndependentIncrements getCloneWithModifiedTimeDiscretization(final TimeDiscretization newTimeDiscretization) {
		return new IndependentIncrementsFromICDF(newTimeDiscretization, getNumberOfFactors(), getNumberOfPaths(), getSeed(), inverseCumulativeDistributionFunctions, randomVariableFactory);
	}

	@Override
	public RandomVariable getIncrement(final int timeIndex, final int factor) {

		// Thread safe lazy initialization
		synchronized(incrementsLazyInitLock) {
			if(increments == null) {
				doGenerateIncrements();
			}
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
		if(increments != null) {
			return;	// Nothing to do
		}

		// Create random number sequence generator
		final MersenneTwister			mersenneTwister		= new MersenneTwister(seed);

		// Allocate memory
		final double[][][] incrementsArray = new double[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors][numberOfPaths];

		// Pre-fetch icdfs
		final DoubleUnaryOperator[][] inverseCumulativeDistributionFunctions = new DoubleUnaryOperator[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			for(int factor=0; factor<numberOfFactors; factor++) {
				inverseCumulativeDistributionFunctions[timeIndex][factor] = this.inverseCumulativeDistributionFunctions.apply(timeIndex).apply(factor);
			}
		}

		/*
		 * Generate independent increments, each having distribution specified by ICDF.
		 *
		 * The inner loop goes over time and factors.
		 * MersenneTwister is known to generate "independent" increments in 623 dimensions.
		 * Since we want to generate independent streams (paths), the loop over path is the outer loop.
		 */
		for(int path=0; path<numberOfPaths; path++) {
			for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
				// Generate uncorrelated Brownian increment
				for(int factor=0; factor<numberOfFactors; factor++) {
					final double uniformIncrement = mersenneTwister.nextDouble();
					incrementsArray[timeIndex][factor][path] = inverseCumulativeDistributionFunctions[timeIndex][factor].applyAsDouble(uniformIncrement);
				}
			}
		}

		// Allocate memory for RandomVariableFromDoubleArray wrapper objects.
		increments = new RandomVariable[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];

		// Wrap the values in RandomVariableFromDoubleArray objects
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final double time = timeDiscretization.getTime(timeIndex+1);
			for(int factor=0; factor<numberOfFactors; factor++) {
				increments[timeIndex][factor] =
						randomVariableFactory.createRandomVariable(time, incrementsArray[timeIndex][factor]);
			}
		}
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
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
	public RandomVariable getRandomVariableForConstant(final double value) {
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
		return super.toString()
				+ "\n" + "timeDiscretizationFromArray: " + timeDiscretization.toString()
				+ "\n" + "numberOfPaths: " + numberOfPaths
				+ "\n" + "numberOfFactors: " + numberOfFactors
				+ "\n" + "seed: " + seed;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final IndependentIncrementsFromICDF that = (IndependentIncrementsFromICDF) o;

		if (numberOfFactors != that.numberOfFactors) {
			return false;
		}
		if (numberOfPaths != that.numberOfPaths) {
			return false;
		}
		if (seed != that.seed) {
			return false;
		}
		return timeDiscretization.equals(that.timeDiscretization);
	}

	@Override
	public int hashCode() {
		int result = timeDiscretization.hashCode();
		result = 31 * result + numberOfFactors;
		result = 31 * result + numberOfPaths;
		result = 31 * result + seed;
		return result;
	}

	private void readObject(final java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		incrementsLazyInitLock = new Object();
	}
}
