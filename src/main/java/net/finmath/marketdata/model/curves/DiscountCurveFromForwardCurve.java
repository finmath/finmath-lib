/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * A discount curve derived from a given forward curve.
 * The discount factors df(t) are defined at t = k * d for integers k
 * via df(t+d) = df(t) / (1 + f(t) * d) and
 * for t = k * d and 0 < r < d
 * via df(t+r) = df(t) / (1 + f(t) * r)
 * where d is a given the payment offset and f(t) is the forward curve.
 * 
 * <i>Note that a special interpolation is performed for in-between points.
 * Hence, creating a {@link ForwardCurveFromDiscountCurve} and from it
 * a DiscountCurveFromForwardCurve will not recover the original curve
 * since interpolation points may be lost.
 * 
 * @author Christian Fries
 */
public class DiscountCurveFromForwardCurve extends AbstractCurve implements Serializable, DiscountCurveInterface {

    private static final long serialVersionUID = -4126228588123963885L;

    private String					forwardCurveName;
    private ForwardCurveInterface	forwardCurve;

	/**
     * Create a discount curve using a given forward curve.
     * The discount factors df(t) are defined at t = k * d for integers k
     * via df(t+d) = df(t) / (1 + f(t) * d) and
     * for t = k * d and 0 < r < d
     * via df(t+r) = df(t) / (1 + f(t) * r)
     * where d is a given the payment offset and f(t) is the forward curve.
     * 
     * @param forwardCurveName The name of the forward curve used for calculation of the discount factors.
     */
    public DiscountCurveFromForwardCurve(String forwardCurveName) {
		super("DiscountCurveFromForwardCurve" + forwardCurveName + ")");

		this.forwardCurveName	= forwardCurveName;
    }

	/**
     * Create a discount curve using a given forward curve.
     * The discount factors df(t) are defined at t = k * d for integers k
     * via df(t+d) = df(t) / (1 + f(t) * d) and
     * for t = k * d and 0 < r < d
     * via df(t+r) = df(t) / (1 + f(t) * r)
     * where d is a given the payment offset and f(t) is the forward curve.
     * 
     * @param forwardCurve The forward curve used for calculation of the discount factors.
     */
    public DiscountCurveFromForwardCurve(ForwardCurveInterface forwardCurve) {
		super("DiscountCurveFromForwardCurve" + forwardCurve.getName() + ")");

		this.forwardCurve	= forwardCurve;
    }
   
	/* (non-Javadoc)
	 * @see net.finmath.marketdata.DiscountCurveInterface#getDiscountFactor(double)
	 */
	@Override
	public double getDiscountFactor(double maturity) {
		return getDiscountFactor(null, maturity);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.DiscountCurveInterface#getDiscountFactor(double)
	 */
	@Override
	public double getDiscountFactor(AnalyticModelInterface model, double maturity) {
	    ForwardCurveInterface	forwardCurve;
		if(this.forwardCurve != null)	forwardCurve = this.forwardCurve;
		else							forwardCurve = model.getForwardCurve(forwardCurveName);

		double	time			= 0;
		double	discountFactor	= 1.0;
		double paymentOffset = 0;
		while(time < maturity) {
			paymentOffset	= forwardCurve.getPaymentOffset(time);
			if(paymentOffset <= 0) throw new RuntimeException("Payment offset non-positive.");
			discountFactor /= 1.0 + forwardCurve.getForward(model, time) * Math.min(paymentOffset, maturity-time);
			time += paymentOffset;
		}
		
		return discountFactor;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		return getDiscountFactor(model, time);
	}


	@Override
	public double[] getParameter() {
		return null;
	}

	@Override
	public void setParameter(double[] parameter) {
	}
}
