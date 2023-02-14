/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the pricing of a Caplet using a given <code>TermStructureMonteCarloSimulationModel</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class Caplet extends AbstractTermStructureMonteCarloProduct {

	public enum ValueUnit {
		VALUE,
		/**
		 * @deprecated Use INTEGRATEDLOGNORMALVARIANCE
		 */
		INTEGRATEDVARIANCE,
		/**
		 * @deprecated Use LOGNORMALVOLATILITY
		 */
		VOLATILITY,
		INTEGRATEDLOGNORMALVARIANCE,
		LOGNORMALVOLATILITY,
		INTEGRATEDNORMALVARIANCE,
		NORMALVOLATILITY
	}

	private final double	maturity;
	private final double	periodLength;
	private final double	strike;
	private final double	daycountFraction;
	private final boolean	isFloorlet;
	private final ValueUnit					valueUnit;

	/**
	 * Create a caplet or a floorlet.
	 *
	 * A caplet pays \( max(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 *
	 * A floorlet pays \( -min(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 *
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period.
	 * @param strike The strike given as double.
	 * @param daycountFraction The daycount fraction used in the payout function.
	 * @param isFloorlet If true, this object will represent a floorlet, otherwise a caplet.
	 * @param valueUnit The unit of the value returned by the <code>getValue</code> method.
	 */
	public Caplet(final double maturity, final double periodLength, final double strike, final double daycountFraction, final boolean isFloorlet, final ValueUnit valueUnit) {
		super();
		this.maturity = maturity;
		this.periodLength = periodLength;
		this.strike = strike;
		this.daycountFraction = daycountFraction;
		this.isFloorlet = isFloorlet;
		this.valueUnit = valueUnit;
	}

	/**
	 * Create a caplet or a floorlet.
	 *
	 * A caplet pays \( max(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 *
	 * A floorlet pays \( -min(L-K,0) * daycountFraction \) at maturity+periodLength
	 * where L is fixed at maturity.
	 *
	 * This simplified constructor uses daycountFraction = periodLength.
	 *
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period in ACT/365 convention.
	 * @param strike The strike given as double.
	 * @param isFloorlet If true, this object will represent a floorlet, otherwise a caplet.
	 */
	public Caplet(final double maturity, final double periodLength, final double strike, final boolean isFloorlet) {
		this(maturity, periodLength, strike, periodLength, isFloorlet, ValueUnit.VALUE);
	}

	/**
	 * Create a caplet.
	 *
	 * A caplet pays \( max(L-K,0) * periodLength \) at maturity+periodLength
	 * where L is fixed at maturity.
	 *
	 * @param maturity The fixing date given as double. The payment is at the period end.
	 * @param periodLength The length of the forward rate period.
	 * @param strike The strike given as double.
	 */
	public Caplet(final double maturity, final double periodLength, final double strike) {
		this(maturity, periodLength, strike, false);
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
		// This is on the LIBOR discretization
		final double	paymentDate	= maturity+periodLength;

		// Get random variables
		final RandomVariable	forwardRate				= model.getForwardRate(maturity, maturity, maturity+periodLength);
		final RandomVariable	numeraire				= model.getNumeraire(paymentDate);
		final RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(paymentDate);

		/*
		 * Calculate the payoff, which is
		 *    max(L-K,0) * periodLength         for caplet or
		 *   -min(L-K,0) * periodLength         for floorlet.
		 */
		RandomVariable values = forwardRate;
		if(!isFloorlet) {
			values = values.sub(strike).floor(0.0).mult(daycountFraction);
		} else {
			values = values.sub(strike).cap(0.0).mult(-1.0 * daycountFraction);
		}

		values = values.div(numeraire).mult(monteCarloProbabilities);

		final RandomVariable	numeraireAtValuationTime				= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtValuationTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime);

		if(valueUnit == ValueUnit.VALUE) {
			return values;
		}
		else if(valueUnit == ValueUnit.LOGNORMALVOLATILITY || valueUnit == ValueUnit.VOLATILITY) {
			/*
			 * This calculation makes sense only if the value is an unconditional one.
			 */
			final double discountFactor = monteCarloProbabilities.div(numeraire).mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime).getAverage();
			final double forward = forwardRate.div(numeraire).mult(monteCarloProbabilities).mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime).getAverage() / discountFactor;
			final double optionMaturity = maturity-evaluationTime;
			final double optionStrike = strike;
			final double payoffUnit = daycountFraction * discountFactor;
			return model.getRandomVariableForConstant(AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, values.getAverage()));
		}
		else if(valueUnit == ValueUnit.NORMALVOLATILITY) {
			/*
			 * This calculation makes sense only if the value is an unconditional one.
			 */
			final double discountFactor = monteCarloProbabilities.div(numeraire).mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime).getAverage();
			final double forward = forwardRate.div(numeraire).mult(monteCarloProbabilities).mult(numeraireAtValuationTime).div(monteCarloProbabilitiesAtValuationTime).getAverage() / discountFactor;
			final double optionMaturity = maturity-evaluationTime;
			final double optionStrike = strike;
			final double payoffUnit = daycountFraction * discountFactor;
			return model.getRandomVariableForConstant(AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, values.getAverage()));
		}
		else {
			throw new IllegalArgumentException("Value unit " + valueUnit + " unsupported.");
		}
	}
}
