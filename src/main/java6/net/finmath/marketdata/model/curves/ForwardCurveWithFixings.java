/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 22.05.2014
 */

package net.finmath.marketdata.model.curves;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * @author Christian Fries
 *
 */
public class ForwardCurveWithFixings extends PiecewiseCurve implements ForwardCurveInterface {

	/**
	 * Create a piecewise forward curve.
	 * 
	 * @param curveInterface Base curve, to be used by default.
	 * @param fixedPartCurve Curve to be used for the open time interval from fixedPartStartTime to fixedPartEndTime.
	 * @param fixedPartStartTime Start time of the interval where we use the fixedPartCurve.
	 * @param fixedPartEndTime End time of the interval where we use the fixedPartCurve.
	 */
	public ForwardCurveWithFixings(ForwardCurveInterface curveInterface, ForwardCurveInterface fixedPartCurve, double fixedPartStartTime, double fixedPartEndTime) {
		super(curveInterface, fixedPartCurve, fixedPartStartTime, fixedPartEndTime);
	}

	@Override
	public double getForward(AnalyticModelInterface model, double fixingTime) {
		if(fixingTime > this.getFixedPartStartTime() && fixingTime < this.getFixedPartEndTime()) {
			return ((ForwardCurveInterface)getFixedPartCurve()).getForward(model, fixingTime);
		}
		else {
			return ((ForwardCurveInterface)getBaseCurve()).getForward(model, fixingTime);
		}
	}

	@Override
	public double getForward(AnalyticModelInterface model, double fixingTime, double paymentOffset) {
		if(fixingTime > this.getFixedPartStartTime() && fixingTime < this.getFixedPartEndTime()) {
			return ((ForwardCurveInterface)getFixedPartCurve()).getForward(model, fixingTime, paymentOffset);
		}
		else {
			return ((ForwardCurveInterface)getBaseCurve()).getForward(model, fixingTime, paymentOffset);
		}
	}

	/**
	 * Returns the forwards for a given vector fixing times.
	 * 
	 * @param model An analytic model providing a context. The discount curve (if needed) is obtained from this model.
	 * @param fixingTimes The given fixing times.
	 * @return The forward rates.
	 */
	public double[] getForwards(AnalyticModelInterface model, double[] fixingTimes)
	{
		double[] values = new double[fixingTimes.length];

		for(int i=0; i<fixingTimes.length; i++) values[i] = getForward(model, fixingTimes[i]);

		return values;
	}

	@Override
	public String getDiscountCurveName() {
		return ((ForwardCurveInterface)getBaseCurve()).getDiscountCurveName();
	}

	@Override
	public double getPaymentOffset(double fixingTime) {
		return ((ForwardCurveInterface)getBaseCurve()).getPaymentOffset(fixingTime);
	}

	@Override
	public CurveInterface getCloneForParameter(double[] value) throws CloneNotSupportedException {
		return new ForwardCurveWithFixings((ForwardCurveInterface)getBaseCurve().getCloneForParameter(value), (ForwardCurveInterface)getFixedPartCurve(), getFixedPartStartTime(), getFixedPartEndTime());
	}

	@Override
	public ForwardCurveWithFixings clone() throws CloneNotSupportedException {
		return new ForwardCurveWithFixings((ForwardCurveInterface)getBaseCurve().clone(), (ForwardCurveInterface)getFixedPartCurve(), getFixedPartStartTime(), getFixedPartEndTime());
	}
	
	@Override
	public double getForward(double fixingTime) { // needs to be implemented here as this class directly inherits from an interface (without any intermediate abstract class)
		return getForward(null, fixingTime);
	}
}
