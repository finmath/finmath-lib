/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.08.2017
 */
package net.finmath.stochastic;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;

/**
 * A scalar value implementing the RandomVariable.
 *
 * @author Christian Fries
 * @version 1.0.1
 */
public class Scalar implements RandomVariable {

	private static final long serialVersionUID = -729437972787062992L;

	private final double value;

	public static Scalar of(final double value) {
		return new Scalar(value);
	}

	public Scalar(final double value) {
		super();
		this.value = value;
		//		if(Double.isNaN(value)) {
		//			throw new ArithmeticException("Not a Numbber");
		//		}
	}

	public static Scalar[] arrayOf(final double[] arrayOfDoubles) {
		final Scalar[] array = new Scalar[arrayOfDoubles.length];
		for(int i=0; i<arrayOfDoubles.length; i++) {
			array[i] = new Scalar(arrayOfDoubles[i]);
		}
		return array;
	}

	@Override
	public boolean equals(final RandomVariable randomVariable) {
		return randomVariable.isDeterministic() && value == randomVariable.get(0);
	}

	@Override
	public double getFiltrationTime() {
		return Double.NEGATIVE_INFINITY;
	}

	@Override
	public int getTypePriority() {
		return 0;
	}

	@Override
	public double get(final int pathOrState) {
		return value;
	}

	@Override
	public int size() {
		return 1;
	}

	@Override
	public boolean isDeterministic() {
		return true;
	}

	@Override
	public double[] getRealizations() {
		return null;
	}

	@Override
	public Double doubleValue() {
		return value;
	}

	@Override
	public IntToDoubleFunction getOperator() {
		return null;
	}

	@Override
	public DoubleStream getRealizationsStream() {
		return null;
	}

	@Override
	public double getMin() {
		return value;
	}

	@Override
	public double getMax() {
		return value;
	}

	@Override
	public double getAverage() {
		return value;
	}

	@Override
	public double getAverage(final RandomVariable probabilities) {
		return value * probabilities.getAverage();
	}

	@Override
	public double getVariance() {
		return 0;
	}

	@Override
	public double getVariance(final RandomVariable probabilities) {
		return 0;
	}

	@Override
	public double getSampleVariance() {
		return 0;
	}

	@Override
	public double getStandardDeviation() {
		return 0;
	}

	@Override
	public double getStandardDeviation(final RandomVariable probabilities) {
		return 0;
	}

	@Override
	public double getStandardError() {
		return 0;
	}

	@Override
	public double getStandardError(final RandomVariable probabilities) {
		return 0;
	}

	@Override
	public double getQuantile(final double quantile) {
		return value;
	}

	@Override
	public double getQuantile(final double quantile, final RandomVariable probabilities) {
		return value;
	}

	@Override
	public double getQuantileExpectation(final double quantileStart, final double quantileEnd) {
		return value;
	}

	@Override
	public double[] getHistogram(final double[] intervalPoints) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[][] getHistogram(final int numberOfPoints, final double standardDeviations) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable cache() {
		return this;
	}

	@Override
	public RandomVariable apply(final DoubleUnaryOperator operator) {
		return new Scalar(operator.applyAsDouble(value));
	}

	@Override
	public RandomVariable apply(final DoubleBinaryOperator operator, final RandomVariable argument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariable apply(final DoubleTernaryOperator operator, final RandomVariable argument1,
			final RandomVariable argument2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariable cap(final double cap) {
		return new Scalar(Math.min(value, cap));
	}

	@Override
	public RandomVariable floor(final double floor) {
		return new Scalar(Math.max(value, floor));
	}

	@Override
	public RandomVariable add(final double value) {
		return new Scalar(this.value + value);
	}

	@Override
	public RandomVariable sub(final double value) {
		return new Scalar(this.value - value);
	}

	@Override
	public RandomVariable mult(final double value) {
		return new Scalar(this.value * value);
	}

	@Override
	public RandomVariable div(final double value) {
		return new Scalar(this.value / value);
	}

	@Override
	public RandomVariable pow(final double exponent) {
		return new Scalar(Math.pow(value, exponent));
	}

	@Override
	public RandomVariable average() {
		return this;
	}

	@Override
	public RandomVariable squared() {
		return new Scalar(value * value);
	}

	@Override
	public RandomVariable sqrt() {
		return new Scalar(Math.sqrt(value));
	}

	@Override
	public RandomVariable exp() {
		return new Scalar(Math.exp(value));
	}

	@Override
	public RandomVariable expm1() {
		return new Scalar(Math.expm1(value));
	}

	@Override
	public RandomVariable log() {
		return new Scalar(Math.log(value));
	}

	@Override
	public RandomVariable sin() {
		return new Scalar(Math.sin(value));
	}

	@Override
	public RandomVariable cos() {
		return new Scalar(Math.cos(value));
	}

	@Override
	public RandomVariable add(final RandomVariable randomVariable) {
		return randomVariable.add(value);
	}

	@Override
	public RandomVariable sub(final RandomVariable randomVariable) {
		return randomVariable.sub(value).mult(-1.0);
	}

	@Override
	public RandomVariable bus(final RandomVariable randomVariable) {
		return randomVariable.sub(value);
	}

	@Override
	public RandomVariable mult(final RandomVariable randomVariable) {
		return randomVariable.mult(value);
	}

	@Override
	public RandomVariable div(final RandomVariable randomVariable) {
		return randomVariable.invert().mult(value);
	}

	@Override
	public RandomVariable vid(final RandomVariable randomVariable) {
		return randomVariable.div(value);
	}

	@Override
	public RandomVariable cap(final RandomVariable cap) {
		return cap.cap(value);
	}

	@Override
	public RandomVariable floor(final RandomVariable floor) {
		return floor.floor(value);
	}

	@Override
	public RandomVariable accrue(final RandomVariable rate, final double periodLength) {
		return rate.mult(periodLength*value).add(value);
	}

	@Override
	public RandomVariable discount(final RandomVariable rate, final double periodLength) {
		if(value == 0) {
			return rate.mult(0.0);
		}
		else {
			return rate.mult(periodLength/value).add(1.0/value).invert();
		}
	}

	@Override
	public RandomVariable choose(final RandomVariable valueIfTriggerNonNegative, final RandomVariable valueIfTriggerNegative) {
		if(value >= 0) {
			return valueIfTriggerNonNegative;
		} else {
			return valueIfTriggerNegative;
		}
	}

	@Override
	public RandomVariable invert() {
		return new Scalar(1.0/value);
	}

	@Override
	public RandomVariable abs() {
		return new Scalar(Math.abs(value));
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final double factor2) {
		return factor1.mult(factor2).add(value);
	}

	@Override
	public RandomVariable addProduct(final RandomVariable factor1, final RandomVariable factor2) {
		return factor1.mult(factor2).add(value);
	}

	@Override
	public RandomVariable addRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return numerator.div(denominator).add(value);
	}

	@Override
	public RandomVariable subRatio(final RandomVariable numerator, final RandomVariable denominator) {
		return numerator.div(denominator).sub(value).mult(-1.0);
	}

	@Override
	public RandomVariable isNaN() {
		return new Scalar(Double.isNaN(value) ? 1.0 : 0.0);
	}

	@Override
	public String toString() {
		return "Scalar [value=" + value + ", filtrationTime=" + getFiltrationTime() + ", typePriority()="
				+ getTypePriority() + "]";
	}
}
