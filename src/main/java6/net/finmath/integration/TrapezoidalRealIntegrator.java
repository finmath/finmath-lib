/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.05.2004
 */
package net.finmath.integration;

import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;

/**
 * A simple integrator using the trapezoidal rule.
 * 
 * @author Christian Fries
 */
public class TrapezoidalRealIntegrator extends AbstractRealIntegral{

	private int			numberOfEvaluationPoints;
	private double[]	evaluationPoints;

	/**
	 * Create an integrator using the trapezoidal rule.
	 * 
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param evaluationPoints An ordered array of the inner evaluation points to use.
	 */
	public TrapezoidalRealIntegrator(double lowerBound, double upperBound, double[] evaluationPoints) {
		super(lowerBound, upperBound);
		this.evaluationPoints = evaluationPoints;
	}

	/**
	 * Create an integrator using the trapezoidal rule and an equi-distant grid of evaluation points.
	 * The minimum number of evaluation points (<code>numberOfEvaluationPoints</code>) is 2, since the
	 * trapezoidal rule operates on intervals. That is, lowerBound and upperBound are always evaluated. For
	 * <code>numberOfEvaluationPoints &gt; 2</code> additional inner points will be evaluated.
	 * 
	 * @param lowerBound Lower bound of the integral.
	 * @param upperBound Upper bound of the integral.
	 * @param numberOfEvaluationPoints Number of evaluation points (that is calls to the applyAsDouble of integrand). Has to be &gt; 2;
	 */
	public TrapezoidalRealIntegrator(double lowerBound, double upperBound, int numberOfEvaluationPoints) {
		super(lowerBound, upperBound);
		this.numberOfEvaluationPoints = numberOfEvaluationPoints;
		if(numberOfEvaluationPoints < 2) {
			throw new IllegalArgumentException("Invalid numberOfEvaluationPoints (minumum admissible value is 2).");
		}
	}

	@Override
	public double integrate(DoubleUnaryOperator integrand) {
		double	lowerBound			= getLowerBound();
		double	upperBound			= getUpperBound();

		double sum = 0.0;
		if(evaluationPoints != null) {
			/*
			 * Trapezoidal integration on a possibly non-equi-distant grid. 
			 */
			int i = 0;
			while(i<evaluationPoints.length && evaluationPoints[i] < lowerBound) {
				i++;
			}

			double pointLeft = lowerBound;
			double valueLeft = integrand.applyAsDouble(lowerBound);

			for(;i<evaluationPoints.length && evaluationPoints[i]<upperBound; i++) {
				double pointRight = evaluationPoints[i];
				double valueRight = integrand.applyAsDouble(pointRight);

				sum += (valueRight + valueLeft) * (pointRight - pointLeft);

				pointLeft = pointRight;
				valueLeft = valueRight;
			}
			double pointRight = upperBound;
			double valueRight = integrand.applyAsDouble(pointRight);

			sum += (valueRight + valueLeft) * (pointRight - pointLeft);

			sum /= 2.0;
		}
		else {
			/*
			 * Trapezoidal integration on an equi-distant grid. 
			 */

			double intervall = (upperBound-lowerBound) / (numberOfEvaluationPoints-1);
			// Sum of inner points
			for(int i=1; i<numberOfEvaluationPoints-1; i++) {
				double point = lowerBound + i * intervall;
				sum += integrand.applyAsDouble(point) * intervall;
			}
			// Sum of boundary points
			sum += (integrand.applyAsDouble(lowerBound) + integrand.applyAsDouble(upperBound)) / 2.0 * intervall;

		}
		return sum;
	}
}
