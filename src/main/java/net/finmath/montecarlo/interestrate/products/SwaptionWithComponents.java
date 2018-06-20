/*
 * Created on 06.12.2009
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Collection;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.components.AbstractNotional;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.montecarlo.interestrate.products.components.Notional;
import net.finmath.montecarlo.interestrate.products.components.Option;
import net.finmath.montecarlo.interestrate.products.components.Period;
import net.finmath.montecarlo.interestrate.products.components.ProductCollection;
import net.finmath.montecarlo.interestrate.products.indices.FixedCoupon;
import net.finmath.montecarlo.interestrate.products.indices.LIBORIndex;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the pricing of a swap under a AbstractLIBORMarketModel
 * 
 * @author Christian Fries
 * @version 1.2
 */
public class SwaptionWithComponents extends AbstractLIBORMonteCarloProduct {

	private AbstractLIBORMonteCarloProduct option;
	
	/**
	 * @param exerciseDate The exercise date
	 * @param fixingDates Vector of fixing dates
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public SwaptionWithComponents(
		double exerciseDate,
		double[] fixingDates,
		double[] paymentDates,
		double[] swaprates) {
		super();
		
		/*
		 * Create components.
		 * 
		 * The interesting part here is, that the creation of the components implicitly
		 * constitutes the (traditional) pricing algorithms (e.g., loop over all periods).
		 * Hence, the definition of the product is the definition of the pricing algorithm.
		 */

		Collection<AbstractProductComponent> legs = new ArrayList<AbstractProductComponent>();

		AbstractNotional notional = new Notional(1.0);

		Collection<AbstractProductComponent> fixedLegPeriods = new ArrayList<AbstractProductComponent>();
		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			double daycountFraction = paymentDates[periodIndex]-fixingDates[periodIndex];
			Period period = new Period(fixingDates[periodIndex], paymentDates[periodIndex], fixingDates[periodIndex],
					paymentDates[periodIndex], notional, new FixedCoupon(swaprates[periodIndex]), daycountFraction,
					true, true, true, false);
			fixedLegPeriods.add(period);
		}
		ProductCollection fixedLeg = new ProductCollection(fixedLegPeriods);
		legs.add(fixedLeg);
		
		Collection<AbstractProductComponent> floatingLegPeriods = new ArrayList<AbstractProductComponent>();
		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			double daycountFraction = paymentDates[periodIndex]-fixingDates[periodIndex];
			double periodLength = paymentDates[periodIndex]-fixingDates[periodIndex];
			Period period = new Period(fixingDates[periodIndex], paymentDates[periodIndex], fixingDates[periodIndex],
					paymentDates[periodIndex], notional, new LIBORIndex(0.0,periodLength), daycountFraction,
					true, true, false, false);
			floatingLegPeriods.add(period);
		}
		ProductCollection floatingLeg = new ProductCollection(floatingLegPeriods);
		legs.add(floatingLeg);

		ProductCollection underlying = new ProductCollection(legs);
		option = new Option(exerciseDate, underlying);
	}
	
    /**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws CalculationException Thrown when valuation fails from valuation model.
     */
    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        
    	RandomVariableInterface	values	= 	option.getValue(evaluationTime, model);

        return values;
	}
}
