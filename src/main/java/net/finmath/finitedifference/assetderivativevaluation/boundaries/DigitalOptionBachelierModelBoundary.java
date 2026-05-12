package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.models.FDMBachelierModel;
import net.finmath.finitedifference.assetderivativevaluation.products.DigitalOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.modelling.products.DigitalPayoffType;

/**
 * Boundary conditions for {@link DigitalOption} under the
 * {@link FDMBachelierModel}.
 *
 * <p>
 * The boundary asymptotics depend on the exercise style:
 * </p>
 * <ul>
 *   <li>European exercise: discounted continuation asymptotics.</li>
 *   <li>Bermudan / American exercise: early-exercise asymptotics.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class DigitalOptionBachelierModelBoundary implements FiniteDifferenceBoundary {

	/**
	 * The epsilon.
	 */
	private static final double EPSILON = 1E-6;

	/**
	 * The model.
	 */
	private final FDMBachelierModel model;

	/**
	 * Creates the boundary condition associated with a given
	 * {@link FDMBachelierModel}.
	 *
	 * @param model The Bachelier model used to determine
	 *              risk-free and dividend discount factors.
	 */
	public DigitalOptionBachelierModelBoundary(final FDMBachelierModel model) {
		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			double time,
			final double... stateVariables) {

		final DigitalOption option = (DigitalOption) product;
		final CallOrPut sign = option.getCallOrPut();
		final DigitalPayoffType payoffType = option.getDigitalPayoffType();
		final Exercise exercise = option.getExercise();

		if (sign == CallOrPut.CALL) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		if (payoffType == DigitalPayoffType.CASH_OR_NOTHING) {
			final double value = isEuropeanExercise(exercise)
					? getDiscountedCashValue(option, time)
					: option.getCashPayoff();

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
			};
		} else if (payoffType == DigitalPayoffType.ASSET_OR_NOTHING) {
			final double stateVariable = stateVariables[0];
			final double value = isEuropeanExercise(exercise)
					? getDiscountedAssetValue(stateVariable, option, time)
					: stateVariable;

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
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

		final DigitalOption option = (DigitalOption) product;
		final CallOrPut sign = option.getCallOrPut();
		final DigitalPayoffType payoffType = option.getDigitalPayoffType();
		final Exercise exercise = option.getExercise();

		if (sign == CallOrPut.PUT) {
			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(0.0)
			};
		}

		time = Math.max(time, EPSILON);

		if (payoffType == DigitalPayoffType.CASH_OR_NOTHING) {
			final double value = isEuropeanExercise(exercise)
					? getDiscountedCashValue(option, time)
					: option.getCashPayoff();

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
			};
		} else if (payoffType == DigitalPayoffType.ASSET_OR_NOTHING) {
			final double stateVariable = stateVariables[0];
			final double value = isEuropeanExercise(exercise)
					? getDiscountedAssetValue(stateVariable, option, time)
					: stateVariable;

			return new BoundaryCondition[] {
					StandardBoundaryCondition.dirichlet(value)
			};
		} else {
			throw new IllegalArgumentException("Unsupported digital payoff type.");
		}
	}

	/**
	 * Returns true if the exercise is European.
	 *
	 * @param exercise The exercise specification.
	 * @return True if and only if the exercise is European.
	 */
	private boolean isEuropeanExercise(final Exercise exercise) {
		return exercise != null && exercise.isEuropean();
	}

	/**
	 * Returns the discounted cash payoff for a European cash-or-nothing
	 * digital.
	 *
	 * @param option The digital option.
	 * @param time The current model time.
	 * @return Discounted cash payoff.
	 */
	private double getDiscountedCashValue(final DigitalOption option, final double time) {

		final double discountFactorRiskFree = model.getRiskFreeCurve().getDiscountFactor(time);
		final double riskFreeRate = -Math.log(discountFactorRiskFree) / time;

		final double maturity = option.getMaturity();

		return option.getCashPayoff() * Math.exp(-riskFreeRate * (maturity - time));
	}

	/**
	 * Returns the discounted stock asymptotic value for a European
	 * asset-or-nothing digital.
	 *
	 * @param stateVariable The boundary stock level.
	 * @param option The digital option.
	 * @param time The current model time.
	 * @return Discounted stock value.
	 */
	private double getDiscountedAssetValue(
			final double stateVariable,
			final DigitalOption option,
			final double time) {

		final double discountFactorDividend = model.getDividendYieldCurve().getDiscountFactor(time);
		final double dividendYieldRate = -Math.log(discountFactorDividend) / time;

		final double maturity = option.getMaturity();

		return stateVariable * Math.exp(-dividendYieldRate * (maturity - time));
	}
}
