/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;

import net.finmath.concurrency.FutureWrapper;
import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;

/**
 * An right to choose between two underlyings.
 * Provides the function underlying1(exerciseDate) &gt; underlying2(exerciseDate) ? underlying1 : underlying2.
 *
 * @author Christian Fries
 * @version 1.1
 * @since finmath-lib 4.1.0
 */
public class Choice extends AbstractProductComponent {

	private static final long serialVersionUID = 3211126102506873636L;

	private final double exerciseDate;
	private final TermStructureMonteCarloProduct underlying1;
	private final TermStructureMonteCarloProduct underlying2;


	/**
	 * Creates the function underlying1(exerciseDate) &gt; underlying2(exerciseDate) ? underlying1 : underlying2.
	 *
	 * @param exerciseDate The exercise date at which the option is exercised.
	 * @param underlying1 The first underlying to choose of.
	 * @param underlying2 The second underlying to choose of.
	 */
	public Choice(final double exerciseDate,
			final TermStructureMonteCarloProduct underlying1,
			final TermStructureMonteCarloProduct underlying2) {
		super();
		this.exerciseDate = exerciseDate;
		this.underlying1 = underlying1;
		this.underlying2 = underlying2;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = null;
		for(final TermStructureMonteCarloProduct product : new TermStructureMonteCarloProduct[] {underlying1, underlying2}) {
			if(product instanceof AbstractProductComponent) {
				final Set<String> productUnderlyingNames = ((AbstractProductComponent)product).queryUnderlyings();
				if(productUnderlyingNames != null) {
					if(underlyingNames == null) {
						underlyingNames = productUnderlyingNames;
					} else {
						underlyingNames.addAll(productUnderlyingNames);
					}
				} else {
					throw new IllegalArgumentException("Underlying cannot be queried for underlyings.");
				}
			}
		}
		return underlyingNames;
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

		// >=?
		if(evaluationTime > exerciseDate) {
			return new RandomVariableFromDoubleArray(0.0);
		}

		// Parallel calculation of the underlyings
		Future<RandomVariable> valueUnderlying1Future;
		try {
			valueUnderlying1Future = getExecutor().submit(
					new Callable<RandomVariable>() {
						@Override
						public RandomVariable call() throws CalculationException {
							return underlying1.getValue(exerciseDate, model);
						}
					}
					);
		}
		catch(final RejectedExecutionException e) {
			valueUnderlying1Future = new FutureWrapper<>(underlying1.getValue(exerciseDate, model));
		}


		Future<RandomVariable> valueUnderlying2Future;
		try {
			valueUnderlying2Future = getExecutor().submit(
					new Callable<RandomVariable>() {
						@Override
						public RandomVariable call() throws CalculationException {
							return underlying2.getValue(exerciseDate, model);
						}
					}
					);
		}
		catch(final RejectedExecutionException e) {
			valueUnderlying2Future = new FutureWrapper<>(underlying2.getValue(exerciseDate, model));
		}


		// Syncronize
		RandomVariable valueUnderlying1 = null;
		RandomVariable valueUnderlying2 = null;
		try {
			valueUnderlying1 = valueUnderlying1Future.get();
			valueUnderlying2 = valueUnderlying2Future.get();
		} catch (final InterruptedException | ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Trigger index
		final RandomVariable triggerValues = valueUnderlying1.sub(valueUnderlying2);

		// Remove foresight through conditional expectation
		final MonteCarloConditionalExpectationRegression conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(exerciseDate, (LIBORModelMonteCarloSimulationModel) model));

		// Calculate cond. expectation. Note that no discounting (numeraire division) is required!
		final RandomVariable triggerExpectedValues = triggerValues.getConditionalExpectation(conditionalExpectationOperator);

		// Apply exercise criteria
		final RandomVariable values = triggerExpectedValues.choose(valueUnderlying1, valueUnderlying2);

		// Dicount to evaluation time
		if(evaluationTime != exerciseDate) {
			final RandomVariable	numeraireAtEval			= model.getNumeraire(evaluationTime);
			final RandomVariable	numeraire				= model.getNumeraire(exerciseDate);
			values.div(numeraire).mult(numeraireAtEval);
		}

		// Return values
		return values;
	}

	/**
	 * @param exerciseDate
	 * @param model
	 * @return
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	private RandomVariable[] getRegressionBasisFunctions(final double exerciseDate, final LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		RandomVariable basisFunction;

		// Constant
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		basisFunctions.add(basisFunction);

		// LIBORs
		int liborPeriodIndex, liborPeriodIndexEnd;
		RandomVariable rate;

		// 1 Period
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = liborPeriodIndex+1;
		final double periodLength1 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);

		// n/2 Period
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = (liborPeriodIndex + model.getNumberOfLibors())/2;

		final double periodLength2 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength2 != periodLength1) {
			rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);

			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);

			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);
		}


		// n Period
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = model.getNumberOfLibors();
		final double periodLength3 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength3 != periodLength1 && periodLength3 != periodLength2) {
			rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength3);
			basisFunctions.add(basisFunction);

			basisFunction = basisFunction.discount(rate, periodLength3);
			basisFunctions.add(basisFunction);
		}

		return basisFunctions.toArray(new RandomVariable[0]);
	}
}
