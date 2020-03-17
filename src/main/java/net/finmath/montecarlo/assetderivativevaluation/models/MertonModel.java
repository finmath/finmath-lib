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

	private final RandomVariableFactory randomVariableFactory;

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)

	private final double volatility;

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final double jumpIntensity;
	private final double jumpSizeMean;
	private final double jumpSizeStdDev;

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
		super();

		this.randomVariableFactory = new RandomVariableFromArrayFactory();
		this.initialValue	= initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate	= Double.NaN;
		this.volatility		= volatility;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate   = Double.NaN;
		this.jumpIntensity	= jumpIntensity;
		this.jumpSizeMean	= jumpSizeMean;
		jumpSizeStdDev	= jumpSizeStDev;
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
		super();

		this.randomVariableFactory = new RandomVariableFromArrayFactory();
		this.initialValue	= initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		discountCurveForDiscountRate = null;
		this.discountRate   = discountRate;
		this.jumpIntensity	= jumpIntensity;
		this.jumpSizeMean	= jumpSizeMean;
		jumpSizeStdDev	= jumpSizeStDev;
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
	public RandomVariable applyStateSpaceTransform(final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(final int componentIndex, final RandomVariable randomVariable) {
		return randomVariable.log();
	}

	@Override
	public RandomVariable[] getInitialState(MonteCarloProcess process) {
		return new RandomVariable[] { getRandomVariableForConstant(Math.log(initialValue)) };
	}

	@Override
	public RandomVariable getNumeraire(MonteCarloProcess process, final double time) {
			if(discountCurveForDiscountRate != null) {
				return getRandomVariableForConstant(1.0/discountCurveForDiscountRate.getDiscountFactor(time));
			}
			else {
				return getRandomVariableForConstant(Math.exp(discountRate * time));
			}
	}

	@Override
	public RandomVariable[] getDrift(final MonteCarloProcess process, final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		double riskFreeRateAtTimeStep = 0.0;
		if(discountCurveForForwardRate != null) {
			final double time		= process.getTime(timeIndex);
			final double timeNext	= process.getTime(timeIndex+1);

			riskFreeRateAtTimeStep = Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time);

		}else {
			riskFreeRateAtTimeStep = riskFreeRate;
		}
		return new RandomVariable[] { process.getStochasticDriver().getRandomVariableForConstant(riskFreeRateAtTimeStep - (Math.exp(jumpSizeMean)-1)*jumpIntensity - 0.5 * volatility*volatility) };
	}

	@Override
	public RandomVariable[] getFactorLoading(final MonteCarloProcess process, final int timeIndex, final int componentIndex, final RandomVariable[] realizationAtTimeIndex) {
		final RandomVariable[] factors = new RandomVariable[3];
		factors[0] = process.getStochasticDriver().getRandomVariableForConstant(volatility);
		factors[1] = process.getStochasticDriver().getRandomVariableForConstant(jumpSizeStdDev);
		factors[2] = process.getStochasticDriver().getRandomVariableForConstant(jumpSizeMean - 0.5*jumpSizeStdDev*jumpSizeStdDev);

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
		final double newInitialValue = (double) dataModified.getOrDefault("initialValue", initialValue);
		final double newRiskFreeRate = (double) dataModified.getOrDefault("riskFreeRate", riskFreeRate);
		final double newVolatility = (double) dataModified.getOrDefault("volatility", volatility);
		final double newDiscountRate = (double) dataModified.getOrDefault("discountRate", discountRate);
		final double newJumpIntensity = (double) dataModified.getOrDefault("jumpIntensity", jumpIntensity);
		final double newJumpSizeMean = (double) dataModified.getOrDefault("jumpSizeMean", jumpSizeMean);
		final double newJumpSizeStDev = (double) dataModified.getOrDefault("jumpSizeStdDev", jumpSizeStdDev);
		return new MertonModel(newInitialValue,newRiskFreeRate,newVolatility,newDiscountRate,newJumpIntensity,newJumpSizeMean,newJumpSizeStDev);
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
