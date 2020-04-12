/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.12.2012
 */
package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;

/**
 * Implements the valuation of a forward using curves (discount curve, forward curve).
 * The forward value is simply the product of a discount factor and a forward.
 * This is similar to a FRA (a forward rate), except that there is no scaling with a period length.
 *
 * The class can be used to define equity forwards. Here the discount curve can be interpreted
 * as a repo curve.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class Forward extends AbstractAnalyticProduct implements AnalyticProduct {

	private final double						maturity;
	private final double						paymentOffset;
	private final String						forwardCurveName;
	private final double						spread;
	private final String						discountCurveName;

	/**
	 * Creates a forward. The forward has a unit notional of 1.
	 *
	 * @param maturity Maturity, i.e., fixing on the forward curve.
	 * @param paymentOffset Payment offset, i.e. payment is maturity + paymentOffset.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix payment.
	 * @param spread Additional fixed payment (if any).
	 * @param discountCurveName Name of the discount curve for the forward.
	 */
	public Forward(final double maturity, final double paymentOffset, final String forwardCurveName, final double spread, final String discountCurveName) {
		super();
		this.maturity = maturity;
		this.paymentOffset = paymentOffset;
		this.forwardCurveName = forwardCurveName;
		this.spread = spread;
		this.discountCurveName = discountCurveName;
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.products.AnalyticProductInterface#getValue(double, net.finmath.marketdata.model.AnalyticModelInterface)
	 */
	@Override
	public double getValue(final double evaluationTime, final AnalyticModel model) {
		final ForwardCurve	forwardCurve	= model.getForwardCurve(forwardCurveName);
		final DiscountCurve	discountCurve	= model.getDiscountCurve(discountCurveName);

		DiscountCurve	discountCurveForForward = null;
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			// User might like to get forward from discount curve.
			discountCurveForForward	= model.getDiscountCurve(forwardCurveName);

			if(discountCurveForForward == null) {
				// User specified a name for the forward curve, but no curve was found.
				throw new IllegalArgumentException("No curve of the name " + forwardCurveName + " was found in the model.");
			}
		}

		double forward		= spread;
		if(forwardCurve != null) {
			forward			+= forwardCurve.getForward(model, maturity);
		}
		else if(discountCurveForForward != null) {
			forward			+= (discountCurveForForward.getDiscountFactor(maturity) / discountCurveForForward.getDiscountFactor(maturity+paymentOffset) - 1.0) / paymentOffset;
		}

		final double discountFactor	= maturity+paymentOffset > evaluationTime ? discountCurve.getDiscountFactor(model, maturity+paymentOffset) : 0.0;

		return forward * discountFactor / discountCurve.getDiscountFactor(model, evaluationTime);
	}
}
