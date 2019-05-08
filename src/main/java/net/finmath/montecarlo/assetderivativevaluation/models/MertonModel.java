/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.modelling.descriptor.MertonModelDescriptor;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.model.ProcessModel;
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
	public MertonModel(MertonModelDescriptor descriptor) {
		this(descriptor.getInitialValue(),
			descriptor.getDiscountCurveForDiscountRate(),
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
			double initialValue,
			DiscountCurve discountCurveForForwardRate,
			double volatility,
			DiscountCurve discountCurveForDiscountRate,
			double jumpIntensity,
			double jumpSizeMean,
			double jumpSizeStDev
			) {
		super();

		this.initialValue	= initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.riskFreeRate	= Double.NaN;;
		this.volatility		= volatility;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.discountRate   = Double.NaN;
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
			double initialValue,
			double riskFreeRate,
			double volatility,
			double discountRate,
			double jumpIntensity,
			double jumpSizeMean,
			double jumpSizeStDev
			) {
		super();

		this.initialValue	= initialValue;
		this.discountCurveForForwardRate = null;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.discountCurveForDiscountRate = null;
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
			double initialValue,
			double riskFreeRate,
			double volatility,
			double jumpIntensity,
			double jumpSizeMean,
			double jumpSizeStDev
			) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate,jumpIntensity,jumpSizeMean,jumpSizeStDev);
	}

	@Override
	public int getNumberOfComponents() {
		return 1;
	}

	@Override
	public RandomVariable applyStateSpaceTransform(int componentIndex, RandomVariable randomVariable) {
		return randomVariable.exp();
	}

	@Override
	public RandomVariable applyStateSpaceTransformInverse(int componentIndex, RandomVariable randomVariable) {
		return randomVariable.log();
	}

	@Override
	public RandomVariable[] getInitialState() {
		return new RandomVariable[] { getProcess().getStochasticDriver().getRandomVariableForConstant(Math.log(initialValue)) };
	}

	@Override
	public RandomVariable getNumeraire(double time) throws CalculationException {
		if(discountCurveForDiscountRate != null) {
			return getProcess().getStochasticDriver().getRandomVariableForConstant(1.0/discountCurveForDiscountRate.getDiscountFactor(time));
		}
		else {
			return getProcess().getStochasticDriver().getRandomVariableForConstant(Math.exp(discountRate * time));
		}
	}

	@Override
	public RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
		double riskFreeRateAtTimeStep = 0.0;
		if(discountCurveForForwardRate != null) {
			double time		= getTime(timeIndex);
			double timeNext	= getTime(timeIndex+1);

			riskFreeRateAtTimeStep = Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time);

		}else {
			riskFreeRateAtTimeStep = riskFreeRate;
		}
		return new RandomVariable[] { getProcess().getStochasticDriver().getRandomVariableForConstant(riskFreeRateAtTimeStep - (Math.exp(jumpSizeMean)-1)*jumpIntensity - 0.5 * volatility*volatility) };
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex) {
		RandomVariable[] factors = new RandomVariable[3];
		factors[0] = getProcess().getStochasticDriver().getRandomVariableForConstant(volatility);
		factors[1] = getProcess().getStochasticDriver().getRandomVariableForConstant(jumpSizeStdDev);
		factors[2] = getProcess().getStochasticDriver().getRandomVariableForConstant(jumpSizeMean - 0.5*jumpSizeStdDev*jumpSizeStdDev);

		return factors;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.ProcessModel#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public ProcessModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		double newInitialValue = (double) dataModified.getOrDefault("initialValue", initialValue);
		double newRiskFreeRate = (double) dataModified.getOrDefault("riskFreeRate", riskFreeRate);
		double newVolatility = (double) dataModified.getOrDefault("volatility", volatility);
		double newDiscountRate = (double) dataModified.getOrDefault("discountRate", discountRate);
		double newJumpIntensity = (double) dataModified.getOrDefault("jumpIntensity", jumpIntensity);
		double newJumpSizeMean = (double) dataModified.getOrDefault("jumpSizeMean", jumpSizeMean);
		double newJumpSizeStDev = (double) dataModified.getOrDefault("jumpSizeStdDev", jumpSizeStdDev);
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
