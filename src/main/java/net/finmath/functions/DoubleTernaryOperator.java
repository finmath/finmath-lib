/*
 * Created on 07.02.2014.
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.functions;

/**
 * Functional interface for functions mapping (double,double,double) to double.
 *
 * @author Christian Fries
 * @see java.util.function.DoubleUnaryOperator
 * @version 1.0
 */
@FunctionalInterface
public interface DoubleTernaryOperator {
	/**
	 * Applies this operator to the given operands.
	 *
	 * @param x the first operand
	 * @param y the second operand
	 * @param z the third operand
	 * @return the operator result
	 */
	double applyAsDouble(double x, double y, double z);
}
