package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FiniteDifference1DBoundary;
import net.finmath.finitedifference.models.FiniteDifference1DModel;

/**
 * Implementation of a European option to be valued by a the finite difference method.
 *
 * @author Christian Fries
 * @author Ralph Rudd
 */
public class FDMEuropeanPutOption implements FiniteDifference1DProduct, FiniteDifference1DBoundary {
	private final double maturity;
	private final double strike;

	public FDMEuropeanPutOption(double optionMaturity, double optionStrike) {
		this.maturity = optionMaturity;
		this.strike = optionStrike;
	}

	@Override
	public double[][] getValue(double evaluationTime, FiniteDifference1DModel model) {

		/*
		 * The FDM algorithm requires the boundary conditions of the product.
		 * This product implements the boundary interface
		 */
		final FiniteDifference1DBoundary boundary = this;

		return model.getValue(evaluationTime, maturity, assetValue ->  Math.max(strike - assetValue, 0), boundary);
	}

	/*
	 * Implementation of the interface:
	 * @see net.finmath.finitedifference.products.FiniteDifference1DBoundary#getValueAtLowerBoundary(net.finmath.finitedifference.models.FDMBlackScholesModel, double, double)
	 */

	@Override
	public double getValueAtLowerBoundary(FiniteDifference1DModel model, double currentTime, double stockPrice) {
		return  strike * Math.exp(-model.getRiskFreeRate()*(maturity - currentTime));
	}

	@Override
	public double getValueAtUpperBoundary(FiniteDifference1DModel model, double currentTime, double stockPrice) {
		return 0;
	}
}
