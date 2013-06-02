/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 18.11.2012
 */
package net.finmath.montecarlo.process;

import java.util.Vector;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.optimizer.SolverException;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.RandomVariableMutableClone;

/**
 * This class is an abstract base class to implement a multi-dimensional multi-factor log-normal Ito process.
 * The dimension is called <code>numberOfComponents</code> here. The default for <code>numberOfFactors</code> is 1.
 * 
 * @author Christian Fries
 * @version 1.4
 */
public class ProcessEulerScheme extends AbstractProcess {

	public enum Scheme {
		EULER, PREDICTOR_CORRECTOR
	};

	private BrownianMotionInterface brownianMotion;

	private Scheme		scheme = Scheme.EULER;

	// Uses locally for multi-threadded calculation.
	private ExecutorService executor;

	/*
	 * The storage of the simulated stochastic process.
	 */
	private transient ImmutableRandomVariableInterface[][]	discreteProcess = null;
	private transient ImmutableRandomVariableInterface[]	discreteProcessWeights;

	/**
	 * @param brownianMotion The Brownian driver of the process
	 */
	public ProcessEulerScheme(BrownianMotionInterface brownianMotion) {
		super(brownianMotion.getTimeDiscretization());
		this.brownianMotion = brownianMotion;
	}

	/**
	 * This method returns the realization of the process at a certain time index.
	 * 
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of process realizations (on path)
	 */
	public RandomVariableInterface getProcessValue(int timeIndex, int componentIndex) {
		// Thread safe lazy initialization
		synchronized(this) {
			if (discreteProcess == null || discreteProcess.length == 0) {
				doPrecalculateProcess();
			}
		}

		// Return value of process
		return new RandomVariableMutableClone(discreteProcess[timeIndex][componentIndex]);
	}

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 * 
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights
	 */
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) {
		// Thread safe lazy initialization
		synchronized(this) {
			if (discreteProcessWeights == null || discreteProcessWeights.length == 0) {
				doPrecalculateProcess();
			}
		}

		// Return value of process
		return new RandomVariableMutableClone(discreteProcessWeights[timeIndex]);
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
		discreteProcess			= new ImmutableRandomVariableInterface[getTimeDiscretization().getNumberOfTimeSteps() + 1][getNumberOfComponents()];
		discreteProcessWeights	= new ImmutableRandomVariableInterface[getTimeDiscretization().getNumberOfTimeSteps() + 1];

		// Set initial Monte-Carlo weights
		discreteProcessWeights[0] = new RandomVariable(getTime(0), 1.0 / numberOfPaths);

		// Set initial value
		ImmutableRandomVariableInterface[] initialState = getInitialState();
		final RandomVariableInterface[] currentState = new RandomVariableInterface[numberOfComponents];
		for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
			currentState[componentIndex] = initialState[componentIndex].getMutableCopy();
			discreteProcess[0][componentIndex] = applyStateSpaceTransform(componentIndex, currentState[componentIndex].getMutableCopy());
		}

		/*
		 * Evolve the process using an Euler scheme.
		 * The evolution is performed multi-threadded.
		 * Each component vector runs in its own thread.
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
			ImmutableRandomVariableInterface[] drift = getDrift(timeIndex - 1, discreteProcess[timeIndex - 1], null);

			// Calculate new realization
			Vector<Future<ImmutableRandomVariableInterface>> discreteProcessAtCurrentTimeIndex = new Vector<Future<ImmutableRandomVariableInterface>>(numberOfComponents);
			discreteProcessAtCurrentTimeIndex.setSize(numberOfComponents);
			for (int componentIndex2 = 0; componentIndex2 < numberOfComponents; componentIndex2++) {
				final int componentIndex = componentIndex2;

				final ImmutableRandomVariableInterface	driftOfComponent	= drift[componentIndex];

				// Check if the component process has stopped to evolve
				if (driftOfComponent == null) continue;

				Callable<ImmutableRandomVariableInterface> worker = new  Callable<ImmutableRandomVariableInterface>() {
					public ImmutableRandomVariableInterface call() throws SolverException {
						ImmutableRandomVariableInterface[]	factorLoadings		= getFactorLoading(timeIndex - 1, componentIndex, discreteProcess[timeIndex - 1]);

						// Check if the component process has stopped to evolve
						if (factorLoadings == null) return null;

						// Temp storage for variance and diffusion
						RandomVariable diffusionOfComponent		= new RandomVariable(getTime(timeIndex - 1), 0.0);

						// Generate values for diffusionOfComponent and varianceOfComponent 
						for (int factor = 0; factor < numberOfFactors; factor++) {
							ImmutableRandomVariableInterface factorLoading		= factorLoadings[factor];
							ImmutableRandomVariableInterface brownianIncrement	= brownianMotion.getBrownianIncrement(timeIndex - 1, factor);

							diffusionOfComponent.addProduct(factorLoading, brownianIncrement);
						}

						RandomVariableInterface increment = diffusionOfComponent;
						if(driftOfComponent != null) increment.addProduct(driftOfComponent, deltaT);

						// Add increment to state and applyStateSpaceTransform
						currentState[componentIndex].add(increment);
						
						// Transform the state space to the value space and return it.
						return applyStateSpaceTransform(componentIndex, currentState[componentIndex].getMutableCopy());
					}
				};

				
				// The following line will add the result of the calculation to the vector discreteProcessAtCurrentTimeIndex
				discreteProcessAtCurrentTimeIndex.set(componentIndex, executor.submit(worker));
			}

			// Fetch results and move to discreteProcess[timeIndex]
			for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
				try {
					Future<ImmutableRandomVariableInterface> discreteProcessAtCurrentTimeIndexAndComponent = discreteProcessAtCurrentTimeIndex.get(componentIndex);
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

				ImmutableRandomVariableInterface[] driftWithPredictor = getDrift(timeIndex - 1, discreteProcess[timeIndex], null);

				for (int componentIndex = 0; componentIndex < getNumberOfComponents(); componentIndex++) {
					ImmutableRandomVariableInterface driftWithPredictorOfComponent		= driftWithPredictor[componentIndex];
					ImmutableRandomVariableInterface driftWithoutPredictorOfComponent	= drift[componentIndex];

					if (driftWithPredictorOfComponent == null || driftWithoutPredictorOfComponent == null) continue;

					// Get reference to newRealization
					RandomVariableInterface newRealization = (RandomVariableInterface)currentState[componentIndex];

					// newRealization[pathIndex] = newRealization[pathIndex] * Math.exp(0.5 * (driftWithPredictorOnPath - driftWithoutPredictorOnPath) * deltaT);
					RandomVariableInterface driftAdjustment = driftWithPredictorOfComponent.getMutableCopy().sub(driftWithoutPredictorOfComponent).div(2.0).mult(deltaT);
					currentState[componentIndex].add(driftAdjustment);

					// Reapply state space transform
					discreteProcess[timeIndex][componentIndex] = applyStateSpaceTransform(componentIndex, currentState[componentIndex].getMutableCopy());
				} // End for(componentIndex)
			} // End if(scheme == Scheme.PREDICTOR_CORRECTOR)

			// Set Monte-Carlo weights
			discreteProcessWeights[timeIndex] = discreteProcessWeights[timeIndex - 1];
		} // End for(timeIndex)

		executor.shutdown();
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
	public int getNumberOfPaths() {
		return this.brownianMotion.getNumberOfPaths();
	}

	/**
	 * @return Returns the numberOfFactors.
	 */
	public int getNumberOfFactors() {
		return this.brownianMotion.getNumberOfFactors();
	}

	/**
	 * @param seed The seed to set.
	 * @deprecated The class will soon be changed to be immutable
	 */
	public void setSeed(int seed) {
		// Create a new Brownian motion
		this.setBrownianMotion(new net.finmath.montecarlo.BrownianMotion(
				brownianMotion.getTimeDiscretization(), brownianMotion
				.getNumberOfFactors(), brownianMotion
				.getNumberOfPaths(), seed));
		// Force recalculation of the process
		this.reset();
	}

	/**
	 * @return Returns the Brownian motion used in the generation of the process
	 */
	public BrownianMotionInterface getBrownianMotion() {
		return brownianMotion;
	}

	/**
	 * @param brownianMotion The brownianMotion to set.
	 * @deprecated Do not use anymore. Processes should be immutable.
	 */
	public void setBrownianMotion(
			net.finmath.montecarlo.BrownianMotion brownianMotion) {
		this.brownianMotion = brownianMotion;
		// Force recalculation of the process
		this.reset();
	}

	/**
	 * @return Returns the scheme.
	 */
	public Scheme getScheme() {
		return scheme;
	}

	/**
	 * @param scheme The scheme to set.
	 * @deprecated Do not use anymore. Processes should be immutable.
	 */
	public void setScheme(Scheme scheme) {
		this.scheme = scheme;
		// Force recalculation of the process
		this.reset();
	}

	@Override
	public ProcessEulerScheme clone() {
		return new ProcessEulerScheme(getBrownianMotion());
	}

	@Override
	public Object getCloneWithModifiedSeed(int seed) {
		return new ProcessEulerScheme((BrownianMotionInterface)this.getBrownianMotion().getCloneWithModifiedSeed(seed));
	}
}