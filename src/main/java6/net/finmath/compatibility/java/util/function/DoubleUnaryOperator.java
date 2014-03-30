/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 29.03.2014
 */
package net.finmath.compatibility.java.util.function;

/**
 * Interface mimiking Java 8.
 *
 * @author Christian Fries
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
