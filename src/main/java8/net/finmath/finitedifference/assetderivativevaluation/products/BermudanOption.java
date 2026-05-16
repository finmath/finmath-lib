package net.finmath.finitedifference.assetderivativevaluation.products;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.modelling.BermudanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.time.TimeDiscretization;

/**
 * Finite difference valuation of a Bermudan option on a single asset.
 *
 * <p>
 * Exercise times are specified in running time and converted internally to the
 * solver's time-to-maturity coordinates through
 * {@link FiniteDifferenceExerciseUtil}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class BermudanOption implements FiniteDifferenceEquityProduct {

	/**
	 * The underlying name.
	 */
	private final String underlyingName;
	/**
	 * The maturity.
	 */
	private final double maturity;
	/**
	 * The strike.
	 */
	private final double strike;
	/**
	 * The call or put sign.
	 */
	private final CallOrPut callOrPutSign;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param exerciseTimes The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public BermudanOption(
			final String underlyingName,
			final double[] exerciseTimes,
			final double strike,
			final double callOrPutSign) {
		this(
				underlyingName,
				exerciseTimes,
				strike,
				mapCallOrPut(callOrPutSign)
		);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param exerciseTimes The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public BermudanOption(
			final String underlyingName,
			final double[] exerciseTimes,
			final double strike,
			final CallOrPut callOrPutSign) {

		if (exerciseTimes == null || exerciseTimes.length == 0) {
			throw new IllegalArgumentException("Exercise times must not be null or empty.");
		}

		this.underlyingName = underlyingName;
		this.exercise = new BermudanExercise(exerciseTimes);
		this.maturity = this.exercise.getMaturity();
		this.strike = strike;
		this.callOrPutSign = callOrPutSign;
	}

	/**
	 * Performs the operation.
	 *
	 * @param exerciseTimes The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public BermudanOption(
			final double[] exerciseTimes,
			final double strike,
			final double callOrPutSign) {
		this(null, exerciseTimes, strike, callOrPutSign);
	}

	/**
	 * Performs the operation.
	 *
	 * @param exerciseTimes The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public BermudanOption(
			final double[] exerciseTimes,
			final double strike,
			final CallOrPut callOrPutSign) {
		this(null, exerciseTimes, strike, callOrPutSign);
	}

	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization refinedDiscretization = getRefinedSpaceTimeDiscretization(model);
		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, refinedDiscretization, exercise);

		if (callOrPutSign == CallOrPut.CALL) {
			return solver.getValue(
					evaluationTime,
					maturity,
					assetValue -> Math.max(assetValue - strike, 0.0)
			);
		} else {
			return solver.getValue(
					evaluationTime,
					maturity,
					assetValue -> Math.max(strike - assetValue, 0.0)
			);
		}
	}

	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization refinedDiscretization = getRefinedSpaceTimeDiscretization(model);
		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, refinedDiscretization, exercise);

		if (callOrPutSign == CallOrPut.CALL) {
			return solver.getValues(maturity, assetValue -> Math.max(assetValue - strike, 0.0));
		} else {
			return solver.getValues(maturity, assetValue -> Math.max(strike - assetValue, 0.0));
		}
	}

	private SpaceTimeDiscretization getRefinedSpaceTimeDiscretization(final FiniteDifferenceEquityModel model) {
		final SpaceTimeDiscretization base = model.getSpaceTimeDiscretization();

		final TimeDiscretization refinedTimeDiscretization =
				FiniteDifferenceExerciseUtil.refineTimeDiscretization(
						base.getTimeDiscretization(),
						exercise
				);

		if (base.getNumberOfSpaceGrids() == 1) {
			return new SpaceTimeDiscretization(
					base.getSpaceGrid(0),
					refinedTimeDiscretization,
					base.getTheta(),
					new double[] {base.getCenter(0) }
			);
		}

		final int numberOfSpaceGrids = base.getNumberOfSpaceGrids();
		final Grid[] spaceGrids = new Grid[numberOfSpaceGrids];
		final double[] center = new double[numberOfSpaceGrids];

		for (int i = 0; i < numberOfSpaceGrids; i++) {
			spaceGrids[i] = base.getSpaceGrid(i);
			center[i] = base.getCenter(i);
		}

		return new SpaceTimeDiscretization(
				spaceGrids,
				refinedTimeDiscretization,
				base.getTheta(),
				center
		);
	}

	private static CallOrPut mapCallOrPut(final double callOrPutSign) {
		if (callOrPutSign == 1.0) {
			return CallOrPut.CALL;
		}
		if (callOrPutSign == -1.0) {
			return CallOrPut.PUT;
		}
		throw new IllegalArgumentException("Unknown option type");
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public String getUnderlyingName() {
		return underlyingName;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getMaturity() {
		return maturity;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPutSign;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public Exercise getExercise() {
		return exercise;
	}
}
