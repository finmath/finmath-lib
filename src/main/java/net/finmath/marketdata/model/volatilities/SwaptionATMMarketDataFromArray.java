/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.volatilities;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Simple swaption market data class.
 *
 * The class does currently not provide a surface interpolation
 * like SABR.
 *
 * This will be added in a future version.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SwaptionATMMarketDataFromArray implements SwaptionMarketData {

	// @TODO: CurveFromInterpolationPoints data currently not used.
	private final ForwardCurve				forwardCurve;
	private final DiscountCurve			discountCurve;

	private final TimeDiscretization		optionMaturities;
	private final TimeDiscretization		tenor;

	private final double							swapPeriodLength;
	private final double[][]						impliedVolatilities;


	public SwaptionATMMarketDataFromArray(double[] optionMaturities, double[] tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		forwardCurve = null;		// Implied vol only.
		discountCurve = null;		// Implied vol only.
		this.optionMaturities = new TimeDiscretizationFromArray(optionMaturities);
		this.tenor = new TimeDiscretizationFromArray(tenor);
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	public SwaptionATMMarketDataFromArray(ForwardCurve forwardCurve, DiscountCurve discountCurve, double[] optionMaturities, double[] tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.optionMaturities = new TimeDiscretizationFromArray(optionMaturities);
		this.tenor = new TimeDiscretizationFromArray(tenor);
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	public SwaptionATMMarketDataFromArray(ForwardCurve forwardCurve, DiscountCurve discountCurve, TimeDiscretization optionMatruities, TimeDiscretization tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		optionMaturities = optionMatruities;
		this.tenor = tenor;
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	@Override
	public TimeDiscretization getOptionMaturities() {
		return optionMaturities;
	}

	@Override
	public TimeDiscretization getTenor() {
		return tenor;
	}

	@Override
	public double getSwapPeriodLength() {
		return swapPeriodLength;
	}

	@Override
	public double getValue(double optionMatruity, double tenorLength, double periodLength, double strike) {
		throw new RuntimeException("Method not implemented.");
	}

	public double getVolatility(double optionMatruity, double tenorLength) {
		int indexOptionMaturity = optionMaturities.getTimeIndex(optionMatruity);
		int indexTenorIndex = tenor.getTimeIndex(tenorLength);
		if(indexOptionMaturity < 0) {
			throw new IllegalArgumentException("Option maturity not part of data.");
		}
		if(indexTenorIndex < 0) {
			throw new IllegalArgumentException("TenorFromArray maturity not part of data.");
		}

		return impliedVolatilities[indexOptionMaturity][indexTenorIndex];
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.AbstractSwaptionMarketData#getVolatility(double, double, double, double)
	 */
	@Override
	public double getVolatility(double optionMatruity, double tenorLength, double periodLength, double strike) {
		int indexOptionMaturity = optionMaturities.getTimeIndex(optionMatruity);
		int indexTenorIndex = tenor.getTimeIndex(tenorLength);
		if(indexOptionMaturity < 0) {
			throw new IllegalArgumentException("Option maturity not part of data.");
		}
		if(indexTenorIndex < 0) {
			throw new IllegalArgumentException("TenorFromArray maturity not part of data.");
		}

		return impliedVolatilities[indexOptionMaturity][indexTenorIndex];
	}
}
