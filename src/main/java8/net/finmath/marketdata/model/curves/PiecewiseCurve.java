/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.05.2014
 */

package net.finmath.marketdata.model.curves;

import java.time.LocalDate;

import net.finmath.marketdata.model.AnalyticModel;

/**
 * A piecewise curve. The curve consists of a base curve and a second curve.
 * If the <code>time</code> parameter of the {@link #getValue(AnalyticModel, double)}
 * method falls inside a pre-defined open interval, it is delegated to the second curve,
 * otherwise it is delegated to the base curve.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class PiecewiseCurve extends AbstractCurve implements Curve {

	private static final long serialVersionUID = 8846923173857477343L;

	private Curve	baseCurve;
	private final Curve	fixedPartCurve;

	private final double fixedPartStartTime;
	private final double fixedPartEndTime;


	/**
	 * A builder (following the builder pattern) for PiecewiseCurve objects.
	 * Allows to successively construct a curve object by adding points to its base points.
	 *
	 * @author Christian Fries
	 */
	public static class Builder extends CurveInterpolation.Builder implements CurveBuilder {

		private PiecewiseCurve			curve = null;

		/**
		 * Create a CurveBuilder from a given piecewiseCurve
		 *
		 * @param piecewiseCurve The piecewise curve from which to copy the fixed part upon build().
		 * @throws CloneNotSupportedException Thrown, when the base curve could not be cloned.
		 */
		public Builder(final PiecewiseCurve piecewiseCurve) throws CloneNotSupportedException {
			super((CurveInterpolation)(piecewiseCurve.baseCurve));
			curve = piecewiseCurve;
		}

		@Override
		public Curve build() throws CloneNotSupportedException {
			final PiecewiseCurve buildCurve = curve.clone();
			buildCurve.baseCurve = super.build();
			curve = null;
			return buildCurve;
		}
	}

	public PiecewiseCurve(final Curve curve,
			final Curve fixedPartCurve, final double fixedPartStartTime,
			final double fixedPartEndTime) {
		super(curve.getName(), curve.getReferenceDate());
		baseCurve = curve;
		this.fixedPartCurve = fixedPartCurve;
		this.fixedPartStartTime = fixedPartStartTime;
		this.fixedPartEndTime = fixedPartEndTime;
	}

	@Override
	public double[] getParameter() {
		return baseCurve.getParameter();
	}

	@Override
	public void setParameter(final double[] parameter) {
		baseCurve.setParameter(parameter);
	}

	@Override
	public String getName() {
		return baseCurve.getName();
	}

	@Override
	public LocalDate getReferenceDate() {
		return baseCurve.getReferenceDate();
	}

	/**
	 * @return the baseCurve
	 */
	public Curve getBaseCurve() {
		return baseCurve;
	}

	/**
	 * @return the fixedPartCurve
	 */
	public Curve getFixedPartCurve() {
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
	public double getValue(final double time) {
		return getValue(null, time);
	}

	@Override
	public double getValue(final AnalyticModel model, final double time) {
		if(time >fixedPartStartTime && time < fixedPartEndTime) {
			return fixedPartCurve.getValue(model, time);
		}
		else {
			return baseCurve.getValue(model, time);
		}
	}

	@Override
	public Curve getCloneForParameter(final double[] value) throws CloneNotSupportedException {
		final PiecewiseCurve newCurve = clone();
		newCurve.baseCurve = baseCurve.getCloneForParameter(value);

		return newCurve;
	}

	@Override
	public PiecewiseCurve clone() throws CloneNotSupportedException {
		return new PiecewiseCurve((Curve) baseCurve.clone(), fixedPartCurve, fixedPartStartTime, fixedPartEndTime);
	}

	@Override
	public Builder getCloneBuilder() throws CloneNotSupportedException {
		return new Builder(this);
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
