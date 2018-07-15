/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.03.2014
 */
package net.finmath.compatibility.java.util.function;

/**
 * Interface mimicking <code>java.util.function.Function</code>.
 *
 * @author Christian Fries
 *
 * @param <T> Argument type.
 * @param <R> Result type.
 */
public interface Function<T, R> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param t the function argument
	 * @return the function result
	 */
	R apply(T t);
}
