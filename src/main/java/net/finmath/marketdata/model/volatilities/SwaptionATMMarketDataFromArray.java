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

	// @TODO CurveFromInterpolationPoints data currently not used.
	private final ForwardCurve				forwardCurve;
	private final DiscountCurve			discountCurve;

	private final TimeDiscretization		optionMaturities;
	private final TimeDiscretization		tenor;

	private final double							swapPeriodLength;
	private final double[][]						impliedVolatilities;


	public SwaptionATMMarketDataFromArray(final double[] optionMaturities, final double[] tenor, final double swapPeriodLength, final double[][] impliedVolatilities) {
		super();
		forwardCurve = null;		// Implied vol only.
		discountCurve = null;		// Implied vol only.
		this.optionMaturities = new TimeDiscretizationFromArray(optionMaturities);
		this.tenor = new TimeDiscretizationFromArray(tenor);
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	public SwaptionATMMarketDataFromArray(final ForwardCurve forwardCurve, final DiscountCurve discountCurve, final double[] optionMaturities, final double[] tenor, final double swapPeriodLength, final double[][] impliedVolatilities) {
		super();
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.optionMaturities = new TimeDiscretizationFromArray(optionMaturities);
		this.tenor = new TimeDiscretizationFromArray(tenor);
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	public SwaptionATMMarketDataFromArray(final ForwardCurve forwardCurve, final DiscountCurve discountCurve, final TimeDiscretization optionMatruities, final TimeDiscretization tenor, final double swapPeriodLength, final double[][] impliedVolatilities) {
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
	public double getValue(final double optionMatruity, final double tenorLength, final double periodLength, final double strike) {
		throw new RuntimeException("Method not implemented.");
	}

	public double getVolatility(final double optionMatruity, final double tenorLength) {
		final int indexOptionMaturity = optionMaturities.getTimeIndex(optionMatruity);
		final int indexTenorIndex = tenor.getTimeIndex(tenorLength);
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
	public double getVolatility(final double optionMatruity, final double tenorLength, final double periodLength, final double strike) {
		final int indexOptionMaturity = optionMaturities.getTimeIndex(optionMatruity);
		final int indexTenorIndex = tenor.getTimeIndex(tenorLength);
		if(indexOptionMaturity < 0) {
			throw new IllegalArgumentException("Option maturity not part of data.");
		}
		if(indexTenorIndex < 0) {
			throw new IllegalArgumentException("TenorFromArray maturity not part of data.");
		}

		return impliedVolatilities[indexOptionMaturity][indexTenorIndex];
	}
}
