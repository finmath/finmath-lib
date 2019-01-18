/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.11.2015
 */

package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 * @version 1.0
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
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		// Accumulating values in the random variable
		RandomVariable value = model.getRandomVariableForConstant(0.0);

		for(int periodIndex=0; periodIndex<fixingDates.length; periodIndex++) {
			double fixingDate = fixingDates[periodIndex];
			double paymentDate = paymentDates[periodIndex];
			double periodLength = paymentDate-fixingDate;

			// Get floating rate for coupon
			RandomVariable coupon = model.getLIBOR(fixingDate, fixingDate, paymentDate);

			// Apply spread, if any
			if(spreads != null) {
				coupon = coupon.sub(spreads[periodIndex]);
			}

			// Apply floor, if any
			if(floors != null) {
				coupon = coupon.floor(floors[periodIndex]);
			}

			// Apply cap, if any
			if(caps != null) {
				coupon = coupon.cap(caps[periodIndex]);
			}

			coupon = coupon.mult(periodLength);

			RandomVariable numeraire = model.getNumeraire(paymentDate);
			RandomVariable monteCarloProbabilities	= model.getMonteCarloWeights(paymentDate);

			value = value.add(coupon.div(numeraire).mult(monteCarloProbabilities));
		}

		// Add unit notional payment at maturity
		RandomVariable notionalPayoff = model.getRandomVariableForConstant(1.0);
		RandomVariable numeraire = model.getNumeraire(maturity);
		RandomVariable monteCarloProbabilities	= model.getMonteCarloWeights(maturity);
		value = value.add(notionalPayoff.div(numeraire).mult(monteCarloProbabilities));

		RandomVariable	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
		RandomVariable	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		value = value.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

		return value;
	}
}
