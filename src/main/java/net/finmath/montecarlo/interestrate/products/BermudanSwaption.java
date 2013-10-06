/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 11.03.2006
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectation;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the valuation of a cancelable swap under a <code>LIBORModelMonteCarloSimulationInterface</code>
 * 
 * @author Christian Fries
 * @version 1.2
 * @date 06.12.2009
 */
public class BermudanSwaption extends AbstractLIBORMonteCarloProduct {

	private final boolean[]	isPeriodStartDateExerciseDate;	// Exercise date
	private final double[]	fixingDates;                  	// Vector of fixing dates (must be sorted)
	private final double[]	periodLengths;					// Vector of period length;
	private final double[]	paymentDates;	                // Vector of payment dates (same length as fixing dates)
	private final double[]	periodNotionals;				// Vector of notionals for each period
	private final double[]	swaprates;	                 	// Vector of strikes

	/**
	 * @param isPeriodStartDateExerciseDate If true, we may exercise at period start
	 * @param fixingDates Vector of fixing dates
	 * @param periodLength Period lengths (must have same length as fixing dates)
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param periodNotionals Period notionals (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public BermudanSwaption(boolean[] isPeriodStartDateExerciseDate, double[] fixingDates, double[] periodLength, double[] paymentDates, double[] periodNotionals, double[] swaprates) {
		super();
		this.isPeriodStartDateExerciseDate = isPeriodStartDateExerciseDate;
		this.fixingDates = fixingDates;
		this.periodLengths = periodLength;
		this.paymentDates = paymentDates;
		this.periodNotionals = periodNotionals;
		this.swaprates = swaprates;
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

		// After the last period the product has value zero: Initialize values to zero.
		RandomVariableInterface values				= new RandomVariable(fixingDates[fixingDates.length-1], 0.0);
		RandomVariableInterface valuesUnderlying	= new RandomVariable(fixingDates[fixingDates.length-1], 0.0);

		// Loop backward over the swap periods
		for(int period=fixingDates.length-1; period>=0; period--)
		{
			double fixingDate	= fixingDates[period];
			double periodLength	= periodLengths[period];
			double paymentDate	= paymentDates[period];
			double notional		= periodNotionals[period];
			double swaprate		= swaprates[period];

			// Get random variables - note that this is the rate at simulation time = exerciseDate
			RandomVariableInterface	libor					= model.getLIBOR(fixingDate, fixingDate, fixingDate+periodLength);

			// foreach(path) values[path] += notional * (libor.get(path) - swaprate) * periodLength / numeraire.get(path) * monteCarloProbabilities.get(path);
			RandomVariableInterface payoff = libor.sub(swaprate).mult(periodLength).mult(notional);

			// Apply discounting and Monte-Carlo probabilities
			RandomVariableInterface	numeraire               = model.getNumeraire(paymentDate);
			RandomVariableInterface	monteCarloProbabilities = model.getMonteCarloWeights(paymentDate);
            payoff = payoff.div(numeraire).mult(monteCarloProbabilities);
			
//			model.discount(paymentDate, values);
            valuesUnderlying = valuesUnderlying.add(payoff);

			if(isPeriodStartDateExerciseDate[period]) {
				RandomVariableInterface triggerValuesDiscounted = values.sub(valuesUnderlying);

				// Remove foresight through condition expectation
		    	MonteCarloConditionalExpectation condExpEstimator = getConditionalExpectationEstimator(period, model);
		    	
				// Calculate conditional expectation. Note that no discounting (numeraire division) is required!
				RandomVariableInterface triggerValues         = condExpEstimator.getConditionalExpectation(triggerValuesDiscounted);

				// Apply the exercise criteria
				// foreach(path) if(valueIfExcercided.get(path) < 0.0) values[path] = 0.0;
                values = values.barrier(triggerValues, values, valuesUnderlying);
			}
		}

//		model.discount(evaluationTime, values);

        // Note that values is a relative price - no numeraire division is required
        RandomVariableInterface	numeraireAtZero					= model.getNumeraire(evaluationTime);
        RandomVariableInterface	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
        values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

        return values;
	}

	/**
	 * Return the conditional expectation estimator suitable for this product.
	 * 
     * @param fixingDateIndex The time index corresponding to the fixing date
     * @param model The model
     * @return The conditional expectation estimator suitable for this product
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    private MonteCarloConditionalExpectation getConditionalExpectationEstimator(int fixingDateIndex, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
    	MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(
    			getRegressionBasisFunctions(fixingDateIndex, model)
    			);
		return condExpEstimator;
    }

	/**
	 * Return the basis functions for the regression suitable for this product.
	 * 
     * @param fixingDateIndex The time index corresponding to the fixing date
     * @param model The model
	 * @return The basis functions for the regression suitable for this product.
	 * @throws CalculationException 
	 */
	private RandomVariableInterface[] getRegressionBasisFunctions(int fixingDateIndex, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		double fixingDate   = fixingDates[fixingDateIndex];

		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<RandomVariableInterface>();

		// Constant
		RandomVariableInterface basisFunction = new RandomVariable(fixingDate, 1.0);
		basisFunctions.add(basisFunction);

		// LIBORs
		RandomVariableInterface rate = model.getLIBOR(fixingDate, fixingDates[0], paymentDates[paymentDates.length-1]);
		double periodLength = paymentDates[paymentDates.length-1]-fixingDates[0];
		basisFunction = basisFunctions.get(0).discount(rate, periodLength);
		basisFunctions.add(basisFunction);
		basisFunction = basisFunctions.get(1).discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		return basisFunctions.toArray(new RandomVariableInterface[basisFunctions.size()]);
	}
}
