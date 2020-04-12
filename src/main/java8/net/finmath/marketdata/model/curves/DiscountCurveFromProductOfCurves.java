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
 * A discount curve derived from other discount curves by multiplying the discount factors.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class DiscountCurveFromProductOfCurves extends AbstractCurve implements Serializable, DiscountCurve {

	private static final long serialVersionUID = 6643801855646089707L;

	private final String[] curveNames;
	private final DiscountCurve[] curves;

	/**
	 * Create a discount curve using one or more curves.
	 *
	 * The product curve is generated dynamically by looking up the given curveNames in the model passed
	 * to the method {@link #getDiscountFactor(AnalyticModel, double)}.
	 *
	 * @param name The name of this curve.
	 * @param referenceDate The reference date of this curve.
	 * @param curveNames Argument list or array of curve names.
	 */
	public DiscountCurveFromProductOfCurves(final String name, final LocalDate referenceDate, final String... curveNames) {
		super(name, referenceDate);

		this.curveNames = curveNames;
		this.curves = null;
	}

	/**
	 * Create a discount curve using one or more given curves.
	 *
	 * @param name The name of this curve.
	 * @param referenceDate The reference date of this curve.
	 * @param curves Argument list or array of curves.
	 */
	public DiscountCurveFromProductOfCurves(final String name, final LocalDate referenceDate, final DiscountCurve... curves) {
		super(name, referenceDate);

		this.curveNames = null;
		this.curves = curves;
	}

	@Override
	public double getDiscountFactor(final double maturity) {
		return getDiscountFactor(null, maturity);
	}

	@Override
	public double getDiscountFactor(final AnalyticModel model, final double maturity) {
		double discountFactor = 1.0;

		if(curveNames != null) {
			if(model == null) {
				throw new IllegalArgumentException("This object requires that a reference to an AnalyticModel is passed to a call this method.");
			}

			for(final String curveName : curveNames) {
				discountFactor *= model.getDiscountCurve(curveName).getDiscountFactor(model, maturity);
			}
		}
		else {
			for(final DiscountCurve curve : curves) {
				discountFactor *= curve.getDiscountFactor(model, maturity);
			}
		}

		return discountFactor;
	}

	@Override
	public double getValue(final AnalyticModel model, final double time) {
		return getDiscountFactor(model, time);
	}


	@Override
	public double[] getParameter() {
		return null;
	}

	@Override
	public void setParameter(final double[] parameter) {
	}

	@Override
	public CurveBuilder getCloneBuilder() {
		// TODO Auto-generated method stub
		return null;
	}
}
