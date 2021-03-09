/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import java.io.IOException;
import java.io.Serializable;

import org.apache.commons.lang3.Validate;

import net.finmath.randomnumbers.RandomNumberGenerator;
import net.finmath.stochastic.RandomVariable;
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
 */
public class BrownianMotionFromRandomNumberGenerator implements BrownianMotion, Serializable {

	private static final long serialVersionUID = -5430067621669213475L;

	private final TimeDiscretization						timeDiscretization;

	private final int			numberOfFactors;
	private final int			numberOfPaths;
	private final RandomNumberGenerator randomNumberGenerator;

	private final RandomVariableFactory randomVariableFactory;

	private transient	RandomVariable[][]	brownianIncrements;
	private transient 	Object				brownianIncrementsLazyInitLock = new Object();

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
	 * @param randomNumberGenerator A random number generator for n-dimensional uniform random numbers (n = numberOfTimeSteps*numberOfFactors).
	 * @param randomVariableFactory Factory to be used to create random variable.
	 */
	public BrownianMotionFromRandomNumberGenerator(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final RandomNumberGenerator randomNumberGenerator,
			final RandomVariableFactory randomVariableFactory) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;
		this.randomNumberGenerator = randomNumberGenerator;

		this.randomVariableFactory = randomVariableFactory;

		brownianIncrements	= null; 	// Lazy initialization

		Validate.notNull(timeDiscretization);
		Validate.notNull(randomNumberGenerator);
		final int requiredDimension = numberOfFactors*timeDiscretization.getNumberOfTimeSteps();
		Validate.isTrue(randomNumberGenerator.getDimension() >= requiredDimension, "Dimension of RandomNumberGenerator required to be at least %d.", requiredDimension);
	}

	/**
	 * Construct a Brownian motion.
	 *
	 * @param timeDiscretization The time discretization used for the Brownian increments.
	 * @param numberOfFactors Number of factors.
	 * @param numberOfPaths Number of paths to simulate.
	 * @param randomNumberGenerator A random number generator for n-dimensional uniform random numbers (n = numberOfTimeSteps*numberOfFactors).
	 */
	public BrownianMotionFromRandomNumberGenerator(
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors,
			final int numberOfPaths,
			final RandomNumberGenerator randomNumberGenerator) {
		this(timeDiscretization, numberOfFactors, numberOfPaths, randomNumberGenerator, new RandomVariableFromArrayFactory());
	}

	@Override
	public BrownianMotion getCloneWithModifiedSeed(final int seed) {
		return new BrownianMotionFromRandomNumberGenerator(getTimeDiscretization(), getNumberOfFactors(), getNumberOfPaths(), randomNumberGenerator);
	}

	@Override
	public BrownianMotion getCloneWithModifiedTimeDiscretization(final TimeDiscretization newTimeDiscretization) {
		/// @TODO This can be improved: a complete recreation of the Brownian motion wouldn't be necessary!
		return new BrownianMotionFromRandomNumberGenerator(newTimeDiscretization, getNumberOfFactors(), getNumberOfPaths(), randomNumberGenerator);
	}

	@Override
	public RandomVariable getBrownianIncrement(final int timeIndex, final int factor) {

		// Thread safe lazy initialization
		synchronized(brownianIncrementsLazyInitLock) {
			if(brownianIncrements == null) {
				doGenerateBrownianMotion();
			}
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
		if(brownianIncrements != null) {
			return;	// Nothing to do
		}

		// Allocate memory
		final double[][][] brownianIncrementsArray = new double[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors][numberOfPaths];

		// Pre-calculate square roots of deltaT
		final double[] sqrtOfTimeStep = new double[timeDiscretization.getNumberOfTimeSteps()];
		for(int timeIndex=0; timeIndex<sqrtOfTimeStep.length; timeIndex++) {
			sqrtOfTimeStep[timeIndex] = Math.sqrt(timeDiscretization.getTimeStep(timeIndex));
		}

		/*
		 * Generate normal distributed independent increments.
		 *
		 * The inner loop goes over time and factors.
		 * Since we want to generate independent streams (paths), the loop over path is the outer loop.
		 */
		for(int path=0; path<numberOfPaths; path++) {
			final double[] randomNumbers = randomNumberGenerator.getNext();
			for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
				final double sqrtDeltaT = sqrtOfTimeStep[timeIndex];
				// Generate uncorrelated Brownian increment
				for(int factor=0; factor<numberOfFactors; factor++) {
					final double uniformIncrement = randomNumbers[timeIndex * numberOfFactors + factor];
					brownianIncrementsArray[timeIndex][factor][path] = net.finmath.functions.NormalDistribution.inverseCumulativeDistribution(uniformIncrement) * sqrtDeltaT;
				}
			}
		}

		// Allocate memory for RandomVariableFromDoubleArray wrapper objects.
		brownianIncrements = new RandomVariable[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];

		// Wrap the values in RandomVariableFromDoubleArray objects
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final double time = timeDiscretization.getTime(timeIndex+1);
			for(int factor=0; factor<numberOfFactors; factor++) {
				brownianIncrements[timeIndex][factor] =
						randomVariableFactory.createRandomVariable(time, brownianIncrementsArray[timeIndex][factor]);
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

	@Override
	public String toString() {
		return super.toString()
				+ "\n" + "timeDiscretizationFromArray: " + timeDiscretization.toString()
				+ "\n" + "numberOfPaths: " + numberOfPaths
				+ "\n" + "numberOfFactors: " + numberOfFactors
				+ "\n" + "randomNumberGenerator: " + randomNumberGenerator;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final BrownianMotionFromRandomNumberGenerator that = (BrownianMotionFromRandomNumberGenerator) o;

		if (numberOfFactors != that.numberOfFactors) {
			return false;
		}
		if (numberOfPaths != that.numberOfPaths) {
			return false;
		}
		if (randomNumberGenerator != that.randomNumberGenerator) {
			return false;
		}
		return timeDiscretization.equals(that.timeDiscretization);
	}

	@Override
	public RandomVariable getIncrement(final int timeIndex, final int factor) {
		return getBrownianIncrement(timeIndex, factor);
	}

	@Override
	public int hashCode() {
		int result = timeDiscretization.hashCode();
		result = 31 * result + numberOfFactors;
		result = 31 * result + numberOfPaths;
		result = 31 * result + randomNumberGenerator.hashCode();
		return result;
	}

	private void readObject(final java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		brownianIncrementsLazyInitLock = new Object();
	}
}
