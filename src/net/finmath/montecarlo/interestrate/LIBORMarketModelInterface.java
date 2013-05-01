package net.finmath.montecarlo.interestrate;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

public interface LIBORMarketModelInterface extends AbstractModelInterface {

	public abstract RandomVariableInterface getLIBOR(int timeIndex,
			int liborIndex) throws CalculationException;

	/**
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

	public abstract TimeDiscretizationInterface getLiborPeriodDiscretization();

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

}