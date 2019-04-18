/*
 * Created on 01.01.2019
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;

/**
 * A (floating) rate index representing the performance of the numeraire asset.
 *
 * The index is given give
 * \( F = \frac{1}{dc(t-s)} ( N(t)/N(s) - 1 ) \).
 * where \( s \) denotes the period start and  \( t \) denotes the period end and dc is a given daycount convention.
 *
 * The index fetches fixing, payment and daycount conventions, etc. from a given schedule.
 * The index fails to evaluate
 *
 * @author Christian Fries
 * @version 1.1
 */
public class NumerairePerformanceOnScheduleIndex extends AbstractIndex {

	private static final long serialVersionUID = 1L;

	private final Schedule schedule;

	public NumerairePerformanceOnScheduleIndex(String name, String currency, Schedule schedule) {
		super(name, currency);
		this.schedule = schedule;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		/*
		 * The periodLength may be a given float or (more exact) derived from the rolling convetions.
		 */
		double fixingTime = evaluationTime;
		Period period = getPeriod(FloatingpointDate.getDateFromFloatingPointDate(model.getReferenceDate(), fixingTime));

		LocalDate paymentDate = period.getPayment();
		double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), paymentDate);

		double periodLength = schedule.getDaycountconvention().getDaycountFraction(period.getPeriodStart(), period.getPeriodEnd());

		/*
		 * Fetch numeraire performance rate from model
		 */
		RandomVariable numeraireAtStart = model.getNumeraire(fixingTime);
		RandomVariable numeraireAtEnd = model.getNumeraire(paymentTime);

		RandomVariable numeraireRatio = numeraireAtEnd.div(numeraireAtStart);

		if(getName() != null && !model.getModel().getDiscountCurve().getName().equals(getName())) {
			// Perform a multiplicative adjustment on the forward bonds
			AnalyticModel analyticModel = model.getModel().getAnalyticModel();
			DiscountCurve indexDiscountCurve = analyticModel.getDiscountCurve(getName());
			DiscountCurve modelDisountCurve = model.getModel().getDiscountCurve();
			double forwardBondOnIndexCurve = indexDiscountCurve.getDiscountFactor(analyticModel, fixingTime)/indexDiscountCurve.getDiscountFactor(analyticModel, paymentTime);
			double forwardBondOnModelCurve = modelDisountCurve.getDiscountFactor(analyticModel, fixingTime)/modelDisountCurve.getDiscountFactor(analyticModel, paymentTime);
			double adjustment = forwardBondOnModelCurve/forwardBondOnIndexCurve;
			numeraireRatio = numeraireRatio.mult(adjustment);
		}

		RandomVariable forwardRate = numeraireRatio.sub(1.0).div(periodLength);

		return forwardRate;
	}

	private Period getPeriod(LocalDateTime localDateTime) {
		for(Period period : schedule.getPeriods()) {
			if(period.getFixing().isEqual(localDateTime.toLocalDate())) {
				return period;
			}
		}

		return null;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "NumerairePerformanceOnScheduleIndex [schedule=" + schedule + "]";
	}
}
