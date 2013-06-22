/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * A container for a forward (rate) curve. The forward curve is based on the {@link net.finmath.marketdata.model.curves.Curve} class.
 * It thus features all interpolation and extrapolation methods and interpolation entities
 * as {@link net.finmath.marketdata.model.curves.Curve}.
 * 
 * @author Christian Fries
 */
public class ForwardCurve extends Curve implements Serializable, ForwardCurveInterface {

    private static final long serialVersionUID = -4126228588123963885L;

	enum InterpolationEntityForward {
		FORWARD,
		FORWARD_TIMES_DISCOUNTFACTOR
	}

	private InterpolationEntityForward	interpolationEntityForward = InterpolationEntityForward.FORWARD;
	private final String						discountCurveName;
    private final double						paymentOffset;

    /**
     * Generate a forward curve using a given discount curve and payment offset. The forward F(t) of an index is such that
     * F(t) * D(t+p) equals the market price of the corresponding index fixed in t and paid in t+d, where t is the fixing time
     * of the index and t+p is the payment time of the index. F(t) is the corresponding forward and D is the associated discount
     * curve.
     * 
     * @param name The name of this curve.
     * @param discountCurveName The name of a discount curve associated with this index (associated with it's funding or collateralization), if any.
     * @param paymentOffset Time between fixing and payment.
     */
    public ForwardCurve(String name, String discountCurveName, double paymentOffset) {
    	super(name, InterpolationMethod.CUBIC_SPLINE, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE);
	    this.interpolationEntityForward	= InterpolationEntityForward.FORWARD;
	    this.discountCurveName			= discountCurveName;
	    this.paymentOffset				= paymentOffset;
	}

    /**
     * Generate a forward curve using a given discount curve and payment offset. The forward F(t) of an index is such that
     * F(t) * D(t+p) equals the market price of the corresponding index fixed in t and paid in t+d, where t is the fixing time
     * of the index and t+p is the payment time of the index. F(t) is the corresponding forward and D is the associated discount
     * curve.
     * 
     * @param name The name of this curve.
     * @param interpolationEntityForward The interpolation entity used to interpolate this forward curve
     * @param discountCurveName Name of the discount curve associated with this index (associated with it's funding or collateralization).
     * @param paymentOffset Time between fixing and payment.
     */
    public ForwardCurve(String name, InterpolationEntityForward interpolationEntityForward, String discountCurveName, double paymentOffset) {
    	super(name, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE);
	    this.interpolationEntityForward	= interpolationEntityForward;
	    this.discountCurveName			= discountCurveName;
	    this.paymentOffset				= paymentOffset;
    }

	/**
	 * Create a forward curve from given times and given forwards.
	 * 
     * @param name The name of this curve.
	 * @param times A vector of given time points.
	 * @param givenForwards A vector of given forwards (corresponding to the given time points).
	 * @return A new ForwardCurve object.
	 */
	public static ForwardCurve createForwardCurveFromForwards(String name, double[] times, double[] givenForwards, double paymentOffset) {
		ForwardCurve forwardCurve = new ForwardCurve(name, InterpolationEntityForward.FORWARD, null, paymentOffset);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			forwardCurve.addForward(null, times[timeIndex], givenForwards[timeIndex]);
		}
		
		return forwardCurve;
	}

	/**
	 * Create a forward curve from given times and given forwards with respect to an associated discount curve and payment offset.
	 * 
     * @param name The name of this curve.
	 * @param times A vector of given time points.
	 * @param givenForwards A vector of given forwards (corresponding to the given time points).
	 * @param model An analytic model providing a context. The discount curve (if needed) is obtained from this model.
	 * @param discountCurveName Name of the discount curve associated with this index (associated with it's funding or collateralization).
     * @param paymentOffset Time between fixing and payment.
	 * @return A new ForwardCurve object.
	 */
	public static ForwardCurve createForwardCurveFromForwards(String name, double[] times, double[] givenForwards, AnalyticModelInterface model, String discountCurveName, double paymentOffset) {
		ForwardCurve forwardCurve = new ForwardCurve(name, InterpolationEntityForward.FORWARD, discountCurveName, paymentOffset);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			forwardCurve.addForward(model, times[timeIndex], givenForwards[timeIndex]);
		}
		
		return forwardCurve;
	}

    /**
	 * Returns the forward for the corresponding fixing time.
	 * 
	 * @param model An analytic model providing a context. The discount curve (if needed) is obtained from this model.
     * @param fixingTime The fixing time of the index.
	 * 
	 * @return The forward
	 */
	@Override
    public double getForward(AnalyticModelInterface model, double fixingTime)
	{
		double interpolationEntityForwardValue = this.getValue(model, fixingTime);
		switch(interpolationEntityForward) {
		case FORWARD:
		default:
			return interpolationEntityForwardValue;
		case FORWARD_TIMES_DISCOUNTFACTOR:
			return interpolationEntityForwardValue / model.getDiscountCurve(discountCurveName).getValue(model, fixingTime+paymentOffset);
		}
	}


	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface#getDiscountCurve()
	 */
	@Override
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface#getPaymentOffset()
	 */
	@Override
	public double getPaymentOffset() {
		return paymentOffset;
	}

	protected void addForward(AnalyticModelInterface model, double fixingTime, double forward) {
		double interpolationEntityForwardValue;
		switch(interpolationEntityForward) {
		case FORWARD:
		default:
			interpolationEntityForwardValue = forward;
			break;
		case FORWARD_TIMES_DISCOUNTFACTOR:
			interpolationEntityForwardValue = forward * model.getDiscountCurve(discountCurveName).getValue(model, fixingTime+paymentOffset);
			break;
		}
		this.addPoint(fixingTime, interpolationEntityForwardValue);
	}

}
