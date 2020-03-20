/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.marketdata.model.curves.locallinearregression;

import java.util.Arrays;

/**
 * This class represents a set of discrete points in time with weighted interval reference points.
 *
 * @author Moritz Scherrmann
 * @version 1.0
 */
public class Partition {

	private final double[] points;
	private final double weight;
	private final double[] referencePoints;

	/**
	 * Creates a partition.
	 *
	 * @param points The points of the partition. It should be kept in mind that no point should be included twice.
	 *               There is no need to take care of the order of the points.
	 * @param weight The weight if the partition as double. It is needed to compute the reference points.
	 */
	public Partition(
			final double[] points,
			final double weight
			){
		this.points=points;
		this.weight=weight;
		Arrays.sort(this.points);
		referencePoints=new double[points.length-1];
		for(int i=0; i<referencePoints.length;i++) {
			referencePoints[i]=(1-weight)*points[i]+weight*points[i+1];
		}
	}

	/**
	 * Creates a partition with fixed weight=0.5.
	 *
	 * @param points The points of the partition. It should be kept in mind that no point should be included twice.
	 *               There is no need to take care of the order of the points.
	 */
	public Partition(
			final double[] points
			){
		this(points,0.5);
	}

	/**
	 * Returns for a given x the number of the interval where x is included. Note that the intervals are of the type
	 * [x_i,x_{i+1}). The first interval has number 1, the second number 2 and so on. If x is smaller than the minimum
	 * of the partition, the method return 0. If x is greater or equal the maximum of the partition, it returns the
	 * length of the partition.
	 *
	 * @param x The point of interest.
	 * @return The number of the intervals which contains x.
	 */
	public int getIntervalNumber(final double x) {
		if (x<points[0]) {
			return 0;
		} else if (x>=points[points.length-1]) {
			return points.length;
		}
		for(int i=0; i<points.length-2;i++) {
			if ( x< points[i+1]) {
				return i+1;
			}
		}
		return points.length-1;
	}

	/**
	 * If a given x is into an interval of the partition, this method returns the reference point of the corresponding interval.
	 * If the given x is not contained in any interval of the partition, this method returns x.
	 *
	 * @param x The point of interest.
	 * @return The discretized value.
	 */
	public double d(final double x){
		final int intervalNumber =getIntervalNumber(x);
		if (intervalNumber==0 || intervalNumber==points.length) {
			return x;
		}
		return getIntervalReferencePoint(intervalNumber-1);
	}


	public double[] getReferencePoints(){
		return referencePoints;
	}

	public double getIntervalReferencePoint(final int intervalIndex) {
		return referencePoints[intervalIndex];
	}

	public double[] getPoints() {
		return points;
	}

	public double getPoint(final int pointIndex) {
		return points[pointIndex];
	}

	public int getLength() {
		return points.length;
	}

	public int getNumberOfIntervals() {
		return points.length-1;
	}

	public double getIntervalLength(final int intervalIndex) {
		return points[intervalIndex+1]-points[intervalIndex];
	}

	public double getWeight() {
		return weight;
	}


}
