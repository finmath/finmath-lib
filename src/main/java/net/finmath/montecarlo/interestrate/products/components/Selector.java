/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

/**
 * A selection of a value on another component.
 * Implements this.getValue(time, model) via underlying.getValues(time, model)[key].
 *
 * @author Christian Fries
 * @version 1.1
 */
public class Selector extends AbstractProductComponent {

	private static final long serialVersionUID = 3211126102506873636L;

	private final String key;
	private final TermStructureMonteCarloProduct underlying;

	/**
	 * Creates the function underlying.getValues()[key]
	 *
	 * @param key Name of the key to be selected.
	 * @param underlying Underlying to which the selector should be applied.
	 */
	public Selector(final String key, final TermStructureMonteCarloProduct underlying) {
		super();
		this.key = key;
		this.underlying = underlying;
	}

	@Override
	public Set<String> queryUnderlyings() {
		if(underlying instanceof AbstractProductComponent) {
			return ((AbstractProductComponent)underlying).queryUnderlyings();
		} else {
			throw new IllegalArgumentException("Underlying cannot be queried for underlyings.");
		}
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		return (RandomVariable)(underlying.getValues(evaluationTime, model).get(key));
	}
}
