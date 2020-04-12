/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.04.2015
 */
package net.finmath.montecarlo.hybridassetinterestrate;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Basic interface which has to be implemented by Monte Carlo models for hybrid processes.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface HybridAssetMonteCarloSimulation extends MonteCarloSimulationModel {

	/**
	 * Return the (default) numeraire at a given time.
	 *
	 * @param time The time for which the numeraire is returned.
	 * @return The numeraire at a given time.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getNumeraire(double time) throws CalculationException;

	/**
	 * Return the numeraire associated with a given (collateral or funding) account at a given time.
	 *
	 * @param account The account associated with this numeraire.
	 * @param time The time for which the numeraire is returned.
	 * @return The numeraire at a given time.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getNumeraire(String account, double time) throws CalculationException;

	/**
	 * Return the random variable of a risk factor with a given name at a given observation time index.
	 *
	 * @param riskFactorIdentifyer The identifier of the risk factor.
	 * @param time The time at which the risk factor is observed.
	 * @return Random variable representing the corresponding risk factor.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getValue(RiskFactorID riskFactorIdentifyer, double time) throws CalculationException;
}
