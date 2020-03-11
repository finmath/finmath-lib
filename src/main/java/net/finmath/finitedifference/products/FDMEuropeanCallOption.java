package net.finmath.finitedifference.products;

import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.models.FiniteDifference1DBoundary;
import net.finmath.finitedifference.models.FiniteDifference1DModel;

/**
 * Implementation of a European option to be valued by a the finite difference method.
 *
 * @author Christian Fries
 * @author Ralph Rudd
 * @version 1.0
 */
public class FDMEuropeanCallOption implements FiniteDifference1DProduct, FiniteDifference1DBoundary {
	private final double maturity;
	private final double strike;

	public FDMEuropeanCallOption(final double optionMaturity, final double optionStrike) {
		maturity = optionMaturity;
		strike = optionStrike;
	}

	@Override
	public double[][] getValue(final double evaluationTime, final FiniteDifference1DModel model) {

		/*
		 * The FDM algorithm requires the boundary conditions of the product.
		 * This product implements the boundary interface
		 */
		final FiniteDifference1DBoundary boundary = this;

		return model.getValue(evaluationTime, maturity, new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double assetValue) {
				return Math.max(assetValue - strike, 0);
			}
		}, boundary);
	}

	/*
	 * Implementation of the interface:
	 * @see net.finmath.finitedifference.products.FiniteDifference1DBoundary#getValueAtLowerBoundary(net.finmath.finitedifference.models.FDMBlackScholesModel, double, double)
	 */

	@Override
	public double getValueAtLowerBoundary(final FiniteDifference1DModel model, final double currentTime, final double stockPrice) {
		return 0;
	}

	@Override
	public double getValueAtUpperBoundary(final FiniteDifference1DModel model, final double currentTime, final double stockPrice) {
		return stockPrice - strike * Math.exp(-model.getRiskFreeRate()*(maturity - currentTime));
	}
}
