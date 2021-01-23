/*
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

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
	public Cashflow(final String currency, final double flowAmount, final double flowDate, final boolean isPayer) {
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
	public Cashflow(final double flowAmount, final double flowDate, final boolean isPayer) {
		this(null, flowAmount, flowDate, isPayer);
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
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
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		// Note: We use > here. To distinguish an end of day valuation use hour of day for cash flows and evaluation date.
		if(evaluationTime > flowDate) {
			return model.getRandomVariableForConstant(0.0);
		}

		RandomVariable values = model.getRandomVariableForConstant(flowAmount);
		if(isPayer) {
			values = values.mult(-1.0);
		}

		// Rebase to evaluationTime
		if(flowDate != evaluationTime) {
			// Get random variables
			final RandomVariable	numeraire				= model.getNumeraire(flowDate);
			final RandomVariable	numeraireAtEval			= model.getNumeraire(evaluationTime);
			//        RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(getPaymentDate());
			values = values.div(numeraire).mult(numeraireAtEval);
		}

		// Return values
		return values;
	}

	@Override
	public String toString() {
		return "Cashflow [flowAmount=" + flowAmount + ", flowDate=" + flowDate
				+ ", isPayer=" + isPayer + ", toString()=" + super.toString()
				+ "]";
	}
}
