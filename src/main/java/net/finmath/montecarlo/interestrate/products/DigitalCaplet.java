/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 06.04.2005
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Implements the valuation of a digital caplet using a given
 * <code>LIBORModelMonteCarloSimulationModel</code>.
 * The digital caplet pays periodLength if <i>L &gt; K</i> and else 0.
 * Here <i>L = L(T<sub>i</sub>,T<sub>i+1</sub>;t)</i> is the
 * forward rate with period start <i>T<sub>i</sub></i>
 * and period end <i>T<sub>i+1</sub></i> and fixing <i>t</i>.
 * <i>K</i> denotes the strike rate.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class DigitalCaplet extends AbstractTermStructureMonteCarloProduct {
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
	public DigitalCaplet(final double optionMaturity, final double periodStart,
			final double periodEnd, final double strike) {
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
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		// Set payment date and period length
		final double	paymentDate		= periodEnd;
		final double	periodLength	= periodEnd - periodStart;

		// Get random variables
		final RandomVariable	libor		= model.getForwardRate(optionMaturity, periodStart, periodEnd);

		final RandomVariable 		trigger		= libor.sub(strike);
		RandomVariable				values		= trigger.choose((new Scalar(periodLength)), (new Scalar(0.0)));

		// Get numeraire and probabilities for payment time
		final RandomVariable	numeraire					= model.getNumeraire(paymentDate);
		final RandomVariable	monteCarloProbabilities		= model.getMonteCarloWeights(paymentDate);

		values = values.div(numeraire).mult(monteCarloProbabilities);

		// Get numeraire and probabilities for evaluation time
		final RandomVariable	numeraireAtEvaluationTime					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtEvaluationTime		= model.getMonteCarloWeights(evaluationTime);

		values = values.mult(numeraireAtEvaluationTime).div(monteCarloProbabilitiesAtEvaluationTime);

		// Return values
		return values;
	}

	public double getOptionMaturity() {
		return optionMaturity;
	}

	public double getPeriodStart() {
		return periodStart;
	}

	public double getPeriodEnd() {
		return periodEnd;
	}

	public double getStrike() {
		return strike;
	}
}
