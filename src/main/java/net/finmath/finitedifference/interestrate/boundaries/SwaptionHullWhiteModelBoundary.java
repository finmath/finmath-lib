package net.finmath.finitedifference.interestrate.boundaries;

import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.interestrate.models.FDMHullWhiteModel;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;
import net.finmath.finitedifference.interestrate.products.Swaption;
import net.finmath.finitedifference.interestrate.products.Swaption.ResolvedExerciseData;

/**
 * Boundary conditions for {@link Swaption} under {@link FDMHullWhiteModel}.
 *
 * <p>
 * This implementation provides asymptotic Dirichlet boundary values compatible
 * with European, Bermudan, and American swaption exercise.
 * </p>
 *
 * <p>
 * The guiding asymptotics are:
 * </p>
 * <ul>
 * <li>on the deep out-of-the-money side, the swaption value tends to zero,</li>
 *   <li>on the deep in-the-money side, the swaption value approaches the
 *       intrinsic value envelope over the remaining admissible exercise
 *       opportunities.</li>
 * </ul>
 *
 * <p>
 * Concretely:
 * </p>
 * <ul>
 *   <li>for a {@code RECEIVER} swaption, the lower boundary is treated as the
 *       deep in-the-money side and the upper boundary as the deep
 *       out-of-the-money side,</li>
 *   <li>for a {@code PAYER} swaption, the upper boundary is treated as the deep
 *       in-the-money side and the lower boundary as the deep out-of-the-money
 *       side.</li>
 * </ul>
 *
 * <p>
 * On the deep in-the-money side, the boundary value is chosen as
 * </p>
 *
 * <p>
 * <i>
 * \max_{j \in \mathcal{E}(t)} \bigl( V_{\mathrm{swap},j}(t,x), 0 \bigr),
 * </i>
 * </p>
 *
 * <p>
 * where {@code \mathcal{E}(t)} denotes the set of remaining admissible exercise
 * opportunities and {@code V_swap,j(t,x)} is the value of the corresponding
 * underlying swap.
 * </p>
 *
 * <p>
 * This is a pragmatic boundary choice for the unified PDE swaption class.
 * A European-only Jamshidian-exact boundary can be added later as a refinement.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class SwaptionHullWhiteModelBoundary implements FiniteDifferenceInterestRateBoundary {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;

	/**
	 * The model.
	 */
	private final FDMHullWhiteModel model;

	/**
	 * Creates the Hull-White boundary for {@link Swaption}.
	 *
	 * @param model The Hull-White model.
	 */
	public SwaptionHullWhiteModelBoundary(final FDMHullWhiteModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getInitialValue() == null || model.getInitialValue().length != 1) {
			throw new IllegalArgumentException(
					"SwaptionHullWhiteModelBoundary requires a one-dimensional Hull-White model."
			);
		}

		this.model = model;
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final Swaption swaption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double boundaryValue = getLowerBoundaryValue(swaption, time, stateVariables[0]);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(boundaryValue)
		};
	}

	@Override
	public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			final FiniteDifferenceInterestRateProduct product,
			final double time,
			final double... stateVariables) {

		final Swaption swaption = validateAndCastProduct(product);
		validateStateVariables(stateVariables);

		final double boundaryValue = getUpperBoundaryValue(swaption, time, stateVariables[0]);

		return new BoundaryCondition[] {
				StandardBoundaryCondition.dirichlet(boundaryValue)
		};
	}

	private Swaption validateAndCastProduct(final FiniteDifferenceInterestRateProduct product) {
		if (!(product instanceof Swaption)) {
			throw new IllegalArgumentException(
					"SwaptionHullWhiteModelBoundary requires a Swaption product."
			);
		}

		return (Swaption) product;
	}

	private void validateStateVariables(final double[] stateVariables) {
		if (stateVariables == null || stateVariables.length != 1) {
			throw new IllegalArgumentException("Exactly one state variable is required.");
		}
	}

	private double getLowerBoundaryValue(
			final Swaption swaption,
			final double time,
			final double stateVariable) {

		switch (swaption.getSwaptionType()) {
		case RECEIVER:
			return getDeepInTheMoneyBoundaryValue(swaption, time, stateVariable);
		case PAYER:
			return 0.0;
		default:
			throw new IllegalArgumentException("Unsupported swaption type: " + swaption.getSwaptionType());
		}
	}

	private double getUpperBoundaryValue(
			final Swaption swaption,
			final double time,
			final double stateVariable) {

		switch (swaption.getSwaptionType()) {
		case RECEIVER:
			return 0.0;
		case PAYER:
			return getDeepInTheMoneyBoundaryValue(swaption, time, stateVariable);
		default:
			throw new IllegalArgumentException("Unsupported swaption type: " + swaption.getSwaptionType());
		}
	}

	private double getDeepInTheMoneyBoundaryValue(
			final Swaption swaption,
			final double time,
			final double stateVariable) {

		final ResolvedExerciseData resolvedExerciseData = swaption.getResolvedExerciseData(model);

		double value = 0.0;

		for (int i = 0; i < resolvedExerciseData.getNumberOfExerciseTimes(); i++) {
			if (resolvedExerciseData.getExerciseTime(i) < time - TIME_TOLERANCE) {
				continue;
			}

			final int scheduleIndex = resolvedExerciseData.getScheduleIndex(i);

			value = Math.max(
					value,
					swaption.getIntrinsicValue(
							time,
							scheduleIndex,
							stateVariable,
							model
					)
			);
		}

		return value;
	}
}
