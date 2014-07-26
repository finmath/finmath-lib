/*
 * Created on 15.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A forward rate index for a given period start offset (offset from fixing) and period length.
 * 
 * @author Christian Fries
 */
public class LIBORIndex extends AbstractIndex {

	private static final long serialVersionUID = 1L;
	
	private final double periodStartOffset;
	private final double periodLength;
    

    /**
     * Creates a forward rate index for a given period start offset (offset from fixing) and period length.
     * 
     * @param periodStartOffset An offset added to the fixing to define the period start.
     * @param periodLength The period length
     */
    public LIBORIndex(double periodStartOffset, double periodLength) {
	    super();
	    this.periodStartOffset = periodStartOffset;
	    this.periodLength = periodLength;
    }

    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

    	RandomVariableInterface forwardRate = model.getLIBOR(evaluationTime, evaluationTime+periodStartOffset, evaluationTime+periodStartOffset+periodLength);

    	return forwardRate;
    }
}
