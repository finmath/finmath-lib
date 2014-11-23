/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.ArrayList;
import java.util.Set;

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
 * @version 1.2
 */
public class Option extends AbstractProductComponent {

	private static final long serialVersionUID = -7268432817913776974L;
	private final double							exerciseDate;
	private final double							strikePrice;
	private final AbstractLIBORMonteCarloProduct	underlying;
	private final AbstractLIBORMonteCarloProduct	strikeProduct;
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
		this.strikeProduct	= null;
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice
	 * 
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param isCall If true, the function implements is underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice. Otherwise it is underlying(exerciseDate) &lt; strikePrice ? underlying : strikePrice.
	 * @param strikeProduct The strike.
	 * @param underlying The underlying.
	 */
	public Option(double exerciseDate,boolean isCall,  AbstractLIBORMonteCarloProduct strikeProduct, AbstractLIBORMonteCarloProduct underlying) {
		super();
		this.exerciseDate	= exerciseDate;
		this.strikePrice	= Double.NaN;
		this.strikeProduct	= strikeProduct;
		this.underlying		= underlying;
		this.isCall			= isCall;
	}

	@Override
	public String getCurrency() {
		return underlying.getCurrency();
	}

	@Override
	public Set<String> queryUnderlyings() {
		if(underlying instanceof AbstractProductComponent)	return ((AbstractProductComponent)underlying).queryUnderlyings();
		else												throw new IllegalArgumentException("Underlying cannot be queried for underlyings.");
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
		
		// TODO >=? -
		if(evaluationTime > exerciseDate) return zero;

		RandomVariableInterface values = underlying.getValue(exerciseDate, model);
		RandomVariableInterface strike = strikeProduct.getValue(exerciseDate, model);

		RandomVariableInterface exerciseTrigger = null;
		if(strikeProduct != null)	exerciseTrigger = values.sub(strike).mult(isCall ? 1.0 : -1.0);
		else						exerciseTrigger = values.sub(strikePrice).mult(isCall ? 1.0 : -1.0);
		
		if(exerciseTrigger.getFiltrationTime() > exerciseDate) {
			RandomVariableInterface filterNaN = exerciseTrigger.isNaN().sub(1.0).mult(-1.0);
			RandomVariableInterface exerciseTriggerFiltered = exerciseTrigger.mult(filterNaN);

			/*
			 * Cut off two standard deviations from regression
			 */
			double exerciseTriggerMean		= exerciseTriggerFiltered.getAverage();
			double exerciseTriggerStdDev	= exerciseTriggerFiltered.getStandardDeviation();
			double exerciseTriggerFloor		= exerciseTriggerMean*(1.0-Math.signum(exerciseTriggerMean)*1E-5)-2.0*exerciseTriggerStdDev;
			double exerciseTriggerCap		= exerciseTriggerMean*(1.0+Math.signum(exerciseTriggerMean)*1E-5)+2.0*exerciseTriggerStdDev;
			RandomVariableInterface filter = exerciseTrigger
					.barrier(exerciseTrigger.sub(exerciseTriggerFloor), one, zero)
					.mult(exerciseTrigger.barrier(exerciseTrigger.sub(exerciseTriggerCap).mult(-1.0), one, zero));
			filter = filter.mult(filterNaN);
			// Filter exerciseTrigger and regressionBasisFunctions
			exerciseTrigger = exerciseTrigger.mult(filter);

			RandomVariableInterface[] regressionBasisFunctions			= getRegressionBasisFunctions(exerciseDate, model);
			RandomVariableInterface[] filteredRegressionBasisFunctions	= new RandomVariableInterface[regressionBasisFunctions.length];
			for(int i=0; i<regressionBasisFunctions.length; i++) filteredRegressionBasisFunctions[i] = regressionBasisFunctions[i].mult(filter);

			// Remove foresight through conditional expectation
			MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(filteredRegressionBasisFunctions, regressionBasisFunctions);

			// Calculate cond. expectation. Note that no discounting (numeraire division) is required!
			exerciseTrigger         = condExpEstimator.getConditionalExpectation(exerciseTrigger);
		}

		// Apply exercise criteria
		if(strikeProduct != null)	values = values.barrier(exerciseTrigger, values, strikeProduct.getValue(exerciseDate, model));
		else						values = values.barrier(exerciseTrigger, values, strikePrice);

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
	 * Return the regression basis functions.
	 * 
	 * @param exerciseDate The date w.r.t. which the basis functions should be measurable.
	 * @param model The model.
	 * @return Array of random variables.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method. 
	 */
	private RandomVariableInterface[] getRegressionBasisFunctions(double exerciseDate, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<RandomVariableInterface>();

		RandomVariableInterface basisFunction;

		// Constant
		basisFunction = model.getRandomVariableForConstant(1.0);
		basisFunctions.add(basisFunction);

		// LIBORs
		int liborPeriodIndex, liborPeriodIndexEnd;
		RandomVariableInterface rate;
		
		// 1 Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) liborPeriodIndex = -liborPeriodIndex-1;
		liborPeriodIndexEnd = liborPeriodIndex+1;
		double periodLength1 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

		// n/2 Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) liborPeriodIndex = -liborPeriodIndex-1;
		liborPeriodIndexEnd = (liborPeriodIndex + model.getNumberOfLibors())/2;

		double periodLength2 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength2 != periodLength1) {
			rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength2);
//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength2);
//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));
		}


		// n Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) liborPeriodIndex = -liborPeriodIndex-1;
		liborPeriodIndexEnd = model.getNumberOfLibors();
		double periodLength3 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength3 != periodLength1 && periodLength3 != periodLength2) {
			rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength3);
			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength3);
//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));
		}
		
		return basisFunctions.toArray(new RandomVariableInterface[0]);
	}

	@Override
	public String toString() {
		return "Option [exerciseDate=" + exerciseDate + ", strikePrice="
				+ strikePrice + ", underlying=" + underlying + ", isCall="
				+ isCall + ", toString()=" + super.toString() + "]";
	}
}
