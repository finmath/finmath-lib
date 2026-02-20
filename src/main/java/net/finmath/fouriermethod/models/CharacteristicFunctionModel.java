/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.03.2014
 */

package net.finmath.fouriermethod.models;

import java.time.LocalDate;

import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.Model;

/**
 * Interface which has to be implemented by models providing the
 * characteristic functions of stochastic processes.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface CharacteristicFunctionModel extends Model {

	/**
	 * Returns the characteristic function of X(t), where X is <code>this</code> stochastic process.
	 *
	 * @param time The time at which the stochastic process is observed.
	 * @return The characteristic function of X(t).
	 */
	CharacteristicFunction apply(double time);

	/**
	 * 
	 * @return the reference date
	 */
	public LocalDate getReferenceDate();

	/**
	 * 
	 * @return the initial value of the stock
	 */
	public double getInitialValue();

	/**
	 * @return the discountCurveForForwardRate
	 */
	public DiscountCurve getDiscountCurveForForwardRate();

	/**
	 * @return the riskFreeRate
	 */
	public double getRiskFreeRate();

	/**
	 * @return the discountCurveForDiscountRate
	 */
	public DiscountCurve getDiscountCurveForDiscountRate();

	/**
	 * @return the discountRate
	 */
	public double getDiscountRate();

}
