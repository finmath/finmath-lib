/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariableInterface;

/**
 *
 * @author Christian Fries
 */
public class RandomVariableOperatorFactory extends AbstractRandomVariableFactory {

    @Override
    public RandomVariableInterface createRandomVariable(double time, double value) {
        return new RandomVariableOperator(time, value);
    }

    @Override
    public RandomVariableInterface createRandomVariable(double time, double[] values) {
        return new RandomVariableOperator(time, values);
    }
}
