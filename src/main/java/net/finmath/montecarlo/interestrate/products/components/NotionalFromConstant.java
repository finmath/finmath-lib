/*
 * Created on 05.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.components;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * A constant (non-stochastic) notional.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class NotionalFromConstant implements Notional {

	private final String currency;
	private final RandomVariableFromDoubleArray notional;

	/**
	 * Creates a constant (non-stochastic) notional.
	 *
	 * @param notional The constant notional value.
	 * @param currency The currency.
	 */
	public NotionalFromConstant(double notional, String currency) {
		super();
		this.notional = new RandomVariableFromDoubleArray(0.0,notional);
		this.currency = currency;
	}

	/**
	 * Creates a constant (non-stochastic) notional.
	 *
	 * @param notional The constant notional value.
	 */
	public NotionalFromConstant(double notional) {
		this(notional, null);
	}

	@Override
	public String getCurrency() {
		return currency;
	}

	@Override
	public RandomVariable getNotionalAtPeriodEnd(AbstractPeriod period, LIBORModelMonteCarloSimulationModel model) {
		return notional;
	}

	@Override
	public RandomVariable getNotionalAtPeriodStart(AbstractPeriod period, LIBORModelMonteCarloSimulationModel model) {
		return notional;
	}

	@Override
	public String toString() {
		return "Notional [currency=" + currency + ", notional=" + notional
				+ "]";
	}
}
