/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 22.06.2014
 */

package net.finmath.marketdata.products;

import java.util.ArrayList;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.CapletVolatilities;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleInterface;

/**
 * Implements the valuation of a cap via an analytic model,
 * i.e. the specification of a forward curve, discount curve and volatility surface.
 * 
 * @author Christian Fries
 */
public class Cap extends AbstractAnalyticProduct {

	private final ScheduleInterface			schedule;
	private final String					forwardCurveName;
	private final double					strike;
	private final boolean					isStrikeMoneyness;
	private final String					discountCurveName;
	private final String					volatiltiySufaceName;

	public Cap(ScheduleInterface schedule, String forwardCurveName, double strike, boolean isStrikeMoneyness, String discountCurveName, String volatiltiySufaceName) {
		super();
		this.schedule = schedule;
		this.forwardCurveName = forwardCurveName;
		this.strike = strike;
		this.isStrikeMoneyness = isStrikeMoneyness;
		this.discountCurveName = discountCurveName;
		this.volatiltiySufaceName = volatiltiySufaceName;
	}


	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {
		ForwardCurveInterface	forwardCurve	= model.getForwardCurve(forwardCurveName);
		DiscountCurveInterface	discountCurve	= model.getDiscountCurve(discountCurveName);
		
		DiscountCurveInterface	discountCurveForForward = null;
		if(forwardCurve == null && forwardCurveName != null && forwardCurveName.length() > 0) {
			// User might like to get forward from discount curve.
			discountCurveForForward	= model.getDiscountCurve(forwardCurveName);
			
			if(discountCurveForForward == null) {
				// User specified a name for the forward curve, but no curve was found.
				throw new IllegalArgumentException("No curve of the name " + forwardCurveName + " was found in the model.");
			}
		}

		double value = 0.0;
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods(); periodIndex++) {
			double fixingDate	= schedule.getFixing(periodIndex);
			double paymentDate	= schedule.getPayment(periodIndex);
			double periodLength	= schedule.getPeriodLength(periodIndex);
			
			/*
			 * We do not count empty periods.
			 * Since empty periods are an indication for a ill-specified product,
			 * it might be reasonable to throw an illegal argument exception instead.
			 */
			if(periodLength == 0) continue;

			double forward = 0.0;
			if(forwardCurve != null) {
				forward			+= forwardCurve.getForward(model, fixingDate, paymentDate-fixingDate);
			}
			else if(discountCurveForForward != null) {
				/*
				 * Classical single curve case: using a discount curve as a forward curve.
				 * This is only implemented for demonstration purposes (an exception would also be appropriate :-)
				 */
				if(fixingDate != paymentDate)
					forward			+= (discountCurveForForward.getDiscountFactor(fixingDate) / discountCurveForForward.getDiscountFactor(paymentDate) - 1.0) / (paymentDate-fixingDate);
			}
			double discountFactor	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;

			double effektiveStrike = strike;
			if(isStrikeMoneyness)	effektiveStrike += this.getATMForward(model, true);

			VolatilitySurfaceInterface volatilitySurface	= model.getVolatilitySurface(volatiltiySufaceName);
			double volatility = volatilitySurface.getValue(model, fixingDate, effektiveStrike, VolatilitySurfaceInterface.QuotingConvention.VOLATILITYNORMAL);
			
			value += AnalyticFormulas.bachelierOptionValue(forward, volatility, fixingDate, effektiveStrike, discountFactor);
		}
		return value / discountCurve.getDiscountFactor(model, evaluationTime);
	}

	/**
	 * Return the ATM forward for this cap.
	 * The ATM forward is the fixed rate K such that the value of the payoffs
	 * \( F(t_i) - K \)
	 * is zero, where \( F(t_i) \) is the ATM forward of the i-th caplet.
	 * Note however that the is a convention to determine the ATM forward of a cap
	 * from the payoffs excluding the first one. The reason here is that for non-forward starting
	 * cap, the first period is already fixed, i.e. it has no vega.
	 * 
	 * @param model The model to retrieve the forward curve from (by name).
	 * @param isFirstPeriodIncluded If true, the forward will be determined by considering the periods after removal of the first periods (except, if the Cap consists only of 1 period).
	 * @return The ATM forward of this cap.
	 */
	double getATMForward(AnalyticModelInterface model, boolean isFirstPeriodIncluded) {
		@SuppressWarnings("unchecked")
		ArrayList<Period> periods = (ArrayList<Period>)schedule.getPeriods().clone();

		if(periods.size() > 1 && !isFirstPeriodIncluded) periods.remove(0);

		Schedule remainderSchedule = new Schedule(schedule.getReferenceDate(), periods, schedule.getDaycountconvention());

		SwapLeg floatLeg = new SwapLeg(remainderSchedule, forwardCurveName, 0.0, discountCurveName, false);
		SwapLeg annuityLeg = new SwapLeg(remainderSchedule, null, 1.0, discountCurveName, false);

		return floatLeg.getValue(model) / annuityLeg.getValue(model);
	}
	
	public double getImpliedVolatility(double evaluationTime, AnalyticModelInterface model, VolatilitySurfaceInterface.QuotingConvention quotingConvention, double value) {
		double lowerBound = model.getVolatilitySurface(volatiltiySufaceName).getMinimum();
		double upperBound = model.getVolatilitySurface(volatiltiySufaceName).getMaximum();
		GoldenSectionSearch solver = new GoldenSectionSearch(lowerBound, upperBound);
		
		while(!solver.isDone()) {
			double volatility = solver.getNextPoint();
			double[] maturities = { 1.0 };
			double[] strikes = { 0.0 };
			double[] volatilities = { volatility };
			VolatilitySurfaceInterface flatSurface = new CapletVolatilities(model.getVolatilitySurface(volatiltiySufaceName).getName(), model.getVolatilitySurface(volatiltiySufaceName).getReferenceDate(), model.getForwardCurve(forwardCurveName), maturities, strikes, volatilities, quotingConvention, model.getDiscountCurve(discountCurveName));
			AnalyticModel flatModel = new AnalyticModel();
			flatModel.setCurve(model.getForwardCurve(forwardCurveName));
			flatModel.setCurve(model.getDiscountCurve(discountCurveName));
			flatModel.setVolatilitySurface(flatSurface);
			double flatModelValue = this.getValue(evaluationTime, flatModel);
			double error = value-flatModelValue;
			solver.setValue(error*error);
		}
		return solver.getBestPoint();
	}

	@Override
	public String toString() {
		return "Cap [schedule=" + schedule + ", forwardCurveName="
				+ forwardCurveName + ", strike=" + strike
				+ ", discountCurveName=" + discountCurveName
				+ ", volatiltiySufaceName=" + volatiltiySufaceName + "]";
	}
}
