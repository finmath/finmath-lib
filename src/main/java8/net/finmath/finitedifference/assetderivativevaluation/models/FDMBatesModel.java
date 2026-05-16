package net.finmath.finitedifference.assetderivativevaluation.models;

import java.util.Optional;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Finite difference model for option pricing under the Bates stochastic
 * volatility jump-diffusion model.
 *
 * <p>
 * State variables are {@code (S, v)} where {@code S} is the spot and {@code v}
 * is the instantaneous variance.
 * </p>
 *
 * <p>
 * The local part of the model coincides with the Heston model, while the jump
 * part is supplied separately through {@link #getJumpComponent()}.
 * </p>
 *
 * <p>
 * Hence:
 * </p>
 * <ul>
 *   <li>{@link #getDrift(double, double...)} returns the local drift vector of
 *       the state variables, inherited from {@link FDMHestonModel},</li>
 *   <li>{@link #getFactorLoading(double, double...)} returns the local factor
 *       loading matrix, inherited from {@link FDMHestonModel},</li>
 *   <li>{@link #getJumpComponent()} returns the finite-activity Bates jump
 *       component acting on the spot variable.</li>
 * </ul>
 *
 * <p>
 * The constructor argument order follows the Heston constructors first and then
 * appends the jump parameters.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class FDMBatesModel extends FDMHestonModel {

	/**
	 * The jump component.
	 */
	private final BatesJumpComponent jumpComponent;

	/**
	 * Constructs a Bates finite difference model from discount curves and an
	 * explicit jump component.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpComponent Bates jump component.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final BatesJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		super(
				initialSpot,
				initialVariance,
				riskFreeCurve,
				dividendYieldCurve,
				kappa,
				thetaV,
				sigma,
				rho,
				spaceTimeDiscretization
		);

		if (jumpComponent == null) {
			throw new IllegalArgumentException("Jump component must not be null.");
		}

		this.jumpComponent = jumpComponent;
	}

	/**
	 * Constructs a Bates finite difference model from discount curves and jump
	 * parameters.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param dividendYieldCurve Dividend yield discount curve.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final DiscountCurve riskFreeCurve,
			final DiscountCurve dividendYieldCurve,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialSpot,
				initialVariance,
				riskFreeCurve,
				dividendYieldCurve,
				kappa,
				thetaV,
				sigma,
				rho,
				new BatesJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev
				),
				spaceTimeDiscretization
		);
	}

	/**
	 * Constructs a Bates finite difference model without dividend yield curve,
	 * using an explicit jump component.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpComponent Bates jump component.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final DiscountCurve riskFreeCurve,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final BatesJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		super(
				initialSpot,
				initialVariance,
				riskFreeCurve,
				kappa,
				thetaV,
				sigma,
				rho,
				spaceTimeDiscretization
		);

		if (jumpComponent == null) {
			throw new IllegalArgumentException("Jump component must not be null.");
		}

		this.jumpComponent = jumpComponent;
	}

	/**
	 * Constructs a Bates finite difference model without dividend yield curve,
	 * using jump parameters.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeCurve Risk-free discount curve.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final DiscountCurve riskFreeCurve,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialSpot,
				initialVariance,
				riskFreeCurve,
				kappa,
				thetaV,
				sigma,
				rho,
				new BatesJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev
				),
				spaceTimeDiscretization
		);
	}

	/**
	 * Constructs a Bates finite difference model from constant rates and an
	 * explicit jump component.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield rate.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpComponent Bates jump component.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final BatesJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		super(
				initialSpot,
				initialVariance,
				riskFreeRate,
				dividendYieldRate,
				kappa,
				thetaV,
				sigma,
				rho,
				spaceTimeDiscretization
		);

		if (jumpComponent == null) {
			throw new IllegalArgumentException("Jump component must not be null.");
		}

		this.jumpComponent = jumpComponent;
	}

	/**
	 * Constructs a Bates finite difference model from constant rates and jump
	 * parameters.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param dividendYieldRate Constant dividend yield rate.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final double riskFreeRate,
			final double dividendYieldRate,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialSpot,
				initialVariance,
				riskFreeRate,
				dividendYieldRate,
				kappa,
				thetaV,
				sigma,
				rho,
				new BatesJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev
				),
				spaceTimeDiscretization
		);
	}

	/**
	 * Constructs a Bates finite difference model from a constant risk-free rate
	 * and zero dividend yield, using an explicit jump component.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpComponent Bates jump component.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final double riskFreeRate,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final BatesJumpComponent jumpComponent,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		super(
				initialSpot,
				initialVariance,
				riskFreeRate,
				kappa,
				thetaV,
				sigma,
				rho,
				spaceTimeDiscretization
		);

		if (jumpComponent == null) {
			throw new IllegalArgumentException("Jump component must not be null.");
		}

		this.jumpComponent = jumpComponent;
	}

	/**
	 * Constructs a Bates finite difference model from a constant risk-free rate
	 * and zero dividend yield, using jump parameters.
	 *
	 * @param initialSpot Initial spot price.
	 * @param initialVariance Initial variance.
	 * @param riskFreeRate Constant risk-free rate.
	 * @param kappa Mean reversion speed of variance.
	 * @param thetaV Long-term mean of variance.
	 * @param sigma Vol-of-vol parameter.
	 * @param rho Correlation between the Brownian motions.
	 * @param jumpIntensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param spaceTimeDiscretization Grid object defining the spatial
	 *     discretization.
	 */
	public FDMBatesModel(
			final double initialSpot,
			final double initialVariance,
			final double riskFreeRate,
			final double kappa,
			final double thetaV,
			final double sigma,
			final double rho,
			final double jumpIntensity,
			final double jumpMean,
			final double jumpStdDev,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final SpaceTimeDiscretization spaceTimeDiscretization) {
		this(
				initialSpot,
				initialVariance,
				riskFreeRate,
				kappa,
				thetaV,
				sigma,
				rho,
				new BatesJumpComponent(
						lowerIntegrationBound,
						upperIntegrationBound,
						jumpIntensity,
						jumpMean,
						jumpStdDev
				),
				spaceTimeDiscretization
		);
	}

	@Override
	public Optional<JumpComponent> getJumpComponent() {
		return Optional.of(jumpComponent);
	}

	/**
	 * Returns the Bates jump component.
	 *
	 * @return The Bates jump component.
	 */
	public BatesJumpComponent getBatesJumpComponent() {
		return jumpComponent;
	}

	@Override
	public FiniteDifferenceEquityModel getCloneWithModifiedSpaceTimeDiscretization(
			final SpaceTimeDiscretization newSpaceTimeDiscretization) {
		return new FDMBatesModel(
				getInitialValue()[0],
				getInitialValue()[1],
				getRiskFreeCurve(),
				getDividendYieldCurve(),
				getKappa(),
				getThetaV(),
				getSigma(),
				getRho(),
				jumpComponent,
				newSpaceTimeDiscretization
		);
	}
}
