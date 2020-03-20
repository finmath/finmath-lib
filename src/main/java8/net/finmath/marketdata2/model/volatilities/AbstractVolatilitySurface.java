/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.marketdata2.model.volatilities;

import java.time.LocalDate;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.time.daycount.DayCountConvention;

/**
 * Abstract base class for a volatility surface. It stores the name of the surface and
 * provides some convenient way of getting values.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractVolatilitySurface implements VolatilitySurface, Cloneable {

	private	final	LocalDate	referenceDate;
	private final	String		name;

	private ForwardCurve forwardCurve;
	private DiscountCurve discountCurve;
	private QuotingConvention quotingConvention;
	private DayCountConvention daycountConvention;

	public AbstractVolatilitySurface(final String name, final LocalDate referenceDate, final ForwardCurve forwardCurve,
			final DiscountCurve discountCurve, final QuotingConvention quotingConvention, final DayCountConvention daycountConvention) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.quotingConvention = quotingConvention;
		this.daycountConvention = daycountConvention;
	}

	public AbstractVolatilitySurface(final String name, final LocalDate referenceDate) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	@Override
	public String toString() {
		return super.toString() + "\n\"" + this.getName() + "\"";
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	@Override
	public QuotingConvention getQuotingConvention() {
		return quotingConvention;
	}

	/**
	 * Convert the value of a caplet from one quoting convention to another quoting convention.
	 *
	 * @param model An analytic model providing the context when fetching required market date.
	 * @param optionMaturity Option maturity of the caplet.
	 * @param optionStrike Option strike of the caplet.
	 * @param value Value of the caplet given in the form of <code>fromQuotingConvention</code>.
	 * @param fromQuotingConvention The quoting convention of the given value.
	 * @param toQuotingConvention The quoting convention requested.
	 * @return Value of the caplet given in the form of <code>toQuotingConvention</code>.
	 */
	public double convertFromTo(final AnalyticModel model, final double optionMaturity, final double optionStrike, final double value, final QuotingConvention fromQuotingConvention, final QuotingConvention toQuotingConvention) {

		if(fromQuotingConvention.equals(toQuotingConvention)) {
			return value;
		}

		if(getDiscountCurve() == null) {
			throw new IllegalArgumentException("Missing discount curve. Conversion of QuotingConvention requires forward curve and discount curve to be set.");
		}
		if(getForwardCurve() == null) {
			throw new IllegalArgumentException("Missing forward curve. Conversion of QuotingConvention requires forward curve and discount curve to be set.");
		}

		final double periodStart = optionMaturity;
		final double periodEnd = periodStart + getForwardCurve().getPaymentOffset(periodStart);

		final double forward = getForwardCurve().getForward(model, periodStart);

		double daycountFraction;
		if(getDaycountConvention() != null) {
			final LocalDate startDate = referenceDate.plusDays((int)Math.round(periodStart*365));
			final LocalDate endDate   = referenceDate.plusDays((int)Math.round(periodEnd*365));
			daycountFraction = getDaycountConvention().getDaycountFraction(startDate, endDate);
		}
		else {
			daycountFraction = getForwardCurve().getPaymentOffset(periodStart);
		}

		final double payoffUnit = getDiscountCurve().getDiscountFactor(optionMaturity+getForwardCurve().getPaymentOffset(optionMaturity)) * daycountFraction;

		if(toQuotingConvention.equals(QuotingConvention.PRICE) && fromQuotingConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL)) {
			return AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(toQuotingConvention.equals(QuotingConvention.PRICE) && fromQuotingConvention.equals(QuotingConvention.VOLATILITYNORMAL)) {
			return AnalyticFormulas.bachelierOptionValue(forward, value, optionMaturity, optionStrike, payoffUnit);
		}
		else if(toQuotingConvention.equals(QuotingConvention.VOLATILITYLOGNORMAL) && fromQuotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else if(toQuotingConvention.equals(QuotingConvention.VOLATILITYNORMAL) && fromQuotingConvention.equals(QuotingConvention.PRICE)) {
			return AnalyticFormulas.bachelierOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, value);
		}
		else {
			return convertFromTo(model, optionMaturity, optionStrike, convertFromTo(model, optionMaturity, optionStrike, value, fromQuotingConvention, QuotingConvention.PRICE), QuotingConvention.PRICE, toQuotingConvention);
		}
	}

	/**
	 * Convert the value of a caplet from one quoting convention to another quoting convention.
	 *
	 * @param optionMaturity Option maturity of the caplet.
	 * @param optionStrike Option strike of the caplet.
	 * @param value Value of the caplet given in the form of <code>fromQuotingConvention</code>.
	 * @param fromQuotingConvention The quoting convention of the given value.
	 * @param toQuotingConvention The quoting convention requested.
	 * @return Value of the caplet given in the form of <code>toQuotingConvention</code>.
	 */
	public double convertFromTo(final double optionMaturity, final double optionStrike, final double value, final QuotingConvention fromQuotingConvention, final QuotingConvention toQuotingConvention) {
		return convertFromTo(null, optionMaturity, optionStrike, value, fromQuotingConvention, toQuotingConvention);
	}

	public ForwardCurve getForwardCurve() {
		return forwardCurve;
	}

	public DiscountCurve getDiscountCurve() {
		return discountCurve;
	}

	public DayCountConvention getDaycountConvention() {
		return daycountConvention;
	}
}
