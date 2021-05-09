/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.05.2004
 */
package net.finmath.marketdata2.interpolation;

import java.io.IOException;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
//import net.finmath.interpolation.RationalFunctionInterpolation.RationalFunction;
import net.finmath.stochastic.RandomVariable;

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
public class RationalFunctionInterpolation  {

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
	private final double[]	points; // times (i.e. double[])
	private final RandomVariable[]	values;

	private InterpolationMethod	interpolationMethod = InterpolationMethod.LINEAR;
	private ExtrapolationMethod	extrapolationMethod = ExtrapolationMethod.DEFAULT;

	private static class RationalFunction {
		private final RandomVariable[] coefficientsNumerator;
		private final RandomVariable[] coefficientsDenominator;

		/**
		 * Create a rational interpolation function.
		 *
		 * @param coefficientsNumerator The coefficients of the polynomial of the numerator, in increasing order.
		 * @param coefficientsDenominator The coefficients of the polynomial of the denominator, in increasing order.
		 */
		RationalFunction(final RandomVariable[] coefficientsNumerator, final RandomVariable[]coefficientsDenominator) {
			super();
			this.coefficientsNumerator = coefficientsNumerator;
			this.coefficientsDenominator = coefficientsDenominator;
		}

		/**
		 * Create a polynomial interpolation function.
		 *
		 * @param coefficients The coefficients of the polynomial, in increasing order.
		 */
		RationalFunction(final RandomVariable[] coefficients) {
			super();
			coefficientsNumerator = coefficients;
			coefficientsDenominator = null;
		}

		/**
		 * Returns the value for a given arguments.
		 *
		 * @param x Given argument.
		 * @return Returns the value for the given argument.
		 */
		public RandomVariable getValue(final double x)
		{
			RandomVariable powerOfX	= new RandomVariableFromDoubleArray(1.0);

			RandomVariable valueNumerator = coefficientsNumerator[0];

			for (int i = 1; i<coefficientsNumerator.length;i++) {
				powerOfX = powerOfX.mult(x);
				valueNumerator = valueNumerator.addProduct(coefficientsNumerator[i],powerOfX);
			}

			if(coefficientsDenominator == null) {
				return valueNumerator;
			}
			RandomVariable valueDenominator = coefficientsDenominator[0];
			powerOfX	=  new RandomVariableFromDoubleArray(1.0);
			for (int i = 1; i<coefficientsDenominator.length;i++) {
				powerOfX = powerOfX.mult(x);
				valueDenominator = valueDenominator.addProduct(coefficientsDenominator[i],powerOfX);
			}

			return valueNumerator.div(valueDenominator);
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
	public RationalFunctionInterpolation(final double[] points, final RandomVariable[] values) {
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
	public RationalFunctionInterpolation(final double[] points, final RandomVariable[] values,  final InterpolationMethod interpolationMethod, final ExtrapolationMethod extrapolationMethod) {
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
	public RandomVariable getValue(final double x) // x is time
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
				return values[0].add((values[1].sub(values[0])).div(points[1]-points[0]).mult(x-points[0]));
			} else {
				intervalIndex = 0;
			}
		}
		else if(intervalIndex > points.length-2) {
			// Extrapolation
			if(extrapolationMethod == ExtrapolationMethod.CONSTANT) {
				return values[points.length-1];
			} else if(extrapolationMethod == ExtrapolationMethod.LINEAR) {
				return values[points.length-1].add((values[points.length-2].sub(values[points.length-1])).div(points[points.length-2]-points[points.length-1]).mult(x-points[points.length-1]));
			} else {
				intervalIndex = points.length-2;
			}
		}

		final RationalFunction rationalFunction = interpolatingRationalFunctions[intervalIndex];

		// Calculate interpolating value
		return rationalFunction.getValue(x-points[intervalIndex]);
	}

	private void readObject(final java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		interpolatingRationalFunctionsLazyInitLock = new Object();
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
			RandomVariable[] numeratorPolynomCoeff;
			if (interpolationMethod == InterpolationMethod.PIECEWISE_CONSTANT_RIGHTPOINT) {
				numeratorPolynomCoeff = new RandomVariable[] {values[pointIndex+1]};
			} else {
				numeratorPolynomCoeff = new RandomVariable[] {values[pointIndex]};
			}
			interpolatingRationalFunctions[pointIndex] = new RationalFunction(numeratorPolynomCoeff);
		}
	}

	private void doCreateRationalFunctionsForLinearInterpolation()
	{
		/*
		 * Generate a rational function for each given interval
		 */
		interpolatingRationalFunctions = new RationalFunction[points.length-1];

		// create numerator polynomials (linear)
		for(int pointIndex = 0; pointIndex < points.length-1; pointIndex++ ) {
			final RandomVariable[] numeratorPolynomCoeff		= new RandomVariable[2];

			final double xl = points[pointIndex];
			final double xr = points[pointIndex+1];

			final RandomVariable fl = values[pointIndex];
			final RandomVariable fr = values[pointIndex+1];

			numeratorPolynomCoeff[1] = fr.sub(fl).div(xr-xl);
			numeratorPolynomCoeff[0] = fl;

			interpolatingRationalFunctions[pointIndex] = new RationalFunction(numeratorPolynomCoeff);
		}
	}

	/*
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

		// in t_n
		derivative[numberOfPoints-1] =(2*step[numberOfPoints-2] + step[numberOfPoints-3])/doubleStep[numberOfPoints-3] * slope[numberOfPoints-2]
				- step[numberOfPoints-2]/doubleStep[numberOfPoints-3] * slope[numberOfPoints-3];

		//  monotonicity filtering
		if(interpolationMethod == InterpolationMethod.HARMONIC_SPLINE_WITH_MONOTONIC_FILTERING){
			// in t_0
			if((derivative[0]*slope[0] > 0) && (slope[0]*slope[1] <= 0) && (Math.abs(derivative[0]) < 3*Math.abs(slope[0])))
				derivative[0] = 3 * slope[0];
			if( derivative[0]*slope[0] <= 0 )
				derivative[0] = 0;

			// in t_n
			if((derivative[numberOfPoints-1]*slope[numberOfPoints-2] > 0) && (slope[numberOfPoints-2]*slope[numberOfPoints-3] <= 0)
					&& (Math.abs(derivative[numberOfPoints-1]) < 3*Math.abs(slope[numberOfPoints-2])))
				derivative[numberOfPoints-1] = 3 * slope[numberOfPoints-2];
			if( derivative[numberOfPoints-1]*slope[numberOfPoints-2] <= 0 )
				derivative[numberOfPoints-1] = 0;
		}

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


		interpolatingRationalFunctions = new RationalFunction[numberOfPoints-1];

		// create numerator polynomials (third order polynomial)
		for(int i = 0; i < numberOfPoints-1; i++ ) {
			double[] numeratortorPolynomCoeff		= new double[4];

			numeratortorPolynomCoeff[0] = values[i];
			numeratortorPolynomCoeff[1] = derivative[i];
			numeratortorPolynomCoeff[2] = (3*slope[i] - 2*derivative[i] - derivative[i+1]) / step[i];
			numeratortorPolynomCoeff[3] = (derivative[i] + derivative[i+1] - 2*slope[i]) / (step[i] * step[i]);

			interpolatingRationalFunctions[i] = new RationalFunction(numeratortorPolynomCoeff);
		}

	}
	 */




}
