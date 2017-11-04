/*
 * Created on 06.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A time-lagged index paying index(t+fixingOffset)
 * 
 * @author Christian Fries
 */
public class LaggedIndex extends AbstractIndex {

	private static final long serialVersionUID = 4899043672016395530L;

	final AbstractProductComponent	index;
	final double					fixingOffset;

	/**
	 * Creates a time-lagged index paying index(t+fixingOffset).
	 * 
	 * @param index An index.
	 * @param fixingOffset Offset added to the fixing (evaluation time) of this index to fix the underlying index.
	 */
	public LaggedIndex(AbstractProductComponent index, double fixingOffset) {
		super();
		this.index			= index;
		this.fixingOffset	= fixingOffset;
	}

	@Override
	public Set<String> queryUnderlyings() {
		return index.queryUnderlyings();
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return index.getValue(evaluationTime + fixingOffset, model);
	}
}
