/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2012
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a Heston Model, that is, it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
 *
 * The model is
 * \[
 * 	dS(t) = r^{\text{c}} S(t) dt + \sqrt{V(t)} S(t) dW_{1}(t), \quad S(0) = S_{0},
 * \]
 * \[
 * 	dV(t) = \kappa ( \theta - V(t) ) dt + \xi \sqrt{V(t)} dW_{2}(t), \quad V(0) = \sigma^2,
 * \]
 * \[
 * 	dW_{1} dW_{2} = \rho dt
 * \]
 * \[
 * 	dN(t) = r^{\text{d}} N(t) dt, \quad N(0) = N_{0},
 * \]
 * 
 * The class provides the model of (S,V) to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f_{1} = exp , f_{2} = identity \), \( \mu_{1} = r^{\text{c}} - \frac{1}{2} V^{+}(t) , \mu_{2} = \kappa ( \theta - V^{+}(t) ) \), \( \lambda_{1,1} = \sqrt{V^{+}(t)} , \lambda_{1,2} = 0  ,\lambda_{2,1} = \xi \sqrt{V^+(t)} \rho  , \lambda_{2,2} = \xi \sqrt{V^+(t)} \sqrt{1-\rho^{2}} \), i.e.,
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
 * The model allows to specify two independent rate for forwarding (\( r^{\text{c}} \)) and discounting (\( r^{\text{d}} \)).
 * It thus allow for a simple modelling of a funding / collateral curve (via (\( r^{\text{d}} \)) and/or the specification of
 * a dividend yield.
 * 
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class HestonModel extends AbstractModel {

	/**
	 * Truncation schemes to be used in the calculation of drift and diffusion coefficients.
	 */
	public enum Scheme {
		/**
		 * Reflection scheme, that is V is replaced by Math.abs(V), where V denotes the current realization of V(t).
		 */
		REFLECTION,
		
		/**
		 * Full truncation scheme, that is V is replaced by Math.max(V,0), where V denotes the current realization of V(t).
		 */
		FULL_TRUNCATION
	};

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double volatility;
	private final double discountRate;		// The discount rate, can be differ

	private final double theta;
	private final double kappa;
	private final double xi;
	private final double rho;

	private final Scheme scheme;

	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private RandomVariableInterface[]	initialValueVector	= new RandomVariableInterface[2];

	/**
	 * Create a Heston model.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param discountRate The discount rate used in the numeraire.
	 * @param theta The longterm mean reversion level of V (a reasonable value is volatility*volatility).
	 * @param kappa The mean reversion speed.
	 * @param xi The volatility of the volatility (of V).
	 * @param rho The instantaneous correlation of the Brownian drivers (aka leverage).
	 * @param scheme The truncation scheme, that is, either reflection (V &rarr; abs(V)) or truncation (V &rarr; max(V,0)).
	 */
	public HestonModel(
			double initialValue,
			double riskFreeRate,
			double volatility,
			double discountRate,
			double theta,
			double kappa,
			double xi,
			double rho,
			Scheme scheme
			) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.discountRate	= discountRate;
		this.theta			= theta;
		this.kappa			= kappa;
		this.xi				= xi;
		this.rho			= rho;
		this.scheme			= scheme;
	}

	/**
	 * Create a Heston model.
	 * 
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param theta The longterm mean reversion level of V (a reasonable value is volatility*volatility).
	 * @param kappa The mean reversion speed.
	 * @param xi The volatility of the volatility (of V).
	 * @param rho The instantaneous correlation of the Brownian drivers (aka leverage).
	 * @param scheme The truncation scheme, that is, either reflection (V &rarr; abs(V)) or truncation (V &rarr; max(V,0)).
	 */
	public HestonModel(
			double initialValue,
			double riskFreeRate,
			double volatility,
			double theta,
			double kappa,
			double xi,
			double rho,
			Scheme scheme
			) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.discountRate	= riskFreeRate;
		this.theta			= theta;
		this.kappa			= kappa;
		this.xi				= xi;
		this.rho			= rho;
		this.scheme			= scheme;
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		// Since the underlying process is configured to simulate log(S), the initial value and the drift are transformed accordingly.
		if(initialValueVector[0] == null) 	{
			initialValueVector[0] = getRandomVariableForConstant(Math.log(initialValue));
			initialValueVector[1] = getRandomVariableForConstant(volatility*volatility);
		}

		return initialValueVector;
	}

	@Override
	public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
		RandomVariableInterface stochasticVariance;
		if(scheme == Scheme.FULL_TRUNCATION)	stochasticVariance = realizationAtTimeIndex[1].floor(0.0);
		else if(scheme == Scheme.REFLECTION)	stochasticVariance = realizationAtTimeIndex[1].abs();
		else throw new UnsupportedOperationException("Scheme " + scheme.name() + " not supported.");

		RandomVariableInterface[] drift = new RandomVariableInterface[2];

		drift[0] = getRandomVariableForConstant(riskFreeRate).sub(stochasticVariance.div(2.0));
		drift[1] = getRandomVariableForConstant(theta).sub(stochasticVariance).mult(kappa);

		return drift;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		RandomVariableInterface stochasticVolatility;
		if(scheme == Scheme.FULL_TRUNCATION)	stochasticVolatility = realizationAtTimeIndex[1].floor(0.0).sqrt();
		else if(scheme == Scheme.REFLECTION)	stochasticVolatility = realizationAtTimeIndex[1].abs().sqrt();
		else throw new UnsupportedOperationException("Scheme " + scheme.name() + " not supported.");

		RandomVariableInterface[] factorLoadings = new RandomVariableInterface[2];

		if(component == 0) {
			factorLoadings[0] = stochasticVolatility;
			factorLoadings[1] = getRandomVariableForConstant(0.0);
		}
		else if(component == 1) {
			RandomVariableInterface volatility = stochasticVolatility.mult(xi);
			factorLoadings[0] = volatility.mult(rho);
			factorLoadings[1] = volatility.mult(Math.sqrt(1-rho*rho));
		}
		else {
			throw new UnsupportedOperationException("Component " + component + " does not exist.");
		}

		return factorLoadings;
	}

	@Override
	public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		if(componentIndex == 0) {
			return randomVariable.exp();
		}
		else if(componentIndex == 1) {
			return randomVariable;
		}
		else {
			throw new UnsupportedOperationException("Component " + componentIndex + " does not exist.");
		}
	}

	@Override
	public RandomVariableInterface getNumeraire(double time) {
		double numeraireValue = Math.exp(discountRate * time);

		return getRandomVariableForConstant(numeraireValue);
	}

	@Override
	public int getNumberOfComponents() {
		return 2;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.AbstractModelInterface#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public HestonModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : this.getRiskFreeRate();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: this.getVolatility();
		double	newTheta		= dataModified.get("theta") != null			? ((Number)dataModified.get("theta")).doubleValue()	: rho;
		double	newKappa		= dataModified.get("kappa") != null			? ((Number)dataModified.get("kappa")).doubleValue()	: kappa;
		double	newXi			= dataModified.get("xi") != null			? ((Number)dataModified.get("xi")).doubleValue()	: xi;
		double	newRho			= dataModified.get("rho") != null			? ((Number)dataModified.get("rho")).doubleValue()	: rho;

		return new HestonModel(newInitialValue, newRiskFreeRate, newVolatility, newTheta, newKappa, newXi, newRho, scheme);
	}

	@Override
	public String toString() {
		return "HestonModel [initialValue=" + initialValue + ", riskFreeRate=" + riskFreeRate + ", volatility="
				+ volatility + ", theta=" + theta + ", kappa=" + kappa + ", xi=" + xi + ", rho=" + rho + ", scheme="
				+ scheme + "]";
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * Returns the volatility parameter of this model.
	 * 
	 * @return Returns the volatility.
	 */
	public double getVolatility() {
		return volatility;
	}
}
