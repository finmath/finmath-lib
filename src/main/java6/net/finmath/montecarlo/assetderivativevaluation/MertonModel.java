/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f = exp \), \( \mu = r - \frac{1}{2} \sigma^2 - (exp(a)-1) \lambda \), \( \lambda_{1,1} = \sigma, \lambda_{1,2} = a - \frac{1}{2} b^2, \lambda_{1,3} = b \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW + \lambda_{1,2} dN + \lambda_{1,3} Z dN, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.AbstractProcessInterface} for the notation.
 *
 * For an example on the construction of the three factors \( dW \), \( dN \), and \( Z dN \) see {@link net.finmath.montecarlo.assetderivativevaluation.MonteCarloMertonModel}.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.assetderivativevaluation.MonteCarloMertonModel
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
	public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
		return randomVariable.log();
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

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.model.AbstractModelInterface#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
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

