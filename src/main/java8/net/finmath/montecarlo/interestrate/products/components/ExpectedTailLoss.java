/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.ArrayList;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * The expected tail loss.
 * Provides the function underlying(exerciseDate) &ge; quantileValue ? underlying : 0.0,
 * where quantileValue is such that P(underlying &gt; quantileValue) = quantile
 *
 * @author Christian Fries
 * @version 1.1
 * @since finmath-lib 4.1.0
 */
public class ExpectedTailLoss extends AbstractProductComponent {

	/**
	 *
	 */
	private static final long serialVersionUID = 3211126102506873636L;
	private final double exerciseDate;
	private final double quantile;
	private final TermStructureMonteCarloProduct underlying;

	/**
	 * Creates the function underlying(exerciseDate) &ge; quantileValue ? underlying : 0.0,
	 * where quantileValue is such that P(underlying &gt; quantileValue) = quantile
	 *
	 * @param exerciseDate The date at which the underlying should be observed.
	 * @param quantile The quantile value.
	 * @param underlying The underlying.
	 */
	public ExpectedTailLoss(final double exerciseDate,
			final double quantile,
			final TermStructureMonteCarloProduct underlying) {
		super();
		this.exerciseDate = exerciseDate;
		this.quantile = quantile;
		this.underlying = underlying;
	}

	@Override
	public Set<String> queryUnderlyings() {
		if(underlying instanceof AbstractProductComponent) {
			return ((AbstractProductComponent)underlying).queryUnderlyings();
		} else {
			throw new IllegalArgumentException("Underlying cannot be queried for underlyings.");
		}
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

		// Values
		RandomVariable values = underlying.getValue(exerciseDate, model);


		// Remove foresight through conditional expectation
		final MonteCarloConditionalExpectationRegression conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(exerciseDate, (LIBORModelMonteCarloSimulationModel) model));

		// Calculate cond. expectation. Note that no discounting (numeraire division) is required!
		final RandomVariable underlyingExpectedValues = values.getConditionalExpectation(conditionalExpectationOperator);

		final double quantileValue = underlyingExpectedValues.getQuantile(quantile);

		// Apply floor
		values = values.sub(quantileValue).choose(values, new Scalar(0.0));

		// Discount to evaluation time
		if(evaluationTime != exerciseDate) {
			final RandomVariable	numeraireAtEval			= model.getNumeraire(evaluationTime);
			final RandomVariable	numeraire				= model.getNumeraire(exerciseDate);
			values = values.div(numeraire).mult(numeraireAtEval);
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
