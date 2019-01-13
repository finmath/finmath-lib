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
 * An implementation of <code>RandomVariableArray</code> implementing an array of <code>RandomVariableInterface</code> objects,
 * implementing the <code>RandomVariableInterface</code> interface.
 * 
 * @author Christian Fries
 */
public class RandomVariableArrayImplementation implements RandomVariableArray {

	private static final long serialVersionUID = -5718980901166760522L;

	private final RandomVariableInterface[] elements;

	private RandomVariableArrayImplementation(RandomVariableInterface[] elements) {
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

	private int getLevel(RandomVariableInterface randomVariable) {
		if(randomVariable instanceof RandomVariableArray) {
			return ((RandomVariableArray) randomVariable).getLevel();
		}
		else {
			return 0;
		}
	}

	@Override
	public RandomVariableArray of(RandomVariableInterface[] elements) {
		return new RandomVariableArrayImplementation(elements.clone());
	}

	@Override
	public int getNumberOfElements() {
		return elements.length;
	}
	
	@Override
	public RandomVariableInterface getElement(int index) {
		return elements[index];
	}

	@Override
	public RandomVariableArray map(Function<RandomVariableInterface, RandomVariableInterface> operator) {
		RandomVariableInterface[] newElments = new RandomVariableInterface[getNumberOfElements()];
		for(int i=1; i<elements.length; i++) {
			newElments[i] = operator.apply(elements[i]);
		}
		return new RandomVariableArrayImplementation(newElments);
	}

	@Override
	public RandomVariableInterface sumProduct(RandomVariableArray array) {
		RandomVariableInterface result = elements[0].mult(array.getElement(0));
		for(int i=1; i<elements.length; i++) {
			result = result.add(elements[i].mult(array.getElement(i)));
		}
		return result;
	}

	/*
	 * net.finmath.stochastic.RandomVariableInterface
	 */

	@Override
	public boolean equals(RandomVariableInterface randomVariable) {
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
		return 0;
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
	public double getAverage(RandomVariableInterface probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getVariance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getVariance(RandomVariableInterface probabilities) {
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
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardError() {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getStandardError(RandomVariableInterface probabilities) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantile(double quantile) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getQuantile(double quantile, RandomVariableInterface probabilities) {
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
	public RandomVariableInterface cache() {
		return this;
	}

	@Override
	public RandomVariableInterface apply(DoubleUnaryOperator operator) {
		return map(x -> x.apply(operator));
	}

	@Override
	public RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument) {
		return map(x -> x.apply(operator, argument));
	}

	@Override
	public RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1, RandomVariableInterface argument2) {
		return map(x -> x.apply(operator, argument1, argument2));
	}

	@Override
	public RandomVariableInterface cap(double cap) {
		return map(x -> x.cap(cap));
	}

	@Override
	public RandomVariableInterface floor(double floor) {
		return map(x -> x.floor(floor));
	}

	@Override
	public RandomVariableInterface add(double value) {
		return map(x -> x.add(value));
	}

	@Override
	public RandomVariableInterface sub(double value) {
		return map(x -> x.sub(value));
	}

	@Override
	public RandomVariableInterface mult(double value) {
		return map(x -> x.mult(value));
	}

	@Override
	public RandomVariableInterface div(double value) {
		return map(x -> x.div(value));
	}

	@Override
	public RandomVariableInterface pow(double exponent) {
		return map(x -> x.pow(exponent));
	}

	@Override
	public RandomVariableInterface average() {
		return map(x -> x.average());
	}

	@Override
	public RandomVariableInterface squared() {
		return map(x -> x.squared());
	}

	@Override
	public RandomVariableInterface sqrt() {
		return map(x -> x.sqrt());
	}

	@Override
	public RandomVariableInterface exp() {
		return map(x -> x.exp());
	}

	@Override
	public RandomVariableInterface log() {
		return map(x -> x.log());
	}

	@Override
	public RandomVariableInterface sin() {
		return map(x -> x.sin());
	}

	@Override
	public RandomVariableInterface cos() {
		return map(x -> x.cos());
	}

	@Override
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {
		return map(x -> x.add(randomVariable));
	}

	@Override
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		return map(x -> x.sub(randomVariable));
	}

	@Override
	public RandomVariableInterface bus(RandomVariableInterface randomVariable) {
		return map(x -> x.bus(randomVariable));
	}

	@Override
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		return map(x -> x.mult(randomVariable));
	}

	@Override
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		return map(x -> x.div(randomVariable));
	}

	@Override
	public RandomVariableInterface vid(RandomVariableInterface randomVariable) {
		return map(x -> x.vid(randomVariable));
	}

	@Override
	public RandomVariableInterface cap(RandomVariableInterface cap) {
		return map(x -> x.cap(cap));
	}

	@Override
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		return map(x -> x.cap(floor));
	}

	@Override
	public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		return map(x -> x.accrue(rate, periodLength));
	}

	@Override
	public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		return map(x -> x.discount(rate, periodLength));
	}

	@Override
	public RandomVariableInterface choose(RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		return map(x -> x.choose(valueIfTriggerNonNegative, valueIfTriggerNegative));
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		throw new UnsupportedOperationException();
	}

	@Override
	public RandomVariableInterface invert() {
		return map(x -> x.invert());
	}

	@Override
	public RandomVariableInterface abs() {
		return map(x -> x.abs());
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {
		return map(x -> x.addProduct(factor1, factor2));
	}

	@Override
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		return map(x -> x.addProduct(factor1, factor2));
	}

	@Override
	public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return map(x -> x.addRatio(numerator, denominator));
	}

	@Override
	public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return map(x -> x.subRatio(numerator, denominator));
	}

	@Override
	public RandomVariableInterface isNaN() {
		return map(x -> x.isNaN());
	}
}
