/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.05.2004
 */
package net.finmath.interpolation;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import net.finmath.functions.LinearAlgebra;

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
public class RationalFunctionInterpolation implements DoubleUnaryOperator, Serializable {

	private static final long serialVersionUID = -3214160594013393575L;

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
		/** Akima interpolation (C1 sub-spline interpolation) with a smoothing in the weights. **/
		AKIMA_CONTINUOUS,
		/** Harmonic spline interpolation (C1 sub-spline interpolation). **/
		HARMONIC_SPLINE,
		/** Harmonic spline interpolation (C1 sub-spline interpolation) with a monotonic filtering at the boundary points. **/
		HARMONIC_SPLINE_WITH_MONOTONIC_FILTERING

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

	private static class RationalFunction implements Serializable {

		private static final long serialVersionUID = -1596026703859403853L;

		private final double[] coefficientsNumerator;
		private final double[] coefficientsDenominator;

		/**
		 * Create a rational interpolation function.
		 *
		 * @param coefficientsNumerator The coefficients of the polynomial of the numerator, in increasing order.
		 * @param coefficientsDenominator The coefficients of the polynomial of the denominator, in increasing order.
		 */
		RationalFunction(final double[] coefficientsNumerator, final double[] coefficientsDenominator) {
			super();
			this.coefficientsNumerator = coefficientsNumerator;
			this.coefficientsDenominator = coefficientsDenominator;
		}

		/**
		 * Create a polynomial interpolation function.
		 *
		 * @param coefficients The coefficients of the polynomial, in increasing order.
		 */
		RationalFunction(final double[] coefficients) {
			this(coefficients, null);
		}

		/**
		 * Returns the value for a given arguments.
		 *
		 * @param x Given argument.
		 * @return Returns the value for the given argument.
		 */
		public double getValue(final double x)
		{
			double valueNumerator	= 0.0;
			double valueDenominator	= 0.0;
			double powerOfX	= 1.0;

			for (final double polynomialCoefficient : coefficientsNumerator) {
				valueNumerator += polynomialCoefficient * powerOfX;
				powerOfX *= x;
			}

			if(coefficientsDenominator == null) {
				return valueNumerator;
			}

			powerOfX	= 1.0;
			for (final double polynomialCoefficient : coefficientsDenominator) {
				valueDenominator += polynomialCoefficient * powerOfX;
				powerOfX *= x;
			}

			return valueNumerator/valueDenominator;
		}
	}

	// The interpolated curve - a rational function for each interval (one less than number of points)
	private RationalFunction[]	interpolatingRationalFunctions;
	private transient Object		interpolatingRationalFunctionsLazyInitLock = new Object();

	/**
	 * Generate a rational function interpolation from a given set of points.
	 *
	 * @param points The array of the x<sub>i</sub> sample points of a function y=f(x).
	 * @param values The corresponding array of the y<sub>i</sub> sample values to the sample points x<sub>i</sub>.
	 */
	public RationalFunctionInterpolation(final double[] points, final double[] values) {
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
	public RationalFunctionInterpolation(final double[] points, final double[] values,  final InterpolationMethod interpolationMethod, final ExtrapolationMethod extrapolationMethod) {
		super();
		this.points = points;
		this.values = values;
		this.interpolationMethod = interpolationMethod;
		this.extrapolationMethod = extrapolationMethod;
	}

	/**
	 * Returns the interpolation method used.
	 *
	 * @return Returns the interpolationMethod.
	 */
	public InterpolationMethod getInterpolationMethod() {
		return interpolationMethod;
	}

	/**
	 * Get an interpolated value for a given argument x.
	 *
	 * @param x The abscissa at which the interpolation should be performed.
	 * @return The interpolated value (ordinate).
	 */
	public double getValue(final double x)
	{
		synchronized(interpolatingRationalFunctionsLazyInitLock) {
			if(interpolatingRationalFunctions == null) {
				doCreateRationalFunctions();
			}
		}

		// Get interpolating rational function for the given point x
		final int pointIndex = java.util.Arrays.binarySearch(points, x);
		if(pointIndex >= 0) {
			return values[pointIndex];
		}

		int intervalIndex = -pointIndex-2;

		// Check for extrapolation
		if(intervalIndex < 0) {
			// Extrapolation
			if(extrapolationMethod == ExtrapolationMethod.CONSTANT) {
				return values[0];
			} else if(extrapolationMethod == ExtrapolationMethod.LINEAR) {
				return values[0]+(values[1]-values[0])/(points[1]-points[0])*(x-points[0]);
			} else {
				intervalIndex = 0;
			}
		}
		else if(intervalIndex > points.length-2) {
			// Extrapolation
			if(extrapolationMethod == ExtrapolationMethod.CONSTANT) {
				return values[points.length-1];
			} else if(extrapolationMethod == ExtrapolationMethod.LINEAR) {
				return values[points.length-1]+(values[points.length-2]-values[points.length-1])/(points[points.length-2]-points[points.length-1])*(x-points[points.length-1]);
			} else {
				intervalIndex = points.length-2;
			}
		}

		final RationalFunction rationalFunction = interpolatingRationalFunctions[intervalIndex];

		// Calculate interpolating value
		return rationalFunction.getValue(x-points[intervalIndex]);
	}

	private void doCreateRationalFunctions()
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
		case AKIMA_CONTINUOUS:
			doCreateRationalFunctionsForAkimaInterpolation(1E-02);
			break;
		case HARMONIC_SPLINE:
			doCreateRationalFunctionsForHarmonicSplineInterpolation();
			break;
		case HARMONIC_SPLINE_WITH_MONOTONIC_FILTERING:
			doCreateRationalFunctionsForHarmonicSplineInterpolation();
			break;
		}
	}

	private void doCreateRationalFunctionsForPiecewiseConstantInterpolation()
	{
		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[points.length-1];

		// create numerator polynomials (constant)
		for(int pointIndex = 0; pointIndex < points.length-1; pointIndex++ ) {
			double[] numeratorPolynomCoeff;
			if (interpolationMethod == InterpolationMethod.PIECEWISE_CONSTANT_RIGHTPOINT) {
				numeratorPolynomCoeff = new double[] {values[pointIndex+1]};
			} else {
				numeratorPolynomCoeff = new double[] {values[pointIndex]};
			}
			interpolatingRationalFunctions[pointIndex] = new RationalFunction(numeratorPolynomCoeff);
		}
	}

	private void doCreateRationalFunctionsForLinearInterpolation()
	{
		if(points.length == 0) {
			throw new IllegalArgumentException("Interpolation requested on curve with no points.");
		}

		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[points.length-1];

		// create numerator polynomials (linear)
		for(int pointIndex = 0; pointIndex < points.length-1; pointIndex++ ) {
			final double[] numeratorPolynomCoeff		= new double[2];

			final double xl = points[pointIndex];
			final double xr = points[pointIndex+1];

			final double fl = values[pointIndex];
			final double fr = values[pointIndex+1];

			numeratorPolynomCoeff[1] = (fr-fl) / (xr-xl);
			numeratorPolynomCoeff[0] = fl;

			interpolatingRationalFunctions[pointIndex] = new RationalFunction(numeratorPolynomCoeff);
		}
	}

	private void doCreateRationalFunctionsForCubicSplineInterpolation()
	{
		final int numberOfPoints = points.length;

		// Calculate interval lengths
		final double[] step = new double[numberOfPoints-1];
		for (int i=0; i<numberOfPoints-1; i++ ) {
			step[i] = points[i+1] - points[i];
		}

		/*
		 * Calculate 2nd derivatives of given function at states by solving
		 * a linear system of equations (secondDerivativeMarix * secondDerivativeVector = v).
		 */
		double[] secondDerivativeVector	= new double[numberOfPoints];

		final double[][]	secondDerivativeMarix	= new double[numberOfPoints][numberOfPoints];
		final double[]	v						= new double[numberOfPoints];

		// Initialize A and b
		secondDerivativeMarix[0][0] = 1.0;
		secondDerivativeMarix[numberOfPoints-1][numberOfPoints-1] = 1.0;
		v[0] = 0.0;
		v[numberOfPoints-1] = 0.0;

		for (int intervalIndex=1; intervalIndex<numberOfPoints-1; intervalIndex++)
		{
			v[intervalIndex] =  6 * ( (values[intervalIndex+1] - values[intervalIndex])/step[intervalIndex] - (values[intervalIndex]-values[intervalIndex-1]) / step[intervalIndex-1]);

			secondDerivativeMarix[intervalIndex][intervalIndex-1] = step[intervalIndex-1];
			secondDerivativeMarix[intervalIndex][intervalIndex  ] = 2 * (step[intervalIndex-1] + step[intervalIndex]);
			secondDerivativeMarix[intervalIndex][intervalIndex+1] = step[intervalIndex];
		}
		// Making it a symmetric matrix
		if(numberOfPoints > 1)	{
			secondDerivativeMarix[0][1] 								= secondDerivativeMarix[1][0];
			secondDerivativeMarix[numberOfPoints-2][numberOfPoints-1]	= secondDerivativeMarix[numberOfPoints-1][numberOfPoints-2];
		}

		// Solve equation
		secondDerivativeVector = LinearAlgebra.solveLinearEquationSymmetric(secondDerivativeMarix, v);

		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[numberOfPoints-1];

		// create numerator polynomials (third order polynomial)
		for(int i = 0; i < numberOfPoints-1; i++ ) {
			final double[] numeratortorPolynomCoeff		= new double[4];

			numeratortorPolynomCoeff[0] = values[i];
			numeratortorPolynomCoeff[1] = (values[i+1] - values[i])/step[i] - (secondDerivativeVector[i+1] + 2*secondDerivativeVector[i])*step[i]/6;
			numeratortorPolynomCoeff[2] = secondDerivativeVector[i] / 2;
			numeratortorPolynomCoeff[3] = (secondDerivativeVector[i+1] - secondDerivativeVector[i]) / (6*step[i]);

			interpolatingRationalFunctions[i] = new RationalFunction(numeratortorPolynomCoeff);
		}
	}

	private void doCreateRationalFunctionsForAkimaInterpolation()
	{
		doCreateRationalFunctionsForAkimaInterpolation(0.0);
	}

	private void doCreateRationalFunctionsForAkimaInterpolation(final double minSlopeDifferenceWeight)
	{
		final int numberOfPoints = points.length;

		if(numberOfPoints < 4) {
			// Akima interpolation not possible
			doCreateRationalFunctionsForCubicSplineInterpolation();
		}
		else {
			// Calculate slopes
			final double[] step = new double[numberOfPoints-1];
			final double[] slope = new double[numberOfPoints-1];
			final double[] absSlopeDifference	= new double[numberOfPoints-2];
			for(int i = 0; i < numberOfPoints-1; i++){
				step[i]		= (points[i+1] - points[i]);
				slope[i]	= (values[i+1] - values[i]) / step[i];
				if(i > 0) {
					absSlopeDifference[i-1] = Math.abs(slope[i] - slope[i-1]) + minSlopeDifferenceWeight;
				}
			}

			// Calculate first derivatives ...
			final double[] derivative = new double[numberOfPoints];

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

			// create numerator polynomials (third order polynomial)
			for(int i = 0; i < numberOfPoints-1; i++ ) {
				final double[] numeratorPolynomCoeff		= new double[4];

				numeratorPolynomCoeff[0] = values[i];
				numeratorPolynomCoeff[1] = derivative[i];
				numeratorPolynomCoeff[2] = (3*slope[i] - 2*derivative[i] - derivative[i+1]) / step[i];
				numeratorPolynomCoeff[3] = (derivative[i] + derivative[i+1] - 2*slope[i]) / (step[i] * step[i]);

				interpolatingRationalFunctions[i] = new RationalFunction(numeratorPolynomCoeff);
			}
		}
	}

	private void doCreateRationalFunctionsForHarmonicSplineInterpolation(){
		final int numberOfPoints = points.length;

		// Calculate parameters
		final double[] step = new double[numberOfPoints-1];
		final double[] slope = new double[numberOfPoints-1];
		final double[] doubleStep	= new double[numberOfPoints-2];
		for(int i = 0; i < numberOfPoints-1; i++){
			step[i] = (points[i+1] - points[i]);
			slope[i] = (values[i+1] - values[i]) / step[i];
			if(i > 0){
				doubleStep[i-1] = points[i+1] - points[i-1];
			}
		}

		// Calculate first derivatives ...
		final double[] derivative = new double[numberOfPoints];

		// ... for boundary points:
		// in t_0
		derivative[0] =(2*step[0] + step[1])/doubleStep[0] * slope[0] - step[0]/doubleStep[0] * slope[1];

		// in t_n
		derivative[numberOfPoints-1] =(2*step[numberOfPoints-2] + step[numberOfPoints-3])/doubleStep[numberOfPoints-3] * slope[numberOfPoints-2]
				- step[numberOfPoints-2]/doubleStep[numberOfPoints-3] * slope[numberOfPoints-3];

		//  monotonicity filtering
		if(interpolationMethod == InterpolationMethod.HARMONIC_SPLINE_WITH_MONOTONIC_FILTERING){
			// in t_0
			if((derivative[0]*slope[0] > 0) && (slope[0]*slope[1] <= 0) && (Math.abs(derivative[0]) < 3*Math.abs(slope[0]))) {
				derivative[0] = 3 * slope[0];
			}
			if( derivative[0]*slope[0] <= 0 ) {
				derivative[0] = 0;
			}

			// in t_n
			if((derivative[numberOfPoints-1]*slope[numberOfPoints-2] > 0) && (slope[numberOfPoints-2]*slope[numberOfPoints-3] <= 0)
					&& (Math.abs(derivative[numberOfPoints-1]) < 3*Math.abs(slope[numberOfPoints-2]))) {
				derivative[numberOfPoints-1] = 3 * slope[numberOfPoints-2];
			}
			if( derivative[numberOfPoints-1]*slope[numberOfPoints-2] <= 0 ) {
				derivative[numberOfPoints-1] = 0;
			}
		}

		// ... for inner points:
		for(int i = 1; i < numberOfPoints-1; i++){
			if( slope[i-1] * slope[i] <= 0 ){
				derivative[i] = 0;
			}
			else{
				final double weightedHarmonicMean = (step[i-1] + 2*step[i]) / (3*doubleStep[i-1]*slope[i-1])
						+ (2*step[i-1] + step[i]) / (3*doubleStep[i-1]*slope[i]);
				derivative[i] = 1.0 / weightedHarmonicMean;
			}
		}

		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[numberOfPoints-1];

		// create numerator polynomials (third order polynomial)
		for(int i = 0; i < numberOfPoints-1; i++ ) {
			final double[] numeratortorPolynomCoeff		= new double[4];

			numeratortorPolynomCoeff[0] = values[i];
			numeratortorPolynomCoeff[1] = derivative[i];
			numeratortorPolynomCoeff[2] = (3*slope[i] - 2*derivative[i] - derivative[i+1]) / step[i];
			numeratortorPolynomCoeff[3] = (derivative[i] + derivative[i+1] - 2*slope[i]) / (step[i] * step[i]);

			interpolatingRationalFunctions[i] = new RationalFunction(numeratortorPolynomCoeff);
		}

	}


	public static void main(final String[] args) {

		/*
		 * Example. Shows how to use this class.
		 */
		final int samplePoints = 200;

		/*
		 * Given input points
		 */
		final double[] givenPoints		= { 0.0, 1.0, 2.0, 3.0, 4.0, 5.0 };
		final double[] givenValues		= { 5.0, 6.0, 4.0, 7.0, 5.0, 6.0 };

		System.out.println("Interplation of given input points (x,y):");
		System.out.println("  x: " + Arrays.toString(givenPoints));
		System.out.println("  y: " + Arrays.toString(givenValues));
		System.out.println();


		// Create interpolated curve
		System.out.println("Default:");
		RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(givenPoints, givenValues);

		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			final double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			final double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println();

		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.AKIMA, ExtrapolationMethod.CONSTANT);
		System.out.println("AKIMA:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			final double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			final double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println();

		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.CUBIC_SPLINE, ExtrapolationMethod.CONSTANT);
		System.out.println("CUBIC_SPLINE:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			final double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			final double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println();

		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.PIECEWISE_CONSTANT, ExtrapolationMethod.CONSTANT);
		System.out.println("PIECEWISE_CONSTANT:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			final double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			final double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println();

		// Create interpolated curve
		interpolation = new RationalFunctionInterpolation(givenPoints, givenValues, InterpolationMethod.HARMONIC_SPLINE, ExtrapolationMethod.CONSTANT);
		System.out.println("HARMONIC_SPLINE:");
		for(int samplePointIndex = 0; samplePointIndex<samplePoints; samplePointIndex++) {
			final double point = givenPoints[0] + (double)samplePointIndex / (double)(samplePoints-1) * givenPoints[givenPoints.length-1]-givenPoints[0];
			final double value = interpolation.getValue(point);
			System.out.println("" + point + "\t" + value);
		}
		System.out.println();
	}

	@Override
	public double applyAsDouble(final double operand) {
		return getValue(operand);
	}

	private void readObject(final java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		interpolatingRationalFunctionsLazyInitLock = new Object();
	}
}
