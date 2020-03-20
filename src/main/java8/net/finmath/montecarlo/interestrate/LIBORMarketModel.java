/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */

package net.finmath.montecarlo.interestrate;

import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.time.TimeDiscretization;

/**
 * Interface for LIBOR Market Models which are determined by a covariance structure defined on discrete forward rates.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface LIBORMarketModel extends LIBORModel {

	/**
	 * Return the forward rate (LIBOR) covariance model.
	 *
	 * @return The covariance model.
	 */
	LIBORCovarianceModel getCovarianceModel();

	/**
	 * Create a new object implementing LIBORMarketModel, using the new covariance model.
	 *
	 * @param calibrationCovarianceModel The new covariance model.
	 * @return A new object implementing LIBORMarketModel, using the new covariance model.
	 */
	LIBORMarketModel getCloneWithModifiedCovarianceModel(LIBORCovarianceModel calibrationCovarianceModel);

	/**
	 * Returns the integrated instantaneous log-forward rate covariance, i.e.,
	 * \( \int_{0}^{t_i} \mathrm{d} \log(L_{j}) \mathrm{d} \log(L_{k}) \mathrm{d}t \).
	 *
	 * The array returned has the parametrization [i][j][k], i.e.,
	 * <code>integratedLIBORCovariance[timeIndex][componentIndex1][componentIndex2]</code>.
	 *
	 * @param timeDiscretization The timeDiscretization used for the integration.
	 * @return The integrated instantaneous log-LIBOR covariance.
	 */
	double[][][] getIntegratedLIBORCovariance(TimeDiscretization timeDiscretization);
}
