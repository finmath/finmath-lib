package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBachelierModel;
import net.finmath.finitedifference.assetderivativevaluation.products.EuropeanOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.products.CallOrPut;

/**
 * Boundary conditions for {@link EuropeanOption} under the
 * {@link FDMBachelierModel}.
 *
 * <p>
 * This class supports both the legacy boundary API returning {@code double[]}
 * and the newer explicit boundary-condition API returning
 * {@link BoundaryCondition} objects.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class EuropeanOptionBachelierModelBoundary
		implements FiniteDifferenceBoundary {

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
	public EuropeanOptionBachelierModelBoundary(final FDMBachelierModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final EuropeanOption option = (EuropeanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		if (sign == CallOrPut.CALL) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		} else {
			time = Math.max(time, EPSILON);

			final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
			final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

			final double strike = option.getStrike();
			final double maturity = option.getMaturity();

			final double value = strike * Math.exp(-riskFreeRate * (maturity - time));

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
			};
		}
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final EuropeanOption option = (EuropeanOption) product;
		final CallOrPut sign = option.getCallOrPut();

		if (sign == CallOrPut.CALL) {
			time = Math.max(time, EPSILON);

			final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
			final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

			final double strike = option.getStrike();
			final double maturity = option.getMaturity();
			final double stateVariable = stateVariables[0];

			final double value = stateVariable - strike * Math.exp(-riskFreeRate * (maturity - time));

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
			};
		} else {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}
	}
}
