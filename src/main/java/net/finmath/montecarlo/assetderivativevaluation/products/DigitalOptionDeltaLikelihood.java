package net.finmath.montecarlo.assetderivativevaluation.products;
/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 12.02.2013
 */


import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.BlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;

/**
 * Implements calculation of the delta of a digital option.
 *
 * @author Christian Fries
 * @version 1.0
 * @since
 */
public class DigitalOptionDeltaLikelihood extends AbstractAssetMonteCarloProduct {

	private final double	maturity;
	private final double	strike;

	private boolean	isLikelihoodByFiniteDifference = false;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 *
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 */
	public DigitalOptionDeltaLikelihood(double maturity, double strike) {
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
	public double getValue(MonteCarloAssetModel model) throws CalculationException
	{
		BlackScholesModel blackScholesModel = null;
		try {
			blackScholesModel = (BlackScholesModel)model.getModel();
		}
		catch(Exception e) {
			throw new ClassCastException("This method requires a Black-Scholes type model (MonteCarloBlackScholesModel).");
		}

		// Get underlying and numeraire
		RandomVariableInterface underlyingAtMaturity	= model.getAssetValue(maturity,0);
		RandomVariableInterface numeraireAtMaturity	= model.getNumeraire(maturity);
		RandomVariableInterface underlyingAtToday		= model.getAssetValue(0.0,0);
		RandomVariableInterface numeraireAtToday		= model.getNumeraire(0);
		RandomVariableInterface monteCarloWeights		= model.getMonteCarloWeights(maturity);

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
				double T		= maturity;
				double S0		= underlyingAtToday.get(path);
				double r		= blackScholesModel.getRiskFreeRate().doubleValue();
				double sigma	= blackScholesModel.getVolatility().doubleValue();

				double ST		= underlyingAtMaturity.get(path);

				double x		= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0)));

				double lr;
				if(isLikelihoodByFiniteDifference) {
					double h		= 1E-6;

					double x1		= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0)));
					double logPhi1	= Math.log(1.0/Math.sqrt(2 * Math.PI) * Math.exp(-x1*x1/2.0) / (ST * (sigma) * Math.sqrt(T)) );

					double x2		= 1.0 / (sigma * Math.sqrt(T)) * (Math.log(ST) - (r * T - 0.5 * sigma*sigma * T + Math.log(S0+h)));
					double logPhi2	= Math.log(1.0/Math.sqrt(2 * Math.PI) * Math.exp(-x2*x2/2.0) / (ST * (sigma) * Math.sqrt(T)) );

					lr		= (logPhi2 - logPhi1) / h;
				}
				else {
					lr		= x / (sigma * Math.sqrt(T)) / S0;
				}

				double payOff			= 1.0;	// Note: if condition (underlyingAtMaturity > strike) already applied
				double modifiedPayoff	= payOff * lr;

				average += modifiedPayoff / numeraireAtMaturity.get(path) * monteCarloWeights.get(path) * numeraireAtToday.get(path);
			}
		}

		return average;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {
		return new Scalar(getValue((MonteCarloAssetModel)model));//new RuntimeException("Method not supported.");
	}
}
