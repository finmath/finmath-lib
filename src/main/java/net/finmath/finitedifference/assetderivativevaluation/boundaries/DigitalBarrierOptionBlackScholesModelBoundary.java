package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.DigitalBarrierOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.BarrierType;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.modelling.products.DigitalPayoffType;

/**
 * Boundary conditions for {@link DigitalBarrierOption} under the
 * {@link FDMBlackScholesModel}.
 *
 * <p>
 * Conventions:
 * </p>
 * <ul>
 *   <li>For knock-out options, the barrier-side boundary is pinned to 0.0.</li>
 *   <li>On the continuation side, digital asymptotics are used.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class DigitalBarrierOptionBlackScholesModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMBlackScholesModel model;

	/**
	 * Performs the operation.
	 *
	 * @param model The value.
	 */
	public DigitalBarrierOptionBlackScholesModelBoundary(final FDMBlackScholesModel model) {
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

		if ((barrierType == BarrierType.DOWN_OUT) || (sign == CallOrPut.CALL)) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		if (payoffType == DigitalPayoffType.CASH_OR_NOTHING) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(getDiscountedCashValue(option, time))
			};
		} else if (payoffType == DigitalPayoffType.ASSET_OR_NOTHING) {
			final double stateVariable = stateVariables[0];
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(getDiscountedAssetValue(stateVariable, option, time))
			};
		} else {
			throw new IllegalArgumentException("Unsupported digital payoff type.");
		}
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

		if ((barrierType == BarrierType.UP_OUT) || (sign == CallOrPut.PUT)) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		if (payoffType == DigitalPayoffType.CASH_OR_NOTHING) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(getDiscountedCashValue(option, time))
			};
		} else if (payoffType == DigitalPayoffType.ASSET_OR_NOTHING) {
			final double stateVariable = stateVariables[0];
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(getDiscountedAssetValue(stateVariable, option, time))
			};
		} else {
			throw new IllegalArgumentException("Unsupported digital payoff type.");
		}
	}

	private double getDiscountedCashValue(final DigitalBarrierOption option, final double time) {
		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;
		final double maturity = option.getMaturity();
		return option.getCashPayoff() * Math.exp(-riskFreeRate * (maturity - time));
	}

	private double getDiscountedAssetValue(
			final double stateVariable,
			final DigitalBarrierOption option,
			final double time) {

		final double discountFactorDividend = model.getDividendYieldCurve().getDiscountFactor(time);
		final double dividendYieldRate = -Math.log(discountFactorDividend) / time;
		final double maturity = option.getMaturity();
		return stateVariable * Math.exp(-dividendYieldRate * (maturity - time));
	}
}
