/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 18.11.2012
 */
package net.finmath.montecarlo.process;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.finmath.concurrency.FutureWrapper;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements some numerical schemes for multi-dimensional multi-factor Ito process.
 *
 * <p>
 * It features the standard Euler scheme and the standard predictor-corrector Euler scheme
 * for <i>Y</i>, then applies the <i>state space transform</i> \( X = f(Y) \).
 * </p>
 *
 * <p>
 * For the standard Euler scheme the process \( Y = (Y_{1},\ldots,Y_{d}) \) is discretized as
 * \[
 * 	Y_{j}(t_{i+1}) = Y_{j}(t_{i}) + \mu_{j}(t_{i}) \Delta t_{i} + \lambda_{1,j}(t_{i}) \Delta W_{1}(t_{i}) + \ldots + \lambda_{m,j} \Delta W_{m} \text{.}
 * \]
 * </p>
 *
 * The parameters have to be provided by a {@link net.finmath.montecarlo.model.ProcessModel}:
 * <ul>
 * 	<li> \( f \) - <code>applyStateSpaceTransform</code> </li>
 * 	<li> \( Y(t_{0}) \) - <code>getInitialState</code> </li>
 * 	<li> \( \mu \) - <code>getDrift</code> </li>
 * 	<li> \( \lambda_{j} \) - <code>getFactorLoading</code> </li>
 * </ul>
 *
 * <p>
 * Using the <i>state space transform</i> \( (t,x) \mapsto \exp(x) \), it is possible to create a log-Euler scheme, i.e.,
 * \[
 * 	X(t_{i+1}) = X(t_{i}) \cdot \exp\left( (r(t_{i}) - \frac{1}{2} \sigma(t_{i})^2) \Delta t_{i} + \sigma(t_{i}) \Delta W(t_{i}) \right) \text{.}
 * \]
 * for a process \( \mathrm{d} X = r X \mathrm{d}t + \sigma X \mathrm{d}W \).
 * </p>
 *
 * The dimension \( d \) is called <code>numberOfComponents</code> here.
 * The value \( m \) is called <code>numberOfFactors</code> here.
 * The default for <code>numberOfFactors</code> is 1.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.model.ProcessModel The interface definition contains more details.
 * @version 1.4
 */
public class EulerSchemeFromProcessModel extends MonteCarloProcessFromProcessModel {

	private static boolean isUseMultiThreadding;
	static {
		// Default value is true
		isUseMultiThreadding = Boolean.parseBoolean(System.getProperty("net.finmath.montecarlo.process.EulerSchemeFromProcessModel.isUseMultiThreadding","true"));
	}

	public enum Scheme {
		EULER,
		PREDICTOR_CORRECTOR,
		EULER_FUNCTIONAL,
		PREDICTOR_CORRECTOR_FUNCTIONAL
	}

	private final IndependentIncrements stochasticDriver;

	private Scheme		scheme = Scheme.EULER_FUNCTIONAL;

	// Used locally for multi-threadded calculation.
	private ExecutorService executor;

	/*
	 * The storage of the simulated stochastic process.
	 */
	private transient RandomVariable[][]	discreteProcess = null;
	private transient RandomVariable[]		discreteProcessWeights;


	/**
	 * Create an Euler discretization scheme.
	 *
	 * @param model The model (the SDE specifcation) used to generate the (sampling of the) stochastic process.
	 * @param stochasticDriver The stochastic driver of the process (e.g. a Brownian motion).
	 * @param scheme The scheme to use. See {@link Scheme}.
	 */
	public EulerSchemeFromProcessModel(final ProcessModel model, final IndependentIncrements stochasticDriver, final Scheme scheme) {
		super(stochasticDriver.getTimeDiscretization(), model);
		this.stochasticDriver = stochasticDriver;
		this.scheme = scheme;
	}

	/**
	 * Create an Euler discretization scheme.
	 *
	 * @param model The model (the SDE specification) used to generate the (sampling of the) stochastic process.
	 * @param stochasticDriver The stochastic driver of the process (e.g. a Brownian motion).
	 */
	public EulerSchemeFromProcessModel(final ProcessModel model, final IndependentIncrements stochasticDriver) {
		super(stochasticDriver.getTimeDiscretization(), model);
		this.stochasticDriver = stochasticDriver;
	}

	/**
	 * This method returns the realization of the process at a certain time index.
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of process realizations (on path)
	 */
	@Override
	public RandomVariable getProcessValue(final int timeIndex, final int componentIndex) {
		// Thread safe lazy initialization
		synchronized(this) {
			if (discreteProcess == null || discreteProcess.length == 0) {
				doPrecalculateProcess();
			}
		}

		if(discreteProcess[timeIndex][componentIndex] == null) {
			throw new NullPointerException("Generation of process component " + componentIndex + " at time index " + timeIndex + " failed. Likely due to out of memory");
		}

		// Return value of process
		return discreteProcess[timeIndex][componentIndex];
	}

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights
	 */
	@Override
	public RandomVariable getMonteCarloWeights(final int timeIndex) {
		// Thread safe lazy initialization
		synchronized(this) {
			if (discreteProcessWeights == null || discreteProcessWeights.length == 0) {
				doPrecalculateProcess();
			}
		}

		// Return value of process
		return discreteProcessWeights[timeIndex];
	}

	/**
	 * Calculates the whole (discrete) process.
	 */
	private void doPrecalculateProcess() {
		if (discreteProcess != null && discreteProcess.length != 0) {
			return;
		}

		final int numberOfPaths			= this.getNumberOfPaths();
		final int numberOfFactors		= this.getNumberOfFactors();
		final int numberOfComponents	= this.getNumberOfComponents();

		// Allocate Memory
		discreteProcess			= new RandomVariable[getTimeDiscretization().getNumberOfTimeSteps() + 1][getNumberOfComponents()];
		discreteProcessWeights	= new RandomVariable[getTimeDiscretization().getNumberOfTimeSteps() + 1];

		// Set initial Monte-Carlo weights
		discreteProcessWeights[0] = stochasticDriver.getRandomVariableForConstant(1.0 / numberOfPaths);

		// Set initial value
		final RandomVariable[] initialState = getInitialState();
		final RandomVariable[] currentState = new RandomVariable[numberOfComponents];
		for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
			currentState[componentIndex] = initialState[componentIndex];
			discreteProcess[0][componentIndex] = applyStateSpaceTransform(0, componentIndex, currentState[componentIndex]);
		}

		/*
		 * Evolve the process using an Euler scheme.
		 * The evolution is performed multi-threadded.
		 * Each component of the vector runs in its own thread.
		 */
		executor = Executors.newCachedThreadPool();

		// Evolve process
		for (int timeIndex2 = 1; timeIndex2 < getTimeDiscretization().getNumberOfTimeSteps()+1; timeIndex2++) {
			final int timeIndex = timeIndex2;
			// Generate process from timeIndex-1 to timeIndex
			final double deltaT = getTime(timeIndex) - getTime(timeIndex - 1);

			// Fetch drift vector
			final RandomVariable[] drift;
			try {
				drift = getDrift(timeIndex - 1, discreteProcess[timeIndex - 1], null);
			}
			catch(final Exception e) {
				throw new RuntimeException(e + " - drift calculaton failed at time index " + timeIndex + " (time=" + getTime(timeIndex - 1) + ") . See cause of this exception for details.", e);
			}

			// Fetch brownianIncrement vector
			final RandomVariable[] brownianIncrement	= stochasticDriver.getIncrement(timeIndex - 1);

			// Calculate new realization
			final ArrayList<Future<RandomVariable>> discreteProcessAtCurrentTimeIndex = new ArrayList<>(numberOfComponents);
			for (int componentIndex2 = 0; componentIndex2 < numberOfComponents; componentIndex2++) {
				final int componentIndex = componentIndex2;

				final RandomVariable	driftOfComponent	= drift[componentIndex];

				// Check if the component process has stopped to evolve
				if (driftOfComponent == null) {
					discreteProcessAtCurrentTimeIndex.add(componentIndex, null);
					continue;
				}

				final Callable<RandomVariable> worker = new  Callable<RandomVariable>() {
					@Override
					public RandomVariable call() {
						if(scheme == Scheme.EULER_FUNCTIONAL || scheme == Scheme.PREDICTOR_CORRECTOR_FUNCTIONAL) {
							currentState[componentIndex] = applyStateSpaceTransformInverse(timeIndex - 1, componentIndex, discreteProcess[timeIndex - 1][componentIndex]);
						}

						final RandomVariable[]	factorLoadings		= getFactorLoading(timeIndex - 1, componentIndex, discreteProcess[timeIndex - 1]);

						// Check if the component process has stopped to evolve
						if (factorLoadings == null) {
							return null;
						}

						// Apply drift
						if(driftOfComponent != null) {
							currentState[componentIndex] = currentState[componentIndex].addProduct(driftOfComponent, deltaT);
						}

						// Apply diffusion
						currentState[componentIndex] = currentState[componentIndex].addSumProduct(factorLoadings, brownianIncrement);

						// Transform the state space to the value space and return it.
						return applyStateSpaceTransform(timeIndex, componentIndex, currentState[componentIndex]);
					}
				};


				/*
				 * Optional multi-threadding (asyncronous calculation of the components)
				 */
				Future<RandomVariable> result = null;
				try {
					if(isUseMultiThreadding) {
						result = executor.submit(worker);
					} else {
						result = new FutureWrapper<>(worker.call());
					}
				} catch (final Exception e) {
					throw new RuntimeException("Euler step failed at time index " + timeIndex + " (time=" + getTime(timeIndex) + "). See cause of this exception for details.", e);
				}

				// The following line will add the result of the calculation to the vector discreteProcessAtCurrentTimeIndex
				discreteProcessAtCurrentTimeIndex.add(componentIndex, result);
			}

			// Fetch results and move to discreteProcess[timeIndex]
			for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
				try {
					final Future<RandomVariable> discreteProcessAtCurrentTimeIndexAndComponent = discreteProcessAtCurrentTimeIndex.get(componentIndex);
					if(discreteProcessAtCurrentTimeIndexAndComponent != null) {
						discreteProcess[timeIndex][componentIndex] = discreteProcessAtCurrentTimeIndexAndComponent.get().cache();
					} else {
						discreteProcess[timeIndex][componentIndex] = discreteProcess[timeIndex-1][componentIndex];
					}
				} catch (final InterruptedException | ExecutionException e) {
					throw new RuntimeException("Euler step failed at time index " + timeIndex + " (time=" + getTime(timeIndex) + "). See cause of this exception for details.", e.getCause());
				}
			}

			if (scheme == Scheme.PREDICTOR_CORRECTOR || scheme == Scheme.PREDICTOR_CORRECTOR_FUNCTIONAL) {
				// Apply corrector step to realizations at next time step

				final RandomVariable[] driftWithPredictor = getDrift(timeIndex - 1, discreteProcess[timeIndex], null);

				for (int componentIndex = 0; componentIndex < getNumberOfComponents(); componentIndex++) {
					final RandomVariable driftWithPredictorOfComponent		= driftWithPredictor[componentIndex];
					final RandomVariable driftWithoutPredictorOfComponent	= drift[componentIndex];

					if (driftWithPredictorOfComponent == null || driftWithoutPredictorOfComponent == null) {
						continue;
					}

					// Calculated the predictor corrector drift adjustment
					final RandomVariable driftAdjustment = driftWithPredictorOfComponent.sub(driftWithoutPredictorOfComponent).div(2.0).mult(deltaT);

					// Add drift adjustment
					currentState[componentIndex] = currentState[componentIndex].add(driftAdjustment);

					// Re-apply state space transform
					discreteProcess[timeIndex][componentIndex] = applyStateSpaceTransform(timeIndex, componentIndex, currentState[componentIndex]);
				} // End for(componentIndex)
			} // End if(scheme == Scheme.PREDICTOR_CORRECTOR)

			// Set Monte-Carlo weights
			discreteProcessWeights[timeIndex] = discreteProcessWeights[timeIndex - 1];
		} // End for(timeIndex)

		try {
			executor.shutdown();
		}
		catch(final SecurityException e) {
			// @TODO Improve exception handling here
		}
	}

	/**
	 * @return Returns the numberOfPaths.
	 */
	@Override
	public int getNumberOfPaths() {
		return stochasticDriver.getNumberOfPaths();
	}

	/**
	 * @return Returns the numberOfFactors.
	 */
	@Override
	public int getNumberOfFactors() {
		return stochasticDriver.getNumberOfFactors();
	}

	/**
	 * @return Returns the independent increments interface used in the generation of the process
	 */
	@Override
	public IndependentIncrements getStochasticDriver() {
		return stochasticDriver;
	}

	/**
	 * @return Returns the scheme.
	 */
	public Scheme getScheme() {
		return scheme;
	}

	@Override
	public EulerSchemeFromProcessModel clone() {
		return new EulerSchemeFromProcessModel(getModel(), getStochasticDriver(), scheme);
	}

	@Override
	public MonteCarloProcess getCloneWithModifiedModel(ProcessModel model) {
		return new EulerSchemeFromProcessModel(model, getStochasticDriver(), scheme);
	}

	@Override
	public MonteCarloProcess getCloneWithModifiedData(final Map<String, Object> dataModified) {
		final ProcessModel newModel = (ProcessModel) dataModified.getOrDefault("model", getModel());

		if(dataModified.containsKey("seed") && dataModified.containsKey("stochasticDriver")) {
			throw new IllegalArgumentException("Simultaneous specification of stochasticDriver and seed.");
		}

		final IndependentIncrements newStochasticDriver;
		if(dataModified.containsKey("seed")) {
			newStochasticDriver = getStochasticDriver().getCloneWithModifiedSeed((int)dataModified.get("seed"));
		}
		else if(dataModified.containsKey("stochasticDriver")) {
			newStochasticDriver = (IndependentIncrements) dataModified.getOrDefault("stochasticDriver", stochasticDriver);
		}
		else {
			newStochasticDriver = stochasticDriver;
		}

		final Scheme newScheme = (Scheme) dataModified.getOrDefault("scheme", scheme);

		return new EulerSchemeFromProcessModel(newModel, newStochasticDriver, newScheme);
	}

	@Override
	public Object getCloneWithModifiedSeed(final int seed) {
		return new EulerSchemeFromProcessModel(getModel(), getStochasticDriver().getCloneWithModifiedSeed(seed));
	}

	@Override
	public String toString() {
		return "EulerSchemeFromProcessModel [stochasticDriver=" + stochasticDriver + ", scheme=" + scheme + ", executor="
				+ executor + "]";
	}
}
