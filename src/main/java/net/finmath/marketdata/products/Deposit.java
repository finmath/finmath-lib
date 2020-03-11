package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.time.Schedule;

/**
 * Implements the valuation of the (overnight) deposit (maturity t+1 or t+2). May be used in curve calibration.
 *
 * For definition and convention see Ametrano/Bianchetti (2013). Following the notation there,...
 * <ul>
 * 	<li>for deposit ON set spot offset = 0, start = 0D, maturity = 1D</li>
 *  <li>for deposit TN set spot offset = 1, start = 0D, maturity = 1D</li>
 *  <li>for deposit ST set spot offset = 2, start = 0D, maturity as given.</li>
 * </ul>
 * Date rolling convention: "following" for T &lt; 1M, "modified following, eom" for T &ge; 1
 *
 * The getValue method returns the value df(T) * (1.0+rate*periodLength) - df(t),
 * where t = schedule.getPeriodStart(0), T = schedule.getPayment(0) and df denotes discountCurve.getDiscountFactor.
 *
 * This corresponds to the valuation of an investment of 1 in t, paid back as (1.0+rate*periodLength) in time T.
 *
 * @author Rebecca Declara
 * @author Christian Fries
 * @version 1.0
 */
public class Deposit extends AbstractAnalyticProduct implements AnalyticProduct{

	private final Schedule	schedule;
	private final double				rate;
	private final String				discountCurveName;

	/**
	 * @param schedule The schedule of the deposit consisting of one period, providing start, payment and periodLength.
	 * @param rate The deposit rate.
	 * @param discountCurveName The discount curve name.
	 */
	public Deposit(final Schedule schedule, final double rate, final String discountCurveName) {
		super();
		this.schedule =  schedule;
		this.rate = rate;
		this.discountCurveName = discountCurveName;

		// Check schedule
		if(schedule.getNumberOfPeriods() > 1) {
			throw new IllegalArgumentException("Number of periods has to be 1: Change frequency to 'tenor'!");
		}
	}

	@Override
	public double getValue(final double evaluationTime, final AnalyticModel model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final DiscountCurve discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		final double maturity = schedule.getPayment(0);

		if (evaluationTime > maturity) {
			return 0; // after maturity the contract is worth nothing
		}

		final double payoutDate	= schedule.getPeriodStart(0);
		final double periodLength = schedule.getPeriodLength(0);
		final double discountFactor = discountCurve.getDiscountFactor(model, maturity);
		final double discountFactorPayout = discountCurve.getDiscountFactor(model, payoutDate);

		if (evaluationTime > payoutDate) {
			return discountFactor * (1.0 + rate * periodLength);
		}
		else {
			return discountFactor * (1.0 + rate * periodLength) - discountFactorPayout;
		}
	}

	/**
	 * Return the deposit rate implied by the given model's curve.
	 *
	 * @param model The given model containing the curve of name <code>discountCurveName</code>.
	 * @return The value of the deposit rate implied by the given model's curve.
	 */
	public double getRate(final AnalyticModel model) {
		if(model==null) {
			throw new IllegalArgumentException("model==null");
		}

		final DiscountCurve discountCurve = model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			throw new IllegalArgumentException("No discount curve with name '" + discountCurveName + "' was found in the model:\n" + model.toString());
		}

		final double payoutDate = schedule.getPeriodStart(0);
		final double maturity = schedule.getPayment(0);
		final double periodLength = schedule.getPeriodLength(0);

		final double discountFactor = discountCurve.getDiscountFactor(model, maturity);
		final double discountFactorPayout = discountCurve.getDiscountFactor(model, payoutDate);

		return (discountFactorPayout/discountFactor - 1.)/periodLength;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public String getDiscountCurveName() {
		return discountCurveName;
	}

	public double getRate() {
		return rate;
	}

	public double getPeriodEndTime() {
		final double tenorEnd = schedule.getPeriodEnd(0);
		return tenorEnd;
	}

	public double getFixingTime() {
		final double fixingDate = schedule.getFixing(0);
		return fixingDate;
	}

	@Override
	public String toString() {
		return "Deposit [schedule=" + schedule + ", rate=" + rate + ", forwardCurveName=" + discountCurveName + "]";
	}
}
