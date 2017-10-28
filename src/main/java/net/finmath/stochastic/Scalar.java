/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
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
 * A scalar value implementing the RandomVariableInterface.
 * 
 * @author Christian Fries
 */
public class Scalar implements RandomVariableInterface {

	private static final long serialVersionUID = -729437972787062992L;

	final double value;

	public Scalar(double value) {
		super();
		this.value = value;
	}

	@Override
	public boolean equals(RandomVariableInterface randomVariable) {
		return randomVariable.isDeterministic() && value == randomVariable.get(0);
	}

	@Override
	public double getFiltrationTime() {
		return Double.NEGATIVE_INFINITY;
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
	public double getAverage(RandomVariableInterface probabilities) {
		return value * probabilities.getAverage();
	}

	@Override
	public double getVariance() {
		return 0;
	}

	@Override
	public double getVariance(RandomVariableInterface probabilities) {
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
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		return 0;
	}

	@Override
	public double getStandardError() {
		return 0;
	}

	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		return 0;
	}

	@Override
	public double getQuantile(double quantile) {
		return 0;
	}

	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		return 0;
	}

	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return 0;
	}

	@Override
	public double[] getHistogram(double[] intervalPoints) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariableInterface cache() {
		return this;
	}

	@Override
	public RandomVariableInterface apply(DoubleUnaryOperator operator) {
		return new Scalar(operator.applyAsDouble(value));
	}

	@Override
	public RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1,
			RandomVariableInterface argument2) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariableInterface floor(double floor) {
		return new Scalar(Math.max(value, floor));
	}

	@Override
	public RandomVariableInterface add(double value) {
		return new Scalar(this.value + value);
	}

	@Override
	public RandomVariableInterface sub(double value) {
		return new Scalar(this.value - value);
	}

	@Override
	public RandomVariableInterface mult(double value) {
		return new Scalar(this.value * value);
	}

	@Override
	public RandomVariableInterface div(double value) {
		return new Scalar(this.value / value);
	}

	@Override
	public RandomVariableInterface pow(double exponent) {
		return new Scalar(Math.pow(value, exponent));
	}

	@Override
	public RandomVariableInterface average() {
		return this;
	}

	@Override
	public RandomVariableInterface squared() {
		return new Scalar(value * value);
	}

	@Override
	public RandomVariableInterface sqrt() {
		return new Scalar(Math.sqrt(value));
	}

	@Override
	public RandomVariableInterface exp() {
		return new Scalar(Math.exp(value));
	}

	@Override
	public RandomVariableInterface log() {
		return new Scalar(Math.log(value));
	}

	@Override
	public RandomVariableInterface sin() {
		return new Scalar(Math.sin(value));
	}

	@Override
	public RandomVariableInterface cos() {
		return new Scalar(Math.cos(value));
	}

	@Override
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {
		return randomVariable.add(value);
	}

	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		return randomVariable.sub(value).mult(-1.0);
	}

	@Override
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		if(value == 0) return new Scalar(0.0);
		return randomVariable.mult(value);
	}

	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		if(value == 0) return new Scalar(0.0);
		return randomVariable.mult(value);
	}

	@Override
	public RandomVariableInterface cap(RandomVariableInterface cap) {
		if(cap.isDeterministic()) return new Scalar(Math.min(value, cap.get(0)));
		else return cap.cap(value);
	}

	@Override
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		if(floor.isDeterministic()) return new Scalar(Math.max(value, floor.get(0)));
		else return floor.floor(value);
	}

	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		if(rate.isDeterministic()) return new Scalar(value * (1 + rate.get(0) * periodLength));
		else return rate.mult(periodLength*value).add(value);
	}

	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		if(value == 0) return new Scalar(0.0);
		else if(rate.isDeterministic()) return new Scalar(value / (1 + rate.get(0) * periodLength));
		else return rate.mult(periodLength/value).add(1.0/value).invert();
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		// TODO Auto-generated method stub
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, double)
	 */
	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger,
			RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariableInterface invert() {
		return new Scalar(1.0/value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#abs()
	 */
	@Override
	public RandomVariableInterface abs() {
		return new Scalar(Math.abs(value));
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		if(factor1.isDeterministic()) return new Scalar(value + factor1.get(0) * factor2);
		else return factor1.mult(factor2).add(value);
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		if(factor1.isDeterministic() && factor2.isDeterministic()) return new Scalar(value + factor1.get(0) * factor2.get(0));
		else return factor1.mult(factor2).add(value);
	}

	@Override
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		if(numerator.isDeterministic() && denominator.isDeterministic()) return new Scalar(value + numerator.get(0) * denominator.get(0));
		else return numerator.div(denominator).add(value);
	}

	@Override
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		if(numerator.isDeterministic() && denominator.isDeterministic()) return new Scalar(value - numerator.get(0) * denominator.get(0));
		else return numerator.div(denominator).sub(value).mult(-1.0);
	}

	@Override
	public RandomVariableInterface isNaN() {
		return new Scalar(Double.isNaN(value) ? 1.0 : 0.0);
	}
}
