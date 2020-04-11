/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Map;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

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
 * The class provides the model of (S,V) to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f_{1} = exp , f_{2} = identity \), \( \mu_{1} = r^{\text{c}} - \frac{1}{2} V^{+}(t) , \mu_{2} = \kappa ( \theta - V^{+}(t) ) \), \( \lambda_{1,1} = \sqrt{V^{+}(t)} , \lambda_{1,2} = 0  ,\lambda_{2,1} = \xi \sqrt{V^+(t)} \rho  , \lambda_{2,2} = \xi \sqrt{V^+(t)} \sqrt{1-\rho^{2}} \), i.e.,
 * of the SDE
 * \[
 * 	dX_{1} = \mu_{1} dt + \lambda_{1,1} dW_{1} + \lambda_{1,2} dW_{2}, \quad X_{1}(0) = \log(S_{0}),
 * \]
 * \[
 * 	dX_{2} = \mu_{2} dt + \lambda_{2,1} dW_{1} + \lambda_{2,2} dW_{2}, \quad X_{2}(0) = V_{0} = \sigma^2,
 * \]
 * with \( S = f_{1}(X_{1}) , V = f_{2}(X_{2}) \).
 * See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
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
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class HestonModel extends AbstractProcessModel {

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
	}

	private static final RandomVariable ZERO = new Scalar(0.0);

	private final RandomVariable initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final RandomVariable riskFreeRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final RandomVariable volatility;

	private final DiscountCurve discountCurveForDiscountRate;
	private final RandomVariable discountRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final RandomVariable theta;
	private final RandomVariable kappa;
	private final RandomVariable xi;
	private final RandomVariable rho;
	private final RandomVariable rhoBar;			// Precalculated Math.sqrt(1 - rho*rho);

	private final Scheme scheme;

	private final RandomVariableFactory randomVariableFactory;

	/*
	 * The interface definition requires that we provide the initial value, the drift and the volatility in terms of random variables.
	 * We construct the corresponding random variables here and will return (immutable) references to them.
	 */
	private final RandomVariable[]	initialValueVector	= new RandomVariable[2];

	/**
	 * Create the model from a descriptor.
	 *
	 * @param descriptor A descriptor of the model.
	 * @param scheme The scheme.
	 * @param randomVariableFactory A random variable factory to be used for the parameters.
	 */
	public HestonModel(final HestonModelDescriptor descriptor, 			final Scheme scheme,
			final RandomVariableFactory randomVariableFactory
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
			final RandomVariable initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final RandomVariable volatility,
			final DiscountCurve discountCurveForDiscountRate,
			final RandomVariable theta,
			final RandomVariable kappa,
			final RandomVariable xi,
			final RandomVariable rho,
			final Scheme scheme,
			final RandomVariableFactory randomVariableFactory
			) {
		super();

		this.initialValue	= initialValue;
		this.discountCurveForForwardRate	= discountCurveForForwardRate;
		riskFreeRate = null;
		this.volatility		= volatility;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = null;
		this.theta			= theta;
		this.kappa			= kappa;
		this.xi				= xi;
		this.rho			= rho;
		rhoBar			= rho.squared().sub(1).mult(-1).sqrt();

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
			final RandomVariable initialValue,
			final RandomVariable riskFreeRate,
			final RandomVariable volatility,
			final RandomVariable discountRate,
			final RandomVariable theta,
			final RandomVariable kappa,
			final RandomVariable xi,
			final RandomVariable rho,
			final Scheme scheme,
			final RandomVariableFactory randomVariableFactory
			) {
		super();

		this.initialValue	= initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.discountRate	= discountRate;
		discountCurveForDiscountRate = null;
		this.theta			= theta;
		this.kappa			= kappa;
		this.xi				= xi;
		this.rho			= rho;
		rhoBar			= rho.squared().sub(1).mult(-1).sqrt();

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
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double discountRate,
			final double theta,
			final double kappa,
			final double xi,
			final double rho,
			final Scheme scheme,
			final RandomVariableFactory randomVariableFactory
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
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double discountRate,
			final double theta,
			final double kappa,
			final double xi,
			final double rho,
			final Scheme scheme
			) {
		this(initialValue, riskFreeRate, volatility, discountRate, theta, kappa, xi, rho, scheme, new RandomVariableFromArrayFactory());
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
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double theta,
			final double kappa,
			final double xi,
			final double rho,
			final Scheme scheme
			) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate, theta, kappa, xi, rho, scheme, new RandomVariableFromArrayFactory());
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		// Since the underlying process is configured to simulate log(S), the initial value and the drift are transformed accordingly.
		if(initialValueVector[0] == null) 	{
			initialValueVector[0] = initialValue.log();
			initialValueVector[1] = volatility.squared();
		}

		return initialValueVector;
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		RandomVariable stochasticVariance;
		if(scheme == Scheme.FULL_TRUNCATION) {
			stochasticVariance = realizationAtTimeIndex[1].floor(0.0);
		} else if(scheme == Scheme.REFLECTION) {
			stochasticVariance = realizationAtTimeIndex[1].abs();
		} else {
			throw new UnsupportedOperationException("Scheme " + scheme.name() + " not supported.");
		}

		final RandomVariable[] drift = new RandomVariable[2];

		RandomVariable riskFreeRateAtTimeStep = null;
		if(discountCurveForForwardRate != null) {
			final double time		= process.getTime(timeIndex);
			final double timeNext	= process.getTime(timeIndex+1);

			final double rate = Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time);
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
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		RandomVariable stochasticVolatility;
		if(scheme == Scheme.FULL_TRUNCATION) {
			stochasticVolatility = realizationAtTimeIndex[1].floor(0.0).sqrt();
		} else if(scheme == Scheme.REFLECTION) {
			stochasticVolatility = realizationAtTimeIndex[1].abs().sqrt();
		} else {
			throw new UnsupportedOperationException("Scheme " + scheme.name() + " not supported.");
		}

		final RandomVariable[] factorLoadings = new RandomVariable[2];

		if(component == 0) {
			factorLoadings[0] = stochasticVolatility;
			factorLoadings[1] = ZERO;
		}
		else if(component == 1) {
			final RandomVariable volatility = stochasticVolatility.mult(xi);
			factorLoadings[0] = volatility.mult(rho);
			factorLoadings[1] = volatility.mult(rhoBar);
		}
		else {
			throw new UnsupportedOperationException("Component " + component + " does not exist.");
		}

		return factorLoadings;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(MonteCarloProcess process, int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
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
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
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
	public RandomVariable getNumeraire(MonteCarloProcess process, final double time) {
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

	@Override
	public int getNumberOfFactors() {
		return 1;
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return randomVariableFactory.createRandomVariable(value);
	}

	@Override
	public HestonModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final RandomVariableFactory newRandomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);

		final RandomVariable newInitialValue	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("initialValue"), initialValue);
		final RandomVariable newRiskFreeRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("riskFreeRate"), riskFreeRate);
		final RandomVariable newVolatility		= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("volatility"), volatility);
		final RandomVariable newDiscountRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("discountRate"), discountRate);

		final RandomVariable newTheta	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("theta"), theta);
		final RandomVariable newKappa	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("kappa"), kappa);
		final RandomVariable newXi		= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("xi"), xi);
		final RandomVariable newRho		= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("rho"), rho);

		return new HestonModel(newInitialValue, newRiskFreeRate, newVolatility, newDiscountRate, newTheta, newKappa, newXi, newRho, scheme, randomVariableFactory);
	}

	@Override
	public String toString() {
		return "HestonModel [initialValue=" + initialValue + ", riskFreeRate=" + riskFreeRate + ", volatility="
				+ volatility + ", theta=" + theta + ", kappa=" + kappa + ", xi=" + xi + ", rho=" + rho + ", scheme="
				+ scheme + "]";
	}

	public RandomVariable getInitialValue() {
		return initialValue;
	}

	/**
	 * Returns the risk free rate parameter of this model.
	 *
	 * @return Returns the riskFreeRate.
	 */
	public RandomVariable getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * Returns the volatility parameter of this model.
	 *
	 * @return Returns the volatility.
	 */
	public RandomVariable getVolatility() {
		return volatility;
	}

	public DiscountCurve getDiscountCurveForForwardRate() {
		return discountCurveForForwardRate;
	}

	public DiscountCurve getDiscountCurveForDiscountRate() {
		return discountCurveForDiscountRate;
	}

	public RandomVariable getTheta() {
		return theta;
	}

	public RandomVariable getKappa() {
		return kappa;
	}

	public RandomVariable getXi() {
		return xi;
	}

	public RandomVariable getRho() {
		return rho;
	}

	public Scheme getScheme() {
		return scheme;
	}
}
