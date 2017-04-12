/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;

import org.joda.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingWeekends;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;

/**
 * A forward curve derived from a given discount curve.
 * 
 * The forward with fixing in \( t \) is calculated as
 * \(
 * 		(\frac{1}{d} ( \frac{df(t)}{df(t+d)}-1 )
 * \)
 * where \( d \) is a given the payment offset and \( t \mapsto df(t) \)
 * is the given discount curve.
 * 
 * The discount curve is reference by names and evaluated late (which allows use of
 * this construct in a calibration process referencing changing discount curves.
 * 
 * @author Christian Fries
 */
public class ForwardCurveFromDiscountCurve extends AbstractForwardCurve implements Serializable {

	private static final long serialVersionUID = -4126228588123963885L;
	public static final String nameSuffix = "_asForwardCurve";
	private final String baseDiscountCurveName;	// The discount curve used for calculation of the forward.
	private final double daycountScaling;
	private final double periodOffset;

	/**
	 * Create a forward curve using a given discount curve.
	 * 
	 * The forward with fixing in t is calculated as ( df(t)/df(t+p)-1 ) / p
	 * where df denotes the discount factor as a function of maturity and
	 * p is a given the payment offset.
	 * 
	 * The date t + p is generated from the paymentOffsetCode using the provided paymentOffsetBusinessdayCalendar and paymentOffsetDateRollConvention.
	 * 
	 * @param name The name under which the forward curve can be referenced.
	 * @param baseDiscountCurveName The discount curve used for calculation of the forward.
	 * @param referenceDate The reference date used in the interpretation of times (i.e., the referenceDate where t=0).
	 * @param paymentOffsetCode The payment offset. If null, the parameter p has to be provided to the getForward method.
	 * @param paymentOffsetBusinessdayCalendar The calendar used to generate the payment date from the paymentOffetCode.
	 * @param paymentOffsetDateRollConvention The date roll convention used to generate the payment date from the paymentOffsetCode.
	 * @param daycountScaling The scaling factor applied to the paymentOffset measured in ACT/365.
	 * @param periodOffset An offset in ACT/365 applied to the fixing to construct the period start (the negative of the fixingOffset of the period).
	 */
	public ForwardCurveFromDiscountCurve(String name, String baseDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendarInterface paymentOffsetBusinessdayCalendar, BusinessdayCalendarInterface.DateRollConvention paymentOffsetDateRollConvention, double daycountScaling, double periodOffset) {
		super(name+nameSuffix, referenceDate, paymentOffsetCode, paymentOffsetBusinessdayCalendar, paymentOffsetDateRollConvention);

		this.baseDiscountCurveName = baseDiscountCurveName;
		this.daycountScaling = daycountScaling;
		this.periodOffset = periodOffset;
	}

	/**
	 * See main constructor
	 */
	public ForwardCurveFromDiscountCurve(String name, String baseDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode, 	BusinessdayCalendarInterface paymentOffsetBusinessdayCalendar, BusinessdayCalendarInterface.DateRollConvention paymentOffsetDateRollConvention) {
		this(name, baseDiscountCurveName, referenceDate, paymentOffsetCode, paymentOffsetBusinessdayCalendar, paymentOffsetDateRollConvention, 1.0, 0.0);
	}

	/**
	 * See main constructor
	 */
	public ForwardCurveFromDiscountCurve(String name, String baseDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode) {
		this(name, baseDiscountCurveName, referenceDate, paymentOffsetCode, new BusinessdayCalendarExcludingWeekends(), BusinessdayCalendarInterface.DateRollConvention.FOLLOWING);
	}

	/**
	 * See main constructor
	 */
	public ForwardCurveFromDiscountCurve(String baseDiscountCurveName, LocalDate referenceDate, String paymentOffsetCode) {
		this(baseDiscountCurveName + "_" + paymentOffsetCode, baseDiscountCurveName, referenceDate, paymentOffsetCode);
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
		DiscountCurveInterface baseDiscountCurve = model.getDiscountCurve(baseDiscountCurveName); // do not use discountCurveName here (usually this is an OIS curve)
		if(baseDiscountCurve==null)
			throw new IllegalArgumentException(this.getName() + ": baseDiscountCurve " + baseDiscountCurveName + " not found in the model");
		if(Double.isNaN(paymentOffset) || paymentOffset<=0.0)
			throw new IllegalArgumentException(this.getName() + ": Requesting forward with paymentOffset " + paymentOffset + " not allowed.");
		
		double daycount = paymentOffset * daycountScaling;
		return (baseDiscountCurve.getDiscountFactor(model, fixingTime+periodOffset) / baseDiscountCurve.getDiscountFactor(model, fixingTime+paymentOffset+periodOffset) - 1.0) / daycount;
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
	public String getBaseDiscountCurveName() {
		return baseDiscountCurveName;
	}
	
	@Override
	public String toString() {
		return "ForwardCurveFromDiscountCurve [" + super.toString() + ", baseDiscountCurveName=" + baseDiscountCurveName + "]";
	}
}
