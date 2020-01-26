/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.05.2014
 */

package net.finmath.marketdata.model.curves;

import net.finmath.marketdata.model.AnalyticModel;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class ForwardCurveWithFixings extends PiecewiseCurve implements ForwardCurve {

	private static final long serialVersionUID = -6192098475095644443L;

	/**
	 * Create a piecewise forward curve.
	 *
	 * @param curveInterface Base curve, to be used by default.
	 * @param fixedPartCurve CurveFromInterpolationPoints to be used for the open time interval from fixedPartStartTime to fixedPartEndTime.
	 * @param fixedPartStartTime Start time of the interval where we use the fixedPartCurve.
	 * @param fixedPartEndTime End time of the interval where we use the fixedPartCurve.
	 */
	public ForwardCurveWithFixings(final ForwardCurve curveInterface, final ForwardCurve fixedPartCurve, final double fixedPartStartTime, final double fixedPartEndTime) {
		super(curveInterface, fixedPartCurve, fixedPartStartTime, fixedPartEndTime);
	}

	@Override
	public double getForward(final AnalyticModel model, final double fixingTime) {
		if(fixingTime > this.getFixedPartStartTime() && fixingTime < this.getFixedPartEndTime()) {
			return ((ForwardCurve)getFixedPartCurve()).getForward(model, fixingTime);
		}
		else {
			return ((ForwardCurve)getBaseCurve()).getForward(model, fixingTime);
		}
	}

	@Override
	public double getForward(final AnalyticModel model, final double fixingTime, final double paymentOffset) {
		if(fixingTime > this.getFixedPartStartTime() && fixingTime < this.getFixedPartEndTime()) {
			return ((ForwardCurve)getFixedPartCurve()).getForward(model, fixingTime, paymentOffset);
		}
		else {
			return ((ForwardCurve)getBaseCurve()).getForward(model, fixingTime, paymentOffset);
		}
	}

	/**
	 * Returns the forwards for a given vector fixing times.
	 *
	 * @param model An analytic model providing a context. The discount curve (if needed) is obtained from this model.
	 * @param fixingTimes The given fixing times.
	 * @return The forward rates.
	 */
	public double[] getForwards(final AnalyticModel model, final double[] fixingTimes)
	{
		final double[] values = new double[fixingTimes.length];

		for(int i=0; i<fixingTimes.length; i++) {
			values[i] = getForward(model, fixingTimes[i]);
		}

		return values;
	}

	@Override
	public String getDiscountCurveName() {
		return ((ForwardCurve)getBaseCurve()).getDiscountCurveName();
	}

	@Override
	public double getPaymentOffset(final double fixingTime) {
		return ((ForwardCurve)getBaseCurve()).getPaymentOffset(fixingTime);
	}

	@Override
	public Curve getCloneForParameter(final double[] value) throws CloneNotSupportedException {
		return new ForwardCurveWithFixings((ForwardCurve)getBaseCurve().getCloneForParameter(value), (ForwardCurve)getFixedPartCurve(), getFixedPartStartTime(), getFixedPartEndTime());
	}

	@Override
	public ForwardCurveWithFixings clone() throws CloneNotSupportedException {
		return new ForwardCurveWithFixings((ForwardCurve)getBaseCurve().clone(), (ForwardCurve)getFixedPartCurve(), getFixedPartStartTime(), getFixedPartEndTime());
	}
}
