/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 23.12.2016
 */

package net.finmath.montecarlo.crosscurrency;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Interface for cross currency term structure models.
 * 
 * @author Christian Fries
 */
public interface CrossCurrencyTermStructureModelMonteCarloSimulationInterface extends MonteCarloSimulationInterface {

	/**
	 * Return the forward rate for a given simulation time and a given period start and period end.
	 * 
	 * @param curve The identifier specifying the curve or currency.
	 * @param time          Simulation time
	 * @param periodStart   Start time of period
	 * @param periodEnd     End time of period
	 * @return 				The forward rate as a random variable as seen on simulation time for the specified period.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariableInterface getForwardRate(String curve, double time, double periodStart, double periodEnd) throws CalculationException;

	/**
	 * Return the (cross curve or currency) exchange rate for a given simulation time.
	 * 
	 * @param fromCurve The identifier specifying the curve or currency for the denominator.
	 * @param toCurve	The identifier specifying the curve or currency for the numerator.
	 * @param time		Simulation time
	 * @return 			The (cross curve or currency) exchange rate for a given simulation time.
	 */
	RandomVariableInterface getExchangeRate(String fromCurve, String toCurve, double time);
	
	/**
	 * Return the numeraire at a given time.
	 * 
	 * @param time Time at which the process should be observed
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariableInterface getNumeraire(double time) throws CalculationException;

	/**
	 * Returns the underlying model.
	 * 
	 * The model specifies the measure, the initial value, the drift, the factor loadings (covariance model), etc.
	 * 
	 * @return The underlying model
	 */
	AbstractModelInterface getModel();

	/**
	 * @return The implementation of the process
	 */
	AbstractProcessInterface getProcess();
}