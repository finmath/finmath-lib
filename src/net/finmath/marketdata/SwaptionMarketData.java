/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.marketdata;

import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

public class SwaptionMarketData implements AbstractSwaptionMarketData {

	// @TODO: Curve data currently not used.
	private ForwardCurveInterface			forwardCurve;
	private DiscountCurveInterface			discountCurve;

	private TimeDiscretizationInterface		optionMaturities;
	private TimeDiscretizationInterface		tenor;

	private double							swapPeriodLength;
	private double[][]						impliedVolatilities;


	public SwaptionMarketData(double[] optionMatruities, double[] tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		this.forwardCurve = null;		// Implied vol only.
		this.optionMaturities = new TimeDiscretization(optionMatruities);
		this.tenor = new TimeDiscretization(tenor);
		this.swapPeriodLength = swapPeriodLength;
		this.impliedVolatilities = impliedVolatilities;
	}

	public SwaptionMarketData(ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve, double[] optionMatruities, double[] tenor, double swapPeriodLength, double[][] impliedVolatilities) {
		super();
		this.forwardCurve = forwardCurve;
		this.discountCurve = discountCurve;
		this.optionMaturities = new TimeDiscretization(optionMatruities);
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

	public TimeDiscretizationInterface getOptionMaturities() {
		return optionMaturities;
	}

	public TimeDiscretizationInterface getTenor() {
		return tenor;
	}
	
	public double getSwapPeriodLength() {
		return swapPeriodLength;
	}
	
	public double getValue(double optionMatruity, double tenorLength, double periodLength, double strike) {
		throw new RuntimeException("Method not implemented.");
	}

	public double getVolatility(double optionMatruity, double tenorLength) {
		int indexOptionMaturity = optionMaturities.getTimeIndex(optionMatruity);
		int indexTenorIndex = tenor.getTimeIndex(tenorLength);
		if(indexOptionMaturity < 0 || indexTenorIndex < 0) throw new IllegalArgumentException();
		
		return impliedVolatilities[indexOptionMaturity][indexTenorIndex];
	}

	/* (non-Javadoc)
	 * @see net.finmath.marketdata.AbstractSwaptionMarketData#getVolatility(double, double, double, double)
	 */
	public double getVolatility(double optionMatruity, double tenorLength, double periodLength, double strike) {
		int indexOptionMaturity = optionMaturities.getTimeIndex(optionMatruity);
		int indexTenorIndex = tenor.getTimeIndex(tenorLength);
		if(indexOptionMaturity < 0 || indexTenorIndex < 0) throw new IllegalArgumentException();
		
		return impliedVolatilities[indexOptionMaturity][indexTenorIndex];
	}
}
