/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 25.01.2004
 */

package net.finmath.optimizer;

/**
 * This class implements a Golden Section search algorithm, i.e., a minimization,
 * implemented as a question-and-answer search algorithm.
 *
 * Example:
 * <pre>
 * <code>
 * 		GoldenSectionSearch search = new GoldenSectionSearch(-1.0, 5.0);
 * 		while(search.getAccuracy() &gt; 1E-11 &amp;&amp; !search.isDone()) {
 * 			double x = search.getNextPoint();
 *
 * 			double y = (x - 0.656) * (x - 0.656);
 *
 * 			search.setValue(y);
 * 		}
 * </code>
 * </pre>
 *
 * For an example on how to use this class see also its main method.
 *
 * @author Christian Fries - http://www.christian-fries.de
 * @version 1.1
 */
public class GoldenSectionSearch {

	// This is the golden section ratio
	public static final double GOLDEN_SECTION_RATIO = (3.0 - Math.sqrt(5.0)) / 2.0;

	// We store the left and right end point of the interval and a middle point (placed at golden section ratio) together with their values
	private final double[] points = new double[3]; // left, middle, right
	private final double[] values = new double[3]; // left, middle, right

	/*
	 * State of solver
	 */

	private double	nextPoint;						// Stores the next point to return by getPoint()
	private boolean	expectingValue		= false;	// Stores the state (true, if next call should be setValue(), false for getPoint())

	private int		numberOfIterations	= 0; 		// Number of numberOfIterations
	private double	accuracy;						// Current accuracy of solution
	private boolean	isDone				= false;	// Will be true if machine accuracy has been reached

	public static void main(final String[] args) {
		System.out.println("Test of GoldenSectionSearch Class.\n");

		// Test 1
		System.out.println("1. Find minimum of f(x) = (x - 0.656) * (x - 0.656):");

		final GoldenSectionSearch search = new GoldenSectionSearch(-1.0, 5.0);
		while(search.getAccuracy() > 1E-11 && !search.isDone()) {
			final double x = search.getNextPoint();

			final double y = (x - 0.656) * (x - 0.656);

			search.setValue(y);
		}

		System.out.println("Result....: " + search.getBestPoint());
		System.out.println("Solution..: 0.656");
		System.out.println("Iterations: " + search.getNumberOfIterations() + "\n");

		// Test 2
		System.out.println("2. Find minimum of f(x) = cos(x) on [0.0,6.0]:");

		final GoldenSectionSearch search2 = new GoldenSectionSearch(0.0, 6.0);
		while(search2.getAccuracy() > 1E-11 && !search2.isDone()) {
			final double x = search2.getNextPoint();

			final double y = Math.cos(x);

			search2.setValue(y);
		}

		System.out.println("Result....: " + search2.getBestPoint());
		System.out.println("Solution..: " + Math.PI + " (Pi)");
		System.out.println("Iterations: " + search2.getNumberOfIterations() + "\n");
	}

	/**
	 * @param leftPoint left point of search interval
	 * @param rightPoint right point of search interval
	 */
	public GoldenSectionSearch(final double leftPoint, final double rightPoint) {
		super();
		points[0]	= leftPoint;
		points[1]	= getGoldenSection(leftPoint, rightPoint);
		points[2]	= rightPoint;

		nextPoint	= points[0];
		accuracy	= points[2]-points[0];
	}

	/**
	 * @return Returns the best point obtained so far.
	 */
	public double getBestPoint() {
		// Lazy: we always return the middle point as best point
		return points[1];
	}

	/**
	 * Returns the next point for which a valuation is requested.
	 *
	 * @return Returns the next point for which a value should be set using <code>setValue</code>.
	 */
	public double getNextPoint() {
		expectingValue = true;
		return nextPoint;
	}

	/**
	 * Set the value corresponding to the point returned by a previous call of <code>getNextPoint()</code>.
	 * If setValue is called without prior call to getNextPoint(),
	 * e.g., when called twice, a RuntimeException is thrown.
	 *
	 * @param value Value corresponding to point returned by previous <code>getNextPoint()</code> call.
	 */
	public void setValue(final double value) {
		if(!expectingValue) {
			throw new RuntimeException("Call to setValue() perfomed without prior getNextPoint() call (e.g. call performed twice).");
		}

		if (numberOfIterations < 3) {
			/*
			 * Initially fill values
			 */
			values[numberOfIterations] = value;

			if (numberOfIterations < 2) {
				nextPoint = points[numberOfIterations + 1];
			} else {
				if (points[1] - points[0] > points[2] - points[1]) {
					nextPoint = getGoldenSection(points[0], points[1]);
				} else {
					nextPoint = getGoldenSection(points[1], points[2]);
				}
			}
		}
		else {
			/*
			 * Golden section search update rule
			 */
			if (points[1] - points[0] > points[2] - points[1]) {
				// The left interval is the large one
				if (value < values[1]) {
					/*
					 * Throw away right point
					 */
					points[2] = points[1];
					values[2] = values[1];

					points[1] = nextPoint;
					values[1] = value;
				} else {
					/*
					 * Throw away left point
					 */
					points[0] = nextPoint;
					values[0] = value;
				}
			} else {
				// The right interval is the large one
				if (value < values[1]) {
					/*
					 * Throw away left point
					 */
					points[0] = points[1];
					values[0] = values[1];

					points[1] = nextPoint;
					values[1] = value;
				} else {
					/*
					 * Throw away right point
					 */
					points[2] = nextPoint;
					values[2] = value;
				}
			}

			/*
			 * Update next point to ask value for (create point in larger interval)
			 */
			if (points[1] - points[0] > points[2] - points[1]) {
				nextPoint = getGoldenSection(points[0], points[1]);
			} else {
				nextPoint = getGoldenSection(points[1], points[2]);
			}

			/*
			 * Save belt: check if still improve or if we have reached machine accuracy
			 */
			if(points[2]-points[0] >= accuracy) {
				isDone = true;
			}
			accuracy = points[2]-points[0];
		}

		numberOfIterations++;
		expectingValue = false;
	}

	public GoldenSectionSearch optimize() {
		while(!isDone()) {
			final double parameter	= getNextPoint();
			final double value		= value(parameter);
			this.setValue(value);
		}
		return this;
	}

	public double value(final double parameter) {
		// You need to overwrite this mehtod with you own objective function
		throw new RuntimeException("Objective function not overwritten.");
	}

	/**
	 * Get the golden section of an interval as gs * left + (1-gs) * right, where
	 * gs is GOLDEN_SECTION_RATIO.
	 *
	 * @param left Left point of the interval.
	 * @param right Right point of the interval.
	 * @return Returns the golden section of an interval.
	 */
	public static double getGoldenSection(final double left, final double right) {
		return GOLDEN_SECTION_RATIO * left + (1.0 - GOLDEN_SECTION_RATIO) * right;
	}

	/**
	 * @return Returns the number of iterations needed so far.
	 */
	public int getNumberOfIterations() {
		return numberOfIterations;
	}

	/**
	 * @return Returns the accuracy obtained so far.
	 */
	public double getAccuracy() {
		return accuracy;
	}

	/**
	 * @return Returns true if the solver is unable to improve further. This may be either due to reached accuracy or due to no solution existing.
	 */
	public boolean isDone() {
		return isDone;
	}
}
