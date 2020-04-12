package net.finmath.montecarlo.interestrate.models.covariance;

import net.finmath.stochastic.RandomVariable;

/**
 * Interface for short rate volatility models which are determined by a vector of parameter.
 *
 * @author Christian Fries
 */
public interface ShortRateVolatilityModelParametric extends ShortRateVolatilityModel {

	/**
	 * Get the parameters of determining this parametric
	 * volatility model. The parameters are usually free parameters
	 * which may be used in calibration.
	 *
	 * @return Parameter vector.
	 */
	RandomVariable[] getParameter();

	/**
	 * Return an instance of this model using a new set of parameters.
	 * Note: To improve performance it is admissible to return the same instance of the object given that the parameters have not changed. Models should be immutable.
	 *
	 * @param parameters The new set of parameters.
	 * @return An instance of AbstractShortRateVolatilityModel with modified parameters.
	 */
	ShortRateVolatilityModelParametric getCloneWithModifiedParameters(RandomVariable[] parameters);

	/**
	 * Get the parameters of determining this parametric
	 * volatility model. The parameters are usually free parameters
	 * which may be used in calibration.
	 *
	 * @return Parameter vector.
	 */
	double[] getParameterAsDouble();

	/**
	 * Return an instance of this model using a new set of parameters.
	 * Note: To improve performance it is admissible to return the same instance of the object given that the parameters have not changed. Models should be immutable.
	 *
	 * @param parameters The new set of parameters.
	 * @return An instance of AbstractShortRateVolatilityModel with modified parameters.
	 */
	ShortRateVolatilityModelParametric getCloneWithModifiedParameters(double[] parameters);
}
