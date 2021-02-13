/*
 * Created on 25.10.2014
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;

/**
 * A performance index being numeratorIndex(t) / denominatorIndex(t)
 *
 * @author Christian Fries
 * @version 1.0
 */
public class PerformanceIndex extends AbstractIndex {

	private static final long serialVersionUID = -8181742829635380940L;

	private final AbstractProductComponent numeratorIndex;
	private final AbstractProductComponent denominatorIndex;

	/**
	 * Create a performance index being numeratorIndex(t) / denominatorIndex(t)
	 *
	 * @param numeratorIndex First index.
	 * @param denominatorIndex Second index.
	 */
	public PerformanceIndex(final AbstractProductComponent numeratorIndex, final AbstractProductComponent denominatorIndex) {
		super();
		this.numeratorIndex		= numeratorIndex;
		this.denominatorIndex	= denominatorIndex;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
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
		final Set<String>	underlyingNames2		= denominatorIndex.queryUnderlyings();
		if(underlyingNames2 != null) {
			if(underlyingNames != null) {
				underlyingNames.addAll(underlyingNames2);
			} else {
				underlyingNames = underlyingNames2;
			}
		}
		return underlyingNames;
	}
}
