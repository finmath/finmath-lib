package net.finmath.equities.pricer;

import java.util.HashMap;

import org.apache.commons.lang3.NotImplementedException;

import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.equities.models.Black76Model;
import net.finmath.equities.models.EquityForwardStructure;
import net.finmath.equities.models.VolatilitySurface;
import net.finmath.equities.pricer.EquityPricingRequest.CalculationRequestType;
import net.finmath.equities.products.Option;
import net.finmath.time.daycount.DayCountConvention;

/**
 * This class implements analytic pricing formulas for European options under a
 * Black-Scholes process with Buehler dividends.
 *
 * @author Andreas Grotz
 */

public class AnalyticOptionPricer implements OptionPricer
{

	private final DayCountConvention dcc;

	public AnalyticOptionPricer(
			DayCountConvention dcc) {
		this.dcc = dcc;
	}

	@Override
	public EquityPricingResult calculate(
			EquityPricingRequest request,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface)
	{
		var results = new HashMap<CalculationRequestType, Double>();
		for (var calcType  : request.getCalcsRequested()) {
			results.put(calcType, calculate(request.getOption(), forwardStructure, discountCurve, volaSurface, calcType));
		}

		return new EquityPricingResult(request, results);
	}


	public double calculate(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface,
			CalculationRequestType calcType)
	{
		assert !option.isAmericanOption() : "Analytic pricer cannot handle American options.";
		var valDate = forwardStructure.getValuationDate();
		var expiryDate = option.getExpiryDate();
		var ttm = dcc.getDaycountFraction(forwardStructure.getValuationDate(), expiryDate);
		var forward = forwardStructure.getForward(expiryDate);
		var discountFactor = discountCurve.getDiscountFactor(expiryDate);
		//var repoRate = forwardStructure.getRepoCurve().getRate(expiryDate);
		var repoRate = discountCurve.getRate(expiryDate);
		var adjustedForward = forwardStructure.getDividendAdjustedStrike(forward, expiryDate);
		var adjustedStrike = forwardStructure.getDividendAdjustedStrike(option.getStrike(), expiryDate);
		var volatility = volaSurface.getVolatility(
				option.getStrike(),
				option.getExpiryDate(),
				forwardStructure);

		switch(calcType)
		{
		case Price:
			return Black76Model.optionPrice(
					1.0,
					adjustedStrike / adjustedForward,
					ttm,
					volatility,
					option.isCallOption(),
					discountFactor * adjustedForward);
		case EqDelta:
			var dFdS = forwardStructure.getGrowthDiscountFactor(valDate, expiryDate);
			return dFdS * Black76Model.optionDelta(
					1.0,
					adjustedStrike / adjustedForward,
					ttm,
					volatility,
					option.isCallOption(),
					discountFactor);
		case EqGamma:
			var dFdS2 = Math.pow(forwardStructure.getGrowthDiscountFactor(valDate, expiryDate), 2);
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
					repoRate)
					- repoRate * Black76Model.optionPrice(
							1.0,
							adjustedStrike / adjustedForward,
							ttm,
							volatility,
							option.isCallOption(),
							discountFactor * adjustedForward);
		default:
			throw new NotImplementedException("Calculation for " + calcType + " not implemented yet.");
		}
	}

	public double getPrice(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface)
	{
		return calculate(
				option,
				forwardStructure,
				discountCurve,
				volaSurface,
				CalculationRequestType.Price);
	}

	public double getDelta(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface)
	{
		return calculate(
				option,
				forwardStructure,
				discountCurve,
				volaSurface,
				CalculationRequestType.EqDelta);
	}

	public double getGamma(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface)
	{
		return calculate(
				option,
				forwardStructure,
				discountCurve,
				volaSurface,
				CalculationRequestType.EqGamma);
	}

	public double getVega(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface)
	{
		return calculate(
				option,
				forwardStructure,
				discountCurve,
				volaSurface,
				CalculationRequestType.EqVega);
	}

	public double getTheta(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			VolatilitySurface volaSurface)
	{
		return calculate(
				option,
				forwardStructure,
				discountCurve,
				volaSurface,
				CalculationRequestType.Theta);
	}

	public double getImpliedVolatility(
			Option option,
			EquityForwardStructure forwardStructure,
			FlatYieldCurve discountCurve,
			double price)
	{
		assert !option.isAmericanOption() : "Analytic pricer cannot handle American options.";
		var expiryDate = option.getExpiryDate();
		var ttm = dcc.getDaycountFraction(forwardStructure.getValuationDate(), expiryDate);
		var forward = forwardStructure.getForward(expiryDate);
		var discount = discountCurve.getDiscountFactor(expiryDate);
		var adjustedForward = forwardStructure.getDividendAdjustedStrike(forward, expiryDate);
		var adjustedStrike = forwardStructure.getDividendAdjustedStrike(option.getStrike(), expiryDate);
		var undiscountedPrice = price / discount / adjustedForward;

		return Black76Model.optionImpliedVolatility(
				1.0,
				adjustedStrike / adjustedForward,
				ttm,
				undiscountedPrice,
				option.isCallOption());
	}
}
