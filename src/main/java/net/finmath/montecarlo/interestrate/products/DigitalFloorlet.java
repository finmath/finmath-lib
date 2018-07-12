/*
 * Created on 09.03.2008
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the pricing of a digtal floorlet using a given <code>LIBORModelMonteCarloSimulationInterface</code>.
 * 
 * @author Christian Fries
 * @version 1.2
 */
public class DigitalFloorlet extends AbstractLIBORMonteCarloProduct {
	private double	maturity;
	private double	strike;

	/**
	 * @param maturity The maturity given as double.
	 * @param strike The strike given as double.
	 */
	public DigitalFloorlet(double maturity, double strike) {
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
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        

		// This is on the Libor discretization
		int		liborIndex		= model.getLiborPeriodIndex(maturity);
		double	paymentDate		= model.getLiborPeriod(liborIndex+1);
		double	periodLength	= paymentDate - maturity;

		// Get random variables
		RandomVariableInterface	libor						= model.getLIBOR(maturity, maturity, paymentDate);

		// Set up payoff on path
		double[] payoff = new double[model.getNumberOfPaths()];
		for(int path=0; path<model.getNumberOfPaths(); path++)
		{
			double liborOnPath = libor.get(path);

			if(liborOnPath < strike) {
				payoff[path] = periodLength;
			}
			else {
				payoff[path] = 0.0;
			}
		}

		// Get random variables
		RandomVariableInterface	numeraire					= model.getNumeraire(paymentDate);
		RandomVariableInterface	monteCarloProbabilities		= model.getMonteCarloWeights(paymentDate);

		RandomVariableInterface	numeraireAtEvaluationTime					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvaluationTime		= model.getMonteCarloWeights(evaluationTime);

		RandomVariableInterface values = new RandomVariable(paymentDate, payoff);
		values.div(numeraire).mult(monteCarloProbabilities);
		values.div(numeraireAtEvaluationTime).mult(monteCarloProbabilitiesAtEvaluationTime);		

		// Return values
		return values;
	}
}

