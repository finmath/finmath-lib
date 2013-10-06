package net.finmath.montecarlo.interestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

public interface LIBORMarketModelInterface extends AbstractModelInterface {

	RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException;

	/**
	 * The tenor time discretization of the forward rate curve.
	 * 
	 * @return The tenor time discretization of the forward rate curve.
	 */
    TimeDiscretizationInterface getLiborPeriodDiscretization();

	/**
	 * Get the number of LIBORs in the LIBOR discretization.
	 *  
	 * @return The number of LIBORs in the LIBOR discretization
	 */
    int getNumberOfLibors();

	/**
	 * The period start corresponding to a given forward rate discretization index.
	 * 
     * @param timeIndex The index corresponding to a given time (interpretation is start of period)
     * @return The period start corresponding to a given forward rate discretization index.
     */
    double getLiborPeriod(int timeIndex);

	/**
	 * Same as java.util.Arrays.binarySearch(liborPeriodDiscretization,time). Will return a negative value if the time is not found, but then -index-1 corresponds to the index of the smallest time greater than the given one.
	 * 
     * @param time The period start.
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
    int getLiborPeriodIndex(double time);

	/**
	 * Return the initial forward rate curve.
	 * 
	 * @return the forward rate curve
	 */
    ForwardCurveInterface getForwardRateCurve();

	/**
	 * Return the covariance model.
	 * 
	 * @return The covariance model.
	 */
    AbstractLIBORCovarianceModel getCovarianceModel();

	/**
	 * Create a new object implementing LIBORMarketModelInterface, using the new covariance model.
	 * 
	 * @param calibrationCovarianceModel The new covariance model.
	 * @return A new object implementing LIBORMarketModelInterface, using the new covariance model.
	 */
    LIBORMarketModelInterface getCloneWithModifiedCovarianceModel(AbstractLIBORCovarianceModel calibrationCovarianceModel);

    /**
	 * Create a new object implementing LIBORMarketModelInterface, using the new data.
	 * 
     * @param dataModified A map with values to be used in constructions (keys are identical to parameter names of the constructors).
	 * @return A new object implementing LIBORMarketModelInterface, using the new data.
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    LIBORMarketModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;

	/**
	 * Returns the integrated instantaneous log-forward rate covariance, i.e.,
	 * <i>\int_0^t_i d log(L_j) d log(L_k) dt</i>.
	 * 
	 * The array returned has the parametrization [i][j][k], i.e.,
	 * <code>integratedLIBORCovariance[timeIndex][componentIndex1][componentIndex2]</code>.
	 * 
	 * @return The integrated instantaneous log-LIBOR covariance.
	 */
    double[][][] getIntegratedLIBORCovariance();
}