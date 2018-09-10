/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.03.2014
 */
package net.finmath.compatibility.java.util.function;

/**
 * Interface mimiking Java 8.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface DoubleUnaryOperator {

	/**
	 * Applies this operator to the given operand.
	 *
	 * @param operand the operand
	 * @return the operator result
	 */
	double applyAsDouble(double operand);
}
