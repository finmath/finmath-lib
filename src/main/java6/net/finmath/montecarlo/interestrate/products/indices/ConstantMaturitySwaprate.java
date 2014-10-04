/*
 * Created on 15.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * An idealized (single curve) CMS index with given maturity and given period length. 
 * 
 * @author Christian Fries
 */
public class ConstantMaturitySwaprate extends AbstractIndex {

	private static final long serialVersionUID = -5353191308059733179L;

	final double	fixingOffset;
	final double[]	periodLengths;

	/**
	 * Create a CMS index with given fixing offset and given period lengths.
	 * 
	 * @param name The name of the underlying index.
	 * @param currency The currency of the underlying index, if any.
	 * @param fixingOffset Fixing offset of this index.
	 * @param periodLengths Period length of underlying swap, used for the swap annuity calculation.
	 */
	public ConstantMaturitySwaprate(String name, String currency, double fixingOffset, double[] periodLengths) {
		super(name, currency);
		this.fixingOffset = fixingOffset;
		this.periodLengths = periodLengths;
	}

	/**
	 * Create a CMS index with given fixing offset and given period lengths.
	 * 
	 * @param fixingOffset Fixing offset of this index.
	 * @param periodLengths Period length of underlying swap, used for the swap annuity calculation.
	 */
	public ConstantMaturitySwaprate(double fixingOffset, double[] periodLengths) {
		this(null, null, fixingOffset, periodLengths);
	}

	/**
	 * Create a CMS index with given period lengths.
	 * 
	 * @param periodLengths Period length of underlying swap, used for the swap annuity calculation.
	 */
	public ConstantMaturitySwaprate(double[] periodLengths) {
		this(0.0, periodLengths);
	}

	/**
	 * Create a CMS index with given fixing offset and given maturity and given period length.
	 * Note that maturity must be a multiple of the period length.
	 * 
	 * @param name The name of the underlying index.
	 * @param currency The currency of the underlying index, if any.
	 * @param fixingOffset Fixing offset of this index.
	 * @param maturity The maturity.
	 * @param periodLength Period length of underlying swap, used for the swap annuity calculation.
	 */
	public ConstantMaturitySwaprate(String name, String currency, double fixingOffset, double maturity, double periodLength) {
		super(name, currency);
		this.fixingOffset = fixingOffset;

		int numberOfPeriods = (int) (maturity / periodLength + 0.5);
		if(numberOfPeriods * periodLength != maturity) throw new IllegalArgumentException("matruity not divisible by periodLength");
		this.periodLengths = new double[numberOfPeriods];
		Arrays.fill(this.periodLengths,periodLength);
	}

	/**
	 * Create a CMS index with given fixing offset and given maturity and given period length.
	 * Note that maturity must be a multiple of the period length.
	 * 
	 * @param fixingOffset Fixing offset of this index.
	 * @param maturity The maturity.
	 * @param periodLength Period length of underlying swap, used for the swap annuity calculation.
	 */
	public ConstantMaturitySwaprate(double fixingOffset, double maturity, double periodLength) {
		this(null, null, fixingOffset, maturity, periodLength);
	}

	/**
	 * Create a CMS index with given maturity and given period length. Note that maturity must be a multiple of the period length.
	 * 
	 * @param maturity Maturity of the swap rate.
	 * @param periodLength Period length of the fixed size (determines the swap annuity used)
	 */
	public ConstantMaturitySwaprate(double maturity, double periodLength) {
		this(0.0, maturity, periodLength);
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		int firstPeriodRateIndex = model.getLiborPeriodIndex(evaluationTime+fixingOffset);
		int lastPeriodIndex = firstPeriodRateIndex+periodLengths.length-1;

		// Fetch curve
		RandomVariableInterface forwardRates[] = new RandomVariableInterface[periodLengths.length];
		double periodStart = evaluationTime+fixingOffset;
		for(int periodIndex = firstPeriodRateIndex; periodIndex <= lastPeriodIndex; periodIndex++) {
			forwardRates[periodIndex-firstPeriodRateIndex] = model.getLIBOR(evaluationTime+fixingOffset, periodStart, periodStart+periodLengths[periodIndex-firstPeriodRateIndex]);
			periodStart += periodLengths[periodIndex-firstPeriodRateIndex];
		}

		// Calculate swaprate
		RandomVariableInterface forwardBondInverse           = new RandomVariable(0.0,                                   1.0);
		RandomVariableInterface forwardAnnuityInverse        = new RandomVariable(0.0, periodLengths[periodLengths.length-1]);
		for(int periodIndex = lastPeriodIndex; periodIndex>= firstPeriodRateIndex+1; periodIndex--) {
			RandomVariableInterface forwardBondOnePeriodInverse  = (forwardRates[periodIndex-firstPeriodRateIndex]).mult(periodLengths[periodIndex-firstPeriodRateIndex]).add(1.0);
			forwardBondInverse		= forwardBondInverse.mult(forwardBondOnePeriodInverse);
			forwardAnnuityInverse	= forwardAnnuityInverse.addProduct(forwardBondInverse, periodLengths[periodIndex-firstPeriodRateIndex]);
		}
		RandomVariableInterface forwardBondOnePeriodInverse  = (forwardRates[firstPeriodRateIndex-firstPeriodRateIndex]).mult(periodLengths[firstPeriodRateIndex-firstPeriodRateIndex]).add(1.0);
		forwardBondInverse = forwardBondInverse.mult(forwardBondOnePeriodInverse);

		return forwardBondInverse.sub(1.0).div(forwardAnnuityInverse);
	}

}
