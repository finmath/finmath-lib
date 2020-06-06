/*
 * Created on 16.02.2004
 *
 * (c) Copyright Christian P. Fries, Germany.
 * Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * This class implements a Bisection search algorithm,
 * implemented as a question-and-answer search algorithm.
 *
 * @author Christian Fries
 * @version 1.2
 * @date 2008-03-15
 */
public class BisectionSearch implements RootFinder {

	// We store the left and right end point of the interval
	private final double[] points = new double[2]; // left, right
	private final double[] values = new double[2]; // left, right

	/*
	 * State of solver
	 */

	private double	nextPoint;						// Stores the next point to return by getPoint()

	private int		numberOfIterations	= 0; 		// Number of iterations
	private double	accuracy	= Double.MAX_VALUE;	// Current accuracy of solution
	private boolean	isDone		= false;			// True, if machine accuracy has been reached

	/**
	 * @param leftPoint left point of search interval
	 * @param rightPoint right point of search interval
	 */
	public BisectionSearch(final double leftPoint, final double rightPoint) {
		super();
		points[0]	= leftPoint;
		points[1]	= rightPoint;

		nextPoint	= points[0];
		accuracy	= points[1]-points[0];
	}

	/**
	 * @return Best point optained so far
	 */
	@Override
	public double getBestPoint() {
		// Lazy: we always return the middle point as best point
		return (points[1] + points[0]) / 2.0;
	}

	/**
	 * @return Next point for which a value should be set using <code>setValue</code>.
	 */
	@Override
	public double getNextPoint() {
		return nextPoint;
	}

	/**
	 * @param value Value corresponding to point returned by previous <code>getNextPoint</code> call.
	 */
	@Override
	public void setValue(final double value) {
		if (numberOfIterations < 2) {
			/*
			 * Initially fill values
			 */
			values[numberOfIterations] = value;

			if (numberOfIterations < 1) {
				nextPoint = points[numberOfIterations + 1];
			} else {
				nextPoint = (points[1] + points[0]) / 2.0;
				/*
				 * TODO Check if values[0]*values[1] < 0 here
				 */
			}
		}
		else {
			/*
			 * Bisection search update rule
			 */

			if (values[1] * value > 0) {
				/*
				 * Throw away right point. Replace with current point.
				 */
				points[1] = nextPoint;		// This is the current point. New next point calculated below.
				values[1] = value;
			} else {
				/*
				 * Throw away left point. Replace with current point.
				 */
				points[0] = nextPoint;		// This is the current point. New next point calculated below.
				values[0] = value;
			}

			// Calculate new next point (bisection)
			nextPoint = (points[1] + points[0]) / 2.0;

			// Savety belt: check if still improve or if we have reached machine accuracy
			if(points[1]-points[0] >= accuracy) {
				isDone = true;
			}

			// Update accuracy
			accuracy = points[1]-points[0];
		}

		numberOfIterations++;
	}

	/**
	 * @return Returns the numberOfIterations.
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
	 * @return Returns the isDone.
	 */
	@Override
	public boolean isDone() {
		return isDone;
	}
}
