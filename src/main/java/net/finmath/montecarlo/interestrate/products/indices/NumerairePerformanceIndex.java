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
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.daycount.DayCountConvention;

/**
 * A (floating) rate index representing the performance of the numeraire asset for a given period start offset (offset from fixing) and period length.
 *
 * The index is given give
 * \( F = \frac{1}{dc(t-s)} ( N(t)/N(s) - 1 ) \).
 * where \( s \) denotes the period start and  \( t \) denotes the period end and dc is a given daycount convention.
 *
 * @author Christian Fries
 * @version 1.1
 */
public class NumerairePerformanceIndex extends AbstractIndex {

	private static final long serialVersionUID = 1L;

	private final String paymentOffsetCode;
	private final BusinessdayCalendar paymentBusinessdayCalendar;
	private final BusinessdayCalendar.DateRollConvention paymentDateRollConvention;

	private final DayCountConvention daycountConvention;


	public NumerairePerformanceIndex(final String name, final String currency, final String paymentOffsetCode,
			final BusinessdayCalendar paymentBusinessdayCalendar, final DateRollConvention paymentDateRollConvention,
			final DayCountConvention daycountConvention) {
		super(name, currency);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;
		this.daycountConvention = daycountConvention;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		/*
		 * The periodLength may be a given float or (more exact) derived from the rolling convetions.
		 */
		final double fixingTime = evaluationTime;

		final LocalDateTime referenceDate = model.getReferenceDate();
		final LocalDateTime fixingDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, fixingTime);
		final LocalDate paymentDate = paymentBusinessdayCalendar.getAdjustedDate(fixingDate.toLocalDate(), paymentOffsetCode, paymentDateRollConvention);
		final double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, LocalDateTime.of(paymentDate, fixingDate.toLocalTime()));

		final double periodLength = daycountConvention.getDaycountFraction(fixingDate.toLocalDate(), paymentDate);

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

	@Override
	public Set<String> queryUnderlyings() {
		final Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "NumerairePerformanceIndex [paymentOffsetCode=" + paymentOffsetCode + ", paymentBusinessdayCalendar="
				+ paymentBusinessdayCalendar + ", paymentDateRollConvention=" + paymentDateRollConvention
				+ ", daycountConvention=" + daycountConvention + "]";
	}
}
