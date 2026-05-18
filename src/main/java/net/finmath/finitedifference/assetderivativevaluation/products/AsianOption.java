package net.finmath.finitedifference.assetderivativevaluation.products;

import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;

import net.finmath.finitedifference.assetderivativevaluation.boundaries.FiniteDifferenceBoundary;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMBachelierModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMCevModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMSabrModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.solvers.FDMSolver;
import net.finmath.finitedifference.solvers.adi.AbstractADI2D;
import net.finmath.finitedifference.solvers.adi.AbstractADI3D;
import net.finmath.finitedifference.solvers.adi.FDMAsianADI2D;
import net.finmath.finitedifference.solvers.adi.FDMAsianHestonADI3D;
import net.finmath.finitedifference.solvers.adi.FDMAsianSabrADI3D;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.modelling.products.AsianStrike;
import net.finmath.modelling.products.CallOrPut;

/**
 * Arithmetic Asian option priced via a Markov lift.
 *
 * <p>
 * We lift the 1D Black-Scholes state S to the 2D Markov state (S, I) where
 * I(t) = integral_0^t S(u) du, and solve a 2D PDE.
 * </p>
 *
 * <p>
 * Fixed-strike payoff at maturity T:
 * Call: max(I(T)/T - K, 0)
 * Put : max(K - I(T)/T, 0)
 * </p>
 *
 * <p>
 * Floating-strike payoff at maturity T:
 * Call: max(S(T) - I(T)/T, 0)
 * Put : max(I(T)/T - S(T), 0)
 * </p>
 *
 * <p>
 * For early exercise, the immediate exercise payoff is evaluated using
 * the average accrued up to exercise time t, i.e. I(t) / t.
 * </p>
 *
 * <p>
 * Assumption: averaging times coincide with the PDE grid's time discretization.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class AsianOption implements FiniteDifferenceEquityProduct {

	/**
	 * The time epsilon for exercise.
	 */
	private static final double TIME_EPSILON_FOR_EXERCISE = 1E-12;

	/**
	 * Minimum time used to avoid division by zero.
	 */
	private static final double MINIMUM_TIME = 1.0E-6;

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
	 * The asian strike.
	 */
	private final AsianStrike asianStrike;
	/**
	 * The exercise.
	 */
	private final Exercise exercise;

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 * @param asianStrike The value.
	 * @param exercise The value.
	 */
	public AsianOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign,
			final AsianStrike asianStrike,
			final Exercise exercise) {

		if (callOrPutSign == null) {
			throw new IllegalArgumentException("Option type must not be null.");
		}
		if (asianStrike == null) {
			throw new IllegalArgumentException("Asian strike type must not be null.");
		}
		if (exercise == null) {
			throw new IllegalArgumentException("Exercise must not be null.");
		}
		if (asianStrike == AsianStrike.FIXED_STRIKE && Double.isNaN(strike)) {
			throw new IllegalArgumentException("Strike must be specified for fixed-strike Asian options.");
		}

		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
		this.callOrPutSign = callOrPutSign;
		this.asianStrike = asianStrike;
		this.exercise = exercise;
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public AsianOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign) {
		this(
				underlyingName,
				maturity,
				strike,
				callOrPutSign,
				AsianStrike.FIXED_STRIKE,
				new EuropeanExercise(maturity));
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public AsianOption(
			final String underlyingName,
			final double maturity,
			final double strike,
			final double callOrPutSign) {
		this(underlyingName, maturity, strike, callOrPutFromDouble(callOrPutSign));
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public AsianOption(
			final double maturity,
			final double strike,
			final CallOrPut callOrPutSign) {
		this(null, maturity, strike, callOrPutSign);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 * @param callOrPutSign The value.
	 */
	public AsianOption(
			final double maturity,
			final double strike,
			final double callOrPutSign) {
		this(null, maturity, strike, callOrPutSign);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param strike The value.
	 */
	public AsianOption(
			final String underlyingName,
			final double maturity,
			final double strike) {
		this(underlyingName, maturity, strike, CallOrPut.CALL);
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param strike The value.
	 */
	public AsianOption(
			final double maturity,
			final double strike) {
		this(null, maturity, strike, CallOrPut.CALL);
	}

	/**
	 * Performs the operation.
	 *
	 * @param underlyingName The value.
	 * @param maturity The value.
	 * @param callOrPutSign The value.
	 * @param asianStrike The value.
	 */
	public AsianOption(
			final String underlyingName,
			final double maturity,
			final CallOrPut callOrPutSign,
			final AsianStrike asianStrike) {
		this(
				underlyingName,
				maturity,
				asianStrike == AsianStrike.FLOATING_STRIKE ? Double.NaN : 0.0,
						callOrPutSign,
						asianStrike,
						new EuropeanExercise(maturity));
	}

	/**
	 * Performs the operation.
	 *
	 * @param maturity The value.
	 * @param callOrPutSign The value.
	 * @param asianStrike The value.
	 */
	public AsianOption(
			final double maturity,
			final CallOrPut callOrPutSign,
			final AsianStrike asianStrike) {
		this(null, maturity, callOrPutSign, asianStrike);
	}

	private static CallOrPut callOrPutFromDouble(final double callOrPutSign) {
		if (callOrPutSign == 1.0) {
			return CallOrPut.CALL;
		} else if (callOrPutSign == -1.0) {
			return CallOrPut.PUT;
		} else {
			throw new IllegalArgumentException("Unknown option type.");
		}
	}

	private double payoff(final double average, final double spot) {
		if (asianStrike == AsianStrike.FIXED_STRIKE) {
			if (callOrPutSign == CallOrPut.CALL) {
				return Math.max(average - strike, 0.0);
			} else {
				return Math.max(strike - average, 0.0);
			}
		} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
			if (callOrPutSign == CallOrPut.CALL) {
				return Math.max(spot - average, 0.0);
			} else {
				return Math.max(average - spot, 0.0);
			}
		} else {
			throw new IllegalArgumentException("Unrecognized strike type.");
		}
	}

	private double getExercisePayoff(final double runningTime, final double spot, final double integral) {
		final double tSafe = Math.max(runningTime, TIME_EPSILON_FOR_EXERCISE);
		final double averageSoFar = integral / tSafe;
		return payoff(averageSoFar, spot);
	}

	@Override
	public double[] getValue(final double evaluationTime, final FiniteDifferenceEquityModel model) {
		final double[][] values = getValues(model);

		final double tau = maturity - evaluationTime;
		final int timeIndex = model.getSpaceTimeDiscretization().getTimeDiscretization()
				.getTimeIndexNearestLessOrEqual(tau);

		final double[] column = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			column[i] = values[i][timeIndex];
		}
		return column;
	}

	@Override
	public double[][] getValues(final FiniteDifferenceEquityModel model) {

		final FiniteDifferenceEquityModel liftedModel = getLiftedModel(model);
		final FDMSolver solver = getSolver(liftedModel);

		final DoubleBinaryOperator payoffAtMaturity2D = (S, I) -> {
			final double averageAtMaturity = I / maturity;
			return payoff(averageAtMaturity, S);
		};

		final AbstractADI2D.DoubleTernaryOperator exercisePayoff2D = (runningTime, S, I) ->
		getExercisePayoff(runningTime, S, I);

		final AbstractADI3D.DoubleTernaryOperator payoffAtMaturity3D = (S, v, I) -> {
			final double averageAtMaturity = I / maturity;
			return payoff(averageAtMaturity, S);
		};

		final AbstractADI3D.DoubleQuaternaryOperator exercisePayoff3D = (runningTime, S, v, I) ->
		getExercisePayoff(runningTime, S, I);

		if (solver instanceof AbstractADI2D) {
			return ((AbstractADI2D) solver).getValues(maturity, payoffAtMaturity2D, exercisePayoff2D);
		} else if (solver instanceof AbstractADI3D) {
			return ((AbstractADI3D) solver).getValues(maturity, payoffAtMaturity3D, exercisePayoff3D);
		}

		throw new IllegalArgumentException("Unsupported solver type for AsianOption.");
	}

	/**
	 * Returns the value.
	 *
	 * @param model The value.
	 * @return The value.
	 */
	public FDMSolver getSolver(final FiniteDifferenceEquityModel model) {
		if (model instanceof LiftedFDMBlackScholesModelDecorator
				|| model instanceof LiftedFDMCevModelDecorator
				|| model instanceof LiftedFDMBachelierModelDecorator) {
			return new FDMAsianADI2D(model, this, model.getSpaceTimeDiscretization(), exercise);
		} else if (model instanceof LiftedFDMHestonModelDecorator) {
			return new FDMAsianHestonADI3D(model, this, model.getSpaceTimeDiscretization(), exercise);
		} else if (model instanceof LiftedFDMSabrModelDecorator) {
			return new FDMAsianSabrADI3D(model, this, model.getSpaceTimeDiscretization(), exercise);
		}

		throw new IllegalArgumentException(
				"AsianOption currently supports only FDMBlackScholesModel, FDMCevModel, FDMBachelierModel and FDMHestonModel.");
	}

	/**
	 * Returns the value.
	 *
	 * @param model The value.
	 * @return The value.
	 */
	public FiniteDifferenceEquityModel getLiftedModel(final FiniteDifferenceEquityModel model) {

		final SpaceTimeDiscretization baseDiscretization = model.getSpaceTimeDiscretization();

		if (model instanceof FDMBlackScholesModel) {
			final FDMBlackScholesModel bsModel = (FDMBlackScholesModel) model;

			final Grid sGrid = baseDiscretization.getSpaceGrid(0);
			final double[] sNodes = sGrid.getGrid();
			final double sMax = sNodes[sNodes.length - 1];

			final double iMax = maturity * sMax;
			final int nI = sNodes.length * 4;
			final Grid iGrid = new UniformGrid(nI - 1, 0.0, iMax);

			final SpaceTimeDiscretization liftedDiscretization = new SpaceTimeDiscretization(
					new Grid[] {sGrid, iGrid },
					baseDiscretization.getTimeDiscretization(),
					baseDiscretization.getTheta(),
					new double[] {bsModel.getInitialValue()[0], 0.0 });

			return new LiftedFDMBlackScholesModelDecorator(bsModel, liftedDiscretization);
		} else if (model instanceof FDMCevModel) {
			final FDMCevModel cevModel = (FDMCevModel) model;

			final Grid sGrid = baseDiscretization.getSpaceGrid(0);
			final double[] sNodes = sGrid.getGrid();
			final double sMax = sNodes[sNodes.length - 1];

			final double iMax = maturity * sMax;
			final int nI = sNodes.length * 4;
			final Grid iGrid = new UniformGrid(nI - 1, 0.0, iMax);

			final SpaceTimeDiscretization liftedDiscretization = new SpaceTimeDiscretization(
					new Grid[] {sGrid, iGrid },
					baseDiscretization.getTimeDiscretization(),
					baseDiscretization.getTheta(),
					new double[] {cevModel.getInitialValue()[0], 0.0 });

			return new LiftedFDMCevModelDecorator(cevModel, liftedDiscretization);
		} else if (model instanceof FDMBachelierModel) {
			final FDMBachelierModel bachelierModel = (FDMBachelierModel) model;

			final Grid sGrid = baseDiscretization.getSpaceGrid(0);
			final double[] sNodes = sGrid.getGrid();
			final double sMax = sNodes[sNodes.length - 1];

			/*
			 * Keep the current lifted scale policy for now.
			 * Note: for Bachelier, if negative S values are allowed on the base
			 * grid,
			 * a more robust iMax policy might be max |S| * T instead of T *
			 * sMax.
			 */
			final double iMax = maturity * sMax;
			final int nI = sNodes.length * 4;
			final Grid iGrid = new UniformGrid(nI - 1, 0.0, iMax);

			final SpaceTimeDiscretization liftedDiscretization = new SpaceTimeDiscretization(
					new Grid[] {sGrid, iGrid },
					baseDiscretization.getTimeDiscretization(),
					baseDiscretization.getTheta(),
					new double[] {bachelierModel.getInitialValue()[0], 0.0 });

			return new LiftedFDMBachelierModelDecorator(bachelierModel, liftedDiscretization);
		} else if (model instanceof FDMHestonModel) {
			final FDMHestonModel hestonModel = (FDMHestonModel) model;

			final Grid sGrid = baseDiscretization.getSpaceGrid(0);
			final Grid vGrid = baseDiscretization.getSpaceGrid(1);

			final double[] sNodes = sGrid.getGrid();
			final double sMax = sNodes[sNodes.length - 1];

			final double iMax = maturity * sMax;
			final int nI = sNodes.length * 4;
			final Grid iGrid = new UniformGrid(nI - 1, 0.0, iMax);

			final double[] initialValue = hestonModel.getInitialValue();

			final SpaceTimeDiscretization liftedDiscretization = new SpaceTimeDiscretization(
					new Grid[] {sGrid, vGrid, iGrid },
					baseDiscretization.getTimeDiscretization(),
					baseDiscretization.getTheta(),
					new double[] {initialValue[0], initialValue[1], 0.0 });

			return new LiftedFDMHestonModelDecorator(hestonModel, liftedDiscretization);
		} else if (model instanceof FDMSabrModel) {
			final FDMSabrModel sabrModel = (FDMSabrModel) model;

			final Grid sGrid = baseDiscretization.getSpaceGrid(0);
			final Grid alphaGrid = baseDiscretization.getSpaceGrid(1);

			final double[] sNodes = sGrid.getGrid();
			final double sMax = sNodes[sNodes.length - 1];

			final double iMax = maturity * sMax;
			final int nI = sNodes.length * 4;
			final Grid iGrid = new UniformGrid(nI - 1, 0.0, iMax);

			final double[] initialValue = sabrModel.getInitialValue();

			final SpaceTimeDiscretization liftedDiscretization = new SpaceTimeDiscretization(
					new Grid[] {sGrid, alphaGrid, iGrid },
					baseDiscretization.getTimeDiscretization(),
					baseDiscretization.getTheta(),
					new double[] {initialValue[0], initialValue[1], 0.0 });

			return new LiftedFDMSabrModelDecorator(sabrModel, liftedDiscretization);
		} else {
			throw new IllegalArgumentException(
					"AsianOption currently supports only FDMBlackScholesModel, FDMCevModel, FDMBachelierModel and FDMHestonModel.");
		}
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
	public AsianStrike getAsianStrike() {
		return asianStrike;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public Exercise getExercise() {
		return exercise;
	}

	/**
	 * Decorator that lifts a 1D Black-Scholes model to a 2D model with state
	 * (S, I),
	 * where I(t) = integral_0^t S(u) du.
	 */
	private static final class LiftedFDMBlackScholesModelDecorator
	implements FiniteDifferenceEquityModel, FiniteDifferenceBoundary {

		/**
		 * The delegate.
		 */
		private final FDMBlackScholesModel delegate;
		/**
		 * The lifted discretization.
		 */
		private final SpaceTimeDiscretization liftedDiscretization;

		private LiftedFDMBlackScholesModelDecorator(
				final FDMBlackScholesModel delegate,
				final SpaceTimeDiscretization liftedDiscretization) {
			this.delegate = delegate;
			this.liftedDiscretization = liftedDiscretization;
		}

		@Override
		public DiscountCurve getRiskFreeCurve() {
			return delegate.getRiskFreeCurve();
		}

		@Override
		public DiscountCurve getDividendYieldCurve() {
			return delegate.getDividendYieldCurve();
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return liftedDiscretization;
		}

		@Override
		public double[] getDrift(final double time, final double... stateVariables) {
			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];

			final double muS = delegate.getDrift(effectiveTime, stock)[0];
			final double muI = stock;

			return new double[] {muS, muI };
		}

		@Override
		public double[][] getFactorLoading(final double time, final double... stateVariables) {
			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];

			final double sigma = delegate.getFactorLoading(time, stock)[0][0];

			return new double[][] {{sigma, 0.0 }, {0.0, 0.0 } };
		}

		@Override
		public double[] getInitialValue() {
			final double[] oldArray = delegate.getInitialValue();
			final double[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
			newArray[newArray.length - 1] = 0.0;
			return newArray;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final BoundaryCondition[] result = new BoundaryCondition[2];

			final double integral = stateVariables.length > 1 ? stateVariables[1] : 0.0;
			final double averageSoFar = integral / maturity;

			final double lowerSValue;

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = discount * Math.max(averageSoFar - strike, 0.0);
				} else {
					lowerSValue = discount * Math.max(strike - averageSoFar, 0.0);
				}
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = 0.0;
				} else {
					lowerSValue = discount * averageSoFar;
				}
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			result[0] = StandardBoundaryCondition.dirichlet(lowerSValue);
			result[1] = StandardBoundaryCondition.none();

			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double q = -Math.log(getDividendYieldCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;
			final double integral = stateVariables.length > 1 ? stateVariables[1] : 0.0;

			final BoundaryCondition[] result = new BoundaryCondition[2];

			final double averageSoFar = integral / maturity;

			final double discountedExpectedRemainingAverageContribution;
			if (Math.abs(r - q) > 1E-12) {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * (Math.exp(-q * delta) - Math.exp(-r * delta)) / (r - q);
			} else {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * delta * Math.exp(-r * delta);
			}

			final double discountedExpectedAverage =
					discount * averageSoFar + discountedExpectedRemainingAverageContribution;

			final double discountedExpectedSpot =
					stock * Math.exp(-q * delta);

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				final double upperSCallValue =
						discountedExpectedAverage - discount * strike;

				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? upperSCallValue : 0.0);
				result[1] = StandardBoundaryCondition.none();
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				final double upperSCallValue =
						discountedExpectedSpot - discountedExpectedAverage;

				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? upperSCallValue : 0.0);
				result[1] = StandardBoundaryCondition.none();
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			return result;
		}

		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {

			return new LiftedFDMBlackScholesModelDecorator(delegate, newSpaceTimeDiscretization);
		}
	}

	/**
	 * Decorator that lifts a 1D CEV model to a 2D model with state (S, I),
	 * where I(t) = integral_0^t S(u) du.
	 */
	private static final class LiftedFDMCevModelDecorator
	implements FiniteDifferenceEquityModel, FiniteDifferenceBoundary {

		/**
		 * The delegate.
		 */
		private final FDMCevModel delegate;
		/**
		 * The lifted discretization.
		 */
		private final SpaceTimeDiscretization liftedDiscretization;

		private LiftedFDMCevModelDecorator(
				final FDMCevModel delegate,
				final SpaceTimeDiscretization liftedDiscretization) {
			this.delegate = delegate;
			this.liftedDiscretization = liftedDiscretization;
		}

		@Override
		public DiscountCurve getRiskFreeCurve() {
			return delegate.getRiskFreeCurve();
		}

		@Override
		public DiscountCurve getDividendYieldCurve() {
			return delegate.getDividendYieldCurve();
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return liftedDiscretization;
		}

		@Override
		public double[] getDrift(final double time, final double... stateVariables) {
			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];

			final double muS = delegate.getDrift(effectiveTime, stock)[0];
			final double muI = stock;

			return new double[] {muS, muI };
		}

		@Override
		public double[][] getFactorLoading(final double time, final double... stateVariables) {
			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];

			final double sigma = delegate.getFactorLoading(time, stock)[0][0];

			return new double[][] {{sigma, 0.0 }, {0.0, 0.0 } };
		}

		@Override
		public double[] getInitialValue() {
			final double[] oldArray = delegate.getInitialValue();
			final double[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
			newArray[newArray.length - 1] = 0.0;
			return newArray;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final BoundaryCondition[] result = new BoundaryCondition[2];

			final double integral = stateVariables.length > 1 ? stateVariables[1] : 0.0;
			final double averageSoFar = integral / maturity;

			final double lowerSValue;

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = discount * Math.max(averageSoFar - strike, 0.0);
				} else {
					lowerSValue = discount * Math.max(strike - averageSoFar, 0.0);
				}
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = 0.0;
				} else {
					lowerSValue = discount * averageSoFar;
				}
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			result[0] = StandardBoundaryCondition.dirichlet(lowerSValue);
			result[1] = StandardBoundaryCondition.none();

			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double q = -Math.log(getDividendYieldCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;
			final double integral = stateVariables.length > 1 ? stateVariables[1] : 0.0;

			final BoundaryCondition[] result = new BoundaryCondition[2];

			final double averageSoFar = integral / maturity;

			final double discountedExpectedRemainingAverageContribution;
			if (Math.abs(r - q) > 1E-12) {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * (Math.exp(-q * delta) - Math.exp(-r * delta)) / (r - q);
			} else {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * delta * Math.exp(-r * delta);
			}

			final double discountedExpectedAverage =
					discount * averageSoFar + discountedExpectedRemainingAverageContribution;

			final double discountedExpectedSpot =
					stock * Math.exp(-q * delta);

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				final double upperSCallValue =
						discountedExpectedAverage - discount * strike;

				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? upperSCallValue : 0.0);

				result[1] = StandardBoundaryCondition.none();
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				final double upperSCallValue =
						discountedExpectedSpot - discountedExpectedAverage;

				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? upperSCallValue : 0.0);

				result[1] = StandardBoundaryCondition.none();
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			return result;
		}

		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {

			return new LiftedFDMCevModelDecorator(delegate, newSpaceTimeDiscretization);
		}
	}

	/**
	 * Decorator that lifts a 1D Bachelier model to a 2D model with state (S,
	 * I),
	 * where I(t) = integral_0^t S(u) du.
	 */
	private static final class LiftedFDMBachelierModelDecorator
	implements FiniteDifferenceEquityModel, FiniteDifferenceBoundary {

		/**
		 * The delegate.
		 */
		private final FDMBachelierModel delegate;
		/**
		 * The lifted discretization.
		 */
		private final SpaceTimeDiscretization liftedDiscretization;

		private LiftedFDMBachelierModelDecorator(
				final FDMBachelierModel delegate,
				final SpaceTimeDiscretization liftedDiscretization) {
			this.delegate = delegate;
			this.liftedDiscretization = liftedDiscretization;
		}

		@Override
		public DiscountCurve getRiskFreeCurve() {
			return delegate.getRiskFreeCurve();
		}

		@Override
		public DiscountCurve getDividendYieldCurve() {
			return delegate.getDividendYieldCurve();
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return liftedDiscretization;
		}

		@Override
		public double[] getDrift(final double time, final double... stateVariables) {
			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];

			final double muS = delegate.getDrift(effectiveTime, stock)[0];
			final double muI = stock;

			return new double[] {muS, muI };
		}

		@Override
		public double[][] getFactorLoading(final double time, final double... stateVariables) {
			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];

			final double sigma = delegate.getFactorLoading(time, stock)[0][0];

			return new double[][] {{sigma, 0.0 }, {0.0, 0.0 } };
		}

		@Override
		public double[] getInitialValue() {
			final double[] oldArray = delegate.getInitialValue();
			final double[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
			newArray[newArray.length - 1] = 0.0;
			return newArray;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final BoundaryCondition[] result = new BoundaryCondition[2];

			final double integral = stateVariables.length > 1 ? stateVariables[1] : 0.0;
			final double averageSoFar = integral / maturity;

			final double lowerSValue;

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = discount * Math.max(averageSoFar - strike, 0.0);
				} else {
					lowerSValue = discount * Math.max(strike - averageSoFar, 0.0);
				}
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = 0.0;
				} else {
					lowerSValue = discount * averageSoFar;
				}
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			result[0] = StandardBoundaryCondition.dirichlet(lowerSValue);
			result[1] = StandardBoundaryCondition.none();

			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double q = -Math.log(getDividendYieldCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;
			final double integral = stateVariables.length > 1 ? stateVariables[1] : 0.0;

			final BoundaryCondition[] result = new BoundaryCondition[2];

			final double averageSoFar = integral / maturity;

			final double discountedExpectedRemainingAverageContribution;
			if (Math.abs(r - q) > 1E-12) {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * (Math.exp(-q * delta) - Math.exp(-r * delta)) / (r - q);
			} else {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * delta * Math.exp(-r * delta);
			}

			final double discountedExpectedAverage =
					discount * averageSoFar + discountedExpectedRemainingAverageContribution;

			final double discountedExpectedSpot =
					stock * Math.exp(-q * delta);

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				final double upperSCallValue =
						discountedExpectedAverage - discount * strike;

				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? upperSCallValue : 0.0);

				result[1] = StandardBoundaryCondition.none();
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				final double upperSCallValue =
						discountedExpectedSpot - discountedExpectedAverage;

				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? upperSCallValue : 0.0);

				result[1] = StandardBoundaryCondition.none();
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			return result;
		}


		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {

			return new LiftedFDMBachelierModelDecorator(delegate, newSpaceTimeDiscretization);
		}
	}

	/**
	 * Decorator that lifts a 2D Heston model with state (S, v)
	 * to a 3D model with state (S, v, I),
	 * where I(t) = integral_0^t S(u) du.
	 */
	private static final class LiftedFDMHestonModelDecorator
	implements FiniteDifferenceEquityModel, FiniteDifferenceBoundary {

		/**
		 * The delegate.
		 */
		private final FDMHestonModel delegate;
		/**
		 * The lifted discretization.
		 */
		private final SpaceTimeDiscretization liftedDiscretization;

		private LiftedFDMHestonModelDecorator(
				final FDMHestonModel delegate,
				final SpaceTimeDiscretization liftedDiscretization) {
			this.delegate = delegate;
			this.liftedDiscretization = liftedDiscretization;
		}

		@Override
		public DiscountCurve getRiskFreeCurve() {
			return delegate.getRiskFreeCurve();
		}

		@Override
		public DiscountCurve getDividendYieldCurve() {
			return delegate.getDividendYieldCurve();
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return liftedDiscretization;
		}

		@Override
		public double[] getDrift(final double time, final double... stateVariables) {
			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];
			final double variance = stateVariables.length > 1 ? stateVariables[1] : delegate.getInitialValue()[1];

			final double[] baseDrift = delegate.getDrift(effectiveTime, stock, variance);

			final double muS = baseDrift[0];
			final double muV = baseDrift[1];
			final double muI = stock;

			return new double[] {muS, muV, muI };
		}

		@Override
		public double[][] getFactorLoading(final double time, final double... stateVariables) {
			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];
			final double variance = stateVariables.length > 1 ? stateVariables[1] : delegate.getInitialValue()[1];

			final double[][] baseLoading = delegate.getFactorLoading(time, stock, variance);

			return new double[][] {
				{baseLoading[0][0], baseLoading[0][1], 0.0 },
				{baseLoading[1][0], baseLoading[1][1], 0.0 },
				{0.0,               0.0,               0.0 }
			};
		}

		@Override
		public double[] getInitialValue() {
			final double[] oldArray = delegate.getInitialValue();
			final double[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
			newArray[newArray.length - 1] = 0.0;
			return newArray;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final BoundaryCondition[] result = new BoundaryCondition[3];

			final double I = stateVariables.length > 2 ? stateVariables[2] : 0.0;
			final double averageSoFar = I / maturity;

			final double lowerSValue;

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = discount * Math.max(averageSoFar - strike, 0.0);
				} else {
					lowerSValue = discount * Math.max(strike - averageSoFar, 0.0);
				}
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = 0.0;
				} else {
					lowerSValue = discount * averageSoFar;
				}
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			result[0] = StandardBoundaryCondition.dirichlet(lowerSValue);
			result[1] = StandardBoundaryCondition.none();
			result[2] = StandardBoundaryCondition.none();

			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				double time,
				final double... stateVariables) {

			if (time == 0.0) {
				time = 1e-6;
			}

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(time)) / time;
			final double q = -Math.log(getDividendYieldCurve().getDiscountFactor(time)) / time;
			final double delta = maturity - time;
			final double discount = Math.exp(-r * delta);

			final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;
			final double integral = stateVariables.length > 2 ? stateVariables[2] : 0.0;

			final BoundaryCondition[] result = new BoundaryCondition[3];

			final double averageSoFar = integral / maturity;

			final double discountedExpectedRemainingAverageContribution;
			if (Math.abs(r - q) > 1E-12) {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * (Math.exp(-q * delta) - Math.exp(-r * delta)) / (r - q);
			} else {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * delta * Math.exp(-r * delta);
			}

			final double discountedExpectedAverage =
					discount * averageSoFar + discountedExpectedRemainingAverageContribution;

			final double discountedExpectedSpot =
					stock * Math.exp(-q * delta);

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				final double callAsymptoticValue =
						discountedExpectedAverage - discount * strike;

				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? callAsymptoticValue : 0.0);

				/*
				 * Upper I is the inflow boundary for the transport solve.
				 * For large I the fixed-strike call is deep ITM and the put is
				 * asymptotically 0.
				 */
				result[2] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? callAsymptoticValue : 0.0);
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				final double putAsymptoticValue =
						discountedExpectedAverage - discountedExpectedSpot;

				/*
				 * Upper S boundary.
				 */
				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? 0.0 : putAsymptoticValue);

				/*
				 * Upper I is the inflow boundary.
				 * For large I the floating-strike put is deep ITM and the call
				 * is asymptotically 0.
				 */
				result[2] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? 0.0 : putAsymptoticValue);
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			result[1] = StandardBoundaryCondition.none();

			return result;
		}

		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {
			return new LiftedFDMHestonModelDecorator(delegate, newSpaceTimeDiscretization);
		}
	}

	/**
	 * Decorator that lifts a 2D SABR model with state (S, alpha)
	 * to a 3D model with state (S, alpha, I),
	 * where I(t) = integral_0^t S(u) du.
	 */
	private static final class LiftedFDMSabrModelDecorator
	implements FiniteDifferenceEquityModel, FiniteDifferenceBoundary {

		/**
		 * The delegate.
		 */
		private final FDMSabrModel delegate;
		/**
		 * The lifted discretization.
		 */
		private final SpaceTimeDiscretization liftedDiscretization;

		private LiftedFDMSabrModelDecorator(
				final FDMSabrModel delegate,
				final SpaceTimeDiscretization liftedDiscretization) {
			this.delegate = delegate;
			this.liftedDiscretization = liftedDiscretization;
		}

		@Override
		public DiscountCurve getRiskFreeCurve() {
			return delegate.getRiskFreeCurve();
		}

		@Override
		public DiscountCurve getDividendYieldCurve() {
			return delegate.getDividendYieldCurve();
		}

		@Override
		public SpaceTimeDiscretization getSpaceTimeDiscretization() {
			return liftedDiscretization;
		}

		@Override
		public double[] getDrift(final double time, final double... stateVariables) {
			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final double s = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];
			final double alpha = stateVariables.length > 1 ? stateVariables[1] : delegate.getInitialValue()[1];

			final double[] baseDrift = delegate.getDrift(effectiveTime, s, alpha);

			final double muS = baseDrift[0];
			final double muAlpha = baseDrift[1];
			final double muI = s;

			return new double[] {muS, muAlpha, muI };
		}

		@Override
		public double[][] getFactorLoading(final double time, final double... stateVariables) {
			final double stock = stateVariables.length > 0 ? stateVariables[0] : delegate.getInitialValue()[0];
			final double alpha = stateVariables.length > 1 ? stateVariables[1] : delegate.getInitialValue()[1];

			final double[][] baseLoading = delegate.getFactorLoading(time, stock, alpha);

			return new double[][] {
				{baseLoading[0][0], baseLoading[0][1], 0.0 },
				{baseLoading[1][0], baseLoading[1][1], 0.0 },
				{0.0,               0.0,               0.0 }
			};
		}

		@Override
		public double[] getInitialValue() {
			final double[] oldArray = delegate.getInitialValue();
			final double[] newArray = Arrays.copyOf(oldArray, oldArray.length + 1);
			newArray[newArray.length - 1] = 0.0;
			return newArray;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final BoundaryCondition[] result = new BoundaryCondition[3];

			final double integral = stateVariables.length > 2 ? stateVariables[2] : 0.0;
			final double averageSoFar = integral / maturity;

			final double lowerSValue;

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = discount * Math.max(averageSoFar - strike, 0.0);
				} else {
					lowerSValue = discount * Math.max(strike - averageSoFar, 0.0);
				}
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				if (callOrPut == CallOrPut.CALL) {
					lowerSValue = 0.0;
				} else {
					lowerSValue = discount * averageSoFar;
				}
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			/*
			 * S -> lower boundary:
			 * same Asian asymptotics as in the existing lifted models.
			 */
			result[0] = StandardBoundaryCondition.dirichlet(lowerSValue);

			/*
			 * alpha -> lower boundary:
			 * keep PDE row intact, consistent with current SABR boundary
			 * policy.
			 */
			result[1] = StandardBoundaryCondition.none();

			/*
			 * I -> lower boundary:
			 * keep PDE row intact, as in the existing Asian lifted models.
			 */
			result[2] = StandardBoundaryCondition.none();

			return result;
		}

		@Override
		public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
				final FiniteDifferenceEquityProduct product,
				final double time,
				final double... stateVariables) {

			final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

			final AsianOption option = (AsianOption) product;
			final double maturity = option.getMaturity();
			final double strike = option.getStrike();
			final CallOrPut callOrPut = option.getCallOrPut();
			final AsianStrike asianStrike = option.getAsianStrike();

			final double r = -Math.log(getRiskFreeCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double q = -Math.log(getDividendYieldCurve().getDiscountFactor(effectiveTime)) / effectiveTime;
			final double delta = maturity - effectiveTime;
			final double discount = Math.exp(-r * delta);

			final double stock = stateVariables.length > 0 ? stateVariables[0] : 0.0;
			final double integral = stateVariables.length > 2 ? stateVariables[2] : 0.0;

			final BoundaryCondition[] result = new BoundaryCondition[3];

			final double averageSoFar = integral / maturity;

			final double discountedExpectedRemainingAverageContribution;
			if (Math.abs(r - q) > 1E-12) {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * (Math.exp(-q * delta) - Math.exp(-r * delta)) / (r - q);
			} else {
				discountedExpectedRemainingAverageContribution =
						(stock / maturity) * delta * Math.exp(-r * delta);
			}

			final double discountedExpectedAverage =
					discount * averageSoFar + discountedExpectedRemainingAverageContribution;

			final double discountedExpectedSpot =
					stock * Math.exp(-q * delta);

			if (asianStrike == AsianStrike.FIXED_STRIKE) {
				final double callAsymptoticValue =
						discountedExpectedAverage - discount * strike;

				/*
				 * Upper S boundary.
				 */
				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? callAsymptoticValue : 0.0);

				/*
				 * Upper alpha boundary:
				 * keep PDE row intact for now.
				 */
				result[1] = StandardBoundaryCondition.none();

				/*
				 * Upper I boundary is the inflow boundary for the transport
				 * solve.
				 * For large I the fixed-strike call is deep ITM and the put is
				 * asymptotically 0.
				 */
				result[2] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? callAsymptoticValue : 0.0);
			} else if (asianStrike == AsianStrike.FLOATING_STRIKE) {
				final double putAsymptoticValue =
						discountedExpectedAverage - discountedExpectedSpot;

				/*
				 * Upper S boundary.
				 */
				result[0] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? 0.0 : putAsymptoticValue);

				/*
				 * Upper alpha boundary:
				 * keep PDE row intact for now.
				 */
				result[1] = StandardBoundaryCondition.none();

				/*
				 * Upper I boundary is the inflow boundary.
				 * For large I the floating-strike put is deep ITM and the call
				 * is asymptotically 0.
				 */
				result[2] = StandardBoundaryCondition.dirichlet(
						(callOrPut == CallOrPut.CALL) ? 0.0 : putAsymptoticValue);
			} else {
				throw new IllegalArgumentException("Unrecognized strike type.");
			}

			return result;
		}

		@Override
		public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
				final SpaceTimeDiscretization newSpaceTimeDiscretization) {
			return new LiftedFDMSabrModelDecorator(delegate, newSpaceTimeDiscretization);
		}
	}
}
