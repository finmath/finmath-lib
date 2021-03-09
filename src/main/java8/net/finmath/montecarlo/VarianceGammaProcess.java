package net.finmath.montecarlo;

import java.io.Serializable;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implementation of a time-discrete n-dimensional Variance Gamma process via Brownian subordination through
 * a Gamma Process.
 *
 * To simulate the Variance Gamma process with paramters \( (\sigma,\theta,\nu) \) we proceed in two steps:
 * <ul>
 *   <li>we simulate the path of a GammaProcess with parameters \( \frac{1}{\nu} and \nu \) </li>
 *	<li>use the GammaProcess as a subordinator for a Brownian motion with drift </li>
 * </ul>
 *
 * \( \theta \Gamma(t) + \sigma W(\Gamma(t)) \)
 *
 * The class is immutable and thread safe. It uses lazy initialization.
 *
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class VarianceGammaProcess implements IndependentIncrements, Serializable{

	private static final long serialVersionUID = -338038617011804530L;

	private final double sigma;
	private final double nu;
	private final double theta;

	private final TimeDiscretization	timeDiscretization;

	private final int			numberOfFactors;
	private final int			numberOfPaths;
	private final int			seed;

	private GammaProcess myGammaProcess;
	private BrownianMotion myBrownianMotion;

	private final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

	private transient RandomVariable[][]	varianceGammaIncrements;

	public VarianceGammaProcess(final double sigma, final double nu, final double theta,
			final TimeDiscretization timeDiscretization,
			final int numberOfFactors, final int numberOfPaths, final int seed) {
		super();
		this.sigma = sigma;
		this.nu = nu;
		this.theta = theta;
		this.timeDiscretization = timeDiscretization;
		this.numberOfFactors = numberOfFactors;
		this.numberOfPaths = numberOfPaths;
		this.seed = seed;

		varianceGammaIncrements = null;
	}

	@Override
	public RandomVariable getIncrement(final int timeIndex, final int factor) {
		// Thread safe lazy initialization
		synchronized(this) {
			if(varianceGammaIncrements == null) {
				doGenerateVarianceGammaIncrements();
			}
		}

		/*
		 *  For performance reasons we return directly the stored data (no defensive copy).
		 *  We return an immutable object to ensure that the receiver does not alter the data.
		 */
		return varianceGammaIncrements[timeIndex][factor];
	}

	/**
	 *Lazy initialization of gammaIncrement. Synchronized to ensure thread safety of lazy init.
	 */
	private void doGenerateVarianceGammaIncrements() {
		if(varianceGammaIncrements != null) {
			return;
		}

		myGammaProcess =
				new GammaProcess(timeDiscretization,numberOfFactors,numberOfPaths,seed,1/nu,nu);

		myBrownianMotion =
				new BrownianMotionFromMersenneRandomNumbers(timeDiscretization,numberOfFactors,numberOfPaths,seed+1);

		varianceGammaIncrements = new RandomVariable[timeDiscretization.getNumberOfTimeSteps()][numberOfFactors];

		/*
		 * Generate variance gamma distributed independent increments.
		 *
		 * Since we already have a Brownian motion and a Gamma process at our disposal,
		 * we are simply combining them.
		 */
		for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {

			// Generate uncorrelated Gamma distributed increment
			for(int factor=0; factor<numberOfFactors; factor++) {
				varianceGammaIncrements[timeIndex][factor] =
						(myGammaProcess.getIncrement(timeIndex, factor).mult(theta))
						.add((myGammaProcess.getIncrement(timeIndex, factor)).sqrt()
								.mult(myBrownianMotion.getBrownianIncrement(timeIndex,factor)
										.mult(sigma/Math.sqrt(timeDiscretization.getTimeStep(timeIndex)))));
			}
		}

	}

	/**
	 * @return the sigma
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * @return the nu
	 */
	public double getNu() {
		return nu;
	}

	/**
	 * @return the theta
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * @return the Brownian motion
	 */
	public BrownianMotion getBrownianMotion(){
		return myBrownianMotion;
	}

	/**
	 * @return the Gamma subordinator
	 */
	public GammaProcess getGammaProcess(){
		return myGammaProcess;
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public int getNumberOfFactors() {
		return numberOfFactors;
	}

	@Override
	public int getNumberOfPaths() {
		return numberOfPaths;
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public IndependentIncrements getCloneWithModifiedSeed(final int seed) {
		return new VarianceGammaProcess(sigma, nu,theta, timeDiscretization, numberOfFactors, numberOfPaths, seed);
	}

	@Override
	public IndependentIncrements getCloneWithModifiedTimeDiscretization(final TimeDiscretization newTimeDiscretization) {
		return new VarianceGammaProcess(sigma, nu, theta, newTimeDiscretization, numberOfFactors, numberOfPaths, seed);
	}

}
