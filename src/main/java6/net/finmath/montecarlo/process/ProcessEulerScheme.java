/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 18.11.2012
 */
package net.finmath.montecarlo.process;

import java.util.Map;
import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.finmath.concurrency.FutureWrapper;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.IndependentIncrementsInterface;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements some numerical schemes for multi-dimensional multi-factor Ito process.
 * 
 * It features the standard Euler scheme and the standard predictor-corrector Euler scheme
 * for <i>Y</i>, then applies the <i>state space transform</i> \( X = f(Y) \). For the standard Euler scheme
 * the process Y is discretized as
 * \[
 * 	Y(t_{i+1}) = Y(t_{i}) + \mu(t_{i}) \Delta t_{i} + \sigma(t_{i}) \Delta W(t_{i}) \text{.} 
 * \]

 * 
 * Hence, using the <i>state space transform</i>, it is possible to create a log-Euler scheme, i.e.,
 * \[
 * 	X(t_{i+1}) = X(t_{i}) \cdot \exp\left( (\mu(t_{i}) - \frac{1}{2} \sigma(t_{i})^2) \Delta t_{i} + \sigma(t_{i}) \Delta W(t_{i}) \right) \text{.} 
 * \]
 * 
 * The dimension is called <code>numberOfComponents</code> here. The default for <code>numberOfFactors</code> is 1.
 * 
 * @author Christian Fries
 * @see AbstractProcessInterface The interface definition contains more details.
 * @version 1.4
 */
public class ProcessEulerScheme extends AbstractProcess {
	
	private static boolean isUseMultiThreadding;
	static {
		// Default value is true
		isUseMultiThreadding = Boolean.parseBoolean(System.getProperty("net.finmath.montecarlo.process.ProcessEulerScheme.isUseMultiThreadding","true"));
		System.out.println(isUseMultiThreadding);
	}

	public enum Scheme {
		EULER, PREDICTOR_CORRECTOR
	};

	private IndependentIncrementsInterface stochasticDriver;

	private Scheme		scheme = Scheme.EULER;

	// Used locally for multi-threadded calculation.
	private ExecutorService executor;

	/*
	 * The storage of the simulated stochastic process.
	 */
	private transient RandomVariableInterface[][]	discreteProcess = null;
	private transient RandomVariableInterface[]		discreteProcessWeights;


	/**
	 * Create an Euler discretization scheme.
	 * 
	 * @param stochasticDriver The stochastic driver of the process (e.g. a Brownian motion).
	 * @param scheme The scheme to use. See {@link Scheme}.
	 */
	public ProcessEulerScheme(IndependentIncrementsInterface stochasticDriver, Scheme scheme) {
		super(stochasticDriver.getTimeDiscretization());
		this.stochasticDriver = stochasticDriver;
		this.scheme = scheme;
	}

	/**
	 * Create an Euler discretization scheme.
	 * 
	 * @param stochasticDriver The stochastic driver of the process (e.g. a Brownian motion).
	 */
	public ProcessEulerScheme(IndependentIncrementsInterface stochasticDriver) {
		super(stochasticDriver.getTimeDiscretization());
		this.stochasticDriver = stochasticDriver;
	}

	/**
	 * This method returns the realization of the process at a certain time index.
	 * 
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of process realizations (on path)
	 */
	@Override
	public RandomVariableInterface getProcessValue(int timeIndex, int componentIndex) {
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
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) {
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
		if (discreteProcess != null && discreteProcess.length != 0)	return;

		final int numberOfPaths			= this.getNumberOfPaths();
		final int numberOfFactors		= this.getNumberOfFactors();
		final int numberOfComponents	= this.getNumberOfComponents();

		// Allocate Memory
		discreteProcess			= new RandomVariableInterface[getTimeDiscretization().getNumberOfTimeSteps() + 1][getNumberOfComponents()];
		discreteProcessWeights	= new RandomVariableInterface[getTimeDiscretization().getNumberOfTimeSteps() + 1];

		// Set initial Monte-Carlo weights
		discreteProcessWeights[0] = stochasticDriver.getRandomVariableForConstant(1.0 / numberOfPaths);

		// Set initial value
		RandomVariableInterface[] initialState = getInitialState();
		final RandomVariableInterface[] currentState = new RandomVariableInterface[numberOfComponents];
		for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
			currentState[componentIndex] = initialState[componentIndex];
			discreteProcess[0][componentIndex] = applyStateSpaceTransform(componentIndex, currentState[componentIndex]);
		}

		/*
		 * Evolve the process using an Euler scheme.
		 * The evolution is performed multi-threadded.
		 * Each component of the vector runs in its own thread.
		 */

		// We do not allocate more threads the twice the number of processors.
		int numberOfThreads = Math.min(Math.max(2 * Runtime.getRuntime().availableProcessors(),1),numberOfComponents);
		executor = Executors.newFixedThreadPool(numberOfThreads);

		// Evolve process
		for (int timeIndex2 = 1; timeIndex2 < getTimeDiscretization().getNumberOfTimeSteps()+1; timeIndex2++) {
			final int timeIndex = timeIndex2;
			// Generate process from timeIndex-1 to timeIndex
			final double deltaT = getTime(timeIndex) - getTime(timeIndex - 1);

			// Fetch drift vector
			RandomVariableInterface[] drift = getDrift(timeIndex - 1, discreteProcess[timeIndex - 1], null);

			// Calculate new realization
			Vector<Future<RandomVariableInterface>> discreteProcessAtCurrentTimeIndex = new Vector<Future<RandomVariableInterface>>(numberOfComponents);
			discreteProcessAtCurrentTimeIndex.setSize(numberOfComponents);
			for (int componentIndex2 = 0; componentIndex2 < numberOfComponents; componentIndex2++) {
				final int componentIndex = componentIndex2;

				final RandomVariableInterface	driftOfComponent	= drift[componentIndex];

				// Check if the component process has stopped to evolve
				if (driftOfComponent == null) continue;

				Callable<RandomVariableInterface> worker = new  Callable<RandomVariableInterface>() {
					public RandomVariableInterface call() throws SolverException {
						RandomVariableInterface[]	factorLoadings		= getFactorLoading(timeIndex - 1, componentIndex, discreteProcess[timeIndex - 1]);

						// Check if the component process has stopped to evolve
						if (factorLoadings == null) return null;

						// Temp storage for variance and diffusion
						RandomVariableInterface diffusionOfComponent		= stochasticDriver.getRandomVariableForConstant(0.0);

						// Generate values for diffusionOfComponent and varianceOfComponent 
						for (int factor = 0; factor < numberOfFactors; factor++) {
							RandomVariableInterface factorLoading		= factorLoadings[factor];
							if(factorLoading == null) continue;
							
							RandomVariableInterface brownianIncrement	= stochasticDriver.getIncrement(timeIndex - 1, factor);

							diffusionOfComponent = diffusionOfComponent.addProduct(factorLoading, brownianIncrement);
						}

						RandomVariableInterface increment = diffusionOfComponent;
						if(driftOfComponent != null) increment = increment.addProduct(driftOfComponent, deltaT);

						// Add increment to state and applyStateSpaceTransform
						currentState[componentIndex] = currentState[componentIndex].add(increment);
						
						// Transform the state space to the value space and return it.
						return applyStateSpaceTransform(componentIndex, currentState[componentIndex]);
					}
				};


				/*
				 * Optional multi-threadding (asyncronous calculation of the components)
				 */
				Future<RandomVariableInterface> result = null;
				if(isUseMultiThreadding) result = executor.submit(worker);
				else {
					try {
						result = new FutureWrapper<RandomVariableInterface>(worker.call());
					} catch (Exception e) {}
				}
				
				// The following line will add the result of the calculation to the vector discreteProcessAtCurrentTimeIndex
				discreteProcessAtCurrentTimeIndex.set(componentIndex, result);
			}

			// Fetch results and move to discreteProcess[timeIndex]
			for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
				try {
					Future<RandomVariableInterface> discreteProcessAtCurrentTimeIndexAndComponent = discreteProcessAtCurrentTimeIndex.get(componentIndex);
					if(discreteProcessAtCurrentTimeIndexAndComponent != null)	discreteProcess[timeIndex][componentIndex] = discreteProcessAtCurrentTimeIndexAndComponent.get();
					else														discreteProcess[timeIndex][componentIndex] = null;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (scheme == Scheme.PREDICTOR_CORRECTOR) {
				// Apply corrector step to realizations at next time step

				RandomVariableInterface[] driftWithPredictor = getDrift(timeIndex - 1, discreteProcess[timeIndex], null);

				for (int componentIndex = 0; componentIndex < getNumberOfComponents(); componentIndex++) {
					RandomVariableInterface driftWithPredictorOfComponent		= driftWithPredictor[componentIndex];
					RandomVariableInterface driftWithoutPredictorOfComponent	= drift[componentIndex];

					if (driftWithPredictorOfComponent == null || driftWithoutPredictorOfComponent == null) continue;

					// Calculated the predictor corrector drift adjustment
					RandomVariableInterface driftAdjustment = driftWithPredictorOfComponent.sub(driftWithoutPredictorOfComponent).div(2.0).mult(deltaT);

					// Add drift adjustment
					currentState[componentIndex] = currentState[componentIndex].add(driftAdjustment);

					// Re-apply state space transform
					discreteProcess[timeIndex][componentIndex] = applyStateSpaceTransform(componentIndex, currentState[componentIndex]);
				} // End for(componentIndex)
			} // End if(scheme == Scheme.PREDICTOR_CORRECTOR)

			// Set Monte-Carlo weights
			discreteProcessWeights[timeIndex] = discreteProcessWeights[timeIndex - 1];
		} // End for(timeIndex)

		try {
			executor.shutdown();
		}
		catch(SecurityException e) {
			// @TODO Improve exception handling here
		}
	}


	/**
	 * Reset all precalculated values
	 */
	private synchronized void reset() {
		this.discreteProcess = null;
		this.discreteProcessWeights = null;
	}

	/**
	 * @return Returns the numberOfPaths.
	 */
	@Override
	public int getNumberOfPaths() {
		return this.stochasticDriver.getNumberOfPaths();
	}

	/**
	 * @return Returns the numberOfFactors.
	 */
	@Override
	public int getNumberOfFactors() {
		return this.stochasticDriver.getNumberOfFactors();
	}

	/**
	 * @return Returns the independent increments interface used in the generation of the process
	 */
	@Override
	public IndependentIncrementsInterface getStochasticDriver() {
		return stochasticDriver;
	}

	/**
	 * @return Returns the Brownian motion used in the generation of the process
	 */
	@Override
	public BrownianMotionInterface getBrownianMotion() {
		return (BrownianMotionInterface)stochasticDriver;
	}

	/**
	 * @return Returns the scheme.
	 */
	public Scheme getScheme() {
		return scheme;
	}

	@Override
	public ProcessEulerScheme clone() {
		return new ProcessEulerScheme(getStochasticDriver(), scheme);
	}

	@Override
	public AbstractProcessInterface getCloneWithModifiedData(Map<String, Object> dataModified) {
		// @TODO Implement cloning with modified properties
		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public Object getCloneWithModifiedSeed(int seed) {
		return new ProcessEulerScheme(getBrownianMotion().getCloneWithModifiedSeed(seed));
	}

	@Override
	public String toString() {
		return "ProcessEulerScheme [stochasticDriver=" + stochasticDriver + ", scheme=" + scheme + ", executor="
				+ executor + "]";
	}

}