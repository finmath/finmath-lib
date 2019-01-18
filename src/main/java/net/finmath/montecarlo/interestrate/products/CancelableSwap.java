/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 11.03.2006
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Implements the pricing of a cancelable swap under a <code>LIBORModelMonteCarloSimulationInterface</code>
 *
 * @author Christian Fries
 * @version 1.2
 * @date 11.03.2006, 31.05.2009
 */
public class CancelableSwap extends AbstractLIBORMonteCarloProduct {

	private boolean[]	isPeriodStartDateExerciseDate;	// Exercise date
	private double[]	fixingDates;                  	// Vector of fixing dates (must be sorted)
	private double[]	periodLengths;					// Vector of period length;
	private double[]	paymentDates;	                // Vector of payment dates (same length as fixing dates)
	private double[]	periodNotionals;				// Vector of notionals for each period
	private double[]	swaprates;	                 	// Vector of strikes

	/**
	 * @param isPeriodStartDateExerciseDate If true, we may exercise at period start
	 * @param fixingDates Vector of fixing dates
	 * @param periodLength Vector of periodLength
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param periodNotionals Vector of period notionals.
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public CancelableSwap(boolean[] isPeriodStartDateExerciseDate, double[] fixingDates, double[] periodLength, double[] paymentDates, double[] periodNotionals, double[] swaprates) {
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
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// After the last period the product has value zero: Initialize values to zero.
		RandomVariable values = new RandomVariableFromDoubleArray(fixingDates[fixingDates.length-1], 0.0);

		// Loop backward over the swap periods
		for(int period=fixingDates.length-1; period>=0; period--)
		{
			double fixingDate	= fixingDates[period];
			double periodLength	= periodLengths[period];
			double paymentDate	= paymentDates[period];
			double notional		= periodNotionals[period];
			double swaprate		= swaprates[period];

			// Get random variables - note that this is the rate at simulation time = exerciseDate
			RandomVariable	libor					= model.getLIBOR(fixingDate, fixingDate, fixingDate+periodLength);
			RandomVariable	numeraire               = model.getNumeraire(paymentDate);
			RandomVariable	monteCarloProbabilities = model.getMonteCarloWeights(paymentDate);

			// foreach(path) values[path] += notional * (libor.get(path) - swaprate) * periodLength / numeraire.get(path) * monteCarloProbabilities.get(path);
			RandomVariable payoff = libor.sub(swaprate).mult(periodLength).mult(notional);

			// Apply discounting and Monte-Carlo probabilities
			payoff = payoff.div(numeraire).mult(monteCarloProbabilities);

			values = values.add(payoff);

			if(isPeriodStartDateExerciseDate[period]) {
				// Remove foresight through condition expectation
				ConditionalExpectationEstimator condExpEstimator = getConditionalExpectationEstimator(period, model);

				// Calculate conditional expectation. Note that no discounting (numeraire division) is required!
				RandomVariable valueIfExcercised         = condExpEstimator.getConditionalExpectation(values);

				// Apply the exercise criteria
				// foreach(path) if(valueIfExcercided.get(path) < 0.0) values[path] = 0.0;
				values = valueIfExcercised.choose(values, new Scalar(0.0));
			}
		}

		RandomVariable	numeraireAtZero						= model.getNumeraire(evaluationTime);
		RandomVariable	monteCarloProbabilitiesAtZero		= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		return values;
	}

	/**
	 * @param fixingDateIndex
	 * @param model
	 * @return
	 * @throws CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	private ConditionalExpectationEstimator getConditionalExpectationEstimator(int fixingDateIndex, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		ConditionalExpectationEstimator condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(fixingDates[fixingDateIndex], model));
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
	private RandomVariable[] getRegressionBasisFunctions(double fixingDate, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		// Constant
		// @TODO Use non differentiable
		RandomVariable basisFunction = new RandomVariableFromDoubleArray(1.0);//.getRandomVariableForConstant(1.0);
		basisFunctions.add(basisFunction);

		int fixingDateIndex = Arrays.binarySearch(fixingDates, fixingDate);
		if(fixingDateIndex < 0) {
			fixingDateIndex = -fixingDateIndex;
		}
		if(fixingDateIndex >= fixingDates.length) {
			fixingDateIndex = fixingDates.length-1;
		}

		// forward rate to the next period
		RandomVariable rateShort = model.getLIBOR(fixingDate, fixingDate, paymentDates[fixingDateIndex+1]);
		RandomVariable bondShort = rateShort.mult(paymentDates[fixingDateIndex+1]-fixingDate).add(1.0).invert();
		basisFunctions.add(bondShort);
		basisFunctions.add(bondShort.pow(2.0));
		basisFunctions.add(bondShort.pow(3.0));

		// forward rate to the end of the product
		RandomVariable rateLong = model.getLIBOR(fixingDate, fixingDates[fixingDateIndex], paymentDates[paymentDates.length-1]);
		RandomVariable bondLong = rateLong.mult(paymentDates[paymentDates.length-1]-fixingDates[fixingDateIndex]).add(1.0).invert();
		basisFunctions.add(bondLong);
		basisFunctions.add(bondLong.pow(2.0));
		basisFunctions.add(bondLong.pow(3.0));

		// Numeraire
		RandomVariable numeraire = model.getNumeraire(fixingDate);
		basisFunctions.add(numeraire);
		basisFunctions.add(numeraire.pow(2.0));
		basisFunctions.add(numeraire.pow(3.0));

		// Cross
		basisFunctions.add(rateLong.mult(numeraire));

		return basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
	}
}
