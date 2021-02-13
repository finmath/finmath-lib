/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.Model;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.FundingCapacity;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the valuation of a forward on a single asset.
 *
 * Given a model for an asset <i>S</i>, the Forward Agreement with forward <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>V(T) = S(T) - K</i> in <i>T</i>.
 * <br>
 *
 * The <code>getValue</code> method of this class will return the random variable <i>N(t) * V(T) / N(T)</i>,
 * where <i>N</i> is the numeraire provided by the model. If <i>N(t)</i> is deterministic,
 * calling <code>getAverage</code> on this random variable will result in the value. Otherwise a
 * conditional expectation has to be applied.
 *
 * @author Christian Fries
 * @version 1.3
 */
public class ForwardAgreementWithFundingRequirement extends AbstractAssetMonteCarloProduct {

	private final double maturity;
	private final double forwardValue;
	private final Integer underlyingIndex;
	private final String nameOfUnderliyng;

	private final FundingCapacity fundingCapacity;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff S(T)-K
	 * @param forwardValue The strike K in the option payoff S(T)-K.
	 * @param fundingCapacity A funding capacity monitor.
	 */
	public ForwardAgreementWithFundingRequirement(final String underlyingName, final double maturity, final double forwardValue, final FundingCapacity fundingCapacity) {
		super();
		nameOfUnderliyng	= underlyingName;
		this.maturity		= maturity;
		this.forwardValue	= forwardValue;
		underlyingIndex		= 0;

		this.fundingCapacity	= fundingCapacity;
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param forwardValue The strike K in the option payoff max(S(T)-K,0).
	 * @param underlyingIndex The index of the underlying to be fetched from the model.
	 * @param fundingCapacity A funding capacity monitor.
	 */
	public ForwardAgreementWithFundingRequirement(final double maturity, final double forwardValue, final int underlyingIndex, final FundingCapacity fundingCapacity) {
		super();
		this.maturity			= maturity;
		this.forwardValue		= forwardValue;
		this.underlyingIndex	= underlyingIndex;
		nameOfUnderliyng		= null;		// Use underlyingIndex

		this.fundingCapacity	= fundingCapacity;
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param forwardValue The strike K in the option payoff max(S(T)-K,0).
	 */
	public ForwardAgreementWithFundingRequirement(final double maturity, final double forwardValue) {
		this(maturity, forwardValue, 0, null);
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
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {
		// Get underlying and numeraire

		// Get S(T)
		final RandomVariable underlyingAtMaturity	= model.getAssetValue(maturity, underlyingIndex);

		// The payoff: values = underlying - strike = V(T) = S(T)-K
		RandomVariable values = underlyingAtMaturity.sub(forwardValue);

		final RandomVariable defaultCompensation = fundingCapacity.getDefaultFactors(maturity, values).getDefaultCompensation();
		values = values.mult(defaultCompensation);

		// Discounting...
		final RandomVariable numeraireAtMaturity	= model.getNumeraire(maturity);
		final RandomVariable monteCarloWeights		= model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		final RandomVariable	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return values;
	}

	@Override
	public Map<String, Object> getValues(final double evaluationTime, final Model model) {
		final Map<String, Object>  result = new HashMap<>();

		try {
			final double value = getValue(evaluationTime, (AssetModelMonteCarloSimulationModel) model).getAverage();
			result.put("value", value);
		} catch (final CalculationException e) {
			result.put("exception", e);
		}

		return result;
	}

}
