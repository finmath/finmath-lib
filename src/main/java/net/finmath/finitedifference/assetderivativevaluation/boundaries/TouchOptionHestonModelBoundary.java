package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.TouchOption;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.BarrierType;

/**
 * Boundary conditions for {@link TouchOption} under {@link FDMHestonModel}.
 *
 * <p>
 * State variables are assumed to be (S, v), where S is the asset level and
 * v the variance.
 * </p>
 *
 * <p>
 * Milestone scope:
 * European expiry-settled cash one-touch / no-touch only.
 * </p>
 *
 * <p>
 * The barrier is assumed to lie on the spot boundary. The variance-direction
 * boundaries are left free via {@link StandardBoundaryCondition#none()}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class TouchOptionHestonModelBoundary implements FiniteDifferenceBoundary {

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
	public TouchOptionHestonModelBoundary(final FDMHestonModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final TouchOption option = (TouchOption) product;
		final BarrierType barrierType = option.getBarrierType();

		time = Math.max(time, EPSILON);

		final BoundaryCondition[] result = new BoundaryCondition[2];

		/*
		 * Spot lower boundary.
		 *
		 * DOWN_OUT no-touch: knocked out -> 0.
		 * Otherwise (DOWN_IN one-touch, or continuation side of UP_OUT no-
		 * touch):
		 * discounted expiry cash.
		 */
		if (barrierType == BarrierType.DOWN_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					getDiscountedCashValue(option, time)
			);
		}

		/* Variance lower boundary left free. */
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final TouchOption option = (TouchOption) product;
		final BarrierType barrierType = option.getBarrierType();

		time = Math.max(time, EPSILON);

		final BoundaryCondition[] result = new BoundaryCondition[2];

		/*
		 * Spot upper boundary.
		 *
		 * UP_OUT no-touch: knocked out -> 0.
		 * Otherwise (UP_IN one-touch, or continuation side of DOWN_OUT no-
		 * touch):
		 * discounted expiry cash.
		 */
		if (barrierType == BarrierType.UP_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					getDiscountedCashValue(option, time)
			);
		}

		/* Variance upper boundary left free. */
		result[1] = StandardBoundaryCondition.none();

		return result;
	}

	private double getDiscountedCashValue(final TouchOption option, final double evaluationTime) {
		if (evaluationTime >= option.getMaturity()) {
			return option.getPayoffAmount();
		}

		final double dfTime = model.getRiskFreeCurve().getDiscountFactor(evaluationTime);
		final double dfMat = model.getRiskFreeCurve().getDiscountFactor(option.getMaturity());

		return option.getPayoffAmount() * dfMat / dfTime;
	}
}
