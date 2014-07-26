/*
 * Created on 05.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 */
public interface AbstractNotional {

	
    /**
     * Calculates the notional at the start of a period, given a period.
     * Example: The notional can be independent of the period (constant running notional) or depending on the period (accruing notional).
     * 
     * @param period Period.
     * @param model The model against we are evaluation.
     * @return The notional for the given period as of period start.
     */
    public abstract RandomVariableInterface getNotionalAtPeriodStart(AbstractPeriod period, LIBORModelMonteCarloSimulationInterface model);

    /**
     * Calculates the notional at the end of a period, given a period.
     * Example: The notional can be independent of the period (constant running notional) or depending on the period (accruing notional).
     * 
     * @param period Period.
     * @param model The model against we are evaluation.
     * @return The notional for the given period as of period end.
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method. 
     */
    public abstract RandomVariableInterface getNotionalAtPeriodEnd(AbstractPeriod period, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;
}
