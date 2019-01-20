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
 * @author Moritz Scherrmann
 * @author Chrisitan Fries
 * @version 1.0
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
	public Bond(Schedule                    schedule,
			String             discountCurveName,
			String              forwardCurveName,
			String  survivalProbabilityCurveName,
			String          basisFactorCurveName,
			double                   fixedCoupon,
			double                floatingSpread,
			double                  recoveryRate) {
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
	public Bond(Schedule schedule, String discountCurveName, String survivalProbabilityCurveName ,String basisFactorCurveName, double fixedCoupon, double recoveryRate) {
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
	public Bond(Schedule schedule, String discountCurveName,String forwardCurveName, String survivalProbabilityCurveName ,String basisFactorCurveName, double fixedCoupon, double floatingSpread) {
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
	public Bond(Schedule schedule, String discountCurveName, String survivalProbabilityCurveName ,String basisFactorCurveName, double fixedCoupon) {
		this(schedule, discountCurveName, null,survivalProbabilityCurveName, basisFactorCurveName, fixedCoupon,0, 0);
	}




	@Override
	public double getValue(double evaluationTime, AnalyticModel model) {

		boolean positiveRecoveryRate = recoveryRate>0;

		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		ForwardCurve forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		DiscountCurve discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		Curve survivalProbabilityCurve = model.getCurve(survivalProbabilityCurveName);
		if(survivalProbabilityCurve == null) {
			throw new IllegalArgumentException("No survival probability curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		Curve basisFactorCurve = model.getCurve(basisFactorCurveName);
		if(basisFactorCurve == null) {
			throw new IllegalArgumentException("No basis factor curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		double value = 0.0;
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods(); periodIndex++) {
			double paymentDate	= schedule.getPayment(periodIndex);
			double periodLength	= schedule.getPeriodLength(periodIndex);

			double discountFactor	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;

			double survivalProbabilityFactor	= paymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, paymentDate) : 0.0;

			double basisFactorFactor	= paymentDate > evaluationTime ? basisFactorCurve.getValue(model, paymentDate) : 0.0;

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

				double previousSurvivalProbabilityFactor	= previousPaymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, previousPaymentDate) : 1.0;

				value += recoveryRate * discountFactor * (previousSurvivalProbabilityFactor- survivalProbabilityFactor) *  basisFactorFactor;
			}
		}

		double paymentDate	= schedule.getPayment(schedule.getNumberOfPeriods()-1);

		double discountFactor	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;

		double survivalProbabilityFactor	= paymentDate > evaluationTime ? survivalProbabilityCurve.getValue(model, paymentDate) : 0.0;

		double basisFactorFactor	= paymentDate > evaluationTime ? basisFactorCurve.getValue(model, paymentDate) : 0.0;

		value +=  discountFactor * survivalProbabilityFactor * basisFactorFactor;

		// @TODO: The forward value should use division of all curves
		return value / discountCurve.getDiscountFactor(model, evaluationTime);
	}

	/**
	 * Returns the coupon payment of the period with the given index. The analytic model is needed in case of floating bonds.
	 *
	 * @param periodIndex The index of the period of interest.
	 * @param model The model under which the product is valued.
	 * @return The value of the coupon payment in the given period.
	 */
	public double getCouponPayment(int periodIndex, AnalyticModel model) {

		ForwardCurve forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		double periodLength	= schedule.getPeriodLength(periodIndex);
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
	public double getValueWithGivenSpreadOverCurve(double evaluationTime,Curve referenceCurve, double spread, AnalyticModel model) {
		double value=0;
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods();periodIndex++) {
			double paymentDate	= schedule.getPayment(periodIndex);
			value+= paymentDate>evaluationTime ? getCouponPayment(periodIndex,model)*Math.exp(-spread*paymentDate)*referenceCurve.getValue(paymentDate): 0.0;
		}

		double paymentDate	= schedule.getPayment(schedule.getNumberOfPeriods()-1);
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
	public double getValueWithGivenYield(double evaluationTime, double rate, AnalyticModel model) {
		DiscountCurve referenceCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors("referenceCurve", new double[] {0.0, 1.0}, new double[]  {1.0, 1.0});
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
	public double getSpread(double bondPrice, Curve referenceCurve, AnalyticModel model) {
		GoldenSectionSearch search = new GoldenSectionSearch(-2.0, 2.0);
		while(search.getAccuracy() > 1E-11 && !search.isDone()) {
			double x = search.getNextPoint();
			double fx=getValueWithGivenSpreadOverCurve(0.0,referenceCurve,x,model);
			double y = (bondPrice-fx)*(bondPrice-fx);

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
	public double getYield(double bondPrice, AnalyticModel model) {
		GoldenSectionSearch search = new GoldenSectionSearch(-2.0, 2.0);
		while(search.getAccuracy() > 1E-11 && !search.isDone()) {
			double x = search.getNextPoint();
			double fx=getValueWithGivenYield(0.0,x,model);
			double y = (bondPrice-fx)*(bondPrice-fx);

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
	public double getAccruedInterest(LocalDate date, AnalyticModel model) {
		int periodIndex=schedule.getPeriodIndex(date);
		Period period=schedule.getPeriod(periodIndex);
		DayCountConvention dcc= schedule.getDaycountconvention();
		double accruedInterest=getCouponPayment(periodIndex,model)*(dcc.getDaycountFraction(period.getPeriodStart(), date))/schedule.getPeriodLength(periodIndex);
		return accruedInterest;
	}

	/**
	 * Returns the accrued interest of the bond for a given time.
	 *
	 * @param time The time of interest as double.
	 * @param model The model under which the product is valued.
	 * @return The accrued interest.
	 */
	public double getAccruedInterest(double time, AnalyticModel model) {
		LocalDate date= FloatingpointDate.getDateFromFloatingPointDate(schedule.getReferenceDate(), time);
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
