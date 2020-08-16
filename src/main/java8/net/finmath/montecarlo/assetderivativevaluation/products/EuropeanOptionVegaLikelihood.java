package net.finmath.montecarlo.assetderivativevaluation.products;
/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.02.2013
 */


import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.RandomVariableAccumulator;

/**
 * Implements calculation of the delta of a European option.
 *
 * @author Christian Fries
 * @version 1.0
 * @since finmath-lib 4.2.0
 */
public class EuropeanOptionVegaLikelihood extends AbstractAssetMonteCarloProduct {

	private final double	maturity;
	private final double	strike;

	private final boolean	isLikelihoodByFiniteDifference = false;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 *
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 */
	public EuropeanOptionVegaLikelihood(final double maturity, final double strike) {
		super();
		this.maturity = maturity;
		this.strike = strike;
	}

	/**
	 * Calculates the value of the option under a given model.
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
		final RandomVariable monteCarloWeightsAtToday	= model.getMonteCarloWeights(0.0);

		/*
		 *  The following way of calculating the expected value (average) is discouraged since it makes too strong
		 *  assumptions on the internals of the <code>RandomVariableAccumulatorInterface</code>. Instead you should use
		 *  the mutators sub, div, mult and the getter getAverage. This code is provided for illustrative purposes.
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

				final double x		= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0)));

				double lr;
				if(isLikelihoodByFiniteDifference) {
					final double h		= 1E-6;

					final double x1		= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0)));
					final double logPhi1	= Math.log(1.0/Math.sqrt(2 * Math.PI) * Math.exp(-x1*x1/2.0) / (ST * (sigma) * Math.sqrt(T)) );

					final double x2		= 1.0 / ((sigma+h) * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * (sigma+h)*(sigma+h) * T + Math.log(S0)));
					final double logPhi2	= Math.log(1.0/Math.sqrt(2 * Math.PI) * Math.exp(-x2*x2/2.0) / (ST * (sigma+h) * Math.sqrt(T)) );

					lr		= (logPhi2 - logPhi1) / h;
				}
				else {
					final double dxdsigma = -x / sigma + Math.sqrt(T);

					lr		= - x * dxdsigma - 1/sigma;
				}

				final double payOff			= (underlyingAtMaturity.get(path) - strike);
				final double modifiedPayoff	= payOff * lr;

				average += modifiedPayoff / numeraireAtMaturity.get(path) * numeraireAtToday.get(path) * monteCarloWeights.get(path) / monteCarloWeightsAtToday.get(path);
			}
		}

		return average;
	}

	@Override
	public RandomVariableAccumulator getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) {
		throw new RuntimeException("Method not supported.");
	}
}
