/*
 * Created on 19.02.2004
 *
 * (c) Copyright Christian P. Fries, Germany.
 * Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * This class implements a root finder as question-and-answer algorithm using
 * the secant method.
 *
 * @author Christian Fries
 * @version 1.3
 * @date 2008-04-06
 */
public class SecantMethod extends NewtonsMethod implements RootFinder {

	// We need a second guess for the initial secant
	private final double secondGuess;

	// State of the solver
	private double currentPoint;	// Actually the same as NewtonsMethod.nextPoint
	private double lastPoint;
	private double lastValue;

	/**
	 * @param firstGuess
	 *      The first guess for the solver to use.
	 * @param secondGuess
	 *      A second guess for the solver to use (different from first guess).
	 */
	public SecantMethod(final double firstGuess, final double secondGuess) {
		super(firstGuess);
		this.secondGuess = secondGuess;
	}

	@Override
	public double getNextPoint() {
		// Ask NewtonsMethods for next point and rember it as current point
		currentPoint = super.getNextPoint();
		return currentPoint;
	}

	/**
	 * @param value
	 *      The value corresponding to the point returned
	 *      by previous <code>getNextPoint</code> call.
	 */
	@Override
	public void setValue(final double value) {
		// Calculate approximation for derivative
		double derivative;
		if (getNumberOfIterations() == 0) {
			/* Trick: This derivative will let Newton's method
			 * propose the second guess as next point
			 */
			derivative = value / (secondGuess - currentPoint);
		} else {
			derivative = (value - lastValue) / (currentPoint - lastPoint);
		}

		// Remember last point
		lastPoint = currentPoint;
		lastValue = value;

		super.setValueAndDerivative(value, derivative);

	}

	/**
	 * @param value
	 *     The value corresponding to the point returned by previous
	 *     <code>getNextPoint</code> call.
	 * @param derivative
	 *     The derivative corresponding to the point returned by previous
	 *     <code>getNextPoint</code> call.
	 */
	@Override
	public void setValueAndDerivative(final double value, final double derivative) {
		// Remember last point
		lastPoint = super.getNextPoint();
		lastValue = value;

		super.setValueAndDerivative(value, derivative);

	}
}
