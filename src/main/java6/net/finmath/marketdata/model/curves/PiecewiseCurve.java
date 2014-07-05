/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.05.2014
 */

package net.finmath.marketdata.model.curves;

import java.util.Calendar;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * A piecewise curve. The curve consists of a base curve and a second curve.
 * If the <code>time</code> parameter of the {@link #getValue(AnalyticModelInterface, double)}
 * method falls inside a pre-defined open interval, it is delegated to the second curve,
 * otherwise it is delegated to the base curve.
 * 
 * @author Christian Fries
 */
public class PiecewiseCurve extends AbstractCurve implements CurveInterface {

	private CurveInterface	baseCurve;
	private CurveInterface	fixedPartCurve;

	private double fixedPartStartTime;
	private double fixedPartEndTime;
	

	/**
	 * A builder (following the builder pattern) for PiecewiseCurve objects.
	 * Allows to successively construct a curve object by adding points to its base points.
	 * 
	 * @author Christian Fries
	 */
	public static class CurveBuilder extends Curve.CurveBuilder implements CurveBuilderInterface {

		private PiecewiseCurve			curve = null;
		
		/**
		 * Create a CurveBuilder from a given piecewiseCurve
		 * 
		 * @param piecewiseCurve The piecewise curve from which to copy the fixed part upon build().
		 * @throws CloneNotSupportedException Thrown, when the base curve could not be cloned.
		 */
		public CurveBuilder(PiecewiseCurve piecewiseCurve) throws CloneNotSupportedException {
			super((Curve)(piecewiseCurve.baseCurve));
			this.curve = piecewiseCurve;
		}
		
		@Override
		public CurveInterface build() throws CloneNotSupportedException {
			PiecewiseCurve buildCurve = curve.clone();
			buildCurve.baseCurve = super.build();
			curve = null;
			return buildCurve;
		}
	}

	public PiecewiseCurve(CurveInterface curveInterface,
			CurveInterface fixedPartCurve, double fixedPartStartTime,
			double fixedPartEndTime) {
		super(curveInterface.getName(), curveInterface.getReferenceDate());
		this.baseCurve = curveInterface;
		this.fixedPartCurve = fixedPartCurve;
		this.fixedPartStartTime = fixedPartStartTime;
		this.fixedPartEndTime = fixedPartEndTime;
	}

	@Override
	public double[] getParameter() {
		return baseCurve.getParameter();
	}

	@Override
	public void setParameter(double[] parameter) {
		baseCurve.setParameter(parameter);
	}

	@Override
	public String getName() {
		return baseCurve.getName();
	}

	@Override
	public Calendar getReferenceDate() {
		return baseCurve.getReferenceDate();
	}

	/**
	 * @return the baseCurve
	 */
	public CurveInterface getBaseCurve() {
		return baseCurve;
	}

	/**
	 * @return the fixedPartCurve
	 */
	public CurveInterface getFixedPartCurve() {
		return fixedPartCurve;
	}

	/**
	 * @return the fixedPartStartTime
	 */
	public double getFixedPartStartTime() {
		return fixedPartStartTime;
	}

	/**
	 * @return the fixedPartEndTime
	 */
	public double getFixedPartEndTime() {
		return fixedPartEndTime;
	}

	@Override
	public double getValue(double time) {
		return getValue(null, time);
	}

	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		if(time >fixedPartStartTime && time < this.fixedPartEndTime) {
			return fixedPartCurve.getValue(model, time);
		}
		else {
			return baseCurve.getValue(model, time);
		}
	}

	@Override
	public CurveInterface getCloneForParameter(double[] value) throws CloneNotSupportedException {
		PiecewiseCurve newCurve = clone();
		newCurve.baseCurve = baseCurve.getCloneForParameter(value);
		
		return newCurve;
	}

	@Override
	public PiecewiseCurve clone() throws CloneNotSupportedException {
		return new PiecewiseCurve((CurveInterface) baseCurve.clone(), fixedPartCurve, fixedPartStartTime, fixedPartEndTime);
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		return new CurveBuilder(this);
	}

	@Override
	public String toString() {
		return "ForwardCurveWithFixings [getBaseCurve()=" + getBaseCurve()
				+ ", getFixedPartCurve()=" + getFixedPartCurve()
				+ ", getFixedPartStartTime()=" + getFixedPartStartTime()
				+ ", getFixedPartEndTime()=" + getFixedPartEndTime()
				+ ", toString()=" + super.toString() + "]";
	}
}
