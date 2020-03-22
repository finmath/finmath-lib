package net.finmath.montecarlo;

import java.io.Serializable;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import net.finmath.functions.NormalDistribution;
import net.finmath.functions.PoissonDistribution;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Implementation of the compound Poisson process for the Merton jump diffusion model.
 *
 * @author Christian Fries
 * @author Alessandro Gnoatto
 */
public class MertonJumpProcess implements IndependentIncrements, Serializable {

	private static final long serialVersionUID = -6984273344382051927L;

	private final RandomVariableFactory randomVariableFactory;
	private final IndependentIncrements internalProcess;
	private final double jumpIntensity;
	private final double jumpSizeMean;
	private final double jumpSizeStDev;

	/**
	 * Constructs a Merton Jump Process for Monte Carlo simulation.
	 *
	 * @param jumpIntensity The jump intensity.
	 * @param jumpSizeMean The mean of the jump size distribution.
	 * @param jumpSizeStDev The std dev of the jump size distribution.
	 * @param timeDiscretization The time discretization of the process.
	 * @param numberOfPaths The number of path.
	 * @param seed The seed for the random number generator.
	 */
	public MertonJumpProcess(double jumpIntensity, double jumpSizeMean, double jumpSizeStDev,
			TimeDiscretization timeDiscretization,
			int numberOfPaths, int seed) {
		super();
		// TODO randomVariableFactory should become a parameter to allow AAD
		this.randomVariableFactory = new RandomVariableFromArrayFactory();
		this.jumpIntensity = jumpIntensity;
		this.jumpSizeMean = jumpSizeMean;
		this.jumpSizeStDev = jumpSizeStDev;

		final IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions = new IntFunction<IntFunction<DoubleUnaryOperator>>() {
			@Override
			public IntFunction<DoubleUnaryOperator> apply(int i) {
				return new IntFunction<DoubleUnaryOperator>() {
					@Override
					public DoubleUnaryOperator apply(int j) {
						if(j==0) {
							// The Brownian increment
							final double sqrtOfTimeStep = Math.sqrt(timeDiscretization.getTimeStep(i));
							return new DoubleUnaryOperator() {
								@Override
								public double applyAsDouble(double x) {
									return NormalDistribution.inverseCumulativeDistribution(x)*sqrtOfTimeStep;
								}
							};
						}
						else if(j==1) {
							// The random jump size
							return new DoubleUnaryOperator() {
								@Override
								public double applyAsDouble(double x) {
									return NormalDistribution.inverseCumulativeDistribution(x);
								}
							};
						}
						else if(j==2) {
							// The jump increment
							final double timeStep = timeDiscretization.getTimeStep(i);
							final PoissonDistribution poissonDistribution = new PoissonDistribution(jumpIntensity*timeStep);
							return new DoubleUnaryOperator() {
								@Override
								public double applyAsDouble(double x) {
									return poissonDistribution.inverseCumulativeDistribution(x);
								}
							};
						}
						else {
							return null;
						}
					}
				};
			}
		};

		final IndependentIncrements icrements = new IndependentIncrementsFromICDF(timeDiscretization, 3, numberOfPaths, seed, inverseCumulativeDistributionFunctions ) {
			private static final long serialVersionUID = -7858107751226404629L;

			@Override
			public RandomVariable getIncrement(int timeIndex, int factor) {
				if(factor == 1) {
					final RandomVariable Z = super.getIncrement(timeIndex, 1);
					final RandomVariable N = super.getIncrement(timeIndex, 2);
					return Z.mult(N.sqrt());
				}
				else {
					return super.getIncrement(timeIndex, factor);
				}
			}
		};

		this.internalProcess = icrements;
	}

	@Override
	public RandomVariable getIncrement(int timeIndex, int factor) {
		return internalProcess.getIncrement(timeIndex, factor);
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return internalProcess.getTimeDiscretization();
	}

	@Override
	public int getNumberOfFactors() {
		return internalProcess.getNumberOfFactors();
	}

	@Override
	public int getNumberOfPaths() {
		return internalProcess.getNumberOfPaths();
	}

	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public IndependentIncrements getCloneWithModifiedSeed(int seed) {
		return internalProcess.getCloneWithModifiedSeed(seed);
	}

	@Override
	public IndependentIncrements getCloneWithModifiedTimeDiscretization(TimeDiscretization newTimeDiscretization) {
		return internalProcess.getCloneWithModifiedTimeDiscretization(newTimeDiscretization);
	}


	/**
	 * @return the jumpIntensity
	 */
	public double getJumpIntensity() {
		return jumpIntensity;
	}

	/**
	 * @return the jumpSizeMean
	 */
	public double getJumpSizeMean() {
		return jumpSizeMean;
	}

	/**
	 * @return the jumpSizeStDev
	 */
	public double getJumpSizeStDev() {
		return jumpSizeStDev;
	}

}
