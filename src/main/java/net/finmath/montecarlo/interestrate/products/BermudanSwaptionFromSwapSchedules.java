package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.products.Swaption;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.process.ProcessTimeDiscretizationProvider;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implements the valuation of a Bermudan swaption under a <code>LIBORModelMonteCarloSimulationModel</code>
 *
 * @author Christian Fries
 * @version 1.4
 * @date 06.12.2009
 * @date 07.04.2019
 */
public class BermudanSwaptionFromSwapSchedules extends AbstractLIBORMonteCarloProduct implements ProcessTimeDiscretizationProvider, Swaption {

	private static Logger logger = Logger.getLogger("net.finmath");

	public enum SwaptionType{
		PAYER,
		RECEIVER
	}

	private LocalDateTime 			referenceDate;
	private final SwaptionType 		swaptionType;
	private LocalDate[] 			exerciseDates;
	private LocalDate 				swapEndDate;
	private double[]				swaprates;
	private double[] 				notionals;
	private final Schedule[]  		fixSchedules;
	private final Schedule[]  		floatSchedules;

	/**
	 * Create a Bermudan swaption.
	 * 
	 * @param referenceDate The date associated with the inception (t=0) of this product. (Not used).
	 * @param swaptionType The type of the underlying swap (PAYER, RECEIVER).
	 * @param exerciseDates A vector of exercise dates.
	 * @param swapEndDate The final maturity of the underlying swap.
	 * @param swaprates A vector of swap rates for the underlying swaps.
	 * @param notionals A vector of notionals for the underlying swaps.
	 * @param fixSchedules A vector of fix leg schedules for the underlying swaps.
	 * @param floatSchedules A vector of float leg schedules for the underlying swaps.
	 */
	public BermudanSwaptionFromSwapSchedules(LocalDateTime referenceDate, SwaptionType swaptionType, LocalDate[] exerciseDates,
			LocalDate swapEndDate, double[] swaprates, double[] notionals, Schedule[]  fixSchedules,
			Schedule[]  floatSchedules) {
		this.referenceDate = referenceDate;
		this.swaptionType = swaptionType;
		this.swapEndDate = swapEndDate;
		this.swaprates = swaprates;
		this.notionals = notionals;
		this.exerciseDates = exerciseDates;
		this.fixSchedules = fixSchedules;
		this.floatSchedules = floatSchedules;

	}

	/**
	 * Create a Bermudan swaption.
	 * 
	 * @param referenceDate The date associated with the inception (t=0) of this product.
	 * @param swaptionType The type of the underlying swap (PAYER, RECEIVER).
	 * @param exerciseDates A vector of exercise dates.
	 * @param swapEndDate The final maturity of the underlying swap.
	 * @param swaprate A constant swaprate applying to all underlying swaps.
	 * @param notional A constant notional applying to all underlying swaps.
	 * @param fixSchedules A vector of fix leg schedules for the underlying swaps.
	 * @param floatSchedules A vector of float leg schedules for the underlying swaps.
	 */
	public BermudanSwaptionFromSwapSchedules(LocalDateTime referenceDate, SwaptionType swaptionType, LocalDate[] exerciseDates,
			LocalDate swapEndDate, double swaprate, double notional, Schedule[] fixSchedules, Schedule[]  floatSchedules) {
		this(referenceDate, swaptionType, exerciseDates, swapEndDate, IntStream.range(0, exerciseDates.length).mapToDouble(i -> swaprate).toArray(), IntStream.range(0, exerciseDates.length).mapToDouble(i -> notional).toArray(), fixSchedules, floatSchedules);
	}

	/**
	 * Returns the exercise dates.
	 *
	 * @return the exercise dates
	 */
	public LocalDate[] getExerciseDates() {
		return exerciseDates;
	}

	/**
	 * @return the swaptionType
	 */
	public SwaptionType getSwaptionType() {
		return swaptionType;
	}

	/**
	 * @return the swapEndDate
	 */
	public LocalDate getSwapEndDate() {
		return swapEndDate;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		LocalDate modelReferenceDate = model.getReferenceDate().toLocalDate();

		RandomVariable values			= model.getRandomVariableForConstant(0.0);
		RandomVariable exerciseTimes	= new Scalar(Double.POSITIVE_INFINITY);

		double[] regressionBasisfunctionTimes = Stream.concat(Arrays.stream(exerciseDates),Stream.of(swapEndDate)).mapToDouble(date -> { return FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, date); }).sorted().toArray();

		RandomVariable valuesUnderlying	= model.getRandomVariableForConstant(0.0);
		for(int exerciseIndex = exerciseDates.length - 1; exerciseIndex >=0; exerciseIndex--) {
			double exerciseTime = FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, exerciseDates[exerciseIndex]);

			RandomVariable discountedCashflowFixLeg			= getValueUnderlyingNumeraireRelative(model, fixSchedules[exerciseIndex], false, swaprates[exerciseIndex], notionals[exerciseIndex]);
			RandomVariable discountedCashflowFloatingLeg	= getValueUnderlyingNumeraireRelative(model, floatSchedules[exerciseIndex], true, 0.0, notionals[exerciseIndex]);

			// Distinguish whether the swaption is of type "Payer" or "Receiver":
			if(swaptionType.equals(SwaptionType.PAYER)) {
				RandomVariable discountedPayoff = discountedCashflowFloatingLeg.sub(discountedCashflowFixLeg);
				valuesUnderlying = discountedPayoff;//valuesUnderlying.add(discountedPayoff);
			} else if(swaptionType.equals(SwaptionType.RECEIVER)){
				RandomVariable discountedPayoff = discountedCashflowFixLeg.sub(discountedCashflowFloatingLeg);
				valuesUnderlying = discountedPayoff;//valuesUnderlying.add(discountedPayoff);
			}

			RandomVariable discountedTriggerValues = values.sub(valuesUnderlying);

			// Remove foresight through condition expectation
			ConditionalExpectationEstimator conditionalExpectationOperator = getConditionalExpectationEstimator(modelReferenceDate, exerciseTime, model, regressionBasisfunctionTimes);

			// Calculate conditional expectation. Note that no discounting (numeraire division) is required!
			RandomVariable triggerValues = discountedTriggerValues.getConditionalExpectation(conditionalExpectationOperator);

			// Apply the exercise criteria
			// if triggerValues(omega) <=0 choose valuesUnderlying else values
			values = triggerValues.choose(values, valuesUnderlying);
			exerciseTimes = triggerValues.choose(exerciseTimes, new Scalar(exerciseTime));
		}


		// Logging the exercise probabilities for every exercise time.
		if(logger.isLoggable(Level.FINE)) {
			double probabilityToExercise = 1.0;
			for(int exerciseIndex = 0; exerciseIndex < exerciseDates.length; exerciseIndex++) {
				double exerciseTime = FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, exerciseDates[exerciseIndex]);
				double probabilityToExerciseAfter = exerciseTimes.sub(exerciseTime+1E-12).choose(new Scalar(1.0), new Scalar(0.0)).getAverage();
				double probability = probabilityToExercise - probabilityToExerciseAfter;
				probabilityToExercise = probabilityToExerciseAfter;

				logger.finer("Exercise " + (exerciseIndex+1) + " on " + exerciseDates[exerciseIndex] + " with probability " + probability);
			}
			logger.finer("No exercise with probability " + probabilityToExercise);
		}

		// Note that values is a relative price - no numeraire division is required
		RandomVariable	numeraireAtZero	= model.getNumeraire(evaluationTime);
		RandomVariable	monteCarloProbabilitiesAtZero = model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);
		return values;

	}

	@Override
	public TimeDiscretization getProcessTimeDiscretization(LocalDateTime referenceDate) {
		Set<Double> times = new HashSet<>();

		for(int exerciseDateIndex = 0; exerciseDateIndex < exerciseDates.length; exerciseDateIndex++) {
			times.add(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, exerciseDates[exerciseDateIndex].atStartOfDay()));

			Schedule scheduleFixedLeg = fixSchedules[exerciseDateIndex];
			Schedule scheduleFloatLeg = floatSchedules[exerciseDateIndex];

			Function<Period, Double> periodToTime = period -> { return FloatingpointDate.getFloatingPointDateFromDate(referenceDate, period.getPayment().atStartOfDay()); };
			times.addAll(scheduleFixedLeg.getPeriods().stream().map(periodToTime).collect(Collectors.toList()));
			times.addAll(scheduleFloatLeg.getPeriods().stream().map(periodToTime).collect(Collectors.toList()));
		}

		return new TimeDiscretizationFromArray(times);
	}

	/**
	 * Calculated the numeraire relative value of an underlying swap leg.
	 *
	 * @param model The Monte Carlo model.
	 * @param legSchedule The schedule of the leg.
	 * @param paysFloat If true a floating rate is payed.
	 * @param swaprate The swaprate. May be 0.0 for pure floating leg.
	 * @param notional The notional.
	 * @return The sum of the numeraire relative cash flows.
	 * @throws CalculationException Thrown if underlying model failed to calculate stochastic process.
	 */
	private RandomVariable getValueUnderlyingNumeraireRelative(LIBORModelMonteCarloSimulationModel model, Schedule legSchedule, boolean paysFloat, double swaprate, double notional) throws CalculationException {
		RandomVariable value	= model.getRandomVariableForConstant(0.0);

		for(int periodIndex = legSchedule.getNumberOfPeriods() - 1; periodIndex >= 0; periodIndex--) {

			double fixingTime = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), legSchedule.getPeriod(periodIndex).getFixing());
			double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), legSchedule.getPeriod(periodIndex).getPayment());
			double periodLength	= legSchedule.getPeriodLength(periodIndex);

			RandomVariable	numeraireAtPayment  = model.getNumeraire(paymentTime);
			RandomVariable	monteCarloProbabilitiesAtPayment = model.getMonteCarloWeights(paymentTime);
			if(swaprate != 0.0) {
				RandomVariable periodCashFlowFix = model.getRandomVariableForConstant(swaprate * periodLength * notional).div(numeraireAtPayment).mult(monteCarloProbabilitiesAtPayment);
				value = value.add(periodCashFlowFix);
			}
			if(paysFloat) {
				RandomVariable libor = model.getLIBOR(fixingTime, fixingTime, paymentTime);
				RandomVariable periodCashFlowFloat = libor.mult(periodLength).mult(notional).div(numeraireAtPayment).mult(monteCarloProbabilitiesAtPayment);
				value = value.add(periodCashFlowFloat);
			}
		}
		return value;
	}

	/**
	 * The conditional expectation is calculated using a Monte-Carlo regression technique.
	 *
	 * @param referenceDate The date associated with the models time t = 0.
	 * @param exerciseTime The exercise time
	 * @param model The valuation model
	 * @param interimTimes The interimTimes
	 * @return condExpEstimator The condition expectation estimator
	 * @throws CalculationException Thrown if underlying model failed to calculate stochastic process.
	 */
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(LocalDate referenceDate, double exerciseTime, LIBORModelMonteCarloSimulationModel model, double[] interimTimes) throws CalculationException {
		MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(
				getRegressionBasisFunctions(referenceDate,exerciseTime, model, interimTimes)
				);
		return condExpEstimator;
	}

	/**
	 * The conditional expectation is calculated using a Monte-Carlo regression technique.
	 *
	 * @param referenceDate The date associated with the models time t = 0.
	 * @param exerciseTime The exercise time
	 * @param model The valuation model
	 * @param interimTimes The start / end times of the underlyings used to construct basis functions.
	 * @return The vector of regression basis functions.
	 * @throws CalculationException Thrown if underlying model failed to calculate stochastic process.
	 */
	private RandomVariable[] getRegressionBasisFunctions(LocalDate referenceDate, double exerciseTime, LIBORModelMonteCarloSimulationModel model, double[] interimTimes) throws CalculationException {

		ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		double swapMaturity = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, swapEndDate);

		// Constant
		RandomVariable one = new RandomVariableFromDoubleArray(1.0);

		RandomVariable basisFunction = one;
		basisFunctions.add(basisFunction);

		int exerciseIndex = Arrays.binarySearch(interimTimes, exerciseTime);
		if(exerciseIndex < 0) {
			exerciseIndex = -exerciseIndex;
		}
		if(exerciseIndex >= exerciseDates.length) {
			exerciseIndex = exerciseDates.length-1;
		}

		// forward rate to the next period
		RandomVariable rateShort = model.getLIBOR(exerciseTime, exerciseTime, interimTimes[exerciseIndex + 1]);
		basisFunctions.add(rateShort);
		basisFunctions.add(rateShort.pow(2.0));

		// forward rate to the end of the product
		RandomVariable rateLong = model.getLIBOR(exerciseTime, interimTimes[exerciseIndex], swapMaturity);
		basisFunctions.add(rateLong);
		basisFunctions.add(rateLong.pow(2.0));

		// Numeraire (adapted to multicurve framework)
		RandomVariable discountFactor = model.getNumeraire(exerciseTime).invert();
		basisFunctions.add(discountFactor);

		// Cross
		basisFunctions.add(rateLong.mult(discountFactor));

		return basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
	}

	@Override
	public String toString() {
		return "BermudanSwaptionFromSwapSchedules[type: "  + swaptionType.toString() + ", " +"exerciseDate: "  + Arrays.toString(exerciseDates)  +", " + "swapEndDate: " + swapEndDate + ", " + "strike: " + Arrays.toString(swaprates) + ", " + "floatingTenor: " + Arrays.toString(floatSchedules) + ", " + "fixTenor: " + Arrays.toString(fixSchedules) + "]";
	}
}


