package net.finmath.marketdata.model.volatility.caplet;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.products.Cap;
import net.finmath.time.Schedule;

/**
 * Implements the valuation of a cap via an analytic model,
 * i.e. the specification of a forward curve, discount curve and volatility surface.
 *
 * A cap is a portfolio of Caplets with a common strike, i.e., the strike is the same for all Caplets.
 *
 * The class can value a caplet with a given strike or given moneyness. If moneyness is given,
 * the class calculates the ATM forward. Note that this is done by omitting the first (fixed) period,
 * see {@link #getATMForward(AnalyticModel, boolean)}.
 *
 * Note: A fixing in arrears is not handled correctly since a convexity adjustment is currently not applied.
 *
 * @TODO Support convexity adjustment if fixing is in arrears.
 * @TODO Fix JavaDoc for shift.
 *
 * @author Christian Fries
 * @version 1.0
 */

public class CapShiftedVol extends Cap {

	private final double shift;
	private final Schedule schedule;
	private final boolean isStrikeMoneyness;
	private final String volatilitySurfaceName;

	/**
	 * Create a Caplet with a given schedule, strike on a given forward curve (by name)
	 * with a given discount curve and volatility surface (by name).
	 *
	 * The valuation is performed using analytic valuation formulas for the underlying caplets.
	 *
	 * @param schedule A given payment schedule, i.e., a collection of <code>Period</code>s with fixings, payments and period length.
	 * @param forwardCurveName The forward curve to be used for the forward of the index.
	 * @param strike The given strike (or moneyness).
	 * @param isStrikeMoneyness If true, then the strike argument is interpreted as moneyness, i.e. we calculate an ATM forward from the schedule.
	 * @param discountCurveName The discount curve to be used for discounting.
	 * @param volatilitySurfaceName The volatility surface to be used.
	 * @param shift The shift of the volatility surface.
	 */
	public CapShiftedVol(final Schedule schedule, final String forwardCurveName, final double strike, final boolean isStrikeMoneyness,
			final String discountCurveName, final String volatilitySurfaceName, final double shift) {
		super(schedule, forwardCurveName, strike, isStrikeMoneyness, discountCurveName, volatilitySurfaceName);
		this.shift = shift;
		this.schedule = schedule;
		this.isStrikeMoneyness = isStrikeMoneyness;
		this.volatilitySurfaceName = volatilitySurfaceName;
	}

	/**
	 * Returns the value of this product under the given model.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param model The model.
	 * @return Value of this product under the given model.
	 */
	@Override
	public double getValueAsPrice(final double evaluationTime, final AnalyticModel model) {
		final ForwardCurve	forwardCurve	= model.getForwardCurve(super.getForwardCurveName());
		final DiscountCurve	discountCurve	= model.getDiscountCurve(super.getDiscountCurveName());

		DiscountCurve	discountCurveForForward = null;
		if(forwardCurve == null && super.getForwardCurveName() != null && super.getForwardCurveName().length() > 0) {
			// User might like to get forward from discount curve.
			discountCurveForForward	= model.getDiscountCurve(super.getForwardCurveName());

			if(discountCurveForForward == null) {
				// User specified a name for the forward curve, but no curve was found.
				throw new IllegalArgumentException("No curve of the name " + super.getForwardCurveName() + " was found in the model.");
			}
		}

		double value = 0.0;
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods(); periodIndex++) {
			final double fixingDate	= schedule.getFixing(periodIndex);
			final double paymentDate	= schedule.getPayment(periodIndex);
			final double periodLength	= schedule.getPeriodLength(periodIndex);

			/*
			 * We do not count empty periods.
			 * Since empty periods are an indication for a ill-specified product,
			 * it might be reasonable to throw an illegal argument exception instead.
			 */
			if(periodLength == 0) {
				continue;
			}

			double forward = 0.0;
			if(forwardCurve != null) {
				forward			+= forwardCurve.getForward(model, fixingDate, paymentDate-fixingDate);
			}
			else if(discountCurveForForward != null) {
				/*
				 * Classical single curve case: using a discount curve as a forward curve.
				 * This is only implemented for demonstration purposes (an exception would also be appropriate :-)
				 */
				if(fixingDate != paymentDate) {
					forward			+= (discountCurveForForward.getDiscountFactor(fixingDate) / discountCurveForForward.getDiscountFactor(paymentDate) - 1.0) / (paymentDate-fixingDate);
				}
			}

			final double discountFactor	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;
			final double payoffUnit = discountFactor * periodLength;

			double effektiveStrike = super.getStrike();
			if(isStrikeMoneyness) {
				effektiveStrike += getATMForward(model, true);
			}

			final VolatilitySurface volatilitySurface	= model.getVolatilitySurface(volatilitySurfaceName);
			if(volatilitySurface == null) {
				throw new IllegalArgumentException("Volatility surface not found in model: " + volatilitySurfaceName);
			}
			if(volatilitySurface.getQuotingConvention() == QuotingConvention.VOLATILITYLOGNORMAL) {
				final double volatility = volatilitySurface.getValue(model, fixingDate, effektiveStrike, VolatilitySurface.QuotingConvention.VOLATILITYLOGNORMAL);
				if(fixingDate >= (paymentDate - fixingDate)*0.5 && volatility != 0.0) {
					value += AnalyticFormulas.blackScholesGeneralizedOptionValue(forward + shift, volatility, fixingDate, effektiveStrike + shift, payoffUnit);
				}
			}
			else {
				// Default to normal volatility as quoting convention
				final double volatility = volatilitySurface.getValue(model, fixingDate, effektiveStrike, VolatilitySurface.QuotingConvention.VOLATILITYNORMAL);
				if (fixingDate >= (paymentDate - fixingDate)*0.5 && volatility != 0) {
					value += AnalyticFormulas.bachelierOptionValue(forward + shift, volatility, fixingDate, effektiveStrike + shift, payoffUnit);
				}
			}
		}

		return value / discountCurve.getDiscountFactor(model, evaluationTime);
	}

}
