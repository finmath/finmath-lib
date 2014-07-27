/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A linear combination index paying scaling1 * index1(t) + scaling2 * index2(t)
 * 
 * @author Christian Fries
 */
public class LinearCombinationIndex extends AbstractIndex {

	private static final long serialVersionUID = -8181742829635380940L;

	private AbstractProductComponent index1;
	private AbstractProductComponent index2;
	private double scaling1;
	private double scaling2;

    /**
     * Create a linear combination index paying scaling1 * index1(t) + scaling2 * index2(t)
     * 
     * @param scaling1 Scaling for first index.
     * @param index1 First index.
     * @param scaling2 Scaling for second index.
     * @param index2 Second index.
     */
    public LinearCombinationIndex(double scaling1, AbstractProductComponent index1, double scaling2, AbstractProductComponent index2) {
		super();
		this.scaling1	= scaling1;
		this.index1		= index1;
		this.scaling2	= scaling2;
		this.index2		= index2;
	}

    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
    	return index1.getValue(evaluationTime, model).mult(scaling1)
    			.add(index2.getValue(evaluationTime, model).mult(scaling2));
    }
}
