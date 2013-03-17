/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.05.2004
 */
package net.finmath.interpolation;

import cern.colt.matrix.DoubleMatrix1D;
import cern.colt.matrix.DoubleMatrix2D;
import cern.colt.matrix.impl.DenseDoubleMatrix1D;
import cern.colt.matrix.impl.DenseDoubleMatrix2D;

/**
 * This class provides methodologies to interpolate given sample points by
 * rational functions, that is, given interpolation points (x<sub>i</sub>,y<sub>i</sub>)
 * the class provides a continuous function y = f(x) where
 * <ul>
 * 	<li>
 * 		f(x<sub>i</sub>) = y<sub>i</sub> and
 * 	</li>
 * <li>
 *		for x<sub>i</sub> &lt; x &lt; x<sub>i+1</sub> the function is a fraction of two polynomes
 * f(x) = (sum a<sub>j</sub> x<sup>j</sup>) / (sum b<sub>k</sub> x<sup>k</sup>).
 * </li>
 * </ul>
 * 
 * This setup comprises linear interpolation (for which the function is C<sup>0</sup>) and
 * cubic spline interpolation (for which the function is C<sup>1</sup>).
 * 
 * @author Christian Fries
 * @version 1.3
 */
public class RationalFunctionInterpolation {

	public enum InterpolationMethod {
		LINEAR,
		CUBIC_SPLINE
	};

	public enum ExtrapolationMethod {
		CONSTANT,
		LINEAR
	};
	
	// The curve to interpolate
	private double[]	points;
	private double[]	values;
	
	private InterpolationMethod	interpolationMethod = InterpolationMethod.LINEAR;
	private ExtrapolationMethod	extrapolationMethod = ExtrapolationMethod.CONSTANT;
	
	private class RationalFunction {
		public double[] coefficientsNumerator;
		public double[] coefficientsDenominator;

		/**
         * @param coefficientsNumerator
         * @param coefficientsDenominator
         */
        public RationalFunction(double[] coefficientsNumerator, double[] coefficientsDenominator) {
	        super();
	        this.coefficientsNumerator = coefficientsNumerator;
	        this.coefficientsDenominator = coefficientsDenominator;
        }
	}
	
	// The interpolated curve - a rational function for each interval (one less than number of points)
	private RationalFunction[]	interpolatingRationalFunctions;

	/**
	 * Generate a rational function interpolation from a given set of points.
	 * 
	 * @param points The array of the x<sub>i</sub> sample points of a function y=f(x).
	 * @param values The corresponding array of the y<sub>i</sub> sample values to the sample points x<sub>i</sub>.
	 */
	public RationalFunctionInterpolation(double[] points, double[] values) {
		super();
		this.points = points;
		this.values = values;
	}
	
	/**
	 * Generate a rational function interpolation from a given set of points using
	 * the specified interpolation and extrapolation method.
	 * 
	 * @param points The array of the x<sub>i</sub> sample points of a function y=f(x).
	 * @param values The corresponding array of the y<sub>i</sub> sample values to the sample points x<sub>i</sub>.
	 * @param interpolationMethod The interpolation method to be used.
	 * @param extrapolationMethod The extrapolation method to be used.
	 */
	public RationalFunctionInterpolation(double[] points, double[] values,  InterpolationMethod interpolationMethod, ExtrapolationMethod extrapolationMethod) {
		super();
		this.points = points;
		this.values = values;
		this.interpolationMethod = interpolationMethod;
		this.extrapolationMethod = extrapolationMethod;
	}

	/**
	 * @return Returns the interpolationMethod.
	 */
	public InterpolationMethod getInterpolationMethod() {
		return interpolationMethod;
	}
	
	/**
	 * @param interpolationMethod The interpolationMethod to set.
	 */
	@Deprecated
	public void setInterpolationMethod(InterpolationMethod interpolationMethod) {
		if(interpolatingRationalFunctions != null) interpolatingRationalFunctions = null;		// Invalidate old calculations
		this.interpolationMethod = interpolationMethod;
	}

	/**
	 * Get an interpolated value for a given argument x.
	 * 
	 * @param x The abscissa at which the interpolation should be performed.
	 * @return The interpolated value (ordinate).
	 */
	public double getValue(double x)
	{
		synchronized(this) {
			if(interpolatingRationalFunctions == null) doCreateRationalFunctions();
		}

		// Get interpolating rational function for the given strike
		int pointIndex = java.util.Arrays.binarySearch(points, x);
		if(pointIndex >= 0) return values[pointIndex];
		
		int intervallIndex = -pointIndex-2;
		
		// Check for extrapolation
		if(intervallIndex < 0) {
			// Extrapolation
			if(this.extrapolationMethod == ExtrapolationMethod.CONSTANT) return values[0];
			else intervallIndex = 0;
		}
		if(intervallIndex > points.length-2) {
			// Extrapolation
			if(this.extrapolationMethod == ExtrapolationMethod.CONSTANT) return values[points.length-1];
			else intervallIndex = points.length-2;
		}
		
		RationalFunction rationalFunction = interpolatingRationalFunctions[intervallIndex];
		
		// Calculate interpolating value
		double numerator	= getRationalFunctionValue(rationalFunction.coefficientsNumerator,		x-points[intervallIndex]);
		double denominator	= getRationalFunctionValue(rationalFunction.coefficientsDenominator,	x-points[intervallIndex]);
		
		return numerator / denominator;
	}
	
	private double getRationalFunctionValue(double[] polynomialCoefficients, double x)
	{
		double value	= 0.0;
		double powerOfX	= 1.0;
		
		for(int coefficientIndex = 0; coefficientIndex<polynomialCoefficients.length; coefficientIndex++) {
			value		+= polynomialCoefficients[coefficientIndex] * powerOfX;
			powerOfX	*= x;
		}
		
		return value;
	}
	
	private synchronized void doCreateRationalFunctions()
	{
		switch(interpolationMethod)
		{
			case LINEAR:
			default:
				doCreateRationalFunctionsForLinearInterpolation();
				break;
			case CUBIC_SPLINE:
				doCreateRationalFunctionsForCubicSplineInterpolation();
				break;
		}
	}

	private synchronized void doCreateRationalFunctionsForLinearInterpolation()
	{
		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[points.length-1];
		
		for(int pointIndex = 0; pointIndex < points.length-1; pointIndex++ ) {
			double[] nominatorPolynomCoeff		= new double[2];
			double[] denominatorPolynomCoeff	= new double[1];
			
			double xl = points[pointIndex];
			double xr = points[pointIndex+1];

			double fl = values[pointIndex];
			double fr = values[pointIndex+1];
			
			nominatorPolynomCoeff[1] = (fr-fl) / (xr-xl);
			nominatorPolynomCoeff[0] = fl;
			denominatorPolynomCoeff[0] = 1;
			
			interpolatingRationalFunctions[pointIndex] = new RationalFunction(nominatorPolynomCoeff, denominatorPolynomCoeff);			
		}
	}
		
	private void doCreateRationalFunctionsForCubicSplineInterpolation()
	{
		int numberOfPoints = points.length;
		
		// Calculate interval lengths
		double[] step = new double[numberOfPoints-1];
		for (int i=0; i<numberOfPoints-1; i++ )	step[i] = points[i+1] - points[i];

		/*
		 * Calculate 2nd derivatives of given function at states by solving
		 * a linear system of equations (secondDerivativeMarix * secondDerivativeVector = v).
		 */
		double[] secondDerivativeVector	= new double[numberOfPoints];		

		DoubleMatrix2D secondDerivativeMarix	= new DenseDoubleMatrix2D(numberOfPoints,numberOfPoints);	secondDerivativeMarix.assign(0.0);
		DoubleMatrix1D v 						= new DenseDoubleMatrix1D(numberOfPoints);					v.assign(0.0);

		// Initialize A and b
		secondDerivativeMarix.set(0,	 0,	1.0);
		secondDerivativeMarix.set(numberOfPoints-1,	numberOfPoints-1,	1.0);
		for (int intervallIndex=1; intervallIndex<numberOfPoints-1; intervallIndex++)
		{
			v.set(intervallIndex, 6 * ( (values[intervallIndex+1] - values[intervallIndex])/step[intervallIndex] - (values[intervallIndex]-values[intervallIndex-1]) / step[intervallIndex-1]));
			
			secondDerivativeMarix.set(intervallIndex, intervallIndex-1, step[intervallIndex-1]);
			secondDerivativeMarix.set(intervallIndex, intervallIndex  , 2 * (step[intervallIndex-1] + step[intervallIndex]));
			secondDerivativeMarix.set(intervallIndex, intervallIndex+1, step[intervallIndex]);
		}

		// Solve equation
		cern.colt.matrix.linalg.Algebra linAlg = new cern.colt.matrix.linalg.Algebra();
		secondDerivativeVector = linAlg.mult(linAlg.inverse(secondDerivativeMarix),v).toArray();

		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[numberOfPoints-1];

		for(int i = 0; i < numberOfPoints-1; i++ ) {
			double[] nominatorPolynomCoeff		= new double[4];
			double[] denominatorPolynomCoeff	= new double[1];
						
			nominatorPolynomCoeff[0] = values[i];
			nominatorPolynomCoeff[1] = (values[i+1] - values[i])/step[i] - (secondDerivativeVector[i+1] + 2*secondDerivativeVector[i])*step[i]/6;
			nominatorPolynomCoeff[2] = secondDerivativeVector[i] / 2;
			nominatorPolynomCoeff[3] = (secondDerivativeVector[i+1] - secondDerivativeVector[i]) / (6*step[i]);
			denominatorPolynomCoeff[0] = 1;
			
			interpolatingRationalFunctions[i] = new RationalFunction(nominatorPolynomCoeff, denominatorPolynomCoeff);			
		}
	}

	public static void main(String[] args) {

		/**
		 * Example. Shows how to use this class.
		 */
		final int samplePoints = 200;
		
		double[] givenPoints		= { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 };
		double[] givenValues		= { 5.0, 6.0, 4.0, 7.0, 5.0, 6.0 };
		
		RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(givenPoints,
				givenValues);

		// Create interpolated curve
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
	}
}
