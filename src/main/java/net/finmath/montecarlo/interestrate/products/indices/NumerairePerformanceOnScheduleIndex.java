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
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
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

	public NumerairePerformanceOnScheduleIndex(final String name, final String currency, final Schedule schedule) {
		super(name, currency);
		this.schedule = schedule;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		/*
		 * The periodLength may be a given float or (more exact) derived from the rolling convetions.
		 */
		final double fixingTime = evaluationTime;
		final Period period = getPeriod(FloatingpointDate.getDateFromFloatingPointDate(model.getReferenceDate(), fixingTime));

		final LocalDate paymentDate = period.getPayment();
		final double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(model.getReferenceDate().toLocalDate(), paymentDate);

		final double periodLength = schedule.getDaycountconvention().getDaycountFraction(period.getPeriodStart(), period.getPeriodEnd());

		/*
		 * Fetch numeraire performance rate from model
		 */
		final RandomVariable numeraireAtStart = model.getNumeraire(fixingTime);
		final RandomVariable numeraireAtEnd = model.getNumeraire(paymentTime);

		RandomVariable numeraireRatio = numeraireAtEnd.div(numeraireAtStart);

		if(getName() != null && !model.getModel().getDiscountCurve().getName().equals(getName())) {
			// Perform a multiplicative adjustment on the forward bonds
			final AnalyticModel analyticModel = model.getModel().getAnalyticModel();
			final DiscountCurve indexDiscountCurve = analyticModel.getDiscountCurve(getName());
			final DiscountCurve modelDisountCurve = model.getModel().getDiscountCurve();
			final double forwardBondOnIndexCurve = indexDiscountCurve.getDiscountFactor(analyticModel, fixingTime)/indexDiscountCurve.getDiscountFactor(analyticModel, paymentTime);
			final double forwardBondOnModelCurve = modelDisountCurve.getDiscountFactor(analyticModel, fixingTime)/modelDisountCurve.getDiscountFactor(analyticModel, paymentTime);
			final double adjustment = forwardBondOnModelCurve/forwardBondOnIndexCurve;
			numeraireRatio = numeraireRatio.mult(adjustment);
		}

		final RandomVariable forwardRate = numeraireRatio.sub(1.0).div(periodLength);

		return forwardRate;
	}

	private Period getPeriod(final LocalDateTime localDateTime) {
		for(final Period period : schedule.getPeriods()) {
			if(period.getFixing().isEqual(localDateTime.toLocalDate())) {
				return period;
			}
		}

		return null;
	}

	@Override
	public Set<String> queryUnderlyings() {
		final Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "NumerairePerformanceOnScheduleIndex [schedule=" + schedule + "]";
	}
}
