/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.06.2004
 */
package net.finmath.montecarlo;

import java.io.Serializable;

import net.finmath.functions.GammaDistribution;
import net.finmath.randomnumbers.MersenneTwister;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implementation of a time-discrete n-dimensional Gamma process
 * \(
 * \Gamma = (\Gamma_{1},\ldots,\Gamma_{n})
 * \), where \( \Gamma_{i} \) is
 * a Gamma process and \( \Gamma_{i} \), \( \Gamma_{j} \) are
 * independent for <i>i</i> not equal <i>j</i>.
 *
 * The increments \( \Delta \Gamma \) are Gamma distributed with shape parameter <code>shape * (t-s)</code>
 * for \( t-s = \Delta t \) and scale parameter 1.
 *
 * Here the dimension <i>n</i> is called factors since this Gamma process is used to
 * generate multi-dimensional multi-factor Levy processes and there one might
 * use a different number of factors to generate Levy processes of different
 * dimension.
 *
 * The quintruppel (time discretization, number of factors, number of paths, seed, shape)
 * defines the state of an object of this class, i.e., GammaProcess for which
 * there parameters agree, generate the same random numbers.
 *
 * The class is immutable and thread safe. It uses lazy initialization.
 *
 * @author Christian Fries
 * @version 1.6
 */
public class GammaProcess implements IndependentIncrements, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5430067621669213475L;

	private final double shape;
	private final double scale;

	private final TimeDiscretization						timeDiscretization;

	private final int			numberOfFactors;
	private final int			numberOfPaths;
	private final int			seed;

	private final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

	private transient RandomVariable[][]	gammaIncrements;

	/**
	 * Construct a Gamma process with a given shape parameter.
	 *
	 * @param timeDiscretization The time discretization used for the Gamma increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param shape The shape parameter of the Gamma distribution.
	 * @param scale The scale parameter of the Gamma distribution.
	 */
	public GammaProcess(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final int seed,
			final double shape,
			final double scale) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;
		this.seed				= seed;
		this.shape				= shape;
		this.scale				= scale;

		gammaIncrements	= null; 	// Lazy initialization
	}

	/**
	 * Construct a Gamma process with a given shape parameter.
	 *
	 * @param timeDiscretization The time discretization used for the Gamma increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param seed The seed of the random number generator.
	 * @param shape The shape parameter of the Gamma distribution.
	 */
	public GammaProcess(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final int seed,
			final double shape) {
		this(timeDiscretization, numberOfFactors, numberOfPaths, seed, shape, 1.0);
	}

	@Override
	public IndependentIncrements getCloneWithModifiedSeed(final int seed) {
		return new GammaProcess(getTimeDiscretization(), getNumberOfFactors(), getNumberOfPaths(), seed, shape);
	}

	@Override
	public IndependentIncrements getCloneWithModifiedTimeDiscretization(final TimeDiscretization newTimeDiscretization) {
		/// @TODO This can be improved: a complete recreation of the Gamma process wouldn't be necessary!
		return new GammaProcess(newTimeDiscretization, getNumberOfFactors(), getNumberOfPaths(), getSeed(), shape);
	}

	@Override
	public RandomVariable getIncrement(final int timeIndex, final int factor) {
		// Thread safe lazy initialization
		synchronized(this) {
			if(gammaIncrements == null) {
				doGenerateGammaIncrements();
			}
		}

		/*
		 *  For performance reasons we return directly the stored data (no defensive copy).
		 *  We return an immutable object to ensure that the receiver does not alter the data.
		 */
		return gammaIncrements[timeIndex][factor];
	}

	/**
	 * Lazy initialization of gammaIncrement. Synchronized to ensure thread safety of lazy init.
	 */
	private void doGenerateGammaIncrements() {
		if(gammaIncrements != null) {
			return;	// Nothing to do
		}

		// Create random number sequence generator
		final MersenneTwister			mersenneTwister		= new MersenneTwister(seed);

		// Allocate memory
		final double[][][] gammaIncrementsArray = new double[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors][numberOfPaths];

		// Pre-calculate distributions
		final GammaDistribution[] gammaDistributions = new GammaDistribution[timeDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<gammaDistributions.length; timeIndex++) {
			final double deltaT = timeDiscretization.getTimeStep(timeIndex);
			gammaDistributions[timeIndex] = new GammaDistribution(shape * deltaT, scale);
		}

		/*
		 * Generate gamma distributed independent increments.
		 *
		 * The inner loop goes over time and factors.
		 * MersenneTwister is known to generate "independent" increments in 623 dimensions.
		 * Since we want to generate independent streams (paths), the loop over path is the outer loop.
		 */
		for(int path=0; path<numberOfPaths; path++) {
			for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
				final GammaDistribution gammaDistribution = gammaDistributions[timeIndex];
				// Generate uncorrelated Gamma distributed increment
				for(int factor=0; factor<numberOfFactors; factor++) {
					final double uniformIncrement = mersenneTwister.nextDouble();
					gammaIncrementsArray[timeIndex][factor][path] = gammaDistribution.inverseCumulativeDistribution(uniformIncrement);
				}
			}
		}

		// Allocate memory for RandomVariableFromDoubleArray wrapper objects.
		gammaIncrements = new RandomVariable[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];

		// Wrap the values in RandomVariableFromDoubleArray objects
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final double time = timeDiscretization.getTime(timeIndex+1);
			for(int factor=0; factor<numberOfFactors; factor++) {
				gammaIncrements[timeIndex][factor] =
						randomVariableFactory.createRandomVariable(time, gammaIncrementsArray[timeIndex][factor]);
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
				+ "\n" + "seed: " + seed
				+ "\n" + "shape: " + shape;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final GammaProcess that = (GammaProcess) o;

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
		result = 31 * result + Double.hashCode(shape);
		return result;
	}
}
