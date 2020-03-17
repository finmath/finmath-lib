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
import net.finmath.time.TimeDiscretization;

/**
 * The interface for a model of a stochastic process <i>X</i> where
 * <i>X = f(Y)</i> and <br>
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
 * @version 1.0
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
	 * Returns the time discretization of the model parameters. It is not necessary that this time discretization agrees
	 * with the discretization of the stochactic process used in Abstract Process implementation.
	 *
	 * @return The time discretization
	 */
	TimeDiscretization getTimeDiscretization();

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
	 * @param componentIndex The component index <i>i</i>.
	 * @param randomVariable The state random variable <i>Y<sub>i</sub></i>.
	 * @return New random variable holding the result of the state space transformation.
	 */
	RandomVariable applyStateSpaceTransform(int componentIndex, RandomVariable randomVariable);

	default RandomVariable applyStateSpaceTransformInverse(final int componentIndex, final RandomVariable randomVariable) {
		throw new UnsupportedOperationException("Inverse of statespace transform not set");
	}

	/**
	 * Returns the initial value of the state variable of the process <i>Y</i>, not to be
	 * confused with the initial value of the model <i>X</i> (which is the state space transform
	 * applied to this state value.
	 *
	 * @return The initial value of the state variable of the process <i>Y(t=0)</i>.
	 */
	default RandomVariable[] getInitialState(MonteCarloProcess process) {
		return getInitialState();
	}

	/**
	 * Returns the initial value of the state variable of the process <i>Y</i>, not to be
	 * confused with the initial value of the model <i>X</i> (which is the state space transform
	 * applied to this state value.
	 *
	 * @return The initial value of the state variable of the process <i>Y(t=0)</i>.
	 */
	@Deprecated
	RandomVariable[] getInitialState();

	/**
	 * Return the numeraire at a given time index.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @param time The time <i>t</i> for which the numeraire <i>N(t)</i> should be returned.
	 * @return The numeraire at the specified time as <code>RandomVariableFromDoubleArray</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	default RandomVariable getNumeraire(MonteCarloProcess process, double time) throws CalculationException
	{
		return getNumeraire(time);
	}

	/**
	 * Return the numeraire at a given time index.
	 * Note: The random variable returned is a defensive copy and may be modified.
	 *
	 * @param time The time <i>t</i> for which the numeraire <i>N(t)</i> should be returned.
	 * @return The numeraire at the specified time as <code>RandomVariableFromDoubleArray</code>
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 * @deprecated Will be removed. Please use getNumeraire(process).get(time).
	 */
	@Deprecated
	default RandomVariable getNumeraire(double time) throws CalculationException {
		return getNumeraire(getProcess(), time);
	}

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
	default RandomVariable[] getDrift(MonteCarloProcess process, int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
		return getDrift(timeIndex, realizationAtTimeIndex, realizationPredictor);
	}

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
	 * @param timeIndex The time index (related to the model times discretization).
	 * @param realizationAtTimeIndex The given realization at timeIndex
	 * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null if no predictor is available.
	 * @return The drift or average drift from timeIndex to timeIndex+1, i.e. \( \frac{1}{t_{i+1}-t_{i}} \int_{t_{i}}^{t_{i+1}} \mu(\tau) \mathrm{d}\tau \) (or a suitable approximation).
	 * @deprecated Will be removed. Please use getDrift(process, ...).
	 */
	@Deprecated
	default RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
		throw new UnsupportedOperationException("This methods is deprecated, however, legacy code needs to override it.");
	}

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
	default RandomVariable[] getFactorLoading(MonteCarloProcess process, int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex) {
		return getFactorLoading(timeIndex, componentIndex, realizationAtTimeIndex);
	}

	/**
	 * This method has to be implemented to return the factor loadings, i.e.
	 * the coefficient vector <br>
	 * <i>&lambda;<sub>j</sub> =  (&lambda;<sub>1,j</sub>, ..., &lambda;<sub>m,j</sub>)</i> such that <i>X = f(Y)</i> and <br>
	 * <i>dY<sub>j</sub> = &mu;<sub>j</sub> dt + &lambda;<sub>1,j</sub> dW<sub>1</sub> + ... + &lambda;<sub>m,j</sub> dW<sub>m</sub></i> <br>
	 * in an <i>m</i>-factor model. Here <i>j</i> denotes index of the component of the resulting
	 * process.
	 *
	 * @param timeIndex The time index (related to the model times discretization).
	 * @param componentIndex The index <i>j</i> of the driven component.
	 * @param realizationAtTimeIndex The realization of X at the time corresponding to timeIndex (in order to implement local and stochastic volatlity models).
	 * @return The factor loading for given factor and component.
	 * @deprecated Will be removed. Please use getFactorLoading(process, ...).
	 */
	@Deprecated
	default RandomVariable[] getFactorLoading(int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException("This methods is deprecated, however, legacy code needs to override it.");
	}

	/**
	 * Return a random variable initialized with a constant using the models random variable factory.
	 *
	 * @param value The constant value.
	 * @return A new random variable initialized with a constant value.
	 */
	RandomVariable getRandomVariableForConstant(double value);

	/**
	 * Set the numerical scheme used to generate the stochastic process.
	 *
	 * The model needs the numerical scheme to calculate, e.g., the numeraire.
	 *
	 * @param process The process.
	 * @Deprecated Models will no longer hold references to processes.
	 */
	@Deprecated
	void setProcess(MonteCarloProcess process);

	/**
	 * Get the numerical scheme used to generate the stochastic process.
	 *
	 * The model needs the numerical scheme to calculate, e.g., the numeraire.
	 *
	 * @return the process
	 * @Deprecated Models will no longer hold references to processes.
	 */
	@Deprecated
	MonteCarloProcess getProcess();

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
