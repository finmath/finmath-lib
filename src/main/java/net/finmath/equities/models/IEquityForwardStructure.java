/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.equities.models;

import java.time.LocalDate;
import net.finmath.equities.marketdata.*;

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

public interface IEquityForwardStructure {

	enum DividendModelType
	{
		None,
		Buehler,
		Escrowed,
		HaugHaugLewis,
	}

	public DividendModelType getDividendModel();

	public LocalDate getValuationDate();

	public double getSpot();

	public FlatYieldCurve getRepoCurve();

	public AffineDividendStream getDividendStream();

	public IEquityForwardStructure cloneWithNewSpot(double newSpot);

	public IEquityForwardStructure cloneWithNewDate(LocalDate newDate);

	public double getGrowthDiscountFactor(double startTime, double endTime);

	public double getGrowthDiscountFactor(LocalDate startDate, LocalDate endDate);

	public double getFutureDividendFactor(double valTime);

	public double getFutureDividendFactor(LocalDate valDate);

	public double getForward(double expiryTime);

	public double getForward(LocalDate expiryDate);

	public double getDividendAdjustedStrike(double strike, double expiryTime);

	public double getDividendAdjustedStrike(double strike, LocalDate expiryDate);

	public double getLogMoneyness(double strike, double expiryTime);

	public double getLogMoneyness(double strike, LocalDate expiryDate);
}