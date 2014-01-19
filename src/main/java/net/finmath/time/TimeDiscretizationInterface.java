/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 23.11.2012
 */
package net.finmath.time;

import java.util.ArrayList;

/**
 * @author Christian Fries
 */
public interface TimeDiscretizationInterface extends Iterable<Double> {

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
	 * to the given time, being less or equal (i.e. max(i : timeDiscretization[i] &le; time
	 * where timeDiscretization[i] &le; timeDiscretization[j]).
	 * 
	 * @param time Given time.
	 * @return Returns a time index
	 */
    int getTimeIndexNearestLessOrEqual(double time);

	/**
	 * Returns the time index for the time in the time discretization which is the nearest
	 * to the given time, being greater or equal (i.e. min(i : timeDiscretization[i] &ge; time
	 * where timeDiscretization[i] &le; timeDiscretization[j]).
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
     * Return a new time discretization where all time points have been shifted by
     * a given time shift.
     * 
     * @param timeShift A time shift applied to all discretization points.
     * @return A new time discretization where all time points have been shifted by the given time shift.
     */
    TimeDiscretizationInterface getTimeShiftedTimeDiscretization(double timeShift);
}