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
 * 
 * The discount factors <i>df(t)</i> are defined at <i>t = k * d</i>
 * for integers <i>k</i> via
 * <i>df(t+d) = df(t) / (1 + f(t) * d)</i> and
 * for <i>t = k * d</i> and <i>0 &lt; r &lt; d</i>
 * via <i>df(t+r) = df(t) / (1 + f(t) * r)</i>
 * where <i>d</i> is a given the payment offset and <i>f(t)</i> is the forward curve.
 * 
 * <p>
 * <i>Note that a special interpolation is performed for in-between points.
 * Hence, creating a {@link ForwardCurveFromDiscountCurve} and from it
 * a DiscountCurveFromForwardCurve will not recover the original curve
 * since interpolation points may be lost.
 * </i>
 * </p>
 * 
 * @author Christian Fries
 */
public class DiscountCurveFromForwardCurve extends AbstractCurve implements Serializable, DiscountCurveInterface {

	private static final long serialVersionUID = -4126228588123963885L;

	private String					forwardCurveName;
	private ForwardCurveInterface	forwardCurve;

	private final double			timeScaling;
	
	/**
	 * Create a discount curve using a given forward curve.
	 * The discount factors df(t) are defined at t = k * d for integers k
	 * via df(t+d) = df(t) / (1 + f(t) * d) and
	 * for t = k * d and 0 &lt; r &lt; d
	 * via df(t+r) = df(t) / (1 + f(t) * r)
	 * where d is a given the payment offset and f(t) is the forward curve.
	 * 
	 * @param forwardCurveName The name of the forward curve used for calculation of the discount factors.
	 * @param periodLengthTimeScaling A scaling factor applied to d, adjusting for the internal double time to the period length daycount fraction (note that this may only be an approximate solution to capture daycount effects).
	 */
	public DiscountCurveFromForwardCurve(String forwardCurveName, double periodLengthTimeScaling) {
		super("DiscountCurveFromForwardCurve(" + forwardCurveName + ")", null);

		this.forwardCurveName	= forwardCurveName;
		this.timeScaling		= periodLengthTimeScaling;
	}

	/**
	 * Create a discount curve using a given forward curve.
	 * The discount factors df(t) are defined at t = k * d for integers k
	 * via df(t+d) = df(t) / (1 + f(t) * d) and
	 * for t = k * d and 0 &lt; r &lt; d
	 * via df(t+r) = df(t) / (1 + f(t) * r)
	 * where d is a given the payment offset and f(t) is the forward curve.
	 * 
	 * @param forwardCurve The forward curve used for calculation of the discount factors.
	 * @param periodLengthTimeScaling A scaling factor applied to d, adjusting for the internal double time to the period length daycount fraction (note that this may only be an approximate solution to capture daycount effects).
	 */
	public DiscountCurveFromForwardCurve(ForwardCurveInterface forwardCurve, double periodLengthTimeScaling) {
		super("DiscountCurveFromForwardCurve" + forwardCurve.getName() + ")", null);

		this.forwardCurve	= forwardCurve;
		this.timeScaling	= periodLengthTimeScaling;
	}
   
	/**
	 * Create a discount curve using a given forward curve.
	 * The discount factors df(t) are defined at t = k * d for integers k
	 * via df(t+d) = df(t) / (1 + f(t) * d) and
	 * for t = k * d and 0 &lt; r &lt; d
	 * via df(t+r) = df(t) / (1 + f(t) * r)
	 * where d is a given the payment offset and f(t) is the forward curve.
	 * 
	 * @param forwardCurveName The name of the forward curve used for calculation of the discount factors.
	 */
	public DiscountCurveFromForwardCurve(String forwardCurveName) {
		this(forwardCurveName, 1.0);
	}

	/**
	 * Create a discount curve using a given forward curve.
	 * The discount factors df(t) are defined at t = k * d for integers k
	 * via df(t+d) = df(t) / (1 + f(t) * d) and
	 * for t = k * d and 0 &lt; r &lt; d
	 * via df(t+r) = df(t) / (1 + f(t) * r)
	 * where d is a given the payment offset and f(t) is the forward curve.
	 * 
	 * @param forwardCurve The forward curve used for calculation of the discount factors.
	 */
	public DiscountCurveFromForwardCurve(ForwardCurveInterface forwardCurve) {
		this(forwardCurve, 1.0);
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

		if(forwardCurve == null) throw new IllegalArgumentException("No forward curve given and no forward curve found in the model under the name " + forwardCurveName + ".");

		double	time			= 0;
		double	discountFactor	= 1.0;
		double paymentOffset = 0;
		while(time < maturity) {
			paymentOffset	= forwardCurve.getPaymentOffset(time);
			if(paymentOffset <= 0) throw new RuntimeException("Trying to calculate a discount curve from a forward curve with non-positive payment offset.");
			discountFactor /= 1.0 + forwardCurve.getForward(model, time) * Math.min(paymentOffset, maturity-time) * timeScaling;
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

	@Override
	public CurveBuilderInterface getCloneBuilder() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}
}
