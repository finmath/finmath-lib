/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.marketdata.model.volatilities;

import java.util.Calendar;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;

/**
 * Abstract base class for a volatility surface. It stores the name of the surface and
 * provides some convenient way of getting values.
 *  
 * @author Christian Fries
 */
public abstract class AbstractVolatilitySurface implements VolatilitySurfaceInterface, Cloneable {

	private	final	Calendar	referenceDate;
	private final	String		name;

	protected ForwardCurveInterface forwardCurve;
	protected DiscountCurveInterface discountCurve;
	protected QuotingConvention quotingConvention;

	public AbstractVolatilitySurface(String name, Calendar referenceDate) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Calendar getReferenceDate() {
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
	 * Convert the value of a caplet from on quoting convention to another quoting convention.
	 * 
	 * @param model An analytic model providing the context when fetching required market date.
	 * @param optionMaturity Option maturity of the caplet.
	 * @param optionStrike Option strike of the cpalet.
	 * @param value Value of the caplet given in the form of <code>fromQuotingConvention</code>.
	 * @param fromQuotingConvention The quoting convention of the given value.
	 * @param toQuotingConvention The quoting convention requested.
	 * @return Value of the caplet given in the form of <code>toQuotingConvention</code>. 
	 */
	public double convertFromTo(AnalyticModelInterface model, double optionMaturity, double optionStrike, double value, QuotingConvention fromQuotingConvention, QuotingConvention toQuotingConvention) {

		if(fromQuotingConvention.equals(toQuotingConvention)) return value;

		double forward = forwardCurve.getForward(null, optionMaturity);
		double payoffUnit = discountCurve.getDiscountFactor(optionMaturity+forwardCurve.getPaymentOffset(optionMaturity)) * forwardCurve.getPaymentOffset(optionMaturity);

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
	public double convertFromTo(double optionMaturity, double optionStrike, double value, QuotingConvention fromQuotingConvention, QuotingConvention toQuotingConvention) {
		return convertFromTo(null, optionMaturity, optionStrike, value, fromQuotingConvention, toQuotingConvention);
	}
}