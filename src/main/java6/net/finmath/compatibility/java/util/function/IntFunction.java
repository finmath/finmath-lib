/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.06.2016
 */

package net.finmath.compatibility.java.util.function;

/**
 * Interface mimicking Java 8.
 *
 * @author Christian Fries
 *
 * @param <R> The argument type.
 * @version 1.0
 */
public interface IntFunction<R> {

	/**
	 * Applies this function to the given argument.
	 *
	 * @param value the function argument
	 * @return the function result
	 */
	R apply(int value);
}
