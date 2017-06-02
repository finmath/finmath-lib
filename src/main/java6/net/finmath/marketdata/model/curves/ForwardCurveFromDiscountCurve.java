/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import org.threeten.bp.LocalDate;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;

/**
 * A forward curve derived from a given discount curve.
 * 
 * The forward with fixing in \( t \) is calculated as
 * \(
 * 		(\frac{1}{dcf(t,t+d)} ( \frac{df(t)}{df(t+d)}-1 )
 * \)
 * where \( dcf(t,t+d) \) is the daycount-fraction between t and t+d and \( t \mapsto df(t) \) is the given referenceDiscountCurveNameForForwardCurve.
 * The payment offset \( t+d \) is either generated from the paymentOffsetCode or directly specified if paymentOffsetCode=NaN.
 * 
 * The referenceDiscountCurveNameForForwardCurve is referenced by name and evaluated late (which allows use of
 * this construct in a calibration process referencing changing discount curves.
 * 
 * @author Christian Fries
 */
public class ForwardCurveFromDiscountCurve extends AbstractForwardCurve implements Serializable {

	private static final long serialVersionUID = -4126228588123963885L;
	private final String referenceDiscountCurveForForwardsName; // The (pseudo-)discount curve that the forwards are calculated from. Note that this is in general different from the discount curve associated with the forwards
	
	private final double daycountScaling;
	private final double periodOffset;

	/**
	 * Create a forward curve using a given referenceDiscountCurveForForwards.
	 * 
	 * @param name The name under which the forward curve can be referenced.
	 * @param referenceDiscountCurveName The (pseudo-)discount curve that the forwards are calculated from.
	 * @param discountCurveName The name of the discount curve associated with this forward curve (usually OIS).
	 * @param referenceDate The reference date used in the interpretation of times (i.e., the referenceDate where t=0).
	 * @param paymentOffsetCode The payment offset. If null, the parameter p has to be provided to the getForward method.
	 * @param paymentOffsetBusinessdayCalendar The calendar used to generate the payment date from the paymentOffetCode.
	 * @param paymentOffsetDateRollConvention The date roll convention used to generate the payment date from the paymentOffsetCode.
	 * @param daycountScaling The scaling factor applied to the paymentOffset measured in ACT/365.
	 * @param periodOffset An offset in ACT/365 applied to the fixing to construct the period start (the negative of the fixingOffset of the period).
	 */
	public ForwardCurveFromDiscountCurve(String name, String referenceDiscountCurveName, String discountCurveName, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendarInterface paymentOffsetBusinessdayCalendar, BusinessdayCalendarInterface.DateRollConvention paymentOffsetDateRollConvention, double daycountScaling, double periodOffset) {
		super(name, referenceDate, paymentOffsetCode, paymentOffsetBusinessdayCalendar, paymentOffsetDateRollConvention, discountCurveName);

		this.referenceDiscountCurveForForwardsName = referenceDiscountCurveName;
		this.daycountScaling = daycountScaling;
		this.periodOffset = periodOffset;
	}
	
	/**
	 * Create a forward curve using a given referenceDiscountCurveForForwards. 
	 * Note that the referenceDiscountCurveForForwards is also used as the discount curve associated with the forwards (i.e. single curve).
	 * 
	 * @param name The name under which the forward curve can be referenced.
	 * @param referenceDiscountCurveName The (pseudo-)discount curve that the forwards are calculated from.
	 * @param referenceDate The reference date used in the interpretation of times (i.e., the referenceDate where t=0).
	 * @param paymentOffsetCode The payment offset. If null, the parameter p has to be provided to the getForward method.
	 * @param paymentOffsetBusinessdayCalendar The calendar used to generate the payment date from the paymentOffetCode.
	 * @param paymentOffsetDateRollConvention The date roll convention used to generate the payment date from the paymentOffsetCode.
	 * @param daycountScaling The scaling factor applied to the paymentOffset measured in ACT/365.
	 * @param periodOffset An offset in ACT/365 applied to the fixing to construct the period start (the negative of the fixingOffset of the period).
	 */
	public ForwardCurveFromDiscountCurve(String name, String referenceDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendarInterface paymentOffsetBusinessdayCalendar, BusinessdayCalendarInterface.DateRollConvention paymentOffsetDateRollConvention, double daycountScaling, double periodOffset) {
		this(name, referenceDiscountCurveName, referenceDiscountCurveName, referenceDate, paymentOffsetCode, paymentOffsetBusinessdayCalendar, paymentOffsetDateRollConvention, daycountScaling, periodOffset);
	}

	/**
	 * Create a forward curve using a given referenceDiscountCurveForForwards. 
	 * Note that the referenceDiscountCurveForForwards is also used as the discount curve associated with the forwards (i.e. single curve).
	 * 
	 * @param name The name under which the forward curve can be referenced.
	 * @param referenceDiscountCurveName The (pseudo-)discount curve that the forwards are calculated from.
	 * @param referenceDate The reference date used in the interpretation of times (i.e., the referenceDate where t=0).
	 * @param paymentOffsetCode The payment offset. If null, the parameter p has to be provided to the getForward method.
	 * @param paymentOffsetBusinessdayCalendar The calendar used to generate the payment date from the paymentOffetCode.
	 * @param paymentOffsetDateRollConvention The date roll convention used to generate the payment date from the paymentOffsetCode.
	 */
	public ForwardCurveFromDiscountCurve(String name, String referenceDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendarInterface paymentOffsetBusinessdayCalendar, BusinessdayCalendarInterface.DateRollConvention paymentOffsetDateRollConvention) {
		this(name, referenceDiscountCurveName, referenceDate, paymentOffsetCode, paymentOffsetBusinessdayCalendar, paymentOffsetDateRollConvention, 1.0, 0.0);
	}

	/**
	 * Create a forward curve using a given referenceDiscountCurveForForwards. 
	 * Note that the referenceDiscountCurveForForwards is also used as the discount curve associated with the forwards (i.e. single curve).
	 * 
	 * @param name The name under which the forward curve can be referenced.
	 * @param referenceDiscountCurveName The (pseudo-)discount curve that the forwards are calculated from.
	 * @param referenceDate The reference date used in the interpretation of times (i.e., the referenceDate where t=0).
	 * @param paymentOffsetCode The payment offset. If null, the parameter p has to be provided to the getForward method.
	 */
	public ForwardCurveFromDiscountCurve(String name, String referenceDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode) {
		this(name, referenceDiscountCurveName, referenceDate, paymentOffsetCode, new BusinessdayCalendarExcludingWeekends(), BusinessdayCalendarInterface.DateRollConvention.FOLLOWING);
	}

	/**
	 * Create a forward curve using a given referenceDiscountCurveForForwards. 
	 * Note that the referenceDiscountCurveForForwards is also used as the discount curve associated with the forwards (i.e. single curve).
	 * 
	 * The name of the this forward curve will be
	 * <code>
	 * 	"ForwardCurveFromDiscountCurve(" +  referenceDiscountCurveName + "," + paymentOffsetCode + ")",
	 * </code>
	 * but code should not reply on this. Instead you should use getName() to get the name of the curve.
	 * 
	 * @param referenceDiscountCurveName The (pseudo-)discount curve that the forwards are calculated from.
	 * @param referenceDate The reference date used in the interpretation of times (i.e., the referenceDate where t=0).
	 * @param paymentOffsetCode The payment offset. If null, the parameter p has to be provided to the getForward method.
	 */
	public ForwardCurveFromDiscountCurve(String referenceDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode) {
		this("ForwardCurveFromDiscountCurve(" +  referenceDiscountCurveName + "," + paymentOffsetCode + ")", referenceDiscountCurveName, referenceDate, paymentOffsetCode);
	}

	@Override
	public double getForward(AnalyticModelInterface model, double fixingTime)
	{
		double paymentOffset = getPaymentOffset(fixingTime+periodOffset);
		return getForward(model, fixingTime, paymentOffset);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.ForwardCurveInterface#getForward(double)
	 */
	@Override
	public double getForward(AnalyticModelInterface model, double fixingTime, double paymentOffset)
	{
		if(model==null)
			throw new IllegalArgumentException(this.getName() + ": model==null");
		DiscountCurveInterface referenceDiscountCurveForForwards = model.getDiscountCurve(referenceDiscountCurveForForwardsName); // do not use discountCurveName here (usually this is an OIS curve)
		if(referenceDiscountCurveForForwards==null)
			throw new IllegalArgumentException(this.getName() + ": referenceDiscountCurveForForwards " + referenceDiscountCurveForForwardsName + " not found in the model:\n" + model.toString());
		if(Double.isNaN(paymentOffset) || paymentOffset<=0.0)
			throw new IllegalArgumentException(this.getName() + ": Requesting forward with paymentOffset " + paymentOffset + " not allowed.");

		double daycount = paymentOffset * daycountScaling;
		return (referenceDiscountCurveForForwards.getDiscountFactor(model, fixingTime+periodOffset) / referenceDiscountCurveForForwards.getDiscountFactor(model, fixingTime+paymentOffset+periodOffset) - 1.0) / daycount;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
	public double getValue(double time) {
		return getValue(null, time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		return getForward(model, time);
	}

	@Override
	public double[] getParameter() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String toString() {
		return "ForwardCurveFromDiscountCurve [" + super.toString() + ", referenceDiscountCurveForForwardsName=" + referenceDiscountCurveForForwardsName + ", daycountScaling=" + daycountScaling + ", periodOffset=" + periodOffset + "]";
	}
}
