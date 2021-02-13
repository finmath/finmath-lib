/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.11.2011
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implements the valuation of an Asian option.
 *
 * Given a model for an asset <i>S</i>, the Asian option with strike <i>K</i>, maturity <i>T</i>
 * and averaging points <i>T<sub>i</sub></i> for <i>i = 1,...,n</i> pays
 * <br>
 * 	<i>max(A(T) - K , 0)</i> in <i>T</i>
 * <br>
 * where
 * <br>
 * 	<i>A(T) = 1/n (S(T<sub>1</sub>)+ ... + S(T<sub>n</sub>))</i>
 * <br>
 *
 * @author Christian Fries
 * @version 1.2
 */
public class AsianOption extends AbstractAssetMonteCarloProduct {

	private final double maturity;
	private final double strike;
	private final TimeDiscretization timesForAveraging;
	private final Integer underlyingIndex;

	/**
	 * Construct a product representing an Asian option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * A(T) = 1/n sum_{i=1,...,n} S(t_i), where t_i are given observation times.
	 *
	 * @param strike The strike K in the option payoff max(A(T)-K,0).
	 * @param maturity The maturity T in the option payoff maxAS(T)-K,0)
	 * @param timesForAveraging The times t_i used in the calculation of A(T) = 1/n sum_{i=1,...,n} S(t_i).
	 * @param underlyingIndex The index of the asset S to be fetched from the model
	 */
	public AsianOption(final double maturity, final double strike, final TimeDiscretization timesForAveraging, final Integer underlyingIndex) {
		super();
		this.maturity = maturity;
		this.strike = strike;
		this.timesForAveraging = timesForAveraging;
		this.underlyingIndex = underlyingIndex;
	}

	/**
	 * Construct a product representing an Asian option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * A(T) = 1/n sum_{i=1,...,n} S(t_i), where t_i are given observation times.
	 *
	 * @param strike The strike K in the option payoff max(A(T)-K,0).
	 * @param maturity The maturity T in the option payoff maxAS(T)-K,0)
	 * @param timesForAveraging The times t_i used in the calculation of A(T) = 1/n sum_{i=1,...,n} S(t_i).
	 */
	public AsianOption(final double maturity, final double strike, final TimeDiscretization timesForAveraging) {
		this(maturity, strike, timesForAveraging, 0);
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
		// Calculate average
		RandomVariable average = model.getRandomVariableForConstant(0.0);
		for(final double time : timesForAveraging) {
			final RandomVariable underlying	= model.getAssetValue(time, underlyingIndex);
			average = average.add(underlying);
		}
		average = average.div(timesForAveraging.getNumberOfTimes());

		// The payoff: values = max(underlying - strike, 0)
		RandomVariable values = average.sub(strike).floor(0.0);

		// Discounting...
		final RandomVariable numeraireAtMaturity	= model.getNumeraire(maturity);
		final RandomVariable monteCarloWeights		= model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		final RandomVariable	numeraireAtEvalTime			= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloWeightsAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloWeightsAtEvalTime);

		return values;
	}

	/**
	 * Returns the maturity of the option.
	 *
	 * @return the maturity of the option.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the strike of the option.
	 *
	 * @return the strike of the option.
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * Returns the TimeDiscretization used for averaging in the asian option.
	 *
	 * @return the TimeDiscretization used for averaging in the asian option.
	 */
	public TimeDiscretization getTimesForAveraging() {
		return timesForAveraging;
	}

	/**
	 * Returns the index of the asset requested from model.getUnderlying(time, assetIndex) to get the underlying.
	 *
	 * @return the index of the asset requested from model.getUnderlying(time, assetIndex) to get the underlying.
	 */
	public Integer getUnderlyingIndex() {
		return underlyingIndex;
	}
}
