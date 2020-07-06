/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.11.2012
 */
package net.finmath.time;

import java.util.ArrayList;
import java.util.stream.DoubleStream;

/**
 * @author Christian Fries
 * @version 1.0
 */
public interface TimeDiscretization extends Iterable<Double> {

	/**
	 * @return Returns the number of time discretization points.
	 */
	int getNumberOfTimes();

	/**
	 * @return Returns the number of time steps (= number of discretization points-1).
	 */
	int getNumberOfTimeSteps();

	/**
	 * Returns the time for the given time index.
	 *
	 * @param timeIndex Time index
	 * @return Returns the time for a given time index.
	 */
	double getTime(int timeIndex);

	/**
	 * Returns the time step from the given time index to the next one.
	 *
	 * @param timeIndex Time index
	 * @return Returns the time step
	 */
	double getTimeStep(int timeIndex);

	/**
	 * Returns the time index for the given time. If the given time is not in the time discretization
	 * the method returns a negative number being (-insertionPoint-1).
	 *
	 * @param time The time.
	 * @return Returns the time index for a given time.
	 */
	int getTimeIndex(double time);

	/**
	 * Returns the time index for the time in the time discretization which is the nearest
	 * to the given time, being less or equal (i.e. max(i : timeDiscretizationFromArray[i] &le; time
	 * where timeDiscretizationFromArray[i] &le; timeDiscretizationFromArray[j]).
	 *
	 * @param time Given time.
	 * @return Returns a time index
	 */
	int getTimeIndexNearestLessOrEqual(double time);

	/**
	 * Returns the time index for the time in the time discretization which is the nearest
	 * to the given time, being greater or equal (i.e. min(i : timeDiscretizationFromArray[i] &ge; time
	 * where timeDiscretizationFromArray[i] &le; timeDiscretizationFromArray[j]).
	 *
	 * @param time Given time.
	 * @return Returns a time index
	 */
	int getTimeIndexNearestGreaterOrEqual(double time);

	/**
	 * Return a clone of this time discretization as <code>double[]</code>.
	 * @return The time discretization as <code>double[]</code>
	 */
	double[] getAsDoubleArray();

	/**
	 * Return a clone of this time discretization as <code>ArrayList&lt;Double&gt;</code>.
	 * Note that this method is costly in terms of performance.
	 *
	 * @return The time discretization as <code>ArrayList&lt;Double&gt;</code>
	 */
	ArrayList<Double> getAsArrayList();

	/**
	 * Return a DoubleStream of this time discretization.
	 *
	 * @return The time discretization as <code>DoubleStream</code>
	 */
	default DoubleStream doubleStream() {
		return DoubleStream.of(getAsDoubleArray());
	}

	/**
	 * Return a new time discretization where all time points have been shifted by
	 * a given time shift.
	 *
	 * @param timeShift A time shift applied to all discretization points.
	 * @return A new time discretization where all time points have been shifted by the given time shift.
	 */
	TimeDiscretization getTimeShiftedTimeDiscretization(double timeShift);

	/**
	 * Returns the union of this time discretization with another one. This means that the times of the other time discretization will be added.
	 * In case the tick sizes differ the union will have the smaller tick size, i. e. the finer precision.
	 * Note that when the differing tick sizes are not integer multiples of each other time points might get shifted due to rounding;
	 * for example <code>a.intersect(a.union(b))</code> might not be equal to <code>a</code>.
	 *
	 * @param that Another time discretization containing points to add to the time discretization.
	 * @return A new time discretization containing both the time points of this and the other discretization.
	 */
	TimeDiscretization union(TimeDiscretization that);

	/**
	 * Returns the intersection of this time discretization with another one. This means that all times not contained in the other time discretization will be removed.
	 * In case the tick sizes differ the intersection will have the greater tick size, i. e. the coarser precision.
	 * Note that when the differing tick sizes are not integer multiples of each other time points might get shifted due to rounding;
	 * for example <code>a.intersect(a.union(b))</code> might not be equal to <code>a</code>.
	 *
	 * @param that Another time discretization containing points to add to the time discretization.
	 * @return A new time discretization containing both the time points of this and the other discretization.
	 */
	TimeDiscretization intersect(TimeDiscretization that);

	/**
	 * Returns the smallest time span distinguishable in this time discretization.
	 * @return A non-negative double containing the tick size.
	 */
	double getTickSize();
}
