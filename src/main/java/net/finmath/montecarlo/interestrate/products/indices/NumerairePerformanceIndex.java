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


	public NumerairePerformanceIndex(String name, String currency, String paymentOffsetCode,
			BusinessdayCalendar paymentBusinessdayCalendar, DateRollConvention paymentDateRollConvention,
			DayCountConvention daycountConvention) {
		super(name, currency);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;
		this.daycountConvention = daycountConvention;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		/*
		 * The periodLength may be a given float or (more exact) derived from the rolling convetions.
		 */
		double fixingTime = evaluationTime;

		LocalDateTime referenceDate = model.getReferenceDate();
		LocalDateTime fixingDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, fixingTime);
		LocalDate paymentDate = paymentBusinessdayCalendar.getAdjustedDate(fixingDate.toLocalDate(), paymentOffsetCode, paymentDateRollConvention);
		double paymentTime = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, LocalDateTime.of(paymentDate, fixingDate.toLocalTime()));

		double periodLength = daycountConvention.getDaycount(fixingDate.toLocalDate(), paymentDate);

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

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = new HashSet<>();
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
