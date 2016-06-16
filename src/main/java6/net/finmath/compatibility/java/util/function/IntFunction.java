/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 12.06.2016
 */

package net.finmath.compatibility.java.util.function;

/**
 * Interface mimiking Java 8.
 *
 * @author Christian Fries
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
