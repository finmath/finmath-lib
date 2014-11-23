/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 04.05.2008
 */
package net.finmath.stochastic;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntToDoubleFunction;
import java.util.stream.DoubleStream;

import net.finmath.functions.DoubleTernaryOperator;


/**
 * This class wraps an immutable class (here <code>RandomVariableInterface</code>)
 * and implements the interface of an mutable class (here <code>RandomVariableInterface</code>).
 * If a method is called which mutates the object (a mutator), a defensive copy is performed once.
 * The reference to the immutable class is then replaced by the reference to the defensive copy.
 * By returning an <code>RandomVariableMutableClone</code> of an
 * <code>RandomVariableInterface</code> defensive copies are made only when necessary.
 * 
 * This class is not thread safe (classes implementing <code>RandomVariableInterface</code>
 * are not expected to be thread safe).
 * 
 * @author Christian Fries
 * @version 1.1
 * @deprecated
 */
public class RandomVariableMutableClone implements RandomVariableInterface {
	
	private boolean isRandomVariableMutable = false;
	private RandomVariableInterface randomVariable;

	public RandomVariableMutableClone(RandomVariableInterface randomVariable) {
		super();
		this.randomVariable = randomVariable;
	}

	/**
	 * Access to the defensive copy. The defensive copy is made only once.
	 * @return The defensive copy.
	 */
	private RandomVariableInterface ensureMutable() {
		if(!isRandomVariableMutable) {
			randomVariable = randomVariable.getMutableCopy();
			isRandomVariableMutable = true;
		}

		return randomVariable;
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#abs()
	 */
	public RandomVariableInterface abs() {
		return ensureMutable().abs();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(double)
	 */
	public RandomVariableInterface add(double value) {
		return ensureMutable().add(value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#add(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface add(RandomVariableInterface randomVariable) {
		return ensureMutable().add(randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, double)
	 */
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, double factor2) {		
		return ensureMutable().addProduct(factor1, factor2);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#addProduct(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface addProduct(RandomVariableInterface factor1, RandomVariableInterface factor2) {
		return ensureMutable().addProduct(factor1, factor2);
	}

	/* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#accrue(net.finmath.stochastic.RandomVariableInterface, double)
     */
    public RandomVariableInterface accrue(RandomVariableInterface rate, double periodLength) {
		return ensureMutable().accrue(rate, periodLength);
    }

    /* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, RandomVariableInterface valueIfTriggerNegative) {
		return ensureMutable().barrier(trigger, valueIfTriggerNonNegative, valueIfTriggerNegative);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#barrier(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface barrier(RandomVariableInterface trigger, RandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative) {
		return ensureMutable().barrier(trigger, valueIfTriggerNonNegative, valueIfTriggerNegative);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cap(double)
	 */
	public RandomVariableInterface cap(double cap) {
		return ensureMutable().cap(cap);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface cap(RandomVariableInterface cap) {
		return ensureMutable().cap(cap);
	}

	/* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#discount(net.finmath.stochastic.RandomVariableInterface, double)
     */
    public RandomVariableInterface discount(RandomVariableInterface rate, double periodLength) {
		return ensureMutable().discount(rate, periodLength);
    }

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(double)
	 */
	public RandomVariableInterface div(double value) {
		return ensureMutable().div(value);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#div(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface div(RandomVariableInterface randomVariable) {
		return ensureMutable().div(randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#equals(net.finmath.stochastic.RandomVariableInterface)
	 */
	public boolean equals(RandomVariableInterface randomVariable) {
		return randomVariable.equals(randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#exp()
	 */
	public RandomVariableInterface exp() {
		return ensureMutable().exp();
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(double)
	 */
	public RandomVariableInterface floor(double floor) {
		return ensureMutable().floor(floor);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#floor(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface floor(RandomVariableInterface floor) {
		return ensureMutable().floor(floor);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#get(int)
	 */
	public double get(int pathOrState) {
		return randomVariable.get(pathOrState);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage()
	 */
	public double getAverage() {
		return randomVariable.getAverage();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getAverage(net.finmath.montecarlo.RandomVariable)
	 */
	public double getAverage(RandomVariableInterface probabilities) {
		return randomVariable.getAverage(probabilities);
	}


	/* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double)
     */
    @Override
    public double getQuantile(double quantile) {
		return randomVariable.getQuantile(quantile);
    }
    
	/* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getQuantile(double, net.finmath.stochastic.RandomVariableInterface)
     */
    @Override
    public double getQuantile(double quantile, RandomVariableInterface probabilities) {
		return randomVariable.getQuantile(quantile, probabilities);
    }

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getQuantileExpectation(double)
	 */
    @Override
    public double getQuantileExpectation(double quantileStart, double quantileEnd) {
		return randomVariable.getQuantileExpectation(quantileStart, 1.0);
    }

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(double[])
	 */
    @Override
    public double[] getHistogram(double[] intervalPoints) {
		return randomVariable.getHistogram(intervalPoints);
    }

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getHistogram(int,double)
	 */
    @Override
	public double[][] getHistogram(int numberOfPoints, double standardDeviations) {
		return randomVariable.getHistogram(numberOfPoints, standardDeviations);
	}

    @Override
    public RandomVariableInterface apply(DoubleUnaryOperator operator) {
        return randomVariable.apply(operator);
    }

    @Override
    public RandomVariableInterface apply(DoubleBinaryOperator operator, RandomVariableInterface argument) {
        return randomVariable.apply(operator, argument);
    }

    @Override
    public RandomVariableInterface apply(DoubleTernaryOperator operator, RandomVariableInterface argument1, RandomVariableInterface argument2) {
        return randomVariable.apply(operator, argument1, argument2);
    }

    @Override
	public double getFiltrationTime() {
		return randomVariable.getFiltrationTime();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMax()
	 */
	public double getMax() {
		return randomVariable.getMax();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMin()
	 */
	public double getMin() {
		return randomVariable.getMin();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getMutableCopy()
	 */
	public RandomVariableInterface getMutableCopy() {
		return randomVariable.getMutableCopy();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations()
	 */
	public double[] getRealizations() {
		return randomVariable.getRealizations();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getRealizations(int)
	 */
	@Override
	public double[] getRealizations(int numberOfPaths) {
		return randomVariable.getRealizations(numberOfPaths);
	}

    @Override
    public IntToDoubleFunction getOperator() {
        return randomVariable.getOperator();
    }

    @Override
    public DoubleStream getRealizationsStream() {
        return randomVariable.getRealizationsStream();
    }

    /* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation()
     */
	@Override
	public double getStandardDeviation() {
		return randomVariable.getStandardDeviation();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardDeviation(net.finmath.stochastic.RandomVariableInterface)
	 */
	@Override
	public double getStandardDeviation(RandomVariableInterface probabilities) {
		return randomVariable.getStandardDeviation(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError()
	 */
	public double getStandardError() {
		return randomVariable.getStandardError();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getStandardError(net.finmath.stochastic.RandomVariableInterface)
	 */
	public double getStandardError(RandomVariableInterface probabilities) {
		return randomVariable.getStandardError(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance()
	 */
	public double getVariance() {
		return randomVariable.getVariance();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#getVariance(net.finmath.stochastic.RandomVariableInterface)
	 */
	public double getVariance(RandomVariableInterface probabilities) {
		return randomVariable.getVariance(probabilities);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#invert()
	 */
	public RandomVariableInterface invert() {
		return ensureMutable().invert();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isDeterministic()
	 */
	public boolean isDeterministic() {
		return randomVariable.isDeterministic();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#log()
	 */
	public RandomVariableInterface log() {
		return ensureMutable().log();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sin()
	 */
	public RandomVariableInterface sin() {
		return ensureMutable().sin();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#cos()
	 */
	public RandomVariableInterface cos() {
		return ensureMutable().cos();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(double)
	 */
	public RandomVariableInterface mult(double value) {
		return ensureMutable().mult(value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#mult(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface mult(RandomVariableInterface randomVariable) {
		return ensureMutable().mult(randomVariable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#pow(double)
	 */
	public RandomVariableInterface pow(double exponent) {
		return ensureMutable().pow(exponent);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#size()
	 */
	public int size() {
		return randomVariable.size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#squared()
	 */
	public RandomVariableInterface squared() {
		return ensureMutable().squared();
	}

	/* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#sqrt()
     */
    public RandomVariableInterface sqrt() {
		return ensureMutable().sqrt();
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(double)
	 */
	public RandomVariableInterface sub(double value) {
		return ensureMutable().sub(value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#sub(net.finmath.stochastic.RandomVariableInterface)
	 */
	public RandomVariableInterface sub(RandomVariableInterface randomVariable) {
		return ensureMutable().sub(randomVariable);
	}

	/* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#addRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
     */
    public RandomVariableInterface addRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return ensureMutable().addRatio(numerator, denominator);
    }

	/* (non-Javadoc)
     * @see net.finmath.stochastic.RandomVariableInterface#subRatio(net.finmath.stochastic.RandomVariableInterface, net.finmath.stochastic.RandomVariableInterface)
     */
    public RandomVariableInterface subRatio(RandomVariableInterface numerator, RandomVariableInterface denominator) {
		return ensureMutable().subRatio(numerator, denominator);
    }

	/* (non-Javadoc)
	 * @see net.finmath.stochastic.RandomVariableInterface#isNaN()
	 */
	@Override
	public RandomVariableInterface isNaN() {
		// TODO Auto-generated method stub
		return ensureMutable().isNaN();
	}

	@Override
    public RandomVariableInterface cache() {
        return this;
    }
}
