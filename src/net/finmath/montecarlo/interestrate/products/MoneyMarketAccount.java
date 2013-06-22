/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 14.06.2013
 */

package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the valuation of a money market account. The money market account
 * is characterized by its inception time <i>t<sub>0</sub></i> and its accrual period
 * <i>&Delta; t</i>. <br/>
 * 
 * With <i>t<sub>i</sub> := t<sub>0</sub> + i &Delta; t</i>
 * the money market account value <i>N(t<sub>0</sub>) = 1</i> and
 * <i>N(t) = N(t<sub>i</sub>) (1 + L(t<sub>i</sub>,t<sub>i+1</sub>;t<sub>i</sub>) (t - t<sub>i</sub>))</i>.
 * for <i>t<sub>i</sub> &lt; t &lt; t<sub>i+1</sub></i>. <br/>
 * 
 * The value of the account at inception is 1.0. The value of the account prior to inception is zero.
 * 
 * @author Christian Fries
 */
public class MoneyMarketAccount extends AbstractLIBORMonteCarloProduct {

	private double inceptionTime	= 0.0;
	private double accrualPeriod	= -1.0;		// Accrual period, if this period is &lt; 0, then the finest model libor period discretization is used

	/**
	 * Create a default money market account.
	 */
	public MoneyMarketAccount() {
	}

	/**
	 * Create a money market account.
	 * 
	 * @param inceptionTime The inception time. The value of the account at inception is 1.0. The value of the account prior to inception is zero.
	 * @param accrualPeriod The accrual period.
	 */
	public MoneyMarketAccount(double inceptionTime, double accrualPeriod) {
		super();
		this.inceptionTime = inceptionTime;
		this.accrualPeriod = accrualPeriod;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct#getValue(double, net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface)
	 */
	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		if(inceptionTime > evaluationTime) return new RandomVariable(0.0);
		
		// Initialize the value of the account to 1.0
		RandomVariableInterface value = new RandomVariable(1.0);

		// Loop over accrual periods
		for(double time=inceptionTime; time<evaluationTime; time += accrualPeriod) {	
			// Get the forward fixed at the beginning of the period
			RandomVariableInterface	forwardRate				= model.getLIBOR(time, time, time+accrualPeriod);
			double					currentAccrualPeriod	= Math.min(accrualPeriod , evaluationTime-time);
			
			// Accrue the value using the current forward rate
			value.accrue(forwardRate, currentAccrualPeriod);
		}
		
		return value;
	}
}
