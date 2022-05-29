package net.finmath.integration;

import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.Validate;

/**
 * A piecewise constant {@link java.util.function.DoubleUnaryOperator} \( f : \mathbb{R} \rightarrow \mathbb{R} \)
 * with exact calculation of the integral \( \int_{a}^{b} f(x) dx \) for given bounds \( a, b \).
 *
 * The summation uses Kahan error correction.
 *
 * For convenience the class can act as {@link java.util.function.DoubleUnaryOperator} specialization
 * and as {@link java.util.function.Function}.
 *
 * @author Christian Fries
 */
public class PiecewiseContantDoubleUnaryOperator implements DoubleUnaryOperator, Function<Double, Double> {

	private final double[] intervalRightPoints;
	private final double[] values;

	/**
	 * Construct a piecewise constant {@link java.util.function.DoubleUnaryOperator}
	 * \( f : \mathbb{R} \rightarrow \mathbb{R} \).
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

	/**
	 * Construct a piecewise constant {@link java.util.function.DoubleUnaryOperator}
	 * \( f : \mathbb{R} \rightarrow \mathbb{R} \).
	 *
	 * @param intervalRightPoints List of length \( n \) with the right hand points \( x_{i} \) of the intervals \( (x_{i-1},x_{i}] \) on which we have values.
	 * @param values List of length \( n+1 \) with the values \( f_{i} \) on the intervals \( (x_{i-1},x_{i}] \) where:
	 * <ul>
	 * 	<li> the first value \( f_{0} \) in this array corresponds to the value on \( (-\infty,x_{0}] \) </li>
	 *	<li> the last value \( f_{n} \) in this array corresponds to the value on \( (x_{n-1},\infty) \) </li>
	 * </ul>
	 */
	public PiecewiseContantDoubleUnaryOperator(List<Double> intervalRightPoints, List<Double> values) {
		this(
				ArrayUtils.toPrimitive(intervalRightPoints.toArray(new Double[intervalRightPoints.size()])),
				ArrayUtils.toPrimitive(values.toArray(new Double[values.size()]))
				);
	}

	/**
	 * Get the integral \( \int_{a}^{b} g(f(x)) dx \) of this function \( f \) plugged into a given function \( g \)
	 * for given bounds \( a, b \).
	 *
	 * @param lowerBound The lower bound a.
	 * @param upperBound The upper bound b.
	 * @param operator The given function g.
	 * @return The integral \( \int_{a}^{b} g(f(x)) dx \).
	 */
	public double getIntegral(double lowerBound, double upperBound, DoubleUnaryOperator operator) {
		if(lowerBound == upperBound) {
			return 0.0;
		}

		if(lowerBound > upperBound) {
			return -getIntegral(upperBound, lowerBound);
		}

		int indexUpperOfLowerBound = Arrays.binarySearch(intervalRightPoints, lowerBound);
		if(indexUpperOfLowerBound < 0) {
			indexUpperOfLowerBound = -indexUpperOfLowerBound-1;
		}

		int indexLowerOfUpperBound = Arrays.binarySearch(intervalRightPoints, upperBound);
		if(indexLowerOfUpperBound < 0) {
			indexLowerOfUpperBound = -indexLowerOfUpperBound-1;
		}
		indexLowerOfUpperBound--;

		if(indexLowerOfUpperBound < indexUpperOfLowerBound) {
			// lower and upper bound fall in the same interval
			return operator.applyAsDouble(values[indexUpperOfLowerBound]) * (upperBound-lowerBound);
		}
		else {
			// running error of error correction
			double error = 0.0;

			// right part of interval where lower bound is
			double integral = operator.applyAsDouble(values[indexUpperOfLowerBound]) * (intervalRightPoints[indexUpperOfLowerBound]-lowerBound);

			// in between intervals (if any)
			for(int i=indexUpperOfLowerBound; i<indexLowerOfUpperBound; i++) {
				final double value = operator.applyAsDouble(values[i+1]) * (intervalRightPoints[i+1]-intervalRightPoints[i]) - error;
				final double newIntegral = integral + value;
				error = newIntegral - integral - value;
				integral = newIntegral;
			}

			// left part of interval where uper bound is
			integral += operator.applyAsDouble(values[indexLowerOfUpperBound+1]) * (upperBound-intervalRightPoints[indexLowerOfUpperBound]) - error;

			return integral;
		}
	}

	/**
	 * Get the integral \( \int_{a}^{b} g(f(x)) dx \) of this function \( f \) plugged into a given function \( g \)
	 * for given bounds \( a, b \).
	 *
	 * @param lowerBound The lower bound a.
	 * @param upperBound The upper bound b.
	 * @param operator The given function g.
	 * @return The integral \( \int_{a}^{b} g(f(x)) dx \).
	 */
	public double getIntegral(double lowerBound, double upperBound, Function<Double, Double> operator) {
		return getIntegral(lowerBound, upperBound, toPrimitive(operator));
	}

	private DoubleUnaryOperator toPrimitive(Function<Double, Double> operator) {
		final DoubleUnaryOperator doubleUnaryOperator = x -> operator.apply(x);
		return doubleUnaryOperator;
	}

	/**
	 * Get the integral \( \int_{a}^{b} f(x) dx \) of this function \( f \)
	 * for given bounds \( a, b \).
	 *
	 * @param lowerBound The lower bound a.
	 * @param upperBound The upper bound b.
	 * @return The integral \( \int_{a}^{b} f(x) dx \).
	 */
	public double getIntegral(double lowerBound, double upperBound) {
		return getIntegral(lowerBound, upperBound, DoubleUnaryOperator.identity());
	}

	/**
	 * Get the value of this unary operator \( f \) at the given argument.
	 *
	 * @param operand The given argument.
	 * @return The value \( f(x) \).
	 */
	@Override
	public double applyAsDouble(double operand) {
		int index = Arrays.binarySearch(intervalRightPoints, operand);
		if (index < 0) {
			index = -index - 1;
		}
		return values[index];
	}

	/**
	 * Get the value of this function \( f \) at the given argument.
	 *
	 * @param value The given argument.
	 * @return The value \( f(x) \).
	 */
	@Override
	public Double apply(Double value) {
		return applyAsDouble(value);
	}
}
