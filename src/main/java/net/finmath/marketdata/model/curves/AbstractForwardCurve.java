/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 04.10.2013
 */

package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;

/**
 * Abstract base class for a forward curve, extending a curve object
 *
 * It stores the maturity of the underlying index (paymentOffset) and the associated discount curve.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractForwardCurve extends CurveInterpolation implements ForwardCurve {

	private static final long serialVersionUID = 3735595267579329042L;

	private final String discountCurveName; // The name of the discount curve associated with this forward curve (e.g. OIS for collateralized forwards)
	private final Map<Double, Double> paymentOffsets = new ConcurrentHashMap<>();

	private final String paymentOffsetCode;
	private final BusinessdayCalendar paymentBusinessdayCalendar;
	private final BusinessdayCalendar.DateRollConvention paymentDateRollConvention;

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
	 * @param extrapolationMethod The extrapolation method used for the curve.
	 * @param interpolationEntity The entity interpolated/extrapolated.
	 * @param discountCurveName The name of the discount curve associated with this forward curve (e.g. OIS for collateralized forwards).
	 */
	public AbstractForwardCurve(final String name, final LocalDate referenceDate, final String paymentOffsetCode,
			final BusinessdayCalendar paymentBusinessdayCalendar,
			final BusinessdayCalendar.DateRollConvention paymentDateRollConvention,
			final InterpolationMethod interpolationMethod, final ExtrapolationMethod extrapolationMethod,
			final InterpolationEntity interpolationEntity, final String discountCurveName) {
		super(name, referenceDate, interpolationMethod, extrapolationMethod, interpolationEntity);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;

		paymentOffset = Double.NaN;

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
	 * @param discountCurveName The name of the discount curve associated with this forward curve (e.g. OIS for collateralized forwards).
	 */
	public AbstractForwardCurve(final String name, final LocalDate referenceDate, final String paymentOffsetCode, final BusinessdayCalendar paymentBusinessdayCalendar, final BusinessdayCalendar.DateRollConvention paymentDateRollConvention, final String discountCurveName) {
		this(name, referenceDate, paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE, discountCurveName);
	}

	/**
	 * Construct a base forward curve with a reference date and a payment offset.
	 *
	 * @param name The name of this curve.
	 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
	 * @param paymentOffset The maturity of the index modeled by this curve.
	 * @param discountCurveName The name of the discount curve associated with this forward curve (e.g. OIS for collateralized forwards).
	 */
	public AbstractForwardCurve(final String name, final LocalDate referenceDate, final double paymentOffset, final String discountCurveName) {
		super(name, referenceDate, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE);
		this.paymentOffset = paymentOffset;
		this.discountCurveName = discountCurveName;

		paymentOffsetCode = null;
		paymentBusinessdayCalendar = null;
		paymentDateRollConvention = null;
	}

	@Override
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	@Override
	public double getPaymentOffset(final double fixingTime) {
		if(getPaymentOffsetCode() == null) {
			return paymentOffset;
		}

		if(paymentOffsets.containsKey(fixingTime)) {
			return paymentOffsets.get(fixingTime);
		}
		else {
			/*
			 * TODO In case paymentDate is relevant for the index modeling, it should be checked
			 * if the following derivation of paymentDate is accurate (e.g. wo we have a fixingOffset).
			 * In such a case, this method may be overridden.
			 */
			final LocalDate referenceDate = getReferenceDate();
			final LocalDate fixingDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, fixingTime);
			final LocalDate paymentDate = getPaymentBusinessdayCalendar().getAdjustedDate(fixingDate, getPaymentOffsetCode(), getPaymentDateRollConvention());
			final double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, paymentDate);
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
	public double[] getForwards(final AnalyticModel model, final double[] fixingTimes)
	{
		final double[] values = new double[fixingTimes.length];

		for(int i=0; i<fixingTimes.length; i++) {
			values[i] = getForward(model, fixingTimes[i]);
		}

		return values;
	}

	@Override
	public String toString() {
		return "AbstractForwardCurve [" + super.toString() + ", discountCurveName=" + getDiscountCurveName() + ", paymentOffsetCode=" + getPaymentOffsetCode() + ", paymentBusinessdayCalendar=" + getPaymentBusinessdayCalendar() + ", paymentDateRollConvention=" + getPaymentDateRollConvention() + "]";
	}

	public String getPaymentOffsetCode() {
		return paymentOffsetCode;
	}

	public BusinessdayCalendar getPaymentBusinessdayCalendar() {
		return paymentBusinessdayCalendar;
	}

	public BusinessdayCalendar.DateRollConvention getPaymentDateRollConvention() {
		return paymentDateRollConvention;
	}
}
