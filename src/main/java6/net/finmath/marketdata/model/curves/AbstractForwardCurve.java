/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.10.2013
 */

package net.finmath.marketdata.model.curves;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.joda.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * Abstract base class for a forward curve, extending a curve object
 * 
 * It stores the maturity of the underlying index (paymentOffset) and the associated discount curve.
 * 
 * @author Christian Fries
 */
public abstract class AbstractForwardCurve extends Curve implements ForwardCurveInterface {

	private static final long serialVersionUID = 3735595267579329042L;

	protected final String discountCurveName;
	private final Map<Double, Double> paymentOffsets = new ConcurrentHashMap<Double, Double>();

	protected final String paymentOffsetCode;
	protected final BusinessdayCalendarInterface paymentBusinessdayCalendar;
	protected final BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention;

	private final double paymentOffset;

	/**
	 * Construct a base forward curve with a reference date and a payment offset.
	 * 
	 * @param name The name of this curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param paymentOffsetCode The maturity of the index modeled by this curve.
	 * @param paymentBusinessdayCalendar The business day calendar used for adjusting the payment date.
	 * @param paymentDateRollConvention The date roll convention used for adjusting the payment date.
	 * @param interpolationMethod The interpolation method used for the curve.
	 * @param extrapolationMethod The extrapolation mehtod used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @param discountCurveName The name of a discount curve associated with this index (associated with it's funding or collateralization), if any.
	 */
	public AbstractForwardCurve(String name, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendarInterface paymentBusinessdayCalendar, 
			BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention, InterpolationMethod interpolationMethod, 
			ExtrapolationMethod extrapolationMethod, InterpolationEntity interpolationEntity, String discountCurveName) {
		super(name, referenceDate, interpolationMethod, extrapolationMethod, interpolationEntity);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;

		this.paymentOffset = Double.NaN;

		this.discountCurveName = discountCurveName;
	}

	/**
	 * Construct a base forward curve with a reference date and a payment offset.
	 * 
	 * @param name The name of this curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param paymentOffsetCode The maturity of the index modeled by this curve.
	 * @param paymentBusinessdayCalendar The business day calendar used for adjusting the payment date.
	 * @param paymentDateRollConvention The date roll convention used for adjusting the payment date.
	 * @param discountCurveName The name of a discount curve associated with this index (associated with it's funding or collateralization), if any.
	 */
	public AbstractForwardCurve(String name, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendarInterface paymentBusinessdayCalendar, BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention, String discountCurveName) {
		super(name, referenceDate, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;

		this.paymentOffset = Double.NaN;

		this.discountCurveName = discountCurveName;
	}

	/**
	/**
	 * Construct a base forward curve with a reference date and a payment offset.
	 * 
	 * @param name The name of this curve.
	 * @param referenceDate The reference date for this code, i.e., the date which defined t=0.
	 * @param paymentOffset The maturity of the index modeled by this curve.
	 * @param discountCurveName The name of a discount curve associated with this index (associated with it's funding or collateralization), if any.
	 */
	public AbstractForwardCurve(String name, LocalDate referenceDate, double paymentOffset, String discountCurveName) {
		super(name, referenceDate, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE);
		this.paymentOffset = paymentOffset;
		this.discountCurveName = discountCurveName;

		this.paymentOffsetCode = null;
		this.paymentBusinessdayCalendar = null;
		this.paymentDateRollConvention = null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface#getDiscountCurveName()
	 */
	@Override
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface#getPaymentOffset(double)
	 */
	@Override
	public double getPaymentOffset(double fixingTime) {
		if(paymentOffsetCode == null) return paymentOffset;

		if(paymentOffsets.containsKey(fixingTime)) {
			return paymentOffsets.get(fixingTime);
		}
		else {
			LocalDate paymentDate = paymentBusinessdayCalendar.getAdjustedDate(
					getReferenceDate().plusDays((int)Math.round(fixingTime*365)), 
					paymentOffsetCode, 
					paymentDateRollConvention);
			double paymentTime = (new DayCountConvention_ACT_365()).getDaycountFraction(getReferenceDate(), paymentDate);
			paymentOffsets.put(fixingTime, paymentTime-fixingTime);
			return paymentTime-fixingTime;
		}
	}

	/**
	 * Returns the forwards for a given vector fixing times.
	 * 
	 * @param model An analytic model providing a context. The discount curve (if needed) is obtained from this model.
	 * @param fixingTimes The given fixing times.
	 * @return The forward rates.
	 */
	public double[] getForwards(AnalyticModelInterface model, double[] fixingTimes)
	{
		double[] values = new double[fixingTimes.length];

		for(int i=0; i<fixingTimes.length; i++) values[i] = getForward(model, fixingTimes[i]);

		return values;
	}
}