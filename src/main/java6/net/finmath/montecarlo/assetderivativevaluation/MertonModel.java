/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2012
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a Heston Model, that is, it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
 *
 * The model is
 * \[
 * 	dS(t) = r S(t) dt + \sqrt{V(t)} S(t) dW_{1}(t), \quad S(0) = S_{0},
 * \]
 * \[
 * 	dV(t) = \kappa ( \theta - V(t) ) dt + \xi \sqrt{V(t)} dW_{2}(t), \quad V(0) = \sigma^2,
 * \]
 * \[
 * 	dW_{1} dW_{1} = \rho dt
 * \]
 * \[
 * 	dN(t) = r N(t) dt, \quad N(0) = N_{0},
 * \]
 * 
 * The class provides the model of (S,V) to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f_{1} = exp , f_{2} = identity \), \( \mu_{1} = r - \frac{1}{2} V^{+}(t) , \mu_{2} = \kappa (\theta - V^{+}(t) \), \( \lambda_{1,1} = \sqrt{V^{+}(t)} , \lambda_{2,1} = \xi \sqrt{V^+(t)} \rho  , \lambda_{2,2} = \xi \sqrt{V^+(t)} \sqrt{1-\rho^{2}} \), i.e.,
 * of the SDE
 * \[
 * 	dX_{1} = \mu_{1} dt + \lambda_{1,1} dW_{1} + \lambda_{1,2} dW_{2}, \quad X_{1}(0) = \log(S_{0}),
 * \]
 * \[
 * 	dX_{2} = \mu_{2} dt + \lambda_{2,1} dW_{1} + \lambda_{2,2} dW_{2}, \quad X_{2}(0) = V_{0} = \sigma^2,
 * \]
 * with \( S = f_{1}(X_{1}) , V = f_{2}(X_{2}) \).
 * See {@link net.finmath.montecarlo.process.AbstractProcessInterface} for the notation.
 * 
 * Here \( V^{+} \) denotes a <i>truncated</i> value of V. Different truncation schemes are available:
 * <code>FULL_TRUNCATION</code>: \( V^{+} = max(V,0) \),
 * <code>REFLECTION</code>: \( V^{+} = abs(V) \).
 * 
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class MertonModel extends AbstractModel {

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double volatility;

	private final double jumpIntensity;
	private final double jumpSizeMean;
	private final double jumpSizeStdDev;

	/**
	 * Create a Heston model.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 */
	public MertonModel(
			double initialValue,
			double riskFreeRate,
			double volatility,
			double jumpIntensity,
			double jumpSizeMean,
			double jumpSizeStDev
			) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.jumpIntensity	= jumpIntensity;
		this.jumpSizeMean	= jumpSizeMean;
		this.jumpSizeStdDev	= jumpSizeStDev;
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		return new RandomVariableInterface[] { getProcess().getStochasticDriver().getRandomVariableForConstant(Math.log(initialValue)) };
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(Math.exp(riskFreeRate * time));
	}

	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		return new RandomVariableInterface[] { getProcess().getStochasticDriver().getRandomVariableForConstant(riskFreeRate - (Math.exp(jumpSizeMean)-1)*jumpIntensity - 0.5 * volatility*volatility) };
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex) {
		RandomVariableInterface[] factors = new RandomVariableInterface[3];
		factors[0] = getProcess().getStochasticDriver().getRandomVariableForConstant(volatility);
		factors[1] = getProcess().getStochasticDriver().getRandomVariableForConstant(jumpSizeStdDev);
		factors[2] = getProcess().getStochasticDriver().getRandomVariableForConstant(jumpSizeMean - 0.5*jumpSizeStdDev*jumpSizeStdDev);

		return factors;
	}

	@Override
	public AbstractModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @return the riskFreeRate
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * @return the volatility
	 */
	public double getVolatility() {
		return volatility;
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
	 * @return the jumpSizeStdDev
	 */
	public double getJumpSizeStdDev() {
		return jumpSizeStdDev;
	}

}
