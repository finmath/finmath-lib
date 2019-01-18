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
	private boolean	isDone						= false;             					// Will be true if machine accuracy has been reached

	private MethodForAccuracy method;

	/**
	 * @param guess {@link RandomVariable} representing a first guess to start of the Newton Method
	 * @param method defines the Method used to gain the accuracy for the Newton Iteration
	 */
	public StochasticNewtonMethod(RandomVariable guess, MethodForAccuracy method) {
		bestPoint = guess;
		nextPoint = guess;
		this.method = method;
	}

	/* (non-Javadoc)
	 * @see net.finmath.rootfinder.RandomVariableRootFinderUsingDerivative#getNextPoint()
	 */
	@Override
	public RandomVariable getNextPoint() {
		return nextPoint;
	}

	/* (non-Javadoc)
	 * @see net.finmath.rootfinder.RandomVariableRootFinderUsingDerivative#setValueAndDerivative(net.finmath.stochastic.RandomVariable, net.finmath.stochastic.RandomVariable)
	 */
	@Override
	public void setValueAndDerivative(RandomVariable value, RandomVariable derivative) {

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

	/* (non-Javadoc)
	 * @see net.finmath.rootfinder.RandomVariableRootFinderUsingDerivative#getNumberOfIterations()
	 */
	@Override
	public int getNumberOfIterations() {
		return numberOfIterations;
	}

	/* (non-Javadoc)
	 * @see net.finmath.rootfinder.RandomVariableRootFinderUsingDerivative#getAccuracy()
	 */
	@Override
	public double getAccuracy() {
		return accuracy;
	}

	/* (non-Javadoc)
	 * @see net.finmath.rootfinder.RandomVariableRootFinderUsingDerivative#isDone()
	 */
	@Override
	public boolean isDone() {
		return isDone;
	}

	/* (non-Javadoc)
	 * @see net.finmath.rootfinder.RandomVariableRootFinderUsingDerivative#getBestPoint()
	 */
	@Override
	public RandomVariable getBestPoint() {
		return bestPoint;
	}

}
