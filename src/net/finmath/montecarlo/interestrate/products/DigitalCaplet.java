/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 06.04.2005
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the valuation of a digital caplet using a given
 * <code>LIBORModelMonteCarloSimulationInterface</code>.
 * The digital caplet pays periodLength if <i>L &gt; K</i> and else 0.
 * Here <i>L = L(T<sub>i</sub>,T<sub>i+1</sub>;t)</i> is the
 * forward rate with period start <i>T<sub>i</sub></i>
 * and period end <i>T<sub>i+1</sub></i> and fixing <i>t</i>.
 * <i>K</i> denotes the strike rate.
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class DigitalCaplet extends AbstractLIBORMonteCarloProduct {
	private final double	optionMaturity;
	private final double	periodStart;
	private final double	periodEnd;
	private final double	strike;
		
	/**
	 * Create a digital caplet with given maturity and strike.
	 * 
	 * @param optionMaturity The option maturity.
	 * @param periodStart The period start of the forward rate.
	 * @param periodEnd The period end of the forward rate.
	 * @param strike The strike rate.
	 */
	public DigitalCaplet(double optionMaturity, double periodStart,
			double periodEnd, double strike) {
		super();
		this.optionMaturity = optionMaturity;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.strike = strike;
	}

    /**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws net.finmath.exception.CalculationException
     */
    @Override
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// Set payment date and period length
		double	paymentDate		= periodEnd;
		double	periodLength	= periodEnd - periodStart;

        // Get random variables
		RandomVariableInterface	libor		= model.getLIBOR(optionMaturity, periodStart, periodEnd);

		RandomVariableInterface 			trigger		= libor.getMutableCopy().sub(strike).mult(periodLength);
		RandomVariableInterface				values		= (new RandomVariable(1.0)).barrier(trigger, (new RandomVariable(periodLength)), (new RandomVariable(0.0)));

        // Get numeraire and probabilities for payment time
		RandomVariableInterface	numeraire					= model.getNumeraire(paymentDate);
		RandomVariableInterface	monteCarloProbabilities		= model.getMonteCarloWeights(paymentDate);

		values = values.div(numeraire).mult(monteCarloProbabilities);
		
        // Get numeraire and probabilities for evaluation time
		RandomVariableInterface	numeraireAtEvaluationTime					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvaluationTime		= model.getMonteCarloWeights(evaluationTime);

		values = values.mult(numeraireAtEvaluationTime).div(monteCarloProbabilitiesAtEvaluationTime);		
		
		// Return values
		return values;
	}
}

