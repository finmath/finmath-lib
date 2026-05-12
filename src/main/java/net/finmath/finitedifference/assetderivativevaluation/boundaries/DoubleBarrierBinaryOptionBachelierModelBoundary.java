package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBachelierModel;
import net.finmath.finitedifference.assetderivativevaluation.products.DoubleBarrierBinaryOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;

/**
 * Boundary conditions for {@link DoubleBarrierBinaryOption} under
 * {@link FDMBachelierModel}.
 *
 * @author Alessandro Gnoatto
 */
public class DoubleBarrierBinaryOptionBachelierModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMBachelierModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public DoubleBarrierBinaryOptionBachelierModelBoundary(final FDMBachelierModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final DoubleBarrierBinaryOption option = (DoubleBarrierBinaryOption) product;
		final double value = getLowerBoundaryValue(option, time);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(value)
		};
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... stateVariables) {

		final DoubleBarrierBinaryOption option = (DoubleBarrierBinaryOption) product;
		final double value = getUpperBoundaryValue(option, time);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(value)
		};
	}

	private double getLowerBoundaryValue(
			final DoubleBarrierBinaryOption option,
			final double time) {

		switch (option.getDoubleBarrierType()) {
		case KNOCK_OUT:
			return 0.0;
		case KNOCK_IN:
			return getDiscountedCashValue(option, time);
		case KIKO:
			return getDiscountedCashValue(option, time);
		case KOKI:
			return 0.0;
		default:
			throw new IllegalArgumentException("Unsupported double barrier type.");
		}
	}

	private double getUpperBoundaryValue(
			final DoubleBarrierBinaryOption option,
			final double time) {

		switch (option.getDoubleBarrierType()) {
		case KNOCK_OUT:
			return 0.0;
		case KNOCK_IN:
			return getDiscountedCashValue(option, time);
		case KIKO:
			return 0.0;
		case KOKI:
			return getDiscountedCashValue(option, time);
		default:
			throw new IllegalArgumentException("Unsupported double barrier type.");
		}
	}

	private double getDiscountedCashValue(
			final DoubleBarrierBinaryOption option,
			final double time) {

		if (option.getCashPayoff() == 0.0) {
			return 0.0;
		}

		final double effectiveTime = Math.max(time, EPSILON);

		if (effectiveTime >= option.getMaturity()) {
			return option.getCashPayoff();
		}

		final double discountFactorAtTime =
				model.getRiskFreeCurve().getDiscountFactor(effectiveTime);
		final double discountFactorAtMaturity =
				model.getRiskFreeCurve().getDiscountFactor(option.getMaturity());

		return option.getCashPayoff() * discountFactorAtMaturity / discountFactorAtTime;
	}
}
