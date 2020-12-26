/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.marketdata.model.bond;

import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.products.AbstractAnalyticProduct;
import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Implements the valuation of a bond (zero-coupon, fixed coupon or floating coupon)
 * with unit notional of 1 using curves:
 * <ul>
 * 	<li>a forward curve, if the bond has floating rate coupons</li>
 * 	<li>a discount curve as a base curve for discounting</li>
 * 	<li>a survival probability curve for additional credit risk related discount factor</li>
 * 	<li>a basis factor curve for additional bond related discount factor</li>
 * </ul>
 *
 * Support for day counting is provided via the class implementing
 * <code>Schedule</code>.
 *
 * The effective bond curve is a combination of the discount curve (risk free curve), the basis factor curve
 * (which could be considers as an additional industry specific factor) and the issuer specific
 * survival probalbilty. The effective discount factor is the product of the three:
 * discountFactor * survivalProbabilityFactor * basisFactorFactor
 *
 * You may set the arguments for the survival probability curve and the basis factor curve to null.
 *
 * @author Moritz Scherrmann
 * @author Chrisitan Fries
 * @version 1.1
 */
public class Bond extends AbstractAnalyticProduct implements AnalyticProduct {

	private final Schedule	schedule;
	private final String discountCurveName;
	private final String forwardCurveName; //Set null if fixed- or zero-coupon bond
	private final String survivalProbabilityCurveName;
	private final String basisFactorCurveName;
	private final double fixedCoupon; //Set equal zero if floating rate note
	private final double floatingSpread;
	private final double recoveryRate;


	/**
	 * Creates a bond.
	 *
	 * @param schedule ScheduleFromPeriods of the bond.
	 * @param discountCurveName Name of the discount curve.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix coupon bond or a zero-coupon bond.
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param basisFactorCurveName Name of the basis factor curve.
	 * @param fixedCoupon The fixed coupon of the bond expressed as absolute value.
	 * @param floatingSpread The floating spread of the bond expressed as absolute value.
	 * @param recoveryRate The recovery rate of the bond.
	 */
	public Bond(final Schedule                    schedule,
			final String             discountCurveName,
			final String              forwardCurveName,
			final String  survivalProbabilityCurveName,
			final String          basisFactorCurveName,
			final double                   fixedCoupon,
			final double                floatingSpread,
			final double                  recoveryRate) {
		super();
		this.schedule = schedule;
		this.discountCurveName = discountCurveName;
		this.forwardCurveName = forwardCurveName;
		this.survivalProbabilityCurveName = survivalProbabilityCurveName;
		this.basisFactorCurveName=basisFactorCurveName;
		this.fixedCoupon=fixedCoupon;
		this.floatingSpread=floatingSpread;
		this.recoveryRate=recoveryRate;
	}

	/**
	 * Creates a fixed coupon bond with recovery rate.
	 *
	 * @param schedule ScheduleFromPeriods of the bond.
	 * @param discountCurveName Name of the discount curve.
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param basisFactorCurveName Name of the basis factor curve.
	 * @param fixedCoupon The fixed coupon of the bond expressed as absolute value.
	 * @param recoveryRate The recovery rate of the bond.
	 */
	public Bond(final Schedule schedule, final String discountCurveName, final String survivalProbabilityCurveName ,final String basisFactorCurveName, final double fixedCoupon, final double recoveryRate) {
		this(schedule, discountCurveName, null,survivalProbabilityCurveName, basisFactorCurveName, fixedCoupon,0, recoveryRate);
	}

	/**
	 * Creates a fixed or floating bond without recovery rate.
	 *
	 * @param schedule ScheduleFromPeriods of the bond.
	 * @param discountCurveName Name of the discount curve.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix coupon bond or a zero-coupon bond.
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param basisFactorCurveName Name of the basis factor curve.
	 * @param fixedCoupon The fixed coupon of the bond expressed as absolute value.
	 * @param floatingSpread The floating spread of the bond expressed as absolute value.
	 */
	public Bond(final Schedule schedule, final String discountCurveName,final String forwardCurveName, final String survivalProbabilityCurveName ,final String basisFactorCurveName, final double fixedCoupon, final double floatingSpread) {
		this(schedule, discountCurveName, forwardCurveName,survivalProbabilityCurveName, basisFactorCurveName, fixedCoupon,floatingSpread, 0);
	}

	/**
	 * Creates a fixed coupon bond without recovery rate.
	 *
	 * @param schedule ScheduleFromPeriods of the bond.
	 * @param discountCurveName Name of the discount curve.
	 * @param survivalProbabilityCurveName Name of the survival probability curve.
	 * @param basisFactorCurveName Name of the basis factor curve.
	 * @param fixedCoupon The fixed coupon of the bond expressed as absolute value.
	 */
	public Bond(final Schedule schedule, final String discountCurveName, final String survivalProbabilityCurveName ,final String basisFactorCurveName, final double fixedCoupon) {
		this(schedule, discountCurveName, null,survivalProbabilityCurveName, basisFactorCurveName, fixedCoupon,0, 0);
	}

	/**
	 * Creates a fixed coupon bond using a single discount curve only.
	 *
	 * @param schedule ScheduleFromPeriods of the bond.
	 * @param discountCurveName Name of the discount curve.
	 * @param fixedCoupon The fixed coupon of the bond expressed as absolute value.
	 */
	public Bond(final Schedule schedule, final String discountCurveName, final double fixedCoupon) {
		this(schedule, discountCurveName, null,null, null, fixedCoupon,0, 0);
	}

	@Override
	public double getValue(final double evaluationTime, final AnalyticModel model) {

		final boolean positiveRecoveryRate = recoveryRate>0;

		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final ForwardCurve forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		final DiscountCurve discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		final Curve survivalProbabilityCurve = model.getCurve(survivalProbabilityCurveName);
		final Curve basisFactorCurve = model.getCurve(basisFactorCurveName);

		double value = 0.0;
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods(); periodIndex++) {
			final double paymentDate	= schedule.getPayment(periodIndex);
			final double periodLength	= schedule.getPeriodLength(periodIndex);

			final double discountFactor	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;

			final double survivalProbabilityFactor	= survivalProbabilityCurve != null ? (paymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, paymentDate) : 0.0) : 1.0;

			final double basisFactorFactor	= basisFactorCurve != null ? (paymentDate > evaluationTime ? basisFactorCurve.getValue(model, paymentDate) : 0.0) : 1.0;

			double couponPayment=fixedCoupon ;
			if(forwardCurve != null ) {
				couponPayment = floatingSpread+forwardCurve.getForward(model, schedule.getFixing(periodIndex));
			}

			value += couponPayment * periodLength * discountFactor * survivalProbabilityFactor * basisFactorFactor;

			// Consider notional payments if required
			if(positiveRecoveryRate) {
				double previousPaymentDate = 0	;
				if(periodIndex>0) {
					previousPaymentDate	= schedule.getPayment(periodIndex-1);
				}

				final double previousSurvivalProbabilityFactor	= previousPaymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, previousPaymentDate) : 1.0;

				value += recoveryRate * discountFactor * (previousSurvivalProbabilityFactor- survivalProbabilityFactor) *  basisFactorFactor;
			}
		}

		final double paymentDate	= schedule.getPayment(schedule.getNumberOfPeriods()-1);

		final double discountFactor	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;

		final double survivalProbabilityFactor	= survivalProbabilityCurve != null ? (paymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, paymentDate) : 0.0) : 1.0;

		final double basisFactorFactor	= basisFactorCurve != null ? (paymentDate > evaluationTime ? basisFactorCurve.getValue(model, paymentDate) : 0.0) : 1.0;

		value +=  discountFactor * survivalProbabilityFactor * basisFactorFactor;

		// @TODO The forward value should use division of all curves
		return value / discountCurve.getDiscountFactor(model, evaluationTime);
	}

	/**
	 * Returns the coupon payment of the period with the given index. The analytic model is needed in case of floating bonds.
	 *
	 * @param periodIndex The index of the period of interest.
	 * @param model The model under which the product is valued.
	 * @return The value of the coupon payment in the given period.
	 */
	public double getCouponPayment(final int periodIndex, final AnalyticModel model) {

		final ForwardCurve forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		final double periodLength	= schedule.getPeriodLength(periodIndex);
		double couponPayment=fixedCoupon ;
		if(forwardCurve != null ) {
			couponPayment = floatingSpread+forwardCurve.getForward(model, schedule.getFixing(periodIndex));
		}
		return couponPayment*periodLength;
	}

	/**
	 * Returns the value of the sum of discounted cash flows of the bond where
	 * the discounting is done with the given reference curve and an additional spread.
	 * This method can be used for optimizer.
	 *
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param referenceCurve The reference curve used for discounting the coupon payments.
	 * @param spread The spread which should be added to the discount curve.
	 * @param model The model under which the product is valued.
	 * @return The value of the bond for the given curve and spread.
	 */
	public double getValueWithGivenSpreadOverCurve(final double evaluationTime,final Curve referenceCurve, final double spread, final AnalyticModel model) {
		double value=0;
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods();periodIndex++) {
			final double paymentDate	= schedule.getPayment(periodIndex);
			value+= paymentDate>evaluationTime ? getCouponPayment(periodIndex,model)*Math.exp(-spread*paymentDate)*referenceCurve.getValue(paymentDate): 0.0;
		}

		final double paymentDate	= schedule.getPayment(schedule.getNumberOfPeriods()-1);
		return paymentDate>evaluationTime ? value+Math.exp(-spread*paymentDate)*referenceCurve.getValue(paymentDate):0.0;
	}

	/**
	 * Returns the value of the sum of discounted cash flows of the bond where
	 * the discounting is done with the given yield curve.
	 * This method can be used for optimizer.
	 *
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param rate The yield which is used for discounted the coupon payments.
	 * @param model The model under which the product is valued.
	 * @return The value of the bond for the given yield.
	 */
	public double getValueWithGivenYield(final double evaluationTime, final double rate, final AnalyticModel model) {
		final DiscountCurve referenceCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors("referenceCurve", new double[] {0.0, 1.0}, new double[]  {1.0, 1.0});
		return getValueWithGivenSpreadOverCurve(evaluationTime, referenceCurve, rate, model);
	}

	/**
	 * Returns the spread value such that the sum of cash flows of the bond discounted with a given reference curve
	 * with the additional spread coincides with a given price.
	 *
	 * @param bondPrice The target price as double.
	 * @param referenceCurve The reference curve used for discounting the coupon payments.
	 * @param model The model under which the product is valued.
	 * @return The optimal spread value.
	 */
	public double getSpread(final double bondPrice, final Curve referenceCurve, final AnalyticModel model) {
		final GoldenSectionSearch search = new GoldenSectionSearch(-2.0, 2.0);
		while(search.getAccuracy() > 1E-11 && !search.isDone()) {
			final double x = search.getNextPoint();
			final double fx=getValueWithGivenSpreadOverCurve(0.0,referenceCurve,x,model);
			final double y = (bondPrice-fx)*(bondPrice-fx);

			search.setValue(y);
		}
		return search.getBestPoint();
	}

	/**
	 * Returns the yield value such that the sum of cash flows of the bond discounted with the yield curve
	 * coincides with a given price.
	 *
	 * @param bondPrice The target price as double.
	 * @param model The model under which the product is valued.
	 * @return The optimal yield value.
	 */
	public double getYield(final double bondPrice, final AnalyticModel model) {
		final GoldenSectionSearch search = new GoldenSectionSearch(-2.0, 2.0);
		while(search.getAccuracy() > 1E-11 && !search.isDone()) {
			final double x = search.getNextPoint();
			final double fx=getValueWithGivenYield(0.0,x,model);
			final double y = (bondPrice-fx)*(bondPrice-fx);

			search.setValue(y);
		}
		return search.getBestPoint();
	}

	/**
	 * Returns the accrued interest of the bond for a given date.
	 *
	 * @param date The date of interest.
	 * @param model The model under which the product is valued.
	 * @return The accrued interest.
	 */
	public double getAccruedInterest(final LocalDate date, final AnalyticModel model) {
		final int periodIndex=schedule.getPeriodIndex(date);
		final Period period=schedule.getPeriod(periodIndex);
		final DayCountConvention dcc= schedule.getDaycountconvention();
		final double accruedInterest=getCouponPayment(periodIndex,model)*(dcc.getDaycountFraction(period.getPeriodStart(), date))/schedule.getPeriodLength(periodIndex);
		return accruedInterest;
	}

	/**
	 * Returns the accrued interest of the bond for a given time.
	 *
	 * @param time The time of interest as double.
	 * @param model The model under which the product is valued.
	 * @return The accrued interest.
	 */
	public double getAccruedInterest(final double time, final AnalyticModel model) {
		final LocalDate date= FloatingpointDate.getDateFromFloatingPointDate(schedule.getReferenceDate(), time);
		return getAccruedInterest(date, model);
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

	public String getBasisFactorCurveName() {
		return basisFactorCurveName;
	}

	public double getFixedCoupon() {
		return fixedCoupon;
	}

	public double getFloatingSpread() {
		return floatingSpread;
	}

	public double getRecoveryRate() {
		return recoveryRate;
	}

	@Override
	public String toString() {
		return "CouponBond [ScheduleFromPeriods=" + schedule
				+ ", discountCurveName=" + discountCurveName
				+ ", forwardtCurveName=" + forwardCurveName
				+ ", survivalProbabilityCurveName=" + survivalProbabilityCurveName
				+ ", basisFactorCurveName=" + basisFactorCurveName
				+ ", fixedCoupon=" + fixedCoupon
				+ ", floatingSpread=" + floatingSpread
				+ ", recoveryRate=" + recoveryRate + "]";
	}


}
