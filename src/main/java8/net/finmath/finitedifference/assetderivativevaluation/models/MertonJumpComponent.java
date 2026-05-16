package net.finmath.finitedifference.assetderivativevaluation.models;

/**
 * Jump component for the Merton jump-diffusion model.
 *
 * <p>
 * The jump part acts on one selected state variable through multiplicative
 * jumps
 * of the form
 * </p>
 *
 * <pre>
 * x -> x * exp(y),
 * </pre>
 *
 * <p>
 * where the log-jump size {@code y} is normally distributed with mean
 * {@code jumpMean} and standard deviation {@code jumpStdDev}. The associated
 * Levy density is
 * </p>
 *
 * <pre>
 * nu(y) = intensity * gaussianDensity(y; jumpMean, jumpStdDev^2).
 * </pre>
 *
 * <p>
 * Since the Merton jump component has finite activity, it also has finite
 * variation.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class MertonJumpComponent extends AbstractStateIndependentJumpComponent {

	private static final long serialVersionUID = 1L;

	/**
	 * The intensity.
	 */
	private final double intensity;
	/**
	 * The jump mean.
	 */
	private final double jumpMean;
	/**
	 * The jump std dev.
	 */
	private final double jumpStdDev;
	/**
	 * The normalization factor.
	 */
	private final double normalizationFactor;

	/**
	 * Creates a Merton jump component acting on the first state variable.
	 *
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param intensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 */
	public MertonJumpComponent(
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double intensity,
			final double jumpMean,
			final double jumpStdDev) {
		this(0, lowerIntegrationBound, upperIntegrationBound, intensity, jumpMean, jumpStdDev);
	}

	/**
	 * Creates a Merton jump component.
	 *
	 * @param stateVariableIndex Index of the affected state variable.
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param intensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 */
	public MertonJumpComponent(
			final int stateVariableIndex,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double intensity,
			final double jumpMean,
			final double jumpStdDev) {
		super(stateVariableIndex, lowerIntegrationBound, upperIntegrationBound, true, true);

		if (intensity < 0.0) {
			throw new IllegalArgumentException("Jump intensity must be non-negative.");
		}
		if (jumpStdDev <= 0.0) {
			throw new IllegalArgumentException("Jump standard deviation must be positive.");
		}

		this.intensity = intensity;
		this.jumpMean = jumpMean;
		this.jumpStdDev = jumpStdDev;
		normalizationFactor = 1.0 / (jumpStdDev * Math.sqrt(2.0 * Math.PI));
	}

	@Override
	protected double getLevyDensity(final double time, final double jumpSize) {
		final double standardizedJump = (jumpSize - jumpMean) / jumpStdDev;
		return intensity * normalizationFactor * Math.exp(-0.5 * standardizedJump * standardizedJump);
	}

	/**
	 * Returns the jump intensity.
	 *
	 * @return The jump intensity.
	 */
	public double getIntensity() {
		return intensity;
	}

	/**
	 * Returns the mean of the log-jump size.
	 *
	 * @return The mean of the log-jump size.
	 */
	public double getJumpMean() {
		return jumpMean;
	}

	/**
	 * Returns the standard deviation of the log-jump size.
	 *
	 * @return The standard deviation of the log-jump size.
	 */
	public double getJumpStdDev() {
		return jumpStdDev;
	}
}
