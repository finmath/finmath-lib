package net.finmath.tree.assetderivativevaluation.models;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;
import net.finmath.tree.assetderivativevaluation.AbstractRecombiningTreeModel;

/**
 * This class is used in order to construct a trinomial model. The trinomial model is a discrete model
 * for a stochastic process S, such that every time n we have
 * S(n+1)=S(n)*M(n),
 * where M(n) can take values u, 1 or 1/u with probabilities q_u, q_m and q_d, respectively.
 * This is done under a martingale measure: it can be seen (see notes) that there exists infinitely many
 * martingale measures once one fixed q_u.
 * For any time index i, all possible value of the process are computed starting from the biggest one.
 *
 * @author Andrea Mazzon
 * @version  1.0
 */

public class BoyleTrinomial extends AbstractRecombiningTreeModel {

	/** Specific parameters */
	private final double dt;
	private final double r;
	private final double u;
	private final double d;
	private final double pu, pm, pd;

	/**
	 * It constructs an object which represents the approximation of a Black-Scholes model via the Jarrow-Rudd model.
	 *
	 * @param spotPrice, the spot price of the asset modeled by the process
	 * @param riskFreeRate, the number r such that the value of a risk-free bond at time T is e^(rT)
	 * @param volatility, the log-volatility of the Black-Scholes model
	 * @param lastTime, the last time T in the time discretization 0=t_0<t_1<..<t_n=T
	 * @param timeStep, the length t_k-t_{k-1} of the equally spaced time steps that we take for the approximating
	 * time discretization 0=t_0<t_1<..<t_n=T
	 */

	public BoyleTrinomial(double spotPrice, double riskFreeRate, double volatility, double lastTime, double timeStep) {
		super(spotPrice, riskFreeRate, volatility, lastTime, timeStep);
		this.dt = getTimeStep();
		this.r  = Math.exp(getRiskFreeRate() * dt);
		this.u  = Math.exp(getVolatility() * Math.sqrt(2.0 * dt));
		this.d  = 1.0 / u;

		double M2 = Math.exp(2.0 * getRiskFreeRate() * dt + Math.pow(getVolatility(), 2) * dt);

		double b1 = r  - 1.0;
		double b2 = M2 - 1.0;

		double A11 = (u - 1.0);
		double A12 = (d - 1.0);
		double A21 = (u*u - 1.0);
		double A22 = (d*d - 1.0);

		double det = A11*A22 - A12*A21;
		if(Math.abs(det) < 1e-14) {
			throw new IllegalArgumentException("Degenerate system for trinomial probabilities (det≈0).");
		}

		double puTmp = ( b1*A22 - b2*A12 ) / det;
		double pdTmp = ( A11*b2 - A21*b1 ) / det;
		double pmTmp = 1.0 - puTmp - pdTmp;

		double puC = Math.max(0.0, Math.min(1.0, puTmp));
		double pmC = Math.max(0.0, Math.min(1.0, pmTmp));
		double pdC = Math.max(0.0, Math.min(1.0, pdTmp));
		double sum = puC + pmC + pdC;
		if(sum <= 0.0) throw new IllegalArgumentException("Invalid probabilities in Boyle trinomial.");
		this.pu = puC / sum;
		this.pm = pmC / sum;
		this.pd = pdC / sum;
	}
	/**
	 * It constructs an object which represents the approximation of a Black-Scholes model via the Jarrow-Rudd model.
	 *
	 * @param spotPrice, the spot price of the asset modeled by the process
	 * @param riskFreeRate, the number r such that the value of a risk-free bond at time T is e^(rT)
	 * @param volatility, the log-volatility of the Black-Scholes model
	 * @param lastTime, the last time T in the time discretization 0=t_0<t_1<..<t_n=T
	 * @param numberOfTimes, the number of times in the equally spaced time steps that we take for the approximating
	 * time discretization 0=t_0<t_1<..<t_n=T
	 */

	public BoyleTrinomial(double spotPrice, double riskFreeRate, double volatility, double lastTime, int numberOfTimes) {
		this(spotPrice, riskFreeRate, volatility, lastTime, lastTime/(numberOfTimes-1.0));
	}

	/** Return number of states at k level (trinomial: 2k+1)
	 * @param  k level of depth */
	@Override
	public int statesAt(int k) {
		return 2*k + 1;
	}

	/** Builds all the realizations S_k[i]
	 * @param  k level of depth  */
	@Override
	protected RandomVariable buildSpotLevel(int k) {
		double[] level = new double[2*k + 1];
		double S0 = getSpot();
		for(int j=-k; j<=k; j++) {
			int idx = j + k;
			int upCount   = Math.max( j, 0);
			int downCount = Math.max(-j, 0);
			level[idx] = S0 * Math.pow(u, upCount) * Math.pow(d, downCount);
		}
		double time = k * dt;
		return new RandomVariableFromDoubleArray(time, level);
	}

	/**
	 * Backward induction step: V_k(j) = (pu*V_{k+1}(j+1) + pm*V_{k+1}(j) + pd*V_{k+1}(j-1)) / R.
	 * vNext has dimension 2(k+1)+1 = 2k+3 (j = -(k+1)..(k+1)).
	 */
	@Override
	protected RandomVariable conditionalExpectation(RandomVariable vNext, int k) {
		double[] next = vNext.getRealizations();
		int expectedLen = 2*(k+1) + 1; // = 2k+3
		if(next.length != expectedLen) {
			throw new IllegalArgumentException("vNext length "+next.length+" != expected "+expectedLen+" for level k="+k);
		}

		double[] now = new double[2*k + 1];
		for(int j=-k; j<=k; j++) {
			int idxNow = j + k;

			double up   = next[j + k + 2];
			double mid  = next[j + k + 1];
			double down = next[j + k    ];

			now[idxNow] = (pu * up + pm * mid + pd * down) / r;
		}

		double time = k * dt;
		return new RandomVariableFromDoubleArray(time, now);
	}
}
