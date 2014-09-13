/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import java.util.Calendar;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * Implementation of a forward given by a Nelson-Siegel-Svensson (NSS) parameterization.
 * In the NSS parameterization the zero rate \( r(T) \) is given by
 * 
 * \( r(T) = \beta_0 + \beta_1 \frac{(1-x_0)/(T/\tau_0)} + \beta_2 ( \frac{(1-x_0)/(T/\tau_0)} - x_0) + \beta_3 ( \frac{(1-x_2)/(T/\tau_1)} - x_1) \)
 * 
 * where \( x_0 = \exp(-T/\tau_0) \) and \( x_1 = \exp(-T/\tau_1) \).
 * 
 * The sub-family of curve with \( \beta_3 = 0 \) is called Nelson-Siegel parameterization.
 * 
 * @author Christian Fries
 */
public class ForwardCurveNelsonSiegelSvenson extends AbstractCurve implements Serializable, ForwardCurveInterface {

	private static final long serialVersionUID = 8024640795839972709L;

	private String paymentOffsetCode;
	private BusinessdayCalendarInterface paymentBusinessdayCalendar;
	private BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention;
	private DayCountConventionInterface daycountConvention;

	private DiscountCurveNelsonSiegelSvenson discountCurve;
	
	/**
	 * @param name The name of the curve. The curve can be fetched under this name when being part of an {@link AnalyticModel}.
	 * @param referenceDate The reference date to the curve, i.e., the date associated with t=0.
	 * @param paymentOffsetCode The payment offset code, like 3M, 6M, 12M, etc., used in calculating forwards from discount factors.
	 * @param paymentBusinessdayCalendar The payment businessday calendar.
	 * @param paymentDateRollConvention The payment date roll convention.
	 * @param daycountConvention The daycount convention.
	 * @param parameter The six Nelson Siegel Svenson parameters, beta_0, ..., beta_3, tau_0, tau_1.
	 * @param timeScaling A scaling factor applied to t when converting from global double time to the parametric function argument t.
	 */
	public ForwardCurveNelsonSiegelSvenson(String name, Calendar referenceDate, String paymentOffsetCode, BusinessdayCalendarInterface paymentBusinessdayCalendar, BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention, DayCountConventionInterface daycountConvention, double[] parameter, double timeScaling) {
		super(name, referenceDate);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;
		this.daycountConvention = daycountConvention;

		discountCurve = new DiscountCurveNelsonSiegelSvenson(name, referenceDate, parameter, timeScaling);
	}

	@Override
	public double getForward(AnalyticModelInterface model, double fixingTime) {
		return getForward(model, fixingTime, getPaymentOffset(fixingTime));
	}

	@Override
	public double getForward(AnalyticModelInterface model, double fixingTime, double paymentOffset) {
		double daycountFraction = getCurveTime(fixingTime + paymentOffset)-getCurveTime(fixingTime);
		return (discountCurve.getDiscountFactor(model, fixingTime) / discountCurve.getDiscountFactor(model, fixingTime + paymentOffset) - 1.0) / daycountFraction;
	}

	public double getCurveTime(double time) {
		// @TODO This will later performed by an utility time class
		Calendar modelDate	= (Calendar)getReferenceDate().clone();
		modelDate.add(Calendar.DAY_OF_YEAR, (int)(time*365));

		double paymentTime = daycountConvention.getDaycountFraction(getReferenceDate(), modelDate);
		return paymentTime;
	}

	@Override
	public String getDiscountCurveName() {
		return null;
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		return null;
	}

	@Override
	public ForwardCurveNelsonSiegelSvenson clone() throws CloneNotSupportedException {
		return (ForwardCurveNelsonSiegelSvenson)super.clone();
	}

	@Override
	public ForwardCurveNelsonSiegelSvenson getCloneForParameter(double[] value) throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		return getForward(model, time, getPaymentOffset(time));
	}

	@Override
	public double[] getParameter() {
		return discountCurve.getParameter();
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.calibration.ParameterObjectInterface#setParameter(double[])
	 */
	@Override
	public void setParameter(double[] parameter) {
		discountCurve.setParameter(parameter);
	}

	/*
	 * @TODO: This operates on model internal time and not on curve time - which is inefficient. Performance improvement possible.
	 */
	@Override
	public double getPaymentOffset(double fixingTime) {
		Calendar paymentDate	= (Calendar)getReferenceDate().clone();
		paymentDate.add(Calendar.DAY_OF_YEAR, (int)(fixingTime*365));
		paymentDate = paymentBusinessdayCalendar.getAdjustedDate(paymentDate, paymentOffsetCode, paymentDateRollConvention);
		double paymentTime = (new DayCountConvention_ACT_365()).getDaycountFraction(getReferenceDate(), paymentDate);
		return paymentTime-fixingTime;
	}
}
