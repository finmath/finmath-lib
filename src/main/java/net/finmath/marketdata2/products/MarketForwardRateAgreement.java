/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */
package net.finmath.marketdata2.products;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements the valuation of a market forward rate agreement using curves
 * (discount curve, forward curve).
 *
 * The value of the forward rate agreement at its maturity time <i>t</i> is
 * <br>
 * <i>
 * (F(t)-K) / (1 + F(t) * dcf(periodStart,periodEnd))
 * </i>
 * where <i>F(t)</i> is the forward evaluated at maturity and
 * dcf(periodStart,periodEnd) is a given paymentOffset.
 *
 * The value of the forward rate agreement returned for an earlier time is
 * the above payoff multiplied with the corresponding discount factor curve.
 * Note that this valuation ignores a possible convexity adjustment between
 * the forward and the discount factors since the above formula is not a
 * linear function of <i>F</i>. Put differently, if this product is used
 * to calibrate a forward curve to a forward rate agreement, then the
 * calibrated forward curve will include the convexity adjustment.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class MarketForwardRateAgreement extends AbstractAnalyticProduct implements AnalyticProduct {

	private final double						maturity;
	private final double						paymentOffset;
	private final String						forwardCurveName;
	private final double						spread;
	private final String						discountCurveName;

	/**
	 * Creates a market forward rate agreement.
	 *
	 * @param maturity Maturity, i.e., fixing on the forward curve.
	 * @param paymentOffset Payment offset.
	 * @param forwardCurveName Name of the forward curve, leave empty if this is a fix payment.
	 * @param spread Additional fixed payment (if any).
	 * @param discountCurveName Name of the discount curve for the forward.
	 */
	public MarketForwardRateAgreement(final double maturity, final double paymentOffset, final String forwardCurveName, final double spread, final String discountCurveName) {
		super();
		this.maturity = maturity;
		this.paymentOffset = paymentOffset;
		this.forwardCurveName = forwardCurveName;
		this.spread = spread;
		this.discountCurveName = discountCurveName;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AnalyticModel model) {
		final ForwardCurveInterface	forwardCurve	= model.getForwardCurve(forwardCurveName);
		final DiscountCurveInterface	discountCurve	= model.getDiscountCurve(discountCurveName);

		DiscountCurveInterface	discountCurveForForward = null;
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			// User might like to get forward from discount curve.
			discountCurveForForward	= model.getDiscountCurve(forwardCurveName);

			if(discountCurveForForward == null) {
				// User specified a name for the forward curve, but no curve was found.
				throw new IllegalArgumentException("No curve of the name " + forwardCurveName + " was found in the model.");
			}
		}

		RandomVariable forward		= model.getRandomVariableForConstant(-spread);
		if(forwardCurve != null) {
			forward = forward.add(forwardCurve.getForward(model, maturity));
		}
		else if(discountCurveForForward != null) {
			forward = forward.add(discountCurveForForward.getDiscountFactor(maturity).div(discountCurveForForward.getDiscountFactor(maturity+paymentOffset)).sub(1.0).div(paymentOffset));
		}

		final RandomVariable payoff = forward.discount(forward, paymentOffset);

		final RandomVariable discountFactor	= maturity > evaluationTime ? discountCurve.getDiscountFactor(model, maturity) : model.getRandomVariableForConstant(0.0);

		return payoff.mult(discountFactor).div(discountCurve.getDiscountFactor(model, evaluationTime));
	}
}
