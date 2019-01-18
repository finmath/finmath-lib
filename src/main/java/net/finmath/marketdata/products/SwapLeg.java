/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata.products;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Optional;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.InterestRateSwapLegProductDescriptor;
import net.finmath.modelling.descriptor.ScheduleDescriptor;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Schedule;

/**
 * Implements the valuation of a swap leg with unit notional of 1 using curves (discount curve, forward curve).
 *
 * The swap leg valuation supports distinct discounting and forward curves.
 *
 * Support for day counting is provided via the class implementing
 * <code>Schedule</code>.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SwapLeg extends AbstractAnalyticProduct implements AnalyticProductInterface, DescribedProduct<InterestRateSwapLegProductDescriptor> {

	private final Optional<LocalDateTime>	cashFlowEffectiveDate;

	private final Schedule		legSchedule;
	private final String				forwardCurveName;
	private final String				discountCurveName;
	private final String				discountCurveForNotionalResetName;
	private boolean						isNotionalExchanged = false;

	private final double[] 				notionals;
	private final double[]				spreads;

	/**
	 * Creates a swap leg.
	 *
	 * @param cashFlowEffectiveDate If present, cash-flows are effective after this date, otherwise cash-flows are effective after the valuation date.
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param notionals Array of notionals for each period.
	 * @param spreads Array of fixed spreads on the forward or fix rate for each period.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(Optional<LocalDateTime> cashFlowEffectiveDate, Schedule legSchedule, String forwardCurveName, double[] notionals, double[] spreads, String discountCurveName, boolean isNotionalExchanged) {
		super();
		this.cashFlowEffectiveDate = cashFlowEffectiveDate;
		this.legSchedule = legSchedule;
		this.forwardCurveName = forwardCurveName;
		this.spreads = spreads;
		this.discountCurveName = discountCurveName;
		this.discountCurveForNotionalResetName = discountCurveName;
		this.isNotionalExchanged = isNotionalExchanged;
		this.notionals = notionals;
	}

	/**
	 * Creates a swap leg.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param notionals Array of notionals for each period.
	 * @param spreads Array of fixed spreads on the forward or fix rate for each period.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(Schedule legSchedule, String forwardCurveName, double[] notionals, double[] spreads, String discountCurveName, boolean isNotionalExchanged) {
		this(Optional.empty(), legSchedule, forwardCurveName, notionals, spreads, discountCurveName, isNotionalExchanged);
	}

	/**
	 * Creates a swap leg.
	 *
	 * @param cashFlowEffectiveDate If present, cash-flows are effective after this date, otherwise cash-flows are effective after the valuation date.
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param discountCurveForNotionalResetName Name of the discount curve used for notional reset. If it is left empty or equal to discountCurveName then there is no notional reset.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(Optional<LocalDateTime> cashFlowEffectiveDate, Schedule legSchedule, String forwardCurveName, double spread, String discountCurveName, String discountCurveForNotionalResetName, boolean isNotionalExchanged) {
		super();
		this.cashFlowEffectiveDate = cashFlowEffectiveDate;
		this.legSchedule = legSchedule;
		this.forwardCurveName = forwardCurveName;
		this.discountCurveName = discountCurveName;
		this.discountCurveForNotionalResetName = discountCurveForNotionalResetName=="" ? discountCurveName : discountCurveForNotionalResetName; // empty discountCurveForNotionalResetName is interpreted as no notional reset
		this.isNotionalExchanged = isNotionalExchanged;

		double[] notionals = new double[legSchedule.getNumberOfPeriods()];
		Arrays.fill(notionals, 1);
		this.notionals = notionals;
		double[] spreads = new double[legSchedule.getNumberOfPeriods()];
		Arrays.fill(spreads, spread);
		this.spreads = spreads;
	}

	/**
	 * Creates a swap leg.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param discountCurveForNotionalResetName Name of the discount curve used for notional reset. If it is left empty or equal to discountCurveName then there is no notional reset.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(Schedule legSchedule, String forwardCurveName, double spread, String discountCurveName, String discountCurveForNotionalResetName, boolean isNotionalExchanged) {
		this(Optional.empty(), legSchedule, forwardCurveName, spread, discountCurveName, discountCurveForNotionalResetName, isNotionalExchanged);
	}

	/**
	 * Creates a swap leg without notional reset.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 * @param isNotionalExchanged If true, the leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public SwapLeg(Schedule legSchedule, String forwardCurveName, double spread, String discountCurveName, boolean isNotionalExchanged) {
		this(legSchedule, forwardCurveName, spread, discountCurveName, discountCurveName, isNotionalExchanged);
	}

	/**
	 * Creates a swap leg without notional reset and without notional exchange.
	 *
	 * @param cashFlowEffectiveDate If present, cash-flows are effective after this date, otherwise cash-flows are effective after the valuation date.
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 */
	public SwapLeg(Optional<LocalDateTime> cashFlowEffectiveDate, Schedule legSchedule, String forwardCurveName, double spread, String discountCurveName) {
		this(cashFlowEffectiveDate, legSchedule, forwardCurveName, spread, discountCurveName, discountCurveName, false);
	}

	/**
	 * Creates a swap leg without notional reset and without notional exchange.
	 *
	 * @param legSchedule ScheduleFromPeriods of the leg.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spread Fixed spread on the forward or fix rate.
	 * @param discountCurveName Name of the discount curve for the leg.
	 */
	public SwapLeg(Schedule legSchedule, String forwardCurveName, double spread, String discountCurveName) {
		this(legSchedule, forwardCurveName, spread, discountCurveName, discountCurveName, false);
	}

	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		DiscountCurveInterface discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		double productToModelTimeOffset = 0;

		// TODO modelReferenceDate should move to LocalDateTime.
		LocalDate modelReferenceDate = discountCurve.getReferenceDate();
		LocalDate productRefereceDate = legSchedule.getReferenceDate();
		if(productRefereceDate != null && modelReferenceDate != null) {
			productToModelTimeOffset = FloatingpointDate.getFloatingPointDateFromDate(modelReferenceDate, productRefereceDate);
		}

		DiscountCurveInterface discountCurveForNotionalReset = model.getDiscountCurve(discountCurveForNotionalResetName);
		if(discountCurveForNotionalReset == null  && notionals == null) {
			throw new IllegalArgumentException("No discountCurveForNotionalReset with name '" + discountCurveForNotionalResetName + "' was found in the model:\n" + model.toString());
		}

		ForwardCurveInterface forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		double cashFlowEffectiveTime = cashFlowEffectiveDate.isPresent() ? FloatingpointDate.getFloatingPointDateFromDate(LocalDateTime.of(modelReferenceDate, LocalTime.of(0,0)), cashFlowEffectiveDate.get()) : evaluationTime;

		double value = 0.0;
		for(int periodIndex=0; periodIndex<legSchedule.getNumberOfPeriods(); periodIndex++) {
			double fixingDate	= productToModelTimeOffset + legSchedule.getFixing(periodIndex);
			double periodStart	= productToModelTimeOffset + legSchedule.getPeriodStart(periodIndex);
			double periodEnd	= productToModelTimeOffset + legSchedule.getPeriodEnd(periodIndex);
			double paymentDate	= productToModelTimeOffset + legSchedule.getPayment(periodIndex);
			double periodLength	= legSchedule.getPeriodLength(periodIndex);

			double forward = spreads[periodIndex];
			if(forwardCurve != null) {
				forward += forwardCurve.getForward(model, fixingDate, paymentDate-fixingDate);
			}

			// note that notional=1 if discountCurveForNotionalReset=discountCurve
			double notional;
			if(notionals != null) {
				notional = notionals[periodIndex];
			}
			else {
				notional = discountCurveForNotionalReset.getDiscountFactor(model,periodStart) / discountCurve.getDiscountFactor(model,periodStart);
			}

			double discountFactor = paymentDate > cashFlowEffectiveTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;
			value += notional * forward * periodLength * discountFactor;

			// Consider notional payments if required
			if(isNotionalExchanged) {
				value += periodEnd > cashFlowEffectiveTime ? notional * discountCurve.getDiscountFactor(model, periodEnd) : 0.0;
				value -= periodStart > cashFlowEffectiveTime ? notional * discountCurve.getDiscountFactor(model, periodStart) : 0.0;
			}
		}

		return value / discountCurve.getDiscountFactor(model, evaluationTime);
	}

	public Schedule getSchedule() {
		return legSchedule;
	}

	public String getForwardCurveName() {
		return forwardCurveName;
	}

	public double[] getSpreads() {
		return spreads.clone();
	}

	/**
	 * Returns the constant spread, , if the spread of this leg is constant. Otherwise an <code>UnsupportedOperationException</code> is thrown.
	 *
	 * @return The constant spread.
	 */
	public double getSpread() {
		// Checking spread array for constant spreads
		double spread = spreads[0];
		for(int i=1; i<spreads.length; i++) {
			if(spreads[i] != spread) {
				throw new UnsupportedOperationException("The method getSpread() is only supported for swap legs with constant spreads.");
			}
		}

		return spread;
	}

	public String getDiscountCurveName() {
		return discountCurveName;
	}

	public boolean isNotionalExchanged() {
		return isNotionalExchanged;
	}

	@Override
	public String toString() {
		return "SwapLeg [legSchedule=" + legSchedule
				+ ", forwardCurveName=" + forwardCurveName
				+ ", notionals=" + Arrays.toString(notionals)
				+ ", spreads=" + Arrays.toString(spreads)
				+ ", discountCurveName=" + discountCurveName
				+ ", discountCurveForNotionalResetName=" + discountCurveForNotionalResetName
				+ ", isNotionalExchanged=" + isNotionalExchanged + "]";
	}

	@Override
	public InterestRateSwapLegProductDescriptor getDescriptor() {
		return new InterestRateSwapLegProductDescriptor(forwardCurveName, discountCurveName, new ScheduleDescriptor(legSchedule), notionals, spreads, isNotionalExchanged);
	}
}
