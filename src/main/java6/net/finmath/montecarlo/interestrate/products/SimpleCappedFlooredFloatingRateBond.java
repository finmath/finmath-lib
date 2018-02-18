/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.11.2015
 */

package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public class SimpleCappedFlooredFloatingRateBond extends AbstractLIBORMonteCarloProduct {

	private final double[]	fixingDates;	// Vector of fixing dates
	private final double[]	paymentDates;	// Vector of payment dates (same length as fixing dates)
	private final double[]	spreads;		// Vector of spreads
	private final double[]	floors;			// Vector of floors
	private final double[]	caps;			// Vector of caps
	private final double	maturity;

	public SimpleCappedFlooredFloatingRateBond(String currency, double[] fixingDates, double[] paymentDates, double[] spreads, double[] floors, double[] caps, double maturity) {
		super(currency);
		this.fixingDates = fixingDates;
		this.paymentDates = paymentDates;
		this.spreads = spreads;
		this.floors = floors;
		this.caps = caps;
		this.maturity = maturity;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {

		// Accumulating values in the random variable
		RandomVariableInterface value = model.getRandomVariableForConstant(0.0);

		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			double fixingDate = fixingDates[periodIndex];
			double paymentDate = paymentDates[periodIndex];
			double periodLength = paymentDate-fixingDate;

			// Get floating rate for coupon
			RandomVariableInterface coupon = model.getLIBOR(fixingDate, fixingDate, paymentDate);

			// Apply spread, if any
			if(spreads != null)	coupon = coupon.sub(spreads[periodIndex]);

			// Apply floor, if any
			if(floors != null)	coupon = coupon.floor(floors[periodIndex]);

			// Apply cap, if any
			if(caps != null)	coupon = coupon.cap(caps[periodIndex]);

			coupon = coupon.mult(periodLength);

			RandomVariableInterface numeraire = model.getNumeraire(paymentDate);
			RandomVariableInterface monteCarloProbabilities	= model.getMonteCarloWeights(paymentDate);

			value = value.add(coupon.div(numeraire).mult(monteCarloProbabilities));
		}

		// Add unit notional payment at maturity
		RandomVariableInterface notionalPayoff = model.getRandomVariableForConstant(1.0);
		RandomVariableInterface numeraire = model.getNumeraire(maturity);
		RandomVariableInterface monteCarloProbabilities	= model.getMonteCarloWeights(maturity);
		value = value.add(notionalPayoff.div(numeraire).mult(monteCarloProbabilities));

		RandomVariableInterface	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		value = value.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return value;
	}
}
