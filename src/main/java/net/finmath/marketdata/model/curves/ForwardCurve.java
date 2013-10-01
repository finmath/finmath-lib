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

    /**
     * Additional choice of interpolation entities for forward curves.
     */
	public enum InterpolationEntityForward {
		/** Interpolation is performed on the forward **/
		FORWARD,
		/** Interpolation is performed on the value = forward * discount factor **/
		FORWARD_TIMES_DISCOUNTFACTOR,
		/** Interpolation is performed on the zero rate **/
		ZERO
	}

	private InterpolationEntityForward	interpolationEntityForward = InterpolationEntityForward.FORWARD;
	private final String				discountCurveName;
    private final Curve					paymentOffsets;

    /**
     * Generate a forward curve using a given discount curve and payment offset. The forward F(t) of an index is such that
     * F(t) * D(t+p) equals the market price of the corresponding index fixed in t and paid in t+d, where t is the fixing time
     * of the index and t+p is the payment time of the index. F(t) is the corresponding forward and D is the associated discount
     * curve.
     * 
     * @param name The name of this curve.
     * @param discountCurveName The name of a discount curve associated with this index (associated with it's funding or collateralization), if any.
     * @param paymentOffsets Time between fixing and payment.
     */
    public ForwardCurve(String name, String discountCurveName) {
    	super(name, InterpolationMethod.CUBIC_SPLINE, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE);
	    this.interpolationEntityForward	= InterpolationEntityForward.FORWARD;
	    this.discountCurveName			= discountCurveName;
	    this.paymentOffsets				= new Curve("", Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.VALUE);
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
     * @param paymentOffsets Time between fixing and payment.
     */
    public ForwardCurve(String name, InterpolationEntityForward interpolationEntityForward, String discountCurveName) {
    	super(name, InterpolationMethod.CUBIC_SPLINE, ExtrapolationMethod.CONSTANT, InterpolationEntity.VALUE);
	    this.interpolationEntityForward	= interpolationEntityForward;
	    this.discountCurveName			= discountCurveName;
	    this.paymentOffsets				= new Curve("", Curve.InterpolationMethod.LINEAR, Curve.ExtrapolationMethod.CONSTANT, Curve.InterpolationEntity.VALUE);
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
		ForwardCurve forwardCurve = new ForwardCurve(name, InterpolationEntityForward.FORWARD, null);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			forwardCurve.addForward(null, times[timeIndex], paymentOffset, givenForwards[timeIndex]);
		}
		
		return forwardCurve;
	}

	/**
	 * Create a forward curve from given times and discount factors.
	 * 
	 * The forward curve will have times.length-1 fixing times from times[0] to times[times.length-2]
	 * where the forwards are calculated via 
	 * <code>
	 * 			forward[timeIndex] = (givenDiscountFactors[timeIndex]/givenDiscountFactors[timeIndex+1]-1.0) / (times[timeIndex+1] - times[timeIndex]);
	 * </code>
	 * Note: If time[0] > 0, then the discount factor 1.0 will inserted at time 0.0
	 * 
     * @param name The name of this curve.
	 * @param times A vector of given time points.
	 * @param givenDiscountFactors A vector of given discount factors (corresponding to the given time points).
	 * @return A new ForwardCurve object.
	 */
	public static ForwardCurve createForwardCurveFromDiscountFactors(String name, double[] times, double[] givenDiscountFactors, double paymentOffset) {
		ForwardCurve forwardCurve = new ForwardCurve(name, InterpolationEntityForward.FORWARD, null);

		if(times.length == 0) throw new IllegalArgumentException("Vector of times must not be empty.");

		if(times[0] > 0) {
			// Add first forward
			double forward = (1.0/givenDiscountFactors[0]-1.0) / (times[0] - 0);
			forwardCurve.addForward(null, 0.0, paymentOffset, forward);
		}
		
		for(int timeIndex=0; timeIndex<times.length-1;timeIndex++) {
			double forward = (givenDiscountFactors[timeIndex]/givenDiscountFactors[timeIndex+1]-1.0) / (times[timeIndex+1] - times[timeIndex]);
			forwardCurve.addForward(null, times[timeIndex], paymentOffset, forward);
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
     * @param paymentOffsets Time between fixing and payment.
	 * @return A new ForwardCurve object.
	 */
	public static ForwardCurve createForwardCurveFromForwards(String name, double[] times, double[] givenForwards, AnalyticModelInterface model, String discountCurveName, double paymentOffset) {
		ForwardCurve forwardCurve = new ForwardCurve(name, InterpolationEntityForward.FORWARD, discountCurveName);

		for(int timeIndex=0; timeIndex<times.length;timeIndex++) {
			forwardCurve.addForward(model, times[timeIndex], paymentOffset, givenForwards[timeIndex]);
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
		return this.getForward(model, fixingTime, getPaymentOffset(fixingTime));
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.ForwardCurveInterface#getForward(double)
	 */
	@Override
    public double getForward(AnalyticModelInterface model, double fixingTime, double paymentOffset)
	{
		if(paymentOffset != this.getPaymentOffset(fixingTime)) {
//			Logger.getLogger("net.finmath").warning("Requesting forward with paymentOffsets not aggreeing with original calibration. Requested: " + paymentOffsets +". Calibrated: " + getPaymentOffset(fixingTime) + ".");
		}

		double interpolationEntityForwardValue = this.getValue(model, fixingTime);
		switch(interpolationEntityForward) {
		case FORWARD:
		default:
			return interpolationEntityForwardValue;
		case FORWARD_TIMES_DISCOUNTFACTOR:
			return interpolationEntityForwardValue / model.getDiscountCurve(discountCurveName).getValue(model, fixingTime+paymentOffset);
		case ZERO:
			return (Math.exp(interpolationEntityForwardValue * paymentOffset) - 1.0) / paymentOffset;
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
	
	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface#getDiscountCurve()
	 */
	@Override
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	
	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface#getPaymentOffsets()
	 */
	@Override
	public CurveInterface getPaymentOffsets() {
		return paymentOffsets;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.ForwardCurveInterface#getPaymentOffset()
	 */
	@Override
	public double getPaymentOffset(double fixingTime) {
		return paymentOffsets.getValue(fixingTime);
	}

	
	protected void addForward(AnalyticModelInterface model, double fixingTime, double paymentOffset, double forward) {
		double interpolationEntityForwardValue;
		switch(interpolationEntityForward) {
		case FORWARD:
		default:
			interpolationEntityForwardValue = forward;
			break;
		case FORWARD_TIMES_DISCOUNTFACTOR:
			interpolationEntityForwardValue = forward * model.getDiscountCurve(discountCurveName).getValue(model, fixingTime+paymentOffset);
			break;
		case ZERO:
			interpolationEntityForwardValue = Math.log(1.0 + forward * paymentOffset) / paymentOffset;
			break;
		}
		this.addPoint(fixingTime, interpolationEntityForwardValue);
		this.paymentOffsets.addPoint(fixingTime, paymentOffset);
	}

	@Override
	public String toString() {
		return "ForwardCurve [interpolationEntityForward="
				+ interpolationEntityForward + ", discountCurveName="
				+ discountCurveName + ", paymentOffsets=" + paymentOffsets + "]";
	}
}
