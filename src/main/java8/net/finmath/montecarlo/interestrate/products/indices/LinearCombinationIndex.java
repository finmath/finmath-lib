/*
 * Created on 06.12.2009
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
 * A linear combination index paying scaling1 * index1(t) + scaling2 * index2(t)
 *
 * @author Christian Fries
 * @version 1.0
 */
public class LinearCombinationIndex extends AbstractIndex {

	private static final long serialVersionUID = -8181742829635380940L;

	private final AbstractProductComponent index1;
	private final AbstractProductComponent index2;
	private final double scaling1;
	private final double scaling2;

	/**
	 * Create a linear combination index paying scaling1 * index1(t) + scaling2 * index2(t)
	 *
	 * @param scaling1 Scaling for first index.
	 * @param index1 First index.
	 * @param scaling2 Scaling for second index.
	 * @param index2 Second index.
	 */
	public LinearCombinationIndex(final double scaling1, final AbstractProductComponent index1, final double scaling2, final AbstractProductComponent index2) {
		super();
		this.scaling1	= scaling1;
		this.index1		= index1;
		this.scaling2	= scaling2;
		this.index2		= index2;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		return index1.getValue(evaluationTime, model).mult(scaling1)
				.addProduct(index2.getValue(evaluationTime, model),scaling2);
	}

	/**
	 * Returns the index 1.
	 *
	 * @return the index 1.
	 */
	public AbstractProductComponent getIndex1() {
		return index1;
	}

	/**
	 * Returns the index 2.
	 *
	 * @return the index 2
	 */
	public AbstractProductComponent getIndex2() {
		return index2;
	}

	/**
	 * Returns the scaling 1.
	 *
	 * @return the scaling 1
	 */
	public double getScaling1() {
		return scaling1;
	}

	/**
	 * Returns the scaling 2.
	 *
	 * @return the scaling 2
	 */
	public double getScaling2() {
		return scaling2;
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

	@Override
	public String toString() {
		return "LinearCombinationIndex [index1=" + index1 + ", index2="
				+ index2 + ", scaling1=" + scaling1 + ", scaling2=" + scaling2
				+ ", toString()=" + super.toString() + "]";
	}
}
