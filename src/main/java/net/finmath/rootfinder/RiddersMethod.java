/*
 * Created on 17.02.2004
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * This class implements Ridders root finder as a question-and-answer algorithm.
 * The method is documented in Numerical Receipts in C.
 *
 * @author Christian Fries
 * @version 1.1
 * @date 2008-04-06
 */
public class RiddersMethod implements RootFinder {

	// We store the left and right end point of the interval
	private final double[] points = new double[3]; // left, middle, right
	private final double[] values = new double[3]; // left, middle, right

	/*
	 * State of solver
	 */
	private double	nextPoint;						// Stores the next point to return by getPoint()
	private int		solverState			= 0;		// Internal state of the solver - see <code>setValue()</code>

	private int		numberOfIterations	= 0; 		// Number of numberOfIterations
	private double	bestPoint;
	private double  accuracy            = Double.MAX_VALUE;     // Current accuracy of solution
	private boolean	isDone				= false;	// Will be true if machine accuracy has been reached

	public static void main(final String[] args) {
		// Test
		final RiddersMethod search = new RiddersMethod(-1.0, 5.0);

		while(search.getAccuracy() > 1E-13 && !search.isDone()) {
			final double x = search.getNextPoint();

			final double y = x - 0.656;

			search.setValue(y);
			System.out.println(search.getAccuracy());
		}

		System.out.println(search.getNumberOfIterations() + " " + search.getBestPoint());
	}

	/**
	 * @param leftPoint left point of search interval
	 * @param rightPoint right point of search interval
	 */
	public RiddersMethod(final double leftPoint, final double rightPoint) {
		super();
		points[0]	= leftPoint;
		points[2]	= rightPoint;
		points[1]	= (points[0] + points[2]) / 2.0;

		nextPoint	= points[0];
		bestPoint	= points[1];
		accuracy	= points[2]-points[0];
	}

	/**
	 * @return Best point optained so far
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
	 * @param value Value corresponding to point returned by previous <code>getNextPoint</code> call.
	 */
	@Override
	public void setValue(final double value) {
		switch(solverState)
		{
		case 0:
		default:
			// State 0: We have asked for left point
			values[0] = value;

			// In next state ask for right point
			nextPoint = points[2];
			solverState++;
			break;
		case 1:
			// State 1: We have asked for right point
			values[2] = value;

			// Calculate next point to propose
			points[1] = 0.5*(points[0]+points[2]);		// Middle point

			// In next state ask for middle point
			nextPoint = points[1];
			solverState++;
			break;
		case 2:
			// State 2: We have asked for middle point
			values[1] = value;

			final double s = Math.sqrt(values[1]*values[1]-values[0]*values[2]);
			if (s == 0.0) {
				accuracy = 0.0;
				bestPoint = nextPoint;
				isDone = true;
			}

			// Calculate next point to propose
			nextPoint = points[1]+(points[1]-points[0])* (values[0] >= values[2] ? 1.0 : -1.0) *values[1]/s; // Updating formula.
			solverState++;
			break;
		case 3:
			// State 3: We have asked for an additional point (stored in next point)
			if(value == 0.0)
			{
				accuracy = 0.0;
				bestPoint = nextPoint;
				isDone = true;
			}

			if(sign(values[1],value) != values[1])	// Bookkeeping to keep the root bracketed on next iteration. xl=xm;
			{
				points[0] = points[1];
				values[0] = values[1];

				points[2] = nextPoint;
				values[2] = value;
			}
			else if(sign(values[0],value) != values[0])
			{
				points[2] = nextPoint;
				values[2] = value;

			}
			else if (sign(values[2],value) != values[2])
			{
				points[0] = nextPoint;
				values[0] = value;
			}
			else
			{
				System.err.println(this.getClass().getName() + ": Error: Never get here.");
				isDone = true;
				return;
			}

			points[1] = 0.5*(points[0]+points[2]);		// Middle point

			// In next state ask for middle point
			nextPoint = points[1];
			solverState = 2;				// Next state is 2

			// Savety belt: check if still improve or if we have reached machine accuracy
			//			if(Math.abs(points[2]-points[0]) >= accuracy) isDone = true;

			break;
		}

		if(!isDone) {
			// Update accuracy
			accuracy = Math.abs(points[2]-points[0]);

			// Update best point
			bestPoint = points[1];
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

	private static double sign(final double a, final double b)
	{
		return b>= 0.0 ? (a>=0 ? a : -a) : (a>0 ? -a : a);
	}
}
