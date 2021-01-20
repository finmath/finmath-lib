/*
 * Created on 06.12.2009
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Collection;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.components.NotionalFromConstant;
import net.finmath.montecarlo.interestrate.products.components.Period;
import net.finmath.montecarlo.interestrate.products.components.ProductCollection;
import net.finmath.montecarlo.interestrate.products.indices.AbstractIndex;
import net.finmath.montecarlo.interestrate.products.indices.FixedCoupon;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the pricing of a swap under a AbstractLIBORMarketModel
 *
 * @author Christian Fries
 * @version 1.2
 */
public class SwapWithComponents extends AbstractLIBORMonteCarloProduct {

	private final ProductCollection underlying;

	/**
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public SwapWithComponents(
			final double[] fixingDates,
			final double[] paymentDates,
			final double[] swaprates) {
		super();

		/*
		 * Create components.
		 *
		 * The interesting part here is, that the creation of the components implicitly
		 * constitutes the (traditional) pricing algorithms (e.g., loop over all periods).
		 * Hence, the definition of the product is the definition of the pricing algorithm.
		 */

		final Collection<AbstractProductComponent> legs = new ArrayList<>();

		final Notional notional = new NotionalFromConstant(1.0);

		final Collection<AbstractProductComponent> fixedLegPeriods = new ArrayList<>();
		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			final AbstractIndex index = new FixedCoupon(swaprates[periodIndex]);
			final Period period = new Period(fixingDates[periodIndex], paymentDates[periodIndex], fixingDates[periodIndex], paymentDates[periodIndex], notional, index, true, false, true);
			fixedLegPeriods.add(period);
		}
		final ProductCollection fixedLeg = new ProductCollection(fixedLegPeriods);
		legs.add(fixedLeg);

		final Collection<AbstractProductComponent> floatingLegPeriods = new ArrayList<>();
		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			final double periodLength = paymentDates[periodIndex]-fixingDates[periodIndex];
			final AbstractIndex index = new LIBORIndex(0.0,periodLength);
			//			AbstractIndex index = new ConstantMaturitySwaprate(5.0, periodLength);
			final Period period = new Period(fixingDates[periodIndex], paymentDates[periodIndex], fixingDates[periodIndex], paymentDates[periodIndex], notional, index, true, false, false);
			floatingLegPeriods.add(period);
		}
		final ProductCollection floatingLeg = new ProductCollection(floatingLegPeriods);
		legs.add(floatingLeg);

		underlying = new ProductCollection(legs);
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
		final RandomVariable	values	= 	underlying.getValue(evaluationTime, model);

		return values;
	}
}
