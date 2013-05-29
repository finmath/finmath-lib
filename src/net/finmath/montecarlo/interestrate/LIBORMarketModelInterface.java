package net.finmath.montecarlo.interestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

public interface LIBORMarketModelInterface extends AbstractModelInterface {

	public abstract RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException;

	/**
	 * The tenor time discretization of the forward rate curve.
	 * 
	 * @return The tenor time discretization of the forward rate curve.
	 */
	public abstract TimeDiscretizationInterface getLiborPeriodDiscretization();

	/**
	 * Get the number of LIBORs in the LIBOR discretization.
	 *  
	 * @return The number of LIBORs in the LIBOR discretization
	 */
	public abstract int getNumberOfLibors();

	/**
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
	public abstract double getLiborPeriod(int timeIndex);

	/**
	 * Same as java.util.Arrays.binarySearch(liborPeriodDiscretization,time). Will return a negative value if the time is not found, but then -index-1 corresponds to the index of the smallest time greater than the given one.
	 * 
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
	public abstract int getLiborPeriodIndex(double time);

	/**
	 * Return the initial forward rate curve.
	 * 
	 * @return the forward rate curve
	 */
	public abstract ForwardCurveInterface getForwardRateCurve();

	/**
	 * Return the covariance model.
	 * 
	 * @return The covariance model.
	 */
	public abstract AbstractLIBORCovarianceModel getCovarianceModel();

	/**
	 * Create a new object implementing LIBORMarketModelInterface, using the new covariance model.
	 * 
	 * @param calibrationCovarianceModel
	 * @return A new object implementing LIBORMarketModelInterface, using the new covariance model.
	 */
	public abstract LIBORMarketModelInterface getCloneWithModifiedCovarianceModel(AbstractLIBORCovarianceModel calibrationCovarianceModel);

    /**
	 * Create a new object implementing LIBORMarketModelInterface, using the new data.
	 * 
     * @param dataModified A map with values to be used in constuctions (keys are identical to parameter names of the constructors).
	 * @return A new object implementing LIBORMarketModelInterface, using the new data.
     * @throws CalculationException
     */
    public abstract LIBORMarketModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;

	/**
	 * @return
	 */
	public abstract double[][][] getIntegratedLIBORCovariance();
}