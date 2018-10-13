package net.finmath.analytic.products;

import net.finmath.analytic.model.AnalyticModelInterface;
import net.finmath.analytic.model.curves.DiscountCurveInterface;
import net.finmath.analytic.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
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
 * @version 1.0
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
	public ForwardRateAgreement(ScheduleInterface schedule,  double spread, String forwardCurveName, String discountCurveName) {
		this(schedule, spread, forwardCurveName, discountCurveName, true /* isPayer */);
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, AnalyticModelInterface model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		DiscountCurveInterface discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve==null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		ForwardCurveInterface forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve==null && forwardCurveName!=null && forwardCurveName.length()>0) {
			throw new IllegalArgumentException("No forward curve with name '" + forwardCurveName + "' was found in the model:\n" + model.toString());
		}

		double fixingDate = schedule.getFixing(0);
		double paymentDate = schedule.getPayment(0);
		double periodLength = schedule.getPeriodLength(0);

		RandomVariableInterface forward = new RandomVariable(0.0);
		if(forwardCurve != null) {
			forward = forwardCurve.getForward(model, fixingDate, paymentDate-fixingDate);
		}

		// Valuation of the market FRA for payer and receiver direction, neglecting convexity adjustment
		double notional = isPayer ? 1.0 : -1.0;
		RandomVariableInterface discountFactorFixingDate = fixingDate > evaluationTime ? discountCurve.getDiscountFactor(model, fixingDate) : new RandomVariable(0.0);
		return forward.sub(spread).div(forward.mult(periodLength).add(1.0)).mult(discountFactorFixingDate).mult(periodLength*notional);
	}

	/**
	 * Return the par FRA rate for a given curve.
	 *
	 * @param model A given model.
	 * @return The par FRA rate.
	 */
	public RandomVariableInterface getRate(AnalyticModelInterface model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		ForwardCurveInterface forwardCurve = model.getForwardCurve(forwardCurveName);
		if(forwardCurve==null) {
			throw new IllegalArgumentException("No forward curve of name '" + forwardCurveName + "' found in given model:\n" + model.toString());
		}

		double fixingDate = schedule.getFixing(0);
		return forwardCurve.getForward(model,fixingDate);
	}
}
