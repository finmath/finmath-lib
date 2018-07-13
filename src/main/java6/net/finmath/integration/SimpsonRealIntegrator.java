package net.finmath.integration;

import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;


/**
 * A simple integrator using Simpson's rule.
 * 
 * The constructor has an optional argument to allow
 * parallel function evaluation. In that case, the integration rule
 * uses Java 8 parallel streams to evaluate.
 * 
 * @author Christian Fries
 */
public class SimpsonRealIntegrator extends AbstractRealIntegral{

	private int		numberOfEvaluationPoints;
	private boolean	useParallelEvaluation = false;


	/**
	 * Create an integrator using Simpson's rule.
	 * 
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used, must be greater or equal to 3.
	 * @param useParallelEvaluation If true, the integration rule will perform parallel evaluation of the integrand.
	 */
	public SimpsonRealIntegrator(double lowerBound, double upperBound, int numberOfEvaluationPoints, boolean useParallelEvaluation) {
		super(lowerBound, upperBound);
		if(numberOfEvaluationPoints < 3) {
			throw new IllegalArgumentException("Invalid numberOfEvaluationPoints.");
		}
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
	public SimpsonRealIntegrator(double lowerBound, double upperBound, int numberOfEvaluationPoints) {
		this(lowerBound, upperBound, numberOfEvaluationPoints, false);
	}

	/* (non-Javadoc)
	 * @see net.finmath.integration.AbstractRealIntegral#integrate(java.util.function.DoubleUnaryOperator)
	 */
	@Override
	public double integrate(DoubleUnaryOperator integrand) {
		double	lowerBound			= getLowerBound();
		double	upperBound			= getUpperBound();
		double	range				= upperBound-lowerBound;
		int		numberOfIntervalls	= (int) ((numberOfEvaluationPoints-1) / 2.0);

		double fullIntervall = range / numberOfIntervalls;
		double halfIntervall = 0.5 * fullIntervall;

		double sum = 0.0;
		for(int i=1; i<numberOfIntervalls; i++) {
			sum += 2 * integrand.applyAsDouble(lowerBound + i * fullIntervall + halfIntervall) + integrand.applyAsDouble(lowerBound + i * fullIntervall);
		}

		sum += 2.0 * integrand.applyAsDouble(lowerBound + halfIntervall);

		return (integrand.applyAsDouble(lowerBound) + integrand.applyAsDouble(upperBound) + 2.0 * sum) / 6.0 * fullIntervall;
	}
}
