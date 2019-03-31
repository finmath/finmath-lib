/*
 * Created on 15.09.2006
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.products.indices;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * An idealized (single curve) CMS index with given maturity and given period length.
 *
 * @author Christian Fries
 * @version 1.0
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
		if(numberOfPeriods * periodLength != maturity) {
			throw new IllegalArgumentException("matruity not divisible by periodLength");
		}
		periodLengths = new double[numberOfPeriods];
		Arrays.fill(periodLengths,periodLength);
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
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		// Fetch curve
		RandomVariable forwardRates[] = new RandomVariable[periodLengths.length];
		double periodStart = evaluationTime+fixingOffset;
		for(int periodIndex = 0; periodIndex < periodLengths.length; periodIndex++) {
			forwardRates[periodIndex] = model.getLIBOR(evaluationTime+fixingOffset, periodStart, periodStart+periodLengths[periodIndex]);
			periodStart += periodLengths[periodIndex];
		}

		// Calculate float leg value (single curve/classical) and annuity.
		RandomVariable forwardBondInverse           = model.getRandomVariableForConstant(1.0);
		RandomVariable forwardAnnuityInverse        = model.getRandomVariableForConstant(periodLengths[periodLengths.length-1]);
		for(int periodIndex = periodLengths.length-1; periodIndex>= 1; periodIndex--) {
			RandomVariable forwardBondOnePeriodInverse  = (forwardRates[periodIndex]).mult(periodLengths[periodIndex]).add(1.0);
			forwardBondInverse		= forwardBondInverse.mult(forwardBondOnePeriodInverse);
			forwardAnnuityInverse	= forwardAnnuityInverse.addProduct(forwardBondInverse, periodLengths[periodIndex]);
		}
		RandomVariable forwardBondOnePeriodInverse  = (forwardRates[0]).mult(periodLengths[0]).add(1.0);
		forwardBondInverse = forwardBondInverse.mult(forwardBondOnePeriodInverse);

		RandomVariable swaprate = forwardBondInverse.sub(1.0).div(forwardAnnuityInverse);

		return swaprate;
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = new HashSet<>();
		underlyingNames.add(getName());
		return underlyingNames;
	}

	@Override
	public String toString() {
		return "ConstantMaturitySwaprate [fixingOffset=" + fixingOffset
				+ ", periodLengths=" + Arrays.toString(periodLengths) + "]";
	}
}
