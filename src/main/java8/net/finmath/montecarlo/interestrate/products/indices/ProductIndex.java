/*
 * Created on 03.09.2006
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
 * A product index being index1(t) * index2(t)
 *
 * @author Christian Fries
 * @version 1.0
 */
public class ProductIndex extends AbstractIndex {

	private static final long serialVersionUID = -8181742829635380940L;

	private final AbstractProductComponent index1;
	private final AbstractProductComponent index2;

	/**
	 * Create a performance index being numeratorIndex(t) / denominatorIndex(t)
	 *
	 * @param index1 First index.
	 * @param index2 Second index.
	 */
	public ProductIndex(final AbstractIndex index1, final AbstractIndex index2) {
		super();
		this.index1	= index1;
		this.index2	= index2;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		return index1.getValue(evaluationTime, model).mult(index2.getValue(evaluationTime, model));
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames			= index1.queryUnderlyings();
		final Set<String>	underlyingNames2		= index2.queryUnderlyings();
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
