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
 * A curve derived from other curves by multiplying the values.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class CurveFromProductOfCurves extends AbstractCurve implements Serializable, Curve {

	private static final long serialVersionUID = 8850409340966149755L;

	private final Curve[] curves;

	/**
	 * Create a curve using one or more curves.
	 *
	 * @param name The name of this curve.
	 * @param referenceDate The reference date of this curve.
	 * @param curves Argument list or array of curves.
	 */
	public CurveFromProductOfCurves(final String name, final LocalDate referenceDate, final Curve... curves) {
		super(name, referenceDate);

		this.curves = curves;
	}

	@Override
	public double getValue(final AnalyticModel model, final double time) {
		double value = 1.0;

		for(final Curve curve : curves) {
			value *= curve.getValue(model, time);
		}

		return value;
	}


	@Override
	public double[] getParameter() {
		return null;
	}

	@Override
	public void setParameter(final double[] parameter) {
	}

	@Override
	public CurveBuilder getCloneBuilder() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Cloning is unsupported for this curve.");
	}
}
