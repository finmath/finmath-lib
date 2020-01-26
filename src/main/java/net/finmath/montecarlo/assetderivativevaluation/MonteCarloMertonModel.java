/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;
import java.util.function.IntFunction;

import net.finmath.exception.CalculationException;
import net.finmath.functions.NormalDistribution;
import net.finmath.functions.PoissonDistribution;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.IndependentIncrementsFromICDF;
import net.finmath.montecarlo.assetderivativevaluation.models.MertonModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class glues together a <code>MertonModel</code> and a Monte-Carlo implementation of a <code>MonteCarloProcessFromProcessModel</code>, namely <code>EulerSchemeFromProcessModel</code>,
 * and forms a Monte-Carlo implementation of the Merton model by implementing <code>AssetModelMonteCarloSimulationModel</code>.
 *
 * The model is
 * \[
 * 	dS = \mu S dt + \sigma S dW + S dJ, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * where \( W \) is Brownian motion and \( J \)  is a jump process (compound Poisson process).
 *
 * The process \( J \) is given by \( J(t) = \sum_{i=1}^{N(t)} (Y_{i}-1) \), where
 * \( \log(Y_{i}) \) are i.i.d. normals with mean \( a - \frac{1}{2} b^{2} \) and standard deviation \( b \).
 * Here \( a \) is the jump size mean and \( b \) is the jump size std. dev.
 *
 * For details on the construction of the model see {@link net.finmath.montecarlo.assetderivativevaluation.models.MertonModel}.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.assetderivativevaluation.models.MertonModel
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class MonteCarloMertonModel implements AssetModelMonteCarloSimulationModel {

	private final MertonModel model;
	private final double initialValue;
	private final int seed;

	/**
	 * Create a Monte-Carlo simulation using given time discretization and given parameters.
	 *
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param seed The seed used for the random number generator.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 */
	public MonteCarloMertonModel(
			final TimeDiscretization timeDiscretization,
			final int numberOfPaths,
			final int seed,
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev
			) {
		super();

		this.initialValue = initialValue;
		this.seed = seed;

		// Create the model
		model = new MertonModel(initialValue, riskFreeRate, volatility, jumpIntensity, jumpSizeMean, jumpSizeStDev);

		final IntFunction<IntFunction<DoubleUnaryOperator>> inverseCumulativeDistributionFunctions = new IntFunction<IntFunction<DoubleUnaryOperator>>() {
			@Override
			public IntFunction<DoubleUnaryOperator> apply(final int i) {
				return new IntFunction<DoubleUnaryOperator>() {
					@Override
					public DoubleUnaryOperator apply(final int j) {
						if(j==0) {
							// The Brownian increment
							final double sqrtOfTimeStep = Math.sqrt(timeDiscretization.getTimeStep(i));
							return new DoubleUnaryOperator() {
								@Override
								public double applyAsDouble(final double x) {
									return NormalDistribution.inverseCumulativeDistribution(x)*sqrtOfTimeStep;
								}
							};
						}
						else if(j==1) {
							// The random jump size
							return new DoubleUnaryOperator() {
								@Override
								public double applyAsDouble(final double x) {
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
								public double applyAsDouble(final double x) {
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
			public RandomVariable getIncrement(final int timeIndex, final int factor) {
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

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(icrements);

		// Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);
	}

	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	@Override
	public RandomVariable getAssetValue(final double time, final int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
	}

	@Override
	public RandomVariable getAssetValue(final int timeIndex, final int assetIndex) throws CalculationException {
		return model.getProcess().getProcessValue(timeIndex, assetIndex);
	}

	@Override
	public RandomVariable getNumeraire(final int timeIndex) throws CalculationException {
		final double time = getTime(timeIndex);

		return model.getNumeraire(time);
	}

	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final double time) throws CalculationException {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getNumberOfAssets()
	 */
	@Override
	public int getNumberOfAssets() {
		return 1;
	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final double	newInitialTime	= dataModified.get("initialTime") != null	? ((Number)dataModified.get("initialTime")).doubleValue() : getTime(0);
		final double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		final double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : model.getRiskFreeRate();
		final double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: model.getVolatility();
		final double	newJumpIntensity	= dataModified.get("jumpIntensity") != null	? ((Number)dataModified.get("jumpIntensity")).doubleValue()	: model.getJumpIntensity();
		final double	newJumpSizeMean		= dataModified.get("jumpSizeMean") != null	? ((Number)dataModified.get("jumpSizeMean")).doubleValue()	: model.getVolatility();
		final double	newJumpSizeStdDev	= dataModified.get("jumpSizeStdDev") != null	? ((Number)dataModified.get("jumpSizeStdDev")).doubleValue()	: model.getVolatility();
		final int		newSeed				= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		return new MonteCarloMertonModel(model.getProcess().getTimeDiscretization().getTimeShiftedTimeDiscretization(newInitialTime-getTime(0)), model.getProcess().getNumberOfPaths(), newSeed, newInitialValue, newRiskFreeRate, newVolatility, newJumpIntensity, newJumpSizeMean, newJumpSizeStdDev);

	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(final int seed) {
		final Map<String, Object> dataModified = new HashMap<>();
		dataModified.put("seed", new Integer(seed));
		return getCloneWithModifiedData(dataModified);
	}

	@Override
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	@Override
	public double getTime(final int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(final double time) {
		return model.getProcess().getTimeIndex(time);
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return model.getRandomVariableForConstant(value);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}
}
