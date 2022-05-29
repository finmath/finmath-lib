package net.finmath.integration;

import java.util.function.DoubleUnaryOperator;
import java.util.stream.IntStream;

import org.apache.commons.lang3.Validate;


/**
 * A simple integrator using Simpson's rule.
 *
 * The constructor has an optional argument to allow
 * parallel function evaluation. In that case, the integration rule
 * uses Java 8 parallel streams to evaluate.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SimpsonRealIntegrator extends AbstractRealIntegral{

	private final int		numberOfEvaluationPoints;
	private final boolean	useParallelEvaluation;


	/**
	 * Create an integrator using Simpson's rule.
	 *
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used, must be greater or equal to 3, should be odd.
	 * @param useParallelEvaluation If true, the integration rule will perform parallel evaluation of the integrand.
	 */
	public SimpsonRealIntegrator(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints, final boolean useParallelEvaluation) {
		super(lowerBound, upperBound);
		Validate.exclusiveBetween(2, Integer.MAX_VALUE, numberOfEvaluationPoints, "Parameter numberOfEvaluationPoints required to be > 2.");
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
		this.useParallelEvaluation = useParallelEvaluation;
	}

	/**
	 * Create an integrator using Simpson's rule.
	 *
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used.
	 */
	public SimpsonRealIntegrator(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		this(lowerBound, upperBound, numberOfEvaluationPoints, false);
	}

	@Override
	public double integrate(final DoubleUnaryOperator integrand) {
		final double	lowerBound			= getLowerBound();
		final double	upperBound			= getUpperBound();
		final double	range				= upperBound-lowerBound;

		final int		numberOfDoubleSizeIntervals	= (int) ((numberOfEvaluationPoints-1) / 2.0);

		final double doubleInterval = range / numberOfDoubleSizeIntervals;
		final double singleInterval = 0.5 * doubleInterval;

		IntStream intervals = IntStream.range(1, numberOfDoubleSizeIntervals);

		if(useParallelEvaluation) {
			intervals = intervals.parallel();
		}

		double sum = intervals.mapToDouble(
				i -> integrand.applyAsDouble(lowerBound + i * doubleInterval) + 2 * integrand.applyAsDouble(lowerBound + i * doubleInterval + singleInterval)
				).sum();

		sum += 2.0 * integrand.applyAsDouble(lowerBound + singleInterval);

		return (integrand.applyAsDouble(lowerBound) + 2.0 * sum + integrand.applyAsDouble(upperBound)) / 3.0 * singleInterval;
	}
}
