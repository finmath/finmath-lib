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
import net.finmath.montecarlo.hybridassetinterestrate.RiskFactorForwardRate;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;

/**
 * This class implements the valuation of a zero coupon bond.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class ForwardRateAgreementGeneralized extends HybridAssetMonteCarloProduct {

	private final LocalDateTime referenceDate;
	private final String currency;
	private final double fixing;
	private final double periodStart;
	private final double periodEnd;

	private final RandomVariable spread;

	private final RandomVariable cap;
	private final RandomVariable floor;



	public ForwardRateAgreementGeneralized(LocalDateTime referenceDate, String currency, double fixing, double periodStart, double periodEnd,
			RandomVariable spread, RandomVariable cap, RandomVariable floor) {
		super();
		this.referenceDate = referenceDate;
		this.currency = currency;
		this.fixing = fixing;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
		this.spread = spread;
		this.cap = cap;
		this.floor = floor;
	}

	/**
	 * Create a forward rate agreement.
	 *
	 * @param referenceDate The referenceDate corresponding to time t=0 of the model.
	 * @param curve The curve on which the fixing of the index occurs.
	 * @param fixing The floating point of the fixing date offset to the referenceData.
	 * @param periodStart The floating point of the period start date offset to the referenceData.
	 * @param periodEnd The floating point of the period end date offset to the referenceData.
	 */
	public ForwardRateAgreementGeneralized(LocalDateTime referenceDate, String curve, double fixing, double periodStart, double periodEnd) {
		this(referenceDate, curve, fixing, periodStart, periodEnd, null, null, null);
	}

	public ForwardRateAgreementGeneralized(String currency, double fixing, double periodStart, double periodEnd) {
		this(null, currency, fixing, periodStart, periodEnd);
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
		final RandomVariable	numeraire				= model.getNumeraire(productToModelTimeOffset + periodEnd);
		final RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(productToModelTimeOffset + periodEnd);

		// Get the index - forward rate.
		RandomVariable values = model.getValue(new RiskFactorForwardRate(currency, productToModelTimeOffset+periodStart, productToModelTimeOffset+periodEnd), productToModelTimeOffset+fixing);

		// Apply a spread, cap or floor - if present
		if(spread != null) {
			values = values.add(spread);
		}
		if(cap != null) {
			values = values.cap(cap);
		}
		if(floor != null) {
			values = values.floor(floor);
		}

		// Convert foreign currency to domenstic currency
		values = values.mult(model.getValue(new RiskFactorFX(currency), periodEnd));

		// Convert to the numerarie relative value
		values = values.div(numeraire).mult(monteCarloProbabilities);

		// Convert back to values
		final RandomVariable	numeraireAtEvaluationTime				= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtEvaluationTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvaluationTime).div(monteCarloProbabilitiesAtEvaluationTime);

		// Return values
		return values;
	}
}
