/*
 * Created on 09.03.2008
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the pricing of a digtal floorlet using a given <code>LIBORModelMonteCarloSimulationModel</code>.
 *
 * @author Christian Fries
 * @version 1.2
 */
public class DigitalFloorlet extends AbstractTermStructureMonteCarloProduct {
	private final double	maturity;
	private final double	strike;

	/**
	 * @param maturity The maturity given as double.
	 * @param strike The strike given as double.
	 */
	public DigitalFloorlet(final double maturity, final double strike) {
		super();
		this.maturity = maturity;
		this.strike   = strike;
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

		// This is on the Libor discretization
		final int		liborIndex		= ((LIBORModelMonteCarloSimulationModel) model).getLiborPeriodIndex(maturity);
		final double	paymentDate		= ((LIBORModelMonteCarloSimulationModel) model).getLiborPeriod(liborIndex+1);
		final double	periodLength	= paymentDate - maturity;

		// Get random variables
		final RandomVariable	libor						= model.getForwardRate(maturity, maturity, paymentDate);

		// Set up payoff on path
		final double[] payoff = new double[model.getNumberOfPaths()];
		for(int path=0; path<model.getNumberOfPaths(); path++)
		{
			final double liborOnPath = libor.get(path);

			if(liborOnPath < strike) {
				payoff[path] = periodLength;
			}
			else {
				payoff[path] = 0.0;
			}
		}

		// Get random variables
		final RandomVariable	numeraire					= model.getNumeraire(paymentDate);
		final RandomVariable	monteCarloProbabilities		= model.getMonteCarloWeights(paymentDate);

		final RandomVariable	numeraireAtEvaluationTime					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtEvaluationTime		= model.getMonteCarloWeights(evaluationTime);

		final RandomVariable values = new RandomVariableFromDoubleArray(paymentDate, payoff);
		values.div(numeraire).mult(monteCarloProbabilities);
		values.div(numeraireAtEvaluationTime).mult(monteCarloProbabilitiesAtEvaluationTime);

		// Return values
		return values;
	}
}
