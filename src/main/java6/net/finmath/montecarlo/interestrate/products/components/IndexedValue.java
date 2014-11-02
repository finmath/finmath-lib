/*
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
 * An indexed value. Implements the function J(t) V(t), where J(t) = E(I(t)|F_t) for the given I(t).
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class IndexedValue extends AbstractProductComponent {

	/**
	 * 
	 */
	private static final long serialVersionUID = -7268432817913776974L;
	private double exerciseDate;
	private AbstractProductComponent index;
	private AbstractProductComponent underlying;

	/**
	 * Creates the function J(t) V(t), where J(t) = E(I(t)|F_t) for the given I(t).
	 * 
     * @param exerciseDate
     * @param underlying
     */
    public IndexedValue(double exerciseDate, AbstractProductComponent index, AbstractProductComponent underlying) {
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
		Set<String> underlyingNames = underlying.queryUnderlyings();
		Set<String> indexUnderylingNames = index.queryUnderlyings();
		if(underlyingNames == null && indexUnderylingNames == null) return null;
		else if(underlyingNames != null && indexUnderylingNames == null) return underlyingNames;
		else if(underlyingNames == null && indexUnderylingNames != null) return indexUnderylingNames;
		else {
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
    public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        
		
    	double evaluationTimeUnderlying = Math.max(evaluationTime, exerciseDate);

    	RandomVariableInterface underlyingValues	= underlying.getValue(evaluationTimeUnderlying, model);
    	RandomVariableInterface indexValues			= index.getValue(exerciseDate, model);

    	// Make index measurable w.r.t time exerciseDate
    	if(indexValues.getFiltrationTime() > exerciseDate && exerciseDate > evaluationTime) {
    		MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(exerciseDate, model));

            // Calculate cond. expectation.
            indexValues         = condExpEstimator.getConditionalExpectation(indexValues);
    	}

    	// Form product
    	underlyingValues = underlyingValues.mult(indexValues);

    	// Discount to evaluation time if necessary
        if(evaluationTime != evaluationTimeUnderlying) {
            RandomVariableInterface	numeraireAtEval			= model.getNumeraire(evaluationTime);
            RandomVariableInterface	numeraire				= model.getNumeraire(evaluationTimeUnderlying);
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
	private RandomVariableInterface[] getRegressionBasisFunctions(double exerciseDate, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<RandomVariableInterface>();

		RandomVariableInterface basisFunction;

		// Constant
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		basisFunctions.add(basisFunction);

		// LIBORs
		int liborPeriodIndex, liborPeriodIndexEnd;
		double periodLength;
		RandomVariableInterface rate;
		
		// 1 Period
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = liborPeriodIndex+1;
		periodLength = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		// n/2 Period
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = (liborPeriodIndex + model.getNumberOfLibors())/2;

		periodLength = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		// n Period
		basisFunction = new RandomVariable(exerciseDate, 1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		liborPeriodIndexEnd = model.getNumberOfLibors();
		periodLength = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getLIBOR(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);

		basisFunction = basisFunction.discount(rate, periodLength);
		basisFunctions.add(basisFunction);
		
		return basisFunctions.toArray(new RandomVariableInterface[0]);
	}
}
