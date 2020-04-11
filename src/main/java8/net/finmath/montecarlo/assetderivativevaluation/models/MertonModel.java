/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Map;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.descriptor.MertonModelDescriptor;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * This class implements a <i>Merton Model</i>, that is, it provides the drift and volatility specification
 * and performs the calculation of the numeraire (consistent with the dynamics, i.e. the drift).
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
 *  The model can be rewritten as \( S = \exp(X) \), where
 * \[
 * 	dX = \mu dt + \sigma dW + dJ^{X}, \quad X(0) = \log(S_{0}),
 * \]
 * with
 * \[
 * 	J^{X}(t) = \sum_{i=1}^{N(t)} \log(Y_{i})
 * \]
 * with \( \mu = r - \frac{1}{2} \sigma^2 - (exp(a)-1) \lambda \).
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = exp \), \( \mu = r - \frac{1}{2} \sigma^2 - (exp(a)-1) \lambda \), \( \lambda_{1,1} = \sigma, \lambda_{1,2} = a - \frac{1}{2} b^2, \lambda_{1,3} = b \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW + \lambda_{1,2} dN + \lambda_{1,3} Z dN, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * For an example on the construction of the three factors \( dW \), \( dN \), and \( Z dN \) see {@link net.finmath.montecarlo.assetderivativevaluation.MonteCarloMertonModel}.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.assetderivativevaluation.MonteCarloMertonModel
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class MertonModel extends AbstractProcessModel {

	private static final RandomVariable ZERO = new Scalar(0.0);

	private final RandomVariable initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final RandomVariable riskFreeRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final RandomVariable volatility;

	private final DiscountCurve discountCurveForDiscountRate;
	private final RandomVariable discountRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final RandomVariable jumpIntensity;
	private final RandomVariable jumpSizeMean;
	private final RandomVariable jumpSizeStdDev;

	private final RandomVariableFactory randomVariableFactory;

	/**
	 * Create a Merton model.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param volatility The log volatility.
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public MertonModel(
			final RandomVariable initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final RandomVariable volatility,
			final DiscountCurve discountCurveForDiscountRate,
			final RandomVariable jumpIntensity,
			final RandomVariable jumpSizeMean,
			final RandomVariable jumpSizeStDev,
			final RandomVariableFactory randomVariableFactory
			) {
		super();
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.riskFreeRate	= null;
		this.volatility		= volatility;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.discountRate   = null;
		this.jumpIntensity = jumpIntensity;
		this.jumpSizeMean = jumpSizeMean;
		this.jumpSizeStdDev = jumpSizeStDev;
		this.randomVariableFactory = randomVariableFactory;
	}

	/**
	 * Create a Merton model.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param volatility The log volatility.
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public MertonModel(
			final double initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final double volatility,
			final DiscountCurve discountCurveForDiscountRate,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev,
			final RandomVariableFactory randomVariableFactory
			) {
		super();

		this.randomVariableFactory = randomVariableFactory;
		this.initialValue	= randomVariableFactory.createRandomVariable(initialValue);
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate	= null;
		this.volatility		= randomVariableFactory.createRandomVariable(volatility);
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate   = null;
		this.jumpIntensity	= randomVariableFactory.createRandomVariable(jumpIntensity);
		this.jumpSizeMean	= randomVariableFactory.createRandomVariable(jumpSizeMean);
		jumpSizeStdDev	= randomVariableFactory.createRandomVariable(jumpSizeStDev);
	}

	/**
	 * Create a Merton model.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param discountRate The discount rate used in the numeraire.
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public MertonModel(
			final RandomVariable initialValue,
			final RandomVariable riskFreeRate,
			final RandomVariable volatility,
			final RandomVariable discountRate,
			final RandomVariable jumpIntensity,
			final RandomVariable jumpSizeMean,
			final RandomVariable jumpSizeStDev,
			final RandomVariableFactory randomVariableFactory
			) {
		super();

		this.randomVariableFactory = randomVariableFactory;
		this.initialValue	= initialValue;
		this.discountCurveForForwardRate = null;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.discountCurveForDiscountRate = null;
		this.discountRate   = discountRate;
		this.jumpIntensity	= jumpIntensity;
		this.jumpSizeMean	= jumpSizeMean;
		this.jumpSizeStdDev	= jumpSizeStDev;
	}

	/**
	 * Create a Merton model.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param discountRate The discount rate used in the numeraire.
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 * @param randomVariableFactory The factory to be used to construct random variables.
	 */
	public MertonModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double discountRate,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev,
			final RandomVariableFactory randomVariableFactory
			) {
		this(randomVariableFactory.createRandomVariable(initialValue),
				randomVariableFactory.createRandomVariable(riskFreeRate),
				randomVariableFactory.createRandomVariable(volatility),
				randomVariableFactory.createRandomVariable(discountRate),
				randomVariableFactory.createRandomVariable(jumpIntensity),
				randomVariableFactory.createRandomVariable(jumpSizeMean),
				randomVariableFactory.createRandomVariable(jumpSizeStDev),
				randomVariableFactory);
	}

	/**
	 * Create the model from a descriptor.
	 *
	 * @param descriptor A descriptor of the model.
	 */
	public MertonModel(final MertonModelDescriptor descriptor) {
		this(descriptor.getInitialValue(),
				descriptor.getDiscountCurveForForwardRate(),
				descriptor.getVolatility(),
				descriptor.getDiscountCurveForDiscountRate(),
				descriptor.getJumpIntensity(),
				descriptor.getJumpSizeMean(),
				descriptor.getJumpSizeStdDev());
	}

	/**
	 * Create a Merton model.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param volatility The log volatility.
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 */
	public MertonModel(
			final double initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final double volatility,
			final DiscountCurve discountCurveForDiscountRate,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev
			) {
		this(initialValue, discountCurveForForwardRate, volatility, discountCurveForDiscountRate,
				jumpIntensity, jumpSizeMean, jumpSizeStDev, new RandomVariableFromArrayFactory());
	}

	/**
	 * Create a Merton model.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param discountRate The discount rate used in the numeraire.
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 */
	public MertonModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double discountRate,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev
			) {
		this(initialValue, riskFreeRate, volatility, discountRate, jumpIntensity, jumpSizeMean, jumpSizeStDev, new RandomVariableFromArrayFactory());
	}

	/**
	 * Create a Merton model.
	 *
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 * @param jumpIntensity The intensity parameter lambda of the compound Poisson process.
	 * @param jumpSizeMean The mean jump size of the normal distributes jump sizes of the compound Poisson process.
	 * @param jumpSizeStDev The standard deviation of the normal distributes jump sizes of the compound Poisson process.
	 */
	public MertonModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double jumpIntensity,
			final double jumpSizeMean,
			final double jumpSizeStDev
			) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate,jumpIntensity,jumpSizeMean,jumpSizeStDev);
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

		}else {
			riskFreeRateAtTimeStep = riskFreeRate;
		}
		return new RandomVariable[] {
				riskFreeRateAtTimeStep
				.sub(jumpSizeMean.exp().sub(1.0).mult(jumpIntensity))
				.sub(volatility.squared().div(2))
		};
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex) {
		final RandomVariable[] factors = new RandomVariable[3];
		factors[0] = volatility;
		factors[1] = jumpSizeStdDev;
		factors[2] = jumpSizeMean.sub(jumpSizeStdDev.squared().div(2));

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
	public ProcessModel getCloneWithModifiedData(final Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		final RandomVariableFactory newRandomVariableFactory = (RandomVariableFactory)dataModified.getOrDefault("randomVariableFactory", randomVariableFactory);

		final RandomVariable newInitialValue	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("initialValue"), initialValue);
		final RandomVariable newRiskFreeRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("riskFreeRate"), riskFreeRate);
		final RandomVariable newVolatility		= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("volatility"), volatility);
		final RandomVariable newDiscountRate	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("discountRate"), discountRate);
		final RandomVariable newJumpIntensity	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("riskFreeRate"), jumpIntensity);
		final RandomVariable newJumpSizeMean	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("jumpSizeMean"), jumpSizeMean);
		final RandomVariable newJumpSizeStDev	= RandomVariableFactory.getRandomVariableOrDefault(newRandomVariableFactory, dataModified.get("jumpSizeStdDev"), jumpSizeStdDev);

		return new MertonModel(newInitialValue, newRiskFreeRate, newVolatility, newDiscountRate, newJumpIntensity, newJumpSizeMean, newJumpSizeStDev, newRandomVariableFactory);
	}

	/**
	 * @return the riskFreeRate
	 */
	public RandomVariable getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * @return the volatility
	 */
	public RandomVariable getVolatility() {
		return volatility;
	}

	/**
	 * @return the jumpIntensity
	 */
	public RandomVariable getJumpIntensity() {
		return jumpIntensity;
	}

	/**
	 * @return the jumpSizeMean
	 */
	public RandomVariable getJumpSizeMean() {
		return jumpSizeMean;
	}

	/**
	 * @return the jumpSizeStdDev
	 */
	public RandomVariable getJumpSizeStdDev() {
		return jumpSizeStdDev;
	}
}
