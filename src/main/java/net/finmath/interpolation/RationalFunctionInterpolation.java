/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 28.05.2004
 */
package net.finmath.interpolation;

import java.util.Arrays;

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
		/** Constant interpolation. Synonym of PIECEWISE_CONSTANT_LEFTPOINT. **/
		PIECEWISE_CONSTANT,
		/** Constant interpolation. Right continuous, i.e. using the value of the left end point of the interval. **/
		PIECEWISE_CONSTANT_LEFTPOINT,
		/** Constant interpolation using the value of the right end point of the interval. **/
		PIECEWISE_CONSTANT_RIGHTPOINT,
		/** Linear interpolation. **/
		LINEAR,
		/** Cubic spline interpolation. **/
		CUBIC_SPLINE,
		/** Akima interpolation (C1 sub-spline interpolation). **/
		AKIMA,
		/** Harmonic spline interpolation (C1 sub-spline interpolation). **/		
		HARMONIC_SPLINE
	}

    public enum ExtrapolationMethod {
		/** Extrapolation using the interpolation function of the adjacent interval **/
		DEFAULT,
		/** Constant extrapolation. **/
		CONSTANT,
		/** Linear extrapolation. **/
		LINEAR
	}

    // The curve to interpolate
	private final double[]	points;
	private final double[]	values;
	
	private InterpolationMethod	interpolationMethod = InterpolationMethod.LINEAR;
	private ExtrapolationMethod	extrapolationMethod = ExtrapolationMethod.DEFAULT;
	
	private class RationalFunction {
		public final double[] coefficientsNumerator;
		public final double[] coefficientsDenominator;

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

		// Get interpolating rational function for the given point x
		int pointIndex = java.util.Arrays.binarySearch(points, x);
		if(pointIndex >= 0) return values[pointIndex];
		
		int intervallIndex = -pointIndex-2;
		
		// Check for extrapolation
		if(intervallIndex < 0) {
			// Extrapolation
			if(this.extrapolationMethod == ExtrapolationMethod.CONSTANT)	return values[0];
			else if(this.extrapolationMethod == ExtrapolationMethod.LINEAR)		return values[0]+(values[1]-values[0])/(points[1]-points[0])*(x-points[0]);
			else intervallIndex = 0;
		}
		if(intervallIndex > points.length-2) {
			// Extrapolation
			if(this.extrapolationMethod == ExtrapolationMethod.CONSTANT) return values[points.length-1];
			else if(this.extrapolationMethod == ExtrapolationMethod.LINEAR)		return values[points.length-1]+(values[points.length-2]-values[points.length-1])/(points[points.length-2]-points[points.length-1])*(x-points[points.length-1]);
			else intervallIndex = points.length-2;
		}
		
		RationalFunction rationalFunction = interpolatingRationalFunctions[intervallIndex];
		
		// Calculate interpolating value
		double numerator	= getRationalFunctionValue(rationalFunction.coefficientsNumerator,		x-points[intervallIndex]);
		double denominator	= getRationalFunctionValue(rationalFunction.coefficientsDenominator,	x-points[intervallIndex]);
		
		return numerator / denominator;
	}
	
	private static double getRationalFunctionValue(double[] polynomialCoefficients, double x)
	{
		double value	= 0.0;
		double powerOfX	= 1.0;

        for (double polynomialCoefficient : polynomialCoefficients) {
            value += polynomialCoefficient * powerOfX;
            powerOfX *= x;
        }
		
		return value;
	}
	
	private synchronized void doCreateRationalFunctions()
	{
		switch(interpolationMethod)
		{
			case PIECEWISE_CONSTANT:
			case PIECEWISE_CONSTANT_LEFTPOINT:
			case PIECEWISE_CONSTANT_RIGHTPOINT:
				doCreateRationalFunctionsForPiecewiseConstantInterpolation();
				break;
			case LINEAR:
			default:
				doCreateRationalFunctionsForLinearInterpolation();
				break;
			case CUBIC_SPLINE:
				doCreateRationalFunctionsForCubicSplineInterpolation();
				break;
			case AKIMA:
				doCreateRationalFunctionsForAkimaInterpolation();
				break;
			case HARMONIC_SPLINE:
				doCreateRationalFunctionsForHarmonicSplineInterpolation();
				break;
		}
	}

	private synchronized void doCreateRationalFunctionsForPiecewiseConstantInterpolation()
	{
		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[points.length-1];
		
		// denominator is always 1.0
		double[] denominatorPolynomCoeff	= { 1.0 };
	
		// create numerator polynomials (constant)
		for(int pointIndex = 0; pointIndex < points.length-1; pointIndex++ ) {
			double[] numeratorPolynomCoeff;
			if (interpolationMethod == InterpolationMethod.PIECEWISE_CONSTANT_RIGHTPOINT)	numeratorPolynomCoeff = new double[] {values[pointIndex+1]};
			else																			numeratorPolynomCoeff = new double[] {values[pointIndex]};
			interpolatingRationalFunctions[pointIndex] = new RationalFunction(numeratorPolynomCoeff, denominatorPolynomCoeff);			
		}
	}
	
	private synchronized void doCreateRationalFunctionsForLinearInterpolation()
	{
		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[points.length-1];
		
		// denominator is always 1.0
		double[] denominatorPolynomCoeff	= { 1.0 };
	
		// create numerator polynomials (linear)
		for(int pointIndex = 0; pointIndex < points.length-1; pointIndex++ ) {
			double[] numeratorPolynomCoeff		= new double[2];
			
			double xl = points[pointIndex];
			double xr = points[pointIndex+1];

			double fl = values[pointIndex];
			double fr = values[pointIndex+1];
			
			numeratorPolynomCoeff[1] = (fr-fl) / (xr-xl);
			numeratorPolynomCoeff[0] = fl;
			
			interpolatingRationalFunctions[pointIndex] = new RationalFunction(numeratorPolynomCoeff, denominatorPolynomCoeff);			
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

		// denominator is always 1.0
		double[] denominatorPolynomCoeff	= { 1.0 };

		// create numerator polynomials (third order polynomial)
		for(int i = 0; i < numberOfPoints-1; i++ ) {
			double[] numeratortorPolynomCoeff		= new double[4];

			numeratortorPolynomCoeff[0] = values[i];
			numeratortorPolynomCoeff[1] = (values[i+1] - values[i])/step[i] - (secondDerivativeVector[i+1] + 2*secondDerivativeVector[i])*step[i]/6;
			numeratortorPolynomCoeff[2] = secondDerivativeVector[i] / 2;
			numeratortorPolynomCoeff[3] = (secondDerivativeVector[i+1] - secondDerivativeVector[i]) / (6*step[i]);
			
			interpolatingRationalFunctions[i] = new RationalFunction(numeratortorPolynomCoeff, denominatorPolynomCoeff);			
		}
	}

	private void doCreateRationalFunctionsForAkimaInterpolation()
	{
		int numberOfPoints = points.length;
		
		if(numberOfPoints < 4) {
			// Akima interpolation not possible
			doCreateRationalFunctionsForCubicSplineInterpolation();
		}
		else {
			// Calculate slopes
			double[] step = new double[numberOfPoints-1];
			double[] slope = new double[numberOfPoints-1];
			double[] absSlopeDifference	= new double[numberOfPoints-2];
			for(int i = 0; i < numberOfPoints-1; i++){
				step[i]		= (points[i+1] - points[i]);
				slope[i]	= (values[i+1] - values[i]) / step[i];
				if(i > 0) {
					absSlopeDifference[i-1] = Math.abs(slope[i] - slope[i-1]);// + 2E-4;
				}
			}
			
			// Calculate first derivatives ...
			double[] derivative = new double[numberOfPoints];
			
			// ... for the 2 left and 2 right outer boundary points:
			// in t_0
			derivative[0] = 0.5 * (3 * slope[0] - slope[1]);
			
			// in t_1
			if((absSlopeDifference[1] == 0) && (absSlopeDifference[0] == 0)){
				derivative[1] = (step[1] * slope[0] + step[0] * slope[1]) / (step[0] + step[1]);
			}
			else{
				derivative[1] = (absSlopeDifference[1] * slope[0] + absSlopeDifference[0] * slope[1]) 
						/ (absSlopeDifference[1] + absSlopeDifference[0]);
			}

			// in t_{n-1}
			if((absSlopeDifference[numberOfPoints-3] == 0) && (absSlopeDifference[numberOfPoints-4] == 0)){
				derivative[numberOfPoints-2] = (step[numberOfPoints-2] * slope[numberOfPoints-3] + step[numberOfPoints-3] * slope[numberOfPoints-2]) / (step[numberOfPoints-3] + step[numberOfPoints-2]);
			}
			else{
				derivative[numberOfPoints-2] = 
						(absSlopeDifference[numberOfPoints-3] * slope[numberOfPoints-3] + absSlopeDifference[numberOfPoints-4] * slope[numberOfPoints-2])
							/ (absSlopeDifference[numberOfPoints-3] + absSlopeDifference[numberOfPoints-4]);
			}

			// in t_n
			derivative[numberOfPoints-1] = 0.5 * (3 * slope[numberOfPoints-2] - slope[numberOfPoints-3]);
			
			// ... for inner points:
			for(int i = 2; i < numberOfPoints-2; i++){
				// Check if denominator would be zero
				if( (absSlopeDifference[i] == 0) && (absSlopeDifference[i-2] == 0) ){
					// Take Convention
					derivative[i] = (step[i] * slope[i-1] + step[i-1] * slope[i]) / (step[i-1] + step[i]);
				}
				else{
					derivative[i] = 
							(absSlopeDifference[i] * slope[i-1] + absSlopeDifference[i-2] * slope[i])
								/ (absSlopeDifference[i] + absSlopeDifference[i-2]);
				}
			}
			
			/*
			 * Generate a rational function for each given interval
			 */
			interpolatingRationalFunctions = new RationalFunction[numberOfPoints-1];
			
			// denominator is always 1.0
			double[] denominatorPolynomCoeff	= { 1.0 };

			// create numerator polynomials (third order polynomial)
			for(int i = 0; i < numberOfPoints-1; i++ ) {
				double[] numeratorPolynomCoeff		= new double[4];

				numeratorPolynomCoeff[0] = values[i];
				numeratorPolynomCoeff[1] = derivative[i];
				numeratorPolynomCoeff[2] = (3*slope[i] - 2*derivative[i] - derivative[i+1]) / step[i];
				numeratorPolynomCoeff[3] = (derivative[i] + derivative[i+1] - 2*slope[i]) / (step[i] * step[i]);

				interpolatingRationalFunctions[i] = new RationalFunction(numeratorPolynomCoeff, denominatorPolynomCoeff);			
			}
		}
	}
	
	private void doCreateRationalFunctionsForHarmonicSplineInterpolation(){
		int numberOfPoints = points.length;
		
		// Calculate parameters
		double[] step = new double[numberOfPoints-1];
		double[] slope = new double[numberOfPoints-1];
		double[] doubleStep	= new double[numberOfPoints-2];
		for(int i = 0; i < numberOfPoints-1; i++){
			step[i] = (points[i+1] - points[i]);
			slope[i] = (values[i+1] - values[i]) / step[i];
			if(i > 0){
				doubleStep[i-1] = points[i+1] - points[i-1];
			}
		}
		
		// Calculate first derivatives ...
		double[] derivative = new double[numberOfPoints];
		
		// ... for boundary points:
		// in t_0
		derivative[0] =(2*step[0] + step[1])/doubleStep[0] * slope[0] - step[0]/doubleStep[0] * slope[1];
		// monotonicity filtering
		if( (derivative[0]*slope[0] > 0) && (slope[0]*slope[1] <= 0) && (Math.abs(derivative[0]) < 3*Math.abs(slope[0])))
			derivative[0] = 3 * slope[0];
		if( derivative[0]*slope[0] <= 0 )
			derivative[0] = 0;
		
		// in t_n
		derivative[numberOfPoints-1] =(2*step[numberOfPoints-2] + step[numberOfPoints-3])/doubleStep[numberOfPoints-3] * slope[numberOfPoints-2] 
										- step[numberOfPoints-2]/doubleStep[numberOfPoints-3] * slope[numberOfPoints-3];
		// monotonicity filtering
		if( (derivative[numberOfPoints-1]*slope[numberOfPoints-2] > 0) && (slope[numberOfPoints-2]*slope[numberOfPoints-3] <= 0) 
				&& (Math.abs(derivative[numberOfPoints-1]) < 3*Math.abs(slope[numberOfPoints-2])))
			derivative[numberOfPoints-1] = 3 * slope[numberOfPoints-2];
		if( derivative[numberOfPoints-1]*slope[numberOfPoints-2] <= 0 )
			derivative[numberOfPoints-1] = 0;
		
		// ... for inner points:
		for(int i = 1; i < numberOfPoints-1; i++){
			if( slope[i-1] * slope[i] <= 0 ){
				derivative[i] = 0;
			}
			else{
				double weightedHarmonicMean = (step[i-1] + 2*step[i]) / (3*doubleStep[i-1]*slope[i-1]) 
												+ (2*step[i-1] + step[i]) / (3*doubleStep[i-1]*slope[i]);
				derivative[i] = 1.0 / weightedHarmonicMean;
			}
		}
		
		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[numberOfPoints-1];
		
		// denominator is always 1.0
		double[] denominatorPolynomCoeff	= { 1.0 };

		// create numerator polynomials (third order polynomial)
		for(int i = 0; i < numberOfPoints-1; i++ ) {
			double[] numeratortorPolynomCoeff		= new double[4];

			numeratortorPolynomCoeff[0] = values[i];
			numeratortorPolynomCoeff[1] = derivative[i];
			numeratortorPolynomCoeff[2] = (3*slope[i] - 2*derivative[i] - derivative[i+1]) / step[i];
			numeratortorPolynomCoeff[3] = (derivative[i] + derivative[i+1] - 2*slope[i]) / (step[i] * step[i]);

			interpolatingRationalFunctions[i] = new RationalFunction(numeratortorPolynomCoeff, denominatorPolynomCoeff);			
		}
			
	}

	public static void main(String[] args) {

		/**
		 * Example. Shows how to use this class.
		 */
		final int samplePoints = 200;

		/*
		 * Given input points
		 */
		double[] givenPoints		= { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 };
		double[] givenValues		= { 5.0, 6.0, 4.0, 7.0, 5.0, 6.0 };
		
		System.out.println("Interplation of given input points (x,y):");
		System.out.println("  x: " + Arrays.toString(givenPoints));
		System.out.println("  y: " + Arrays.toString(givenValues));
		System.out.println("");


		// Create interpolated curve
		System.out.println("Default:");
		RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(givenPoints, givenValues);

		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println("");

		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.AKIMA, ExtrapolationMethod.CONSTANT);
		System.out.println("AKIMA:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println("");

		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.CUBIC_SPLINE, ExtrapolationMethod.CONSTANT);
		System.out.println("CUBIC_SPLINE:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println("");
		
		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.PIECEWISE_CONSTANT, ExtrapolationMethod.CONSTANT);
		System.out.println("PIECEWISE_CONSTANT:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println("");
		
		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.HARMONIC_SPLINE, ExtrapolationMethod.CONSTANT);
		System.out.println("HARMONIC_SPLINE:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println("");
	}
}
