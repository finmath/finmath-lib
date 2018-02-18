/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2012
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
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
 * where \( W \) is a Brownian motion.
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
 * The free parameters of this model are:
 * <dl>
 * 	<dt>\( S_{0} \)</dt> <dd>spot - initial value of S</dd>
 * 	<dt>\( r^{\text{c}} \)</dt> <dd>the risk free rate</dd>
 * 	<dt>\( \sigma \)</dt> <dd>the initial volatility level</dd>
 * 	<dt>\( r^{\text{d}} \)</dt> <dd>the discount rate</dd>
 * 	<dt>\( \xi \)</dt> <dd>the volatility of volatility</dd>
 * 	<dt>\( \theta \)</dt> <dd>the mean reversion level of the stochastic volatility</dd>
 * 	<dt>\( \kappa \)</dt> <dd>the mean reversion speed of the stochastic volatility</dd>
 * 	<dt>\( \rho \)</dt> <dd>the correlation of the Brownian drivers</dd>
 * </dl>
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



	private final RandomVariableInterface initialValue;

	private final DiscountCurveInterface discountCurveForForwardRate;
	private final RandomVariableInterface riskFreeRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final RandomVariableInterface volatility;

	private final DiscountCurveInterface discountCurveForDiscountRate;
	private final RandomVariableInterface discountRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final RandomVariableInterface theta;
	private final RandomVariableInterface kappa;
	private final RandomVariableInterface xi;
	private final RandomVariableInterface rho;
	private final RandomVariableInterface rhoBar;			// Precalculated Math.sqrt(1 - rho*rho);

	private final Scheme scheme;

	private final AbstractRandomVariableFactory randomVariableFactory;

	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private RandomVariableInterface[]	initialValueVector	= new RandomVariableInterface[2];

	/**
	 * Create the model from a descriptor.
	 * 
	 * @param descriptor A descriptor of the product.
	 * @param scheme The scheme.
	 * @param randomVariableFactory A random variable factory to be used for the parameters.
	 */
	public HestonModel(HestonModelDescriptor descriptor, 			Scheme scheme,
			AbstractRandomVariableFactory randomVariableFactory
			) {
		this(
				randomVariableFactory.createRandomVariable(descriptor.getInitialValue()),
				descriptor.getDiscountCurveForForwardRate(),
				randomVariableFactory.createRandomVariable(descriptor.getVolatility()),
				descriptor.getDiscountCurveForDiscountRate(),
				randomVariableFactory.createRandomVariable(descriptor.getTheta()),
				randomVariableFactory.createRandomVariable(descriptor.getKappa()),
				randomVariableFactory.createRandomVariable(descriptor.getXi()),
				randomVariableFactory.createRandomVariable(descriptor.getRho()),
				scheme,
				randomVariableFactory
				);
	}

	/**
	 * Create a Heston model.
	 * 
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate the discount curve  \( df^{\text{c}} \) used to calculate the risk free rate \( r^{\text{c}}(t_{i},t_{i+1}) = \frac{\ln(\frac{df^{\text{c}}(t_{i})}{df^{\text{c}}(t_{i+1})}}{t_{i+1}-t_{i}} \)
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param discountCurveForDiscountRate the discount curve  \( df^{\text{d}} \) used to calculate the numeraire, \( r^{\text{d}}(t_{i},t_{i+1}) = \frac{\ln(\frac{df^{\text{d}}(t_{i})}{df^{\text{d}}(t_{i+1})}}{t_{i+1}-t_{i}} \)
	 * @param theta \( \theta \) - the mean reversion level of the stochastic volatility
	 * @param kappa \( \kappa \) - the mean reversion speed of the stochastic volatility
	 * @param xi \( \xi \) - the volatility of volatility
	 * @param rho \( \rho \) - the correlation of the Brownian drivers
	 * @param scheme The truncation scheme, that is, either reflection (V &rarr; abs(V)) or truncation (V &rarr; max(V,0)).
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public HestonModel(
			RandomVariableInterface initialValue,
			DiscountCurveInterface discountCurveForForwardRate,
			RandomVariableInterface volatility,
			DiscountCurveInterface discountCurveForDiscountRate,
			RandomVariableInterface theta,
			RandomVariableInterface kappa,
			RandomVariableInterface xi,
			RandomVariableInterface rho,
			Scheme scheme,
			AbstractRandomVariableFactory randomVariableFactory
			) {
		super();

		this.initialValue	= initialValue;
		this.discountCurveForForwardRate	= discountCurveForForwardRate;
		this.riskFreeRate = null;
		this.volatility		= volatility;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.discountRate = null;
		this.theta			= theta;
		this.kappa			= kappa;
		this.xi				= xi;
		this.rho			= rho;
		this.rhoBar			= rho.squared().sub(1).mult(-1).sqrt();

		this.scheme			= scheme;

		this.randomVariableFactory = randomVariableFactory;
	}

	/**
	 * Create a Heston model.
	 * 
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate \( r^{\text{c}} \) - the risk free rate
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param discountRate \( r^{\text{d}} \) - the discount rate
	 * @param theta \( \theta \) - the mean reversion level of the stochastic volatility
	 * @param kappa \( \kappa \) - the mean reversion speed of the stochastic volatility
	 * @param xi \( \xi \) - the volatility of volatility
	 * @param rho \( \rho \) - the correlation of the Brownian drivers
	 * @param scheme The truncation scheme, that is, either reflection (V &rarr; abs(V)) or truncation (V &rarr; max(V,0)).
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public HestonModel(
			RandomVariableInterface initialValue,
			RandomVariableInterface riskFreeRate,
			RandomVariableInterface volatility,
			RandomVariableInterface discountRate,
			RandomVariableInterface theta,
			RandomVariableInterface kappa,
			RandomVariableInterface xi,
			RandomVariableInterface rho,
			Scheme scheme,
			AbstractRandomVariableFactory randomVariableFactory
			) {
		super();

		this.initialValue	= initialValue;
		this.discountCurveForForwardRate = null;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.discountRate	= discountRate;
		this.discountCurveForDiscountRate = null;
		this.theta			= theta;
		this.kappa			= kappa;
		this.xi				= xi;
		this.rho			= rho;
		this.rhoBar			= rho.squared().sub(1).mult(-1).sqrt();

		this.scheme			= scheme;

		this.randomVariableFactory = randomVariableFactory;
	}

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
	 * @param randomVariableFactory The factory to be used to construct random variables..
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
			Scheme scheme,
			AbstractRandomVariableFactory randomVariableFactory
			) {
		this(
				randomVariableFactory.createRandomVariable(initialValue),
				randomVariableFactory.createRandomVariable(riskFreeRate),
				randomVariableFactory.createRandomVariable(volatility),
				randomVariableFactory.createRandomVariable(discountRate),
				randomVariableFactory.createRandomVariable(theta),
				randomVariableFactory.createRandomVariable(kappa),
				randomVariableFactory.createRandomVariable(xi),
				randomVariableFactory.createRandomVariable(rho),
				scheme,
				randomVariableFactory);
	}

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
		this(initialValue, riskFreeRate, volatility, discountRate, theta, kappa, xi, rho, scheme, new RandomVariableFactory());
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
		this(initialValue, riskFreeRate, volatility, riskFreeRate, theta, kappa, xi, rho, scheme, new RandomVariableFactory());
	}

	@Override
	public RandomVariableInterface[] getInitialState() {
		// Since the underlying process is configured to simulate log(S), the initial value and the drift are transformed accordingly.
		if(initialValueVector[0] == null) 	{
			initialValueVector[0] = initialValue.log();
			initialValueVector[1] = volatility.squared();
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

		RandomVariableInterface riskFreeRateAtTimeStep = null;
		if(discountCurveForForwardRate != null) {
			double time		= getTime(timeIndex);
			double timeNext	= getTime(timeIndex+1);

			double rate = Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time);
			riskFreeRateAtTimeStep = randomVariableFactory.createRandomVariable(rate);
		}
		else {
			riskFreeRateAtTimeStep = riskFreeRate;
		}

		drift[0] = riskFreeRateAtTimeStep.sub(stochasticVariance.div(2.0));
		drift[1] = theta.sub(stochasticVariance).mult(kappa);

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
			factorLoadings[1] = volatility.mult(rhoBar);
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
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		if(componentIndex == 0) {
			return randomVariable.log();
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
		if(discountCurveForDiscountRate != null) {
			return randomVariableFactory.createRandomVariable(1.0/discountCurveForDiscountRate.getDiscountFactor(time));
		}
		else {
			return discountRate.mult(time).exp();
		}
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
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public HestonModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		RandomVariableInterface	newInitialValue	= getRandomVariableForValue(dataModified.getOrDefault("initialValue", initialValue));
		RandomVariableInterface	newRiskFreeRate	= getRandomVariableForValue(dataModified.getOrDefault("riskFreeRate", riskFreeRate));
		RandomVariableInterface	newVolatility	= getRandomVariableForValue(dataModified.getOrDefault("volatility", volatility));
		RandomVariableInterface	newDiscountRate	= getRandomVariableForValue(dataModified.getOrDefault("discountRate", discountRate));

		RandomVariableInterface	newTheta	= getRandomVariableForValue(dataModified.getOrDefault("theta", theta));
		RandomVariableInterface	newKappa	= getRandomVariableForValue(dataModified.getOrDefault("kappa", kappa));
		RandomVariableInterface	newXi		= getRandomVariableForValue(dataModified.getOrDefault("xi", xi));
		RandomVariableInterface	newRho		= getRandomVariableForValue(dataModified.getOrDefault("rho", rho));

		return new HestonModel(newInitialValue, newRiskFreeRate, newVolatility, newDiscountRate, newTheta, newKappa, newXi, newRho, scheme, randomVariableFactory);
	}

	private RandomVariableInterface getRandomVariableForValue(Object value) {
		if(value instanceof RandomVariableInterface) return (RandomVariableInterface) value;
		else return getRandomVariableForConstant(((Number) value).doubleValue());
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
	public RandomVariableInterface getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * Returns the volatility parameter of this model.
	 * 
	 * @return Returns the volatility.
	 */
	public RandomVariableInterface getVolatility() {
		return volatility;
	}
}
