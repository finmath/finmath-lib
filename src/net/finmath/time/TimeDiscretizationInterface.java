/*
 * Created on 23.11.2012
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.time;

import java.util.ArrayList;

/**
 * @author Christian Fries
 */
public interface TimeDiscretizationInterface extends java.lang.Iterable<Double> {

	/**
	 * Returns the time for the given time index.
	 * 
	 * @param timeIndex Time index
	 * @return Returns the time for a given time index.
	 */
	public abstract double getTime(int timeIndex);

	/**
	 * Returns the time step from the given time index to the next one.
	 * 
	 * @param timeIndex Time index
	 * @return Returns the time step
	 */
	public abstract double getTimeStep(int timeIndex);

	/**
	 * Returns the time index for the given time. If the given time is not in the time discretization
	 * the method returns a negative number being (-insertionPoint-1).
	 * 
	 * @param time The time.
	 * @return Returns the time index for a given time.
	 */
	public abstract int getTimeIndex(double time);

	/**
	 * Returns the time index for the time in the time discretization which is the nearest
	 * to the given time, being less or equal (i.e. max(i : timeDiscretization[i] <= time
	 * where timeDiscretization[i] <= timeDiscretization[j]).
	 * 
	 * @param time Given time.
	 * @return Returns a time index
	 */
	public abstract int getTimeIndexNearestLessOrEqual(double time);

	/**
	 * Returns the time index for the time in the time discretization which is the nearest
	 * to the given time, being greater or equal (i.e. min(i : timeDiscretization[i] >= time
	 * where timeDiscretization[i] <= timeDiscretization[j]).
	 * 
	 * @param time Given time.
	 * @return Returns a time index
	 */
	public abstract int getTimeIndexNearestGreaterOrEqual(double time);

	/**
	 * @return Returns the number of time discretization points.
	 */
	public abstract int getNumberOfTimes();

	/**
	 * @return Returns the number of time steps (= number of discretization points-1).
	 */
	public abstract int getNumberOfTimeSteps();

	/**
	 * Return a clone of this time discretization as <code>double[]</code>.
	 * @return The time discretization as <code>double[]</code>
	 */
	public abstract double[] getAsDoubleArray();

	/**
	 * Return a clone of this time discretization as <code>ArrayList<Double></code>.
	 * Note that this method is costly in terms of performance.
	 * 
	 * @return The time discretization as <code>ArrayList<Double></code>
	 */
	public abstract ArrayList<Double> getAsArrayList();

}