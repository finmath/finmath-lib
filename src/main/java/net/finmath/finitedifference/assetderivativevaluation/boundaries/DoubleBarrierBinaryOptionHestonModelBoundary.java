package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.DoubleBarrierBinaryOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;

/**
 * Boundary conditions for {@link DoubleBarrierBinaryOption} under
 * {@link FDMHestonModel}.
 *
 * <p>
 * State variables are assumed to be (S, v), where S is the asset level and
 * v the variance.
 * </p>
 *
 * <p>
 * The double barriers are enforced internally by the product through state
 * constraints. Therefore, the outer-domain spot boundaries represent the
 * already-triggered state outside the barrier band:
 * </p>
 * <ul>
 *   <li>KNOCK_OUT: zero on both sides,</li>
 *   <li>KNOCK_IN: discounted cash on both sides,</li>
 *   <li>KIKO: discounted cash at the lower side, zero at the upper side,</li>
 *   <li>KOKI: zero at the lower side, discounted cash at the upper side.</li>
 * </ul>
 *
 * <p>
 * The variance-direction boundaries are left free via
 * {@link StandardBoundaryCondition#none()}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DoubleBarrierBinaryOptionHestonModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMHestonModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public DoubleBarrierBinaryOptionHestonModelBoundary(final FDMHestonModel model) {
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
				StandardBoundaryCondition.dirichlet(value),
				StandardBoundaryCondition.none()
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
				StandardBoundaryCondition.dirichlet(value),
				StandardBoundaryCondition.none()
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
