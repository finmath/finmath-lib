package net.finmath.finitedifference.assetderivativevaluation.models;

import java.time.LocalDate;

import net.finmath.finitedifference.assetderivativevaluation.boundaries.FDBoundaryFactory;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.FiniteDifferenceBoundary;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.volatilities.ConstantLocalVolatility;
import net.finmath.marketdata.model.volatilities.LocalVolatility;

/**
 * Finite difference model for option pricing under the Black-Scholes framework
 * for European and American options.
 *
 * <p>
 * The model supports both the classical constant-volatility Black-Scholes case
 * and the local-volatility case. Constant volatility is represented internally
 * as a {@link ConstantLocalVolatility}.
 * </p>
 *
 * @author Alessandro Gnoatto (this version)
 * @author Christian Fries, Ralph Rudd, Jorg Kienitz (original version)
 */
public class FDMBlackScholesModel implements FiniteDifferenceEquityModel {

	/**
	 * The initial value.
	 */
	private final double initialValue;
	/**
	 * The risk free curve.
	 */
	private final DiscountCurve riskFreeCurve;
	/**
	 * The volatility.
	 */
	private final LocalVolatility volatility;
	/**
	 * The dividend yield curve.
	 */
	private final DiscountCurve dividendYieldCurve;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Minimum time used to avoid division by zero.
	 */
	private static final double MINIMUM_TIME = 1.0E-6;

	/**
	 * Constructs a Black-Scholes finite difference model for option pricing.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param volatility Constant volatility of the underlying asset.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				new ConstantLocalVolatility(volatility),
				spaceTimeDiscretization
		);
	}

	/**
	 * Constructs a local-volatility finite difference model for option pricing.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param volatility Local volatility function.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final LocalVolatility volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurve = dividendYieldCurve;
		this.volatility = volatility;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a Black-Scholes finite difference model for option pricing
	 * without
	 * dividend yield.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param volatility Constant volatility of the underlying asset.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this(
				initialValue,
				riskFreeCurve,
				new ConstantLocalVolatility(volatility),
				spaceTimeDiscretization
		);
	}

	/**
	 * Constructs a local-volatility finite difference model for option pricing
	 * without dividend yield.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param volatility Local volatility function.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final LocalVolatility volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;
		this.riskFreeCurve = riskFreeCurve;

		final double[] times = new double[] {0.0, 1.0};
		final double[] givenAnnualizedZeroRates = new double[] {0.0, 0.0};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		final DiscountCurve dividendYieldCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						LocalDate.of(2010, 8, 1),
						times,
						givenAnnualizedZeroRates,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		this.dividendYieldCurve = dividendYieldCurve;

		this.volatility = volatility;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a Black-Scholes finite difference model for option pricing.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield rate.
	 * @param volatility Constant volatility of the underlying asset.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this(
				initialValue,
				riskFreeRate,
				dividendYieldRate,
				new ConstantLocalVolatility(volatility),
				spaceTimeDiscretization
		);
	}

	/**
	 * Constructs a local-volatility finite difference model for option pricing.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield rate.
	 * @param volatility Local volatility function.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final LocalVolatility volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;

		final double[] times = new double[] {0.0, 1.0};
		final double[] givenAnnualizedZeroRates = new double[] {riskFreeRate, riskFreeRate};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		final DiscountCurve riskFreeCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						null,
						times,
						givenAnnualizedZeroRates,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		this.riskFreeCurve = riskFreeCurve;

		final double[] times1 = new double[] {0.0, 1.0};
		final double[] givenAnnualizedZeroRates1 = new double[] {dividendYieldRate, dividendYieldRate};
		final InterpolationMethod interpolationMethod1 = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity1 = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod1 = ExtrapolationMethod.CONSTANT;

		final DiscountCurve dividendYieldCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						null,
						times1,
						givenAnnualizedZeroRates1,
						interpolationMethod1,
						extrapolationMethod1,
						interpolationEntity1);

		this.dividendYieldCurve = dividendYieldCurve;

		this.volatility = volatility;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Constructs a Black-Scholes finite difference model for option pricing
	 * without
	 * dividend yield.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param volatility Constant volatility of the underlying asset.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this(
				initialValue,
				riskFreeRate,
				new ConstantLocalVolatility(volatility),
				spaceTimeDiscretization
		);
	}

	/**
	 * Constructs a local-volatility finite difference model for option pricing
	 * without dividend yield.
	 *
	 * @param initialValue Initial spot price.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param volatility Local volatility function.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBlackScholesModel(
			final double initialValue,
			final double riskFreeRate,
			final LocalVolatility volatility,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		this.initialValue = initialValue;

		final double[] times = new double[] {0.0, 1.0};
		final double[] givenAnnualizedZeroRates = new double[] {riskFreeRate, riskFreeRate};
		final InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;

		final DiscountCurve riskFreeCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						LocalDate.of(2010, 8, 1),
						times,
						givenAnnualizedZeroRates,
						interpolationMethod,
						extrapolationMethod,
						interpolationEntity);

		this.riskFreeCurve = riskFreeCurve;

		final double[] times1 = new double[] {0.0, 1.0};
		final double[] givenAnnualizedZeroRates1 = new double[] {0.0, 0.0};
		final InterpolationMethod interpolationMethod1 = InterpolationMethod.LINEAR;
		final InterpolationEntity interpolationEntity1 = InterpolationEntity.VALUE;
		final ExtrapolationMethod extrapolationMethod1 = ExtrapolationMethod.CONSTANT;

		final DiscountCurve dividendYieldCurve =
				DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
						"dividendCurve",
						LocalDate.of(2010, 8, 1),
						times1,
						givenAnnualizedZeroRates1,
						interpolationMethod1,
						extrapolationMethod1,
						interpolationEntity1);

		this.dividendYieldCurve = dividendYieldCurve;

		this.volatility = volatility;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	@Override
	public DiscountCurve getRiskFreeCurve() {
		return riskFreeCurve;
	}

	/**
	 * Returns the initial value, i.e. the spot value, of the underlying.
	 *
	 * @return The initial value.
	 */
	public double[] getInitialValue() {
		return new double[] {initialValue};
	}

	@Override
	public double[] getDrift(final double time, final double... stateVariables) {
		final double effectiveTime = time == 0.0 ? MINIMUM_TIME : time;

		final double[] result = new double[1];

		final double rF = getRiskFreeCurve().getDiscountFactor(effectiveTime);
		final double riskFreeRate = -Math.log(rF) / effectiveTime;

		final double dY = getDividendYieldCurve().getDiscountFactor(effectiveTime);
		final double dividendYieldRate = -Math.log(dY) / effectiveTime;

		result[0] = (riskFreeRate - dividendYieldRate) * stateVariables[0];
		return result;
	}

	@Override
	public double[][] getFactorLoading(final double time, final double... stateVariables) {
		final double[][] result = new double[1][1];

		final double assetValue = stateVariables[0];
		result[0][0] = volatility.getValue(time, assetValue) * assetValue;

		return result;
	}

	/**
	 * Returns the volatility function.
	 *
	 * @return The local volatility function.
	 */
	public LocalVolatility getVolatility() {
		return volatility;
	}

	/**
	 * Returns the constant volatility if the model has been constructed with a
	 * constant volatility.
	 *
	 * @return The constant volatility.
	 */
	public double getConstantVolatility() {
		if (volatility instanceof ConstantLocalVolatility) {
			return ((ConstantLocalVolatility) volatility).getVolatility();
		}

		throw new IllegalStateException("The model is not using a constant volatility.");
	}

	@Override
	public DiscountCurve getDividendYieldCurve() {
		return dividendYieldCurve;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary =
				FDBoundaryFactory.createBoundary(this, product);

		return boundary.getBoundaryConditionsAtLowerBoundary(product, time, riskFactors);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary =
				FDBoundaryFactory.createBoundary(this, product);

		return boundary.getBoundaryConditionsAtUpperBoundary(product, time, riskFactors);
	}

	@Override
	public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
			final SpaceTimeDiscretization newSpaceTimeDiscretization) {

		return new FDMBlackScholesModel(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				volatility,
				newSpaceTimeDiscretization
		);
	}
}
