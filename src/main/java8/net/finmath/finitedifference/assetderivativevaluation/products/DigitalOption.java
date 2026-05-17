package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.FiniteDifferenceExerciseUtil;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMBachelierModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMCevModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.FDMSolverFactory;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.modelling.products.DigitalPayoffType;
import net.finmath.time.TimeDiscretization;

/**
 * Digital option supporting cash-or-nothing and asset-or-nothing payoffs.
 *
 * <p>
 * The class supports multiple exercise styles through the generic
 * {@link Exercise} field. European exercise is the default when no explicit
 * exercise object is provided.
 * </p>
 *
 * <p>
 * For one-dimensional models, the terminal payoff is initialized using
 * cell-averaged values in order to reduce grid bias caused by the payoff
 * discontinuity at the strike.
 * </p>
 *
 * <p>
 * For Bermudan and American exercise in one-dimensional models, the maturity
 * layer remains cell-averaged, while exercise projection remains pointwise.
 * </p>
 *
 * <p>
 * For the current two-dimensional models, the payoff is initialized pointwise
 * on the first state variable, that is, on the asset dimension.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class DigitalOption implements FiniteDifferenceEquityProduct {

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
	 * The digital payoff type.
	 */
	private final DigitalPayoffType digitalPayoffType;
	/**
	 * The cash payoff.
	 */
	private final double cashPayoff;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	/**
	 * Creates a digital option with explicit exercise specification.
	 *
	 * @param underlyingName Name of the underlying.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param callOrPutSign Call or put flag.
	 * @param digitalPayoffType Type of digital payoff.
	 * @param cashPayoff Cash payoff for cash-or-nothing products.
	 * @param exercise Exercise specification.
	 */
	public DigitalOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise) {

		if (callOrPutSign == null) {
			throw new IllegalArgumentException("Option type must not be null.");
		}
		if (digitalPayoffType == null) {
			throw new IllegalArgumentException("Digital payoff type must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("Exercise must not be null.");
		}
		if (maturity < 0.0) {
			throw new IllegalArgumentException("Maturity must be non-negative.");
		}
		if (digitalPayoffType == DigitalPayoffType.CASH_OR_NOTHING && cashPayoff < 0.0) {
			throw new IllegalArgumentException("Cash payoff must be non-negative.");
		}

		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
		this.callOrPutSign = callOrPutSign;
		this.digitalPayoffType = digitalPayoffType;
		this.cashPayoff = digitalPayoffType == DigitalPayoffType.CASH_OR_NOTHING ? cashPayoff : Double.NaN;
		this.exercise = exercise;
	}

	/**
	 * Creates a European digital option.
	 *
	 * @param underlyingName Name of the underlying.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param callOrPutSign Call or put flag.
	 * @param digitalPayoffType Type of digital payoff.
	 * @param cashPayoff Cash payoff for cash-or-nothing products.
	 */
	public DigitalOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff) {
		this(
				underlyingName,
				maturity,
				strike,
				callOrPutSign,
				digitalPayoffType,
				cashPayoff,
				new EuropeanExercise(maturity)
		);
	}

	/**
	 * Creates a digital option with explicit exercise specification.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param callOrPutSign Call or put flag.
	 * @param digitalPayoffType Type of digital payoff.
	 * @param cashPayoff Cash payoff for cash-or-nothing products.
	 * @param exercise Exercise specification.
	 */
	public DigitalOption(
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise) {
		this(null, maturity, strike, callOrPutSign, digitalPayoffType, cashPayoff, exercise);
	}

	/**
	 * Creates a European digital option.
	 *
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param callOrPutSign Call or put flag.
	 * @param digitalPayoffType Type of digital payoff.
	 * @param cashPayoff Cash payoff for cash-or-nothing products.
	 */
	public DigitalOption(
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff) {
		this(null, maturity, strike, callOrPutSign, digitalPayoffType, cashPayoff, new EuropeanExercise(maturity));
	}

	/**
	 * Creates a digital option with explicit exercise specification.
	 *
	 * @param underlyingName Name of the underlying.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param callOrPutSign +1 for call, -1 for put.
	 * @param digitalPayoffType Type of digital payoff.
	 * @param cashPayoff Cash payoff for cash-or-nothing products.
	 * @param exercise Exercise specification.
	 */
	public DigitalOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double callOrPutSign,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff,
			final Exercise exercise) {
		this(
				underlyingName,
				maturity,
				strike,
				mapCallOrPut(callOrPutSign),
				digitalPayoffType,
				cashPayoff,
				exercise
		);
	}

	/**
	 * Creates a European digital option.
	 *
	 * @param underlyingName Name of the underlying.
	 * @param maturity Option maturity.
	 * @param strike Option strike.
	 * @param callOrPutSign +1 for call, -1 for put.
	 * @param digitalPayoffType Type of digital payoff.
	 * @param cashPayoff Cash payoff for cash-or-nothing products.
	 */
	public DigitalOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double callOrPutSign,
			final DigitalPayoffType digitalPayoffType,
			final double cashPayoff) {
		this(
				underlyingName,
				maturity,
				strike,
				mapCallOrPut(callOrPutSign),
				digitalPayoffType,
				cashPayoff,
				new EuropeanExercise(maturity)
		);
	}

	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization discretization = getEffectiveSpaceTimeDiscretization(model);
		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, discretization, exercise);

		if (isOneDimensionalModel(model)) {
			final double[] terminalValues = buildCellAveragedTerminalValues(discretization);

			if (exercise.isEuropean()) {
				return solver.getValue(evaluationTime, maturity, terminalValues);
			} else {
				return solver.getValue(
						evaluationTime,
						maturity,
						terminalValues,
						getExercisePayoffFunction()
				);
			}
		}

		return solver.getValue(evaluationTime, maturity, getExercisePayoffFunction());
	}

	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization discretization = getEffectiveSpaceTimeDiscretization(model);
		final FDMSolver solver = FDMSolverFactory.createSolver(model, this, discretization, exercise);

		if (isOneDimensionalModel(model)) {
			final double[] terminalValues = buildCellAveragedTerminalValues(discretization);

			if (exercise.isEuropean()) {
				return solver.getValues(maturity, terminalValues);
			} else {
				return solver.getValues(
						maturity,
						terminalValues,
						getExercisePayoffFunction()
				);
			}
		}

		return solver.getValues(maturity, getExercisePayoffFunction());
	}

	/**
	 * Returns the effective space-time discretization.
	 *
	 * <p>
	 * Bermudan exercise dates are inserted exactly into the time
	 * discretization.
	 * European and American exercise keep the model discretization unchanged.
	 * </p>
	 *
	 * @param model The finite-difference model.
	 * @return The effective space-time discretization.
	 */
	private SpaceTimeDiscretization getEffectiveSpaceTimeDiscretization(final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization base = model.getSpaceTimeDiscretization();

		if (!exercise.isBermudan()) {
			return base;
		}

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

	/**
	 * Builds the cell-averaged terminal values on the one-dimensional spot
	 * grid.
	 *
	 * @param discretization The effective discretization.
	 * @return Terminal values on the spot grid.
	 */
	private double[] buildCellAveragedTerminalValues(final SpaceTimeDiscretization discretization) {

		final double[] sGrid = discretization.getSpaceGrid(0).getGrid();
		final double[] terminalValues = new double[sGrid.length];

		for (int i = 0; i < sGrid.length; i++) {
			final double leftEdge = getLeftDualCellEdge(sGrid, i);
			final double rightEdge = getRightDualCellEdge(sGrid, i);
			terminalValues[i] = cellAveragedPayoff(leftEdge, rightEdge);
		}

		return terminalValues;
	}

	/**
	 * Returns the payoff function used for exercise projection.
	 *
	 * <p>
	 * This payoff is pointwise and uses strict inequalities at the strike
	 * in order to avoid a one-node bias exactly at the discontinuity.
	 * </p>
	 *
	 * @return Pointwise exercise payoff function.
	 */
	private DoubleUnaryOperator getExercisePayoffFunction() {
		return this::pointwisePayoff;
	}

	/**
	 * Pointwise digital payoff used for exercise decisions.
	 *
	 * @param assetValue Underlying value.
	 * @return Pointwise payoff.
	 */
	private double pointwisePayoff(final double assetValue) {

		final boolean inTheMoney =
				callOrPutSign == CallOrPut.CALL
						? assetValue > strike
						: assetValue < strike;

		if (!inTheMoney) {
			return 0.0;
		}

		switch (digitalPayoffType) {
		case CASH_OR_NOTHING:
			return cashPayoff;
		case ASSET_OR_NOTHING:
			return assetValue;
		default:
			throw new IllegalStateException("Unsupported digital payoff type.");
		}
	}

	/**
	 * Returns the payoff averaged over a cell.
	 *
	 * @param leftEdge Left cell edge.
	 * @param rightEdge Right cell edge.
	 * @return Cell-averaged payoff.
	 */
	private double cellAveragedPayoff(final double leftEdge, final double rightEdge) {

		if (!(leftEdge < rightEdge)) {
			throw new IllegalArgumentException("Require leftEdge < rightEdge.");
		}

		switch (digitalPayoffType) {
		case CASH_OR_NOTHING:
			return cellAveragedCashDigital(leftEdge, rightEdge);
		case ASSET_OR_NOTHING:
			return cellAveragedAssetDigital(leftEdge, rightEdge);
		default:
			throw new IllegalStateException("Unsupported digital payoff type.");
		}
	}

	/**
	 * Cell average for a cash-or-nothing digital payoff.
	 *
	 * @param leftEdge Left cell edge.
	 * @param rightEdge Right cell edge.
	 * @return Cell-averaged payoff.
	 */
	private double cellAveragedCashDigital(final double leftEdge, final double rightEdge) {

		final double cellLength = rightEdge - leftEdge;

		if (callOrPutSign == CallOrPut.CALL) {
			if (rightEdge <= strike) {
				return 0.0;
			}
			if (leftEdge >= strike) {
				return cashPayoff;
			}
			return cashPayoff * (rightEdge - strike) / cellLength;
		} else {
			if (rightEdge <= strike) {
				return cashPayoff;
			}
			if (leftEdge >= strike) {
				return 0.0;
			}
			return cashPayoff * (strike - leftEdge) / cellLength;
		}
	}

	/**
	 * Cell average for an asset-or-nothing digital payoff.
	 *
	 * @param leftEdge Left cell edge.
	 * @param rightEdge Right cell edge.
	 * @return Cell-averaged payoff.
	 */
	private double cellAveragedAssetDigital(final double leftEdge, final double rightEdge) {

		final double cellLength = rightEdge - leftEdge;

		if (callOrPutSign == CallOrPut.CALL) {
			if (rightEdge <= strike) {
				return 0.0;
			}
			if (leftEdge >= strike) {
				return 0.5 * (leftEdge + rightEdge);
			}
			return (rightEdge * rightEdge - strike * strike) / (2.0 * cellLength);
		} else {
			if (rightEdge <= strike) {
				return 0.5 * (leftEdge + rightEdge);
			}
			if (leftEdge >= strike) {
				return 0.0;
			}
			return (strike * strike - leftEdge * leftEdge) / (2.0 * cellLength);
		}
	}

	/**
	 * Returns the left edge of the dual cell around node i.
	 *
	 * @param grid Spot grid.
	 * @param i Node index.
	 * @return Left dual-cell edge.
	 */
	private double getLeftDualCellEdge(final double[] grid, final int i) {
		if (i == 0) {
			return grid[0];
		}
		return 0.5 * (grid[i - 1] + grid[i]);
	}

	/**
	 * Returns the right edge of the dual cell around node i.
	 *
	 * @param grid Spot grid.
	 * @param i Node index.
	 * @return Right dual-cell edge.
	 */
	private double getRightDualCellEdge(final double[] grid, final int i) {
		if (i == grid.length - 1) {
			return grid[grid.length - 1];
		}
		return 0.5 * (grid[i] + grid[i + 1]);
	}

	/**
	 * Checks whether the model is one-dimensional.
	 *
	 * @param model The model.
	 * @return True if the model is one-dimensional.
	 */
	private boolean isOneDimensionalModel(final FiniteDifferenceEquityModel model) {
		return model instanceof FDMBlackScholesModel
				|| model instanceof FDMCevModel
				|| model instanceof FDMBachelierModel;
	}

	private static CallOrPut mapCallOrPut(final double callOrPutSign) {
		if (callOrPutSign == 1.0) {
			return CallOrPut.CALL;
		}
		if (callOrPutSign == -1.0) {
			return CallOrPut.PUT;
		}
		throw new IllegalArgumentException("Unknown option type.");
	}

	/**
	 * Returns the underlying name.
	 *
	 * @return The underlying name.
	 */
	public String getUnderlyingName() {
		return underlyingName;
	}

	/**
	 * Returns the maturity.
	 *
	 * @return The maturity.
	 */
	public double getMaturity() {
		return maturity;
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
	 * Returns call/put type.
	 *
	 * @return The option type.
	 */
	public CallOrPut getCallOrPut() {
		return callOrPutSign;
	}

	/**
	 * Returns the digital payoff type.
	 *
	 * @return The digital payoff type.
	 */
	public DigitalPayoffType getDigitalPayoffType() {
		return digitalPayoffType;
	}

	/**
	 * Returns the cash payoff amount.
	 *
	 * @return The cash payoff amount.
	 */
	public double getCashPayoff() {
		return cashPayoff;
	}

	/**
	 * Returns the exercise specification.
	 *
	 * @return The exercise specification.
	 */
	public Exercise getExercise() {
		return exercise;
	}
}
