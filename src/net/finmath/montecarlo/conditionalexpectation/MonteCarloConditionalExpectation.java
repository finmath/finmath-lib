/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 13.08.2004
 */
package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 */
public interface MonteCarloConditionalExpectation {
    RandomVariableInterface getConditionalExpectation(ImmutableRandomVariableInterface randomVariable);
}
