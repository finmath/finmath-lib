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
import net.finmath.stochastic.RandomVariable;

/**
 * An indexed value. Implements the function J(t) V(t), where J(t) = E(I(t)|F_t) for the given I(t).
 *
 * @author Christian Fries
 * @version 1.1
 */
public class IndexedValue extends AbstractProductComponent {

	private static final long serialVersionUID = -7268432817913776974L;
	private final double exerciseDate;
	private final AbstractProductComponent index;
	private final AbstractProductComponent underlying;

	/**
	 * Creates the function J(t) V(t), where J(t) = E(I(t)|F_t) for the given I(t).
	 *
	 * @param exerciseDate The time t at which the index I is requested (and to which it is conditioned if necessary).
	 * @param index The index I.
	 * @param underlying The value V.
	 */
	public IndexedValue(final double exerciseDate, final AbstractProductComponent index, final AbstractProductComponent underlying) {
		super();
		this.exerciseDate = exerciseDate;
		this.index = index;
		this.underlying = underlying;
	}

	@Override
	public String getCurrency() {
		return underlying.getCurrency();
	}

	@Override
	public Set<String> queryUnderlyings() {
		final Set<String> underlyingNames = underlying.queryUnderlyings();
		final Set<String> indexUnderylingNames = index.queryUnderlyings();
		if(underlyingNames == null && indexUnderylingNames == null) {
			return null;
		} else if(underlyingNames != null && indexUnderylingNames == null) {
			return underlyingNames;
		} else if(underlyingNames == null && indexUnderylingNames != null) {
			return indexUnderylingNames;
		} else {
			underlyingNames.addAll(indexUnderylingNames);
			return underlyingNames;
		}
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cash flows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final double evaluationTimeUnderlying = Math.max(evaluationTime, exerciseDate);

		RandomVariable underlyingValues	= underlying.getValue(evaluationTimeUnderlying, model);
		RandomVariable indexValues			= index.getValue(exerciseDate, model);

		// Make index measurable w.r.t time exerciseDate
		if(indexValues.getFiltrationTime() > exerciseDate && exerciseDate > evaluationTime) {
			final MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(exerciseDate, (LIBORModelMonteCarloSimulationModel) model));

			// Calculate cond. expectation.
			indexValues         = condExpEstimator.getConditionalExpectation(indexValues);
		}

		// Form product
		underlyingValues = underlyingValues.mult(indexValues);

		// Discount to evaluation time if necessary
		if(evaluationTime != evaluationTimeUnderlying) {
			final RandomVariable	numeraireAtEval			= model.getNumeraire(evaluationTime);
			final RandomVariable	numeraire				= model.getNumeraire(evaluationTimeUnderlying);
			underlyingValues = underlyingValues.div(numeraire).mult(numeraireAtEval);
		}

		// Return values
		return underlyingValues;
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
		double periodLength;
		RandomVariable rate;

		// 1 Period
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = liborPeriodIndex+1;
		periodLength = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		// n/2 Period
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = (liborPeriodIndex + model.getNumberOfLibors())/2;

		periodLength = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		// n Period
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = model.getNumberOfLibors();
		periodLength = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		return basisFunctions.toArray(new RandomVariable[0]);
	}
}
