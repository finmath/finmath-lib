/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 11.03.2006
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.conditionalexpectation.RegressionBasisFunctionsProvider;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Implements the valuation of a Bermudan swaption under a <code>LIBORModelMonteCarloSimulationModel</code>
 *
 * @author Christian Fries
 * @version 1.3
 * @date 06.12.2009
 */
public class BermudanSwaption extends AbstractLIBORMonteCarloProduct implements RegressionBasisFunctionsProvider {

	private final boolean[]	isPeriodStartDateExerciseDate;	// Exercise date
	private final double[]	fixingDates;                  	// Vector of fixing dates (must be sorted)
	private final double[]	periodLengths;					// Vector of period length;
	private final double[]	paymentDates;	                // Vector of payment dates (same length as fixing dates)
	private final double[]	periodNotionals;				// Vector of notionals for each period
	private final double[]	swaprates;	                 	// Vector of strikes
	private final boolean	isCallable;						// If true: the right to enter into a swap, else the right to terminate a swap.

	private final RegressionBasisFunctionsProvider	regressionBasisFunctionsProvider;

	/**
	 * @param isPeriodStartDateExerciseDate If true, we may exercise at period start
	 * @param fixingDates Vector of fixing dates
	 * @param periodLength Period lengths (must have same length as fixing dates)
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param periodNotionals Period notionals (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param isCallable If true, the product represent the Bermudan right to enter into a swap. If false the product represents the Bermudan right to terminate a running swap.
	 * @param regressionBasisFunctionsProvider Used to determine the regression basis functions for the conditional expectation operator.
	 */
	public BermudanSwaption(final boolean[] isPeriodStartDateExerciseDate, final double[] fixingDates, final double[] periodLength, final double[] paymentDates, final double[] periodNotionals, final double[] swaprates, final boolean isCallable, final RegressionBasisFunctionsProvider	regressionBasisFunctionsProvider) {
		super();
		this.isPeriodStartDateExerciseDate = isPeriodStartDateExerciseDate;
		this.fixingDates = fixingDates;
		periodLengths = periodLength;
		this.paymentDates = paymentDates;
		this.periodNotionals = periodNotionals;
		this.swaprates = swaprates;
		this.isCallable = isCallable;
		this.regressionBasisFunctionsProvider = regressionBasisFunctionsProvider;
	}

	/**
	 * @param isPeriodStartDateExerciseDate If true, we may exercise at period start
	 * @param fixingDates Vector of fixing dates
	 * @param periodLength Period lengths (must have same length as fixing dates)
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param periodNotionals Period notionals (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 * @param isCallable If true, the product represent the Bemrudan right to enter into a swap. If false the product represents the Bermudan right to terminate a running swap.
	 */
	public BermudanSwaption(final boolean[] isPeriodStartDateExerciseDate, final double[] fixingDates, final double[] periodLength, final double[] paymentDates, final double[] periodNotionals, final double[] swaprates, final boolean isCallable) {
		this(isPeriodStartDateExerciseDate, fixingDates, periodLength, paymentDates, periodNotionals, swaprates, isCallable, null);
	}

	/**
	 * @param isPeriodStartDateExerciseDate If true, we may exercise at period start
	 * @param fixingDates Vector of fixing dates
	 * @param periodLength Period lengths (must have same length as fixing dates)
	 * @param paymentDates Vector of payment dates (must have same length as fixing dates)
	 * @param periodNotionals Period notionals (must have same length as fixing dates)
	 * @param swaprates Vector of strikes (must have same length as fixing dates)
	 */
	public BermudanSwaption(final boolean[] isPeriodStartDateExerciseDate, final double[] fixingDates, final double[] periodLength, final double[] paymentDates, final double[] periodNotionals, final double[] swaprates) {
		this(isPeriodStartDateExerciseDate, fixingDates, periodLength, paymentDates, periodNotionals, swaprates, true /* isCallable */);
	}

	@Override
	public Map<String, Object> getValues(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		// After the last period the product has value zero: Initialize values to zero.
		RandomVariable values				= model.getRandomVariableForConstant(0.0);
		RandomVariable valuesUnderlying	= model.getRandomVariableForConstant(0.0);
		RandomVariable	exerciseTime	    = model.getRandomVariableForConstant(Double.POSITIVE_INFINITY);

		// Loop backward over the swap periods
		for(int period=fixingDates.length-1; period>=0; period--)
		{
			final double fixingDate	= fixingDates[period];
			final double exerciseDate = fixingDate;
			final double periodLength	= periodLengths[period];
			final double paymentDate	= paymentDates[period];
			final double notional		= periodNotionals[period];
			final double swaprate		= swaprates[period];

			// Get random variables - note that this is the rate at simulation time = exerciseDate
			final RandomVariable	libor					= model.getForwardRate(fixingDate, fixingDate, fixingDate+periodLength);

			// foreach(path) values[path] += notional * (libor.get(path) - swaprate) * periodLength / numeraire.get(path) * monteCarloProbabilities.get(path);
			RandomVariable payoff = libor.sub(swaprate).mult(periodLength).mult(notional);

			// Apply discounting and Monte-Carlo probabilities
			final RandomVariable	numeraire               = model.getNumeraire(paymentDate);
			final RandomVariable	monteCarloProbabilities = model.getMonteCarloWeights(paymentDate);
			payoff = payoff.div(numeraire).mult(monteCarloProbabilities);

			//			model.discount(paymentDate, values);
			if(isCallable) {
				valuesUnderlying = valuesUnderlying.add(payoff);
			}
			else
			{
				values = values.add(payoff); // cancelable
			}

			if(isPeriodStartDateExerciseDate[period]) {
				final RandomVariable triggerValuesDiscounted = values.sub(valuesUnderlying);

				// Remove foresight through condition expectation
				final ConditionalExpectationEstimator conditionalExpectationOperator = getConditionalExpectationEstimator(fixingDate, model);

				// Calculate conditional expectation. Note that no discounting (numeraire division) is required!
				final RandomVariable triggerValues         = triggerValuesDiscounted.getConditionalExpectation(conditionalExpectationOperator);

				// Apply the exercise criteria
				// foreach(path) if(valueIfExcercided.get(path) < 0.0) values[path] = 0.0;
				values = triggerValues.choose(values, valuesUnderlying);

				exerciseTime = triggerValues.choose(exerciseTime, new Scalar(exerciseDate));
			}
		}

		//		model.discount(evaluationTime, values);

		// Note that values is a relative price - no numeraire division is required
		final RandomVariable	numeraireAtZero					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		final Map<String, Object> results = new HashMap<>();
		results.put("value", values);
		results.put("error", values.getStandardError());
		results.put("exerciseTime", exerciseTime);
		return results;
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
		return (RandomVariable) getValues(evaluationTime, model).get("value");
	}

	/**
	 * Return the conditional expectation estimator suitable for this product.
	 *
	 * @param fixingDate The condition time.
	 * @param model The model
	 * @return The conditional expectation estimator suitable for this product
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(final double fixingDate, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		final RandomVariable[] regressionBasisFunctions			= regressionBasisFunctionsProvider != null ? regressionBasisFunctionsProvider.getBasisFunctions(fixingDate, model) : getBasisFunctions(fixingDate, model);
		final MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(regressionBasisFunctions);

		return condExpEstimator;
	}

	/**
	 * Return the basis functions for the regression suitable for this product.
	 *
	 * @param fixingDate The condition time.
	 * @param model The model
	 * @return The basis functions for the regression suitable for this product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable[] getBasisFunctions(final double fixingDate, final MonteCarloSimulationModel model) throws CalculationException {
		if(model instanceof LIBORModelMonteCarloSimulationModel) {
			return getBasisFunctions(fixingDate, (LIBORModelMonteCarloSimulationModel)model);
		}
		else {
			throw new IllegalArgumentException("Requires model to implement LIBORModelMonteCarloSimulationModel.");
		}
	}

	/**
	 * Return the basis functions for the regression suitable for this product.
	 *
	 * @param fixingDate The condition time.
	 * @param model The model
	 * @return The basis functions for the regression suitable for this product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public RandomVariable[] getBasisFunctions(final double fixingDate, final LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		// Constant
		final RandomVariable basisFunction = new RandomVariableFromDoubleArray(1.0);//.getRandomVariableForConstant(1.0);
		basisFunctions.add(basisFunction);

		int fixingDateIndex = Arrays.binarySearch(fixingDates, fixingDate);
		if(fixingDateIndex < 0) {
			fixingDateIndex = -fixingDateIndex;
		}
		if(fixingDateIndex >= fixingDates.length) {
			fixingDateIndex = fixingDates.length-1;
		}

		// forward rate to the next period
		final RandomVariable rateShort = model.getForwardRate(fixingDate, fixingDate, paymentDates[fixingDateIndex]);
		final RandomVariable discountShort = rateShort.mult(paymentDates[fixingDateIndex]-fixingDate).add(1.0).invert();
		basisFunctions.add(discountShort);
		basisFunctions.add(discountShort.pow(2.0));
		//		basisFunctions.add(rateShort.pow(3.0));

		// forward rate to the end of the product
		final RandomVariable rateLong = model.getForwardRate(fixingDate, fixingDates[fixingDateIndex], paymentDates[paymentDates.length-1]);
		final RandomVariable discountLong = rateLong.mult(paymentDates[paymentDates.length-1]-fixingDates[fixingDateIndex]).add(1.0).invert();
		basisFunctions.add(discountLong);
		basisFunctions.add(discountLong.pow(2.0));
		//		basisFunctions.add(rateLong.pow(3.0));

		// Numeraire
		final RandomVariable numeraire = model.getNumeraire(fixingDate).invert();
		basisFunctions.add(numeraire);
		//		basisFunctions.add(numeraire.pow(2.0));
		//		basisFunctions.add(numeraire.pow(3.0));

		return basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
	}

	public double[] getExerciseTimes(){
		final ArrayList<Double> exerciseTimes = new ArrayList<>();
		for(int i=0;i<isPeriodStartDateExerciseDate.length;i++) {
			if(isPeriodStartDateExerciseDate[i]){
				exerciseTimes.add(fixingDates[i]);
			}
		}

		// Convert to primitive
		final double[] times = new double[exerciseTimes.size()];
		for(int i=0;i<times.length;i++) {
			times[i]=exerciseTimes.get(i).doubleValue();
		}
		return times;
	}

	public double[] getFixingDates(final double evaluationTime){ // return the remaining fixing dates after evaluationTime
		final ArrayList<Double> remainingFixingTimes = new ArrayList<>();
		for(int i=0;i<fixingDates.length;i++) {
			if(fixingDates[i]>=evaluationTime){
				remainingFixingTimes.add(fixingDates[i]);
			}
		}
		// Convert to primitive
		final double[] times = new double[remainingFixingTimes.size()];
		for(int i=0;i<times.length;i++) {
			times[i]=remainingFixingTimes.get(i).doubleValue();
		}
		return times;
	}

	public SimpleSwap getSwap() {
		return new SimpleSwap(fixingDates, paymentDates, swaprates, true, periodNotionals);
	}

	public double[] getPaymentDates(){
		return paymentDates;
	}

	public double[] getPeriodNotionals(){
		return periodNotionals;
	}

	public double[] getSwapRates(){
		return swaprates;
	}

	public double[] getPeriodLengths(){
		return periodLengths;
	}

	public double getFinalMaturity() {
		return paymentDates[paymentDates.length-1];
	}

	public boolean getIsCallable(){
		return isCallable;
	}
}

