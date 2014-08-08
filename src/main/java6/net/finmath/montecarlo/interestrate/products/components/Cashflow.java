/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A single deterministic cashflow at a fixed time
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class Cashflow extends AbstractProductComponent {

	private static final long serialVersionUID = 2336470863786839896L;

	private final double flowAmount;
	private final double flowDate;
	private final boolean isPayer;

	/**
	 * Create a single deterministic cashflow at a fixed time.
	 * 
	 * @param currency The currency.
	 * @param flowAmount The amount of the cash flow.
	 * @param flowDate The flow date.
	 * @param isPayer If true, this cash flow will be multiplied by -1 prior valuation.
	 */
	public Cashflow(String currency, double flowAmount, double flowDate, boolean isPayer) {
		super(currency);
		this.flowAmount = flowAmount;
		this.flowDate = flowDate;
		this.isPayer = isPayer;
	}

	/**
	 * Create a single deterministic cashflow at a fixed time.
	 * 
	 * @param flowAmount The amount of the cash flow.
	 * @param flowDate The flow date.
	 * @param isPayer If true, this cash flow will be multiplied by -1 prior valuation.
	 */
	public Cashflow(double flowAmount, double flowDate, boolean isPayer) {
		this(null, flowAmount, flowDate, isPayer);
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * cash-flows prior evaluationTime are not considered.
	 * 
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method. 
	 */
	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {        

		if(evaluationTime >= flowDate) return new RandomVariable(0.0);

		// Get random variables
		RandomVariableInterface	numeraire				= model.getNumeraire(flowDate);
		RandomVariableInterface	numeraireAtEval			= model.getNumeraire(evaluationTime);
		//        RandomVariableInterface	monteCarloProbabilities	= model.getMonteCarloWeights(getPaymentDate());

		RandomVariableInterface values = new RandomVariable(flowAmount);
		if(isPayer) values = values.mult(-1.0);

		values = values.div(numeraire).mult(numeraireAtEval);

		// Return values
		return values;	
	}    
}
