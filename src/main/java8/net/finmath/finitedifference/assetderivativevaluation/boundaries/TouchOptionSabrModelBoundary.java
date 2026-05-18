package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.assetderivativevaluation.products.TouchOption;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.BarrierType;

/**
 * Boundary conditions for {@link TouchOption} under {@link FDMSabrModel}.
 *
 * <p>
 * State variables are assumed to be (S, alpha), where S is the asset level and
 * alpha the stochastic volatility factor.
 * </p>
 *
 * <p>
 * Milestone scope:
 * European expiry-settled cash one-touch / no-touch only.
 * </p>
 *
 * <p>
 * The barrier is assumed to lie on the spot boundary. The alpha-direction
 * boundaries are left free via {@link StandardBoundaryCondition#none()}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class TouchOptionSabrModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMSabrModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public TouchOptionSabrModelBoundary(final FDMSabrModel model) {
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
		 * Otherwise: discounted expiry cash.
		 */
		if (barrierType == BarrierType.DOWN_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					getDiscountedCashValue(option, time)
					);
		}

		/* Alpha lower boundary left free. */
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
		 * Otherwise: discounted expiry cash.
		 */
		if (barrierType == BarrierType.UP_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else {
			result[0] = StandardBoundaryCondition.dirichlet(
					getDiscountedCashValue(option, time)
					);
		}

		/* Alpha upper boundary left free. */
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
