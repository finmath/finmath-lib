package net.finmath.finitedifference.assetderivativevaluation.models;

import java.util.Optional;

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

/**
 * Finite-difference model for option pricing under the Variance Gamma model.
 *
 * <p>
 * The model is formulated in the stock variable and follows the convention used
 * for jump models in this finite-difference framework:
 * </p>
 * <ul>
 * <li>{@link #getDrift(double, double...)} returns the local first-order term,
 *       i.e. {@code (r-q)S},</li>
 *   <li>{@link #getFactorLoading(double, double...)} returns zero, since the
 *       pure Variance Gamma model has no diffusive factor,</li>
 *   <li>the jump part of the generator is supplied separately through
 *       {@link #getJumpComponent()}.</li>
 * </ul>
 *
 * <p>
 * The public constructors use the usual Variance Gamma parameterization
 * {@code (sigma, nu, theta)} in order to stay close to the Monte Carlo side.
 * Constructors / factories based on {@code (C,G,M)} are provided only through
 * named static factory methods in order to avoid Java signature clashes.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMVarianceGammaModel implements FiniteDifferenceEquityModel {

	/**
	 * The initial value.
	 */
	private final double initialValue;
	/**
	 * The risk free curve.
	 */
	private final DiscountCurve riskFreeCurve;
	/**
	 * The dividend yield curve.
	 */
	private final DiscountCurve dividendYieldCurve;
	/**
	 * The jump component.
	 */
	private final VarianceGammaJumpComponent jumpComponent;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Minimum time used to avoid division by zero.
	 */
	private static final double MINIMUM_TIME = 1.0E-6;

	/**
	 * Creates a Variance Gamma model from explicit discount curves and an
	 * explicit
	 * jump component.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param jumpComponent Variance Gamma jump component.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final VarianceGammaJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		if (riskFreeCurve == null) {
			throw new IllegalArgumentException("Risk-free curve must not be null.");
		}
		if (dividendYieldCurve == null) {
			throw new IllegalArgumentException("Dividend yield curve must not be null.");
		}
		if (jumpComponent == null) {
			throw new IllegalArgumentException("Jump component must not be null.");
		}
		if (spaceTimeDiscretization == null) {
			throw new IllegalArgumentException("Space-time discretization must not be null.");
		}

		this.initialValue = initialValue;
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurve = dividendYieldCurve;
		this.jumpComponent = jumpComponent;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Creates a Variance Gamma model from explicit discount curves and the
	 * usual
	 * {@code (sigma, nu, theta)} parameterization.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param sigma The volatility parameter.
	 * @param nu The variance-of-time-change parameter.
	 * @param theta The asymmetry parameter.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double sigma,
			final double nu,
			final double theta,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				VarianceGammaJumpComponent.ofSigmaNuTheta(
						lowerIntegrationBound,
						upperIntegrationBound,
						sigma,
						nu,
						theta
				),
				spaceTimeDiscretization
		);
	}

	/**
	 * Creates a Variance Gamma model assuming zero dividend yield and using an
	 * explicit jump component.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param jumpComponent Variance Gamma jump component.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final VarianceGammaJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeCurve,
				createFlatDiscountCurve("dividendCurve", 0.0),
				jumpComponent,
				spaceTimeDiscretization
		);
	}

	/**
	 * Creates a Variance Gamma model assuming zero dividend yield and using the
	 * usual {@code (sigma, nu, theta)} parameterization.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param sigma The volatility parameter.
	 * @param nu The variance-of-time-change parameter.
	 * @param theta The asymmetry parameter.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final double sigma,
			final double nu,
			final double theta,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeCurve,
				createFlatDiscountCurve("dividendCurve", 0.0),
				sigma,
				nu,
				theta,
				lowerIntegrationBound,
				upperIntegrationBound,
				spaceTimeDiscretization
		);
	}

	/**
	 * Creates a Variance Gamma model from flat rates and an explicit jump
	 * component.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield.
	 * @param jumpComponent Variance Gamma jump component.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final VarianceGammaJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				createFlatDiscountCurve("riskFreeCurve", riskFreeRate),
				createFlatDiscountCurve("dividendCurve", dividendYieldRate),
				jumpComponent,
				spaceTimeDiscretization
		);
	}

	/**
	 * Creates a Variance Gamma model from flat rates and the usual
	 * {@code (sigma, nu, theta)} parameterization.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield.
	 * @param sigma The volatility parameter.
	 * @param nu The variance-of-time-change parameter.
	 * @param theta The asymmetry parameter.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double sigma,
			final double nu,
			final double theta,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				createFlatDiscountCurve("riskFreeCurve", riskFreeRate),
				createFlatDiscountCurve("dividendCurve", dividendYieldRate),
				sigma,
				nu,
				theta,
				lowerIntegrationBound,
				upperIntegrationBound,
				spaceTimeDiscretization
		);
	}

	/**
	 * Creates a Variance Gamma model from a flat risk-free rate, assuming zero
	 * dividend yield, and using an explicit jump component.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param jumpComponent Variance Gamma jump component.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final double riskFreeRate,
			final VarianceGammaJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeRate,
				0.0,
				jumpComponent,
				spaceTimeDiscretization
		);
	}

	/**
	 * Creates a Variance Gamma model from a flat risk-free rate, assuming zero
	 * dividend yield, and using the usual {@code (sigma, nu, theta)}
	 * parameterization.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param sigma The volatility parameter.
	 * @param nu The variance-of-time-change parameter.
	 * @param theta The asymmetry parameter.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization The space-time discretization.
	 */
	public FDMVarianceGammaModel(
			final double initialValue,
			final double riskFreeRate,
			final double sigma,
			final double nu,
			final double theta,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeRate,
				0.0,
				sigma,
				nu,
				theta,
				lowerIntegrationBound,
				upperIntegrationBound,
				spaceTimeDiscretization
		);
	}

	/**
	 * Named factory using the {@code (C,G,M)} parameterization and explicit
	 * curves.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param c The parameter {@code C}.
	 * @param g The parameter {@code G}.
	 * @param m The parameter {@code M}.
	 * @param lowerIntegrationBound Lower integration bound.
	 * @param upperIntegrationBound Upper integration bound.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @return The corresponding Variance Gamma model.
	 */
	public static FDMVarianceGammaModel ofCGM(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double c,
			final double g,
			final double m,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		return new FDMVarianceGammaModel(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				new VarianceGammaJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						c,
						g,
						m
				),
				spaceTimeDiscretization
		);
	}

	/**
	 * Named factory using the {@code (C,G,M)} parameterization and zero
	 * dividend yield.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param c The parameter {@code C}.
	 * @param g The parameter {@code G}.
	 * @param m The parameter {@code M}.
	 * @param lowerIntegrationBound Lower integration bound.
	 * @param upperIntegrationBound Upper integration bound.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @return The corresponding Variance Gamma model.
	 */
	public static FDMVarianceGammaModel ofCGM(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final double c,
			final double g,
			final double m,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		return new FDMVarianceGammaModel(
				initialValue,
				riskFreeCurve,
				new VarianceGammaJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						c,
						g,
						m
				),
				spaceTimeDiscretization
		);
	}

	/**
	 * Named factory using the {@code (C,G,M)} parameterization and flat rates.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield.
	 * @param c The parameter {@code C}.
	 * @param g The parameter {@code G}.
	 * @param m The parameter {@code M}.
	 * @param lowerIntegrationBound Lower integration bound.
	 * @param upperIntegrationBound Upper integration bound.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @return The corresponding Variance Gamma model.
	 */
	public static FDMVarianceGammaModel ofCGM(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double c,
			final double g,
			final double m,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		return new FDMVarianceGammaModel(
				initialValue,
				riskFreeRate,
				dividendYieldRate,
				new VarianceGammaJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						c,
						g,
						m
				),
				spaceTimeDiscretization
		);
	}

	/**
	 * Named factory using the {@code (C,G,M)} parameterization, flat risk-free
	 * rate,
	 * and zero dividend yield.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param c The parameter {@code C}.
	 * @param g The parameter {@code G}.
	 * @param m The parameter {@code M}.
	 * @param lowerIntegrationBound Lower integration bound.
	 * @param upperIntegrationBound Upper integration bound.
	 * @param spaceTimeDiscretization The space-time discretization.
	 * @return The corresponding Variance Gamma model.
	 */
	public static FDMVarianceGammaModel ofCGM(
			final double initialValue,
			final double riskFreeRate,
			final double c,
			final double g,
			final double m,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		return new FDMVarianceGammaModel(
				initialValue,
				riskFreeRate,
				new VarianceGammaJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						c,
						g,
						m
				),
				spaceTimeDiscretization
		);
	}

	@Override
	public DiscountCurve getRiskFreeCurve() {
		return riskFreeCurve;
	}

	@Override
	public DiscountCurve getDividendYieldCurve() {
		return dividendYieldCurve;
	}

	@Override
	public double[] getInitialValue() {
		return new double[] {initialValue };
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public VarianceGammaJumpComponent getVarianceGammaJumpComponent() {
		return jumpComponent;
	}

	@Override
	public SpaceTimeDiscretization getSpaceTimeDiscretization() {
		return spaceTimeDiscretization;
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
		return new double[][] {{0.0 } };
	}

	@Override
	public Optional<JumpComponent> getJumpComponent() {
		return Optional.of(jumpComponent);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary = FDBoundaryFactory.createBoundary(this, product);
		return boundary.getBoundaryConditionsAtLowerBoundary(product, time, riskFactors);
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceEquityProduct product,
			final double time,
			final double... riskFactors) {

		final FiniteDifferenceBoundary boundary = FDBoundaryFactory.createBoundary(this, product);
		return boundary.getBoundaryConditionsAtUpperBoundary(product, time, riskFactors);
	}

	@Override
	public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
			final SpaceTimeDiscretization newSpaceTimeDiscretization) {
		return new FDMVarianceGammaModel(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				jumpComponent,
				newSpaceTimeDiscretization
		);
	}

	private static DiscountCurve createFlatDiscountCurve(final String name, final double zeroRate) {
		final double[] times = new double[] {0.0, 1.0 };
		final double[] zeroRates = new double[] {zeroRate, zeroRate };

		return DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				name,
				null,
				times,
				zeroRates,
				InterpolationMethod.LINEAR,
				ExtrapolationMethod.CONSTANT,
				InterpolationEntity.VALUE
		);
	}
}
