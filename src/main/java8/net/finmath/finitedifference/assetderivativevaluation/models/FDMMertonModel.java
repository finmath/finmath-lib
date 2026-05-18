package net.finmath.finitedifference.assetderivativevaluation.models;

import java.time.LocalDate;
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
 * Finite-difference model for option pricing under the Merton jump-diffusion
 * model.
 *
 * <p>
 * The local part of the dynamics is identical to Black-Scholes:
 * </p>
 *
 * <pre>
 * dS_t = (r(t) - q(t)) S_t dt + sigma S_t dW_t + jump part.
 * </pre>
 *
 * <p>
 * In line with the jump-interface convention adopted in this framework,
 * {@link #getDrift(double, double...)} returns only the local drift
 * {@code (r-q)S}, {@link #getFactorLoading(double, double...)} returns the
 * local
 * diffusion loading {@code sigma S}, and the non-local jump contribution is
 * supplied separately through {@link #getJumpComponent()}.
 * </p>
 *
 * <p>
 * The jump component is represented by a {@link MertonJumpComponent}, acting on
 * the first state variable through multiplicative jumps of the form
 * {@code S -> S exp(Y)}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMMertonModel implements FiniteDifferenceEquityModel {

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
	 * The volatility.
	 */
	private final double volatility;
	/**
	 * The jump component.
	 */
	private final MertonJumpComponent jumpComponent;
	/**
	 * The space time discretization.
	 */
	private final SpaceTimeDiscretization spaceTimeDiscretization;

	/**
	 * Minimum time used to avoid division by zero.
	 */
	private static final double MINIMUM_TIME = 1.0E-6;

	/**
	 * Creates a finite-difference Merton model from discount curves and an
	 * explicit
	 * jump component.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param volatility Diffusion volatility.
	 * @param jumpComponent Merton jump component.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double volatility,
			final MertonJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {

		if (jumpComponent == null) {
			throw new IllegalArgumentException("Jump component must not be null.");
		}

		this.initialValue = initialValue;
		this.riskFreeCurve = riskFreeCurve;
		this.dividendYieldCurve = dividendYieldCurve;
		this.volatility = volatility;
		this.jumpComponent = jumpComponent;
		this.spaceTimeDiscretization = spaceTimeDiscretization;
	}

	/**
	 * Creates a finite-difference Merton model from discount curves and jump
	 * parameters.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param volatility Diffusion volatility.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double volatility,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				volatility,
				new MertonJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev),
				spaceTimeDiscretization
				);
	}

	/**
	 * Creates a finite-difference Merton model assuming zero dividend yield.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param volatility Diffusion volatility.
	 * @param jumpComponent Merton jump component.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final double volatility,
			final MertonJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeCurve,
				createFlatDiscountCurve("dividendCurve", 0.0),
				volatility,
				jumpComponent,
				spaceTimeDiscretization
				);
	}

	/**
	 * Creates a finite-difference Merton model assuming zero dividend yield.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param volatility Diffusion volatility.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final DiscountCurve riskFreeCurve,
			final double volatility,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				riskFreeCurve,
				createFlatDiscountCurve("dividendCurve", 0.0),
				volatility,
				new MertonJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev),
				spaceTimeDiscretization
				);
	}

	/**
	 * Creates a finite-difference Merton model from flat rates and an explicit
	 * jump component.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield rate.
	 * @param volatility Diffusion volatility.
	 * @param jumpComponent Merton jump component.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double volatility,
			final MertonJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				createFlatDiscountCurve("riskFreeCurve", riskFreeRate),
				createFlatDiscountCurve("dividendCurve", dividendYieldRate),
				volatility,
				jumpComponent,
				spaceTimeDiscretization
				);
	}

	/**
	 * Creates a finite-difference Merton model from flat rates and jump
	 * parameters.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield rate.
	 * @param volatility Diffusion volatility.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double volatility,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				createFlatDiscountCurve("riskFreeCurve", riskFreeRate),
				createFlatDiscountCurve("dividendCurve", dividendYieldRate),
				volatility,
				new MertonJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev),
				spaceTimeDiscretization
				);
	}

	/**
	 * Creates a finite-difference Merton model from a flat risk-free rate and
	 * zero dividend yield.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param volatility Diffusion volatility.
	 * @param jumpComponent Merton jump component.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final MertonJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				createFlatDiscountCurve("riskFreeCurve", riskFreeRate),
				createFlatDiscountCurve("dividendCurve", 0.0),
				volatility,
				jumpComponent,
				spaceTimeDiscretization
				);
	}

	/**
	 * Creates a finite-difference Merton model from a flat risk-free rate and
	 * zero dividend yield.
	 *
	 * @param initialValue Initial spot value.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param volatility Diffusion volatility.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Space-time discretization.
	 */
	public FDMMertonModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialValue,
				createFlatDiscountCurve("riskFreeCurve", riskFreeRate),
				createFlatDiscountCurve("dividendCurve", 0.0),
				volatility,
				new MertonJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev),
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
		return new double[][] {{volatility * stateVariables[0] } };
	}

	@Override
	public Optional<JumpComponent> getJumpComponent() {
		return Optional.of(jumpComponent);
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
		return new FDMMertonModel(
				initialValue,
				riskFreeCurve,
				dividendYieldCurve,
				volatility,
				jumpComponent,
				newSpaceTimeDiscretization
				);
	}

	/**
	 * Returns the diffusion volatility.
	 *
	 * @return The diffusion volatility.
	 */
	public double getVolatility() {
		return volatility;
	}

	/**
	 * Returns the Merton jump component.
	 *
	 * @return The Merton jump component.
	 */
	public MertonJumpComponent getMertonJumpComponent() {
		return jumpComponent;
	}

	private static DiscountCurve createFlatDiscountCurve(final String name, final double zeroRate) {
		final double[] times = new double[] {0.0, 1.0 };
		final double[] givenAnnualizedZeroRates = new double[] {zeroRate, zeroRate };

		return DiscountCurveInterpolation.createDiscountCurveFromAnnualizedZeroRates(
				name,
				LocalDate.of(2010, 8, 1),
				times,
				givenAnnualizedZeroRates,
				InterpolationMethod.LINEAR,
				ExtrapolationMethod.CONSTANT,
				InterpolationEntity.VALUE
				);
	}
}
