/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.Vector;

import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * This class represents a curve build from a set of points in 2D.
 * It provides different interpolation and extrapolation methods applied to a transformation of the input point,
 * examples are
 * <ul>
 * 	<li>linear interpolation of the input points</li>
 *  <li>linear interpolation of the log of the input points</li>
 *  <li>linear interpolation of the log of the input points divided by their respective time</li>
 * 	<li>cubic spline of the input points</li>
 *  <li>etc.</li>
 * </ul>
 * For the interpolation methods provided see {@link net.finmath.marketdata.model.curves.Curve.InterpolationMethod}.
 * For the extrapolation methods provided see {@link net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod}.
 * For the possible interpolation entities see {@link net.finmath.marketdata.model.curves.Curve.InterpolationEntity}.
 * 
 * @author Christian Fries
 */
public class Curve extends AbstractCurve implements Serializable {

	/**
	 * Possible interpolation methods.
	 * @author Christian Fries
	 */
	public enum InterpolationMethod {
		/** Linear interpolation. **/
		LINEAR,
		/** Cubic spline interpolation. **/
		CUBIC_SPLINE
	}

	/**
	 * Possible extrapolation methods.
	 * @author Christian Fries
	 */
	public enum ExtrapolationMethod {
		/** Constant extrapolation. **/
		CONSTANT,
		/** Linear extrapolation. **/
		LINEAR
	}

	/**
	 * Possible interpolation entities.
	 * @author Christian Fries
	 */
	public enum InterpolationEntity {
		/** Interpolation is performed on the native point values, i.e. value(t) **/
		VALUE,
		/** Interpolation is performed on the log of the point values, i.e. log(value(t)) **/
		LOG_OF_VALUE,
		/** Interpolation is performed on the log of the point values divided by their respective time, i.e. log(value(t))/t **/
		LOG_OF_VALUE_PER_TIME
	}

	private static class Point implements Comparable<Point>, Serializable {
        private static final long serialVersionUID = 8857387999991917430L;

        public double time;
		public double value;
		public boolean isParameter;

		/**
         * @param time
         * @param value
         */
        public Point(double time, double value, boolean isParameter) {
	        super();
	        this.time = time;
	        this.value = value;
	        this.isParameter = isParameter;
        }

		@Override
        public int compareTo(Point point) {
			if(this.time < point.time) return -1;
			if(this.time > point.time) return +1;

			return 0;
		}
		
		@Override
        public Object clone() {
			return new Point(time,value,isParameter);
		}
	}

	private ArrayList<Point>	points					= new ArrayList<Point>();
	private ArrayList<Point>	pointsBeingParameters	= new ArrayList<Point>();
	private InterpolationMethod	interpolationMethod	= InterpolationMethod.CUBIC_SPLINE;
	private ExtrapolationMethod	extrapolationMethod = ExtrapolationMethod.CONSTANT;
	private InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE;
	
	private RationalFunctionInterpolation rationalFunctionInterpolation =  null;

	private static final long serialVersionUID = -4126228588123963885L;
	static NumberFormat	formatterReal = NumberFormat.getInstance(Locale.US);
	

	/**
	 * Create a curve with a given name, reference date and an interpolation method.
	 * 
     * @param name The name of this curve.
     * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation mehtod used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 */
	public Curve(String name, Calendar referenceDate, InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		super(name, referenceDate);
		this.interpolationMethod	= interpolationMethod;
		this.extrapolationMethod	= extrapolationMethod;
		this.interpolationEntity	= interpolationEntity;
	}

	
	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
    public double getValue(double time)
	{
		return getValue(null, time);
	}


	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
    public double getValue(AnalyticModelInterface model, double time)
	{
		return valueFromInterpolationEntity(getInterpolationEntityValue(time), time);
	}
	
	private double getInterpolationEntityValue(double time)
	{
		synchronized(this) {
			if(rationalFunctionInterpolation == null) {
				double[] pointsArray = new double[points.size()];
				double[] valuesArray = new double[points.size()];
				for(int i=0; i<points.size(); i++) {
					pointsArray[i] = points.get(i).time;
					valuesArray[i] = points.get(i).value;
				}
				rationalFunctionInterpolation = new RationalFunctionInterpolation(
						pointsArray,
						valuesArray,
						RationalFunctionInterpolation.InterpolationMethod.valueOf(this.interpolationMethod.toString()),
						RationalFunctionInterpolation.ExtrapolationMethod.valueOf(this.extrapolationMethod.toString())
						);
			}
		}
		return rationalFunctionInterpolation.getValue(time);
	}

	
	/**
	 * Add a point to this curve. The method will throw an exception if the point
	 * is already part of the curve.
	 * 
	 * @param time The x<sub>i</sub> in <sub>i</sub> = f(x<sub>i</sub>).
	 * @param value The y<sub>i</sub> in <sub>i</sub> = f(x<sub>i</sub>).
	 * @param isParameter If true, then this point is server via {@link #getParameter()} and changed via {@link #setParameter(double[])} and {@link #getCloneForParameter(double[])}, i.e., it can be calibrated.
	 */
	public void addPoint(double time, double value, boolean isParameter) {
		double interpolationEntityValue = interpolationEntityFromValue(value, time);

		int index = getTimeIndex(time);
		if(index >= 0) {
			if(points.get(index).value == interpolationEntityValue) return;			// Already in list
			else throw new RuntimeException("Trying to add a value for a time for which another value already exists.");
		}
		else {
			// Insert the new point, retain ordering.
			Point point = new Point(time, interpolationEntityValue, isParameter);
			points.add(-index-1, point);
	
			if(isParameter) {
				// Add this point also to the list of parameters
				int parameterIndex = getParameterIndex(time);
				if(parameterIndex >= 0) new RuntimeException("Curve inconsistent.");
				pointsBeingParameters.add(-parameterIndex-1, point);
			}
		}
    	this.rationalFunctionInterpolation = null;
	}
	
	protected int getTimeIndex(double maturity) {
		Point df = new Point(maturity, Double.NaN, false);
		return java.util.Collections.binarySearch(points, df);
	}

	protected int getParameterIndex(double maturity) {
		Point df = new Point(maturity, Double.NaN, false);
		return java.util.Collections.binarySearch(pointsBeingParameters, df);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.marketdata.calibration.UnconstrainedParameterVectorInterface#getParameter()
	 */
    @Override
    public double[] getParameter() {
    	double[] parameters = new double[pointsBeingParameters.size()];
    	for(int i=0; i<pointsBeingParameters.size(); i++) {
    		parameters[i] = valueFromInterpolationEntity(pointsBeingParameters.get(i).value, pointsBeingParameters.get(i).time);
    	}
    	return parameters;
    }

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.calibration.UnconstrainedParameterVectorInterface#setParameter(double[])
	 */
    @Override
    public void setParameter(double[] parameter) {
    	for(int i=0; i<pointsBeingParameters.size(); i++) {
    		pointsBeingParameters.get(i).value = interpolationEntityFromValue(parameter[i], pointsBeingParameters.get(i).time);
    	}
    	this.rationalFunctionInterpolation = null;
    }

	public String toString() {
		String objectAsString = super.toString() + "\n";
        for (Point point : points) {
            objectAsString = objectAsString + point.time + "\t" + valueFromInterpolationEntity(point.value, point.time) + "\n";
        }
		return objectAsString;
	}
	
	private double interpolationEntityFromValue(double value, double time) {
		switch(interpolationEntity) {
		case VALUE:
		default:
			return value;
		case LOG_OF_VALUE:
			return Math.log(value);
		case LOG_OF_VALUE_PER_TIME:
			if(time == 0)	return 1;
			else			return Math.log(value) / time;
		}
	}

	private double valueFromInterpolationEntity(double interpolationEntityValue, double time) {
		switch(interpolationEntity) {
		case VALUE:
		default:
			return interpolationEntityValue;
		case LOG_OF_VALUE:
			return Math.exp(interpolationEntityValue);
		case LOG_OF_VALUE_PER_TIME:
			return Math.exp(interpolationEntityValue * time);
		}
	}

	@Override
	public CurveInterface getCloneForParameter(double[] parameter) {
		Curve newCurve = null;
		try {
			newCurve = (Curve) this.clone();
		} catch (CloneNotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		newCurve.points					= new ArrayList<Point>();
		newCurve.pointsBeingParameters	= new ArrayList<Point>();
		for(Point point : points) {
			Point newPoint = (Point) point.clone();
			newCurve.points.add(newPoint);
			if(point.isParameter) newCurve.pointsBeingParameters.add(newPoint);
		}

		newCurve.setParameter(parameter);
		
		return newCurve;
	}

}
