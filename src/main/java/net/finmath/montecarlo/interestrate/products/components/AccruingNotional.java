/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.01.2016
 */

package net.finmath.montecarlo.interestrate.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class AccruingNotional implements AbstractNotional {

	AbstractNotional	previousPeriodNotional;
	AbstractPeriod		previousPeriod;

	/**
	 * Creates a notion where the notional of the period start is calculated as
	 * the notional of the previous period's period end and the notional at period end
	 * is calculated as being accrued via getCoupon on the current period.
	 *
	 * @param previousPeriodNotional The notional of the previous period.
	 * @param previousPeriod The previous period.
	 */
	public AccruingNotional(AbstractNotional previousPeriodNotional, AbstractPeriod previousPeriod) {
		this.previousPeriodNotional = previousPeriodNotional;
		this.previousPeriod = previousPeriod;
	}

	@Override
	public String getCurrency() {
		return previousPeriodNotional.getCurrency();
	}

	@Override
	public RandomVariableInterface getNotionalAtPeriodStart(AbstractPeriod period, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		double productToModelTimeOffset = 0;
		try {
			if(previousPeriod.getReferenceDate() != null) {
				productToModelTimeOffset = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate(), previousPeriod.getReferenceDate());
			}
		}
		catch(UnsupportedOperationException e) {};

		RandomVariableInterface previousPeriodCoupon = previousPeriod.getCoupon(productToModelTimeOffset + previousPeriod.getFixingDate(), model);
		
		return previousPeriodNotional.getNotionalAtPeriodEnd(previousPeriod, model).mult(previousPeriodCoupon.add(1.0));
	}

	@Override
	public RandomVariableInterface getNotionalAtPeriodEnd(AbstractPeriod period, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		return getNotionalAtPeriodStart(period, model);
	}
}
