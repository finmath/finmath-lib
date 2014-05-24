/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.05.2014
 */

package net.finmath.marketdata.model.curves;

import java.util.Calendar;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * @author Christian Fries
 *
 */
public class PiecewiseCurve extends AbstractCurve implements CurveInterface {

	private CurveInterface	baseCurve;
	private CurveInterface	fixedPartCurve;

	private double fixedPartStartTime;
	private double fixedPartEndTime;
	

	/**
	 * A builder (following the builder pattern) for Curve objects.
	 * Allows to successively construct a curve object by adding points.
	 * 
	 * @author Christian Fries
	 */
	public static class CurveBuilder implements CurveBuilderInterface {
		private CurveBuilderInterface	baseCurveBuilder = null;
		private PiecewiseCurve			curve = null;
		
		/**
		 * Build a curve with a given name and given reference date.
		 * 
		 * @param name The name of this curve.
		 * @param referenceDate The reference date for this curve, i.e., the date which defined t=0.
		 */
		public CurveBuilder(CurveBuilderInterface curveBuilderInterface, PiecewiseCurve piecewiseCurve) {
			this.baseCurveBuilder = curveBuilderInterface;
			this.curve = piecewiseCurve;
		}
		
		@Override
		public CurveInterface build() throws CloneNotSupportedException {
			PiecewiseCurve buildCurve = (PiecewiseCurve)curve.clone();
			buildCurve.baseCurve = baseCurveBuilder.build();
			baseCurveBuilder = null;
			curve = null;
			return buildCurve;
		}

		@Override
		public CurveBuilderInterface addPoint(double time, double value, boolean isParameter) {
			baseCurveBuilder.addPoint(time, value, isParameter);
			return this;
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
		PiecewiseCurve newCurve = (PiecewiseCurve)clone();
		newCurve.baseCurve = baseCurve.getCloneForParameter(value);
		
		return newCurve;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new PiecewiseCurve((CurveInterface) baseCurve.clone(), fixedPartCurve, fixedPartStartTime, fixedPartEndTime);
	}

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		return new CurveBuilder(baseCurve.getCloneBuilder(), this);
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
