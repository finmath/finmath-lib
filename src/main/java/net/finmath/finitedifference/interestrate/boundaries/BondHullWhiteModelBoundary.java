package net.finmath.finitedifference.interestrate.boundaries;

import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.interestrate.models.FDMHullWhiteModel;
import net.finmath.finitedifference.interestrate.products.Bond;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;

/**
 * Exact boundary conditions for {@link Bond} under {@link FDMHullWhiteModel}.
 *
 * <p>
 * Since the reduced-scope {@link Bond} product consists only of deterministic
 * fixed cashflows, its value under Hull-White is known exactly as
 * </p>
 *
 * <p>
 * <i>
 * V(t,x) = \sum_{i:\,T_i \ge t} C_i P(t,T_i;x),
 * </i>
 * </p>
 *
 * <p>
 * where {@code C_i} are the remaining deterministic bond cashflows and
 * {@code P(t,T_i;x)} is the model discount bond conditional on the current
 * state variable {@code x}.
 * </p>
 *
 * <p>
 * Hence both lower and upper boundaries can be imposed by exact Dirichlet
 * conditions.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class BondHullWhiteModelBoundary implements FiniteDifferenceInterestRateBoundary {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;

	/**
	 * The model.
	 */
	private final FDMHullWhiteModel model;

	/**
	 * Creates the exact Hull-White boundary for deterministic-cashflow bonds.
	 *
	 * @param model The Hull-White model.
	 */
	public BondHullWhiteModelBoundary(final FDMHullWhiteModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 1) {
			throw new IllegalArgumentException(
					"BondHullWhiteModelBoundary requires a one-dimensional Hull-White model.");
		}

		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final Bond bond = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(
						getExactBondValue(bond, time, stateVariables[0])
						)
		};
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final Bond bond = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(
						getExactBondValue(bond, time, stateVariables[0])
						)
		};
	}

	private Bond validateAndCastProduct(final FiniteDifferenceInterestRateProduct product) {
		if (!(product instanceof Bond)) {
			throw new IllegalArgumentException(
					"BondHullWhiteModelBoundary requires a Bond product.");
		}

		return (Bond) product;
	}

	private void validateStateVariables(final double[] stateVariables) {
		if (stateVariables == null || stateVariables.length != 1) {
			throw new IllegalArgumentException("Exactly one state variable is required.");
		}
	}

	private double getExactBondValue(
			final Bond bond,
			final double time,
			final double stateVariable) {

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < bond.getSchedule().getNumberOfPeriods(); periodIndex++) {
			final double paymentTime = bond.getSchedule().getPayment(periodIndex);

			if (paymentTime < time - TIME_TOLERANCE) {
				continue;
			}

			final double cashflow = bond.getCashflow(periodIndex);
			final double discountBond = model.getDiscountBond(time, paymentTime, stateVariable);

			value += cashflow * discountBond;
		}

		return value;
	}
}
