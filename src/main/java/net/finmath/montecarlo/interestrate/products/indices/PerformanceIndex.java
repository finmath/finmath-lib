/*
 * Created on 25.10.2014
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A performance index being numeratorIndex(t) / denominatorIndex(t)
 * 
 * @author Christian Fries
 */
public class PerformanceIndex extends AbstractIndex {

	private static final long serialVersionUID = -8181742829635380940L;

	private AbstractProductComponent numeratorIndex;
	private AbstractProductComponent denominatorIndex;

    /**
     * Create a performance index being numeratorIndex(t) / denominatorIndex(t)
     * 
     * @param numeratorIndex First index.
     * @param denominatorIndex Second index.
     */
    public PerformanceIndex(AbstractProductComponent numeratorIndex, AbstractProductComponent denominatorIndex) {
		super();
		this.numeratorIndex		= numeratorIndex;
		this.denominatorIndex	= denominatorIndex;
	}

    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
    	return numeratorIndex.getValue(evaluationTime, model).div(denominatorIndex.getValue(evaluationTime, model));
    }

	/**
	 * Returns the numerator index.
	 * 
	 * @return the numerator index.
	 */
	public AbstractProductComponent getNumeratorIndex() {
		return numeratorIndex;
	}

	/**
	 * Returns the denominator index.
	 * 
	 * @return the denominator index.
	 */
	public AbstractProductComponent getDenominatorIndex() {
		return denominatorIndex;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames			= numeratorIndex.queryUnderlyings();
		Set<String>	underlyingNames2		= denominatorIndex.queryUnderlyings();
		if(underlyingNames2 != null) {
			if(underlyingNames != null)	underlyingNames.addAll(underlyingNames2);
			else						underlyingNames = underlyingNames2;
		}
		return underlyingNames;
	}
}
