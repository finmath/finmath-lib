/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata.model.volatilities;

import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Simple swaption market data class.
 *
 * The class does currently not provide a surface interpolation
 * like SABR.
 *
 * This will be added in a future version.
 *
 * @author Christian Fries
 */
public class SwaptionMarketData implements AbstractSwaptionMarketData {

	// @TODO: Curve data currently not used.
	private final ForwardCurveInterface				forwardCurve;
	private final DiscountCurveInterface			discountCurve;

	private final TimeDiscretizationInterface		optionMaturities;
	private final TimeDiscretizationInterface		tenor;

	private final double							swapPeriodLength;
	private final double[][]						impliedVolatilities;


	public SwaptionMarketData(double[] optionMaturities, double[] tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		this.forwardCurve = null;		// Implied vol only.
		this.discountCurve = null;		// Implied vol only.
		this.optionMaturities = new TimeDiscretization(optionMaturities);
		this.tenor = new TimeDiscretization(tenor);
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	public SwaptionMarketData(ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve, double[] optionMaturities, double[] tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.optionMaturities = new TimeDiscretization(optionMaturities);
		this.tenor = new TimeDiscretization(tenor);
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	public SwaptionMarketData(ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve, TimeDiscretizationInterface optionMatruities, TimeDiscretizationInterface tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.optionMaturities = optionMatruities;
		this.tenor = tenor;
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	@Override
	public TimeDiscretizationInterface getOptionMaturities() {
		return optionMaturities;
	}

	@Override
	public TimeDiscretizationInterface getTenor() {
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
			throw new IllegalArgumentException("Tenor maturity not part of data.");
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
			throw new IllegalArgumentException("Tenor maturity not part of data.");
		}

		return impliedVolatilities[indexOptionMaturity][indexTenorIndex];
	}
}

