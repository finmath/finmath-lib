/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * A single deterministic cashflow at a fixed time
 *
 * @author Christian Fries
 * @version 1.1
 */
public class Numeraire extends AbstractProductComponent {

	private static final long serialVersionUID = 2336470863786839896L;

	/**
	 * Create a product being the numeraire of the given model (will depend on the model).
	 */
	public Numeraire() {
		super(null);
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * cash-flows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		return model.getNumeraire(evaluationTime);

	}
}
