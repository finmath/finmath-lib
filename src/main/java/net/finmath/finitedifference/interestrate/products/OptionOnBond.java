package net.finmath.finitedifference.interestrate.products;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;
import net.finmath.finitedifference.solvers.FDMThetaMethod1D;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.products.CallOrPut;

/**
 * Finite-difference valuation of a European option on a deterministic-cashflow
 * bond.
 *
 * <p>
 * The underlying bond is assumed to be a {@link Bond}, that is, a bond with
 * deterministic fixed cashflows. Let {@code T} denote the option exercise date,
 * {@code B(T,x)} the bond value at exercise as a function of the model state,
 * {@code K} the strike, and {@code \omega \in \{+1,-1\}} the call/put sign.
 * The payoff is
 * </p>
 *
 * <p>
 * <i>
 * \bigl( \omega ( B(T,x) - K ) \bigr)^{+}.
 * </i>
 * </p>
 *
 * <p>
 * Since the underlying bond has deterministic cashflows, its value at exercise
 * can be computed directly from the interest-rate model through the remaining
 * discount bonds:
 * </p>
 *
 * <p>
 * <i>
 * B(T,x) = \sum_{i:\,T_i \ge T} C_i P(T,T_i;x),
 * </i>
 * </p>
 *
 * <p>
 * where {@code C_i} are the remaining bond cashflows and {@code P(T,T_i;x)} is
 * the discount bond implied by the model.
 * </p>
 *
 * <p>
 * The current implementation is intended for one-dimensional finite-difference
 * interest-rate models used with {@link FDMThetaMethod1D}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class OptionOnBond implements FiniteDifferenceInterestRateProduct {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;

	/**
	 * The underlying bond.
	 */
	private final Bond underlyingBond;
	/**
	 * The exercise date.
	 */
	private final double exerciseDate;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The call or put.
	 */
	private final CallOrPut callOrPut;

	/**
	 * Creates a European option on a deterministic-cashflow bond.
	 *
	 * @param underlyingBond The underlying bond.
	 * @param exerciseDate The option exercise date.
	 * @param strike The strike.
	 * @param callOrPut The call/put indicator.
	 */
	public OptionOnBond(
			final Bond underlyingBond,
			final double exerciseDate,
			final double strike,
			final CallOrPut callOrPut) {

		if (underlyingBond == null) {
			throw new IllegalArgumentException("underlyingBond must not be null.");
		}
		if (exerciseDate < 0.0) {
			throw new IllegalArgumentException("exerciseDate must be non-negative.");
		}
		if (strike < 0.0) {
			throw new IllegalArgumentException("strike must be non-negative.");
		}
		if (callOrPut == null) {
			throw new IllegalArgumentException("callOrPut must not be null.");
		}
		if (exerciseDate - underlyingBond.getMaturity() > TIME_TOLERANCE) {
			throw new IllegalArgumentException(
					"exerciseDate must not be later than the maturity of the underlying bond.");
		}

		this.underlyingBond = underlyingBond;
		this.exerciseDate = exerciseDate;
		this.strike = strike;
		this.callOrPut = callOrPut;
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final FiniteDifferenceInterestRateModel model) {

		validateModel(model);

		final FDMThetaMethod1D solver = new FDMThetaMethod1D(
				model,
				this,
				model.getSpaceTimeDiscretization(),
				new EuropeanExercise(exerciseDate)
				);

		return solver.getValue(
				evaluationTime,
				exerciseDate,
				buildTerminalValues(model)
				);
	}

	@Override
	public double[][] getValues(final FiniteDifferenceInterestRateModel model) {

		validateModel(model);

		final FDMThetaMethod1D solver = new FDMThetaMethod1D(
				model,
				this,
				model.getSpaceTimeDiscretization(),
				new EuropeanExercise(exerciseDate)
				);

		return solver.getValues(
				exerciseDate,
				buildTerminalValues(model)
				);
	}

	/**
	 * Returns the underlying bond.
	 *
	 * @return The underlying bond.
	 */
	public Bond getUnderlyingBond() {
		return underlyingBond;
	}

	/**
	 * Returns the exercise date.
	 *
	 * @return The exercise date.
	 */
	public double getExerciseDate() {
		return exerciseDate;
	}

	/**
	 * Returns the strike.
	 *
	 * @return The strike.
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * Returns the call/put indicator.
	 *
	 * @return The call/put indicator.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPut;
	}

	private void validateModel(final FiniteDifferenceInterestRateModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 1) {
			throw new IllegalArgumentException(
					"OptionOnBond currently supports only one-dimensional finite-difference interest-rate models."
					);
		}
	}

	private double[] buildTerminalValues(final FiniteDifferenceInterestRateModel model) {
		final SpaceTimeDiscretization discretization = model.getSpaceTimeDiscretization();
		final double[] xGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[xGrid.length];

		for (int i = 0; i < xGrid.length; i++) {
			final double bondValueAtExercise = getUnderlyingBondValueAtExercise(model, xGrid[i]);
			terminalValues[i] = Math.max(
					callOrPut.toInteger() * (bondValueAtExercise - strike),
					0.0
					);
		}

		return terminalValues;
	}

	private double getUnderlyingBondValueAtExercise(
			final FiniteDifferenceInterestRateModel model,
			final double stateVariable) {

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < underlyingBond.getSchedule().getNumberOfPeriods(); periodIndex++) {
			final double paymentTime = underlyingBond.getSchedule().getPayment(periodIndex);

			if (paymentTime < exerciseDate - TIME_TOLERANCE) {
				continue;
			}

			final double cashflow = underlyingBond.getCashflow(periodIndex);
			value += cashflow * model.getDiscountBond(
					exerciseDate,
					paymentTime,
					stateVariable
					);
		}

		return value;
	}
}
