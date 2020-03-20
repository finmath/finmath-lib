package net.finmath.montecarlo.automaticdifferentiation;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariable;

/**
 * A factory for creating objects implementing
 * <code>net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable</code>.
 *
 * The factory extends the <code>net.finmath.montecarlo.RandomVariableFactory</code>, which can be used
 * to allow for dependency injection.
 *
 * @see net.finmath.stochastic.RandomVariable
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RandomVariableDifferentiableFactory extends RandomVariableFactory {

	/**
	 * Create a (deterministic) random variable from a constant.
	 *
	 * @param value A constant value.
	 * @return The <code>RandomVariableDifferentiable</code>.
	 */
	@Override
	RandomVariableDifferentiable createRandomVariable(double value);

	/**
	 * Create a (deterministic) random variable form a constant using a specific filtration time.
	 *
	 * @param time The filtration time of the random variable.
	 * @param value A constant value.
	 * @return The <code>RandomVariableDifferentiable</code>.
	 */
	@Override
	RandomVariableDifferentiable createRandomVariable(double time, double value);

	/**
	 * Create a random variable form an array using a specific filtration time.
	 *
	 * @param time The filtration time of the random variable.
	 * @param values Array representing values of the random variable at the sample paths.
	 * @return The <code>RandomVariableDifferentiable</code>.
	 */
	@Override
	RandomVariableDifferentiable createRandomVariable(double time, double[] values);

	/**
	 * Create a (deterministic) random variable, which is not differentiable, from a constant.
	 *
	 * @param value A constant value.
	 * @param time The filtration time of the random variable.
	 * @return The <code>RandomVariable</code>.
	 */
	RandomVariable createRandomVariableNonDifferentiable(double time, double value);

	/**
	 * Create a random variable, which is not differentiable, from an array using a specific filtration time.
	 *
	 * @param time The filtration time of the random variable.
	 * @param values Array representing values of the random variable at the sample paths.
	 * @return The <code>RandomVariable</code>.
	 */
	RandomVariable createRandomVariableNonDifferentiable(double time, double[] values);
}
