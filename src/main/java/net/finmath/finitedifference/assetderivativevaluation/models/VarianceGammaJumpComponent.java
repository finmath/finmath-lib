package net.finmath.finitedifference.assetderivativevaluation.models;

/**
 * Jump component for the Variance Gamma model.
 *
 * <p>
 * The jump part acts on one selected state variable through multiplicative
 * jumps
 * of the form
 * 
 * \[ x \mapsto x \cdot exp(y), \]
 *
 * where {@code y} denotes the log-jump size. The associated Levy density is
 * \[ \nu(y) = \left\{
 * \begin{array}{ll}
 *   C \cdot exp(-M y) / y, &amp; \text{for\,} y &gt; 0, \\
 *   C \cdot exp(-G |y|) / |y|, &amp; \text{for\,} y &lt; 0.
 * \end{array} \right. \]
 * This is the standard Variance Gamma Levy density in {@code (C,G,M)}
 * parameterization. The process has infinite activity and finite variation.
 * </p>
 *
 * <p>
 * A convenience factory is also provided for the usual Variance Gamma
 * parameters {@code (sigma, nu, theta)}. In that case,
 * </p>
 *
 * <pre>
 * C = 1 / nu,
 * G = (sqrt(theta^2 + 2 sigma^2 / nu) + theta) / sigma^2,
 * M = (sqrt(theta^2 + 2 sigma^2 / nu) - theta) / sigma^2.
 * </pre>
 *
 * @author Alessandro Gnoatto
 */
public class VarianceGammaJumpComponent extends AbstractStateIndependentJumpComponent {

	private static final long serialVersionUID = 1L;

	/**
	 * The c.
	 */
	private final double c;
	/**
	 * The g.
	 */
	private final double g;
	/**
	 * The m.
	 */
	private final double m;

	/**
	 * Creates a Variance Gamma jump component acting on the first state
	 * variable.
	 *
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param c The parameter {@code C} of the Levy density.
	 * @param g The parameter {@code G} governing negative jumps.
	 * @param m The parameter {@code M} governing positive jumps.
	 */
	public VarianceGammaJumpComponent(
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double c,
			final double g,
			final double m) {
		this(0, lowerIntegrationBound, upperIntegrationBound, c, g, m);
	}

	/**
	 * Creates a Variance Gamma jump component.
	 *
	 * @param stateVariableIndex Index of the affected state variable.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param c The parameter {@code C} of the Levy density.
	 * @param g The parameter {@code G} governing negative jumps.
	 * @param m The parameter {@code M} governing positive jumps.
	 */
	public VarianceGammaJumpComponent(
			final int stateVariableIndex,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double c,
			final double g,
			final double m) {
		super(stateVariableIndex, lowerIntegrationBound, upperIntegrationBound, false, true);

		if (c <= 0.0) {
			throw new IllegalArgumentException("Parameter C must be positive.");
		}
		if (g <= 0.0) {
			throw new IllegalArgumentException("Parameter G must be positive.");
		}
		if (m <= 0.0) {
			throw new IllegalArgumentException("Parameter M must be positive.");
		}

		this.c = c;
		this.g = g;
		this.m = m;
	}

	/**
	 * Creates a Variance Gamma jump component from the standard
	 * {@code (sigma, nu, theta)} parameterization, acting on the first state
	 * variable.
	 *
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param sigma The volatility parameter of the Variance Gamma model.
	 * @param nu The variance-of-time-change parameter of the Variance Gamma
	 *     model.
	 * @param theta The asymmetry parameter of the Variance Gamma model.
	 * @return The corresponding Variance Gamma jump component.
	 */
	public static VarianceGammaJumpComponent ofSigmaNuTheta(
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double sigma,
			final double nu,
			final double theta) {
		return ofSigmaNuTheta(0, lowerIntegrationBound, upperIntegrationBound, sigma, nu, theta);
	}

	/**
	 * Creates a Variance Gamma jump component from the standard
	 * {@code (sigma, nu, theta)} parameterization.
	 *
	 * @param stateVariableIndex Index of the affected state variable.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param sigma The volatility parameter of the Variance Gamma model.
	 * @param nu The variance-of-time-change parameter of the Variance Gamma
	 *     model.
	 * @param theta The asymmetry parameter of the Variance Gamma model.
	 * @return The corresponding Variance Gamma jump component.
	 */
	public static VarianceGammaJumpComponent ofSigmaNuTheta(
			final int stateVariableIndex,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double sigma,
			final double nu,
			final double theta) {

		if (sigma <= 0.0) {
			throw new IllegalArgumentException("Parameter sigma must be positive.");
		}
		if (nu <= 0.0) {
			throw new IllegalArgumentException("Parameter nu must be positive.");
		}

		final double sigmaSquared = sigma * sigma;
		final double radical = Math.sqrt(theta * theta + 2.0 * sigmaSquared / nu);

		final double c = 1.0 / nu;
		final double g = (radical + theta) / sigmaSquared;
		final double m = (radical - theta) / sigmaSquared;

		return new VarianceGammaJumpComponent(
				stateVariableIndex,
				lowerIntegrationBound,
				upperIntegrationBound,
				c,
				g,
				m
		);
	}

	@Override
	protected double getLevyDensity(final double time, final double jumpSize) {

		if (jumpSize > 0.0) {
			return c * Math.exp(-m * jumpSize) / jumpSize;
		}
		if (jumpSize < 0.0) {
			final double absoluteJumpSize = Math.abs(jumpSize);
			return c * Math.exp(-g * absoluteJumpSize) / absoluteJumpSize;
		}

		throw new IllegalArgumentException("The Variance Gamma Levy density is singular at zero.");
	}

	/**
	 * Returns the parameter {@code C}.
	 *
	 * @return The parameter {@code C}.
	 */
	public double getC() {
		return c;
	}

	/**
	 * Returns the parameter {@code G}.
	 *
	 * @return The parameter {@code G}.
	 */
	public double getG() {
		return g;
	}

	/**
	 * Returns the parameter {@code M}.
	 *
	 * @return The parameter {@code M}.
	 */
	public double getM() {
		return m;
	}
}
