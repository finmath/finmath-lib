/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 11.03.2006
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.ConditionalExpectationEstimatorInterface;
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
		RandomVariableInterface values				= model.getRandomVariableForConstant(0.0);
		RandomVariableInterface valuesUnderlying	= model.getRandomVariableForConstant(0.0);

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
				ConditionalExpectationEstimatorInterface conditionalExpectationOperator = getConditionalExpectationEstimator(fixingDate, model);

				// Calculate conditional expectation. Note that no discounting (numeraire division) is required!
				RandomVariableInterface triggerValues         = triggerValuesDiscounted.getConditionalExpectation(conditionalExpectationOperator);

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
	 * @param fixingDate The condition time.
	 * @param model The model
	 * @return The conditional expectation estimator suitable for this product
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public ConditionalExpectationEstimatorInterface getConditionalExpectationEstimator(double fixingDate, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(
				getRegressionBasisFunctions(fixingDate, model)
				);
		return condExpEstimator;
	}

	/**
	 * Return the basis functions for the regression suitable for this product.
	 * 
	 * @param fixingDateIndex The time index corresponding to the fixing date
	 * @param model The model
	 * @return The basis functions for the regression suitable for this product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method. 
	 */
	private RandomVariableInterface[] getRegressionBasisFunctions(double fixingDate, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<RandomVariableInterface>();

		// Constant
		// @TODO Use non differentiable
		RandomVariableInterface basisFunction = new RandomVariable(1.0);//.getRandomVariableForConstant(1.0);
		basisFunctions.add(basisFunction);

		int fixingDateIndex = Arrays.binarySearch(fixingDates, fixingDate);
		if(fixingDateIndex < 0) fixingDateIndex = -fixingDateIndex;
		if(fixingDateIndex >= fixingDates.length) fixingDateIndex = fixingDates.length-1;

		// forward rate to the next period
		RandomVariableInterface rateShort = model.getLIBOR(fixingDate, fixingDate, paymentDates[fixingDateIndex]);
		basisFunctions.add(rateShort);
		basisFunctions.add(rateShort.pow(2.0));
		basisFunctions.add(rateShort.pow(3.0));

		// forward rate to the end of the product
		RandomVariableInterface rateLong = model.getLIBOR(fixingDate, fixingDates[fixingDateIndex], paymentDates[paymentDates.length-1]);
		basisFunctions.add(rateLong);
		basisFunctions.add(rateLong.pow(2.0));
		basisFunctions.add(rateLong.pow(3.0));

		// Numeraire
		RandomVariableInterface numeraire = model.getNumeraire(fixingDate);
		basisFunctions.add(numeraire);
		basisFunctions.add(numeraire.pow(2.0));
		basisFunctions.add(numeraire.pow(3.0));

		// Cross
		basisFunctions.add(rateLong.mult(numeraire));

		return basisFunctions.toArray(new RandomVariableInterface[basisFunctions.size()]);
	}
}
