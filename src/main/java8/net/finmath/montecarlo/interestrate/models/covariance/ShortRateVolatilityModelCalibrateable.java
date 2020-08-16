package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.CalibrationProduct;
import net.finmath.montecarlo.interestrate.ShortRateModel;

/**
 * Interface for covariance models which may perform a calibration by providing the corresponding <code>getCloneCalibrated</code>-method.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface ShortRateVolatilityModelCalibrateable extends ShortRateVolatilityModel {

	/**
	 * Performs a calibration of the model by
	 * trying to match a given vector of calibration product to a given vector of target values
	 * using a given vector of weights.
	 *
	 * Optional calibration parameters may be passed using the map calibrationParameters. The keys are (<code>String</code>s):
	 * <ul>
	 * 	<li><code>brownianMotion</code>: Under this key an object implementing {@link net.finmath.montecarlo.BrownianMotion} may be provided. If so, this Brownian motion is used to build the valuation model.</li>
	 * 	<li><code>maxIterations</code>: Under this key an object of type Integer may be provided specifying the maximum number of iterations.</li>
	 * 	<li><code>accuracy</code>: Under this key an object of type Double may be provided specifying the desired accuracy. Note that this is understood in the sense that the solver will stop if the iteration does not improve by more than this number.</li>
	 * </ul>
	 *
	 * @param calibrationModel The LIBOR market model to be used for calibrations (specifies forward curve and tenor discretization).
	 * @param calibrationProducts The array of calibration products.
	 * @param calibrationParameters A map of type Map&lt;String, Object&gt; specifying some (optional) calibration parameters.
	 * @return A new parametric model of the same type than <code>this</code> one, but with calibrated parameters.
	 * @throws CalculationException Thrown if calibration has failed.
	 */
	ShortRateVolatilityModelCalibrateable getCloneCalibrated(ShortRateModel calibrationModel,
			CalibrationProduct[] calibrationProducts, Map<String, Object> calibrationParameters) throws CalculationException;

}
