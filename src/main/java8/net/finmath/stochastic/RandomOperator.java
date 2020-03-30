/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.10.2007
 * Created on 02.02.2014
 */
package net.finmath.stochastic;

import java.util.Objects;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * @author Chrisitan Fries
 */
@FunctionalInterface
public interface RandomOperator extends UnaryOperator<RandomVariable> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param value the function argument
	 * @return the function result
	 */
	@Override
	RandomVariable apply(RandomVariable value);

	/**
	 * Returns a composed function that first applies the {@code before}
	 * function to its input, and then applies this function to the result.
	 * If evaluation of either function throws an exception, it is relayed to
	 * the caller of the composed function.
	 *
	 * @param before the function to apply before this function is applied
	 * @return a composed function that first applies the {@code before}
	 * function and then applies this function
	 * @throws NullPointerException if before is null
	 *
	 * @see #andThen(Function)
	 */
	default RandomOperator compose(RandomOperator before) {
		Objects.requireNonNull(before);
		return v -> apply(before.apply(v));
	}

	/**
	 * Returns a composed function that first applies this function to
	 * its input, and then applies the {@code after} function to the result.
	 * If evaluation of either function throws an exception, it is relayed to
	 * the caller of the composed function.
	 *
	 * @param after the function to apply after this function is applied
	 * @return a composed function that first applies this function and then
	 * applies the {@code after} function
	 * @throws NullPointerException if after is null
	 *
	 * @see #compose(Function)
	 */
	default RandomOperator andThen(RandomOperator after) {
		Objects.requireNonNull(after);
		return t -> after.apply(apply(t));
	}

	/**
	 * Returns a function that always returns its input argument.
	 *
	 * @return a function that always returns its input argument
	 */
	static RandomOperator identity() {
		return t -> t;
	}
}
