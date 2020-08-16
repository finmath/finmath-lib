/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */

package net.finmath.montecarlo.interestrate;

import net.finmath.montecarlo.interestrate.models.covariance.ShortRateVolatilityModel;

/**
 * Interface for Short Rate models which are determined by a ShortRateVolatilityModelInterface.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface ShortRateModel {

	/**
	 * Create a new object implementing ShortRateModel, using the new volatility model.
	 *
	 * @param volatilityModel The new volatility model.
	 * @return A new object implementing ShortRateModel, using the new volatility model.
	 */
	ShortRateModel getCloneWithModifiedVolatilityModel(ShortRateVolatilityModel volatilityModel);

	/**
	 * Return the volatility model.
	 *
	 * @return The volatility model.
	 */
	ShortRateVolatilityModel getVolatilityModel();

	/**
	 * Return the number of factors.
	 *
	 * @return The number of factors.
	 */
	int getNumberOfFactors();
}
