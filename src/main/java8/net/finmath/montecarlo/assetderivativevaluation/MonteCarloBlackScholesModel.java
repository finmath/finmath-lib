/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class glues together a <code>BlackScholeModel</code> and a Monte-Carlo implementation of a <code>MonteCarloProcessFromProcessModel</code>
 * and forms a Monte-Carlo implementation of the Black-Scholes Model by implementing <code>AssetModelMonteCarloSimulationModel</code>.
 *
 * The model is
 * \[
 * 	dS = r S dt + \sigma S dW, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = exp \), \( \mu = r - \frac{1}{2} \sigma^2 \), \( \lambda_{1,1} = \sigma \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class MonteCarloBlackScholesModel extends MonteCarloAssetModel {

	/*
	 * The default seed
	 */
	private static final int seed = 3141;

	/**
	 * Create a Monte-Carlo simulation using given process discretization scheme.
	 *
	 * @param initialValue Spot value
	 * @param riskFreeRate The risk free rate
	 * @param volatility The log volatility
	 * @param brownianMotion The brownian motion driving the model.
	 */
	public MonteCarloBlackScholesModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final BrownianMotion brownianMotion) {
		super(new BlackScholesModel(initialValue, riskFreeRate, volatility), brownianMotion);
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 */
	public MonteCarloBlackScholesModel(
			final TimeDiscretization timeDiscretization,
			final int numberOfPaths,
			final double initialValue,
			final double riskFreeRate,
			final double volatility) {
		this(
				initialValue, riskFreeRate, volatility,
				new BrownianMotionFromMersenneRandomNumbers(
						timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));
	}

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param model The model.
	 * @param process The process.
	 */
	private MonteCarloBlackScholesModel(
			final BlackScholesModel model,
			final MonteCarloProcess process) {
		super(model, process);
	}

	@Override
	public RandomVariable getAssetValue(final double time, final int assetIndex) throws CalculationException {
		final int timeIndex = getTimeIndex(time);
		if(timeIndex < 0) {
			throw new IllegalArgumentException("The model does not provide an interpolation of simulation time (time given was " + time + ").");
		}

		return getAssetValue(timeIndex, assetIndex);
	}

	@Override
	public MonteCarloBlackScholesModel getCloneWithModifiedData(final Map<String, Object> dataModified) {

		final MonteCarloProcess process = getProcess();

		/*
		 * Create a new model with the new model parameters.
		 */
		final BlackScholesModel newModel = getModel().getCloneWithModifiedData(dataModified);

		/*
		 * Create a new BrownianMotion, if requested.
		 */
		final int		newSeed			= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		BrownianMotion newBrownianMotion;
		if(dataModified.get("seed") != null) {
			// The seed has changed. Hence we have to create a new BrownianMotionLazyInit.
			newBrownianMotion = new BrownianMotionFromMersenneRandomNumbers(this.getTimeDiscretization(), 1, this.getNumberOfPaths(), newSeed);
		}
		else {
			// The seed has not changed. We may reuse the random numbers (Brownian motion) of the original model
			newBrownianMotion = (BrownianMotion)process.getStochasticDriver();
		}

		final double newInitialTime	= dataModified.get("initialTime") != null	? ((Number)dataModified.get("initialTime")).doubleValue() : getTime(0);

		final double timeShift = newInitialTime - getTime(0);
		if(timeShift != 0) {
			final TimeDiscretization newTimeDiscretization =  process.getStochasticDriver().getTimeDiscretization().getTimeShiftedTimeDiscretization(timeShift);
			newBrownianMotion = newBrownianMotion.getCloneWithModifiedTimeDiscretization(newTimeDiscretization);
		}

		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel newProcess = new EulerSchemeFromProcessModel(newModel, new BrownianMotionFromMersenneRandomNumbers(this.getTimeDiscretization(), 1 /* numberOfFactors */, this.getNumberOfPaths(), seed));

		return new MonteCarloBlackScholesModel(newModel, newProcess);
	}

	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(final int seed) {
		// Create a corresponding MC process
		final MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(getModel(), new BrownianMotionFromMersenneRandomNumbers(this.getTimeDiscretization(), 1 /* numberOfFactors */, this.getNumberOfPaths(), seed));
		return new MonteCarloBlackScholesModel(getModel(), process);
	}

	/**
	 * Returns the {@link BlackScholesModel} used for this Monte-Carlo simulation.
	 *
	 * @return the model
	 */
	public BlackScholesModel getModel() {
		return (BlackScholesModel)super.getModel();
	}
}
