/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements the valuation of a digital option on a single asset.
 *
 * Given a model for an asset <i>S</i>, the digital option with strike <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>V(T) = indicator(S(T) - K)</i> in <i>T</i>.
 * <br>
 * That is 1 if <i>S(T) &gt; 0</i> otherwise 0.
 *
 * The <code>getValue</code> method of this class will return the random variable <i>N(t) * V(T) / N(T)</i>,
 * where <i>N</i> is the numeraire provided by the model. If <i>N(t)</i> is deterministic,
 * calling <code>getAverage</code> on this random variable will result in the value. Otherwise a
 * conditional expectation has to be applied.
 *
 * @author Christian Fries
 * @version 1.3
 */
public class DigitalOption extends AbstractAssetMonteCarloProduct implements DescribedProduct<SingleAssetDigitalOptionProductDescriptor> {

	private final double maturity;
	private final double strike;
	private final Integer underlyingIndex;
	private final String nameOfUnderlying;

	/**
	 * Construct a product representing an digital option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff indicator(S(T)-K)
	 * @param strike The strike K in the option payoff indicator(S(T)-K).
	 * @param underlyingIndex The index of the underlying to be fetched from the model.
	 */
	public DigitalOption(double maturity, double strike, int underlyingIndex) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		this.underlyingIndex	= underlyingIndex;
		this.nameOfUnderlying	= null;		// Use underlyingIndex
	}

	/**
	 * Construct a product representing an digital option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff indicator(S(T)-K)
	 * @param strike The strike K in the option payoff indicator(S(T)-K).
	 */
	public DigitalOption(double maturity, double strike) {
		this(maturity, strike, 0);
	}

	public DigitalOption(String nameOfUnderlying, double maturity, double strike) {
		super();
		this.nameOfUnderlying = nameOfUnderlying;
		this.maturity = maturity;
		this.strike = strike;
		this.underlyingIndex = 0;
	}

	public DigitalOption(SingleAssetDigitalOptionProductDescriptor descriptor) {
		this(descriptor.getNameOfUnderlying(), descriptor.getMaturity(), descriptor.getStrike());
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
		// Get underlying and numeraire

		// Get S(T)
		RandomVariableInterface underlyingAtMaturity	= model.getAssetValue(maturity, underlyingIndex);

		// The payoff: values = indicator(underlying - strike, 0) = V(T) = max(S(T)-K,0)
		RandomVariableInterface values = underlyingAtMaturity.barrier(underlyingAtMaturity.sub(strike), underlyingAtMaturity.mult(0.0).add(1.0), 0.0);

		// Discounting...
		RandomVariableInterface numeraireAtMaturity		= model.getNumeraire(maturity);
		RandomVariableInterface monteCarloWeights		= model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		RandomVariableInterface	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return values;
	}

	@Override
	public SingleAssetDigitalOptionProductDescriptor getDescriptor() {
		return new SingleAssetDigitalOptionProductDescriptor(nameOfUnderlying, maturity, strike);
	}
}
