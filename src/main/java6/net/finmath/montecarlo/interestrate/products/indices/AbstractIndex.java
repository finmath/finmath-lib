/*
 * Created on 03.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base class for indices.
 * 
 * Indices are small functions mapping time and a vector of
 * random variables to a random variable, where the time
 * is the fixing time of the index.
 * 
 * @author Christian Fries
 */
public abstract class AbstractIndex extends AbstractProductComponent {

    private static final long serialVersionUID = 7992943924779922710L;

	public abstract RandomVariableInterface getValue(double fixingTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;
}
