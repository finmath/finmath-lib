/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 28 Oct 2014
 */

package net.finmath.marketdata.model.curves;

import java.util.Calendar;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * @author Christian Fries
 */
public class IndexCurveFromDiscountCurve extends AbstractCurve implements CurveInterface {

	private final double indexValue;
	private final DiscountCurveInterface discountCurve;
	
	/**
	 * @param name
	 * @param indexValue
	 * @param discountCurve
	 */
	public IndexCurveFromDiscountCurve(String name, double indexValue, DiscountCurveInterface discountCurve) {
		super(name, discountCurve.getReferenceDate());

		this.indexValue = indexValue;
		this.discountCurve = discountCurve;
	}

	@Override
	public double[] getParameter() {
		return discountCurve.getParameter();
	}

	@Override
	public void setParameter(double[] parameter) {
		discountCurve.setParameter(parameter);
	}

	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		return indexValue / discountCurve.getDiscountFactor(model, time);
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}
