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
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
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
	public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {

		/*
		 * The following valuation code requires in-depth knowledge of the model to calculate the denstiy analytically.
		 */
		BlackScholesModel blackScholesModel = null;
		if(model instanceof MonteCarloAssetModel) {
			try {
				blackScholesModel = (BlackScholesModel)((MonteCarloAssetModel)model).getModel();
			}
			catch(Exception e) {}
		}
		else if(model instanceof MonteCarloBlackScholesModel) {
			blackScholesModel = ((MonteCarloBlackScholesModel)model).getModel();
		}
		if(model == null) {
			throw new ClassCastException("This method requires a Black-Scholes type model (MonteCarloBlackScholesModel).");
		}

		// Get underlying and numeraire
		RandomVariableInterface underlyingAtMaturity	= model.getAssetValue(maturity,0);
		RandomVariableInterface underlyingAtToday		= model.getAssetValue(0.0,0);

		// Get some model parameters
		double T		= maturity-evaluationTime;
		double r		= blackScholesModel.getRiskFreeRate().doubleValue();
		double sigma	= blackScholesModel.getVolatility().doubleValue();

		RandomVariableInterface lr = underlyingAtMaturity.log().sub(underlyingAtToday.log()).sub(r * T - 0.5 * sigma*sigma * T).div(sigma * sigma * T).div(underlyingAtToday);

		RandomVariableInterface payoff = underlyingAtMaturity.sub(strike).choose(new Scalar(1.0), new Scalar(0.0));

		RandomVariableInterface modifiedPayoff = payoff.mult(lr);

		RandomVariableInterface numeraireAtMaturity	= model.getNumeraire(maturity);
		RandomVariableInterface numeraireAtToday		= model.getNumeraire(0);
		RandomVariableInterface monteCarloWeightsAtMaturity		= model.getMonteCarloWeights(maturity);
		RandomVariableInterface monteCarloWeightsAtToday		= model.getMonteCarloWeights(maturity);

		return modifiedPayoff.div(numeraireAtMaturity).mult(numeraireAtToday).mult(monteCarloWeightsAtMaturity).div(monteCarloWeightsAtToday);
	}
}
