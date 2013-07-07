/*
 * Created on 19.01.2004
 */
package net.finmath.montecarlo.templatemethoddesign;

import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.RandomVariableMutableClone;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class is an abstract base class to implement an Euler scheme of a multi-dimensional multi-factor log-normal Ito process.
 * The dimension is called <code>numberOfComponents</code> here. The default for <code>numberOfFactors</code> is 1.
 * 
 * @author Christian Fries
 * @date 19.04.2008
 * @version 1.5
 */
public abstract class LogNormalProcess {

	public enum Scheme { EULER, PREDICTOR_USING_EULERSTEP, PREDICTOR_USING_LASTREALIZATION };

	private BrownianMotionInterface	brownianMotion;

	private RandomVariableInterface[][]     discreteProcess         = null;
	private RandomVariableInterface[]       discreteProcessWeights  = null;

	private TimeDiscretizationInterface	timeDiscretization;
	private int			numberOfComponents;
	private int			numberOfFactors;
	private int			numberOfPaths;

	private Scheme		scheme = Scheme.EULER;

	/**
	 * @param numberOfComponents The number of components (scalar processes).
	 * @param brownianMotion A Brownian motion
	 */
	public LogNormalProcess(
			int numberOfComponents,
			BrownianMotionInterface brownianMotion) {
		super();
		this.timeDiscretization	= brownianMotion.getTimeDiscretization();
		this.numberOfComponents	= numberOfComponents;
		this.numberOfFactors	= brownianMotion.getNumberOfFactors();
		this.numberOfPaths		= brownianMotion.getNumberOfPaths();
		this.brownianMotion		= brownianMotion;
	}

	/**
	 * @param timeDiscretization
	 * @param numberOfComponents
	 * @param numberOfPaths
	 */
	public LogNormalProcess(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfComponents,
			int numberOfPaths) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.numberOfComponents = numberOfComponents;
		this.numberOfFactors = 1;
		this.numberOfPaths = numberOfPaths;

		// Create a Brownian motion
		brownianMotion = new net.finmath.montecarlo.BrownianMotion(
				timeDiscretization,
				numberOfFactors,
				numberOfPaths,
				3141 /* seed */);
	}

	/**
	 * @param timeDiscretization
	 * @param numberOfComponents
	 * @param numberOfFactors
	 * @param numberOfPaths
	 */
	public LogNormalProcess(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfComponents,
			int numberOfFactors,
			int numberOfPaths,
			int seed) {
		super();
		this.timeDiscretization	= timeDiscretization;
		this.numberOfComponents	= numberOfComponents;
		this.numberOfFactors	= numberOfFactors;
		this.numberOfPaths		= numberOfPaths;

		// Create a Brownian motion
		brownianMotion = new net.finmath.montecarlo.BrownianMotion(
				timeDiscretization,
				numberOfFactors,
				numberOfPaths,
				seed);
	}


	abstract public RandomVariableInterface[]	getInitialValue();
	abstract public RandomVariableInterface	getDrift(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor);

	/**
     * @param timeIndex The time index (related to the model times discretization).
     * @param realizationAtTimeIndex The given realization at timeIndex
     * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null of no predictor is available.
     * @return The (average) drift from timeIndex to timeIndex+1
     */
    public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {

    	RandomVariableInterface[] drift = new RandomVariableInterface[getNumberOfComponents()];

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
	 * the coeffient lamba(i,j) such that <br>
	 * dS(j) = (...) dt + S(j) * (lambda(1,j) dW(1) + ... + lambda(m,j) dW(m)) <br>
	 * in an m-factor model. Here j denotes index of the component of the resulting
	 * log-normal process and i denotes the index of the factor.<p>
	 * Overwrite this method if you would like to implement a multi factor model.
	 * 
	 * @param timeIndex
	 * @param realizationAtTimeIndex TODO
	 * @param factor
	 * @param component
	 * @return factor loading for given factor and component
	 */
	abstract public RandomVariableInterface getFactorLoading(int timeIndex, int factor, int component, RandomVariableInterface[] realizationAtTimeIndex);

	/**
	 * This method returns the realization of the process at a certain time index.
	 * 
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of process realizations (on path)
	 */
	public RandomVariableInterface[] getProcessValue(int timeIndex)
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
	public RandomVariableInterface getProcessValue(int timeIndex, int componentIndex)
	{
		if(timeIndex == 0) return new RandomVariableMutableClone(getInitialValue()[componentIndex]);

		// Lazy initialization, synchronized for thread safety
		synchronized(this) {
			if(discreteProcess == null || discreteProcess.length == 0)		doPrecalculateProcess();
		}

		// Return value of process
		return new RandomVariableMutableClone(discreteProcess[timeIndex][componentIndex]);
	}

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 * 
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 */
	public RandomVariableInterface getMonteCarloWeights(int timeIndex)
	{
		// Lazy initialization, synchronized for thread safety
		synchronized(this) {
			if(discreteProcessWeights == null || discreteProcessWeights.length == 0)  doPrecalculateProcess();
		}

		// Return value of process
		return new RandomVariableMutableClone(discreteProcessWeights[timeIndex]);
	}

	/**
	 * Calculates the whole (discrete) process.
	 */
	private void doPrecalculateProcess()
	{
		if(discreteProcess != null && discreteProcess.length != 0) return;

		// Allocate Memory
		discreteProcess			= new RandomVariableInterface[timeDiscretization.getNumberOfTimeSteps()+1][numberOfComponents];
		discreteProcessWeights	= new RandomVariableInterface[getTimeDiscretization().getNumberOfTimeSteps()+1];

		// Set initial Monte-Carlo weights
		discreteProcessWeights[0] = new RandomVariable(0.0, 1.0/numberOfPaths);

		// Store components
		discreteProcess[0] = getInitialValue();

		// Evolve process
		for(int timeIndex = 1; timeIndex < timeDiscretization.getNumberOfTimeSteps()+1; timeIndex++)
		{
			// Generate process at timeIndex
			double deltaT = timeDiscretization.getTime(timeIndex) - timeDiscretization.getTime(timeIndex-1);

			// Temp storage for variance and diffusion
			RandomVariableInterface[] variance   = new RandomVariableInterface[numberOfComponents];
			RandomVariableInterface[] diffusion  = new RandomVariableInterface[numberOfComponents];

			// Calculate new realization
			for(int componentIndex = numberOfComponents-1; componentIndex >= 0; componentIndex--)
			{
				// Calculate diffusion

				// Temp storage for variance and diffusion
				RandomVariableInterface varianceOfComponent  = new RandomVariable(getTime(timeIndex-1),0.0);
				RandomVariableInterface diffusionOfComponent = new RandomVariable(getTime(timeIndex-1),0.0);
				
				// Generate values for diffusionOfComponent and varianceOfComponent 
				for(int factor=0; factor<numberOfFactors; factor++) {
					RandomVariableInterface factorLoading       = getFactorLoading(timeIndex-1, factor, componentIndex, null);               
					RandomVariableInterface brownianIncrement   = brownianMotion.getBrownianIncrement(timeIndex-1,factor);

					varianceOfComponent = varianceOfComponent.addProduct(factorLoading, factorLoading);
					diffusionOfComponent = diffusionOfComponent.addProduct(factorLoading, brownianIncrement);
				}

				variance[componentIndex] = varianceOfComponent;
				diffusion[componentIndex] = diffusionOfComponent;
			}

			RandomVariableInterface[] drift;
			if(scheme == Scheme.PREDICTOR_USING_LASTREALIZATION)	drift = getDrift(timeIndex-1, discreteProcess[timeIndex-1], discreteProcess[timeIndex]);
			else				    								drift = getDrift(timeIndex-1, discreteProcess[timeIndex-1], null                      );
			
			// Calculate drift
			for(int componentIndex = numberOfComponents-1; componentIndex >= 0; componentIndex--)
			{
				RandomVariableInterface driftOfComponent  	= drift[componentIndex];
				RandomVariableInterface varianceOfComponent			= variance[componentIndex];
				RandomVariableInterface diffusionOfComponent		= diffusion[componentIndex];

				if(driftOfComponent == null) {
					discreteProcess[timeIndex][componentIndex] = discreteProcess[timeIndex-1][componentIndex];
					continue;
				}
				
				// Allocate memory for on path-realizations
				double[] newRealization			= new double[numberOfPaths];

				// Euler Scheme
				RandomVariableInterface previouseRealization		= discreteProcess[timeIndex-1][componentIndex];

				// Generate values 
				for(int pathIndex = 0; pathIndex < numberOfPaths; pathIndex++)
				{
					double previousValue	= previouseRealization.get(pathIndex);
					double driftOnPath		= driftOfComponent.get(pathIndex);
					double varianceOnPath	= varianceOfComponent.get(pathIndex);
					double diffusionOnPath	= diffusionOfComponent.get(pathIndex);

					// The scheme
					newRealization[pathIndex]	 = previousValue * Math.exp(driftOnPath * deltaT - 0.5 * varianceOnPath * deltaT + diffusionOnPath);
				}

				// Store components
				discreteProcess[timeIndex][componentIndex] = new RandomVariable(getTime(timeIndex),newRealization);
			}

			if(scheme == Scheme.PREDICTOR_USING_EULERSTEP) {   
				RandomVariable[] newRealization = new RandomVariable[numberOfComponents];

				// Note: This is actually more than a predictor corrector: The drift of componentIndex already uses the corrected predictor from the previous components
				drift = getDrift(timeIndex-1, discreteProcess[timeIndex-1], discreteProcess[timeIndex]);
				
				// Apply corrector step to realizations at next time step
				for(int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++)
				{
					RandomVariableInterface driftOfComponent	= drift[componentIndex];
					RandomVariableInterface varianceOfComponent			= variance[componentIndex];
					RandomVariableInterface diffusionOfComponent		= diffusion[componentIndex];

					// Get reference to newRealization (note, we force mutability of an immutable object, but we know what we are doing)
					double[] newRealizationValues = ((RandomVariable)(discreteProcess[timeIndex][componentIndex])).getRealizations(numberOfPaths);

					// Euler Scheme with corrected drift
					RandomVariableInterface previouseRealization 	= discreteProcess[timeIndex-1][componentIndex];

					// Generate values 
					for (int pathIndex = 0; pathIndex < numberOfPaths; pathIndex++ )
					{
						double previousValue	= previouseRealization.get(pathIndex);
						double driftValue		= driftOfComponent.get(pathIndex);					
						double varianceOnPath	= varianceOfComponent.get(pathIndex);
						double diffusionOnPath	= diffusionOfComponent.get(pathIndex);

						// The scheme
						newRealizationValues[pathIndex] = previousValue * Math.exp(driftValue * deltaT - 0.5 * varianceOnPath * deltaT + diffusionOnPath);
					} // End for(pathIndex)

					// Store values
					newRealization[componentIndex] = new RandomVariable(getTime(timeIndex), newRealizationValues);
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
	 * @return Returns the timeDiscretization.
	 */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}

	/**
	 * @param timeIndex Time index
	 * @return Returns the time for a given time index.
	 */
	public double getTime(int timeIndex) {
		return timeDiscretization.getTime(timeIndex);
	}

	/**
	 * @param time
	 * @return Returns the time index for a given time
	 */
	public int getTimeIndex(double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	/**
	 * @return Returns the Brownian motion used in the generation of the process
	 */
	public BrownianMotionInterface getBrownianMotion() {
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
	protected synchronized void setBrownianMotion(BrownianMotionInterface brownianMotion) {
		if(discreteProcessWeights != null && discreteProcessWeights.length != 0) throw new RuntimeException("Tying to change lazy initialized immutable object after initialization.");

		this.brownianMotion			= brownianMotion;
	}

	/**
	 * @param scheme The scheme to set.
	 */
	public synchronized  void setScheme(Scheme scheme) {
		if(discreteProcessWeights != null && discreteProcessWeights.length != 0) throw new RuntimeException("Tying to change lazy initialized immutable object after initialization.");

		this.scheme = scheme;
	}
}