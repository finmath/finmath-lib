package net.finmath.finitedifference.interestrate.products;

import java.util.Arrays;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.interestrate.models.FiniteDifferenceInterestRateModel;
import net.finmath.finitedifference.solvers.FDMThetaMethod1D;
import net.finmath.modelling.EuropeanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.montecarlo.interestrate.products.BermudanSwaptionFromSwapSchedules.SwaptionType;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretization;

/**
 * Finite-difference valuation of a swaption under an interest-rate
 * finite-difference model.
 *
 * <p>
 * This class is designed to cover European, Bermudan, and American swaptions
 * through a single {@link Exercise} field.
 * </p>
 *
 * <p>
 * The product is represented as an option on the value of an underlying swap.
 * At an admissible exercise time {@code t}, the intrinsic value is
 * </p>
 *
 * <p>
 * <i>
 * max(V_swap(t,x), 0).
 * </i>
 * </p>
 *
 * <p>
 * The sign convention is determined by {@link SwaptionType}:
 * </p>
 * <ul>
 * <li>{@code PAYER}: intrinsic value is based on float leg minus fixed
 * leg,</li>
 * <li>{@code RECEIVER}: intrinsic value is based on fixed leg minus float
 * leg.</li>
 * </ul>
 *
 * <p>
 * The class supports:
 * </p>
 * <ul>
 * <li>a single underlying swap (European or American-style grid exercise),</li>
 *   <li>one underlying swap per exercise date (Bermudan style).</li>
 * </ul>
 *
 * <p>
 * Exercise handling is implemented through the interest-rate event-condition
 * mechanism. At each admissible exercise time {@code t},
 * </p>
 *
 * <p>
 * <i>
 * V(t^{-},x) = max(V(t^{+},x), intrinsic(t,x)).
 * </i>
 * </p>
 *
 * <p>
 * American exercise is approximated on the solver time grid and is currently
 * supported only with a single master underlying swap definition.
 * </p>
 *
 * <p>
 * Assumption:
 * the swap schedules supplied for a given exercise date should represent the
 * remaining underlying swap from that exercise date onward. In particular, this
 * class is intended for standard European/Bermudan swaption use cases where the
 * exercise dates and underlying remaining swaps are specified consistently.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class Swaption implements FiniteDifferenceInterestRateProduct {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;

	/**
	 * The exercise.
	 */
	private final Exercise exercise;
	/**
	 * The explicit exercise dates.
	 */
	private final double[] explicitExerciseDates;

	/**
	 * The swaption type.
	 */
	private final SwaptionType swaptionType;
	/**
	 * The forward curve name.
	 */
	private final String forwardCurveName;

	/**
	 * The swap rates.
	 */
	private final double[] swapRates;
	/**
	 * The notionals.
	 */
	private final double[] notionals;
	/**
	 * The fix schedules.
	 */
	private final Schedule[] fixSchedules;
	/**
	 * The float schedules.
	 */
	private final Schedule[] floatSchedules;

	/**
	 * Creates a swaption.
	 *
	 * <p>
	 * If {@code explicitExerciseDates} is {@code null}, then
	 * </p>
	 * <ul>
	 *   <li>European exercise uses {@code exercise.getMaturity()},</li>
	 * <li>Bermudan exercise derives exercise dates from the first period start
	 * of
	 * the supplied schedules if more than one underlying swap is provided,</li>
	 *   <li>American exercise uses all solver time-grid points up to
	 * {@code exercise.getMaturity()} and requires exactly one underlying swap
	 *       definition.</li>
	 * </ul>
	 *
	 * @param exercise The exercise specification.
	 * @param explicitExerciseDates Optional explicit exercise dates.
	 * @param swaptionType The payer/receiver indicator.
	 * @param forwardCurveName The forwarding-curve name. May be {@code null}.
	 * @param swapRates The fixed rates of the underlying swaps.
	 * @param notionals The notionals of the underlying swaps. Must be positive.
	 * @param fixSchedules The fixed-leg schedules of the underlying swaps.
	 * @param floatSchedules The floating-leg schedules of the underlying swaps.
	 */
	public Swaption(
			final Exercise exercise,
			final double[] explicitExerciseDates,
			final SwaptionType swaptionType,
			final String forwardCurveName,
			final double[] swapRates,
			final double[] notionals,
			final Schedule[] fixSchedules,
			final Schedule[] floatSchedules) {

		if (exercise == null) {
			throw new IllegalArgumentException("exercise must not be null.");
		}
		if (swaptionType == null) {
			throw new IllegalArgumentException("swaptionType must not be null.");
		}
		if (swapRates == null || swapRates.length == 0) {
			throw new IllegalArgumentException("swapRates must contain at least one element.");
		}
		if (notionals == null || notionals.length != swapRates.length) {
			throw new IllegalArgumentException("notionals must have the same length as swapRates.");
		}
		if (fixSchedules == null || fixSchedules.length != swapRates.length) {
			throw new IllegalArgumentException("fixSchedules must have the same length as swapRates.");
		}
		if (floatSchedules == null || floatSchedules.length != swapRates.length) {
			throw new IllegalArgumentException("floatSchedules must have the same length as swapRates.");
		}

		this.exercise = exercise;
		this.explicitExerciseDates = explicitExerciseDates == null ? null : explicitExerciseDates.clone();
		this.swaptionType = swaptionType;
		this.forwardCurveName = forwardCurveName;
		this.swapRates = swapRates.clone();
		this.notionals = notionals.clone();
		this.fixSchedules = fixSchedules.clone();
		this.floatSchedules = floatSchedules.clone();

		validateInputs();
	}

	/**
	 * Creates a swaption without explicit exercise dates.
	 *
	 * @param exercise The exercise specification.
	 * @param swaptionType The payer/receiver indicator.
	 * @param forwardCurveName The forwarding-curve name. May be {@code null}.
	 * @param swapRates The fixed rates of the underlying swaps.
	 * @param notionals The notionals of the underlying swaps. Must be positive.
	 * @param fixSchedules The fixed-leg schedules of the underlying swaps.
	 * @param floatSchedules The floating-leg schedules of the underlying swaps.
	 */
	public Swaption(
			final Exercise exercise,
			final SwaptionType swaptionType,
			final String forwardCurveName,
			final double[] swapRates,
			final double[] notionals,
			final Schedule[] fixSchedules,
			final Schedule[] floatSchedules) {
		this(
				exercise,
				null,
				swaptionType,
				forwardCurveName,
				swapRates,
				notionals,
				fixSchedules,
				floatSchedules
				);
	}

	@Override
	public double[] getValue(
			final double evaluationTime,
			final FiniteDifferenceInterestRateModel model) {

		final ResolvedExerciseData resolvedExerciseData = getResolvedExerciseData(model);
		final double lastExerciseTime = resolvedExerciseData.getLastExerciseTime();

		final ResolvedSwaption resolvedProduct = new ResolvedSwaption(resolvedExerciseData);

		final FDMThetaMethod1D solver = new FDMThetaMethod1D(
				model,
				resolvedProduct,
				model.getSpaceTimeDiscretization(),
				new EuropeanExercise(lastExerciseTime)
				);

		return solver.getValue(
				evaluationTime,
				lastExerciseTime,
				buildZeroTerminalValues(model)
				);
	}

	@Override
	public double[][] getValues(final FiniteDifferenceInterestRateModel model) {

		final ResolvedExerciseData resolvedExerciseData = getResolvedExerciseData(model);
		final double lastExerciseTime = resolvedExerciseData.getLastExerciseTime();

		final ResolvedSwaption resolvedProduct = new ResolvedSwaption(resolvedExerciseData);

		final FDMThetaMethod1D solver = new FDMThetaMethod1D(
				model,
				resolvedProduct,
				model.getSpaceTimeDiscretization(),
				new EuropeanExercise(lastExerciseTime)
				);

		return solver.getValues(
				lastExerciseTime,
				buildZeroTerminalValues(model)
				);
	}

	@Override
	public double[] getEventTimes() {
		if (exercise.isAmerican()) {
			throw new UnsupportedOperationException(
					"American swaption exercise times depend on the model time grid. "
							+ "Use getValue(...) or getValues(...), which resolve the exercise times internally."
					);
		}

		return resolveDiscreteExerciseTimes();
	}

	/**
	 * Returns the exercise specification.
	 *
	 * @return The exercise specification.
	 */
	public Exercise getExercise() {
		return exercise;
	}

	/**
	 * Returns the explicit exercise dates, if any.
	 *
	 * @return The explicit exercise dates, or {@code null}.
	 */
	public double[] getExplicitExerciseDates() {
		return explicitExerciseDates == null ? null : explicitExerciseDates.clone();
	}

	/**
	 * Returns the swaption type.
	 *
	 * @return The swaption type.
	 */
	public SwaptionType getSwaptionType() {
		return swaptionType;
	}

	/**
	 * Returns the forwarding-curve name.
	 *
	 * @return The forwarding-curve name, possibly {@code null}.
	 */
	public String getForwardCurveName() {
		return forwardCurveName;
	}

	/**
	 * Returns the fixed rates of the underlying swaps.
	 *
	 * @return The fixed rates.
	 */
	public double[] getSwapRates() {
		return swapRates.clone();
	}

	/**
	 * Returns the notionals of the underlying swaps.
	 *
	 * @return The notionals.
	 */
	public double[] getNotionals() {
		return notionals.clone();
	}

	/**
	 * Returns the fixed-leg schedules.
	 *
	 * @return The fixed-leg schedules.
	 */
	public Schedule[] getFixSchedules() {
		return fixSchedules.clone();
	}

	/**
	 * Returns the floating-leg schedules.
	 *
	 * @return The floating-leg schedules.
	 */
	public Schedule[] getFloatSchedules() {
		return floatSchedules.clone();
	}

	/**
	 * Resolves the exercise data to be used numerically under the given model.
	 *
	 * <p>
	 * For American exercise, the exercise times depend on the model time grid.
	 * For European and Bermudan exercise, they are determined directly from the
	 * contractual product data.
	 * </p>
	 *
	 * @param model The finite-difference interest-rate model.
	 * @return The resolved exercise data.
	 */
	public ResolvedExerciseData getResolvedExerciseData(final FiniteDifferenceInterestRateModel model) {
		validateModel(model);

		if (exercise.isAmerican()) {
			return resolveAmericanExerciseData(model);
		}

		final double[] exerciseTimes = resolveDiscreteExerciseTimes();
		final int[] scheduleIndices = resolveDiscreteScheduleIndices(exerciseTimes);

		return new ResolvedExerciseData(exerciseTimes, scheduleIndices);
	}

	/**
	 * Returns the intrinsic value at the given time and state for the schedule
	 * indexed by {@code scheduleIndex}.
	 *
	 * @param time The running time.
	 * @param scheduleIndex The schedule index.
	 * @param stateVariable The state variable.
	 * @param model The finite-difference interest-rate model.
	 * @return The intrinsic value.
	 */
	public double getIntrinsicValue(
			final double time,
			final int scheduleIndex,
			final double stateVariable,
			final FiniteDifferenceInterestRateModel model) {
		return Math.max(getUnderlyingSwapValue(time, scheduleIndex, stateVariable, model), 0.0);
	}

	/**
	 * Returns the underlying swap value at the given time and state for the
	 * schedule indexed by {@code scheduleIndex}.
	 *
	 * @param time The running time.
	 * @param scheduleIndex The schedule index.
	 * @param stateVariable The state variable.
	 * @param model The finite-difference interest-rate model.
	 * @return The underlying swap value.
	 */
	public double getUnderlyingSwapValue(
			final double time,
			final int scheduleIndex,
			final double stateVariable,
			final FiniteDifferenceInterestRateModel model) {

		final double fixedLegValue = getFixedLegValue(time, scheduleIndex, stateVariable, model);
		final double floatingLegValue = getFloatingLegValue(time, scheduleIndex, stateVariable, model);

		switch (swaptionType) {
		case PAYER:
			return floatingLegValue - fixedLegValue;
		case RECEIVER:
			return fixedLegValue - floatingLegValue;
		default:
			throw new IllegalArgumentException("Unsupported swaption type: " + swaptionType);
		}
	}

	private void validateInputs() {
		for (int i = 0; i < notionals.length; i++) {
			if (notionals[i] <= 0.0) {
				throw new IllegalArgumentException("All notionals must be strictly positive.");
			}
		}

		if (explicitExerciseDates != null) {
			if (explicitExerciseDates.length == 0) {
				throw new IllegalArgumentException("explicitExerciseDates must not be empty.");
			}
			for (int i = 0; i < explicitExerciseDates.length; i++) {
				if (explicitExerciseDates[i] < 0.0) {
					throw new IllegalArgumentException("explicitExerciseDates must be non-negative.");
				}
				if (i > 0 && explicitExerciseDates[i] <= explicitExerciseDates[i - 1]) {
					throw new IllegalArgumentException("explicitExerciseDates must be strictly increasing.");
				}
			}
		}

		for (int i = 0; i < fixSchedules.length; i++) {
			validateSchedule(fixSchedules[i], "fixSchedules[" + i + "]");
			validateSchedule(floatSchedules[i], "floatSchedules[" + i + "]");
		}

		if (exercise.isAmerican() && fixSchedules.length != 1) {
			throw new IllegalArgumentException(
					"American exercise currently requires exactly one master underlying swap definition."
					);
		}
	}

	private void validateSchedule(final Schedule schedule, final String name) {
		if (schedule == null) {
			throw new IllegalArgumentException(name + " must not be null.");
		}
		if (schedule.getNumberOfPeriods() <= 0) {
			throw new IllegalArgumentException(name + " must contain at least one period.");
		}
		for (int periodIndex = 0; periodIndex < schedule.getNumberOfPeriods(); periodIndex++) {
			if (schedule.getFixing(periodIndex) < 0.0) {
				throw new IllegalArgumentException(name + " contains a negative fixing time.");
			}
			if (schedule.getPayment(periodIndex) <= schedule.getFixing(periodIndex)) {
				throw new IllegalArgumentException(
						name + " must satisfy payment > fixing for every period."
						);
			}
		}
	}

	private void validateModel(final FiniteDifferenceInterestRateModel model) {
		if (model == null) {
			throw new IllegalArgumentException("model must not be null.");
		}
		if (model.getSpaceTimeDiscretization().getNumberOfSpaceGrids() != 1) {
			throw new IllegalArgumentException(
					"Swaption currently supports only one-dimensional finite-difference interest-rate models."
					);
		}
	}

	private double[] buildZeroTerminalValues(final FiniteDifferenceInterestRateModel model) {
		final SpaceTimeDiscretization discretization = model.getSpaceTimeDiscretization();
		final double[] xGrid = discretization.getSpaceGrid(0).getGrid();
		return new double[xGrid.length];
	}

	private double[] resolveDiscreteExerciseTimes() {

		if (explicitExerciseDates != null) {
			return explicitExerciseDates.clone();
		}

		if (exercise.isEuropean()) {
			return new double[] {exercise.getMaturity() };
		}

		if (exercise.isBermudan()) {
			if (fixSchedules.length == 1) {
				return new double[] {exercise.getMaturity() };
			}

			final double[] exerciseTimes = new double[fixSchedules.length];
			for (int i = 0; i < exerciseTimes.length; i++) {
				exerciseTimes[i] = inferExerciseDateFromSchedules(i);
			}
			return exerciseTimes;
		}

		throw new IllegalArgumentException("Unsupported exercise type.");
	}

	private int[] resolveDiscreteScheduleIndices(final double[] exerciseTimes) {

		if (fixSchedules.length == 1) {
			final int[] scheduleIndices = new int[exerciseTimes.length];
			Arrays.fill(scheduleIndices, 0);
			return scheduleIndices;
		}

		if (fixSchedules.length != exerciseTimes.length) {
			throw new IllegalArgumentException(
					"The number of underlying swap definitions must be either 1 or equal to the number of exercise dates."
					);
		}

		final int[] scheduleIndices = new int[exerciseTimes.length];
		for (int i = 0; i < scheduleIndices.length; i++) {
			scheduleIndices[i] = i;
		}
		return scheduleIndices;
	}

	private ResolvedExerciseData resolveAmericanExerciseData(final FiniteDifferenceInterestRateModel model) {
		final TimeDiscretization timeDiscretization = model.getSpaceTimeDiscretization().getTimeDiscretization();
		final double exerciseMaturity = exercise.getMaturity();

		int count = 0;
		for (int i = 0; i < timeDiscretization.getNumberOfTimes(); i++) {
			if (timeDiscretization.getTime(i) <= exerciseMaturity + TIME_TOLERANCE) {
				count++;
			}
		}

		if (count == 0) {
			throw new IllegalArgumentException("No model time-grid point is admissible for American exercise.");
		}

		final double[] exerciseTimes = new double[count];
		final int[] scheduleIndices = new int[count];

		int index = 0;
		for (int i = 0; i < timeDiscretization.getNumberOfTimes(); i++) {
			final double time = timeDiscretization.getTime(i);
			if (time <= exerciseMaturity + TIME_TOLERANCE) {
				exerciseTimes[index] = time;
				scheduleIndices[index] = 0;
				index++;
			}
		}

		return new ResolvedExerciseData(exerciseTimes, scheduleIndices);
	}

	private double inferExerciseDateFromSchedules(final int scheduleIndex) {
		final Schedule fixSchedule = fixSchedules[scheduleIndex];
		final Schedule floatSchedule = floatSchedules[scheduleIndex];

		final double fixStart = fixSchedule.getPeriodStart(0);
		final double floatStart = floatSchedule.getPeriodStart(0);

		if (Math.abs(fixStart - floatStart) > TIME_TOLERANCE) {
			throw new IllegalArgumentException(
					"Cannot infer a unique exercise date from schedule pair "
							+ scheduleIndex + ": first fixed-leg start and first float-leg start differ."
					);
		}

		return fixStart;
	}

	private double getFixedLegValue(
			final double time,
			final int scheduleIndex,
			final double stateVariable,
			final FiniteDifferenceInterestRateModel model) {

		final Schedule fixedSchedule = fixSchedules[scheduleIndex];
		final double fixedRate = swapRates[scheduleIndex];
		final double notional = notionals[scheduleIndex];

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < fixedSchedule.getNumberOfPeriods(); periodIndex++) {
			final double paymentTime = fixedSchedule.getPayment(periodIndex);

			if (paymentTime <= time + TIME_TOLERANCE) {
				continue;
			}

			final double accrualFactor = fixedSchedule.getPeriodLength(periodIndex);
			final double discountBond = model.getDiscountBond(time, paymentTime, stateVariable);

			value += notional * fixedRate * accrualFactor * discountBond;
		}

		return value;
	}

	private double getFloatingLegValue(
			final double time,
			final int scheduleIndex,
			final double stateVariable,
			final FiniteDifferenceInterestRateModel model) {

		final Schedule floatingSchedule = floatSchedules[scheduleIndex];
		final double notional = notionals[scheduleIndex];

		double value = 0.0;

		for (int periodIndex = 0; periodIndex < floatingSchedule.getNumberOfPeriods(); periodIndex++) {
			final double fixingTime = floatingSchedule.getFixing(periodIndex);
			final double paymentTime = floatingSchedule.getPayment(periodIndex);

			/*
			 * The intended use is that the schedule supplied for a given
			 * exercise
			 * date is already the remaining forward swap from that date onward.
			 * We therefore ignore already-started periods here.
			 */
			if ((fixingTime < time - TIME_TOLERANCE) || (paymentTime <= time + TIME_TOLERANCE)) {
				continue;
			}

			final double accrualFactor = floatingSchedule.getPeriodLength(periodIndex);
			final double forwardRate = model.getForwardRate(
					forwardCurveName,
					time,
					fixingTime,
					paymentTime,
					stateVariable
					);
			final double discountBond = model.getDiscountBond(time, paymentTime, stateVariable);

			value += notional * forwardRate * accrualFactor * discountBond;
		}

		return value;
	}

	@Override
	public String toString() {
		return "Swaption [exercise=" + exercise
				+ ", explicitExerciseDates=" + Arrays.toString(explicitExerciseDates)
				+ ", swaptionType=" + swaptionType
				+ ", forwardCurveName=" + forwardCurveName
				+ ", swapRates=" + Arrays.toString(swapRates)
				+ ", notionals=" + Arrays.toString(notionals)
				+ ", fixSchedules=" + Arrays.toString(fixSchedules)
				+ ", floatSchedules=" + Arrays.toString(floatSchedules)
				+ "]";
	}

	/**
	 * Container for resolved exercise information.
	 *
	 * @author Alessandro Gnoatto
	 */
	public static final class ResolvedExerciseData {

		/**
		 * The exercise times.
		 */
		private final double[] exerciseTimes;
		/**
		 * The schedule indices.
		 */
		private final int[] scheduleIndices;

		private ResolvedExerciseData(
				final double[] exerciseTimes,
				final int[] scheduleIndices) {
			this.exerciseTimes = exerciseTimes.clone();
			this.scheduleIndices = scheduleIndices.clone();
		}

		/**
		 * Returns the resolved exercise times.
		 *
		 * @return The resolved exercise times.
		 */
		public double[] getExerciseTimes() {
			return exerciseTimes.clone();
		}

		/**
		 * Returns the resolved schedule indices.
		 *
		 * @return The resolved schedule indices.
		 */
		public int[] getScheduleIndices() {
			return scheduleIndices.clone();
		}

		/**
		 * Returns the number of resolved exercise opportunities.
		 *
		 * @return The number of resolved exercise opportunities.
		 */
		public int getNumberOfExerciseTimes() {
			return exerciseTimes.length;
		}

		/**
		 * Returns the exercise time at the given index.
		 *
		 * @param index The index.
		 * @return The exercise time.
		 */
		public double getExerciseTime(final int index) {
			return exerciseTimes[index];
		}

		/**
		 * Returns the schedule index at the given index.
		 *
		 * @param index The index.
		 * @return The schedule index.
		 */
		public int getScheduleIndex(final int index) {
			return scheduleIndices[index];
		}

		/**
		 * Returns the last exercise time.
		 *
		 * @return The last exercise time.
		 */
		public double getLastExerciseTime() {
			return exerciseTimes[exerciseTimes.length - 1];
		}

		/**
		 * Returns the resolved exercise index for the given time.
		 *
		 * @param time The running time.
		 * @return The resolved exercise index.
		 */
		public int findExerciseIndex(final double time) {
			for (int i = 0; i < exerciseTimes.length; i++) {
				if (Math.abs(exerciseTimes[i] - time) <= TIME_TOLERANCE) {
					return i;
				}
			}

			throw new IllegalArgumentException("No exercise index found for time " + time + ".");
		}
	}

	/**
	 * Internal resolved product with model-resolved exercise times.
	 *
	 * @author Alessandro Gnoatto
	 */
	public final class ResolvedSwaption implements FiniteDifferenceInterestRateProduct {

		/**
		 * The resolved exercise data.
		 */
		private final ResolvedExerciseData resolvedExerciseData;

		private ResolvedSwaption(final ResolvedExerciseData resolvedExerciseData) {
			this.resolvedExerciseData = resolvedExerciseData;
		}

		/**
		 * Returns the original outer swaption.
		 *
		 * @return The original swaption.
		 */
		public Swaption getOriginalSwaption() {
			return Swaption.this;
		}

		@Override
		public double[] getEventTimes() {
			return resolvedExerciseData.getExerciseTimes();
		}

		@Override
		public double[] applyEventCondition(
				final double time,
				final double[] valuesAfterEvent,
				final FiniteDifferenceInterestRateModel model) {

			if (valuesAfterEvent == null) {
				throw new IllegalArgumentException("valuesAfterEvent must not be null.");
			}

			final int exerciseIndex = resolvedExerciseData.findExerciseIndex(time);
			final int scheduleIndex = resolvedExerciseData.getScheduleIndex(exerciseIndex);

			final double[] xGrid = model.getSpaceTimeDiscretization().getSpaceGrid(0).getGrid();

			if (valuesAfterEvent.length != xGrid.length) {
				throw new IllegalArgumentException(
						"The value vector length does not match the spatial grid length.");
			}

			final double[] valuesBeforeEvent = valuesAfterEvent.clone();

			for (int i = 0; i < xGrid.length; i++) {
				final double intrinsicValue = getIntrinsicValue(
						time,
						scheduleIndex,
						xGrid[i],
						model
						);
				valuesBeforeEvent[i] = Math.max(valuesBeforeEvent[i], intrinsicValue);
			}

			return valuesBeforeEvent;
		}

		@Override
		public double[] getValue(
				final double evaluationTime,
				final FiniteDifferenceInterestRateModel model) {
			return Swaption.this.getValue(evaluationTime, model);
		}

		@Override
		public double[][] getValues(final FiniteDifferenceInterestRateModel model) {
			return Swaption.this.getValues(model);
		}
	}
}
