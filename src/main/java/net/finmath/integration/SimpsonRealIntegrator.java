package net.finmath.integration;

import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
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
	private boolean	useParallelEvaluation = false;


	/**
	 * Create an integrator using Simpson's rule.
	 *
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used, must be greater or equal to 3.
	 * @param useParallelEvaluation If true, the integration rule will perform parallel evaluation of the integrand.
	 */
	public SimpsonRealIntegrator(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints, final boolean useParallelEvaluation) {
		super(lowerBound, upperBound);
		Validate.exclusiveBetween(1, Integer.MAX_VALUE, numberOfEvaluationPoints, "Parameter numberOfEvaluationPoints required to be > 1.");
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
		final int		numberOfIntervalls	= (int) ((numberOfEvaluationPoints-1) / 2.0);

		final double fullIntervall = range / numberOfIntervalls;
		final double halfIntervall = 0.5 * fullIntervall;

		IntStream intervals = IntStream.range(1, numberOfIntervalls);
		if(useParallelEvaluation) {
			intervals = intervals.parallel();
		}

		double sum = intervals.mapToDouble(
				new IntToDoubleFunction() {
					@Override
					public double applyAsDouble(final int i) {
						return 2 * integrand.applyAsDouble(lowerBound + i * fullIntervall + halfIntervall) + integrand.applyAsDouble(lowerBound + i * fullIntervall);
					}
				}
				).sum();

		sum += 2.0 * integrand.applyAsDouble(lowerBound + halfIntervall);

		return (integrand.applyAsDouble(lowerBound) + integrand.applyAsDouble(upperBound) + 2.0 * sum) / 6.0 * fullIntervall;
	}
}
