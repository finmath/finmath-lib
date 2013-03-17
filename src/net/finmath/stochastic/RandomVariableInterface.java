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
	 * Applies x -> min(x,cap) to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface cap(double cap);

	/**
	 * Applies x -> max(x,floor) to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface floor(double floor);

	/**
	 * Applies x -> x + value to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface add(double value);

	/**
	 * Applies x -> x - value to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface sub(double value);

	/**
	 * Applies x -> x / value to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface mult(double value);

	/**
	 * Applies x -> x * value to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface div(double value);

	/**
	 * Applies x -> pow(x,exponent) to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface pow(double exponent);

	/**
	 * Applies x -> x * x to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface squared();

	/**
	 * Applies x -> sqrt(x) to this random variable.
	 * @return A self reference.
	 */
    public RandomVariableInterface sqrt();

    /**
	 * Applies x -> exp(x) to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface exp();

	/**
	 * Applies x -> log(x) to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface log();

	/**
	 * Applies x -> sin(x) to this random variable.
	 * @return A self reference.
	 */
    public RandomVariableInterface sin();

	/**
	 * Applies x -> cos(x) to this random variable.
	 * @return A self reference.
	 */
    public RandomVariableInterface cos();

    /**
	 * Applies x -> x+randomVariable to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface add(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x -> x-randomVariable to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface sub(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x -> x*randomVariable to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface mult(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x -> x/randomVariable to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface div(ImmutableRandomVariableInterface randomVariable);

	/**
	 * Applies x -> min(x,cap) to this random variable.
	 * @param cap The cap
	 * @return A self reference.
	 */
	public RandomVariableInterface cap(ImmutableRandomVariableInterface cap);

	/**
	 * Applies x -> max(x,floor) to this random variable.
	 * @param floor The floor
	 * @return A self reference.
	 */
	public RandomVariableInterface floor(ImmutableRandomVariableInterface floor);

	/**
	 * Applies x -> x * (1.0 + rate * periodLength) to this random variable.
	 * @param rate The accruing rate
	 * @param periodLength The period length
	 * @return A self reference.
	 */
	public RandomVariableInterface accrue(ImmutableRandomVariableInterface rate, double periodLength);

	/**
	 * Applies x -> x / (1.0 + rate * periodLength) to this random variable.
	 * @param rate The discounting rate
	 * @param periodLength The period length
	 * @return A self reference.
	 */
	public RandomVariableInterface discount(ImmutableRandomVariableInterface rate, double periodLength);

	/**
	 * Applies x -> (trigger >= 0 ? valueIfTriggerNonNegative : valueIfTriggerNegative)
     * @param trigger The trigger
     * @param valueIfTriggerNonNegative The value used if the trigger is greater or equal 0
     * @param valueIfTriggerNegative The value used if the trigger is less than 0
     * @return A self reference
     */
    public RandomVariableInterface barrier(ImmutableRandomVariableInterface trigger, ImmutableRandomVariableInterface valueIfTriggerNonNegative, ImmutableRandomVariableInterface valueIfTriggerNegative);

    /**
	 * Applies x -> (trigger >= 0 ? valueIfTriggerNonNegative : valueIfTriggerNegative)
     * @param trigger The trigger
     * @param valueIfTriggerNonNegative The value used if the trigger is greater or equal 0
     * @param valueIfTriggerNegative The value used if the trigger is less than 0
     * @return A self reference
     */
	public RandomVariableInterface barrier(ImmutableRandomVariableInterface trigger, ImmutableRandomVariableInterface valueIfTriggerNonNegative, double valueIfTriggerNegative);

	/**
	 * Applies x -> 1/x to this random variable. 
	 * @return A self reference.
	 */
	public RandomVariableInterface invert();

	/**
	 * Applies x -> Math.abs(x), i.e. x -> |x| to this random variable.
	 * @return A self reference.
	 */
	public RandomVariableInterface abs();

	/**
	 * Applies x -> x + factor1 * factor2
	 * @return self reference.
	 */
	public RandomVariableInterface addProduct(ImmutableRandomVariableInterface factor1, double factor2);

	/**
	 * Applies x -> x + factor1 * factor2
	 * @return self reference.
	 */
	public RandomVariableInterface addProduct(ImmutableRandomVariableInterface factor1, ImmutableRandomVariableInterface factor2);

	/**
	 * Applies x -> x + numerator / denominator
	 * 
     * @param numerator The numerator of the ratio to add.
     * @param denominator The denominator of the ratio to add.
	 * @return self reference.
     */
    public RandomVariableInterface addRatio(ImmutableRandomVariableInterface numerator, ImmutableRandomVariableInterface denominator);

	/**
	 * Applies x -> x - numerator / denominator
	 * 
     * @param numerator The numerator of the ratio to sub.
     * @param denominator The denominator of the ratio to sub.
	 * @return self reference.
     */
    public RandomVariableInterface subRatio(ImmutableRandomVariableInterface numerator, ImmutableRandomVariableInterface denominator);
}