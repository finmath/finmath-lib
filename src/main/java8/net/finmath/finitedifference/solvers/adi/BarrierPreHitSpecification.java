package net.finmath.finitedifference.solvers.adi;

import net.finmath.modelling.products.BarrierType;

/**
 * Immutable specification for the pre-hit continuation PDE used in the direct
 * 2D knock-in formulation.
 *
 * <p>
 * The pre-hit PDE is solved only on the continuation side of the barrier.
 * The barrier itself is represented by a spot-side interface row carrying the
 * activated trace.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class BarrierPreHitSpecification {

	/**
	 * The barrier type.
	 */
	private final BarrierType barrierType;
	/**
	 * The barrier value.
	 */
	private final double barrierValue;
	/**
	 * The barrier spot index.
	 */
	private final int barrierSpotIndex;
	/**
	 * The spot min.
	 */
	private final double spotMin;
	/**
	 * The spot max.
	 */
	private final double spotMax;
	/**
	 * The number of spot steps.
	 */
	private final int numberOfSpotSteps;
	/**
	 * The activated trace.
	 */
	private final ActivatedBarrierTrace2D activatedTrace;

	/**
	 * Creates the pre-hit PDE specification.
	 *
	 * @param barrierType The barrier type. Only DOWN_IN and UP_IN are valid
	 *     here.
	 * @param barrierValue The barrier level.
	 * @param barrierSpotIndex The spot-grid index corresponding to the barrier
	 *     node
	 *        on the pre-hit auxiliary grid.
	 * @param spotMin The lower bound of the pre-hit spot grid.
	 * @param spotMax The upper bound of the pre-hit spot grid.
	 * @param numberOfSpotSteps The number of uniform spot steps.
	 * @param activatedTrace The activated post-hit trace prescribed on the
	 *     barrier.
	 */
	public BarrierPreHitSpecification(
			final BarrierType barrierType,
			final double barrierValue,
			final int barrierSpotIndex,
			final double spotMin,
			final double spotMax,
			final int numberOfSpotSteps,
			final ActivatedBarrierTrace2D activatedTrace) {

		if (barrierType != BarrierType.DOWN_IN && barrierType != BarrierType.UP_IN) {
			throw new IllegalArgumentException(
					"BarrierPreHitSpecification is only valid for knock-in barrier types.");
		}
		if (numberOfSpotSteps <= 0) {
			throw new IllegalArgumentException("numberOfSpotSteps must be strictly positive.");
		}
		if (spotMax <= spotMin) {
			throw new IllegalArgumentException("spotMax must be strictly greater than spotMin.");
		}
		if (barrierSpotIndex < 0 || barrierSpotIndex > numberOfSpotSteps) {
			throw new IllegalArgumentException(
					"barrierSpotIndex must lie between 0 and numberOfSpotSteps inclusive.");
		}
		if (activatedTrace == null) {
			throw new IllegalArgumentException("activatedTrace must not be null.");
		}

		this.barrierType = barrierType;
		this.barrierValue = barrierValue;
		this.barrierSpotIndex = barrierSpotIndex;
		this.spotMin = spotMin;
		this.spotMax = spotMax;
		this.numberOfSpotSteps = numberOfSpotSteps;
		this.activatedTrace = activatedTrace;
	}

	/**
	 * Returns the knock-in barrier type.
	 *
	 * @return The barrier type.
	 */
	public BarrierType getBarrierType() {
		return barrierType;
	}

	/**
	 * Returns the barrier level.
	 *
	 * @return The barrier value.
	 */
	public double getBarrierValue() {
		return barrierValue;
	}

	/**
	 * Returns the barrier node index on the pre-hit spot grid.
	 *
	 * @return The barrier spot index.
	 */
	public int getBarrierSpotIndex() {
		return barrierSpotIndex;
	}

	/**
	 * Returns the lower bound of the pre-hit spot domain.
	 *
	 * @return The lower spot bound.
	 */
	public double getSpotMin() {
		return spotMin;
	}

	/**
	 * Returns the upper bound of the pre-hit spot domain.
	 *
	 * @return The upper spot bound.
	 */
	public double getSpotMax() {
		return spotMax;
	}

	/**
	 * Returns the number of spot steps of the pre-hit uniform grid.
	 *
	 * @return The number of spot steps.
	 */
	public int getNumberOfSpotSteps() {
		return numberOfSpotSteps;
	}

	/**
	 * Returns the activated trace prescribed on the barrier.
	 *
	 * @return The activated barrier trace.
	 */
	public ActivatedBarrierTrace2D getActivatedTrace() {
		return activatedTrace;
	}

	/**
	 * Returns true if the pre-hit domain corresponds to a down-in option.
	 *
	 * @return True for down-in.
	 */
	public boolean isDownIn() {
		return barrierType == BarrierType.DOWN_IN;
	}

	/**
	 * Returns true if the pre-hit domain corresponds to an up-in option.
	 *
	 * @return True for up-in.
	 */
	public boolean isUpIn() {
		return barrierType == BarrierType.UP_IN;
	}

	/**
	 * Returns the implied uniform spot step size.
	 *
	 * @return The spot step size.
	 */
	public double getSpotStepSize() {
		return (spotMax - spotMin) / numberOfSpotSteps;
	}
}
