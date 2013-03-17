/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 18.11.2012
 */
package net.finmath.montecarlo.process;

import java.util.List;
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

	private ImmutableRandomVariableInterface[][]	discreteProcess = null;
	private ImmutableRandomVariableInterface[]		discreteProcessWeights;

	private Scheme		scheme = Scheme.EULER;

	// Uses locally for multi-threadded calculation.
	private ExecutorService executor;

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
		ImmutableRandomVariableInterface[] initialValue = getInitialValue();
		final RandomVariableInterface[] currentState = new RandomVariableInterface[numberOfComponents];
		for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
			currentState[componentIndex] = initialValue[componentIndex].getMutableCopy();
			//			discreteProcess[0][componentIndex] = (currentState[componentIndex].getMutableCopy());
			discreteProcess[0][componentIndex] = applyStateSpaceTransform(currentState[componentIndex].getMutableCopy());
		}

		int numberOfThreads = 2*Math.min(Math.max(Runtime.getRuntime().availableProcessors(),1),numberOfComponents);
		executor = Executors.newFixedThreadPool(numberOfThreads);
		
		// Evolve process
		for (int timeIndex2 = 1; timeIndex2 < getTimeDiscretization().getNumberOfTimeSteps()+1; timeIndex2++) {
			final int timeIndex = timeIndex2;
			// Generate process from timeIndex-1 to timeIndex
			final double deltaT = getTime(timeIndex) - getTime(timeIndex - 1);

			// Fetch drift vector
			final ImmutableRandomVariableInterface[] drift = getDrift(timeIndex - 1, discreteProcess[timeIndex - 1], null);

			// Calculate new realization
			Vector<Future<ImmutableRandomVariableInterface>> incrementFutures = new Vector<Future<ImmutableRandomVariableInterface>>(numberOfComponents);
			for (int componentIndex2 = 0; componentIndex2 < numberOfComponents; componentIndex2++) {
				final int componentIndex = componentIndex2;

				Callable<ImmutableRandomVariableInterface> worker = new  Callable<ImmutableRandomVariableInterface>() {
					public ImmutableRandomVariableInterface call() throws SolverException {
						ImmutableRandomVariableInterface	driftOfComponent	= drift[componentIndex];
						ImmutableRandomVariableInterface[]	factorLoadings		= getFactorLoadings(timeIndex - 1, componentIndex);

						// Check if the component process has stopped to evolve
						if (driftOfComponent == null && factorLoadings == null) {
							return currentState[componentIndex];
						}

						// Temp storage for variance and diffusion
						RandomVariable varianceOfComponent		= new RandomVariable(getTime(timeIndex - 1), 0.0);
						RandomVariable diffusionOfComponent		= new RandomVariable(getTime(timeIndex - 1), 0.0);

						// Generate values for diffusionOfComponent and varianceOfComponent 
						for (int factor = 0; factor < numberOfFactors; factor++) {
							ImmutableRandomVariableInterface factorLoading = factorLoadings[factor];
							ImmutableRandomVariableInterface brownianIncrement = brownianMotion.getBrownianIncrement(timeIndex - 1, factor);

							varianceOfComponent.addProduct(factorLoading, factorLoading);
							diffusionOfComponent.addProduct(factorLoading, brownianIncrement);
						}

						RandomVariableInterface previouseRealization = currentState[componentIndex];

						RandomVariableInterface increment = diffusionOfComponent;
						if(driftOfComponent != null) increment.addProduct(driftOfComponent, deltaT);

						// Store components
						return previouseRealization.add(increment);
						}
				};
				Future<ImmutableRandomVariableInterface> valueFuture = executor.submit(worker);
				incrementFutures.add(componentIndex, valueFuture);
			}

			discreteProcess[timeIndex] = getProcessFromStateSpace(incrementFutures);

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
					newRealization.add(driftAdjustment);

				} // End for(componentIndex)
//				discreteProcess[timeIndex] = getProcessFromStateSpace(currentState);
			} // End if(scheme == Scheme.PREDICTOR_CORRECTOR)


			// Set Monte-Carlo weights
			discreteProcessWeights[timeIndex] = discreteProcessWeights[timeIndex - 1];
		} // End for(timeIndex)

		// Transform new realization
		for (int timeIndex = 0; timeIndex < getTimeDiscretization().getNumberOfTimeSteps()+1; timeIndex++) {
			for (int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {
				//				discreteProcess[timeIndex][componentIndex] = applyStateSpaceTransform((RandomVariableInterface) discreteProcess[timeIndex][componentIndex]);
			}
		}
		executor.shutdown();
	}

    private RandomVariableInterface[] getProcessFromStateSpace(final Vector<Future<ImmutableRandomVariableInterface>> statesRandomVariable) {
    	RandomVariableInterface[] valuesRandomVariable = new RandomVariableInterface[statesRandomVariable.size()];

		// Transform new realization
    	Vector<Callable<RandomVariableInterface>> calculations = new Vector<Callable<RandomVariableInterface>>(statesRandomVariable.size());
		for (int componentIndex = 0; componentIndex < statesRandomVariable.size(); componentIndex++) {
			final int componentIndexInThread = componentIndex;
			Callable<RandomVariableInterface> callable = new Callable<RandomVariableInterface>() {
				public RandomVariableInterface call() throws InterruptedException, ExecutionException {
					return applyStateSpaceTransform(statesRandomVariable.get(componentIndexInThread).get().getMutableCopy());
				}
			};
			calculations.add(componentIndex, callable);
		}

		try {
			List<Future<RandomVariableInterface>> valuesRandomVariableFutures = executor.invokeAll(calculations);
			for (int componentIndex = 0; componentIndex < statesRandomVariable.size(); componentIndex++) {
				valuesRandomVariable[componentIndex]  = valuesRandomVariableFutures.get(componentIndex).get();
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return valuesRandomVariable;
    	
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

	private ImmutableRandomVariableInterface[] getFactorLoadings(int timeIndex, int componentIndex) {
		int numberOfFactors = getNumberOfFactors();
		ImmutableRandomVariableInterface[] factorLoadings = new ImmutableRandomVariableInterface[numberOfFactors];

		boolean hasFactorLoadings = false;
		for (int factorIndex = 0; factorIndex < numberOfFactors; factorIndex++) {
			factorLoadings[factorIndex] = this.getFactorLoading(timeIndex, factorIndex, componentIndex);
			if(factorLoadings[factorIndex] != null) hasFactorLoadings = true;
		}

		if(hasFactorLoadings)	return factorLoadings;
		else					return null;
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