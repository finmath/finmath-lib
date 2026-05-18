package net.finmath.finitedifference.assetderivativevaluation.models;

/**
 * Jump component for the Bates model.
 *
 * <p>
 * The jump part in the Bates model is a finite-activity compound Poisson jump
 * component acting on one selected state variable through multiplicative jumps
 * of the form
 * </p>
 *
 * <pre>
 * x -> x * exp(y),
 * </pre>
 *
 * <p>
 * where the log-jump size {@code y} is normally distributed with mean
 * {@code jumpMean} and standard deviation {@code jumpStdDev}. Hence the
 * associated Levy density is
 * </p>
 *
 * <pre>
 * nu(y) = intensity * gaussianDensity(y; jumpMean, jumpStdDev^2).
 * </pre>
 *
 * <p>
 * Numerically, this is the same jump specification as in the Merton
 * jump-diffusion model. The dedicated class name is introduced in order to keep
 * the code aligned with the Bates model semantics and naming.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class BatesJumpComponent extends MertonJumpComponent {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a Bates jump component acting on the first state variable.
	 *
	 * @param lowerIntegrationBound Lower integration bound for the log-jump
	 *     variable.
	 * @param upperIntegrationBound Upper integration bound for the log-jump
	 *     variable.
	 * @param intensity Jump intensity.
	 * @param jumpMean Mean of the log-jump size.
	 * @param jumpStdDev Standard deviation of the log-jump size.
	 */
	public BatesJumpComponent(
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double intensity,
			final double jumpMean,
			final double jumpStdDev) {
		super(
				lowerIntegrationBound,
				upperIntegrationBound,
				intensity,
				jumpMean,
				jumpStdDev
				);
	}

	/**
	 * Creates a Bates jump component.
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
	public BatesJumpComponent(
			final int stateVariableIndex,
			final double lowerIntegrationBound,
			final double upperIntegrationBound,
			final double intensity,
			final double jumpMean,
			final double jumpStdDev) {
		super(
				stateVariableIndex,
				lowerIntegrationBound,
				upperIntegrationBound,
				intensity,
				jumpMean,
				jumpStdDev
				);
	}
}
