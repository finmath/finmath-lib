package net.finmath.montecarlo.interestrate.products;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.process.ProcessTimeDiscretizationProvider;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implementation of a Monte-Carlo valuation of a swaption valuation being compatible with AAD.
 *
 * The valuation internally uses an analytic valuation of a swap such that the {@link #getValue(double, LIBORModelMonteCarloSimulationModel)} method
 * returns an valuation being \( \mathcal{F}_{t} \}-measurable where \( t \) is the evaluationTime argument.
 *
 * @author Christian Fries
 */
public class SwaptionFromSwapSchedules extends AbstractLIBORMonteCarloProduct implements ProcessTimeDiscretizationProvider, net.finmath.modelling.products.Swaption {

	public enum SwaptionType{
		PAYER,
		RECEIVER
	}

	private final SwaptionType swaptionType;
	private final LocalDate exerciseDate;
	private final Schedule scheduleFixedLeg;
	private final Schedule scheduleFloatLeg;
	private final double swaprate;
	private final double notional;
	private final ValueUnit valueUnit;
	private final LocalDateTime referenceDate;

	public SwaptionFromSwapSchedules(
			LocalDateTime referenceDate,
			SwaptionType swaptionType,
			LocalDate exerciseDate,
			Schedule scheduleFixedLeg,
			Schedule scheduleFloatLeg,
			double swaprate,
			double notional,
			ValueUnit valueUnit) {

		this.referenceDate = referenceDate;
		this.swaptionType = swaptionType;
		this.exerciseDate = exerciseDate;
		this.scheduleFixedLeg = scheduleFixedLeg;
		this.scheduleFloatLeg = scheduleFloatLeg;
		this.swaprate = swaprate;
		this.notional = notional;
		this.valueUnit = valueUnit;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model)
			throws CalculationException {

		LocalDate modelReferenceDate = null;
		try {
			modelReferenceDate = model.getReferenceDate().toLocalDate();
			if(modelReferenceDate == null) {
				modelReferenceDate = referenceDate.toLocalDate();
			}
		}
		catch(UnsupportedOperationException e) {}

		double exerciseTime = FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, exerciseDate);

		RandomVariable discountedCashflowFixLeg			= getValueOfLegAnalytic(exerciseTime, model, scheduleFixedLeg, false, swaprate, notional);
		RandomVariable discountedCashflowFloatingLeg	= getValueOfLegAnalytic(exerciseTime, model, scheduleFloatLeg, true, 0.0, notional);

		// Distinguish whether the swaption is of type "Payer" or "Receiver":
		RandomVariable values;
		if(swaptionType.equals(SwaptionType.PAYER)){
			values = discountedCashflowFloatingLeg.sub(discountedCashflowFixLeg);
		}
		else if(swaptionType.equals(SwaptionType.RECEIVER)){
			values = discountedCashflowFixLeg.sub(discountedCashflowFloatingLeg);
		}
		else {
			throw new IllegalArgumentException("Unkown swaptionType " + swaptionType);
		}

		// Floor at zero
		values = values.floor(0.0);

		RandomVariable	numeraire = model.getNumeraire(exerciseTime);
		RandomVariable	monteCarloProbabilities	= model.getMonteCarloWeights(exerciseTime);
		values = values.div(numeraire).mult(monteCarloProbabilities);

		// Note that values is a relative price - no numeraire division is required
		RandomVariable	numeraireAtZero	= model.getNumeraire(evaluationTime);
		RandomVariable	monteCarloProbabilitiesAtZero = model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		if(valueUnit == ValueUnit.VALUE) {
			return values;
		}

		/*
		 * Need to convert value to different value unit.
		 */

		// Use analytic formula to calculate the options black/bachelier implied vol using the MC price from above:
		double atmSwaprate 	= Swap.getForwardSwapRate(scheduleFixedLeg, scheduleFloatLeg, model.getModel().getForwardRateCurve(), model.getModel().getAnalyticModel());
		double forward = atmSwaprate;
		double optionStrike = swaprate;
		double optionMaturity = FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, exerciseDate);

		double[] swapTenor = new double[scheduleFixedLeg.getNumberOfPeriods() + 1];
		for(int i = 0; i < scheduleFixedLeg.getNumberOfPeriods(); i++) {
			swapTenor[i] = scheduleFixedLeg.getFixing(i);
		}
		swapTenor[scheduleFixedLeg.getNumberOfPeriods()] = scheduleFixedLeg.getPayment(scheduleFixedLeg.getNumberOfPeriods() - 1);

		double swapAnnuity      = SwapAnnuity.getSwapAnnuity(new TimeDiscretizationFromArray(swapTenor), model.getModel().getDiscountCurve());

		switch(valueUnit) {
		case VALUE:
			return values;
		case VOLATILITYNORMAL:
			double volatitliy = AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, swapAnnuity, values.getAverage());
			return model.getRandomVariableForConstant(volatitliy);
		case VOLATILITYLOGNORMAL:
			return model.getRandomVariableForConstant(AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, swapAnnuity, values.getAverage()));
		case VOLATILITYNORMALATM:
			RandomVariable annuity = getValueOfLegAnalytic(evaluationTime, model, scheduleFixedLeg, false, 1.0, notional);
			annuity = annuity.div(numeraire).mult(monteCarloProbabilities).mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);
			return values.div(Math.sqrt(optionMaturity / Math.PI / 2.0)).div(annuity.average());
		default:
			throw new IllegalArgumentException("Unsupported valueUnit " + valueUnit.name());
		}
	}

	@Override
	public TimeDiscretization getProcessTimeDiscretization(LocalDateTime referenceDate) {
		Set<Double> times = new HashSet<>();

		times.add(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, exerciseDate.atStartOfDay()));

		Function<Period, Double> periodToTime = period -> { return FloatingpointDate.getFloatingPointDateFromDate(referenceDate, period.getPayment().atStartOfDay()); };
		times.addAll(scheduleFixedLeg.getPeriods().stream().map(periodToTime).collect(Collectors.toList()));
		times.addAll(scheduleFloatLeg.getPeriods().stream().map(periodToTime).collect(Collectors.toList()));

		return new TimeDiscretizationFromArray(times);
	}

	/**
	 * @return the exercise date
	 */
	public LocalDate getExerciseDate() {
		return exerciseDate;
	}


	/**
	 * Determines the time \( t \)-measurable value of a swap leg (can handle fix or float).
	 *
	 * @param evaluationTime The time \( t \) conditional to which the value is calculated.
	 * @param model The model implmeneting LIBORModelMonteCarloSimulationModel.
	 * @param schedule The schedule of the leg.
	 * @param paysFloatingRate If true, the leg will pay {@link LIBORModelMonteCarloSimulationModel#getLIBOR(double, double, double)}
	 * @param fixRate The fixed rate (if any)
	 * @param notional The notional
	 * @return The time \( t \)-measurable value
	 * @throws CalculationException
	 */
	public RandomVariable getValueOfLegAnalytic(double evaluationTime, LIBORModelMonteCarloSimulationModel model, Schedule schedule, boolean paysFloatingRate, double fixRate, double notional) throws CalculationException {

		LocalDate modelReferenceDate = null;
		try {
			modelReferenceDate = model.getReferenceDate().toLocalDate();
			if(modelReferenceDate == null) {
				modelReferenceDate = referenceDate.toLocalDate();
			}
		}
		catch(UnsupportedOperationException e) {}

		RandomVariable discountedCashflowFloatingLeg	= model.getRandomVariableForConstant(0.0);
		for(int peridIndex = schedule.getNumberOfPeriods() - 1; peridIndex >= 0; peridIndex--) {
			Period period = schedule.getPeriod(peridIndex);
			double paymentTime		= FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, period.getPayment());
			double fixingTime		= FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, period.getFixing());
			double periodLength		= schedule.getPeriodLength(peridIndex);

			/*
			 * Note that it is important that getForwardDiscountBond and getLIBOR are called with evaluationTime = exerciseTime.
			 */
			RandomVariable discountBond = model.getModel().getForwardDiscountBond(evaluationTime, paymentTime);
			if(paysFloatingRate) {
				RandomVariable libor	= model.getLIBOR(evaluationTime, fixingTime, paymentTime);
				RandomVariable periodCashFlow = libor.mult(periodLength).mult(notional);
				discountedCashflowFloatingLeg = discountedCashflowFloatingLeg.add(periodCashFlow.mult(discountBond));
			}
			if(fixRate != 0) {
				RandomVariable periodCashFlow = model.getRandomVariableForConstant(swaprate * periodLength * notional);
				discountedCashflowFloatingLeg = discountedCashflowFloatingLeg.add(periodCashFlow.mult(discountBond));
			}
		}
		return discountedCashflowFloatingLeg;

	}

	@Override
	public String toString() {
		return "SwaptionConditionalAnalytic [swaptionType=" + swaptionType + ", referenceDate=" + referenceDate + ", exerciseDate="
				+ exerciseDate + ", swaprate=" + swaprate + ", valueUnit=" + valueUnit
				+ ", fixMetaSchedule=" + scheduleFixedLeg + ", floatMetaSchedule="
				+ scheduleFloatLeg + ", notional=" + notional + "]";
	}
}
