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
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.conditionalexpectation.RegressionBasisFunctionsProvider;
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
public class BermudanSwaptionFromSwapSchedules extends AbstractLIBORMonteCarloProduct implements RegressionBasisFunctionsProvider, ProcessTimeDiscretizationProvider, Swaption {

	private static Logger logger = Logger.getLogger("net.finmath");

	public enum SwaptionType{
		PAYER,
		RECEIVER
	}

	private final LocalDateTime		referenceDate;
	private final SwaptionType 		swaptionType;
	private final LocalDate[] 		exerciseDates;
	private final LocalDate 		swapEndDate;
	private final double[]			swaprates;
	private final double[] 			notionals;
	private final Schedule[]  		fixSchedules;
	private final Schedule[]  		floatSchedules;
	private final RegressionBasisFunctionsProvider regressionBasisFunctionProvider;

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
	 * @param regressionBasisFunctionProvider An object implementing RegressionBasisFunctionsProvider to provide the basis functions for the estimation of conditional expectations.
	 */
	public BermudanSwaptionFromSwapSchedules(LocalDateTime referenceDate, SwaptionType swaptionType, LocalDate[] exerciseDates,
			LocalDate swapEndDate, double[] swaprates, double[] notionals, Schedule[]  fixSchedules,
			Schedule[]  floatSchedules, RegressionBasisFunctionsProvider regressionBasisFunctionProvider) {
		this.referenceDate = referenceDate;
		this.swaptionType = swaptionType;
		this.swapEndDate = swapEndDate;
		this.swaprates = swaprates;
		this.notionals = notionals;
		this.exerciseDates = exerciseDates;
		this.fixSchedules = fixSchedules;
		this.floatSchedules = floatSchedules;
		this.regressionBasisFunctionProvider = regressionBasisFunctionProvider != null ? regressionBasisFunctionProvider : this;
	}

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
		this(referenceDate, swaptionType, exerciseDates, swapEndDate, swaprates,notionals, fixSchedules, floatSchedules, null);
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
			ConditionalExpectationEstimator conditionalExpectationOperator = getConditionalExpectationEstimator(exerciseTime, model);

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
	 * @param exerciseTime The exercise time
	 * @param model The valuation model
	 * @return The condition expectation estimator
	 * @throws CalculationException Thrown if underlying model failed to calculate stochastic process.
	 */
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(double exerciseTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(regressionBasisFunctionProvider.getBasisFunctions(exerciseTime, model));
		return condExpEstimator;
	}

	@Override
	public RandomVariable[] getBasisFunctions(double evaluationTime, MonteCarloSimulationModel model) throws CalculationException {
		LIBORModelMonteCarloSimulationModel liborModel = (LIBORModelMonteCarloSimulationModel)model;
		return getBasisFunctions(evaluationTime, liborModel);		
	}
	
	/**
	 * Provides a set of \( \mathcal{F}_{t} \)-measurable random variables which can serve as regression basis functions.
	 * 
	 * @param evaluationTime The evaluation time \( t \) at which the basis function should be observed.
	 * @param model The Monte-Carlo model used to derive the basis function.
	 * @return An \( \mathcal{F}_{t} \)-measurable random variable.
	 * @throws CalculationException Thrown if derivation of the basis function fails.
	 */
	public RandomVariable[] getBasisFunctions(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		
		LocalDateTime modelReferenceDate = model.getReferenceDate();
		
		double[] regressionBasisfunctionTimes = Stream.concat(Arrays.stream(exerciseDates),Stream.of(swapEndDate)).mapToDouble(date -> { return FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, date.atStartOfDay()); }).sorted().toArray();

		ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		double swapMaturity = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, swapEndDate.atStartOfDay());

		double exerciseTime = evaluationTime;

		int exerciseIndex = Arrays.binarySearch(regressionBasisfunctionTimes, exerciseTime);
		if(exerciseIndex < 0) {
			exerciseIndex = -exerciseIndex;
		}
		if(exerciseIndex >= exerciseDates.length) {
			exerciseIndex = exerciseDates.length-1;
		}

		// Constant
		RandomVariable one = new RandomVariableFromDoubleArray(1.0);

		RandomVariable basisFunction = one;
		basisFunctions.add(basisFunction);

		/*
		 * Add swap rates of underlyings.
		 */
		for(int exerciseIndexUnderlying = exerciseIndex; exerciseIndexUnderlying<exerciseDates.length; exerciseIndexUnderlying++) {
			RandomVariable floatLeg = SwaptionFromSwapSchedules.getValueOfLegAnalytic(exerciseTime, model, floatSchedules[exerciseIndexUnderlying], true, 0.0, 1.0);
			RandomVariable annuity = SwaptionFromSwapSchedules.getValueOfLegAnalytic(exerciseTime, model, floatSchedules[exerciseIndexUnderlying], false, 1.0, 1.0);
			RandomVariable swapRate = floatLeg.div(annuity);
			basisFunctions.add(swapRate);
			basisFunctions.add(swapRate.pow(2.0));
		}
		
		// forward rate to the next period
		RandomVariable rateShort = model.getLIBOR(exerciseTime, exerciseTime, regressionBasisfunctionTimes[exerciseIndex + 1]);
		basisFunctions.add(rateShort);
		basisFunctions.add(rateShort.pow(2.0));
		
		// forward rate to the end of the product
		RandomVariable rateLong = model.getLIBOR(exerciseTime, regressionBasisfunctionTimes[exerciseIndex], swapMaturity);
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


