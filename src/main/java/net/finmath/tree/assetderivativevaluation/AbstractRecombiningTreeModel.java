package net.finmath.tree.assetderivativevaluation;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.OneDimensionalRiskFactorTreeModel;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * This class represent the recombining tree model, a general abstract class which collect the common properties
 * about recombining trees.
 *
 * @author Carlo Andrea Tramentozzi
 * @version 1.0
 */

public abstract class AbstractRecombiningTreeModel extends OneDimensionalRiskFactorTreeModel implements OneDimensionalAssetTreeModel {

	/** Common parameters of the model */
	private final double initialPrice;
	private final double riskFreeRate;
	private final double volatility;

	/** Temporal discretization */
	private final double timeStep;
	private final double lastTime;
	private final int numberOfTimes;
	/** Cache of level spot S_k */
	private final List<RandomVariable> spotLevels = new ArrayList<>();

	/**
	 * Constructs a recombining tree model on an equidistant time grid by
	 * specifying the time step.
	 * The constructor sets the common model parameters, computes
	 * the number of time points and initializes the level-0 cache of
	 * spot values with S_0.
	 * @param initialPrice The initial asset price S0
	 * @param riskFreeRate  The continuously compounded risk-free rate r used consistently with df().
	 * @param volatility    The volatility.
	 * @param lastTime      The final time T of the grid.
	 * @param timeStep      The time step Δt between consecutive grid points
	 */
	public AbstractRecombiningTreeModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, double timeStep) {
		this.initialPrice = initialPrice;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		this.lastTime = lastTime;
		this.timeStep = timeStep;
		int steps = (int)Math.round(lastTime / timeStep);
		this.numberOfTimes = steps + 1;
		spotLevels.add(new RandomVariableFromDoubleArray(0.0, new double[] { initialPrice }));
	}

	/**
	 * Constructs a recombining tree model on an equidistant time grid by
	 * specifying the time step.
	 * The constructor sets the common model parameters , computes
	 * the number of time points and initializes the level-0 cache of
	 * spot values with S_0.
	 * @param initialPrice The initial asset price S₀.
	 * @param riskFreeRate  The continuously compounded risk-free rate r used consistently with df().
	 * @param volatility    The (log) volatility σ.
	 * @param lastTime      The final time T of the grid.
	 * @param  numberOfTimes The total number of grid points N (must be ≥ 2). The number of steps is N−1.
	 */
	public AbstractRecombiningTreeModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes) {
		this.initialPrice = initialPrice;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		this.lastTime = lastTime;
		this.numberOfTimes = numberOfTimes;
		this.timeStep = lastTime / (numberOfTimes - 1);

		spotLevels.add(new RandomVariableFromDoubleArray(0.0, new double[] { initialPrice }));
	}


	/** One step discount factor(according to q): continuous */
	protected final double df() {
		return Math.exp(-riskFreeRate * timeStep);
	}

	

	/** Getter */
	@Override
	public double getInitialPrice()   { return initialPrice; }
	@Override
	public double getRiskFreeRate()   { return riskFreeRate; }
	@Override
	public double getVolatility()     { return volatility; }
	
	public double getTimeStep()       { return timeStep; }
	public double getLastTime()       { return lastTime; }
	public int getNumberOfTimes()     { return numberOfTimes; }
	public double getSpot()           {return getInitialPrice(); } //Redefinition


}
