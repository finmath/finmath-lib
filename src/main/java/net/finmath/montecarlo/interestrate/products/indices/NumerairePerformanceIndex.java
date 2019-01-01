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
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.FloatingpointDate;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;
import net.finmath.time.daycount.DayCountConventionInterface;

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
	private final BusinessdayCalendarInterface paymentBusinessdayCalendar;
	private final BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention;

	private final DayCountConventionInterface daycountConvention;


	public NumerairePerformanceIndex(String name, String currency, String paymentOffsetCode,
			BusinessdayCalendarInterface paymentBusinessdayCalendar, DateRollConvention paymentDateRollConvention,
			DayCountConventionInterface daycountConvention) {
		super(name, currency);
		this.paymentOffsetCode = paymentOffsetCode;
		this.paymentBusinessdayCalendar = paymentBusinessdayCalendar;
		this.paymentDateRollConvention = paymentDateRollConvention;
		this.daycountConvention = daycountConvention;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

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
		RandomVariableInterface numeraireAtStart = model.getNumeraire(fixingTime);
		RandomVariableInterface numeraireAtEnd = model.getNumeraire(paymentTime);

		RandomVariableInterface numeraireRatio = numeraireAtEnd.div(numeraireAtStart);

		if(getName() != null && !model.getModel().getDiscountCurve().getName().equals(getName())) {
			// Perform a multiplicative adjustment on the forward bonds
			AnalyticModelInterface analyticModel = model.getModel().getAnalyticModel();
			DiscountCurveInterface indexDiscountCurve = analyticModel.getDiscountCurve(getName());
			DiscountCurveInterface modelDisountCurve = model.getModel().getDiscountCurve();
			double forwardBondOnIndexCurve = indexDiscountCurve.getDiscountFactor(analyticModel, fixingTime)/indexDiscountCurve.getDiscountFactor(analyticModel, paymentTime);
			double forwardBondOnModelCurve = modelDisountCurve.getDiscountFactor(analyticModel, fixingTime)/modelDisountCurve.getDiscountFactor(analyticModel, paymentTime);
			double adjustment = forwardBondOnModelCurve/forwardBondOnIndexCurve;
			numeraireRatio = numeraireRatio.mult(adjustment);
		}
		
		RandomVariableInterface forwardRate = numeraireRatio.sub(1.0).div(periodLength);

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
