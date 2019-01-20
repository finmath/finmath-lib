/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 15 Jan 2015
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import java.time.LocalDateTime;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionView;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

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
 * {@link net.finmath.montecarlo.BrownianMotion}. This can be used to generate correlations to
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
 * @version 1.0
 */
public class LIBORCovarianceModelStochasticVolatility extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = -559341617850035368L;
	private AbstractLIBORCovarianceModelParametric covarianceModel;
	private BrownianMotion brownianMotion;
	private	RandomVariable rho, nu;

	private boolean isCalibrateable = false;

	private MonteCarloProcess stochasticVolatilityScalings = null;

	/**
	 * Create a modification of a given {@link AbstractLIBORCovarianceModelParametric} with a stochastic volatility scaling.
	 *
	 * @param covarianceModel A given AbstractLIBORCovarianceModelParametric.
	 * @param brownianMotion An object implementing {@link BrownianMotion} with at least two factors. This class uses the first two factors, but you may use {@link BrownianMotionView} to change this.
	 * @param nu The initial value for <i>&nu;</i>, the volatility of the volatility.
	 * @param rho The initial value for <i>&rho;</i> the correlation to the first factor.
	 * @param isCalibrateable If true, the parameters <i>&nu;</i> and <i>&rho;</i> are parameters. Note that the covariance model (<code>covarianceModel</code>) may have its own parameter calibration settings.
	 */
	public LIBORCovarianceModelStochasticVolatility(AbstractLIBORCovarianceModelParametric covarianceModel, BrownianMotion brownianMotion, RandomVariable nu, RandomVariable rho, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());

		this.covarianceModel = covarianceModel;
		this.brownianMotion = brownianMotion;
		this.nu		= nu;
		this.rho		= rho;

		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Create a modification of a given {@link AbstractLIBORCovarianceModelParametric} with a stochastic volatility scaling.
	 *
	 * @param covarianceModel A given AbstractLIBORCovarianceModelParametric.
	 * @param brownianMotion An object implementing {@link BrownianMotion} with at least two factors. This class uses the first two factors, but you may use {@link BrownianMotionView} to change this.
	 * @param nu The initial value for <i>&nu;</i>, the volatility of the volatility.
	 * @param rho The initial value for <i>&rho;</i> the correlation to the first factor.
	 * @param isCalibrateable If true, the parameters <i>&nu;</i> and <i>&rho;</i> are parameters. Note that the covariance model (<code>covarianceModel</code>) may have its own parameter calibration settings.
	 */
	public LIBORCovarianceModelStochasticVolatility(AbstractLIBORCovarianceModelParametric covarianceModel, BrownianMotion brownianMotion, double nu, double rho, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());

		this.covarianceModel = covarianceModel;
		this.brownianMotion = brownianMotion;
		this.nu		= new Scalar(nu);
		this.rho	= new Scalar(rho);

		this.isCalibrateable = isCalibrateable;
	}

	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return covarianceModel.getParameter();
		}

		RandomVariable[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) {
			return new RandomVariable[] { nu, rho };
		}

		// Append nu and rho to the end of covarianceParameters
		RandomVariable[] jointParameters = new RandomVariable[covarianceParameters.length+2];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length+0] = nu;
		jointParameters[covarianceParameters.length+1] = rho;

		return jointParameters;
	}

	//	@Override
	private void setParameter(RandomVariable[] parameter) {
		if(parameter == null || parameter.length == 0) {
			return;
		}

		if(!isCalibrateable) {
			covarianceModel = covarianceModel.getCloneWithModifiedParameters(parameter);
			return;
		}

		RandomVariable[] covarianceParameters = new RandomVariable[parameter.length-2];
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
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariable[] parameters) {
		LIBORCovarianceModelStochasticVolatility model = (LIBORCovarianceModelStochasticVolatility)this.clone();
		model.setParameter(parameters);
		return model;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		return getCloneWithModifiedParameters(Scalar.arrayOf(parameters));
	}

	@Override
	public double[] getParameterAsDouble() {
		RandomVariable[] parameters = getParameter();
		double[] parametersAsDouble = new double[parameters.length];
		for(int i=0; i<parameters.length; i++) parametersAsDouble[i] = parameters[i].doubleValue();
		return parametersAsDouble;
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {

		synchronized (this) {
			if(stochasticVolatilityScalings == null) {
				stochasticVolatilityScalings = new EulerSchemeFromProcessModel(brownianMotion);
				stochasticVolatilityScalings.setModel(new ProcessModel() {

					@Override
					public void setProcess(MonteCarloProcess process) {
					}

					@Override
					public LocalDateTime getReferenceDate() {
						throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
					}

					@Override
					public TimeDiscretization getTimeDiscretization() {
						return brownianMotion.getTimeDiscretization();
					}

					@Override
					public MonteCarloProcess getProcess() {
						return stochasticVolatilityScalings;
					}

					@Override
					public RandomVariable getNumeraire(double time) {
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
					public RandomVariable[] getInitialState() {
						return new RandomVariable[] { brownianMotion.getRandomVariableForConstant(0.0) };
					}

					@Override
					public RandomVariable[] getFactorLoading(int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex) {
						return new RandomVariable[] { rho.mult(nu) , rho.squared().sub(1).mult(-1).sqrt().mult(nu) };
					}

					@Override
					public RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
						return new RandomVariable[] { nu.squared().mult(-0.5) };
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
					public RandomVariable getRandomVariableForConstant(double value) {
						return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
					}

					@Override
					public ProcessModel getCloneWithModifiedData(Map<String, Object> dataModified) {
						throw new UnsupportedOperationException("Method not implemented");
					}
				});

			}
		}

		RandomVariable stochasticVolatilityScaling = null;
		try {
			stochasticVolatilityScaling = stochasticVolatilityScalings.getProcessValue(timeIndex,0);
		} catch (CalculationException e) {
			// Exception is not handled explicitly, we just return null
		}

		RandomVariable[] factorLoading = null;

		if(stochasticVolatilityScaling != null) {
			factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);
			for(int i=0; i<factorLoading.length; i++) {
				factorLoading[i] = factorLoading[i].mult(stochasticVolatilityScaling);
			}
		}

		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex) {
		return null;
	}
}
