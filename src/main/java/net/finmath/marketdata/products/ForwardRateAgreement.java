package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.time.ScheduleInterface;

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
 */
public class ForwardRateAgreement extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private ScheduleInterface					schedule;
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
	public ForwardRateAgreement(ScheduleInterface schedule,  double spread, String forwardCurveName, String discountCurveName, boolean isPayer) {
		super();
		this.schedule = schedule;
		this.forwardCurveName = forwardCurveName;
		this.spread = spread;
		this.discountCurveName = discountCurveName;
		this.isPayer = isPayer;
	}

	/**
	 * Creates a payer FRA. The FRA has a unit notional of 1.
	 * 
	 * @param schedule The schedule (provides fixing and periods length).
	 * @param spread The market FRA rate
	 * @param forwardCurveName Name of the forward curve
	 * @param discountCurveName Name of the discount curve (possibly multi curve setting).
	 */
	public ForwardRateAgreement(ScheduleInterface schedule,  double spread, String forwardCurveName, String discountCurveName) {
		this(schedule, spread, forwardCurveName, discountCurveName, true /* isPayer */);
	}

	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {	
		ForwardCurveInterface	forwardCurve	= model.getForwardCurve(forwardCurveName);
		DiscountCurveInterface	discountCurve	= model.getDiscountCurve(discountCurveName);

		double fixingDate = schedule.getFixing(0);
		double periodLength = schedule.getPeriodLength(0);
		double paymentDate = schedule.getPeriodEnd(0);

		double forward = 0.0;		
		if(forwardCurve != null) {
			forward = forwardCurve.getForward(model,fixingDate);
		}

		double discountFactorPaymentDate	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;
		double discountFactorFixingDate		= fixingDate > evaluationTime ? discountCurve.getDiscountFactor(model, fixingDate) : 0.0;

		// Valuation of the market FRA for payer and receiver direction, neglecting convexity adjustment
		double notional = isPayer ? 1.0 : -1.0;

		return notional * (forward - spread) / (1.0 + forward * periodLength) * discountFactorFixingDate * periodLength;
	}

	/**
	 * Return the par FRA rate for a given curve.
	 * 
	 * @param model A given model.
	 * @return The par FRA rate.
	 */
	public double getRate(AnalyticModelInterface model) {	
		ForwardCurveInterface	forwardCurve	= model.getForwardCurve(forwardCurveName);
		if(forwardCurve == null) throw new IllegalArgumentException("No forward curve of name '" + forwardCurveName + "' found in given model.");

		double fixingDate = schedule.getFixing(0);
		return forwardCurve.getForward(model,fixingDate);
	}
}


