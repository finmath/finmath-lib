/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 28.03.2008
 */
package net.finmath.montecarlo.model;

import java.time.LocalDateTime;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

/**
 * The interface for a model of a stochastic process <i>X</i> where
 * <i>X(t) = f(t,Y(t))</i> and <br>
 * \[
 * dY_{j} = \mu_{j} dt + \lambda_{1,j} dW_{1} + \ldots + \lambda_{m,j} dW_{m}
 * \]
 *
 * <ul>
 * <li>The value of <i>Y(0)</i> is provided by the method {@link net.finmath.montecarlo.model.ProcessModel#getInitialState}.
 * <li>The value of &mu; is provided by the method {@link net.finmath.montecarlo.model.ProcessModel#getDrift}.
 * <li>The value &lambda;<sub>j</sub> is provided by the method {@link net.finmath.montecarlo.model.ProcessModel#getFactorLoading}.
 * <li>The function <i>f</i> is provided by the method {@link net.finmath.montecarlo.model.ProcessModel#applyStateSpaceTransform}.
 * </ul>
 * Here, &mu; and &lambda;<sub>j</sub> may depend on <i>X</i>, which allows to implement stochastic drifts (like in a LIBOR market model)
 * of local volatility models.
 *
 * <br>
 * Examples:
 * <ul>
 * 	<li>
 * 		The Black Scholes model can be modeled by S = X = Y (i.e. f is the identity)
 * 		and &mu;<sub>1</sub> = r S and &lambda;<sub>1,1</sub> = &sigma; S.
 * 	</li>
 * 	<li>
 * 		Alternatively, the Black Scholes model can be modeled by S = X = exp(Y) (i.e. f is exp)
 * 		and &mu;<sub>1</sub> = r - 0.5 &sigma; &sigma; and &lambda;<sub>1,1</sub> = &sigma;.
 * 	</li>
 * </ul>
 *
 * @author Christian Fries
 * @version 2.0
 */
public interface ProcessModel {

	/**
	 * Returns the model's date corresponding to the time discretization's \( t = 0 \).
	 *
	 * Note: Currently not all models provide a reference date. This will change in future versions.
	 *
	 * @return The model's date corresponding to the time discretization's \( t = 0 \).
	 */
	LocalDateTime getReferenceDate();

	/**
	 * Returns the number of components
	 *
	 * @return The number of components
	 */
	int getNumberOfComponents();

	/**
	 * Applies the state space transform <i>f<sub>i</sub></i> to the given state random variable
	 * such that <i>Y<sub>i</sub> &rarr; f<sub>i</sub>(Y<sub>i</sub>) =: X<sub>i</sub></i>.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param timeIndex The time index (related to the model times discretization).
	 * @param componentIndex The component index <i>i</i>.
	 * @param randomVariable The state random variable <i>Y<sub>i</sub></i>.
	 * @return New random variable holding the result of the state space transformation.
	 */
	RandomVariable applyStateSpaceTransform(MonteCarloProcess process, int timeIndex, int componentIndex, RandomVariable randomVariable);

	/**
	 * Applies the inverse state space transform <i>f<sup>-1</sup><sub>i</sub></i> to the given random variable
	 * such that <i>X<sub>i</sub> &rarr; f<sup>-1</sup><sub>i</sub>(X<sub>i</sub>) =: Y<sub>i</sub></i>.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param timeIndex The time index (related to the model times discretization).
	 * @param componentIndex The component index <i>i</i>.
	 * @param randomVariable The state random variable <i>X<sub>i</sub></i>.
	 * @return New random variable holding the result of the state space transformation.
	 */
	default RandomVariable applyStateSpaceTransformInverse(MonteCarloProcess process, int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		throw new UnsupportedOperationException("Inverse of statespace transform not set");
	}

	/**
	 * Returns the initial value of the state variable of the process <i>Y</i>, not to be
	 * confused with the initial value of the model <i>X</i> (which is the state space transform
	 * applied to this state value.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @return The initial value of the state variable of the process <i>Y(t=0)</i>.
	 */
	RandomVariable[] getInitialState(MonteCarloProcess process);

	/**
	 * Return the numeraire at a given time index.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param time The time <i>t</i> for which the numeraire <i>N(t)</i> should be returned.
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getNumeraire(MonteCarloProcess process, double time) throws CalculationException;

	/**
	 * This method has to be implemented to return the drift, i.e.
	 * the coefficient vector <br>
	 * <i>&mu; =  (&mu;<sub>1</sub>, ..., &mu;<sub>n</sub>)</i> such that <i>X = f(Y)</i> and <br>
	 * <i>dY<sub>j</sub> = &mu;<sub>j</sub> dt + &lambda;<sub>1,j</sub> dW<sub>1</sub> + ... + &lambda;<sub>m,j</sub> dW<sub>m</sub></i> <br>
	 * in an <i>m</i>-factor model. Here <i>j</i> denotes index of the component of the resulting
	 * process.
	 *
	 * Since the model is provided only on a time discretization, the method may also (should try to) return the drift
	 * as \( \frac{1}{t_{i+1}-t_{i}} \int_{t_{i}}^{t_{i+1}} \mu(\tau) \mathrm{d}\tau \).
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param timeIndex The time index (related to the model times discretization).
	 * @param realizationAtTimeIndex The given realization at timeIndex
	 * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null if no predictor is available.
	 * @return The drift or average drift from timeIndex to timeIndex+1, i.e. \( \frac{1}{t_{i+1}-t_{i}} \int_{t_{i}}^{t_{i+1}} \mu(\tau) \mathrm{d}\tau \) (or a suitable approximation).
	 */
	RandomVariable[] getDrift(MonteCarloProcess process, int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor);

	/**
	 * Returns the number of factors <i>m</i>, i.e., the number of independent Brownian drivers.
	 *
	 * @return The number of factors.
	 */
	int getNumberOfFactors();

	/**
	 * This method has to be implemented to return the factor loadings, i.e.
	 * the coefficient vector <br>
	 * <i>&lambda;<sub>j</sub> =  (&lambda;<sub>1,j</sub>, ..., &lambda;<sub>m,j</sub>)</i> such that <i>X = f(Y)</i> and <br>
	 * <i>dY<sub>j</sub> = &mu;<sub>j</sub> dt + &lambda;<sub>1,j</sub> dW<sub>1</sub> + ... + &lambda;<sub>m,j</sub> dW<sub>m</sub></i> <br>
	 * in an <i>m</i>-factor model. Here <i>j</i> denotes index of the component of the resulting
	 * process.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param timeIndex The time index (related to the model times discretization).
	 * @param componentIndex The index <i>j</i> of the driven component.
	 * @param realizationAtTimeIndex The realization of X at the time corresponding to timeIndex (in order to implement local and stochastic volatlity models).
	 * @return The factor loading for given factor and component.
	 */
	RandomVariable[] getFactorLoading(MonteCarloProcess process, int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex);

	/**
	 * Return a random variable initialized with a constant using the models random variable factory.
	 *
	 * @param value The constant value.
	 * @return A new random variable initialized with a constant value.
	 */
	RandomVariable getRandomVariableForConstant(double value);

	/**
	 * Returns a clone of this model where the specified properties have been modified.
	 *
	 * Note that there is no guarantee that a model reacts on a specification of a properties in the
	 * parameter map <code>dataModified</code>. If data is provided which is ignored by the model
	 * no exception may be thrown.
	 *
	 * @param dataModified Key-value-map of parameters to modify.
	 * @return A clone of this model (or this model if no parameter was modified).
	 * @throws CalculationException Thrown when the model could not be created.
	 */
	ProcessModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;
}
