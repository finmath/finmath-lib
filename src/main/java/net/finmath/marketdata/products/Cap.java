/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.06.2014
 */

package net.finmath.marketdata.products;

import java.io.IOException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.CapletVolatilities;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleInterface;

/**
 * Implements the valuation of a cap via an analytic model,
 * i.e. the specification of a forward curve, discount curve and volatility surface.
 *
 * A cap is a portfolio of Caplets with a common strike, i.e., the strike is the same for all Caplets.
 *
 * The class can value a caplet with a given strike or given moneyness. If moneyness is given,
 * the class calculates the ATM forward. Note that this is done by omitting the first (fixed) period,
 * see {@link #getATMForward(AnalyticModelInterface, boolean)}.
 *
 * Note: A fixing in arrears is not handled correctly since a convexity adjustment is currently not applied.
 *
 * @TODO: Support convexity adjustment if fixing is in arrears.
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

	private final VolatilitySurfaceInterface.QuotingConvention quotingConvention;

	private transient double cachedATMForward = Double.NaN;
	private transient SoftReference<AnalyticModelInterface> cacheStateModel;
	private transient boolean cacheStateIsFirstPeriodIncluded;

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
	 * @param quotingConvention The quoting convention of the value returned by the {@link #getValue}-method.
	 */
	public Cap(ScheduleInterface schedule, String forwardCurveName, double strike, boolean isStrikeMoneyness, String discountCurveName, String volatilitySurfaceName, QuotingConvention quotingConvention) {
		super();
		this.schedule = schedule;
		this.forwardCurveName = forwardCurveName;
		this.strike = strike;
		this.isStrikeMoneyness = isStrikeMoneyness;
		this.discountCurveName = discountCurveName;
		this.volatiltiySufaceName = volatilitySurfaceName;
		this.quotingConvention = quotingConvention;
	}

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
	 */
	public Cap(ScheduleInterface schedule, String forwardCurveName, double strike, boolean isStrikeMoneyness, String discountCurveName, String volatilitySurfaceName) {
		this(schedule, forwardCurveName, strike, isStrikeMoneyness, discountCurveName, volatilitySurfaceName, QuotingConvention.PRICE);
	}

	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {
		if(quotingConvention == QuotingConvention.PRICE) {
			return getValueAsPrice(evaluationTime, model);
		} else {
			return getImpliedVolatility(evaluationTime, model, quotingConvention);
		}
	}

	/**
	 * Returns the value of this product under the given model.
	 *
	 * @param evaluationTime Evaluation time.
	 * @param model The model.
	 * @return Value of this product und the given model.
	 */
	public double getValueAsPrice(double evaluationTime, AnalyticModelInterface model) {
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

			double discountFactor	= paymentDate > evaluationTime ? discountCurve.getDiscountFactor(model, paymentDate) : 0.0;
			double payoffUnit = discountFactor * periodLength;

			double effektiveStrike = strike;
			if(isStrikeMoneyness) {
				effektiveStrike += getATMForward(model, true);
			}

			VolatilitySurfaceInterface volatilitySurface	= model.getVolatilitySurface(volatiltiySufaceName);
			if(volatilitySurface == null) {
				throw new IllegalArgumentException("Volatility surface not found in model: " + volatiltiySufaceName);
			}
			if(volatilitySurface.getQuotingConvention() == QuotingConvention.VOLATILITYLOGNORMAL) {
				double volatility = volatilitySurface.getValue(model, fixingDate, effektiveStrike, VolatilitySurfaceInterface.QuotingConvention.VOLATILITYLOGNORMAL);
				value += AnalyticFormulas.blackScholesGeneralizedOptionValue(forward, volatility, fixingDate, effektiveStrike, payoffUnit);
			}
			else {
				// Default to normal volatility as quoting convention
				double volatility = volatilitySurface.getValue(model, fixingDate, effektiveStrike, VolatilitySurfaceInterface.QuotingConvention.VOLATILITYNORMAL);
				value += AnalyticFormulas.bachelierOptionValue(forward, volatility, fixingDate, effektiveStrike, payoffUnit);
			}
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
	public double getATMForward(AnalyticModelInterface model, boolean isFirstPeriodIncluded) {
		if(!Double.isNaN(cachedATMForward) && cacheStateModel.get() == model && cacheStateIsFirstPeriodIncluded == isFirstPeriodIncluded) {
			return cachedATMForward;
		}

		ScheduleInterface remainderSchedule = schedule;
		if(!isFirstPeriodIncluded) {
			ArrayList<Period> periods = new ArrayList<>();
			periods.addAll(schedule.getPeriods());

			if(periods.size() > 1) {
				periods.remove(0);
			}
			remainderSchedule = new Schedule(schedule.getReferenceDate(), periods, schedule.getDaycountconvention());
		}

		SwapLeg floatLeg = new SwapLeg(remainderSchedule, forwardCurveName, 0.0, discountCurveName, false);
		SwapLeg annuityLeg = new SwapLeg(remainderSchedule, null, 1.0, discountCurveName, false);

		cachedATMForward = floatLeg.getValue(model) / annuityLeg.getValue(model);
		cacheStateModel = new SoftReference<>(model);
		cacheStateIsFirstPeriodIncluded = isFirstPeriodIncluded;

		return cachedATMForward;
	}

	/**
	 * Returns the value of this cap in terms of an implied volatility (of a flat caplet surface).
	 *
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param model The model under which the product is valued.
	 * @param quotingConvention The quoting convention requested for the return value.
	 * @return The value of the product using the given model in terms of a implied volatility.
	 */
	public double getImpliedVolatility(double evaluationTime, AnalyticModelInterface model, VolatilitySurfaceInterface.QuotingConvention quotingConvention) {
		double lowerBound = Double.MAX_VALUE;
		double upperBound = -Double.MAX_VALUE;
		for(int periodIndex=0; periodIndex<schedule.getNumberOfPeriods(); periodIndex++) {
			double fixingDate	= schedule.getFixing(periodIndex);
			double periodLength	= schedule.getPeriodLength(periodIndex);

			if(periodLength == 0) {
				continue;
			}

			double effektiveStrike = strike;
			if(isStrikeMoneyness) {
				effektiveStrike += getATMForward(model, true);
			}

			VolatilitySurfaceInterface volatilitySurface	= model.getVolatilitySurface(volatiltiySufaceName);
			double volatility = volatilitySurface.getValue(model, fixingDate, effektiveStrike, quotingConvention);
			lowerBound = Math.min(volatility, lowerBound);
			upperBound = Math.max(volatility, upperBound);
		}

		double value = getValueAsPrice(evaluationTime, model);

		int		maxIterations	= 100;
		double	maxAccuracy		= 0.0;
		GoldenSectionSearch solver = new GoldenSectionSearch(lowerBound, upperBound);
		while(solver.getAccuracy() > maxAccuracy && !solver.isDone() && solver.getNumberOfIterations() < maxIterations) {
			double volatility = solver.getNextPoint();
			double[] maturities		= { 1.0 };
			double[] strikes		= { 0.0 };
			double[] volatilities	= { volatility };
			VolatilitySurfaceInterface flatSurface = new CapletVolatilities(model.getVolatilitySurface(volatiltiySufaceName).getName(), model.getVolatilitySurface(volatiltiySufaceName).getReferenceDate(), model.getForwardCurve(forwardCurveName), maturities, strikes, volatilities, quotingConvention, model.getDiscountCurve(discountCurveName));
			AnalyticModelInterface flatModel = model.clone();
			flatModel = flatModel.addVolatilitySurfaces(flatSurface);
			double flatModelValue = this.getValueAsPrice(evaluationTime, flatModel);
			double error = value-flatModelValue;
			solver.setValue(error*error);
		}
		return solver.getBestPoint();
	}

	/**
	 * Returns the name of the forward curve references by this cap.
	 *
	 * @return the forward curve name.
	 */
	public String getForwardCurveName() {
		return forwardCurveName;
	}

	/**
	 * Returns the strike of this caplet.
	 *
	 * @return the strike
	 */
	public double getStrike() {
		return strike;
	}

	/**
	 * Returns the name of the discount curve referenced by this cap.
	 *
	 * @return the discount curve name
	 */
	public String getDiscountCurveName() {
		return discountCurveName;
	}

	@Override
	public String toString() {
		return "Cap [schedule=" + schedule + ", forwardCurveName="
				+ forwardCurveName + ", strike=" + strike
				+ ", discountCurveName=" + discountCurveName
				+ ", volatiltiySufaceName=" + volatiltiySufaceName + "]";
	}

	private void readObject(java.io.ObjectInputStream in) throws ClassNotFoundException, IOException {
		in.defaultReadObject();
		// initialization of transients
		cachedATMForward = Double.NaN;
	}
}

