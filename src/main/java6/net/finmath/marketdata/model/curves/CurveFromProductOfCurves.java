/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;

import org.joda.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * A curve derived from other curves by multiplying the values.
 * 
 * @author Christian Fries
 */
public class CurveFromProductOfCurves extends AbstractCurve implements Serializable, CurveInterface {
	
	private static final long serialVersionUID = 8850409340966149755L;

	private CurveInterface[] curves;

	/**
	 * Create a curve using one or more curves.
	 * 
	 * @param name The name of this curve.
	 * @param referenceDate The reference date of this curve.
	 * @param curves Argument list or array of curves.
	 */
	public CurveFromProductOfCurves(String name, LocalDate referenceDate, CurveInterface... curves) {
		super(name, referenceDate);

		this.curves = curves;
	}

	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		double value = 1.0;

		for(CurveInterface curve : curves) value *= curve.getValue(model, time);
		
		return value;
	}


	@Override
	public double[] getParameter() {
		return null;
	}

	@Override
	public void setParameter(double[] parameter) {
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		throw new CloneNotSupportedException("Cloning is unsupported for this curve.");
	}
}
