/*
 * Created on 05.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * A stochastic notional derived from the valuation of a component.
 *
 * @author Christian Fries
 * @since
 * @version 1.0
 */
public class NotionalFromComponent implements AbstractNotional {

	private AbstractProductComponent notional;

	/**
	 * Creates a notional which is derived by calling the getValue method on the period start of a given component.
	 *
	 * Note: The class performs a check of the measurability of the notional. If the notional is not \( F_{t} \)-measurable (for t = period start),
	 * an exception is thrown.
	 *
	 * @param notional The component providing the notation.
	 */
	public NotionalFromComponent(AbstractProductComponent notional) {
		super();
		this.notional = notional;
	}

	@Override
	public String getCurrency() {
		return notional.getCurrency();
	}

	@Override
	public RandomVariable getNotionalAtPeriodEnd(AbstractPeriod period, LIBORModelMonteCarloSimulationInterface model) {
		return this.getNotionalAtPeriodStart(period, model);
	}

	@Override
	public RandomVariable getNotionalAtPeriodStart(AbstractPeriod period, LIBORModelMonteCarloSimulationInterface model) {
		RandomVariable notionalValue = null;
		try {
			notionalValue = notional.getValue(period.getPeriodStart(), model);
		} catch (CalculationException e) {
			throw new IllegalArgumentException(e);
		}

		if(notionalValue.getFiltrationTime() > period.getPeriodStart()) {
			throw new IllegalArgumentException("Notional request at " + period.getPeriodStart() + "reports measuabiblity for time " + notionalValue.getFiltrationTime());
		}

		return notionalValue;
	}
}
