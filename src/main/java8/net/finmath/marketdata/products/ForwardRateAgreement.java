package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.time.Schedule;

/**
 * Implements the valuation of a FRA in multi-curve setting.
 *
 * According to Ametrano/Bianchetti (2013) p.22, the size of the convexity adjustment
 * between market FRA and textbook FRA is neglegible. This class can thus be used for the valuation of the market FRA.
 *
 * market conventions (see Ametrano/Bianchetti (2013), p.22):
 * spot offset: 2BD
 * day count convention: act/360
 * fixing date offset: 2BD
 *
 * @author Rebecca Declara
 * @author Christian Fries
 * @version 1.0
 */
public class ForwardRateAgreement extends AbstractAnalyticProduct implements AnalyticProduct {

	private final Schedule					schedule;
	private final String						forwardCurveName;
	private final double						spread;
	private final String						discountCurveName;
	private final boolean						isPayer;

	/**
	 * Creates a FRA. The FRA has a unit notional of 1.
	 *
	 * @param schedule The schedule (provides fixing and periods length).
	 * @param spread The market FRA rate
	 * @param forwardCurveName Name of the forward curve
	 * @param discountCurveName Name of the discount curve (possibly multi curve setting).
	 * @param isPayer If true, the fra pays fix, i.e., the payoff is forward - spread. Otherwise it is spread - forward.
	 */
	public ForwardRateAgreement(final Schedule schedule,  final double spread, final String forwardCurveName, final String discountCurveName, final boolean isPayer) {
		super();
		this.schedule = schedule;
		this.forwardCurveName = forwardCurveName;
		this.spread = spread;
		this.discountCurveName = discountCurveName;
		this.isPayer = isPayer;

		// Check schedule
		if(schedule.getNumberOfPeriods() > 1) {
			throw new IllegalArgumentException("Number of periods has to be 1: Change frequency to 'tenor'!");
		}
	}

	/**
	 * Creates a payer FRA. The FRA has a unit notional of 1.
	 *
	 * @param schedule The schedule (provides fixing and periods length).
	 * @param spread The market FRA rate
	 * @param forwardCurveName Name of the forward curve
	 * @param discountCurveName Name of the discount curve (possibly multi curve setting).
	 */
	public ForwardRateAgreement(final Schedule schedule,  final double spread, final String forwardCurveName, final String discountCurveName) {
		this(schedule, spread, forwardCurveName, discountCurveName, true /* isPayer */);
	}

	@Override
	public double getValue(final double evaluationTime, final AnalyticModel model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final DiscountCurve discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve==null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		final ForwardCurve forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve==null && forwardCurveName!=null && forwardCurveName.length()>0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		final double fixingDate = schedule.getFixing(0);
		final double paymentDate = schedule.getPayment(0);
		final double periodLength = schedule.getPeriodLength(0);

		double forward = 0.0;
		if(forwardCurve != null) {
			forward += forwardCurve.getForward(model, fixingDate, paymentDate-fixingDate);
		}

		// Valuation of the market FRA for payer and receiver direction, neglecting convexity adjustment
		final double notional = isPayer ? 1.0 : -1.0;
		final double discountFactorFixingDate = fixingDate > evaluationTime ? discountCurve.getDiscountFactor(model, fixingDate) : 0.0;
		return notional * (forward - spread) / (1.0 + forward * periodLength) * discountFactorFixingDate * periodLength;
	}

	/**
	 * Return the par FRA rate for a given curve.
	 *
	 * @param model A given model.
	 * @return The par FRA rate.
	 */
	public double getRate(final AnalyticModel model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final ForwardCurve forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve==null) {
			throw new IllegalArgumentException("No forward curve of name '" + forwardCurveName + "' found in given model:\n" + model.toString());
		}

		final double fixingDate = schedule.getFixing(0);
		return forwardCurve.getForward(model,fixingDate);
	}
}
