/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2016
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariable;

/**
 * A factory for creating objects implementing <code>net.finmath.stochastic.RandomVariable</code>.
 *
 * Use this interface in your implementations to allow <i>dependency injection</i>, i.e. to allow the use
 * of different implementations of <code>net.finmath.stochastic.RandomVariable</code> whenever random variables
 * need to be constructed.
 *
 * @see net.finmath.stochastic.RandomVariable
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RandomVariableFactory {

	/**
	 * Static method for creating random variables from Objects.
	 *
	 * @param randomVariableFactory The RandomVariableFactory used to construct the random variable, if needed.
	 * @param value The value used to construct the random variable, if needed.
	 * @param defaultValue The default value to be used if value is null.
	 * @return A RandomVariable with the given value (or the defaultValue).
	 */
	static 	RandomVariable getRandomVariableOrDefault(RandomVariableFactory randomVariableFactory, Object value, RandomVariable defaultValue) {
		if(value == null) {
			return defaultValue;
		}
		else if(value instanceof RandomVariable) {
			return (RandomVariable)value;
		}
		else if(value instanceof Number) {
			if(randomVariableFactory == null) {
				throw new NullPointerException("Object value of type Number but nor randomVariableFactory given.");
			}
			return randomVariableFactory.createRandomVariable(((Number)value).doubleValue());
		}
		else {
			throw new IllegalArgumentException("Object value must be of type Number or RandomVariable.");
		}
	}

	/**
	 * Create a (deterministic) random variable from a constant.
	 *
	 * @param value A constant value.
	 * @return The <code>RandomVariable</code>.
	 */
	RandomVariable createRandomVariable(double value);

	/**
	 * Create a (deterministic) random variable from a constant using a specific filtration time.
	 *
	 * @param time The filtration time of the random variable.
	 * @param value A constant value.
	 * @return The <code>RandomVariable</code>.
	 */
	RandomVariable createRandomVariable(double time, double value);

	/**
	 * Create a random variable from an array using a specific filtration time.
	 *
	 * @param time The filtration time of the random variable.
	 * @param values Array representing values of the random variable at the sample paths.
	 * @return The <code>RandomVariable</code>.
	 */
	RandomVariable createRandomVariable(double time, double[] values);

	default RandomVariable createRandomVariable(double[] values) {
		return createRandomVariable(0.0, values);
	}

	/**
	 * Create an array of (deterministic) random variables from an array of constants.
	 *
	 * @param values Array representing constants.
	 * @return The <code>RandomVariable</code>.
	 */
	RandomVariable[] createRandomVariableArray(double[] values);

	/**
	 * Create a matrix of (deterministic) random variables from an matrix of constants.
	 *
	 * @param values Matrix representing constants.
	 * @return The <code>RandomVariable</code>.
	 */
	RandomVariable[][] createRandomVariableMatrix(double[][] values);
}
