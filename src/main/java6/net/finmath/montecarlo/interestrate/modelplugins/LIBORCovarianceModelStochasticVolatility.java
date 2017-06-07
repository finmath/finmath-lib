/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15 Jan 2015
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.BrownianMotionView;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Simple stochastic volatility model, using a process
 * \[
 * 	d\lambda(t) = \nu \lambda(t) \left( \rho \mathrm{d} W_{1}(t) + \sqrt{1-\rho^{2}} \mathrm{d} W_{2}(t) \right) \text{,}
 * \]
 * where \( \lambda(0) = 1 \) to scale all factor loadings \( f_{i} \) returned by a given covariance model.
 * 
 * The model constructed is \( \lambda(t) F(t) \) where \( \lambda(t) \) is
 * the (Euler discretization of the) above process and \( F = ( f_{1}, \ldots, f_{m} ) \) is the factor loading
 * from the given covariance model.
 * 
 * The process uses the first two factors of the Brownian motion provided by an object implementing
 * {@link net.finmath.montecarlo.BrownianMotionInterface}. This can be used to generate correlations to
 * other objects. If you like to reuse a factor of another Brownian motion use a
 * {@link net.finmath.montecarlo.BrownianMotionView}
 * to delegate \( ( \mathrm{d} W_{1}(t) , \mathrm{d} W_{2}(t) ) \) to a different object.
 * 
 * The parameter of this model is a joint parameter vector, consisting
 * of the parameter vector of the given base covariance model and
 * appending the parameters <i>&nu;</i> and <i>&rho;</i> at the end.
 * 
 * If this model is not calibrateable, its parameter vector is that of the
 * covariance model, i.e., <i>&nu;</i> and <i>&rho;</i> will be not
 * part of the calibration.
 * 
 * For an illustration of its usage see the associated unit test.
 * 
 * @author Christian Fries
 */
public class LIBORCovarianceModelStochasticVolatility extends AbstractLIBORCovarianceModelParametric {

	private AbstractLIBORCovarianceModelParametric covarianceModel;
	private BrownianMotionInterface brownianMotion;
	private	double rho, nu;

	private boolean isCalibrateable = false;

	private AbstractProcessInterface stochasticVolatilityScalings = null;

	/**
	 * Create a modification of a given {@link AbstractLIBORCovarianceModelParametric} with a stochastic volatility scaling.
	 * 
	 * @param covarianceModel A given AbstractLIBORCovarianceModelParametric.
	 * @param brownianMotion An object implementing {@link BrownianMotionInterface} with at least two factors. This class uses the first two factors, but you may use {@link BrownianMotionView} to change this.
	 * @param nu The initial value for <i>&nu;</i>, the volatility of the volatility.
	 * @param rho The initial value for <i>&rho;</i> the correlation to the first factor.
	 * @param isCalibrateable If true, the parameters <i>&nu;</i> and <i>&rho;</i> are parameters. Note that the covariance model (<code>covarianceModel</code>) may have its own parameter calibration settings.
	 */
	public LIBORCovarianceModelStochasticVolatility(AbstractLIBORCovarianceModelParametric covarianceModel, BrownianMotionInterface brownianMotion, double nu, double rho, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());

		this.covarianceModel = covarianceModel;
		this.brownianMotion = brownianMotion;
		this.nu		= nu;
		this.rho	= rho;
		
		this.isCalibrateable = isCalibrateable;
	}

	@Override
	public double[] getParameter() {
		if(!isCalibrateable) return covarianceModel.getParameter();

		double[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) return new double[] { nu, rho };

		// Append nu and rho to the end of covarianceParameters
		double[] jointParameters = new double[covarianceParameters.length+2];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length+0] = nu;
		jointParameters[covarianceParameters.length+1] = rho;

		return jointParameters;
	}

//	@Override
	private void setParameter(double[] parameter) {
		if(parameter == null || parameter.length == 0) return;

		if(!isCalibrateable) {
			covarianceModel = covarianceModel.getCloneWithModifiedParameters(parameter);
			return;
		}

		double[] covarianceParameters = new double[parameter.length-2];
		System.arraycopy(parameter, 0, covarianceParameters, 0, covarianceParameters.length);

		covarianceModel = covarianceModel.getCloneWithModifiedParameters(covarianceParameters);
		
		nu	= parameter[covarianceParameters.length + 0];
		rho	= parameter[covarianceParameters.length + 1];

		stochasticVolatilityScalings = null;
	}

	@Override
	public Object clone() {
		LIBORCovarianceModelStochasticVolatility newModel = new LIBORCovarianceModelStochasticVolatility((AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), brownianMotion, nu, rho, isCalibrateable);
		return newModel;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		LIBORCovarianceModelStochasticVolatility model = (LIBORCovarianceModelStochasticVolatility)this.clone();
		model.setParameter(parameters);
		return model;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {

		synchronized (this) {
			if(stochasticVolatilityScalings == null) {
				stochasticVolatilityScalings = new ProcessEulerScheme(brownianMotion);
				((AbstractProcess) stochasticVolatilityScalings).setModel(new AbstractModelInterface() {

					@Override
					public void setProcess(AbstractProcessInterface process) {
					}

					@Override
					public TimeDiscretizationInterface getTimeDiscretization() {
						return brownianMotion.getTimeDiscretization();
					}

					@Override
					public AbstractProcessInterface getProcess() {
						return stochasticVolatilityScalings;
					}

					@Override
					public RandomVariableInterface getNumeraire(double time) throws CalculationException {
						return null;
					}

					@Override
					public int getNumberOfFactors() {
						return 2;
					}

					@Override
					public int getNumberOfComponents() {
						return 1;
					}

					@Override
					public RandomVariableInterface[] getInitialState() {
						return new RandomVariableInterface[] { brownianMotion.getRandomVariableForConstant(0.0) };
					}

					@Override
					public RandomVariableInterface[] getFactorLoading(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex) {
						return new RandomVariableInterface[] { brownianMotion.getRandomVariableForConstant(rho * nu) , brownianMotion.getRandomVariableForConstant(Math.sqrt(1.0 - rho*rho) * nu) };
					}

					@Override
					public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
						return new RandomVariableInterface[] { brownianMotion.getRandomVariableForConstant(- 0.5 * nu*nu) };
					}

					@Override
					public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
						return randomVariable.exp();
					}

					@Override
					public AbstractModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
						throw new UnsupportedOperationException("Method not implemented");
					}
				});

			}
		}

		RandomVariableInterface stochasticVolatilityScaling = null;
		try {
			stochasticVolatilityScaling = stochasticVolatilityScalings.getProcessValue(timeIndex,0);
		} catch (CalculationException e) {
			// Exception is not handled explicitly, we just return null
		}

		RandomVariableInterface[] factorLoading = null;

		if(stochasticVolatilityScaling != null) {
			factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);
			for(int i=0; i<factorLoading.length; i++) {
				factorLoading[i] = factorLoading[i].mult(stochasticVolatilityScaling);
			}
		}

		return factorLoading;
	}

	@Override
	public RandomVariableInterface getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariableInterface[] realizationAtTimeIndex) {
		return null;
	}
}
