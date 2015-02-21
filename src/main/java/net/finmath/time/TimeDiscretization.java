/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.04.2008
 */
package net.finmath.time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This class represents a set of discrete points in time.
 * <br>
 * It handles the mapping from time indices to time points and back.
 * It uses a time tick size ("quantum") of 1.0 / (365.0 * 24.0) (which corresponds to one hour if 1.0 is a non-leap-year):
 * Times are rounded to the nearest multiple of 1.0 / (365.0 * 24.0).
 * 
 * Objects of this class are immutable.
 * 
 * @author Christian Fries
 * @version 1.5
 */
public class TimeDiscretization implements Serializable, TimeDiscretizationInterface {

	private static final long serialVersionUID = 6880668325019167781L;

	private final double[]	timeDiscretization;
	private final double	timeTickSize = 1.0 / (365.0 * 24.0);

	public enum ShortPeriodLocation {
		SHORT_PERIOD_AT_START,
		SHORT_PERIOD_AT_END
	}

	/**
	 * Constructs a time discretization from a given set of doubles.
	 * The given array does not need to be sorted.
	 * 
	 * @param times Given array or arguments list of discretization points.
	 */
	public TimeDiscretization(double... times) {
		super();
		this.timeDiscretization = times.clone();
		java.util.Arrays.sort(this.timeDiscretization);
	}

	/**
	 * Constructs a time discretization from a given set of Doubles.
	 * The given array does not need to be sorted.
	 * 
	 * @param times Given array or arguments list of discretization points.
	 */
	public TimeDiscretization(Double[] times) {
		super();
		this.timeDiscretization = new double[times.length];
		for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++) this.timeDiscretization[timeIndex] = roundToTimeTickSize(times[timeIndex]);
		java.util.Arrays.sort(this.timeDiscretization);
	}

	/**
	 * Constructs a time discretization from a given ArrayList of Doubles.
	 * The given list does not need to be sorted.
	 * 
	 * @param timeDiscretization Given ArrayList of discretization points
	 */
	public TimeDiscretization(ArrayList<Double> timeDiscretization) {
		super();
		this.timeDiscretization = new double[timeDiscretization.size()];
		for(int timeIndex=0; timeIndex<timeDiscretization.size(); timeIndex++) this.timeDiscretization[timeIndex] = roundToTimeTickSize(timeDiscretization.get(timeIndex));
		java.util.Arrays.sort(this.timeDiscretization);
	}

	/**
	 * Constructs an equi-distant time discretization with points timeDiscretization[i] being
	 * <code>for(i=0; i &le; timeSteps; i++) timeDiscretization[i] = initial + i * deltaT;</code>
	 * 
	 * @param initial First discretization point.
	 * @param numberOfTimeSteps Number of time steps.
	 * @param deltaT Time step size.
	 */
	public TimeDiscretization(double initial, int numberOfTimeSteps, double deltaT) {
		super();
		timeDiscretization = new double[numberOfTimeSteps+1];
		for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++) timeDiscretization[timeIndex] = roundToTimeTickSize(initial + timeIndex * deltaT);
	}

	/**
	 * Constructs an equi-distant time discretization with stub periods at start or end.
	 * 
	 * @param initial First discretization point.
	 * @param last Last time steps.
	 * @param deltaT Time step size.
	 * @param shortPeriodLocation Placement of the stub period.
	 */
	public TimeDiscretization(double initial, double last, double deltaT, ShortPeriodLocation shortPeriodLocation) {
		super();
		int numberOfTimeSteps = (int)((last-initial)/ deltaT + 0.5);

		// Adjust for short period, if any
		if(roundToTimeTickSize(initial + numberOfTimeSteps * deltaT) < roundToTimeTickSize(last)) numberOfTimeSteps++;

		timeDiscretization = new double[numberOfTimeSteps+1];
		if(shortPeriodLocation == ShortPeriodLocation.SHORT_PERIOD_AT_END) {
			for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++) timeDiscretization[timeIndex] = roundToTimeTickSize(initial + timeIndex * deltaT);
			timeDiscretization[timeDiscretization.length-1] = last;
		}
		else {
			for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++) timeDiscretization[timeIndex] = roundToTimeTickSize(last - (numberOfTimeSteps-timeIndex) * deltaT);
			timeDiscretization[0] = initial;
		}
	}

	@Override
	public int getNumberOfTimes() {
		return timeDiscretization.length;
	}

	@Override
	public int getNumberOfTimeSteps() {
		return timeDiscretization.length-1;
	}

	@Override
	public double getTime(int timeIndex) {
		return timeDiscretization[timeIndex];
	}

	@Override
	public double getTimeStep(int timeIndex) {
		return timeDiscretization[timeIndex+1]-timeDiscretization[timeIndex];
	}

	@Override
	public int getTimeIndex(double time) {
		int index = java.util.Arrays.binarySearch(timeDiscretization,roundToTimeTickSize(time));
		return index;
	}

	@Override
	public int getTimeIndexNearestLessOrEqual(double time) {
		int index = java.util.Arrays.binarySearch(timeDiscretization,roundToTimeTickSize(time));
		if(index < 0) index = -index-2;
		return index;
	}

	@Override
	public int getTimeIndexNearestGreaterOrEqual(double time) {
		int index = java.util.Arrays.binarySearch(timeDiscretization,time);
		if(index < 0) index = -index-1;
		return index;
	}

	@Override
	public double[] getAsDoubleArray() {
		// Note: This is a deep copy
		return timeDiscretization.clone();
	}

	@Override
	public ArrayList<Double> getAsArrayList() {
		ArrayList<Double>	times = new ArrayList<Double>(timeDiscretization.length);
		for (double aTimeDiscretization : timeDiscretization) times.add(aTimeDiscretization);
		return times;
	}

	@Override
	public TimeDiscretizationInterface getTimeShiftedTimeDiscretization(double timeShift) {
		double[] newTimeDiscretization = new double[timeDiscretization.length];
		for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++)
			newTimeDiscretization[timeIndex] = roundToTimeTickSize(timeDiscretization[timeIndex]+timeShift);
		java.util.Arrays.sort(this.timeDiscretization);
		return new TimeDiscretization(newTimeDiscretization);
	}

	@Override
	public Iterator<Double> iterator() {
		return this.getAsArrayList().iterator();
	}

	@Override
	public String toString() {
		return "TimeDiscretization [timeDiscretization="
				+ Arrays.toString(timeDiscretization) + ", timeTickSize="
				+ timeTickSize + "]";
	}

	private double roundToTimeTickSize(double time) {
		return Math.rint(time/timeTickSize)*timeTickSize;
	}
}
