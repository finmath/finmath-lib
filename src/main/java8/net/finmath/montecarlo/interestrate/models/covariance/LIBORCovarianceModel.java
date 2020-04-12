/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.models.LIBORMarketModelFromCovarianceModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Interface for covariance models providing a vector of (possibly stochastic) factor loadings.
 *
 * Classes implementing this interface can be used as "plug ins" for {@link LIBORMarketModelFromCovarianceModel}.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface LIBORCovarianceModel {

	/**
	 * Return the factor loading for a given time and a given component.
	 *
	 * The factor loading is the vector <i>f<sub>i</sub></i> such that the scalar product <br>
	 * <i>f<sub>j</sub>f<sub>k</sub> = f<sub>j,1</sub>f<sub>k,1</sub> + ... + f<sub>j,m</sub>f<sub>k,m</sub></i> <br>
	 * is the instantaneous covariance of the component <i>j</i> and <i>k</i>.
	 *
	 * With respect to simulation time <i>t</i>, this method uses a piece wise constant interpolation, i.e.,
	 * it calculates <i>t_<sub>i</sub></i> such that <i>t_<sub>i</sub></i> is the largest point in <code>getTimeDiscretization</code>
	 * such that <i>t_<sub>i</sub> &le; t </i>.
	 *
	 * The component here, it given via a double <i>T</i> which may be associated with the LIBOR fixing date.
	 * With respect to component time <i>T</i>, this method uses a piece wise constant interpolation, i.e.,
	 * it calculates <i>T_<sub>j</sub></i> such that <i>T_<sub>j</sub></i> is the largest point in <code>getTimeDiscretization</code>
	 * such that <i>T_<sub>j</sub> &le; T </i>.
	 *
	 * @param time The time <i>t</i> at which factor loading is requested.
	 * @param component The component time (as a double associated with the fixing of the forward rate)  <i>T<sub>i</sub></i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @return The factor loading <i>f<sub>i</sub>(t)</i>.
	 */
	RandomVariable[] getFactorLoading(double time, double component, RandomVariable[] realizationAtTimeIndex);

	/**
	 * Return the factor loading for a given time and component index.
	 * The factor loading is the vector <i>f<sub>i</sub></i> such that the scalar product <br>
	 * <i>f<sub>j</sub>f<sub>k</sub> = f<sub>j,1</sub>f<sub>k,1</sub> + ... + f<sub>j,m</sub>f<sub>k,m</sub></i> <br>
	 * is the instantaneous covariance of the component <i>j</i> and <i>k</i>.
	 *
	 * With respect to simulation time <i>t</i>, this method uses a piece wise constant interpolation, i.e.,
	 * it calculates <i>t_<sub>i</sub></i> such that <i>t_<sub>i</sub></i> is the largest point in <code>getTimeDiscretization</code>
	 * such that <i>t_<sub>i</sub> &le; t </i>.
	 *
	 * @param time The time <i>t</i> at which factor loading is requested.
	 * @param component The index of the component <i>i</i>. Note that this class may have its own LIBOR time discretization and that this index refers to this discretization.
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @return The factor loading <i>f<sub>i</sub>(t)</i>.
	 */
	RandomVariable[] getFactorLoading(double time, int component, RandomVariable[] realizationAtTimeIndex);

	/**
	 * Return the factor loading for a given time index and component index.
	 * The factor loading is the vector <i>f<sub>i</sub></i> such that the scalar product <br>
	 * <i>f<sub>j</sub>f<sub>k</sub> = f<sub>j,1</sub>f<sub>k,1</sub> + ... + f<sub>j,m</sub>f<sub>k,m</sub></i> <br>
	 * is the instantaneous covariance of the component <i>j</i> and <i>k</i>.
	 *
	 * @param timeIndex The time index at which factor loading is requested.
	 * @param component The index of the component  <i>i</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @return The factor loading <i>f<sub>i</sub>(t)</i>.
	 */
	RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex);

	/**
	 * Returns the pseudo inverse of the factor matrix.
	 *
	 * @param timeIndex The time index at which factor loading inverse is requested.
	 * @param factor The index of the factor <i>j</i>.
	 * @param component The index of the component  <i>i</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @return The entry of the pseudo-inverse of the factor loading matrix.
	 */
	RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor,
			RandomVariable[] realizationAtTimeIndex);

	/**
	 * Returns the instantaneous covariance calculated from factor loadings.
	 *
	 * @param time The time <i>t</i> at which covariance is requested.
	 * @param component1 Index of component <i>i</i>.
	 * @param component2  Index of component <i>j</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process.
	 * @return The instantaneous covariance between component <i>i</i> and  <i>j</i>.
	 */
	RandomVariable getCovariance(double time, int component1, int component2, RandomVariable[] realizationAtTimeIndex);

	/**
	 * Returns the instantaneous covariance calculated from factor loadings.
	 *
	 * @param timeIndex The time index at which covariance is requested.
	 * @param component1 Index of component <i>i</i>.
	 * @param component2  Index of component <i>j</i>.
	 * @param realizationAtTimeIndex The realization of the stochastic process.
	 * @return The instantaneous covariance between component <i>i</i> and  <i>j</i>.
	 */
	RandomVariable getCovariance(int timeIndex, int component1, int component2,
			RandomVariable[] realizationAtTimeIndex);

	/**
	 * The simulation time discretization associated with this model.
	 *
	 * @return the timeDiscretizationFromArray
	 */
	TimeDiscretization getTimeDiscretization();

	/**
	 * The forward rate time discretization associated with this model (defines the components).
	 *
	 * @return the forward rate time discretization associated with this model.
	 */
	TimeDiscretization getLiborPeriodDiscretization();

	/**
	 * @return the numberOfFactors
	 */
	int getNumberOfFactors();

	/**
	 * Returns a clone of this model where the specified properties have been modified.
	 *
	 * Note that there is no guarantee that a model reacts on a specification of a properties in the
	 * parameter map <code>dataModified</code>. If data is provided which is ignored by the model
	 * no exception may be thrown.
	 *
	 * Furthermore the structure of the covariance model has to match changed data.
	 * A change of the time discretizations may requires a change in the parameters
	 * but this function will just insert the new time discretization without
	 * changing the parameters. An exception may not be thrown.
	 *
	 * @param dataModified Key-value-map of parameters to modify.
	 * @return A clone of this model (or a new instance of this model if no parameter was modified).
	 * @throws CalculationException Thrown when the model could not be created.
	 */
	AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;
}
