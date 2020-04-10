/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.hybridassetinterestrate.products;

import java.time.LocalDateTime;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.hybridassetinterestrate.HybridAssetMonteCarloSimulation;
import net.finmath.montecarlo.hybridassetinterestrate.RiskFactorFX;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;

/**
 * This class implements the valuation of a zero coupon bond.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class Bond extends HybridAssetMonteCarloProduct {

	private final LocalDateTime referenceDate;
	private final String currency;
	private double maturity;

	/**
	 * @param referenceDate The date corresponding to \( t = 0 \).
	 * @param currency The currency to be used (may include information on collateralization).
	 * @param maturity The maturity given as double.
	 */
	public Bond(final LocalDateTime referenceDate, final String currency, final double maturity) {
		super();
		this.referenceDate = referenceDate;
		this.currency = currency;
		this.maturity = maturity;
	}

	/**
	 * @param currency The currency to be used (may include information on collateralization).
	 * @param maturity The maturity given as double.
	 */
	public Bond(final String currency, final double maturity) {
		this(null, currency, maturity);
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
	public RandomVariable getValue(final double evaluationTime, final HybridAssetMonteCarloSimulation model) throws CalculationException {

		double productToModelTimeOffset = 0;
		try {
			if(referenceDate != null) {
				productToModelTimeOffset = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate(), referenceDate);
			}
		}
		catch(final UnsupportedOperationException e) {}

		// Get random variables
		final RandomVariable	numeraire				= model.getNumeraire(productToModelTimeOffset + maturity);
		final RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(productToModelTimeOffset + maturity);

		// Calculate numeraire relative value
		RandomVariable values = model.getRandomVariableForConstant(1.0);

		// Convert foreign currency to domenstic currency
		values = values.mult(model.getValue(new RiskFactorFX(currency), maturity));

		values = values.div(numeraire).mult(monteCarloProbabilities);

		// Convert back to values
		final RandomVariable	numeraireAtEvaluationTime				= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtEvaluationTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvaluationTime).div(monteCarloProbabilitiesAtEvaluationTime);

		// Return values
		return values;
	}

	/**
	 * @return Returns the maturity.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * @param maturity The maturity to set.
	 */
	public void setMaturity(final double maturity) {
		this.maturity = maturity;
	}

	@Override
	public String toString() {
		return super.toString() + "\n" + "maturity: " + maturity;
	}
}
