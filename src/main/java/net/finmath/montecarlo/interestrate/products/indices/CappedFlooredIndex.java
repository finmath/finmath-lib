/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * An capped and floored index paying min(max(index(t),floor(t)),cap(t)), where index, floor and cap are indices,
 * i.e., objects implementing <code>AbstractIndex</code>.
 * 
 * @author Christian Fries
 */
public class CappedFlooredIndex extends AbstractIndex {

	private static final long serialVersionUID = 7835825574794506180L;

	private final AbstractIndex index;
	private final AbstractIndex cap;
	private final AbstractIndex floor;

    /**
     * Create an capped and floored index paying min(max(index(t),floor(t)),cap(t)).
     * 
     * @param index An index.
     * @param cap An index.
     * @param floor An index.
     */
    public CappedFlooredIndex(AbstractIndex index, AbstractIndex cap, AbstractIndex floor) {
		super();
		this.index	= index;
		this.cap	= cap;
		this.floor	= floor;
	}

    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
    	RandomVariableInterface indexValues = index.getValue(evaluationTime, model);

    	if(floor != null)	indexValues = indexValues.floor(floor.getValue(evaluationTime, model));
    	if(cap != null)		indexValues = indexValues.cap(cap.getValue(evaluationTime, model));

    	return indexValues;
    }
}
