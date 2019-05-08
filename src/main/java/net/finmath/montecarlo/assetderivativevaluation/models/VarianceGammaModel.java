package net.finmath.montecarlo.assetderivativevaluation.models;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.model.ProcessModel;
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

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;		// Constant rate, used if discountCurveForForwardRate is null

	private final double sigma;
	private final double theta;
	private final double nu;

	/**
	 * Construct a Variance Gamma model with discount curves for the forward price (i.e. repo rate minus dividend yield) and for discounting.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param sigma The parameter \( \sigma \)
	 * @param theta The parameter \( \theta \)
	 * @param nu The parameter \( \nu \)
	 */
	public VarianceGammaModel(double initialValue, DiscountCurve discountCurveForForwardRate,
			DiscountCurve discountCurveForDiscountRate, double sigma, double theta, double nu) {
		super();
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = Double.NaN;
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
	 * @param sigma The parameter \( \sigma \)
	 * @param theta The parameter \( \theta \)
	 * @param nu The parameter \( \nu \)
	 */
	public VarianceGammaModel(double initialValue, double riskFreeRate, double discountRate, double sigma, double theta,
			double nu) {
		super();
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
	 * Construct a Variance Gamma model with constant rates for the forward price (i.e. repo rate minus dividend yield) and for the discount curve.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate The constant risk free rate for the drift (repo rate of the underlying).
	 * @param sigma The parameter \( \sigma \)
	 * @param theta The parameter \( \theta \)
	 * @param nu The parameter \( \nu \)
	 */
	public VarianceGammaModel(double initialValue, double riskFreeRate, double sigma, double theta, double nu) {
		this(initialValue,riskFreeRate,riskFreeRate,sigma,theta,nu);
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
		return new RandomVariable[] {getProcess().getStochasticDriver().getRandomVariableForConstant(Math.log(initialValue))};
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
	public RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex,
			RandomVariable[] realizationPredictor) {
		double riskFreeRateAtTimeStep = 0.0;
		if(discountCurveForForwardRate != null) {
			double time		= getTime(timeIndex);
			double timeNext	= getTime(timeIndex+1);

			riskFreeRateAtTimeStep = Math.log(discountCurveForForwardRate.getDiscountFactor(time) / discountCurveForForwardRate.getDiscountFactor(timeNext)) / (timeNext-time);

		}else {
			riskFreeRateAtTimeStep = riskFreeRate;
		}
		return new RandomVariable[] {getProcess().getStochasticDriver()
				.getRandomVariableForConstant(riskFreeRateAtTimeStep-1/nu * Math.log(1/(1.0-theta*nu-0.5*sigma*sigma*nu)))};
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int componentIndex,
			RandomVariable[] realizationAtTimeIndex) {
		RandomVariable[] factors = new RandomVariable[1];
		factors[0] = getProcess().getStochasticDriver().getRandomVariableForConstant(1.0);
		return factors;
	}

	/*
	 * (non-Javadoc)
	 * @see net.finmath.montecarlo.model.ProcessModel#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
	}

	@Override
	public ProcessModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
		double newInitialValue = (double) dataModified.getOrDefault("initialValue", initialValue);
		double newRiskFreeRate = (double) dataModified.getOrDefault("riskFreeRate", riskFreeRate);
		double newDiscountRate = (double) dataModified.getOrDefault("discountRate", discountRate);
		double newSigma = (double) dataModified.getOrDefault("volatility", sigma);
		double newTheta = (double) dataModified.getOrDefault("volatility", theta);
		double newNu = (double) dataModified.getOrDefault("volatility", nu);
		return new VarianceGammaModel(newInitialValue,newRiskFreeRate,newDiscountRate,newSigma,newTheta,newNu);
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
	public double getRiskFreeRate() {
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
	public double getDiscountRate() {
		return discountRate;
	}

	/**
	 * @return the sigma
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * @return the theta
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * @return the nu
	 */
	public double getNu() {
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
