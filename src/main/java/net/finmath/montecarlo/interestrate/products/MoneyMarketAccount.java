/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 14.06.2013
 */

package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the valuation of a money market account. The money market account
 * is characterized by its inception time <i>t<sub>0</sub></i> and its accrual period
 * <i>&Delta; t</i>. <br>
 *
 * With <i>t<sub>i</sub> := t<sub>0</sub> + i &Delta; t</i>
 * the money market account value <i>N(t<sub>0</sub>) = 1</i> and
 * <i>N(t) = N(t<sub>i</sub>) (1 + L(t<sub>i</sub>,t<sub>i+1</sub>;t<sub>i</sub>) (t - t<sub>i</sub>))</i>.
 * for <i>t<sub>i</sub> &lt; t &lt; t<sub>i+1</sub></i>. <br>
 *
 * The value of the account at inception is 1.0. The value of the account prior to inception is zero.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class MoneyMarketAccount extends AbstractLIBORMonteCarloProduct {

	private double inceptionTime	= 0.0;
	private double initialValue		= 1.0;
	private double accrualPeriod	= -1.0;		// Accrual period, if this period is &lt; 0, then the finest model LIBOR period discretization is used

	/**
	 * Create a money market account.
	 *
	 * @param inceptionTime The inception time. The value of the account at inception is 1.0. The value of the account prior to inception is zero.
	 * @param initialValue The initial value, i.e., the value at inception time.
	 * @param accrualPeriod The accrual period. If this period is &lt; 0, then the finest model LIBOR period discretization is used
	 */
	public MoneyMarketAccount(final double inceptionTime, final double initialValue, final double accrualPeriod) {
		super();
		this.inceptionTime	= inceptionTime;
		this.initialValue	= initialValue;
		this.accrualPeriod	= accrualPeriod;
	}

	/**
	 * Create a money market account.
	 *
	 * @param inceptionTime The inception time. The value of the account at inception is 1.0. The value of the account prior to inception is zero.
	 * @param accrualPeriod The accrual period. If this period is &lt; 0, then the finest model LIBOR period discretization is used
	 */
	public MoneyMarketAccount(final double inceptionTime, final double accrualPeriod) {
		this(inceptionTime, 1.0, accrualPeriod);
	}

	/**
	 * Create a default money market account. The money market account will use the
	 * models tenor discretization as the accrual period and its inception time is 0.
	 */
	public MoneyMarketAccount() {
		this(0.0, 1.0, -1.0);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct#getValue(double, net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel)
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		if(inceptionTime > evaluationTime) {
			return new RandomVariableFromDoubleArray(0.0);
		}
		if(accrualPeriod <= 0) {
			return new RandomVariableFromDoubleArray(Double.MAX_VALUE);
		}

		// Initialize the value of the account to 1.0
		RandomVariable value = new RandomVariableFromDoubleArray(initialValue);

		// Loop over accrual periods
		for(double time=inceptionTime; time<evaluationTime; time += accrualPeriod) {
			// Get the forward fixed at the beginning of the period
			final RandomVariable	forwardRate				= model.getForwardRate(time, time, time+accrualPeriod);
			final double					currentAccrualPeriod	= Math.min(accrualPeriod , evaluationTime-time);

			// Accrue the value using the current forward rate
			value = value.accrue(forwardRate, currentAccrualPeriod);
		}

		return value;
	}
}
