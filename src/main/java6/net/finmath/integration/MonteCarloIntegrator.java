package net.finmath.integration;

import cern.jet.random.engine.MersenneTwister64;
import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;


/**
 * A simple integrator using Monte-Carlo integration.
 * 
 * The constructor has an optional argument to allow
 * parallel function evaluation. In that case, the integration rule
 * uses Java 8 parallel streams to evaluate.
 * 
 * @author Christian Fries
 */
public class MonteCarloIntegrator extends AbstractRealIntegral{

	private int		numberOfEvaluationPoints;
	private int		seed = 3141;


	/**
	 * Create an integrator using Simpson's rule.
	 * 
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used, must be greater or equal to 3.
	 * @param useParallelEvaluation If true, the integration rule will perform parallel evaluation of the integrand.
	 */
	public MonteCarloIntegrator(double lowerBound, double upperBound, int numberOfEvaluationPoints, boolean useParallelEvaluation) {
		super(lowerBound, upperBound);
		if(numberOfEvaluationPoints < 3) throw new IllegalArgumentException("Invalid numberOfEvaluationPoints.");
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
	}

	/**
	 * Create an integrator using Simpson's rule.
	 * 
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Maximum number of evaluation points to be used.
	 */
	public MonteCarloIntegrator(double lowerBound, double upperBound, int numberOfEvaluationPoints) {
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
		
		// Create random number sequence generator (we use MersenneTwister64 from colt)
		MersenneTwister64		mersenneTwister		= new MersenneTwister64(seed);

        double integral = 0.0;
        for(int i=0; i<numberOfEvaluationPoints; i++) {
            integral += integrand.applyAsDouble(lowerBound + mersenneTwister.nextDouble() * range);
        }

		return integral * range / numberOfEvaluationPoints;
	}
}
