package net.finmath.integration;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.DoubleStream;

import org.apache.commons.lang3.Validate;

import net.finmath.randomnumbers.MersenneTwister;
import net.finmath.randomnumbers.RandomNumberGenerator1D;


/**
 * A simple integrator using Monte-Carlo integration.
 *
 * The constructor has an optional argument to allow
 * parallel function evaluation. In that case, the integration rule
 * uses Java 8 parallel streams to evaluate.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class MonteCarloIntegrator extends AbstractRealIntegral{

	private final int		numberOfEvaluationPoints;
	private final int		seed;

	/**
	 * Create an integrator using Monte-Carlo integration.
	 *
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used, must be greater or equal to 3.
	 * @param seed The seed of the random number generator.
	 * @param useParallelEvaluation If true, the integration rule will perform parallel evaluation of the integrand.
	 */
	public MonteCarloIntegrator(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints, final int seed, final boolean useParallelEvaluation) {
		super(lowerBound, upperBound);
		Validate.exclusiveBetween(0, Integer.MAX_VALUE, numberOfEvaluationPoints, "Parameter numberOfEvaluationPoints required to be > 0.");
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
		this.seed = seed;
	}

	/**
	 * Create an integrator using Monte-Carlo.
	 *
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used, must be greater or equal to 3.
	 * @param useParallelEvaluation If true, the integration rule will perform parallel evaluation of the integrand.
	 */
	public MonteCarloIntegrator(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints, final boolean useParallelEvaluation) {
		this(lowerBound, upperBound, numberOfEvaluationPoints, 3141 /* fixed seed */, useParallelEvaluation);
	}

	/**
	 * Create an integrator using Monte-Carlo.
	 *
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used.
	 */
	public MonteCarloIntegrator(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		this(lowerBound, upperBound, numberOfEvaluationPoints, false);
	}

	@Override
	public double integrate(final DoubleUnaryOperator integrand) {
		final double	lowerBound			= getLowerBound();
		final double	upperBound			= getUpperBound();
		final double	range				= upperBound-lowerBound;

		// Create random number sequence generator (we use MersenneTwister)
		final RandomNumberGenerator1D mersenneTwister	= new MersenneTwister(seed);

		final DoubleStream randomNumberSequence = DoubleStream.generate(mersenneTwister).limit(numberOfEvaluationPoints);

		// Integrate f(a x + b) on [0,1)
		return randomNumberSequence.map(x -> lowerBound + x * range).map(integrand).sum() * range / numberOfEvaluationPoints;
	}

	public int getNumberOfEvaluationPoints() {
		return numberOfEvaluationPoints;
	}

	public int getSeed() {
		return seed;
	}
}
