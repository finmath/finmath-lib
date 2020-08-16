package net.finmath.rootfinder;

import net.finmath.stochastic.RandomVariable;

/**
 * Implementation of Newtons method for maps on random variables.
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.0
 */
public class StochasticNewtonMethod implements StochasticRootFinderUsingDerivative {

	public enum MethodForAccuracy {AVERAGE, MAX, MIN}

	private RandomVariable	nextPoint;											// Stores the next point to be returned by getPoint()
	private RandomVariable	bestPoint;				              				// Best point so far

	private int	numberOfIterations				= 0;           							// Number of numberOfIterations
	private double	accuracy					= Double.MAX_VALUE;						// Current accuracy of solution
	private final boolean	isDone						= false;             					// Will be true if machine accuracy has been reached

	private final MethodForAccuracy method;

	/**
	 * @param guess {@link RandomVariable} representing a first guess to start of the Newton Method
	 * @param method defines the Method used to gain the accuracy for the Newton Iteration
	 */
	public StochasticNewtonMethod(final RandomVariable guess, final MethodForAccuracy method) {
		bestPoint = guess;
		nextPoint = guess;
		this.method = method;
	}

	@Override
	public RandomVariable getNextPoint() {
		return nextPoint;
	}

	@Override
	public void setValueAndDerivative(final RandomVariable value, final RandomVariable derivative) {

		double currentAccuracy;

		switch (method) {
		case AVERAGE:
			currentAccuracy = value.abs().getAverage();
			break;
		case MAX:
			currentAccuracy = value.abs().getMax();
			break;
		case MIN:
			currentAccuracy = value.abs().getMin();
			break;
		default:
			throw new IllegalArgumentException("Method to get current accuracy from RandomVariable not supported!");
		}


		if(currentAccuracy < accuracy)
		{
			accuracy	= currentAccuracy;
			bestPoint	= nextPoint;
		}

		// Calculate next point
		nextPoint = nextPoint.addRatio(value,derivative);

		numberOfIterations++;

	}

	@Override
	public int getNumberOfIterations() {
		return numberOfIterations;
	}

	@Override
	public double getAccuracy() {
		return accuracy;
	}

	@Override
	public boolean isDone() {
		return isDone;
	}

	@Override
	public RandomVariable getBestPoint() {
		return bestPoint;
	}
}
