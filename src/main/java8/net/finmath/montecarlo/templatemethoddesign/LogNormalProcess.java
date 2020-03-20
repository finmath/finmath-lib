/*
 * Created on 19.01.2004
 */
package net.finmath.montecarlo.templatemethoddesign;

import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class is an abstract base class to implement an Euler scheme of a multi-dimensional multi-factor log-normal Ito process.
 * The dimension is called <code>numberOfComponents</code> here. The default for <code>numberOfFactors</code> is 1.
 *
 * @author Christian Fries
 * @date 19.04.2008
 * @version 1.5
 */
public abstract class LogNormalProcess {

	public enum Scheme { EULER, PREDICTOR_USING_EULERSTEP, PREDICTOR_USING_LASTREALIZATION }

	private BrownianMotion	brownianMotion;

	private RandomVariable[][]     discreteProcess         = null;
	private RandomVariable[]       discreteProcessWeights  = null;

	private final TimeDiscretization	timeDiscretization;
	private final int			numberOfComponents;
	private final int			numberOfFactors;
	private final int			numberOfPaths;

	private Scheme		scheme = Scheme.EULER;

	/**
	 * Create a log normal process.
	 *
	 * @param numberOfComponents The number of components (scalar processes).
	 * @param brownianMotion A Brownian motion
	 */
	public LogNormalProcess(
			final int numberOfComponents,
			final BrownianMotion brownianMotion) {
		super();
		timeDiscretization	= brownianMotion.getTimeDiscretization();
		this.numberOfComponents	= numberOfComponents;
		numberOfFactors	= brownianMotion.getNumberOfFactors();
		numberOfPaths		= brownianMotion.getNumberOfPaths();
		this.brownianMotion		= brownianMotion;
	}

	/**
	 * Create a simulation of log normal process.
	 *
	 * @param timeDiscretization The time discretization of the process.
	 * @param numberOfComponents The number of components (the dimension of the process).
	 * @param numberOfPaths The number of path of the simulation.
	 */
	public LogNormalProcess(
			final TimeDiscretization timeDiscretization,
			final int numberOfComponents,
			final int numberOfPaths) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfComponents = numberOfComponents;
		numberOfFactors = 1;
		this.numberOfPaths = numberOfPaths;

		// Create a Brownian motion
		brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(
				timeDiscretization,
				numberOfFactors,
				numberOfPaths,
				3141 /* seed */);
	}

	/**
	 * Create a simulation of log normal process.
	 *
	 * @param timeDiscretization The time discretization of the process.
	 * @param numberOfComponents The number of components (the dimension of the process).
	 * @param numberOfFactors The number of factors of the process.
	 * @param numberOfPaths The number of path of the simulation.
	 * @param seed The seed of the underlying random number generator.
	 */
	public LogNormalProcess(
			final TimeDiscretization timeDiscretization,
			final int numberOfComponents,
			final int numberOfFactors,
			final int numberOfPaths,
			final int seed) {
		super();
		this.timeDiscretization	= timeDiscretization;
		this.numberOfComponents	= numberOfComponents;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;

		// Create a Brownian motion
		brownianMotion = new net.finmath.montecarlo.BrownianMotionLazyInit(
				timeDiscretization,
				numberOfFactors,
				numberOfPaths,
				seed);
	}


	public abstract RandomVariable[]	getInitialValue();
	public abstract RandomVariable	getDrift(int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor);

	/**
	 * Get the the drift.
	 *
	 * @param timeIndex The time index (related to the model times discretization).
	 * @param realizationAtTimeIndex The given realization at timeIndex
	 * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null of no predictor is available.
	 * @return The (average) drift from timeIndex to timeIndex+1
	 */
	public RandomVariable[] getDrift(final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {

		final RandomVariable[] drift = new RandomVariable[getNumberOfComponents()];

		/*
		 * We implemented several different methods to calculate the drift
		 */
		for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
			drift[componentIndex] = getDrift(timeIndex, componentIndex, realizationAtTimeIndex, realizationPredictor);
		}
		return drift;
	}

	/**
	 * This method should be overwritten and return the factor loading, i.e.
	 * the coeffient &lambda;(i,j) such that <br>
	 * dS(j) = (...) dt + S(j) * (&lambda;(1,j) dW(1) + ... + &lambda;(m,j) dW(m)) <br>
	 * in an m-factor model. Here j denotes index of the component of the resulting
	 * log-normal process and i denotes the index of the factor.<p>
	 * Overwrite this method if you would like to implement a multi factor model.
	 *
	 * @param timeIndex The time index of the simulation time discretization.
	 * @param factor The factor index.
	 * @param component The component index.
	 * @param realizationAtTimeIndex The realization at the current time index.
	 * @return factor loading for given factor and component
	 */
	public abstract RandomVariable getFactorLoading(int timeIndex, int factor, int component, RandomVariable[] realizationAtTimeIndex);

	/**
	 * This method returns the realization of the process at a certain time index.
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of process realizations (on path)
	 */
	public RandomVariable[] getProcessValue(final int timeIndex)
	{
		// Thread safe lazy initialization
		synchronized(this) {
			if(discreteProcess == null || discreteProcess.length == 0)
			{
				doPrecalculateProcess();
			}
		}

		// Return value of process
		return discreteProcess[timeIndex];
	}

	/**
	 * This method returns the realization of the process at a certain time index.
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @param componentIndex Component of the process vector
	 * @return A vector of process realizations (on path)
	 */
	public RandomVariable getProcessValue(final int timeIndex, final int componentIndex)
	{
		if(timeIndex == 0) {
			return getInitialValue()[componentIndex];
		}

		// Lazy initialization, synchronized for thread safety
		synchronized(this) {
			if(discreteProcess == null || discreteProcess.length == 0) {
				doPrecalculateProcess();
			}
		}

		// Return value of process
		return discreteProcess[timeIndex][componentIndex];
	}

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 */
	public RandomVariable getMonteCarloWeights(final int timeIndex)
	{
		// Lazy initialization, synchronized for thread safety
		synchronized(this) {
			if(discreteProcessWeights == null || discreteProcessWeights.length == 0) {
				doPrecalculateProcess();
			}
		}

		// Return value of process
		return discreteProcessWeights[timeIndex];
	}

	/**
	 * Calculates the whole (discrete) process.
	 */
	private void doPrecalculateProcess()
	{
		if(discreteProcess != null && discreteProcess.length != 0) {
			return;
		}

		// Allocate Memory
		discreteProcess			= new RandomVariable[timeDiscretization.getNumberOfTimeSteps()+1][numberOfComponents];
		discreteProcessWeights	= new RandomVariable[getTimeDiscretization().getNumberOfTimeSteps()+1];

		// Set initial Monte-Carlo weights
		discreteProcessWeights[0] = new RandomVariableFromDoubleArray(0.0, 1.0/numberOfPaths);

		// Store components
		discreteProcess[0] = getInitialValue();

		// Evolve process
		for(int timeIndex = 1; timeIndex < timeDiscretization.getNumberOfTimeSteps()+1; timeIndex++)
		{
			// Generate process at timeIndex
			final double deltaT = timeDiscretization.getTime(timeIndex) - timeDiscretization.getTime(timeIndex-1);

			// Temp storage for variance and diffusion
			final RandomVariable[] variance   = new RandomVariable[numberOfComponents];
			final RandomVariable[] diffusion  = new RandomVariable[numberOfComponents];

			// Calculate new realization
			for(int componentIndex = numberOfComponents-1; componentIndex >= 0; componentIndex--)
			{
				// Calculate diffusion

				// Temp storage for variance and diffusion
				RandomVariable varianceOfComponent  = new RandomVariableFromDoubleArray(getTime(timeIndex-1),0.0);
				RandomVariable diffusionOfComponent = new RandomVariableFromDoubleArray(getTime(timeIndex-1),0.0);

				// Generate values for diffusionOfComponent and varianceOfComponent
				for(int factor=0; factor<numberOfFactors; factor++) {
					final RandomVariable factorLoading       = getFactorLoading(timeIndex-1, factor, componentIndex, null);
					final RandomVariable brownianIncrement   = brownianMotion.getBrownianIncrement(timeIndex-1,factor);

					varianceOfComponent = varianceOfComponent.addProduct(factorLoading, factorLoading);
					diffusionOfComponent = diffusionOfComponent.addProduct(factorLoading, brownianIncrement);
				}

				variance[componentIndex] = varianceOfComponent;
				diffusion[componentIndex] = diffusionOfComponent;
			}

			RandomVariable[] drift;
			if(scheme == Scheme.PREDICTOR_USING_LASTREALIZATION) {
				drift = getDrift(timeIndex-1, discreteProcess[timeIndex-1], discreteProcess[timeIndex]);
			} else {
				drift = getDrift(timeIndex-1, discreteProcess[timeIndex-1], null                      );
			}

			// Calculate drift
			for(int componentIndex = numberOfComponents-1; componentIndex >= 0; componentIndex--)
			{
				final RandomVariable driftOfComponent  	= drift[componentIndex];
				final RandomVariable varianceOfComponent			= variance[componentIndex];
				final RandomVariable diffusionOfComponent		= diffusion[componentIndex];

				if(driftOfComponent == null) {
					discreteProcess[timeIndex][componentIndex] = discreteProcess[timeIndex-1][componentIndex];
					continue;
				}

				// Allocate memory for on path-realizations
				final double[] newRealization			= new double[numberOfPaths];

				// Euler Scheme
				final RandomVariable previouseRealization		= discreteProcess[timeIndex-1][componentIndex];

				// Generate values
				for(int pathIndex = 0; pathIndex < numberOfPaths; pathIndex++)
				{
					final double previousValue	= previouseRealization.get(pathIndex);
					final double driftOnPath		= driftOfComponent.get(pathIndex);
					final double varianceOnPath	= varianceOfComponent.get(pathIndex);
					final double diffusionOnPath	= diffusionOfComponent.get(pathIndex);

					// The scheme
					newRealization[pathIndex]	 = previousValue * Math.exp(driftOnPath * deltaT - 0.5 * varianceOnPath * deltaT + diffusionOnPath);
				}

				// Store components
				discreteProcess[timeIndex][componentIndex] = new RandomVariableFromDoubleArray(getTime(timeIndex),newRealization);
			}

			if(scheme == Scheme.PREDICTOR_USING_EULERSTEP) {
				final RandomVariable[] newRealization = new RandomVariableFromDoubleArray[numberOfComponents];

				// Note: This is actually more than a predictor corrector: The drift of componentIndex already uses the corrected predictor from the previous components
				drift = getDrift(timeIndex-1, discreteProcess[timeIndex-1], discreteProcess[timeIndex]);

				// Apply corrector step to realizations at next time step
				for(int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++)
				{
					final RandomVariable driftOfComponent	= drift[componentIndex];
					final RandomVariable varianceOfComponent			= variance[componentIndex];
					final RandomVariable diffusionOfComponent		= diffusion[componentIndex];

					// Euler Scheme with corrected drift
					final RandomVariable previouseRealization 	= discreteProcess[timeIndex-1][componentIndex];

					// The scheme
					// newValue = previousValue * Math.exp(driftValue * deltaT - 0.5 * varianceOnPath * deltaT + diffusionOnPath);
					newRealization[componentIndex] = previouseRealization.mult((driftOfComponent.mult(deltaT).sub(varianceOfComponent.mult(0.5 * deltaT)).add(diffusionOfComponent)).exp());
				} // End for(componentIndex)

				// Store predictor-corrector corrected process.
				discreteProcess[timeIndex] = newRealization;
			} // End if(scheme == LogNormalProcess.SCHEME_PREDICTOR_USES_EULER)

			// Set Monte-Carlo weights (since there is no Monte-Carlo weighting, the weights remain the same (namely 1.0/numberOfPaths).
			discreteProcessWeights[timeIndex] = discreteProcessWeights[timeIndex-1];
		} // End for(timeIndex)
	}


	/**
	 * @return Returns the numberOfComponents.
	 */
	public int getNumberOfComponents() {
		return numberOfComponents;
	}

	/**
	 * @return Returns the numberOfPaths.
	 */
	public int getNumberOfPaths() {
		return numberOfPaths;
	}

	/**
	 * @return Returns the numberOfFactors.
	 */
	public int getNumberOfFactors() {
		return numberOfFactors;
	}

	/**
	 * @return Returns the timeDiscretizationFromArray.
	 */
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	/**
	 * Returns the time for a given simulation time index.
	 *
	 * @param timeIndex Time index
	 * @return Returns the time for a given time index.
	 */
	public double getTime(final int timeIndex) {
		return timeDiscretization.getTime(timeIndex);
	}

	/**
	 * Returns the time index for a given simulation time.
	 *
	 * @param time A simulation time
	 * @return Returns the time index for a given time.
	 */
	public int getTimeIndex(final double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	/**
	 * @return Returns the Brownian motion used in the generation of the process
	 */
	public BrownianMotion getBrownianMotion() {
		return brownianMotion;
	}

	/**
	 * @return Returns the scheme.
	 */
	public Scheme getScheme() {
		return scheme;
	}

	/**
	 * A derived class may change the Brownian motion. This is only allowed prior to lazy initialization.
	 * The method should be used only while constructing new object. Do not use in flight.
	 *
	 * @param brownianMotion The brownianMotion to set.
	 */
	protected synchronized void setBrownianMotion(final BrownianMotion brownianMotion) {
		if(discreteProcessWeights != null && discreteProcessWeights.length != 0) {
			throw new RuntimeException("Tying to change lazy initialized immutable object after initialization.");
		}

		this.brownianMotion			= brownianMotion;
	}

	/**
	 * @param scheme The scheme to set.
	 */
	public synchronized  void setScheme(final Scheme scheme) {
		if(discreteProcessWeights != null && discreteProcessWeights.length != 0) {
			throw new RuntimeException("Tying to change lazy initialized immutable object after initialization.");
		}

		this.scheme = scheme;
	}
}
