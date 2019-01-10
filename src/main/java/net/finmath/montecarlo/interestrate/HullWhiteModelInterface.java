/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */

package net.finmath.montecarlo.interestrate;

import net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolatilityModelInterface;

/**
 * Interface for Hull White Models which are determined by a ShortRateVolatilityModelInterface.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface HullWhiteModelInterface {

	/**
	 * Create a new object implementing HullWhiteModelInterface, using the new volatility model.
	 *
	 * @param volatilityModel The new volatility model.
	 * @return A new object implementing HullWhiteModelInterface, using the new volatility model.
	 */
	HullWhiteModelInterface getCloneWithModifiedVolatilityModel(ShortRateVolatilityModelInterface volatilityModel);

	/**
	 * Return the volatility model.
	 *
	 * @return The volatility model.
	 */
	ShortRateVolatilityModelInterface getVolatilityModel();

	/**
	 * Return the number of factors.
	 *
	 * @return The number of factors.
	 */
	int getNumberOfFactors();
}
