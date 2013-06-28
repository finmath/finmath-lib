/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 05.05.2008
 */
package net.finmath.stochastic;


/**
 * The interface implemented by a mutable random variable.
 *
 * @author Christian Fries
 * @version 1.2
 */
public interface RandomVariableInterface extends ImmutableRandomVariableInterface {

	/**
	 * Applies x &rarr; min(x,cap) to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface cap(double cap);

	/**
	 * Applies x &rarr; max(x,floor) to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface floor(double floor);

	/**
	 * Applies x &rarr; x + value to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface add(double value);

	/**
	 * Applies x &rarr; x - value to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface sub(double value);

	/**
	 * Applies x &rarr; x * value to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface mult(double value);

	/**
	 * Applies x &rarr; x / value to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface div(double value);

	/**
	 * Applies x &rarr; pow(x,exponent) to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface pow(double exponent);

	/**
	 * Applies x &rarr; x * x to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface squared();

	/**
	 * Applies x &rarr; sqrt(x) to this random variable.
	 * @return A self reference.
	 */
     RandomVariableInterface sqrt();

    /**
	 * Applies x &rarr; exp(x) to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface exp();

	/**
	 * Applies x &rarr; log(x) to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface log();

	/**
	 * Applies x &rarr; sin(x) to this random variable.
	 * @return A self reference.
	 */
     RandomVariableInterface sin();

	/**
	 * Applies x &rarr; cos(x) to this random variable.
	 * @return A self reference.
	 */
     RandomVariableInterface cos();

    /**
	 * Applies x &rarr; x+randomVariable to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface add(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; x-randomVariable to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface sub(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; x*randomVariable to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface mult(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; x/randomVariable to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface div(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x &rarr; min(x,cap) to this random variable.
	 * @param cap The cap
	 * @return A self reference.
	 */
	 RandomVariableInterface cap(ImmutableRandomVariableInterface cap);

	/**
	 * Applies x &rarr; max(x,floor) to this random variable.
	 * @param floor The floor
	 * @return A self reference.
	 */
	 RandomVariableInterface floor(ImmutableRandomVariableInterface floor);

	/**
	 * Applies x &rarr; x * (1.0 + rate * periodLength) to this random variable.
	 * @param rate The accruing rate
	 * @param periodLength The period length
	 * @return A self reference.
	 */
	 RandomVariableInterface accrue(ImmutableRandomVariableInterface rate, double periodLength);

	/**
	 * Applies x &rarr; x / (1.0 + rate * periodLength) to this random variable.
	 * @param rate The discounting rate
	 * @param periodLength The period length
	 * @return A self reference.
	 */
	 RandomVariableInterface discount(ImmutableRandomVariableInterface rate, double periodLength);

	/**
	 * Applies x &rarr; (trigger >= 0 ? valueIfTriggerNonNegative : valueIfTriggerNegative)
     * @param trigger The trigger
     * @param valueIfTriggerNonNegative The value used if the trigger is greater or equal 0
     * @param valueIfTriggerNegative The value used if the trigger is less than 0
     * @return A self reference
     */
     RandomVariableInterface barrier(ImmutableRandomVariableInterface trigger, ImmutableRandomVariableInterface valueIfTriggerNonNegative, ImmutableRandomVariableInterface valueIfTriggerNegative);

    /**
	 * Applies x &rarr; (trigger >= 0 ? valueIfTriggerNonNegative : valueIfTriggerNegative)
     * @param trigger The trigger
     * @param valueIfTriggerNonNegative The value used if the trigger is greater or equal 0
     * @param valueIfTriggerNegative The value used if the trigger is less than 0
     * @return A self reference
     */
	 RandomVariableInterface barrier(ImmutableRandomVariableInterface trigger, ImmutableRandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative);

	/**
	 * Applies x &rarr; 1/x to this random variable. 
	 * @return A self reference.
	 */
	 RandomVariableInterface invert();

	/**
	 * Applies x &rarr; Math.abs(x), i.e. x &rarr; |x| to this random variable.
	 * @return A self reference.
	 */
	 RandomVariableInterface abs();

	/**
	 * Applies x &rarr; x + factor1 * factor2
	 * @return self reference.
	 */
	 RandomVariableInterface addProduct(ImmutableRandomVariableInterface factor1, double factor2);

	/**
	 * Applies x &rarr; x + factor1 * factor2
	 * @return self reference.
	 */
	 RandomVariableInterface addProduct(ImmutableRandomVariableInterface factor1, ImmutableRandomVariableInterface factor2);

	/**
	 * Applies x &rarr; x + numerator / denominator
	 * 
     * @param numerator The numerator of the ratio to add.
     * @param denominator The denominator of the ratio to add.
	 * @return self reference.
     */
     RandomVariableInterface addRatio(ImmutableRandomVariableInterface numerator, ImmutableRandomVariableInterface denominator);

	/**
	 * Applies x &rarr; x - numerator / denominator
	 * 
     * @param numerator The numerator of the ratio to sub.
     * @param denominator The denominator of the ratio to sub.
	 * @return self reference.
     */
     RandomVariableInterface subRatio(ImmutableRandomVariableInterface numerator, ImmutableRandomVariableInterface denominator);
}