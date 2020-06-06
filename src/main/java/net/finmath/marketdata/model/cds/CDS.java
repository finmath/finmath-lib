package net.finmath.marketdata.model.cds;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.products.AbstractAnalyticProduct;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleFromPeriods;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Implements the valuation of a CDS (fixed-fee CDS, floating-fee CDS, with upfront or without)
 * from the perspective of the protection buyer with unit notional of 1 using curves:
 * <ul>
 * 	<li>a forward curve, if the bond has floating rate coupons</li>
 * 	<li>a discount curve as a base curve for discounting</li>
 * 	<li>a survival probability curve for additional credit risk related discount factor</li>
 * 	<li>a recovery rate curve to model a non-constant recovery rate term-structure</li>
 * </ul>
 *
 * Support for day counting is provided via the class implementing
 * <code>ScheduleFromPeriods</code>.
 *
 * @author Matthias Föhr
 * @author Christian Fries
 */
public class CDS extends AbstractAnalyticProduct implements AnalyticProduct{

	private final Schedule schedule;
	private final String discountCurveName;
	private final String forwardCurveName; //Set null if fixed-fee CDS
	private final String survivalProbabilityCurveName;
	private final String recoveryRateCurveName;
	private final double fixedFee; //Set equal zero if floating rate CDS
	private final double floatingFeeSpread;
	private final double upfrontPayment; //From the perspective of the protection buyer (<0 if upfront is recieved)! Set equal to 0 if conventional CDS spread is considered
	private final LocalDate tradeDate; // Trading Date of the CDS contract
	private final CDS.ValuationModel valuationModel; // Either DISCRETE, JPM or JPM_NOACCFEE (DISCRETE = fee and recovery payments only at tenor dates, JPM = fee and recovery payments every business day according to JP Morgan Modell, JPM_NOACCFEE = JPM with fee payments only at tenor dates)
	private final CDS.DirtyCleanPrice dirtyCleanPrice; // Either CLEAN or DIRTY price
	private final boolean useFinerDiscretization;

	/**
	 * Creates a CDS.
	 *
	 * @param schedule Schedule of the CDS.
	 * @param discountCurveName Name of the discount curve.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fixed fee CDS
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param recoveryRateCurveName Name of the Recovery Rate CurveFromInterpolationPoints (1 Recovery Rate for each timestep in the scheduleFromPeriods).
	 * @param fixedFee The fixed fee of the CDS expressed as absolute value.
	 * @param floatingFeeSpread The floating fee spread of the CDS expressed as absolute value.
	 * @param upfrontPayment The initial upfront payment of the CDS from the perspective of the protection buyer(Nonzero if fee != conventional CDS spread).
	 * @param tradeDate Trading Date of the CDS contract
	 * @param valuationModel Defines the valuation model used (DISCRETE, JPM, JPM_NOACCFEE)
	 * @param dirtyCleanPrice Defines if CLEAN or DIRTY price is considered
	 * @param useFinerDiscretization Boolean if finer Discretization (discountCurve Dates + CDS Dates) is used
	 */
	private CDS(Schedule       schedule,
			String                  discountCurveName,
			String                  forwardCurveName,
			String                  survivalProbabilityCurveName,
			String                  recoveryRateCurveName,
			double                  fixedFee,
			double                  floatingFeeSpread,
			double                  upfrontPayment,
			LocalDate               tradeDate,
			CDS.ValuationModel        valuationModel,
			CDS.DirtyCleanPrice     dirtyCleanPrice,
			boolean                 useFinerDiscretization)
	{
		super();
		this.schedule = schedule;
		this.discountCurveName = discountCurveName;
		this.forwardCurveName = forwardCurveName;
		this.survivalProbabilityCurveName = survivalProbabilityCurveName;
		this.recoveryRateCurveName = recoveryRateCurveName;
		this.fixedFee = fixedFee;
		this.floatingFeeSpread = floatingFeeSpread;
		this.upfrontPayment = upfrontPayment;
		this.tradeDate = tradeDate;
		this.valuationModel = valuationModel;
		this.dirtyCleanPrice = dirtyCleanPrice;
		this.useFinerDiscretization = useFinerDiscretization;
	}

	/**
	 * Creates a CDS with a fixed fee, no upfront.
	 *
	 * @param schedule Schedule of the CDS.
	 * @param discountCurveName Name of the discount curve.
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param recoveryRateCurveName Name of the Recovery Rate CurveFromInterpolationPoints (1 Recovery Rate for each timestep in the scheduleFromPeriods).
	 * @param fixedFee The fixed fee of the CDS expressed as absolute value.
	 * @param tradeDate Trading Date of the CDS contract
	 * @param valuationModel Defines the valuation model used (DISCRETE, JPM, JPM_NOACCFEE)
	 * @param dirtyCleanPrice Defines if CLEAN or DIRTY price is considered
	 * @param useFinerDiscretization Boolean if finer Discretization (discountCurve Dates + CDS Dates) is used
	 */
	public CDS(Schedule schedule, String discountCurveName, String survivalProbabilityCurveName, String recoveryRateCurveName, double fixedFee, LocalDate tradeDate, CDS.ValuationModel valuationModel, CDS.DirtyCleanPrice dirtyCleanPrice, boolean useFinerDiscretization) {
		this(schedule, discountCurveName, null,survivalProbabilityCurveName, recoveryRateCurveName, fixedFee, 0, 0, tradeDate, valuationModel, dirtyCleanPrice, useFinerDiscretization );
	}

	/**
	 * Creates a CDS with a fixed fee, with upfront.
	 *
	 * @param schedule Schedule of the CDS.
	 * @param discountCurveName Name of the discount curve.
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param recoveryRateCurveName Name of the Recovery Rate CurveFromInterpolationPoints (1 Recovery Rate for each timestep in the scheduleFromPeriods).
	 * @param fixedFee The fixed fee of the CDS expressed as absolute value.
	 * @param tradeDate Trading Date of the CDS contract
	 * @param upfrontPayment The initial upfront payment of the CDS from the perspective of the protection buyer(Nonzero if fee != conventional CDS spread).
	 * @param valuationModel Defines the valuation model used (DISCRETE, JPM, JPM_NOACCFEE)
	 * @param dirtyCleanPrice Defines if CLEAN or DIRTY price is considered
	 * @param useFinerDiscretization Boolean if finer Discretization (discountCurve Dates + CDS Dates) is used
	 */
	public CDS(Schedule schedule, String discountCurveName, String survivalProbabilityCurveName, String recoveryRateCurveName, double fixedFee, double upfrontPayment, LocalDate tradeDate, CDS.ValuationModel valuationModel, CDS.DirtyCleanPrice dirtyCleanPrice, boolean useFinerDiscretization) {
		this(schedule, discountCurveName, null,survivalProbabilityCurveName, recoveryRateCurveName, fixedFee, 0, upfrontPayment, tradeDate, valuationModel, dirtyCleanPrice, useFinerDiscretization );
	}

	/**
	 * Creates a CDS with a floating fee, no upfront.
	 *
	 * @param schedule Schedule of the CDS.
	 * @param discountCurveName Name of the discount curve.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fixed fee CDS
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param recoveryRateCurveName Name of the Recovery Rate CurveFromInterpolationPoints (1 Recovery Rate for each timestep in the scheduleFromPeriods).
	 * @param floatingFeeSpread The floating fee spread of the CDS expressed as absolute value.
	 * @param tradeDate Trading Date of the CDS contract
	 * @param valuationModel Defines the valuation model used (DISCRETE, JPM, JPM_NOACCFEE)
	 * @param dirtyCleanPrice Defines if CLEAN or DIRTY price is considered
	 * @param useFinerDiscretization Boolean if finer Discretization (discountCurve Dates + CDS Dates) is used
	 */
	public CDS(ScheduleFromPeriods schedule, String discountCurveName, String forwardCurveName, String survivalProbabilityCurveName, String recoveryRateCurveName, double floatingFeeSpread, LocalDate tradeDate, CDS.ValuationModel valuationModel, CDS.DirtyCleanPrice dirtyCleanPrice, boolean useFinerDiscretization) {
		this(schedule, discountCurveName, forwardCurveName, survivalProbabilityCurveName, recoveryRateCurveName, 0, floatingFeeSpread, 0, tradeDate, valuationModel, dirtyCleanPrice, useFinerDiscretization );
	}

	/**
	 * Creates a CDS with a floating fee, with upfront.
	 *
	 * @param schedule Schedule of the CDS.
	 * @param discountCurveName Name of the discount curve.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fixed fee CDS
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param recoveryRateCurveName Name of the Recovery Rate CurveFromInterpolationPoints (1 Recovery Rate for each timestep in the scheduleFromPeriods).
	 * @param floatingFeeSpread The floating fee spread of the CDS expressed as absolute value.
	 * @param upfrontPayment The initial upfront payment of the CDS from the perspective of the protection buyer(Nonzero if fee != conventional CDS spread).
	 * @param tradeDate Trading Date of the CDS contract
	 * @param valuationModel Defines the valuation model used (DISCRETE, JPM, JPM_NOACCFEE)
	 * @param dirtyCleanPrice Defines if CLEAN or DIRTY price is considered
	 * @param useFinerDiscretization Boolean if finer Discretization (discountCurve Dates + CDS Dates) is used
	 */
	public CDS(ScheduleFromPeriods schedule, String discountCurveName, String forwardCurveName, String survivalProbabilityCurveName, String recoveryRateCurveName, double floatingFeeSpread, double upfrontPayment, LocalDate tradeDate, CDS.ValuationModel valuationModel, CDS.DirtyCleanPrice dirtyCleanPrice, boolean useFinerDiscretization) {
		this(schedule, discountCurveName, forwardCurveName, survivalProbabilityCurveName, recoveryRateCurveName, 0, floatingFeeSpread, upfrontPayment, tradeDate, valuationModel, dirtyCleanPrice, useFinerDiscretization );
	}


	/**
	 * Evaluates the CDS at the evaluationTime according to a Analytic Model.
	 *
	 * @param evaluationTime Evaluation time of the pricing.
	 * @param model Analytic Model, within the CDS is priced.
	 */
	@Override
	public double getValue(double evaluationTime, AnalyticModel model) {

		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final ForwardCurve forwardCurveInterpolation = model.getForwardCurve(forwardCurveName);
		if(forwardCurveInterpolation == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		// TODO Remove cast.
		final DiscountCurveInterpolation discountCurve = (DiscountCurveInterpolation) model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		final Curve survivalProbabilityCurve = model.getCurve(survivalProbabilityCurveName);
		if(survivalProbabilityCurve == null) {
			throw new IllegalArgumentException("No survival probability curve with name '" + survivalProbabilityCurveName + "' was found in the model:\n" + model.toString());
		}

		final Curve recoveryRateCurve = model.getCurve(recoveryRateCurveName);
		if(recoveryRateCurve == null) {
			throw new IllegalArgumentException("No recovery rate curve with name '" + recoveryRateCurveName + "' was found in the model:\n" + model.toString());
		}

		final BusinessdayCalendarExcludingWeekends businessdayCalendarExcludingWeekends = new BusinessdayCalendarExcludingWeekends();
		final LocalDate effectiveDate = businessdayCalendarExcludingWeekends.getRolledDate(tradeDate, 1);     // Effective Date is Business Day after trading date [IGNORES HOLIDAYS]


		// Create joint array of discountfactor times and payment dates
		final Set<Double> jointTimeDiscretizationSet = new HashSet<Double>();
		final double[] timesDiscountCurve = discountCurve.getTimes();
		for(int i=0; i<timesDiscountCurve.length; i++){
			jointTimeDiscretizationSet.add(timesDiscountCurve[i]);
		}
		for(int i=0; i<schedule.getNumberOfPeriods();i++){
			jointTimeDiscretizationSet.add(schedule.getPeriodStart(i));
			jointTimeDiscretizationSet.add(schedule.getPeriodEnd(i));
		}
		final Double[] jointTimeDiscretization = jointTimeDiscretizationSet.toArray(new Double[0]);
		Arrays.sort(jointTimeDiscretization);

		final boolean upfrontIsNonzero = (upfrontPayment != 0);

		double value = 0.0;

		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods(); periodIndex++) {

			// Consider upfront payments if defined
			if (upfrontIsNonzero && periodIndex == 0) {
				// Pay upfront on trade date.
				final double upfrontPaymentDate = FloatingpointDate.getFloatingPointDateFromDate( schedule.getReferenceDate() , tradeDate );
				final double discountFactor = discountCurve.getDiscountFactor(model, upfrontPaymentDate);
				final double survivalProbabilityFactor = survivalProbabilityCurve.getValue(model, upfrontPaymentDate);
				value += -upfrontPayment * discountFactor * survivalProbabilityFactor;
			}

			final double paymentDate = schedule.getPayment(periodIndex);
			double periodLength = schedule.getPeriodLength(periodIndex);

			// When CLEAN Price => Adjust period Length in first period [First coupon not paid in full]
			if (periodIndex == 0 && dirtyCleanPrice == DirtyCleanPrice.CLEAN) {
				periodLength = schedule.getDaycountconvention().getDaycountFraction(effectiveDate, schedule.getPeriod(0).getPeriodEnd());
			}
			// Adjust period Length in last period by 1 businessday (CDS convention)
			if (periodIndex == schedule.getNumberOfPeriods() - 1) {
				periodLength += schedule.getDaycountconvention().getDaycountFraction(schedule.getPeriod(periodIndex).getPeriodEnd(), schedule.getPeriod(periodIndex).getPeriodEnd().plusDays(1));
			}

			// The protection in the first period starts at the effective date = trade date + 1 business day
			final double previousPaymentDate = periodIndex > 0 ? schedule.getPayment(periodIndex - 1) : 0.0 + schedule.getDaycountconvention().getDaycountFraction(tradeDate, effectiveDate);

			double discountFactor = paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;
			double previousDiscountFactor = previousPaymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, previousPaymentDate) : 1.0;

			double survivalProbabilityFactor = paymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, paymentDate) : 0.0;
			double previousSurvivalProbabilityFactor = previousPaymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, previousPaymentDate) : 1.0;

			//Fee to be paid
			double feePayment = fixedFee;
			if (forwardCurveInterpolation != null) {
				feePayment = floatingFeeSpread + forwardCurveInterpolation.getForward(model, schedule.getFixing(periodIndex));
			}
			// Recovery rate in case of default
			final double recoveryRate = recoveryRateCurve.getValue(model, paymentDate);

			// Precomputation for JPM and JPM_NOACCFEE model
			final double periodStartTime = Math.max(schedule.getPeriodStart(periodIndex), evaluationTime);
			final double periodEndTime = Math.max(schedule.getPeriodEnd(periodIndex), evaluationTime);
			double forwardInterestRateFactor = (Math.log(discountCurve.getValue(model, periodStartTime)) - Math.log(discountCurve.getValue(model, periodEndTime))) / (periodEndTime - periodStartTime);
			double hazardRateFactor = (Math.log(survivalProbabilityCurve.getValue(model, periodStartTime)) - Math.log(survivalProbabilityCurve.getValue(model, periodEndTime))) / (periodEndTime - periodStartTime);
			double accruedFactorJPM = hazardRateFactor * (1 / Math.pow(forwardInterestRateFactor + hazardRateFactor, 2) - ((periodEndTime - periodStartTime) / (forwardInterestRateFactor + hazardRateFactor) + 1 / Math.pow(forwardInterestRateFactor + hazardRateFactor, 2)) * (Math.exp(-(forwardInterestRateFactor + hazardRateFactor) * (periodEndTime - periodStartTime))));

			// Switch valuation models
			switch (valuationModel) {
			case DISCRETE:
				value += -feePayment * periodLength * discountFactor * survivalProbabilityFactor; // Fee payment [Protection Buyer View]
				value += Math.max(1.0 - recoveryRate,0) * discountFactor * (previousSurvivalProbabilityFactor - survivalProbabilityFactor); // Recovery payment [Protection Buyer View]
				break;
			case JPM:
				if (useFinerDiscretization) {
					value += -feePayment * periodLength * discountFactor * survivalProbabilityFactor; // Fee payment [Protection Buyer View]
					// JPM Model on jointTimeDiscretization
					final int startIndex = Arrays.binarySearch(jointTimeDiscretization, periodStartTime);		// was ArrayUtils.indexOf
					final int endIndex = Arrays.binarySearch(jointTimeDiscretization, periodEndTime);			// was ArrayUtils.indexOf
					// Loop over subperiods
					for (int subTimeIndex = startIndex; subTimeIndex < endIndex; subTimeIndex++) {
						final double subPeriodStartTime = jointTimeDiscretization[subTimeIndex];
						final double subPeriodEndTime = jointTimeDiscretization[subTimeIndex + 1];
						final double subPeriodLength = subPeriodEndTime - subPeriodStartTime;
						discountFactor = discountCurve.getDiscountFactor(model, subPeriodEndTime);
						previousDiscountFactor = discountCurve.getDiscountFactor(model, subPeriodStartTime);
						survivalProbabilityFactor = survivalProbabilityCurve.getValue(model, subPeriodEndTime);
						previousSurvivalProbabilityFactor = survivalProbabilityCurve.getValue(model, subPeriodStartTime);
						forwardInterestRateFactor = (Math.log(discountCurve.getValue(model, subPeriodStartTime)) - Math.log(discountCurve.getValue(model, subPeriodEndTime))) / (subPeriodEndTime - subPeriodStartTime);
						hazardRateFactor = (Math.log(survivalProbabilityCurve.getValue(model, subPeriodStartTime)) - Math.log(survivalProbabilityCurve.getValue(model, subPeriodEndTime))) / (subPeriodEndTime - subPeriodStartTime);
						accruedFactorJPM = hazardRateFactor * (1 / Math.pow(forwardInterestRateFactor + hazardRateFactor, 2) - ((subPeriodEndTime - subPeriodStartTime) / (forwardInterestRateFactor + hazardRateFactor) + 1 / Math.pow(forwardInterestRateFactor + hazardRateFactor, 2)) * (Math.exp(-(forwardInterestRateFactor + hazardRateFactor) * (subPeriodEndTime - subPeriodStartTime))));
						value += -feePayment * subPeriodLength * previousDiscountFactor * previousSurvivalProbabilityFactor * accruedFactorJPM; // Expected accrued fee according to JPM model
						value += Math.max(1.0 - recoveryRate,0) * (previousDiscountFactor * previousSurvivalProbabilityFactor - discountFactor * survivalProbabilityFactor) * hazardRateFactor / (hazardRateFactor + forwardInterestRateFactor); // Adjusted Recovery payment [Protection Buyer View]
					}
				}
				else {
					value += -feePayment * periodLength * discountFactor * survivalProbabilityFactor; // Fee payment [Protection Buyer View]
					value += -feePayment * periodLength * previousDiscountFactor * previousSurvivalProbabilityFactor * accruedFactorJPM; // Expected accrued fee according to JPM model
					value += Math.max(1.0 - recoveryRate,0) * (previousDiscountFactor * previousSurvivalProbabilityFactor - discountFactor * survivalProbabilityFactor) * hazardRateFactor / (hazardRateFactor + forwardInterestRateFactor); // Adjusted Recovery payment [Protection Buyer View]
				}
				break;
			case JPM_NOACCFEE:
				if (useFinerDiscretization) {
					value += -feePayment * periodLength * discountFactor * survivalProbabilityFactor;
					// JPM Model on jointTimeDiscretization
					final int startIndex = Arrays.binarySearch(jointTimeDiscretization, periodStartTime);
					final int endIndex = Arrays.binarySearch(jointTimeDiscretization, periodEndTime);
					// Loop over subperiods
					for (int subTimeIndex = startIndex; subTimeIndex < endIndex; subTimeIndex++) {
						final double subPeriodStartTime = jointTimeDiscretization[subTimeIndex];
						final double subPeriodEndTime = jointTimeDiscretization[subTimeIndex + 1];
						discountFactor = discountCurve.getDiscountFactor(model, subPeriodEndTime);
						previousDiscountFactor = discountCurve.getDiscountFactor(model, subPeriodStartTime);
						survivalProbabilityFactor = survivalProbabilityCurve.getValue(model, subPeriodEndTime);
						previousSurvivalProbabilityFactor = survivalProbabilityCurve.getValue(model, subPeriodStartTime);
						forwardInterestRateFactor = (Math.log(discountCurve.getValue(model, subPeriodStartTime)) - Math.log(discountCurve.getValue(model, subPeriodEndTime))) / (subPeriodEndTime - subPeriodStartTime);
						hazardRateFactor = (Math.log(survivalProbabilityCurve.getValue(model, subPeriodStartTime)) - Math.log(survivalProbabilityCurve.getValue(model, subPeriodEndTime))) / (subPeriodEndTime - subPeriodStartTime);
						value += Math.max(1.0 - recoveryRate,0) * (previousDiscountFactor * previousSurvivalProbabilityFactor - discountFactor * survivalProbabilityFactor) * hazardRateFactor / (hazardRateFactor + forwardInterestRateFactor); // Adjusted Recovery payment [Protection Buyer View]
					}

				}
				else{
					value += -feePayment * periodLength * discountFactor * survivalProbabilityFactor; // Fee payment [Protection Buyer View]
					value += Math.max(1.0 - recoveryRate,0) * (previousDiscountFactor * previousSurvivalProbabilityFactor - discountFactor * survivalProbabilityFactor) * hazardRateFactor / (hazardRateFactor + forwardInterestRateFactor); // Adjusted Recovery payment [Protection Buyer View]
				}
				break;
			default:
				throw new UnsupportedOperationException("Unknown valuation model: " + valuationModel);
			}
		}

		return value / discountCurve.getDiscountFactor(model, evaluationTime);
	}


	/**
	 * Calculates the conventional CDS spread, i.e. the fee payment such that the CDS is valued at 0 (and no upfront).
	 * Treats conventional CDS spread as a FIXED fee,  not floating.
	 *
	 * @param evaluationTime Evaluation time of the pricing.
	 * @param calibratedModelJPM Analytic Model, within the CDS is priced.
	 * @return The conventional CDS spread.
	 */
	public double getConventionalSpread( double evaluationTime, AnalyticModel calibratedModelJPM ){

		final ForwardCurve forwardCurveInterpolation = calibratedModelJPM.getForwardCurve(forwardCurveName);
		if(forwardCurveInterpolation == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + calibratedModelJPM.toString());
		}

		final DiscountCurve discountCurveInterpolation = calibratedModelJPM.getDiscountCurve(discountCurveName);
		if(discountCurveInterpolation == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + calibratedModelJPM.toString());
		}

		final Curve survivalProbabilityCurve = calibratedModelJPM.getCurve(survivalProbabilityCurveName);
		if(survivalProbabilityCurve == null) {
			throw new IllegalArgumentException("No survival probability curve with name '" + survivalProbabilityCurveName + "' was found in the model:\n" + calibratedModelJPM.toString());
		}

		final Curve recoveryRateCurve = calibratedModelJPM.getCurve(recoveryRateCurveName);
		if(recoveryRateCurve == null) {
			throw new IllegalArgumentException("No recovery rate curve with name '" + recoveryRateCurveName + "' was found in the model:\n" + calibratedModelJPM.toString());
		}

		double valueFloatingLeg = 0.0;
		double valueFixedLeg = 0.0;

		for(int periodIndex=0; periodIndex < schedule.getNumberOfPeriods(); periodIndex++){

			final double paymentDate	= schedule.getPayment(periodIndex);
			final double periodLength	= schedule.getPeriodLength(periodIndex);

			final double discountFactor	= paymentDate > evaluationTime ? discountCurveInterpolation.getDiscountFactor(calibratedModelJPM, paymentDate) : 0.0;
			final double survivalProbabilityFactor	= paymentDate > evaluationTime ? survivalProbabilityCurve.getValue(calibratedModelJPM, paymentDate) : 0.0;

			valueFixedLeg += periodLength * discountFactor * survivalProbabilityFactor;

			double previousPaymentDate = 0	;
			if(periodIndex>0) {
				previousPaymentDate	= schedule.getPayment(periodIndex-1);
			}

			final double previousSurvivalProbabilityFactor	= previousPaymentDate > evaluationTime ? survivalProbabilityCurve.getValue(calibratedModelJPM, previousPaymentDate) : 1.0;

			valueFloatingLeg += (1.0 - recoveryRateCurve.getValue(calibratedModelJPM,paymentDate)) * discountFactor * (previousSurvivalProbabilityFactor- survivalProbabilityFactor);
		}

		final double conventionalSpread = valueFloatingLeg/ valueFixedLeg;

		return conventionalSpread;
	}

	/**
	 * Returns the CDS fee payment of the period with the given index. The analytic model is needed in case of a floating fee.
	 *
	 * @param periodIndex The index of the period of interest.
	 * @param model The model under which the product is valued.
	 * @return The value of the fee payment in the given period.
	 */
	public double getFeePayment(int periodIndex, AnalyticModel model) {

		final ForwardCurve forwardCurveInterpolation = model.getForwardCurve(forwardCurveName);
		if(forwardCurveInterpolation == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		final double periodLength	= schedule.getPeriodLength(periodIndex);
		double couponPayment = fixedFee;
		if(forwardCurveInterpolation != null ) {
			couponPayment = floatingFeeSpread+forwardCurveInterpolation.getForward(model, schedule.getFixing(periodIndex));
		}
		return couponPayment*periodLength;
	}


	/**
	 * Returns the accrued fee of the CDS for a given date.
	 *
	 * @param date The date of interest.
	 * @param model The model under which the product is valued.
	 * @return The accrued fee.
	 */
	public double getAccruedFee(LocalDate date, AnalyticModel model) {
		final int periodIndex=schedule.getPeriodIndex(date);
		final Period period=schedule.getPeriod(periodIndex);
		final DayCountConvention dcc= schedule.getDaycountconvention();
		final double accruedFee = getFeePayment(periodIndex, model)*(dcc.getDaycountFraction(period.getPeriodStart(), date))/schedule.getPeriodLength(periodIndex);
		return accruedFee;
	}

	/**
	 * Returns the accrued interest of the bond for a given time.
	 *
	 * @param time The time of interest as double.
	 * @param model The model under which the product is valued.
	 * @return The accrued interest.
	 */
	public double getAccruedFee(double time, AnalyticModelFromCurvesAndVols model) {
		final LocalDate date= FloatingpointDate.getDateFromFloatingPointDate(schedule.getReferenceDate(), time);
		return getAccruedFee(date, model);
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public String getDiscountCurveName() {
		return discountCurveName;
	}

	public String getForwardCurveName() {
		return forwardCurveName;
	}

	public String getSurvivalProbabilityCurveName() {
		return survivalProbabilityCurveName;
	}

	public String getRecoveryRateCurveName() {
		return recoveryRateCurveName;
	}

	public double getFixedFee() {
		return fixedFee;
	}

	public double getFloatingFeeSpread() {
		return floatingFeeSpread;
	}

	public double getUpfrontPayment() {
		return upfrontPayment;
	}

	public LocalDate getTradeDate() { return tradeDate; }

	public ValuationModel getValuationModel() { return valuationModel; }

	public DirtyCleanPrice getDirtyCleanPrice() { return dirtyCleanPrice; }

	public boolean isUseFinerDiscretization() { return useFinerDiscretization; }


	@Override
	public String toString() {
		return "CouponBond [ScheduleFromPeriods=" + schedule
				+ ", discountCurveName=" + discountCurveName
				+ ", forwardCurveName=" + forwardCurveName
				+ ", survivalProbabilityCurveName=" + survivalProbabilityCurveName
				+ ", recoveryRateCurve=" + recoveryRateCurveName
				+ ", fixedFee=" + fixedFee
				+ ", floatingFeeSpread=" + floatingFeeSpread
				+ ", upfrontPayment=" + upfrontPayment
				+ ", tradeDate=" + tradeDate
				+ ", pricingModel=" + valuationModel
				+ ", dirtyCleanPrice=" + dirtyCleanPrice
				+ ", useFinerDiscretization=" + useFinerDiscretization + "]";
	}


	public enum ValuationModel{
		DISCRETE,
		JPM,
		JPM_NOACCFEE
	}

	public enum DirtyCleanPrice {
		CLEAN,
		DIRTY,
	}



}
