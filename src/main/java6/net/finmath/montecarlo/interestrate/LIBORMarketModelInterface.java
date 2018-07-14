package net.finmath.montecarlo.interestrate;

import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;

public interface LIBORMarketModelInterface extends LIBORModelInterface {

	/**
	 * Return the libor covariance model.
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
	 * Returns the integrated instantaneous log-forward rate covariance, i.e.,
	 * \( \int_{0}^{t_i} \mathrm{d} \log(L_{j}) \mathrm{d} \log(L_{k}) \mathrm{d}t \).
	 *
	 * The array returned has the parametrization [i][j][k], i.e.,
	 * <code>integratedLIBORCovariance[timeIndex][componentIndex1][componentIndex2]</code>.
	 *
	 * @return The integrated instantaneous log-LIBOR covariance.
	 */
	double[][][] getIntegratedLIBORCovariance();
}
