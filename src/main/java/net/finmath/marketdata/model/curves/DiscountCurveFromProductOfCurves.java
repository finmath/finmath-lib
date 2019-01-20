/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;
import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;

/**
 * A discount curve derived from other discount curves
 * by multiplying the discount factors.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DiscountCurveFromProductOfCurves extends AbstractCurve implements Serializable, DiscountCurve {

	private static final long serialVersionUID = 8850409340966149755L;

	private DiscountCurve[] curves;

	/**
	 * Create a discount curve using one or more curves.
	 *
	 * @param name The name of this curve.
	 * @param referenceDate The reference date of this curve.
	 * @param curves Argument list or array of curves.
	 */
	public DiscountCurveFromProductOfCurves(String name, LocalDate referenceDate, DiscountCurve... curves) {
		super(name, referenceDate);

		this.curves = curves;
	}

	@Override
	public double getDiscountFactor(double maturity) {
		return getDiscountFactor(null, maturity);
	}

	@Override
	public double getDiscountFactor(AnalyticModel model, double maturity) {
		double discountFactor = 1.0;

		for(DiscountCurve curve : curves) {
			discountFactor *= curve.getDiscountFactor(model, maturity);
		}

		return discountFactor;
	}

	@Override
	public double getValue(AnalyticModel model, double time) {
		return getDiscountFactor(model, time);
	}


	@Override
	public double[] getParameter() {
		return null;
	}

	@Override
	public void setParameter(double[] parameter) {
	}


	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getCloneBuilder()
	 */
	@Override
	public CurveBuilder getCloneBuilder() {
		// TODO Auto-generated method stub
		return null;
	}
}
