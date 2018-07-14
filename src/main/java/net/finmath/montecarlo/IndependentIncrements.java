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
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

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
public class IndependentIncrements implements IndependentIncrementsInterface, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 6270884840989559532L;

	private final TimeDiscretizationInterface						timeDiscretization;

	private final int			numberOfFactors;
	private final int			numberOfPaths;
	private final int			seed;

	private final AbstractRandomVariableFactory randomVariableFactory;

	private transient	RandomVariableInterface[][]	increments;
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
	 * by different implementations of the RandomVariableInterface
	 * (e.g. the RandomVariableLowMemory internally using float representations).
	 *
	 * @param timeDiscretization The time discretization used for the increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param inverseCumulativeDistributionFunctions A map from the timeIndices to a map from the from the factors to the corresponding inverse cumulative distribution function.
	 * @param randomVariableFactory Factory to be used to create random variable.
	 */
	public IndependentIncrements(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfFactors,
			int numberOfPaths,
			int seed,
			IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions,
			AbstractRandomVariableFactory randomVariableFactory) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;
		this.seed				= seed;

		this.inverseCumulativeDistributionFunctions = inverseCumulativeDistributionFunctions;
		this.randomVariableFactory = randomVariableFactory;

		this.increments	= null; 	// Lazy initialization
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
	public IndependentIncrements(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfFactors,
			int numberOfPaths,
			int seed,
			IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions) {
		this(timeDiscretization, numberOfFactors, numberOfPaths, seed, inverseCumulativeDistributionFunctions, new RandomVariableFactory());
	}

	@Override
	public IndependentIncrementsInterface getCloneWithModifiedSeed(int seed) {
		return new IndependentIncrements(getTimeDiscretization(), getNumberOfFactors(), getNumberOfPaths(), seed, inverseCumulativeDistributionFunctions, randomVariableFactory);
	}

	@Override
	public IndependentIncrementsInterface getCloneWithModifiedTimeDiscretization(TimeDiscretizationInterface newTimeDiscretization) {
		return new IndependentIncrements(newTimeDiscretization, getNumberOfFactors(), getNumberOfPaths(), getSeed(), inverseCumulativeDistributionFunctions, randomVariableFactory);
	}

	@Override
	public RandomVariableInterface getIncrement(int timeIndex, int factor) {

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
		MersenneTwister			mersenneTwister		= new MersenneTwister(seed);

		// Allocate memory
		double[][][] incrementsArray = new double[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors][numberOfPaths];

		// Pre-fetch icdfs
		DoubleUnaryOperator[][] inverseCumulativeDistributionFunctions = new DoubleUnaryOperator[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			for(int factor=0; factor<numberOfFactors; factor++) {
				inverseCumulativeDistributionFunctions[timeIndex][factor] = this.inverseCumulativeDistributionFunctions.apply(timeIndex).apply(factor);
			}
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
				// Generate uncorrelated Brownian increment
				for(int factor=0; factor<numberOfFactors; factor++) {
					double uniformIncrement = mersenneTwister.nextDouble();
					incrementsArray[timeIndex][factor][path] = inverseCumulativeDistributionFunctions[timeIndex][factor].applyAsDouble(uniformIncrement);
				}
			}
		}

		// Allocate memory for RandomVariable wrapper objects.
		increments = new RandomVariableInterface[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];

		// Wrap the values in RandomVariable objects
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			double time = timeDiscretization.getTime(timeIndex+1);
			for(int factor=0; factor<numberOfFactors; factor++) {
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
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IndependentIncrements that = (IndependentIncrements) o;

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

	private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		incrementsLazyInitLock = new Object();
	}
}

