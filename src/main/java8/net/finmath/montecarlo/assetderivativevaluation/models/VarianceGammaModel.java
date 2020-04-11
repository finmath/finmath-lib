package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.descriptor.VarianceGammaModelDescriptor;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements a <i>Variance Gamma Model</i>, that is, it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
 *
 * The model is
 * \[
 * 	dS_t = r S dt + S dL, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 *
 * where the process L is a <code>{@link net.finmath.montecarlo.VarianceGammaProcess}</code>.
 *
 * @author Alessandro Gnoatto
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class VarianceGammaModel extends AbstractProcessModel {

	private final RandomVariableFactory randomVariableFactory;

	private final RandomVariable initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final RandomVariable riskFreeRate;		// Actually the same as the drift (which is not stochastic)

	private final DiscountCurve discountCurveForDiscountRate;
	private final RandomVariable discountRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final RandomVariable sigma;
	private final RandomVariable theta;
	private final RandomVariable nu;

	/**
	 * Construct a Variance Gamma model with discount curves for the forward price (i.e. repo rate minus dividend yield) and for discounting.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param sigma The parameter \( \sigma \).
	 * @param theta The parameter \( \theta \).
	 * @param nu The parameter \( \nu \).
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public VarianceGammaModel(final RandomVariable initialValue, final DiscountCurve discountCurveForForwardRate,
			final DiscountCurve discountCurveForDiscountRate, final RandomVariable sigma, final RandomVariable theta, final RandomVariable nu,
			final RandomVariableFactory randomVariableFactory) {
		super();
		this.randomVariableFactory = randomVariableFactory;

		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = null;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = null;
		this.sigma = sigma;
		this.theta = theta;
		this.nu = nu;
	}

	/**
	 * Construct a Variance Gamma model with constant rates for the forward price (i.e. repo rate minus dividend yield) and for the discount curve.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate The constant risk free rate for the drift (repo rate of the underlying).
	 * @param discountRate The constant rate used for discounting.
	 * @param sigma The parameter \( \sigma \).
	 * @param theta The parameter \( \theta \).
	 * @param nu The parameter \( \nu \).
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public VarianceGammaModel(final RandomVariable initialValue, final RandomVariable riskFreeRate, final RandomVariable discountRate, final RandomVariable sigma, final RandomVariable theta,
			final RandomVariable nu,
			final RandomVariableFactory randomVariableFactory) {
		super();
		this.randomVariableFactory = new RandomVariableFromArrayFactory();
		this.initialValue = initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
		this.sigma = sigma;
		this.theta = theta;
		this.nu = nu;
	}

	/**
	 * Create the model from a descriptor.
	 *
	 * @param descriptor A descriptor of the model.
	 */
	public VarianceGammaModel(VarianceGammaModelDescriptor descriptor) {
		this(descriptor.getInitialValue(),
				descriptor.getDiscountCurveForForwardRate(),
				descriptor.getDiscountCurveForDiscountRate(),
				descriptor.getSigma(),
				descriptor.getTheta(),
				descriptor.getNu());
	}

	/**
	 * Construct a Variance Gamma model with discount curves for the forward price (i.e. repo rate minus dividend yield) and for discounting.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param sigma The parameter \( \sigma \).
	 * @param theta The parameter \( \theta \).
	 * @param nu The parameter \( \nu \).
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public VarianceGammaModel(final double initialValue, final DiscountCurve discountCurveForForwardRate,
			final DiscountCurve discountCurveForDiscountRate, final double sigma, final double theta, final double nu,
			final RandomVariableFactory randomVariableFactory) {
		super();
		this.initialValue = randomVariableFactory.createRandomVariable(initialValue);
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = null;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = null;
		this.sigma = randomVariableFactory.createRandomVariable(sigma);
		this.theta = randomVariableFactory.createRandomVariable(theta);
		this.nu = randomVariableFactory.createRandomVariable(nu);

		this.randomVariableFactory = randomVariableFactory;
	}

	/**
	 * Construct a Variance Gamma model with discount curves for the forward price (i.e. repo rate minus dividend yield) and for discounting.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param sigma The parameter \( \sigma \).
	 * @param theta The parameter \( \theta \).
	 * @param nu The parameter \( \nu \).
	 */
	public VarianceGammaModel(final double initialValue, final DiscountCurve discountCurveForForwardRate,
			final DiscountCurve discountCurveForDiscountRate, final double sigma, final double theta, final double nu) {
		this(initialValue, discountCurveForDiscountRate, discountCurveForDiscountRate, sigma, theta, nu, new RandomVariableFromArrayFactory());
	}

	/**
	 * Construct a Variance Gamma model with constant rates for the forward price (i.e. repo rate minus dividend yield) and for the discount curve.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate The constant risk free rate for the drift (repo rate of the underlying).
	 * @param discountRate The constant rate used for discounting.
	 * @param sigma The parameter \( \sigma \).
	 * @param theta The parameter \( \theta \).
	 * @param nu The parameter \( \nu \).
	 */
	public VarianceGammaModel(final double initialValue, final double riskFreeRate, final double discountRate, final double sigma, final double theta,
			final double nu) {
		super();
		this.randomVariableFactory = new RandomVariableFromArrayFactory();
		this.initialValue = this.randomVariableFactory.createRandomVariable(initialValue);
		discountCurveForForwardRate = null;
		this.riskFreeRate = this.randomVariableFactory.createRandomVariable(riskFreeRate);
		discountCurveForDiscountRate = null;
		this.discountRate = this.randomVariableFactory.createRandomVariable(discountRate);
		this.sigma = this.randomVariableFactory.createRandomVariable(sigma);
		this.theta = this.randomVariableFactory.createRandomVariable(theta);
		this.nu = this.randomVariableFactory.createRandomVariable(nu);
	}


	/**
	 * Construct a Variance Gamma model with constant rates for the forward price (i.e. repo rate minus dividend yield) and for the discount curve.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate The constant risk free rate for the drift (repo rate of the underlying).
	 * @param sigma The parameter \( \sigma \).
	 * @param theta The parameter \( \theta \).
	 * @param nu The parameter \( \nu \).
	 */
	public VarianceGammaModel(final double initialValue, final double riskFreeRate, final double sigma, final double theta, final double nu) {
		this(initialValue,riskFreeRate,riskFreeRate,sigma,theta,nu);
	}

	@Override
	public RandomVariable applyStateSpaceTransform(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.log();
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		return new RandomVariable[] { initialValue.log() };
	}

	@Override
	public RandomVariable getNumeraire(MonteCarloProcess process, final double time) {
		if(discountCurveForDiscountRate != null) {
			return getRandomVariableForConstant(1.0/discountCurveForDiscountRate.getDiscountFactor(time));
		}
		else {
			return discountRate.mult(time).exp();
		}
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		RandomVariable riskFreeRateAtTimeStep;

		if(discountCurveForForwardRate != null) {
			final double time		= process.getTime(timeIndex);
			final double timeNext	= process.getTime(timeIndex+1);

			riskFreeRateAtTimeStep = getRandomVariableForConstant(Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time));
		}
		else {
			riskFreeRateAtTimeStep = riskFreeRate;
		}

		// r + log(1 - theta * nu - 0.5 nu sigma^2)
		return new RandomVariable[] { riskFreeRateAtTimeStep.add(theta.mult(nu).add(sigma.squared().mult(nu).mult(0.5)).mult(-1).add(1.0).log().div(nu)) };
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int componentIndex,
			final RandomVariable[] realizationAtTimeIndex) {
		final RandomVariable[] factors = new RandomVariable[1];
		factors[0] = getRandomVariableForConstant(1.0);
		return factors;
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
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
	public ProcessModel getCloneWithModifiedData(final Map<String, Object> dataModified) throws CalculationException {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final RandomVariableFactory newRandomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);

		final RandomVariable newInitialValue	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("initialValue"), initialValue);
		final RandomVariable newRiskFreeRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("riskFreeRate"), riskFreeRate);
		final RandomVariable newDiscountRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("discountRate"), discountRate);

		final RandomVariable newSigma	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("sigma"), sigma);
		final RandomVariable newTheta	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("theta"), theta);
		final RandomVariable newNu		= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("nu"), nu);

		return new VarianceGammaModel(newInitialValue, newRiskFreeRate, newDiscountRate, newSigma, newTheta, newNu, newRandomVariableFactory);
	}

	/**
	 * @return the discountCurveForForwardRate
	 */
	public DiscountCurve getDiscountCurveForForwardRate() {
		return discountCurveForForwardRate;
	}

	/**
	 * @return the riskFreeRate
	 */
	public RandomVariable getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * @return the discountCurveForDiscountRate
	 */
	public DiscountCurve getDiscountCurveForDiscountRate() {
		return discountCurveForDiscountRate;
	}

	/**
	 * @return the discountRate
	 */
	public RandomVariable getDiscountRate() {
		return discountRate;
	}

	/**
	 * @return the sigma
	 */
	public RandomVariable getSigma() {
		return sigma;
	}

	/**
	 * @return the theta
	 */
	public RandomVariable getTheta() {
		return theta;
	}

	/**
	 * @return the nu
	 */
	public RandomVariable getNu() {
		return nu;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "VarianceGammaModel [initialValue=" + initialValue + ", discountCurveForForwardRate="
				+ discountCurveForForwardRate + ", riskFreeRate=" + riskFreeRate + ", discountCurveForDiscountRate="
				+ discountCurveForDiscountRate + ", discountRate=" + discountRate + ", sigma=" + sigma + ", theta="
				+ theta + ", nu=" + nu + "]";
	}

}
