package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMCevModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.TouchOption;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.BarrierType;

/**
 * Boundary conditions for {@link TouchOption} under {@link FDMCevModel}.
 */
public class TouchOptionCevModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMCevModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public TouchOptionCevModelBoundary(final FDMCevModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final TouchOption option = (TouchOption) product;
		final BarrierType barrierType = option.getBarrierType();

		if (barrierType == BarrierType.DOWN_OUT) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(getDiscountedCashValue(option, time))
		};
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final TouchOption option = (TouchOption) product;
		final BarrierType barrierType = option.getBarrierType();

		if (barrierType == BarrierType.UP_OUT) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(getDiscountedCashValue(option, time))
		};
	}

	private double getDiscountedCashValue(final TouchOption option, final double time) {
		if (time >= option.getMaturity()) {
			return option.getPayoffAmount();
		}

		final double dfTime = model.getRiskFreeCurve().getDiscountFactor(time);
		final double dfMat = model.getRiskFreeCurve().getDiscountFactor(option.getMaturity());

		return option.getPayoffAmount() * dfMat / dfTime;
	}
}
