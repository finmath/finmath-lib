/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.04.2008
 */
package net.finmath.time;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.function.IntToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * This class represents a set of discrete points in time.
 * <br>
 * It handles the mapping from time indices to time points and back.
 * It uses a time tick size ("quantum"). This is to make comparison of times safe.
 * The default tick size is 1.0 / (365.0 * 24.0) (which corresponds to one hour if 1.0 is a non-leap-year):
 * Times are rounded to the nearest multiple of 1.0 / (365.0 * 24.0).
 *
 * This property can be configured via a System.setProperty("net.finmath.functions.TimeDiscretization.timeTickSize").
 *
 * Objects of this class are immutable.
 *
 * @author Christian Fries
 * @version 1.6
 */
public class TimeDiscretizationFromArray implements Serializable, TimeDiscretization {

	private static final long serialVersionUID = 6880668325019167781L;
	private static final double timeTickSizeDefault = Double.parseDouble(System.getProperty("net.finmath.functions.TimeDiscretization.timeTickSize", Double.toString(1.0 / (365.0 * 24.0))));

	private final double[]	timeDiscretization;
	private final double timeTickSize;

	public enum ShortPeriodLocation {
		SHORT_PERIOD_AT_START,
		SHORT_PERIOD_AT_END
	}

	/**
	 * Constructs a time discretization using the given tick size.
	 * The time discretization will be sorted. Duplicate entries are allowed if <code>allowDuplicates</code> is true, otherwise duplicate entries are removed.
	 *
	 * @param times A non closed and not necessarily sorted stream containing the time points.
	 * @param tickSize A non-negative double representing the smallest time span distinguishable.
	 * @param allowDuplicates If true, the time discretization allows duplicate entries.
	 */
	public TimeDiscretizationFromArray(DoubleStream times, final double tickSize, final boolean allowDuplicates) {
		timeTickSize = tickSize;
		times = times.map(this::roundToTimeTickSize);
		if(!allowDuplicates) {
			times = times.distinct();
		}
		timeDiscretization = times.sorted().toArray();
	}

	/**
	 * Constructs a time discretization using the given tick size.
	 *
	 * @param times    A non closed and not necessarily sorted stream containing the time points.
	 * @param tickSize A non-negative double representing the smallest time span distinguishable.
	 */
	public TimeDiscretizationFromArray(final DoubleStream times, final double tickSize) {
		this(times, tickSize, false);
	}

	/**
	 * Constructs a time discretization from a (non closed and not necessarily sorted) stream of doubles.
	 * The time discretization will be sorted. Duplicate entries are allowed if <code>allowDuplicates</code> is true, otherwise duplicate entries are removed.
	 *
	 * @param times A double stream of time points for the time discretization.
	 * @param allowDuplicates If true, the time discretization allows duplicate entries.
	 */
	public TimeDiscretizationFromArray(final DoubleStream times, final boolean allowDuplicates) {
		this(times, timeTickSizeDefault, allowDuplicates);
	}

	/**
	 * Constructs a time discretization from a (non closed and not necessarily sorted) stream of doubles.
	 *
	 * @param times A double stream of time points for the time discretization.
	 */
	public TimeDiscretizationFromArray(final DoubleStream times) {
		this(times, timeTickSizeDefault, false);
	}

	/**
	 * Constructs a time discretization using the given tick size.
	 *
	 * @param times    A non closed and not necessarily sorted stream containing the time points.
	 * @param tickSize A non-negative double representing the smallest time span distinguishable.
	 * @param allowDuplicates If true, the time discretization allows duplicate entries.
	 */
	public TimeDiscretizationFromArray(final Stream<Double> times, final double tickSize, final boolean allowDuplicates) {
		this(times.mapToDouble(Double::doubleValue), tickSize, allowDuplicates);
	}

	/**
	 * Constructs a time discretization using the given tick size.
	 * The time discretization will be sorted. Duplicate entries are allowed if <code>allowDuplicates</code> is true, otherwise duplicate entries are removed.
	 *
	 * @param times    A non closed and not necessarily sorted stream containing the time points.
	 * @param tickSize A non-negative double representing the smallest time span distinguishable.
	 */
	public TimeDiscretizationFromArray(final Stream<Double> times, final double tickSize) {
		this(times.mapToDouble(Double::doubleValue), tickSize, false);
	}

	/**
	 * Constructs a time discretization from a (non closed and not necessarily sorted) stream of boxed doubles.
	 *
	 * @param times A double stream of time points for the time discretization.
	 */
	public TimeDiscretizationFromArray(final Stream<Double> times) {
		this(times.mapToDouble(Double::doubleValue), timeTickSizeDefault, false);
	}

	/**
	 * Constructs a time discretization using the given tick size.
	 * The iteration of the iterable does not have to happen in order.
	 *
	 * @param times The time to constitute the time discretization.
	 * @param tickSize A non-negative double representing the smallest time span distinguishable.
	 * @param allowDuplicates If true, the time discretization allows duplicate entries.
	 */
	public TimeDiscretizationFromArray(final Iterable<Double> times, final double tickSize, final boolean allowDuplicates) {
		this(StreamSupport.stream(times.spliterator(), false), tickSize, allowDuplicates);
	}

	/**
	 * Constructs a time discretization using the given tick size.
	 * The iteration of the iterable does not have to happen in order.
	 *
	 * @param times The time to constitute the time discretization.
	 * @param tickSize A non-negative double representing the smallest time span distinguishable.
	 */
	public TimeDiscretizationFromArray(final Iterable<Double> times, final double tickSize) {
		this(times, tickSize, false);
	}

	/**
	 * Constructs a time discretization from an iterable of doubles.
	 * The iteration does not have to happen in order.
	 *
	 * @param times The time to constitute the time discretization.
	 * @param allowDuplicates If true, the time discretization allows duplicate entries.
	 */
	public TimeDiscretizationFromArray(final Iterable<Double> times, final boolean allowDuplicates) {
		this(times,timeTickSizeDefault, allowDuplicates);
	}

	/**
	 * Constructs a time discretization from an iterable of doubles.
	 * The iteration does not have to happen in order.
	 *
	 * @param times The time to constitute the time discretization.
	 */
	public TimeDiscretizationFromArray(final Iterable<Double> times) {
		this(times, false);
	}

	/**
	 * Constructs a time discretization from a given set of doubles.
	 * The given array does not need to be sorted.
	 *
	 * @param times Given array or arguments list of discretization points.
	 */
	public TimeDiscretizationFromArray(final double... times) {
		this(Arrays.stream(times));
	}

	/**
	 * Constructs a time discretization from a given set of Doubles.
	 * The given array does not need to be sorted.
	 *
	 * @param times Given boxed array of discretization points.
	 */
	public TimeDiscretizationFromArray(final Double[] times) {
		this(Arrays.stream(times));
	}

	/**
	 * Constructs a time discretization using the given tick size.
	 * The given array does not need to be sorted.
	 *
	 * @param times Given boxed array of discretization points.
	 * @param tickSize A non-negative double representing the smallest time span distinguishable.
	 */
	public TimeDiscretizationFromArray(final Double[] times, final double tickSize) {
		this(Arrays.stream(times), tickSize);
	}

	/**
	 * Constructs an equi-distant time discretization with points timeDiscretizationFromArray[i] being
	 * <code>for(i=0; i &le; timeSteps; i++) timeDiscretizationFromArray[i] = initial + i * deltaT;</code>
	 *
	 * @param initial First discretization point.
	 * @param numberOfTimeSteps Number of time steps.
	 * @param deltaT Time step size.
	 */
	public TimeDiscretizationFromArray(final double initial, final int numberOfTimeSteps, final double deltaT) {
		this(IntStream.range(0, numberOfTimeSteps + 1).mapToDouble(new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int n) {
				return initial + n * deltaT;
			}
		}));
	}

	/**
	 * Constructs an equi-distant time discretization with stub periods at start or end.
	 *
	 * @param initial First discretization point.
	 * @param last Last time steps.
	 * @param deltaT Time step size.
	 * @param shortPeriodLocation Placement of the stub period.
	 */
	public TimeDiscretizationFromArray(final double initial, final double last, final double deltaT, final ShortPeriodLocation shortPeriodLocation) {
		this(getEquidistantStreamWithStub(initial, last, deltaT, shortPeriodLocation));
	}

	private static DoubleStream getEquidistantStreamWithStub(final double initial, final double last, final double deltaT, final ShortPeriodLocation shortPeriodLocation) {
		final int numberOfTimeStepsPlusOne = (int) Math.ceil((last - initial) / deltaT) + 1;

		if (shortPeriodLocation == ShortPeriodLocation.SHORT_PERIOD_AT_END) {
			return IntStream.range(0, numberOfTimeStepsPlusOne).mapToDouble(new IntToDoubleFunction() {
				@Override
				public double applyAsDouble(final int n) {
					return Math.min(last, initial + n * deltaT);
				}
			});
		}

		return IntStream.range(0, numberOfTimeStepsPlusOne).mapToDouble(new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int n) {
				return Math.max(initial, last - n * deltaT);
			}
		});
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
	public double getTime(final int timeIndex) {
		return timeDiscretization[timeIndex];
	}

	@Override
	public double getTimeStep(final int timeIndex) {
		return timeDiscretization[timeIndex + 1] - timeDiscretization[timeIndex];
	}

	@Override
	public int getTimeIndex(final double time) {
		return Arrays.binarySearch(timeDiscretization, roundToTimeTickSize(time));
	}

	@Override
	public int getTimeIndexNearestLessOrEqual(final double time) {
		int index = java.util.Arrays.binarySearch(timeDiscretization,roundToTimeTickSize(time));
		if(index < 0) {
			index = -index-2;
		}
		return index;
	}

	@Override
	public int getTimeIndexNearestGreaterOrEqual(final double time) {
		int index = java.util.Arrays.binarySearch(timeDiscretization,time);
		if(index < 0) {
			index = -index-1;
		}
		return index;
	}

	@Override
	public double[] getAsDoubleArray() {
		// Note: This is a deep copy
		return timeDiscretization.clone();
	}

	@Override
	public ArrayList<Double> getAsArrayList() {
		final ArrayList<Double>	times = new ArrayList<>(timeDiscretization.length);
		for (final double aTimeDiscretization : timeDiscretization) {
			times.add(aTimeDiscretization);
		}
		return times;
	}

	@Override
	public TimeDiscretization getTimeShiftedTimeDiscretization(final double timeShift) {
		final double[] newTimeDiscretization = new double[timeDiscretization.length];

		for (int timeIndex = 0; timeIndex < timeDiscretization.length; timeIndex++) {
			newTimeDiscretization[timeIndex] = roundToTimeTickSize(timeDiscretization[timeIndex] + timeShift);
		}

		return new TimeDiscretizationFromArray(newTimeDiscretization);
	}

	/**
	 * @param that Another time discretization containing points to add to the time discretization.
	 * @return A new time discretization containing both the time points of this and the other discretization.
	 */
	@Override
	public TimeDiscretization union(final TimeDiscretization that) {
		return new TimeDiscretizationFromArray(
				Stream.concat(Arrays.stream(timeDiscretization).boxed(), Arrays.stream(that.getAsDoubleArray()).boxed()),
				Math.min(timeTickSize, that.getTickSize()));
	}

	@Override
	public TimeDiscretization intersect(final TimeDiscretization that) {
		final Set<Double> intersectionSet = Arrays.stream(timeDiscretization).boxed().collect(Collectors.toSet());
		intersectionSet.retainAll(that.getAsArrayList());

		return new TimeDiscretizationFromArray(intersectionSet, Math.max(timeTickSize, that.getTickSize()));
	}

	@Override
	public double getTickSize() {
		return timeTickSize;
	}

	@Override
	public Iterator<Double> iterator() {
		return this.getAsArrayList().iterator();
	}

	@Override
	public String toString() {
		return "TimeDiscretizationFromArray [timeDiscretizationFromArray="
				+ Arrays.toString(timeDiscretization) + ", timeTickSize="
				+ timeTickSize + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(timeDiscretization);
		long temp;
		temp = Double.doubleToLongBits(timeTickSize);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		final TimeDiscretizationFromArray other = (TimeDiscretizationFromArray) obj;
		if (!Arrays.equals(timeDiscretization, other.timeDiscretization)) {
			return false;
		}
		return Double.doubleToLongBits(timeTickSize) == Double.doubleToLongBits(other.timeTickSize);
	}

	private double roundToTimeTickSize(final double time) {
		return Math.rint(time/timeTickSize)*timeTickSize;
	}
}
