package net.finmath.tree.assetderivativevaluation.models;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.assetderivativevaluation.AbstractRecombiningTreeModel;

/**
 * This class represents the approximation of a Black-Scholes model via the Cox Ross Rubinstein  model.
 * It extends a recombining tree model. The implemented method computes the up and down factors
 * @author Andrea Mazzon
 * @version 1.0
 */

public class CoxRossRubinsteinModel extends AbstractRecombiningTreeModel {

	/** Specific CRR parameters */
	private final double u;
	private final double d;
	private final double q;
	
	/**
	 * It constructs an object which represents the approximation of a Black-Scholes model via the Cox Ross
	 * Rubinstein model.
	 *
	 * @param initialPrice, the initial price of the asset modeled by the process
	 * @param riskFreeRate, the number r such that the value of a risk-free bond at time T is e^(rT)
	 * @param volatility, the log-volatility of the Black-Scholes model
	 * @param lastTime, the last time T in the time discretization 0=t_0<t_1<..<t_n=T
	 * @param timeStep, the length t_k-t_{k-1} of the equally spaced time steps that we take for the approximating
	 * time discretization 0=t_0<t_1<..<t_n=T
	 */
	public CoxRossRubinsteinModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, double timeStep) {
		super(initialPrice, riskFreeRate, volatility, lastTime, timeStep);
		double dt = getTimeStep();
		double sigma = getVolatility();
		double r = getRiskFreeRate();
		this.u = Math.exp(sigma * Math.sqrt(dt));
		this.d = 1.0 / u;
		double growth = Math.exp(r * dt);
		this.q = (growth - d) / (u - d);
	}

	/**
	 * It constructs an object which represents the approximation of a Black-Scholes model via the Cox Ross
	 * Rubinstein model.
	 *
	 * @param initialPrice, the initial price of the asset modeled by the process
	 * @param riskFreeRate, the number r such that the value of a risk-free bond at time T is e^(rT)
	 * @param volatility, the log-volatility of the Black-Scholes model
	 * @param lastTime, the last time T in the time discretization 0=t_0<t_1<..<t_n=T
	 * @param numberOfTimes the number of equally spaced time steps
	 */
	public CoxRossRubinsteinModel(double initialPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes) {
		super(initialPrice, riskFreeRate, volatility, lastTime, numberOfTimes);

		double dt = getTimeStep();
		double sigma = getVolatility();
		double r = getRiskFreeRate();

		this.u = Math.exp(sigma * Math.sqrt(dt));
		this.d = 1.0 / u;
		double growth = Math.exp(r * dt);
		this.q = (growth - d) / (u - d);
	}

	/** Abstract hook to implement in the underlying classes
	* Binomial: at k level there are k+1 states
	 * @param  k level of depth
	 * */
	@Override
	public int statesAt(int k) {
		return k + 1;
	}

	/** Build S_k[i] with agreement i = #down => S0 * u^(k-i) * d^i.
	 * @param  k level of depth */
	@Override
	protected RandomVariable buildSpotLevel(int k) {
		double[] s = new double[statesAt(k)];
		double s0 = getInitialPrice();
		int down = 0;
		int up = 0;
		for (int i = 0; i <= k; i++) {
			up = k - i;
			down = i;
			s[i] = s0 * Math.pow(u, up) * Math.pow(d, down);
		}
		return new RandomVariableFromDoubleArray(k*getTimeStep(),s);
	}

	/**
	 * Discounted conditional expectation : V_k[i] = df() * ( q * V_{k+1}[i] + (1-q) * V_{k+1}[i+1] ).
	 */
	protected RandomVariable conditionalExpectation(RandomVariable vNext, int k) {
		double[] next = vNext.getRealizations();
		double[] vK = new double[statesAt(k)];
		double disc = getOneStepDiscountFactor(k);

		for (int i = 0; i <= k; i++) {
			vK[i] = disc * (q * next[i] + (1.0 - q) * next[i + 1]);
		}
		return new RandomVariableFromDoubleArray(k * getTimeStep(), vK);
	}

	@Override
	public int getNumberOfBranches(int timeIndex, int stateIndex) {
		return 2;
	}

	@Override
	public double getTransitionProbability(int timeIndex, int stateIndex, int branchIndex) {
		// Convention: 0 = up, 1 = down
		switch(branchIndex) {
			case 0: return q;
			case 1: return 1.0 - q;
			default: throw new IllegalArgumentException("Invalid branchIndex " + branchIndex + " for binomial model.");
		}
	}

	@Override
	public int[] getChildStateIndexShift() {
		// Convention: childIndex = parentIndex + shift[branchIndex]
		// For binomial recombining trees: up keeps index, down increases index by 1.
		return new int[] { 0, 1 };
	}

	/** Getter */
	public double getU() { return u; }
	public double getD() { return d; }
	public double getQ() { return q; }
}

