/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.equities.models;

import java.time.LocalDate;

import net.finmath.equities.marketdata.AffineDividendStream;
import net.finmath.equities.marketdata.FlatYieldCurve;

/**
 * I to cover the forward structure of a stock, i.e. spot, repo curve and dividends.
 * Currently implemented is the Buehler dividend model, where the volatile part of the stock
 * excludes any future dividends.
 *
 * Another possible model is the Escrowed dividend model, where the volatile part of the stock
 * excludes future dividends up to a certain option maturity. Since the process depends on the option maturity,
 * this model is neither consistent nor arbitrage-free.
 * Yet another possible model is the Haug-Haug-Lewis dividend model, where the volatile part of the stock
 * includes all future dividends. This model is arbitrage-free, but it is inconsistent as the stock price
 * could fall below the PV of future dividends.
 * NOTE that these models would also require adjustments in the analytic and PDE pricers.
 *
 * @author Andreas Grotz
 */
public interface EquityForwardStructure {

	enum DividendModelType
	{
		None,
		Buehler,
		Escrowed,
		HaugHaugLewis,
	}

	DividendModelType getDividendModel();

	LocalDate getValuationDate();

	double getSpot();

	FlatYieldCurve getRepoCurve();

	AffineDividendStream getDividendStream();

	EquityForwardStructure cloneWithNewSpot(double newSpot);

	EquityForwardStructure cloneWithNewDate(LocalDate newDate);

	double getGrowthDiscountFactor(double startTime, double endTime);

	double getGrowthDiscountFactor(LocalDate startDate, LocalDate endDate);

	double getFutureDividendFactor(double valTime);

	double getFutureDividendFactor(LocalDate valDate);

	double getForward(double expiryTime);

	double getForward(LocalDate expiryDate);

	double getDividendAdjustedStrike(double strike, double expiryTime);

	double getDividendAdjustedStrike(double strike, LocalDate expiryDate);

	double getLogMoneyness(double strike, double expiryTime);

	double getLogMoneyness(double strike, LocalDate expiryDate);
}
