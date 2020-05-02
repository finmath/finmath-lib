package net.finmath.integration;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import org.apache.commons.lang3.Validate;

/**
 * A piecewise constant {@link java.util.function.DoubleUnaryOperator} \( f : \mathbb{R} \rightarrow \mathbb{R} \)
 * with exact calculation of the integral \( \int_{a}^{b} f(x) dx \) for given bounds \( a, b \).
 * 
 * @author Christian Fries
 */
public class PiecewiseContantDoubleUnaryOperator implements DoubleUnaryOperator {

	private final double[] intervalRightPoints;
	private final double[] values;

	/**
	 * Construct a piecewise constant {@link java.util.function.DoubleUnaryOperator}
	 * \( f : \mathbb{R} \rightarrow \mathbb{R} \).
	 * The summation used Kahan error correction.
	 * 
	 * @param intervalRightPoints Array of length \( n \) with the right hand points \( x_{i} \) of the intervals \( (x_{i-1},x_{i}] \) on which we have values.
	 * @param values Array of length \( n+1 \) with the values \( f_{i} \) on the intervals \( (x_{i-1},x_{i}] \) where:
	 * <ul>
	 * 	<li> the first value \( f_{0} \) in this array corresponds to the value on \( (-\infty,x_{0}] \) </li>
	 *	<li> the last value \( f_{n} \) in this array corresponds to the value on \( (x_{n-1},\infty) \) </li>
	 * </ul>
	 */
	public PiecewiseContantDoubleUnaryOperator(double[] intervalRightPoints, double[] values) {
		super();
		Validate.notNull(intervalRightPoints, "Argument intervalRightPoints must not be null.");
		Validate.notNull(values, "Argument values must not be null.");
		Validate.isTrue(values.length == intervalRightPoints.length+1, "Length of values must equal length of intervalRightPoints + 1.");

		this.intervalRightPoints = intervalRightPoints;
		this.values = values;
	}

	public double getIntegral(double lowerBound, double upperBound) {
		if(lowerBound == upperBound) {
			return 0.0;
		}

		if(lowerBound > upperBound) {
			return -getIntegral(upperBound, lowerBound);
		}

		int indexUpperOfLowerBound = Arrays.binarySearch(intervalRightPoints, lowerBound);
		if(indexUpperOfLowerBound < 0) indexUpperOfLowerBound = -indexUpperOfLowerBound-1;

		int indexLowerOfUpperBound = Arrays.binarySearch(intervalRightPoints, upperBound);
		if(indexLowerOfUpperBound < 0) indexLowerOfUpperBound = -indexLowerOfUpperBound-1;
		indexLowerOfUpperBound--;

		if(indexLowerOfUpperBound < indexUpperOfLowerBound) {
			// lower and upper bound fall in the same intervall
			return values[indexUpperOfLowerBound] * (upperBound-lowerBound);
		}
		else {
			// running error of error correction
			double error = 0.0;

			// right part of interval where lower bound is
			double integral = values[indexUpperOfLowerBound] * (intervalRightPoints[indexUpperOfLowerBound]-lowerBound);

			// in between intervals (if any)
			for(int i=indexUpperOfLowerBound; i<indexLowerOfUpperBound; i++) {
				double value = values[i+1] * (intervalRightPoints[i+1]-intervalRightPoints[i]) - error;
				double newIntegral = integral + value;
				error = newIntegral - integral - value;
				integral = newIntegral;
			}

			// left part of interval where uper bound is
			integral += values[indexLowerOfUpperBound+1] * (upperBound-intervalRightPoints[indexLowerOfUpperBound]) - error;

			return integral;
		}
	}

	@Override
	public double applyAsDouble(double operand) {
		int index = Arrays.binarySearch(intervalRightPoints, operand);
		if (index < 0) index = -index - 1;
		return values[index];
	}
}
