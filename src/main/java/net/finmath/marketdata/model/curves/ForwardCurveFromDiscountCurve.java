/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import java.util.Calendar;

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

	/**
     * Create a forward curve using a given discount curve.
     * The forward with fixing in t is calculated as ( df(t)/df(t+p)-1 ) / p
     * where df denotes the discount factor as a function of maturity and
     * p is a given the payment offset.
     * 
     * @param discountCurveName The discount curve used for calculation of the forward.
     * @param referenceDate The reference date used in the interpretation of times (i.e., the referenceDate where t=0).
     * @param paymentOffsetCode The payment offset. If null, the parameter p has to be provided to the getForward method.
     */
    public ForwardCurveFromDiscountCurve(String discountCurveName, Calendar referenceDate, String paymentOffsetCode) {
    	super("ForwardCurveFromDiscountCurve(" +  discountCurveName + "," + paymentOffsetCode + ")", referenceDate,
    			paymentOffsetCode, new BusinessdayCalendarExcludingWeekends(), BusinessdayCalendarInterface.DateRollConvention.FOLLOWING, discountCurveName
    			);
    }

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.ForwardCurveInterface#getForward(double)
	 */
	@Override
    public double getForward(AnalyticModelInterface model, double fixingTime)
	{
		double paymentOffset = getPaymentOffset(fixingTime);
		return (model.getDiscountCurve(discountCurveName).getDiscountFactor(model, fixingTime) / model.getDiscountCurve(discountCurveName).getDiscountFactor(model, fixingTime+paymentOffset) - 1.0) / paymentOffset;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.ForwardCurveInterface#getForward(double)
	 */
	@Override
    public double getForward(AnalyticModelInterface model, double fixingTime, double paymentOffset)
	{
		double paymentOffsetOfCurve = getPaymentOffset(fixingTime);
		if(Double.isNaN(paymentOffsetOfCurve)) {
			if(paymentOffset <= 0) throw new IllegalArgumentException("Requesting forward for period of length " + paymentOffset + ".");
			paymentOffsetOfCurve = paymentOffset;
		}

		return (model.getDiscountCurve(discountCurveName).getDiscountFactor(model, fixingTime) / model.getDiscountCurve(discountCurveName).getDiscountFactor(model, fixingTime+paymentOffsetOfCurve) - 1.0) / paymentOffsetOfCurve;
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
}
