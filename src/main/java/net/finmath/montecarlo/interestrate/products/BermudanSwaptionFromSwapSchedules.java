package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntToDoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.products.Swaption;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationLinearRegressionFactory;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegressionFactory;
import net.finmath.montecarlo.conditionalexpectation.RegressionBasisFunctionsProvider;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
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
	private final MonteCarloConditionalExpectationRegressionFactory conditionalExpectationRegressionFactory;
	private final boolean			isUseAnalyticSwapValuationAtExercise = true;

	/**
	 * Create a Bermudan swaption from an array of underlying swap schedules (fix leg and float leg), swap rates and notionals.
	 *
	 * This class implements the class backward algorithm using a provided factory for the
	 * determination of the conditional expectation.
	 *
	 * For <code>conditionalExpectationRegressionFactory</code> you may pass
	 * <code>new MonteCarloConditionalExpectationLinearRegressionFactory()</code> (default) or, e.g., <code>new MonteCarloConditionalExpectationLocalizedOnDependentRegressionFactory(2.0)</code>.
	 *
	 * @param referenceDate The date associated with the inception (t=0) of this product. (Not used).
	 * @param swaptionType The type of the underlying swap (PAYER, RECEIVER).
	 * @param exerciseDates A vector of exercise dates.
	 * @param swapEndDate The final maturity of the underlying swap.
	 * @param swaprates A vector of swap rates for the underlying swaps.
	 * @param notionals A vector of notionals for the underlying swaps.
	 * @param fixSchedules A vector of fix leg schedules for the underlying swaps.
	 * @param floatSchedules A vector of float leg schedules for the underlying swaps.
	 * @param conditionalExpectationRegressionFactory A object implementing a factory creating a conditional expectation estimator from given regression basis functions
	 * @param regressionBasisFunctionProvider An object implementing RegressionBasisFunctionsProvider to provide the basis functions for the estimation of conditional expectations.
	 */
	public BermudanSwaptionFromSwapSchedules(final LocalDateTime referenceDate, final SwaptionType swaptionType, final LocalDate[] exerciseDates,
			final LocalDate swapEndDate, final double[] swaprates, final double[] notionals, final Schedule[]  fixSchedules,
			final Schedule[]  floatSchedules, final MonteCarloConditionalExpectationRegressionFactory conditionalExpectationRegressionFactory, final RegressionBasisFunctionsProvider regressionBasisFunctionProvider) {
		this.referenceDate = referenceDate;
		this.swaptionType = swaptionType;
		this.swapEndDate = swapEndDate;
		this.swaprates = swaprates;
		this.notionals = notionals;
		this.exerciseDates = exerciseDates;
		this.fixSchedules = fixSchedules;
		this.floatSchedules = floatSchedules;

		this.regressionBasisFunctionProvider = regressionBasisFunctionProvider != null ? regressionBasisFunctionProvider : this;
		this.conditionalExpectationRegressionFactory = conditionalExpectationRegressionFactory;
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
	 * @param regressionBasisFunctionProvider An object implementing RegressionBasisFunctionsProvider to provide the basis functions for the estimation of conditional expectations.
	 */
	public BermudanSwaptionFromSwapSchedules(final LocalDateTime referenceDate, final SwaptionType swaptionType, final LocalDate[] exerciseDates,
			final LocalDate swapEndDate, final double[] swaprates, final double[] notionals, final Schedule[]  fixSchedules,
			final Schedule[]  floatSchedules, final RegressionBasisFunctionsProvider regressionBasisFunctionProvider) {
		this(referenceDate, swaptionType, exerciseDates, swapEndDate, swaprates,notionals, fixSchedules, floatSchedules, new MonteCarloConditionalExpectationLinearRegressionFactory(), regressionBasisFunctionProvider);
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
	public BermudanSwaptionFromSwapSchedules(final LocalDateTime referenceDate, final SwaptionType swaptionType, final LocalDate[] exerciseDates,
			final LocalDate swapEndDate, final double[] swaprates, final double[] notionals, final Schedule[]  fixSchedules,
			final Schedule[]  floatSchedules) {
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
	public BermudanSwaptionFromSwapSchedules(final LocalDateTime referenceDate, final SwaptionType swaptionType, final LocalDate[] exerciseDates,
			final LocalDate swapEndDate, final double swaprate, final double notional, final Schedule[] fixSchedules, final Schedule[]  floatSchedules) {
		this(referenceDate, swaptionType, exerciseDates, swapEndDate, IntStream.range(0, exerciseDates.length).mapToDouble(new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return swaprate;
			}
		}).toArray(), IntStream.range(0, exerciseDates.length).mapToDouble(new IntToDoubleFunction() {
			@Override
			public double applyAsDouble(final int i) {
				return notional;
			}
		}).toArray(), fixSchedules, floatSchedules);
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
	public Map<String, Object> getValues(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final LocalDate modelReferenceDate = model.getReferenceDate().toLocalDate();

		RandomVariable values			= model.getRandomVariableForConstant(0.0);
		RandomVariable exerciseTimes	= new Scalar(Double.POSITIVE_INFINITY);

		RandomVariable valuesUnderlying	= model.getRandomVariableForConstant(0.0);
		for(int exerciseIndex = exerciseDates.length - 1; exerciseIndex >=0; exerciseIndex--) {
			final double exerciseTime = FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, exerciseDates[exerciseIndex]);

			final RandomVariable discountedCashflowFixLeg			= getValueUnderlyingNumeraireRelative(model, fixSchedules[exerciseIndex], false, swaprates[exerciseIndex], notionals[exerciseIndex]);
			final RandomVariable discountedCashflowFloatingLeg	= getValueUnderlyingNumeraireRelative(model, floatSchedules[exerciseIndex], true, 0.0, notionals[exerciseIndex]);

			// Distinguish whether the swaption is of type "Payer" or "Receiver":
			if(swaptionType.equals(SwaptionType.PAYER)) {
				final RandomVariable discountedPayoff = discountedCashflowFloatingLeg.sub(discountedCashflowFixLeg);
				valuesUnderlying = discountedPayoff;//valuesUnderlying.add(discountedPayoff);
			} else if(swaptionType.equals(SwaptionType.RECEIVER)){
				final RandomVariable discountedPayoff = discountedCashflowFixLeg.sub(discountedCashflowFloatingLeg);
				valuesUnderlying = discountedPayoff;//valuesUnderlying.add(discountedPayoff);
			}

			final RandomVariable discountedTriggerValues = values.sub(valuesUnderlying);

			// Remove foresight through condition expectation
			final ConditionalExpectationEstimator conditionalExpectationOperator = getConditionalExpectationEstimator(exerciseTime, model);

			// Calculate conditional expectation. Note that no discounting (numeraire division) is required!
			final RandomVariable triggerValues = discountedTriggerValues.getConditionalExpectation(conditionalExpectationOperator);

			// Apply the exercise criteria
			// if triggerValues(omega) <=0 choose valuesUnderlying else values
			values = triggerValues.choose(values, valuesUnderlying);
			exerciseTimes = triggerValues.choose(exerciseTimes, new Scalar(exerciseTime));
		}


		// Logging the exercise probabilities for every exercise time.
		if(logger.isLoggable(Level.FINE)) {
			logger.fine("Exercise probabilitie " + getExerciseProbabilitiesFromTimes(model.getReferenceDate(), exerciseTimes));
			double probabilityToExercise = 1.0;
			for(int exerciseIndex = 0; exerciseIndex < exerciseDates.length; exerciseIndex++) {
				final double exerciseTime = FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, exerciseDates[exerciseIndex]);
				final double probabilityToExerciseAfter = exerciseTimes.sub(exerciseTime+1.0/365.0).choose(new Scalar(1.0), new Scalar(0.0)).getAverage();
				final double probability = probabilityToExercise - probabilityToExerciseAfter;
				probabilityToExercise = probabilityToExerciseAfter;

				logger.finer("Exercise " + (exerciseIndex+1) + " on " + exerciseDates[exerciseIndex] + " with probability " + probability);
			}
			logger.finer("No exercise with probability " + probabilityToExercise);
		}

		// Note that values is a relative price - no numeraire division is required
		final RandomVariable	numeraireAtZero	= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtZero = model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		final Map<String, Object> results = new HashMap<>();
		results.put("values", values);
		results.put("exerciseTimes", exerciseTimes);
		return results;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		return (RandomVariable) getValues(evaluationTime, model).get("values");
	}

	/**
	 * Determines the vector of exercise probabilities for a given {@link RandomVariable} of exerciseTimes.
	 * The exerciseTimes is a random variable of {@link FloatingpointDate} offsets from a given referenceDate.
	 *
	 * @param localDateTime A given reference date.
	 * @param exerciseTimes A {@link RandomVariable} of exercise times given as {@link FloatingpointDate} offsets from the given referenceDate.
	 * @return A vector of exercise probabilities. The length of the vector is <code>exerciseDates.length+1</code>. The last entry is the probability that no exercise occurs.
	 */
	public double[] getExerciseProbabilitiesFromTimes(final LocalDateTime localDateTime, final RandomVariable exerciseTimes) {
		final double[] exerciseProbabilities = new double[exerciseDates.length+1];

		double probabilityToExercise = 1.0;
		for(int exerciseIndex = 0; exerciseIndex < exerciseDates.length; exerciseIndex++) {
			final double exerciseTime = FloatingpointDate.getFloatingPointDateFromDate(localDateTime, exerciseDates[exerciseIndex].atStartOfDay());
			final double probabilityToExerciseAfter = exerciseTimes.sub(exerciseTime+1.0/365.0).choose(new Scalar(1.0), new Scalar(0.0)).getAverage();

			exerciseProbabilities[exerciseIndex] = probabilityToExercise - probabilityToExerciseAfter;
			probabilityToExercise = probabilityToExerciseAfter;
		}
		exerciseProbabilities[exerciseDates.length] = probabilityToExercise;

		return exerciseProbabilities;
	}

	@Override
	public TimeDiscretization getProcessTimeDiscretization(final LocalDateTime referenceDate) {
		final Set<Double> times = new HashSet<>();

		for(int exerciseDateIndex = 0; exerciseDateIndex < exerciseDates.length; exerciseDateIndex++) {
			times.add(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, exerciseDates[exerciseDateIndex].atStartOfDay()));

			final Schedule scheduleFixedLeg = fixSchedules[exerciseDateIndex];
			final Schedule scheduleFloatLeg = floatSchedules[exerciseDateIndex];

			final Function<Period, Double> periodToTime = new Function<Period, Double>() {
				@Override
				public Double apply(final Period period) { return FloatingpointDate.getFloatingPointDateFromDate(referenceDate, period.getPayment().atStartOfDay()); }
			};
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
	private RandomVariable getValueUnderlyingNumeraireRelative(final TermStructureMonteCarloSimulationModel model, final Schedule legSchedule, final boolean paysFloat, final double swaprate, final double notional) throws CalculationException {

		if(isUseAnalyticSwapValuationAtExercise) {
			final double valuationTime = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), legSchedule.getPeriod(0).getFixing());
			final RandomVariable numeraireAtValuationTime  = model.getNumeraire(valuationTime);
			final RandomVariable monteCarloProbabilitiesAtValuationTime = model.getMonteCarloWeights(valuationTime);
			RandomVariable value = SwaptionFromSwapSchedules.getValueOfLegAnalytic(valuationTime, model, legSchedule, paysFloat, swaprate, notional);
			value = value.div(model.getNumeraire(valuationTime)).mult(monteCarloProbabilitiesAtValuationTime);
			return value;
		}
		else {

			RandomVariable value	= model.getRandomVariableForConstant(0.0);

			for(int periodIndex = legSchedule.getNumberOfPeriods() - 1; periodIndex >= 0; periodIndex--) {

				final double fixingTime = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), legSchedule.getPeriod(periodIndex).getFixing());
				final double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), legSchedule.getPeriod(periodIndex).getPayment());
				final double periodLength	= legSchedule.getPeriodLength(periodIndex);

				final RandomVariable	numeraireAtPayment  = model.getNumeraire(paymentTime);
				final RandomVariable	monteCarloProbabilitiesAtPayment = model.getMonteCarloWeights(paymentTime);
				if(swaprate != 0.0) {
					final RandomVariable periodCashFlowFix = model.getRandomVariableForConstant(swaprate * periodLength * notional).div(numeraireAtPayment).mult(monteCarloProbabilitiesAtPayment);
					value = value.add(periodCashFlowFix);
				}
				if(paysFloat) {
					final RandomVariable libor = model.getForwardRate(fixingTime, fixingTime, paymentTime);
					final RandomVariable periodCashFlowFloat = libor.mult(periodLength).mult(notional).div(numeraireAtPayment).mult(monteCarloProbabilitiesAtPayment);
					value = value.add(periodCashFlowFloat);
				}
			}
			return value;
		}
	}

	/**
	 * The conditional expectation is calculated using a Monte-Carlo regression technique.
	 *
	 * @param exerciseTime The exercise time
	 * @param model The valuation model
	 * @return The condition expectation estimator
	 * @throws CalculationException Thrown if underlying model failed to calculate stochastic process.
	 */
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(final double exerciseTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		final RandomVariable[] regressionBasisFunctions = regressionBasisFunctionProvider.getBasisFunctions(exerciseTime, model);
		return conditionalExpectationRegressionFactory.getConditionalExpectationEstimator(regressionBasisFunctions, regressionBasisFunctions);
	}


	@Override
	public RandomVariable[] getBasisFunctions(final double evaluationTime, final MonteCarloSimulationModel model) throws CalculationException {
		final LIBORModelMonteCarloSimulationModel liborModel = (LIBORModelMonteCarloSimulationModel)model;
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
	public RandomVariable[] getBasisFunctions(final double evaluationTime, final LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		final LocalDateTime modelReferenceDate = model.getReferenceDate();

		final double[] regressionBasisfunctionTimes = Stream.concat(Arrays.stream(exerciseDates),Stream.of(swapEndDate)).mapToDouble(new ToDoubleFunction<LocalDate>() {
			@Override
			public double applyAsDouble(final LocalDate date) { return FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, date.atStartOfDay()); }
		}).sorted().toArray();

		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		final double exerciseTime = evaluationTime;

		int exerciseIndex = Arrays.binarySearch(regressionBasisfunctionTimes, exerciseTime);
		if(exerciseIndex < 0) {
			exerciseIndex = -exerciseIndex;
		}
		if(exerciseIndex >= exerciseDates.length) {
			exerciseIndex = exerciseDates.length-1;
		}

		// Constant
		final RandomVariable one = new RandomVariableFromDoubleArray(1.0);
		basisFunctions.add(one);

		// Numeraire (adapted to multicurve framework)
		final RandomVariable discountFactor = model.getNumeraire(exerciseTime).invert();
		basisFunctions.add(discountFactor);

		/*
		 * Add swap rates of underlyings.
		 */
		for(int exerciseIndexUnderlying = exerciseIndex; exerciseIndexUnderlying<exerciseDates.length; exerciseIndexUnderlying++) {
			final RandomVariable floatLeg = SwaptionFromSwapSchedules.getValueOfLegAnalytic(exerciseTime, model, floatSchedules[exerciseIndexUnderlying], true, 0.0, 1.0);
			final RandomVariable annuity = SwaptionFromSwapSchedules.getValueOfLegAnalytic(exerciseTime, model, fixSchedules[exerciseIndexUnderlying], false, 1.0, 1.0);
			final RandomVariable swapRate = floatLeg.div(annuity);
			final RandomVariable basisFunction = swapRate.mult(discountFactor);
			basisFunctions.add(basisFunction);
			basisFunctions.add(basisFunction.squared());
		}

		// forward rate to the next period
		final RandomVariable rateShort = model.getForwardRate(exerciseTime, exerciseTime, regressionBasisfunctionTimes[exerciseIndex + 1]);
		basisFunctions.add(rateShort.mult(discountFactor));
		basisFunctions.add(rateShort.mult(discountFactor).pow(2.0));

		return basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
	}

	/*
	 * Some popular variants to create regression basis functions
	 */

	public RegressionBasisFunctionsProvider getBasisFunctionsProviderWithSwapRates() {
		return new RegressionBasisFunctionsProvider() {
			@Override
			public RandomVariable[] getBasisFunctions(final double evaluationTime, final MonteCarloSimulationModel monteCarloModel) throws CalculationException {
				final LIBORModelMonteCarloSimulationModel model = (LIBORModelMonteCarloSimulationModel)monteCarloModel;

				final LocalDateTime modelReferenceDate = model.getReferenceDate();

				final double[] regressionBasisfunctionTimes = Stream.concat(Arrays.stream(exerciseDates),Stream.of(swapEndDate)).mapToDouble(new ToDoubleFunction<LocalDate>() {
					@Override
					public double applyAsDouble(final LocalDate date) { return FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, date.atStartOfDay()); }
				}).sorted().toArray();

				final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

				final double exerciseTime = evaluationTime;

				int exerciseIndex = Arrays.binarySearch(regressionBasisfunctionTimes, exerciseTime);
				if(exerciseIndex < 0) {
					exerciseIndex = -exerciseIndex;
				}
				if(exerciseIndex >= exerciseDates.length) {
					exerciseIndex = exerciseDates.length-1;
				}

				// Constant
				final RandomVariable one = new RandomVariableFromDoubleArray(1.0);
				final RandomVariable basisFunction = one;
				basisFunctions.add(basisFunction);

				/*
				 * Add swap rates of underlyings.
				 */
				for(int exerciseIndexUnderlying = exerciseIndex; exerciseIndexUnderlying<exerciseDates.length; exerciseIndexUnderlying++) {
					final RandomVariable floatLeg = SwaptionFromSwapSchedules.getValueOfLegAnalytic(exerciseTime, model, floatSchedules[exerciseIndexUnderlying], true, 0.0, 1.0);
					final RandomVariable annuity = SwaptionFromSwapSchedules.getValueOfLegAnalytic(exerciseTime, model, fixSchedules[exerciseIndexUnderlying], false, 1.0, 1.0);
					final RandomVariable swapRate = floatLeg.div(annuity);
					basisFunctions.add(swapRate);
				}

				// forward rate to the next period
				final RandomVariable rateShort = model.getForwardRate(exerciseTime, exerciseTime, regressionBasisfunctionTimes[exerciseIndex + 1]);
				basisFunctions.add(rateShort);
				basisFunctions.add(rateShort.pow(2.0));

				// Numeraire (adapted to multicurve framework)
				final RandomVariable discountFactor = model.getNumeraire(exerciseTime).invert();
				basisFunctions.add(discountFactor);

				return basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
			}
		};
	}

	public RegressionBasisFunctionsProvider getBasisFunctionsProviderWithForwardRates() {
		return new RegressionBasisFunctionsProvider() {
			@Override
			public RandomVariable[] getBasisFunctions(final double evaluationTime, final MonteCarloSimulationModel monteCarloModel) throws CalculationException {
				final LIBORModelMonteCarloSimulationModel model = (LIBORModelMonteCarloSimulationModel)monteCarloModel;

				final LocalDateTime modelReferenceDate = model.getReferenceDate();

				final double[] regressionBasisfunctionTimes = Stream.concat(Arrays.stream(exerciseDates),Stream.of(swapEndDate)).mapToDouble(new ToDoubleFunction<LocalDate>() {
					@Override
					public double applyAsDouble(final LocalDate date) { return FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, date.atStartOfDay()); }
				}).sorted().toArray();

				final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

				final double swapMaturity = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, swapEndDate.atStartOfDay());

				final double exerciseTime = evaluationTime;

				int exerciseIndex = Arrays.binarySearch(regressionBasisfunctionTimes, exerciseTime);
				if(exerciseIndex < 0) {
					exerciseIndex = -exerciseIndex;
				}
				if(exerciseIndex >= exerciseDates.length) {
					exerciseIndex = exerciseDates.length-1;
				}

				// Constant
				final RandomVariable one = new RandomVariableFromDoubleArray(1.0);

				final RandomVariable basisFunction = one;
				basisFunctions.add(basisFunction);

				// forward rate to the next period
				final RandomVariable rateShort = model.getForwardRate(exerciseTime, exerciseTime, regressionBasisfunctionTimes[exerciseIndex + 1]);
				basisFunctions.add(rateShort);
				basisFunctions.add(rateShort.pow(2.0));

				// forward rate to the end of the product
				final RandomVariable rateLong = model.getForwardRate(exerciseTime, regressionBasisfunctionTimes[exerciseIndex], swapMaturity);
				basisFunctions.add(rateLong);
				basisFunctions.add(rateLong.pow(2.0));

				// Numeraire (adapted to multicurve framework)
				final RandomVariable discountFactor = model.getNumeraire(exerciseTime).invert();
				basisFunctions.add(discountFactor);

				// Cross
				basisFunctions.add(rateLong.mult(discountFactor));

				return basisFunctions.toArray(new RandomVariable[basisFunctions.size()]);
			}
		};
	}

	@Override
	public String toString() {
		return "BermudanSwaptionFromSwapSchedules[type: "  + swaptionType.toString() + ", " +"exerciseDate: "  + Arrays.toString(exerciseDates)  +", " + "swapEndDate: " + swapEndDate + ", " + "strike: " + Arrays.toString(swaprates) + ", " + "floatingTenor: " + Arrays.toString(floatSchedules) + ", " + "fixTenor: " + Arrays.toString(fixSchedules) + "]";
	}
}


