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

import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * This class represents a curve build from a set of points in 2D.
 * 
 * It provides different interpolation and extrapolation methods applied to a transformation of the input point,
 * examples are
 * <ul>
 * 	<li>linear interpolation of the input points</li>
 *  <li>linear interpolation of the log of the input points</li>
 *  <li>linear interpolation of the log of the input points divided by their respective time</li>
 * 	<li>cubic spline interpolation of the input points (or a function of the input points) (the curve will be C<sup>1</sup>).</li>
 * 	<li>Akima interpolation of the input points (or a function of the input points).</li>
 *  <li>etc.</li>
 * </ul>
 * 
 * <br>
 * 
 * For the interpolation methods provided see {@link net.finmath.marketdata.model.curves.Curve.InterpolationMethod}.
 * For the extrapolation methods provided see {@link net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod}.
 * For the possible interpolation entities see {@link net.finmath.marketdata.model.curves.Curve.InterpolationEntity}.
 * 
 * To construct the curve, please use the inner class CurveBuilder (a builder pattern).
 * 
 * For a demo on how to construct and/or calibrate a curve see, e.g.
 * net.finmath.tests.marketdata.curves.CurveTest.
 * 
 * @author Christian Fries
 */
public class Curve extends AbstractCurve implements Serializable, Cloneable {

	/**
	 * Possible interpolation methods.
	 * @author Christian Fries
	 */
	public enum InterpolationMethod {
		/** Constant interpolation. **/
		PIECEWISE_CONSTANT,
		/** Linear interpolation. **/
		LINEAR,
		/** Cubic spline interpolation. **/
		CUBIC_SPLINE,
		/** Akima interpolation (C1 sub-spline interpolation). **/
		AKIMA,
		/** Harmonic spline interpolation (C1 sub-spline interpolation). **/
		HARMONIC_SPLINE
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

	/**
	 * A builder (following the builder pattern) for Curve objects.
	 * Allows to successively construct a curve object by adding points.
	 * 
	 * @author Christian Fries
	 */
	public static class CurveBuilder implements CurveBuilderInterface {
		private Curve curve = null;
		
		/**
		 * Build a curve.
		 */
		public CurveBuilder() {
			curve = new Curve(null, null);
		}

		/**
		 * Build a curve with a given name and given reference date.
		 * 
		 * @param name The name of this curve.
		 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
		 */
		public CurveBuilder(String name, Calendar referenceDate) {
			curve = new Curve(name, referenceDate);
		}
		
		/**
		 * Build a curve by cloning a given curve.
		 * 
		 * @param curve A curve to be used as starting point for the new curve.
		 * @throws CloneNotSupportedException Thrown, when the curve could not be cloned.
		 */
		public CurveBuilder(Curve curve) throws CloneNotSupportedException {
			this.curve = (Curve)curve.clone();
		}
		
		/* (non-Javadoc)
		 * @see net.finmath.marketdata.model.curves.CurveBuilderInterface#build()
		 */
		@Override
		public CurveInterface build() throws CloneNotSupportedException {
			Curve buildCurve = curve;
			curve = null;
			return buildCurve;
		}

		/**
		 * Set the interpolation method of the curve.
		 * 
		 * @param interpolationMethod The interpolation method of the curve.
		 * @return A self reference to this curve build object.
		 */
		public CurveBuilderInterface setInterpolationMethod(InterpolationMethod interpolationMethod) {
			curve.interpolationMethod = interpolationMethod;
			return this;
		}

		/**
		 * Set the extrapolation method of the curve.
		 * 
		 * @param extrapolationMethod The extrapolation method of the curve.
		 * @return A self reference to this curve build object.
		 */
		public CurveBuilderInterface setExtrapolationMethod(ExtrapolationMethod extrapolationMethod) {
			curve.extrapolationMethod = extrapolationMethod;
			return this;
		}

		/**
		 * Set the interpolationEntity of the curve.
		 * 
		 * @param interpolationEntity The interpolation entity of the curve.
		 * @return A self reference to this curve build object.
		 */
		public CurveBuilderInterface setInterpolationEntity(InterpolationEntity interpolationEntity) {
			curve.interpolationEntity = interpolationEntity;
			return this;
		}
		
		/* (non-Javadoc)
		 * @see net.finmath.marketdata.model.curves.CurveBuilderInterface#addPoint(double, double, boolean)
		 */
		@Override
		public CurveBuilderInterface addPoint(double time, double value, boolean isParameter) {
			curve.addPoint(time, value, isParameter);
			return this;
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
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 */
	protected Curve(String name, Calendar referenceDate, InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity) {
		super(name, referenceDate);
		this.interpolationMethod	= interpolationMethod;
		this.extrapolationMethod	= extrapolationMethod;
		this.interpolationEntity	= interpolationEntity;
	}

	/**
	 * Create a curve with a given name, reference date.
	 * 
     * @param name The name of this curve.
     * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
     */
	private Curve(String name, Calendar referenceDate) {
		super(name, referenceDate);
	}

	@Override
    public double getValue(double time)
	{
		return valueFromInterpolationEntity(getInterpolationEntityValue(time), time);
	}

	@Override
    public double getValue(AnalyticModelInterface model, double time)
	{
		return valueFromInterpolationEntity(getInterpolationEntityValue(time), time);
	}
	
	private double getInterpolationEntityValue(double time)
	{
		synchronized(this) {
			// Lazy initialization of interpolation function
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
	 * @param isParameter If true, then this point is served via {@link #getParameter()} and changed via {@link #getCloneForParameter(double[])}, i.e., it can be calibrated.
	 */
	protected void addPoint(double time, double value, boolean isParameter) {
		if(interpolationEntity == InterpolationEntity.LOG_OF_VALUE_PER_TIME && time == 0) {
			if(value == 1.0 && isParameter == false) return;
			else throw new IllegalArgumentException("The interpolation method LOG_OF_VALUE_PER_TIME does not allow to add a value at time = 0 other than 1.0 (received " + value + ").");
		}

		double interpolationEntityValue = interpolationEntityFromValue(value, time);

		int index = getTimeIndex(time);
		if(index >= 0) {
			if(points.get(index).value == interpolationEntityValue) return;			// Already in list
			else return;//throw new RuntimeException("Trying to add a value for a time for which another value already exists.");
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
	
	/**
	 * Returns the interpolation method used by this curve.
	 * 
	 * @return The interpolation method used by this curve.
	 */
	public InterpolationMethod getInterpolationMethod() {
		return interpolationMethod;
	}

	/**
	 * Returns the extrapolation method used by this curve.
	 * 
	 * @return The extrapolation method used by this curve.
	 */
	public ExtrapolationMethod getExtrapolationMethod() {
		return extrapolationMethod;
	}

	/**
	 * Returns the interpolation entity used by this curve.
	 * 
	 * @return The interpolation entity used by this curve.
	 */
	public InterpolationEntity getInterpolationEntity() {
		return interpolationEntity;
	}

	protected int getTimeIndex(double time) {
		Point point = new Point(time, Double.NaN, false);
		return java.util.Collections.binarySearch(points, point);
	}

	protected int getParameterIndex(double time) {
		Point point = new Point(time, Double.NaN, false);
		return java.util.Collections.binarySearch(pointsBeingParameters, point);
	}
	
    @Override
    public double[] getParameter() {
    	double[] parameters = new double[pointsBeingParameters.size()];
    	for(int i=0; i<pointsBeingParameters.size(); i++) {
    		parameters[i] = valueFromInterpolationEntity(pointsBeingParameters.get(i).value, pointsBeingParameters.get(i).time);
    	}
    	return parameters;
    }

    @Override
    public void setParameter(double[] parameter) {
    	throw new UnsupportedOperationException("This class is immutable. Use getCloneForParameter(double[]) instead.");
    }
    
    private void setParameterPrivate(double[] parameter) {
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
			return Math.log(Math.max(value,0));
		case LOG_OF_VALUE_PER_TIME:
			if(time == 0)	throw new IllegalArgumentException("The interpolation method LOG_OF_VALUE_PER_TIME does not allow to add a value at time = 0.");
			else			return Math.log(Math.max(value,0)) / time;
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
	public Object clone() throws CloneNotSupportedException {
		Curve newCurve = (Curve) super.clone();

		newCurve.points					= new ArrayList<Point>();
		newCurve.pointsBeingParameters	= new ArrayList<Point>();
		newCurve.rationalFunctionInterpolation = null;
		for(Point point : points) {
			Point newPoint = (Point) point.clone();
			newCurve.points.add(newPoint);
			if(point.isParameter) newCurve.pointsBeingParameters.add(newPoint);
		}

		return newCurve;
	}

	@Override
	public CurveInterface getCloneForParameter(double[] parameter) throws CloneNotSupportedException {
		Curve newCurve = (Curve) this.clone();
		newCurve.setParameterPrivate(parameter);
		
		return newCurve;
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		CurveBuilder curveBuilder = new CurveBuilder(this);
		return curveBuilder;
	}
}
