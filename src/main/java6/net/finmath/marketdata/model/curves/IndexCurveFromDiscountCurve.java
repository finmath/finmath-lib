/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28 Oct 2014
 */

package net.finmath.marketdata.model.curves;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * An index curve there the value at time t is given by indexValue / discountCurve.getValue(t).
 *
 * @author Christian Fries
 */
public class IndexCurveFromDiscountCurve extends AbstractCurve implements CurveInterface {

	private static final long serialVersionUID = -3760460344256117452L;

	private final double indexValue;
	private final DiscountCurveInterface discountCurve;

	/**
	 * @param name The name of this curve.
	 * @param indexValue The index value at the discount curve's t=0, i.e., the reference date of the discount curve.
	 * @param discountCurve The discont curve.
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

