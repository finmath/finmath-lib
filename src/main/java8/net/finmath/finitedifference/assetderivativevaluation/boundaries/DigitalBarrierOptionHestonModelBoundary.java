package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.DigitalBarrierOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.BarrierType;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.modelling.products.DigitalPayoffType;

/**
 * Boundary conditions for {@link DigitalBarrierOption} under the
 * {@link FDMHestonModel}.
 *
 * <p>
 * State variables are assumed to be (S, v), where S is the asset level and v
 * the variance.
 * The second-state-variable boundaries are left free.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DigitalBarrierOptionHestonModelBoundary implements FiniteDifferenceBoundary {

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
	public DigitalBarrierOptionHestonModelBoundary(final FDMHestonModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final DigitalBarrierOption option = (DigitalBarrierOption) product;
		final BarrierType barrierType = option.getBarrierType();
		final CallOrPut sign = option.getCallOrPut();
		final DigitalPayoffType payoffType = option.getDigitalPayoffType();

		time = Math.max(time, EPSILON);

		final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;
		final BoundaryCondition[] result = new BoundaryCondition[2];

		if (barrierType == BarrierType.DOWN_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else if (sign == CallOrPut.CALL) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else if (payoffType == DigitalPayoffType.CASH_OR_NOTHING) {
			result[0] = StandardBoundaryCondition.dirichlet(getDiscountedCashValue(option, time));
		} else if (payoffType == DigitalPayoffType.ASSET_OR_NOTHING) {
			result[0] = StandardBoundaryCondition.dirichlet(getDiscountedAssetValue(stock, option, time));
		} else {
			throw new IllegalArgumentException("Unsupported digital payoff type.");
		}

		result[1] = StandardBoundaryCondition.none();
		return result;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final DigitalBarrierOption option = (DigitalBarrierOption) product;
		final BarrierType barrierType = option.getBarrierType();
		final CallOrPut sign = option.getCallOrPut();
		final DigitalPayoffType payoffType = option.getDigitalPayoffType();

		time = Math.max(time, EPSILON);

		final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;
		final BoundaryCondition[] result = new BoundaryCondition[2];

		if (barrierType == BarrierType.UP_OUT) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else if (sign == CallOrPut.PUT) {
			result[0] = StandardBoundaryCondition.dirichlet(0.0);
		} else if (payoffType == DigitalPayoffType.CASH_OR_NOTHING) {
			result[0] = StandardBoundaryCondition.dirichlet(getDiscountedCashValue(option, time));
		} else if (payoffType == DigitalPayoffType.ASSET_OR_NOTHING) {
			result[0] = StandardBoundaryCondition.dirichlet(getDiscountedAssetValue(stock, option, time));
		} else {
			throw new IllegalArgumentException("Unsupported digital payoff type.");
		}

		result[1] = StandardBoundaryCondition.none();
		return result;
	}

	private double getDiscountedCashValue(final DigitalBarrierOption option, final double evaluationTime) {
		final double maturity = option.getMaturity();
		final double tau = Math.max(maturity - evaluationTime, 0.0);
		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(tau);
		return option.getCashPayoff() * discountFactorRiskFree;
	}

	private double getDiscountedAssetValue(
			final double stock,
			final DigitalBarrierOption option,
			final double evaluationTime) {

		final double maturity = option.getMaturity();
		final double tau = Math.max(maturity - evaluationTime, 0.0);
		final double discountFactorDividend = model.getDividendYieldCurve().getDiscountFactor(tau);
		return stock * discountFactorDividend;
	}
}
