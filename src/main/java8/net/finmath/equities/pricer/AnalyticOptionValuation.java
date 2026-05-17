package net.finmath.equities.pricer;

import java.util.HashMap;

import org.apache.commons.lang3.NotImplementedException;

import net.finmath.equities.marketdata.YieldCurve;
import net.finmath.equities.models.Black76Model;
import net.finmath.equities.models.EquityForwardStructure;
import net.finmath.equities.models.VolatilitySurface;
import net.finmath.equities.pricer.EquityValuationRequest.CalculationRequestType;
import net.finmath.equities.products.Option;
import net.finmath.time.daycount.DayCountConvention;

/**
 * This class implements analytic pricing formulas for European options under a
 * Black-Scholes process with Buehler dividends.
 *
 * @author Andreas Grotz
 */
public class AnalyticOptionValuation implements OptionValuation {

	private final DayCountConvention dcc;

	public AnalyticOptionValuation(DayCountConvention dcc) {
		this.dcc = dcc;
	}

	@Override
	public EquityValuationResult calculate(
			EquityValuationRequest request,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			VolatilitySurface volaSurface) {

		final HashMap<CalculationRequestType, Double> results = new HashMap<CalculationRequestType, Double>();
		for(final CalculationRequestType calcType : request.getCalcsRequested()) {
			results.put(
					calcType,
					calculate(request.getOption(), forwardStructure, discountCurve, volaSurface, calcType));
		}

		return new EquityValuationResult(request, results);
	}

	public double calculate(
			Option option,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			VolatilitySurface volaSurface,
			CalculationRequestType calcType) {

		assert !option.isAmericanOption() : "Analytic pricer cannot handle American options.";

		final java.time.LocalDate valDate = forwardStructure.getValuationDate();
		final java.time.LocalDate expiryDate = option.getExpiryDate();
		final double ttm = dcc.getDaycountFraction(forwardStructure.getValuationDate(), expiryDate);
		final double forward = forwardStructure.getForward(expiryDate);
		final double discountFactor = discountCurve.getDiscountFactor(expiryDate);
		final double discountRate = discountCurve.getRate(expiryDate);
		final double adjustedForward = forwardStructure.getDividendAdjustedStrike(forward, expiryDate);
		final double adjustedStrike = forwardStructure.getDividendAdjustedStrike(option.getStrike(), expiryDate);
		final double volatility = volaSurface.getVolatility(option.getStrike(), option.getExpiryDate(), forwardStructure);

		switch(calcType) {
		case Price:
			return Black76Model.optionPrice(
					1.0,
					adjustedStrike / adjustedForward,
					ttm,
					volatility,
					option.isCallOption(),
					discountFactor * adjustedForward);

		case EqDelta:
			final double dFdS = forwardStructure.getGrowthDiscountFactor(valDate, expiryDate);
			return dFdS * Black76Model.optionDelta(
					1.0,
					adjustedStrike / adjustedForward,
					ttm,
					volatility,
					option.isCallOption(),
					discountFactor);

		case EqGamma:
			final double dFdS2 = Math.pow(forwardStructure.getGrowthDiscountFactor(valDate, expiryDate), 2);
			return dFdS2 * Black76Model.optionGamma(
					1.0,
					adjustedStrike / adjustedForward,
					ttm,
					volatility,
					option.isCallOption(),
					discountFactor / adjustedForward);

		case EqVega:
			return Black76Model.optionVega(
					1.0,
					adjustedStrike / adjustedForward,
					ttm,
					volatility,
					option.isCallOption(),
					discountFactor * adjustedForward);

		case Theta:
			return Black76Model.optionTheta(
					1.0,
					adjustedStrike / adjustedForward,
					ttm,
					volatility,
					option.isCallOption(),
					discountFactor * adjustedForward,
					discountRate);

		default:
			throw new NotImplementedException("Calculation for " + calcType + " not implemented yet.");
		}
	}

	public double getPrice(
			Option option,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			VolatilitySurface volaSurface) {
		return calculate(option, forwardStructure, discountCurve, volaSurface, CalculationRequestType.Price);
	}

	public double getDelta(
			Option option,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			VolatilitySurface volaSurface) {
		return calculate(option, forwardStructure, discountCurve, volaSurface, CalculationRequestType.EqDelta);
	}

	public double getGamma(
			Option option,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			VolatilitySurface volaSurface) {
		return calculate(option, forwardStructure, discountCurve, volaSurface, CalculationRequestType.EqGamma);
	}

	public double getVega(
			Option option,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			VolatilitySurface volaSurface) {
		return calculate(option, forwardStructure, discountCurve, volaSurface, CalculationRequestType.EqVega);
	}

	public double getTheta(
			Option option,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			VolatilitySurface volaSurface) {
		return calculate(option, forwardStructure, discountCurve, volaSurface, CalculationRequestType.Theta);
	}

	public double getImpliedVolatility(
			Option option,
			EquityForwardStructure forwardStructure,
			YieldCurve discountCurve,
			double price) {

		assert !option.isAmericanOption() : "Analytic pricer cannot handle American options.";

		final java.time.LocalDate expiryDate = option.getExpiryDate();
		final double ttm = dcc.getDaycountFraction(forwardStructure.getValuationDate(), expiryDate);
		final double forward = forwardStructure.getForward(expiryDate);
		final double discount = discountCurve.getDiscountFactor(expiryDate);
		final double adjustedForward = forwardStructure.getDividendAdjustedStrike(forward, expiryDate);
		final double adjustedStrike = forwardStructure.getDividendAdjustedStrike(option.getStrike(), expiryDate);
		final double undiscountedPrice = price / discount / adjustedForward;

		return Black76Model.optionImpliedVolatility(
				1.0,
				adjustedStrike / adjustedForward,
				ttm,
				undiscountedPrice,
				option.isCallOption());
	}
}
