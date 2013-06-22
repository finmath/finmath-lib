/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.curves;

import java.io.Serializable;

import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * A forward curve derived from a given discount curve.
 * The forward with fixing in t is calculated as ( d(t)/df(t+d)-1 ) / d
 * where d is a given the payment Offset.
 * 
 * @author Christian Fries
 */
public class ForwardCurveFromDiscountCurve extends AbstractCurve implements Serializable, ForwardCurveInterface {

    private static final long serialVersionUID = -4126228588123963885L;

	private final String						discountCurveName;
    private final double						paymentOffset;

    /**
     * Create a forward curve using a given discount curve.
     * The forward with fixing in t is calculated as ( df(t)/df(t+d)-1 ) / p
     * where df denotes the discount factor as a function of maturity and
     * p is a given the payment offset.
     * 
     * @param discountCurveName The discount curve used for calculation of the forward.
     * @param paymentOffset The payment offset.
     */
    public ForwardCurveFromDiscountCurve(String discountCurveName, double paymentOffset) {
    	super("ForwardCurveFromDiscountCurve(" +  discountCurveName + "," + paymentOffset + ")");
	    this.discountCurveName			= discountCurveName;
	    this.paymentOffset				= paymentOffset;
    }

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.ForwardCurveInterface#getForward(double)
	 */
	@Override
    public double getForward(AnalyticModelInterface model, double fixingTime)
	{
		
		return (model.getDiscountCurve(discountCurveName).getDiscountFactor(model, fixingTime) / model.getDiscountCurve(discountCurveName).getDiscountFactor(model, fixingTime+paymentOffset) - 1.0) / paymentOffset;
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

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
	public double getValue(double time) {
		return getValue(null, time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.model.curves.CurveInterface#getValue(double)
	 */
	@Override
	public double getValue(AnalyticModelInterface model, double time) {
		return getForward(model, time);
	}

	@Override
	public double[] getParameter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setParameter(double[] parameter) {
		// TODO Auto-generated method stub
		
	}
}
