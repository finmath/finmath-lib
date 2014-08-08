/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.ArrayList;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * An option. Implements the function max(underlying(t)-K,0) for any underlying object implementing
 * an AbstractLIBORMonteCarloProduct.
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class Option extends AbstractProductComponent {

	private static final long serialVersionUID = -7268432817913776974L;
	private final double							exerciseDate;
	private final double							strikePrice;
	private final AbstractLIBORMonteCarloProduct	underlying;
	private final boolean							isCall;

	/**
	 * Creates the function underlying(exerciseDate) &ge; 0 ? underlying : 0
	 * 
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param underlying The underlying.
	 */
	public Option(double exerciseDate, AbstractLIBORMonteCarloProduct underlying) {
		this(exerciseDate, 0.0, underlying);
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice
	 * 
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param strikePrice The strike price.
	 * @param underlying The underlying.
	 */
	public Option(double exerciseDate, double strikePrice, AbstractLIBORMonteCarloProduct underlying) {
		this(exerciseDate, strikePrice, true, underlying);
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice
	 * 
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param strikePrice The strike price.
	 * @param isCall If true, the function implements is underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice. Otherwise it is underlying(exerciseDate) &lt; strikePrice ? underlying : strikePrice.
	 * @param underlying The underlying.
	 */
	public Option(double exerciseDate, double strikePrice, boolean isCall, AbstractLIBORMonteCarloProduct underlying) {
		super();
		this.exerciseDate	= exerciseDate;
		this.strikePrice	= strikePrice;
		this.underlying		= underlying;
		this.isCall			= isCall;
	}

	@Override
	public String getCurrency() {
		return underlying.getCurrency();
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

		// >=? -
		if(evaluationTime > exerciseDate) return new RandomVariable(0.0);

		RandomVariableInterface values = underlying.getValue(exerciseDate, model);

		RandomVariableInterface valueIfExcercised = null;
		if(values.getFiltrationTime() > exerciseDate) {
			// Remove foresight through conditional expectation
			MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(exerciseDate, model));

			// Calculate cond. expectation. Note that no discounting (numeraire division) is required!
			valueIfExcercised         = condExpEstimator.getConditionalExpectation(values);
		}
		else valueIfExcercised = values;

		// Apply exercise criteria
		values = values.barrier(valueIfExcercised.sub(strikePrice).mult(isCall ? 1.0 : -1.0), values, strikePrice);

		// Discount to evaluation time
		if(evaluationTime != exerciseDate) {
			RandomVariableInterface	numeraireAtEval			= model.getNumeraire(evaluationTime);
			RandomVariableInterface	numeraire				= model.getNumeraire(exerciseDate);
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
	private RandomVariableInterface[] getRegressionBasisFunctions(double exerciseDate, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<RandomVariableInterface>();

		RandomVariableInterface basisFunction;

		// Constant
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		basisFunctions.add(basisFunction);

		// LIBORs
		int liborPeriodIndex, liborPeriodIndexEnd;
		RandomVariableInterface rate;
		
		// 1 Period
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) liborPeriodIndex = -liborPeriodIndex-1;
		liborPeriodIndexEnd = liborPeriodIndex+1;
		double periodLength1 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);

		// n/2 Period
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) liborPeriodIndex = -liborPeriodIndex-1;
		liborPeriodIndexEnd = (liborPeriodIndex + model.getNumberOfLibors())/2;

		double periodLength2 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength2 != periodLength1) {
			rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);

			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);

			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);
		}


		// n Period
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) liborPeriodIndex = -liborPeriodIndex-1;
		liborPeriodIndexEnd = model.getNumberOfLibors();
		double periodLength3 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength3 != periodLength1 && periodLength3 != periodLength2) {
			rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength3);
			basisFunctions.add(basisFunction);

			basisFunction = basisFunction.discount(rate, periodLength3);
			basisFunctions.add(basisFunction);
		}
		
		return basisFunctions.toArray(new RandomVariableInterface[0]);
	}
}
