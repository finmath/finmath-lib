/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.12.2016
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import net.finmath.montecarlo.interestrate.TermStructureModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * A base class and interface description for the instantaneous covariance of
 * an forward rate interest rate model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface TermStructureFactorLoadingsModel {

	/**
	 * Return the factor loading for a given time and a term structure period.
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
	 * @param periodStart Period start of the component.
	 * @param periodEnd Period end of the component.
	 * @param periodDiscretization The period discretization associated with the realizationAtTimeIndex
	 * @param realizationAtTimeIndex The realization of the stochastic process (may be used to implement local volatility/covariance/correlation models).
	 * @param model The term structure model.
	 * @return The factor loading <i>f<sub>i</sub>(t)</i>.
	 */
	RandomVariable[] getFactorLoading(double time, double periodStart, double periodEnd,
			TimeDiscretization periodDiscretization, RandomVariable[] realizationAtTimeIndex,
			TermStructureModel model);

	/**
	 * @return the numberOfFactors
	 */
	int getNumberOfFactors();
}
