package net.finmath.montecarlo.interestrate.covariancemodels;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.interestrate.modelplugins.LIBORVolatilityModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * As Heston like stochastic volatility model, using a process \( lambda(t) = \sqrt(V(t)) \)
 * \[
 * 	dV(t) = \kappa ( \theta - V(t) ) dt + \xi \sqrt{V(t)} dW_{1}(t), \quad V(0) = 1.0,
 * \]
 * where \( \lambda(0) = 1 \) to scale all factor loadings \( f_{i} \) returned by a given covariance model.
 *
 * The model constructed is \( \lambda(t) F(t) \) where \( \lambda(t) \) is
 * a discretization of the above process and \( F = ( f_{1}, \ldots, f_{m} ) \) is the factor loading
 * from the given covariance model.
 *
 * The process uses the first factor of the Brownian motion provided by an object implementing
 * {@link net.finmath.montecarlo.BrownianMotionInterface}. This can be used to generate correlations to
 * other objects. If you like to reuse a factor of another Brownian motion use a
 * {@link net.finmath.montecarlo.BrownianMotionView}
 * to delegate \( ( \mathrm{d} W_{1}(t) ) \) to a different object.
 *
 * The parameter of this model is a joint parameter vector, consisting
 * of the parameter vector of the given base covariance model and
 * appending the parameters <i>&kappa;</i>, <i>&theta;</i> and <i>&xi;</i> at the end.
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
public class LIBORCovarianceModelStochasticHestonVolatility extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = -1438451123632424212L;
	private AbstractLIBORCovarianceModelParametric covarianceModel;
	private BrownianMotionInterface brownianMotion;
	private	RandomVariableInterface kappa, theta, xi;

	private boolean isCalibrateable = false;

	private AbstractProcessInterface stochasticVolatilityScalings = null;

	/**
	 * Create a modification of a given {@link AbstractLIBORCovarianceModelParametric} with a stochastic volatility scaling.
	 *
	 * @param covarianceModel A given AbstractLIBORCovarianceModelParametric.
	 * @param brownianMotion An object implementing {@link BrownianMotionInterface} with at least two factors. This class uses the first two factors, but you may use {@link net.finmath.montecarlo.BrownianMotionView} to change this.
	 * @param kappa The initial value for <i>&kappa;</i>, the mean reversion speed of the variance process V.
	 * @param theta The initial value for <i>&theta;</i> the mean reversion level of the variance process V.
	 * @param xi The initial value for <i>&xi;</i> the volatility of the variance process V.
	 * @param isCalibrateable If true, the parameters <i>&nu;</i> and <i>&rho;</i> are parameters. Note that the covariance model (<code>covarianceModel</code>) may have its own parameter calibration settings.
	 */
	public LIBORCovarianceModelStochasticHestonVolatility(AbstractLIBORCovarianceModelParametric covarianceModel, BrownianMotionInterface brownianMotion, RandomVariableInterface kappa, RandomVariableInterface theta, RandomVariableInterface xi, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());

		this.covarianceModel = covarianceModel;
		this.brownianMotion = brownianMotion;
		this.kappa = kappa;
		this.theta = theta;
		this.xi = xi;

		this.isCalibrateable = isCalibrateable;
	}

	@Override
	public RandomVariableInterface[] getParameter() {
		if(!isCalibrateable) {
			return covarianceModel.getParameter();
		}

		RandomVariableInterface[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) {
			return new RandomVariableInterface[] { theta, kappa, xi };
		}

		// Append nu and rho to the end of covarianceParameters
		RandomVariableInterface[] jointParameters = new RandomVariableInterface[covarianceParameters.length+3];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length+0] = kappa;
		jointParameters[covarianceParameters.length+1] = theta;
		jointParameters[covarianceParameters.length+2] = xi;

		return jointParameters;
	}

	//	@Override
	private void setParameter(RandomVariableInterface[] parameter) {
		if(parameter == null || parameter.length == 0) {
			return;
		}

		if(!isCalibrateable) {
			covarianceModel = covarianceModel.getCloneWithModifiedParameters(parameter);
			return;
		}

		RandomVariableInterface[] covarianceParameters = new RandomVariableInterface[parameter.length-3];
		System.arraycopy(parameter, 0, covarianceParameters, 0, covarianceParameters.length);

		covarianceModel = covarianceModel.getCloneWithModifiedParameters(covarianceParameters);

		kappa	= parameter[covarianceParameters.length + 0];
		theta	= parameter[covarianceParameters.length + 1];
		xi		= parameter[covarianceParameters.length + 2];

		stochasticVolatilityScalings = null;
	}

	@Override
	public Object clone() {
		LIBORCovarianceModelStochasticHestonVolatility newModel = new LIBORCovarianceModelStochasticHestonVolatility((AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), brownianMotion, kappa, theta, xi, isCalibrateable);
		return newModel;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariableInterface[] parameters) {
		LIBORCovarianceModelStochasticHestonVolatility model = (LIBORCovarianceModelStochasticHestonVolatility)this.clone();
		model.setParameter(parameters);
		return model;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {

		synchronized (this) {
			if(stochasticVolatilityScalings == null) {
				stochasticVolatilityScalings = new ProcessEulerScheme(brownianMotion);
				stochasticVolatilityScalings.setModel(new AbstractModelInterface() {

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
					public RandomVariableInterface getNumeraire(double time) {
						return null;
					}

					@Override
					public int getNumberOfFactors() {
						return 1;
					}

					@Override
					public int getNumberOfComponents() {
						return 1;
					}

					@Override
					public RandomVariableInterface[] getInitialState() {
						return new RandomVariableInterface[] { brownianMotion.getRandomVariableForConstant(1.0) };
					}

					@Override
					public RandomVariableInterface[] getFactorLoading(int timeIndex, int componentIndex, RandomVariableInterface[] realizationAtTimeIndex) {
						return new RandomVariableInterface[] { realizationAtTimeIndex[0].floor(0).sqrt().mult(xi) };
					}

					@Override
					public RandomVariableInterface[] getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
						return new RandomVariableInterface[] { realizationAtTimeIndex[0].sub(theta).mult(kappa.mult(-1)) };
					}

					@Override
					public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
						return randomVariable;
					}

					@Override
					public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
						return randomVariable;
					}

					@Override
					public RandomVariableInterface getRandomVariableForConstant(double value) {
						return getProcess().getStochasticDriver().getRandomVariableForConstant(value);
					}

					@Override
					public AbstractModelInterface getCloneWithModifiedData(Map<String, Object> dataModified) {
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
				factorLoading[i] = factorLoading[i].mult(stochasticVolatilityScaling.floor(0.0).sqrt());
			}
		}

		return factorLoading;
	}

	@Override
	public RandomVariableInterface getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariableInterface[] realizationAtTimeIndex) {
		return null;
	}
}
