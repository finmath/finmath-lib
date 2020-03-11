/*
 * Created on 19.02.2004
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * This class implements a root finder as question-and-answer algorithm
 * using Newton's method.
 *
 * @author Christian Fries
 * @version 1.3
 * @date 2008-04-06
 */
public class NewtonsMethod implements RootFinderWithDerivative {

	private double	nextPoint;						// Stores the next point to be returned by getPoint()

	private int		numberOfIterations	= 0;               		// Number of numberOfIterations
	private double	bestPoint;				              		// Best point so far
	private double	accuracy			= Double.MAX_VALUE;		// Current accuracy of solution
	private final boolean	isDone				= false;             	// Will be true if machine accuracy has been reached

	/**
	 * @param guess initial guess where the solver will start
	 */
	public NewtonsMethod(final double guess) {
		nextPoint	= guess;
		bestPoint	= guess;
	}

	/**
	 * @return Returns the best point optained so far
	 */
	@Override
	public double getBestPoint() {
		return bestPoint;
	}

	/**
	 * @return Next point for which a value should be set using <code>setValue</code>.
	 */
	@Override
	public double getNextPoint() {
		return nextPoint;
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

		if(Math.abs(value) < accuracy)
		{
			accuracy	= Math.abs(value);
			bestPoint	= nextPoint;
		}

		// Calculate next point
		nextPoint -= value / derivative;

		numberOfIterations++;
	}

	/**
	 * @return Returns the number of iterations.
	 */
	@Override
	public int getNumberOfIterations() {
		return numberOfIterations;
	}

	/**
	 * @return Returns the accuracy.
	 */
	@Override
	public double getAccuracy() {
		return accuracy;
	}

	/**
	 * @return Returns true if the solver is done (accuracy achieved or unable to improve)
	 */
	@Override
	public boolean isDone() {
		return isDone;
	}
}
