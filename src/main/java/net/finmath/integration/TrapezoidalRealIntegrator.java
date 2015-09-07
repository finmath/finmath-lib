/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.05.2004
 */
package net.finmath.integration;

import java.util.function.DoubleUnaryOperator;

/**
 * A simple integrator using the trapezoidal rule.
 * 
 * @author Christian Fries
 */
public class TrapezoidalRealIntegrator extends AbstractRealIntegral{

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

	@Override
	public double integrate(DoubleUnaryOperator integrand) {
		double	lowerBound			= getLowerBound();
		double	upperBound			= getUpperBound();

		int i = 0;
		while(i<evaluationPoints.length && evaluationPoints[i] < lowerBound) i++;

		double pointLeft = lowerBound;
		double valueLeft = integrand.applyAsDouble(lowerBound);
		
        double sum = 0.0;
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

		return sum;
	}
}
