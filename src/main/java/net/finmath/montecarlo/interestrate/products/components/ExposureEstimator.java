/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.ArrayList;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements (a numerical approximation of) the function
 * \(
 * (t,V) \mapsto E( V(t) \vert \mathcal{F}_t )
 * \)
 * where \( V(t) \) is the (sum of) discounted future value(s) of an underlying \( V \), discounted to \( t \)
 * and \( t \) is a given evaluation time.
 *
 * The conditional expectation is estimated using a regression.
 *
 * @author Christian Fries
 * @version 1.3
 * @see net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct
 */
public class ExposureEstimator extends AbstractProductComponent {

	private static final long serialVersionUID = 2987369289230532162L;

	private final AbstractLIBORMonteCarloProduct	underlying;


	/**
	 * Creates (a numerical approximation of) the function
	 * \(
	 * (t,V) \mapsto E( V(t) \vert \mathcal{F}_t )
	 * \)
	 * where \( V(t) \) is the (sum of) discounted future value(s) of an underlying \( V \), discounted to \( t \)
	 * and \( t \) is a given evaluation time.
	 *
	 * @param underlying The underlying.
	 */
	public ExposureEstimator(AbstractLIBORMonteCarloProduct underlying) {
		super();
		this.underlying		= underlying;
	}

	@Override
	public String getCurrency() {
		return underlying.getCurrency();
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
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		final RandomVariableInterface one	= model.getRandomVariableForConstant(1.0);
		final RandomVariableInterface zero	= model.getRandomVariableForConstant(0.0);

		RandomVariableInterface values = underlying.getValue(evaluationTime, model);

		if(values.getFiltrationTime() > evaluationTime) {
			RandomVariableInterface filterNaN = values.isNaN().sub(1.0).mult(-1.0);
			RandomVariableInterface valuesFiltered = values.mult(filterNaN);

			/*
			 * Cut off two standard deviations from regression
			 */
			double valuesMean		= valuesFiltered.getAverage();
			double valuesStdDev	= valuesFiltered.getStandardDeviation();
			double valuesFloor		= valuesMean*(1.0-Math.signum(valuesMean)*1E-5)-3.0*valuesStdDev;
			double valuesCap		= valuesMean*(1.0+Math.signum(valuesMean)*1E-5)+3.0*valuesStdDev;
			RandomVariableInterface filter = values
					.barrier(values.sub(valuesFloor), one, zero)
					.mult(values.barrier(values.sub(valuesCap).mult(-1.0), one, zero));
			filter = filter.mult(filterNaN);
			// Filter values and regressionBasisFunctions
			values = values.mult(filter);

			RandomVariableInterface[] regressionBasisFunctions			= getRegressionBasisFunctions(evaluationTime, model);
			RandomVariableInterface[] filteredRegressionBasisFunctions	= new RandomVariableInterface[regressionBasisFunctions.length];
			for(int i=0; i<regressionBasisFunctions.length; i++) {
				filteredRegressionBasisFunctions[i] = regressionBasisFunctions[i].mult(filter);
			}

			// Remove foresight through conditional expectation
			MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(filteredRegressionBasisFunctions, regressionBasisFunctions);

			// Calculate cond. expectation. Note that no discounting (numeraire division) is required!
			values         = condExpEstimator.getConditionalExpectation(values);
		}

		// Return values
		return values;
	}

	/**
	 * Return the regression basis functions.
	 *
	 * @param evaluationTime The date w.r.t. which the basis functions should be measurable.
	 * @param model The model.
	 * @return Array of random variables.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	private RandomVariableInterface[] getRegressionBasisFunctions(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<>();

		RandomVariableInterface basisFunction;

		// Constant
		basisFunction = model.getRandomVariableForConstant(1.0);
		basisFunctions.add(basisFunction);

		// LIBORs
		int liborPeriodIndex, liborPeriodIndexEnd;
		RandomVariableInterface rate;

		// 1 Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(evaluationTime);
		if(liborPeriodIndex < 0) {
			liborPeriodIndex = -liborPeriodIndex-1;
		}
		liborPeriodIndexEnd = liborPeriodIndex+1;
		double periodLength1 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getLIBOR(evaluationTime, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

		// n/2 Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(evaluationTime);
		if(liborPeriodIndex < 0) {
			liborPeriodIndex = -liborPeriodIndex-1;
		}
		liborPeriodIndexEnd = (liborPeriodIndex + model.getNumberOfLibors())/2;

		double periodLength2 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength2 != periodLength1) {
			rate = model.getLIBOR(evaluationTime, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength2);
			//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength2);
			//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));
		}


		// n Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(evaluationTime);
		if(liborPeriodIndex < 0) {
			liborPeriodIndex = -liborPeriodIndex-1;
		}
		liborPeriodIndexEnd = model.getNumberOfLibors();
		double periodLength3 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength3 != periodLength1 && periodLength3 != periodLength2) {
			rate = model.getLIBOR(evaluationTime, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength3);
			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength3);
			//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));
		}

		return basisFunctions.toArray(new RandomVariableInterface[0]);
	}
}

