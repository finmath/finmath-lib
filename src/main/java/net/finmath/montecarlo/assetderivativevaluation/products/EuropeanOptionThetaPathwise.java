/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.07.2017
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements calculation of the theta of a European option using the pathwise method.
 *
 * @author Christian Fries
 * @version 1.0
 * @since finmath-lib 4.2.0
 */
public class EuropeanOptionThetaPathwise extends AbstractAssetMonteCarloProduct {

	private final double	maturity;
	private final double	strike;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 *
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 */
	public EuropeanOptionThetaPathwise(final double maturity, final double strike) {
		super();
		this.maturity = maturity;
		this.strike = strike;
	}

	/**
	 * Calculates the theta of the option under a given model.
	 *
	 * @param model A reference to a model
	 * @return the value
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public double getValue(final AssetModelMonteCarloSimulationModel model) throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = null;
		try {
			blackScholesModel = (MonteCarloBlackScholesModel)model;
		}
		catch(final Exception e) {
			throw new ClassCastException("This method requires a Black-Scholes type model (MonteCarloBlackScholesModel).");
		}

		// Get underlying and numeraire
		final RandomVariable underlyingAtMaturity	= model.getAssetValue(maturity,0);
		final RandomVariable numeraireAtMaturity	= model.getNumeraire(maturity);
		final RandomVariable underlyingAtToday		= model.getAssetValue(0.0,0);
		final RandomVariable numeraireAtToday		= model.getNumeraire(0);
		final RandomVariable monteCarloWeights		= model.getMonteCarloWeights(maturity);

		/*
		 *  The following way of calculating the expected value (average) is discouraged since it makes too strong
		 *  assumptions on the internals of the <code>RandomVariable</code>. Instead you should use
		 *  the methods sub, div, mult and the getter getAverage. This code is provided for illustrative purposes.
		 */
		double average = 0.0;
		for(int path=0; path<model.getNumberOfPaths(); path++)
		{
			if(underlyingAtMaturity.get(path) > strike)
			{
				// Get some model parameters
				final double T		= maturity;
				final double S0		= underlyingAtToday.get(path);
				final double r		= blackScholesModel.getModel().getRiskFreeRate().doubleValue();
				final double sigma	= blackScholesModel.getModel().getVolatility().doubleValue();

				final double ST		= underlyingAtMaturity.get(path);
				final double WT		= (Math.log(ST/S0) - r * T + 0.5 * sigma * sigma * T)/sigma;

				final double modifiedPayoff	= 	ST * (r-0.5*sigma*sigma + 0.5 * sigma * WT / T) - (ST-strike) * r;

				average += modifiedPayoff / numeraireAtMaturity.get(path) * monteCarloWeights.get(path) * numeraireAtToday.get(path);
			}
		}

		return average;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) {
		throw new RuntimeException("Method not supported.");
	}
}
