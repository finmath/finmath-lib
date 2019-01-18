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
 * @version 1.0
 */
public class Scalar implements RandomVariable {

	private static final long serialVersionUID = -729437972787062992L;

	final double value;

	public Scalar(double value) {
		super();
		this.value = value;
	}

	@Override
	public boolean equals(RandomVariable randomVariable) {
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
	public double get(int pathOrState) {
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
	public double getAverage(RandomVariable probabilities) {
		return value * probabilities.getAverage();
	}

	@Override
	public double getVariance() {
		return 0;
	}

	@Override
	public double getVariance(RandomVariable probabilities) {
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
	public double getStandardDeviation(RandomVariable probabilities) {
		return 0;
	}

	@Override
	public double getStandardError() {
		return 0;
	}

	@Override
	public double getStandardError(RandomVariable probabilities) {
		return 0;
	}

	@Override
	public double getQuantile(double quantile) {
		return value;
	}

	@Override
	public double getQuantile(double quantile, RandomVariable probabilities) {
		return value;
	}

	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return value;
	}

	@Override
	public double[] getHistogram(double[] intervalPoints) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariable cache() {
		return this;
	}

	@Override
	public RandomVariable apply(DoubleUnaryOperator operator) {
		return new Scalar(operator.applyAsDouble(value));
	}

	@Override
	public RandomVariable apply(DoubleBinaryOperator operator, RandomVariable argument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariable apply(DoubleTernaryOperator operator, RandomVariable argument1,
			RandomVariable argument2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariable cap(double cap) {
		return new Scalar(Math.min(value, cap));
	}

	@Override
	public RandomVariable floor(double floor) {
		return new Scalar(Math.max(value, floor));
	}

	@Override
	public RandomVariable add(double value) {
		return new Scalar(this.value + value);
	}

	@Override
	public RandomVariable sub(double value) {
		return new Scalar(this.value - value);
	}

	@Override
	public RandomVariable mult(double value) {
		return new Scalar(this.value * value);
	}

	@Override
	public RandomVariable div(double value) {
		return new Scalar(this.value / value);
	}

	@Override
	public RandomVariable pow(double exponent) {
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
	public RandomVariable add(RandomVariable randomVariable) {
		return randomVariable.add(value);
	}

	@Override
	public RandomVariable sub(RandomVariable randomVariable) {
		return randomVariable.sub(value).mult(-1.0);
	}

	@Override
	public RandomVariable bus(RandomVariable randomVariable) {
		return randomVariable.sub(value);
	}

	@Override
	public RandomVariable mult(RandomVariable randomVariable) {
		if(value == 0) {
			return new Scalar(0.0);
		}
		return randomVariable.mult(value);
	}

	@Override
	public RandomVariable div(RandomVariable randomVariable) {
		if(value == 0) {
			return new Scalar(0.0);
		}
		return randomVariable.invert().mult(value);
	}

	@Override
	public RandomVariable vid(RandomVariable randomVariable) {
		if(value == 0) {
			return new Scalar(Double.NaN);
		}
		return randomVariable.div(value);
	}

	@Override
	public RandomVariable cap(RandomVariable cap) {
		if(cap.isDeterministic()) {
			return new Scalar(Math.min(value, cap.get(0)));
		} else {
			return cap.cap(value);
		}
	}

	@Override
	public RandomVariable floor(RandomVariable floor) {
		if(floor.isDeterministic()) {
			return new Scalar(Math.max(value, floor.get(0)));
		} else {
			return floor.floor(value);
		}
	}

	@Override
	public RandomVariable accrue(RandomVariable rate, double periodLength) {
		if(rate.isDeterministic()) {
			return new Scalar(value * (1 + rate.get(0) * periodLength));
		} else {
			return rate.mult(periodLength*value).add(value);
		}
	}

	@Override
	public RandomVariable discount(RandomVariable rate, double periodLength) {
		if(value == 0) {
			return new Scalar(0.0);
		} else if(rate.isDeterministic()) {
			return new Scalar(value / (1 + rate.get(0) * periodLength));
		} else {
			return rate.mult(periodLength/value).add(1.0/value).invert();
		}
	}

	@Override
	public RandomVariable choose(RandomVariable valueIfTriggerNonNegative, RandomVariable valueIfTriggerNegative) {
		if(value >= 0) return valueIfTriggerNonNegative;
		else return valueIfTriggerNegative;
	}

	@Override
	public RandomVariable invert() {
		return new Scalar(1.0/value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariable#abs()
	 */
	@Override
	public RandomVariable abs() {
		return new Scalar(Math.abs(value));
	}

	@Override
	public RandomVariable addProduct(RandomVariable factor1, double factor2) {
		if(factor1.isDeterministic()) {
			return new Scalar(value + factor1.get(0) * factor2);
		} else {
			return factor1.mult(factor2).add(value);
		}
	}

	@Override
	public RandomVariable addProduct(RandomVariable factor1, RandomVariable factor2) {
		if(factor1.isDeterministic() && factor2.isDeterministic()) {
			return new Scalar(value + factor1.get(0) * factor2.get(0));
		} else {
			return factor1.mult(factor2).add(value);
		}
	}

	@Override
	public RandomVariable addRatio(RandomVariable numerator, RandomVariable denominator) {
		if(numerator.isDeterministic() && denominator.isDeterministic()) {
			return new Scalar(value + numerator.get(0) * denominator.get(0));
		} else {
			return numerator.div(denominator).add(value);
		}
	}

	@Override
	public RandomVariable subRatio(RandomVariable numerator, RandomVariable denominator) {
		if(numerator.isDeterministic() && denominator.isDeterministic()) {
			return new Scalar(value - numerator.get(0) * denominator.get(0));
		} else {
			return numerator.div(denominator).sub(value).mult(-1.0);
		}
	}

	@Override
	public RandomVariable isNaN() {
		return new Scalar(Double.isNaN(value) ? 1.0 : 0.0);
	}
}
