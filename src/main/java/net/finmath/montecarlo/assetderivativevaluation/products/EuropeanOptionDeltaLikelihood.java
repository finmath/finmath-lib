package net.finmath.montecarlo.assetderivativevaluation.products;
/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.02.2013
 */


import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements calculation of the delta of a European option using the likelihood ratio method.
 *
 * @author Christian Fries
 * @version 1.1
 * @since finmath-lib 4.1.0
 */
public class EuropeanOptionDeltaLikelihood extends AbstractAssetMonteCarloProduct {

	private final double	maturity;
	private final double	strike;

	private boolean	isLikelihoodByFiniteDifference = true;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 *
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 */
	public EuropeanOptionDeltaLikelihood(double maturity, double strike) {
		super();
		this.maturity = maturity;
		this.strike = strike;
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
	public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) throws CalculationException {

		BlackScholesModel blackScholesModel;
		try {
			blackScholesModel = (BlackScholesModel)((MonteCarloAssetModel)model).getModel();
		}
		catch(Exception e) {
			throw new ClassCastException("This method requires a Black-Scholes type model (MonteCarloBlackScholesModel).");
		}

		// Get underlying and numeraire
		RandomVariable underlyingAtMaturity	= model.getAssetValue(maturity, 0);
		RandomVariable underlyingAtToday		= model.getAssetValue(evaluationTime, 0);

		RandomVariable r = blackScholesModel.getRiskFreeRate();
		RandomVariable sigma = blackScholesModel.getVolatility();
		double T = maturity;

		/*
		 * Calculate the likelihood ratio
		 */
		RandomVariable likelihoodRatio;
		if(isLikelihoodByFiniteDifference) {
			/*
			 * Using finite difference to calculate the derivative of log(phi(x))
			 */
			double h		= 1E-6;

			RandomVariable x1 = underlyingAtMaturity.div(underlyingAtToday).log().sub(r.mult(T).sub(sigma.squared().mult(0.5*T))).div(sigma).div(Math.sqrt(T));
			RandomVariable logPhi1	= x1.squared().div(-2.0).exp().div(Math.sqrt(2 * Math.PI)).div(underlyingAtMaturity).div(sigma).div(Math.sqrt(T)).log();

			RandomVariable x2 = underlyingAtMaturity.div(underlyingAtToday.add(h)).log().sub(r.mult(T).sub(sigma.squared().mult(0.5*T))).div(sigma).div(Math.sqrt(T));
			RandomVariable logPhi2	= x2.squared().div(-2.0).exp().div(Math.sqrt(2 * Math.PI)).div(underlyingAtMaturity).div(sigma).div(Math.sqrt(T)).log();

			likelihoodRatio		= logPhi2.sub(logPhi1).div(h);
		}
		else {
			/*
			 * Analytic formula for d/dS0 log(phi(x)) with S0 = underlyingAtToday
			 */
			RandomVariable x = underlyingAtMaturity.div(underlyingAtToday).log().sub(r.mult(T).sub(sigma.squared().mult(0.5*T))).div(sigma).div(Math.sqrt(T));
			likelihoodRatio = x.div(underlyingAtToday).div(sigma).div(Math.sqrt(T));
		}

		RandomVariable values	= underlyingAtMaturity.sub(strike).floor(0.0).mult(likelihoodRatio);

		// Discounting...
		RandomVariable numeraireAtMaturity		= model.getNumeraire(maturity);
		RandomVariable monteCarloWeights		= model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		RandomVariable	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		RandomVariable	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return values;
	}
}
