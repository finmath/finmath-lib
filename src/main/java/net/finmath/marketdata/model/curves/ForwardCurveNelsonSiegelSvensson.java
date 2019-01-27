/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Implementation of a forward given by a Nelson-Siegel-Svensson (NSS) parameterization.
 * In the NSS parameterization the zero rate \( r(T) \) is given by
 *
 * \[ r(T) = \beta_0 + \beta_1 \frac{1-x_0}{T/\tau_0} + \beta_2 ( \frac{1-x_0}{T/\tau_0} - x_0) + \beta_3 ( \frac{1-x_1}{T/\tau_1} - x_1) \]
 *
 * where \( x_0 = \exp(-T/\tau_0) \) and \( x_1 = \exp(-T/\tau_1) \).
 *
 * The sub-family of curves with \( \beta_3 = 0 \) is called Nelson-Siegel parameterization.
 *
 * @see net.finmath.marketdata.model.curves.DiscountCurveNelsonSiegelSvensson
 * @author Christian Fries
 * @version 1.0
 */
public class ForwardCurveNelsonSiegelSvensson extends AbstractCurve implements Serializable, ForwardCurve {

	private static final long serialVersionUID = 8024640795839972709L;

	private String paymentOffsetCode;
	private BusinessdayCalendar paymentBusinessdayCalendar;
	private BusinessdayCalendar.DateRollConvention paymentDateRollConvention;
	private DayCountConvention daycountConvention;
	private double periodOffset = 0.0;

	private DiscountCurveNelsonSiegelSvensson discountCurve;

	/**
	 * @param name The name of the curve. The curve can be fetched under this name when being part of an {@link net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols}.
	 * @param referenceDate The reference date to the curve, i.e., the date associated with t=0.
	 * @param paymentOffsetCode The payment offset code, like 3M, 6M, 12M, etc., used in calculating forwards from discount factors.
	 * @param paymentBusinessdayCalendar The payment businessday calendar.
	 * @param paymentDateRollConvention The payment date roll convention.
	 * @param daycountConvention The daycount convention.
	 * @param parameter The Nelson-Siegel-Svensson parameters in the order \( ( \beta_0, \beta_1, \beta_2, \beta_3, \tau_0, \tau_1 ) \).
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 * @param periodOffset An offset in ACT/365 applied to the fixing to construct the period start (the negative of the fixingOffset of the period).
	 */
	public ForwardCurveNelsonSiegelSvensson(String name, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendar paymentBusinessdayCalendar, BusinessdayCalendar.DateRollConvention paymentDateRollConvention, DayCountConvention daycountConvention, double[] parameter, double timeScaling, double periodOffset) {
		super(name, referenceDate);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;
		this.daycountConvention = daycountConvention;
		this.periodOffset = periodOffset;

		discountCurve = new DiscountCurveNelsonSiegelSvensson(name, referenceDate, parameter, timeScaling);
	}

	/**
	 * @param name The name of the curve. The curve can be fetched under this name when being part of an {@link net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols}.
	 * @param referenceDate The reference date to the curve, i.e., the date associated with t=0.
	 * @param paymentOffsetCode The payment offset code, like 3M, 6M, 12M, etc., used in calculating forwards from discount factors.
	 * @param paymentBusinessdayCalendar The payment businessday calendar.
	 * @param paymentDateRollConvention The payment date roll convention.
	 * @param daycountConvention The daycount convention.
	 * @param parameter The Nelson-Siegel-Svensson parameters in the order \( ( \beta_0, \beta_1, \beta_2, \beta_3, \tau_0, \tau_1 ) \).
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 */
	public ForwardCurveNelsonSiegelSvensson(String name, LocalDate referenceDate, String paymentOffsetCode, BusinessdayCalendar paymentBusinessdayCalendar, BusinessdayCalendar.DateRollConvention paymentDateRollConvention, DayCountConvention daycountConvention, double[] parameter, double timeScaling) {
		this(name, referenceDate, paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, daycountConvention, parameter, timeScaling, 0.0);
	}

	@Override
	public double getForward(AnalyticModel model, double fixingTime) {
		return getForward(model, fixingTime, getPaymentOffset(fixingTime+periodOffset));
	}

	@Override
	public double getForward(AnalyticModel model, double fixingTime, double paymentOffset) {
		double daycountFraction = (paymentOffset*discountCurve.getTimeScaling());
		if(daycountConvention != null) {
			LocalDate fixingDate		= getDateFromModelTime(fixingTime+periodOffset);
			LocalDate paymentDate		= getDateFromModelTime(fixingTime+periodOffset + paymentOffset);
			daycountFraction = Math.max(daycountConvention.getDaycountFraction(fixingDate, paymentDate), 1.0/365.0);
		}

		return (discountCurve.getDiscountFactor(model, fixingTime+periodOffset) / discountCurve.getDiscountFactor(model, fixingTime+periodOffset + paymentOffset) - 1.0) / daycountFraction;
	}

	@Override
	public String getDiscountCurveName() {
		return null;
	}

	@Override
	public CurveBuilder getCloneBuilder() {
		return new CurveBuilder() {
			@Override
			public Curve build() {
				return ForwardCurveNelsonSiegelSvensson.this;
			}

			@Override
			public CurveBuilder addPoint(double time, double value, boolean isParameter) {
				throw new UnsupportedOperationException("NSS curve does not support adding points.");
			}
		};
	}

	@Override
	public ForwardCurveNelsonSiegelSvensson clone() throws CloneNotSupportedException {
		return (ForwardCurveNelsonSiegelSvensson)super.clone();
	}

	@Override
	public ForwardCurveNelsonSiegelSvensson getCloneForParameter(double[] value) throws CloneNotSupportedException {
		return new ForwardCurveNelsonSiegelSvensson(getName(), getReferenceDate(), paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, daycountConvention, value, discountCurve.getTimeScaling(), periodOffset);
	}

	@Override
	public double getValue(AnalyticModel model, double time) {
		return getForward(model, time, getPaymentOffset(time));
	}

	@Override
	public double[] getParameter() {
		return discountCurve.getParameter();
	}

	/**
	 * Returns the forwards for a given vector fixing times.
	 *
	 * @param model An analytic model providing a context. The discount curve (if needed) is obtained from this model.
	 * @param fixingTimes The given fixing times.
	 * @return The forward rates.
	 */
	public double[] getForwards(AnalyticModel model, double[] fixingTimes)
	{
		double[] values = new double[fixingTimes.length];

		for(int i=0; i<fixingTimes.length; i++) {
			values[i] = getForward(model, fixingTimes[i]);
		}

		return values;
	}

	@Override
	public void setParameter(double[] parameter) {
		discountCurve.setParameter(parameter);
	}

	/*
	 * @TODO: This operates on model internal time and not on curve time - which is inefficient. Performance improvement possible.
	 * @TODO: Should use a cache
	 */
	@Override
	public double getPaymentOffset(double fixingTime) {
		LocalDate fixingDate		= getDateFromModelTime(fixingTime);
		LocalDate paymentDate		= paymentBusinessdayCalendar.getAdjustedDate(fixingDate, paymentOffsetCode, paymentDateRollConvention);
		double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(getReferenceDate(), paymentDate);
		return paymentTime-fixingTime;
	}

	private LocalDate getDateFromModelTime(double fixingTime) {
		return getReferenceDate().plusDays((int)Math.round(fixingTime*365.0));
	}
}
