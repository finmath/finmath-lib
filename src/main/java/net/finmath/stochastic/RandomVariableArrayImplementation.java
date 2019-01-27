/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2009
 */
package net.finmath.stochastic;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;

/**
 * An implementation of <code>RandomVariableArray</code> implementing an array of <code>RandomVariable</code> objects,
 * implementing the <code>RandomVariable</code> interface.
 *
 * @author Christian Fries
 */
public class RandomVariableArrayImplementation implements RandomVariableArray {

	private static final long serialVersionUID = -5718980901166760522L;

	private final RandomVariable[] elements;

	public static RandomVariableArray of(RandomVariable[] elements) {
		return new RandomVariableArrayImplementation(elements.clone());
	}

	private RandomVariableArrayImplementation(RandomVariable[] elements) {
		super();

		if(elements.length == 0) throw new IllegalArgumentException("Empty array.");

		// Ensure that all elements
		int level = getLevel(elements[0]);
		for(int i=1; i<elements.length; i++) {
			if(getLevel(elements[i]) != level) {
				throw new IllegalArgumentException("Elements must be of same array type.");
			}
		}

		this.elements = elements;
	}

	private int getLevel(RandomVariable randomVariable) {
		if(randomVariable instanceof RandomVariableArray) {
			return ((RandomVariableArray) randomVariable).getLevel();
		}
		else {
			return 0;
		}
	}

	@Override
	public int getNumberOfElements() {
		return elements.length;
	}

	@Override
	public RandomVariable getElement(int index) {
		return elements[index];
	}

	@Override
	public RandomVariableArray map(Function<RandomVariable, RandomVariable> operator) {
		RandomVariable[] newElments = new RandomVariable[getNumberOfElements()];
		for(int i=1; i<elements.length; i++) {
			newElments[i] = operator.apply(elements[i]);
		}
		return new RandomVariableArrayImplementation(newElments);
	}

	@Override
	public RandomVariable sumProduct(RandomVariableArray array) {
		RandomVariable result = elements[0].mult(array.getElement(0));
		for(int i=1; i<elements.length; i++) {
			result = result.add(elements[i].mult(array.getElement(i)));
		}
		return result;
	}

	/*
	 * net.finmath.stochastic.RandomVariable
	 */

	@Override
	public boolean equals(RandomVariable randomVariable) {
		boolean equal = (randomVariable instanceof RandomVariableArray);
		for(int i=0; i<getNumberOfElements() && equal; i++) {
			equal &= getElement(i).equals(((RandomVariableArray)randomVariable).getElement(i));
		}
		return equal;
	}

	@Override
	public double getFiltrationTime() {
		double time = Double.NEGATIVE_INFINITY;
		for(int i=0; i<getNumberOfElements(); i++) {
			time = Math.max(getElement(i).getFiltrationTime(), time);
		}
		return time;
	}

	@Override
	public int getTypePriority() {
		return 100;
	}


	@Override
	public double get(int pathOrState) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		int size = 1;
		for(int i=0; i<getNumberOfElements(); i++) {
			size = Math.max(size, getElement(i).size());
		}
		return size;
	}

	@Override
	public boolean isDeterministic() {
		boolean deterministic = true;
		for(int i=0; i<getNumberOfElements() && deterministic; i++) {
			deterministic &= getElement(i).isDeterministic();
		}
		return deterministic;
	}

	@Override
	public double[] getRealizations() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Double doubleValue() {
		throw new UnsupportedOperationException();
	}

	@Override
	public IntToDoubleFunction getOperator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public DoubleStream getRealizationsStream() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getMin() {
		double min = Double.POSITIVE_INFINITY;
		for(int i=0; i<getNumberOfElements(); i++) {
			min = Math.min(min, getElement(i).getMin());
		}
		return min;
	}

	@Override
	public double getMax() {
		double max = Double.NEGATIVE_INFINITY;
		for(int i=0; i<getNumberOfElements(); i++) {
			max = Math.max(max, getElement(i).getMax());
		}
		return max;
	}

	@Override
	public double getAverage() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getAverage(RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getVariance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getVariance(RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getSampleVariance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardDeviation() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardDeviation(RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardError() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardError(RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantile(double quantile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantile(double quantile, RandomVariable probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		throw new UnsupportedOperationException();
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
		return map(x -> x.apply(operator));
	}

	@Override
	public RandomVariable apply(DoubleBinaryOperator operator, RandomVariable argument) {
		return map(x -> x.apply(operator, argument));
	}

	@Override
	public RandomVariable apply(DoubleTernaryOperator operator, RandomVariable argument1, RandomVariable argument2) {
		return map(x -> x.apply(operator, argument1, argument2));
	}

	@Override
	public RandomVariable cap(double cap) {
		return map(x -> x.cap(cap));
	}

	@Override
	public RandomVariable floor(double floor) {
		return map(x -> x.floor(floor));
	}

	@Override
	public RandomVariable add(double value) {
		return map(x -> x.add(value));
	}

	@Override
	public RandomVariable sub(double value) {
		return map(x -> x.sub(value));
	}

	@Override
	public RandomVariable mult(double value) {
		return map(x -> x.mult(value));
	}

	@Override
	public RandomVariable div(double value) {
		return map(x -> x.div(value));
	}

	@Override
	public RandomVariable pow(double exponent) {
		return map(x -> x.pow(exponent));
	}

	@Override
	public RandomVariable average() {
		return map(x -> x.average());
	}

	@Override
	public RandomVariable squared() {
		return map(x -> x.squared());
	}

	@Override
	public RandomVariable sqrt() {
		return map(x -> x.sqrt());
	}

	@Override
	public RandomVariable exp() {
		return map(x -> x.exp());
	}

	@Override
	public RandomVariable log() {
		return map(x -> x.log());
	}

	@Override
	public RandomVariable sin() {
		return map(x -> x.sin());
	}

	@Override
	public RandomVariable cos() {
		return map(x -> x.cos());
	}

	@Override
	public RandomVariable add(RandomVariable randomVariable) {
		return map(x -> x.add(randomVariable));
	}

	@Override
	public RandomVariable sub(RandomVariable randomVariable) {
		return map(x -> x.sub(randomVariable));
	}

	@Override
	public RandomVariable bus(RandomVariable randomVariable) {
		return map(x -> x.bus(randomVariable));
	}

	@Override
	public RandomVariable mult(RandomVariable randomVariable) {
		return map(x -> x.mult(randomVariable));
	}

	@Override
	public RandomVariable div(RandomVariable randomVariable) {
		return map(x -> x.div(randomVariable));
	}

	@Override
	public RandomVariable vid(RandomVariable randomVariable) {
		return map(x -> x.vid(randomVariable));
	}

	@Override
	public RandomVariable cap(RandomVariable cap) {
		return map(x -> x.cap(cap));
	}

	@Override
	public RandomVariable floor(RandomVariable floor) {
		return map(x -> x.cap(floor));
	}

	@Override
	public RandomVariable accrue(RandomVariable rate, double periodLength) {
		return map(x -> x.accrue(rate, periodLength));
	}

	@Override
	public RandomVariable discount(RandomVariable rate, double periodLength) {
		return map(x -> x.discount(rate, periodLength));
	}

	@Override
	public RandomVariable choose(RandomVariable valueIfTriggerNonNegative, RandomVariable valueIfTriggerNegative) {
		return map(x -> x.choose(valueIfTriggerNonNegative, valueIfTriggerNegative));
	}

	@Override
	public RandomVariable invert() {
		return map(x -> x.invert());
	}

	@Override
	public RandomVariable abs() {
		return map(x -> x.abs());
	}

	@Override
	public RandomVariable addProduct(RandomVariable factor1, double factor2) {
		return map(x -> x.addProduct(factor1, factor2));
	}

	@Override
	public RandomVariable addProduct(RandomVariable factor1, RandomVariable factor2) {
		return map(x -> x.addProduct(factor1, factor2));
	}

	@Override
	public RandomVariable addRatio(RandomVariable numerator, RandomVariable denominator) {
		return map(x -> x.addRatio(numerator, denominator));
	}

	@Override
	public RandomVariable subRatio(RandomVariable numerator, RandomVariable denominator) {
		return map(x -> x.subRatio(numerator, denominator));
	}

	@Override
	public RandomVariable isNaN() {
		return map(x -> x.isNaN());
	}
}
