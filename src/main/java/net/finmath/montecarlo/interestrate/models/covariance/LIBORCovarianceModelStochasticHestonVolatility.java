package net.finmath.montecarlo.interestrate.models.covariance;

import java.time.LocalDateTime;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;
import net.finmath.time.TimeDiscretization;

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
 * {@link net.finmath.montecarlo.BrownianMotion}. This can be used to generate correlations to
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
	private BrownianMotion brownianMotion;
	private	RandomVariable kappa, theta, xi;

	private boolean isCalibrateable = false;

	private transient MonteCarloProcess stochasticVolatilityScalings = null;

	/**
	 * Create a modification of a given {@link AbstractLIBORCovarianceModelParametric} with a stochastic volatility scaling.
	 *
	 * @param covarianceModel A given AbstractLIBORCovarianceModelParametric.
	 * @param brownianMotion An object implementing {@link BrownianMotion} with at least two factors. This class uses the first two factors, but you may use {@link net.finmath.montecarlo.BrownianMotionView} to change this.
	 * @param kappa The initial value for <i>&kappa;</i>, the mean reversion speed of the variance process V.
	 * @param theta The initial value for <i>&theta;</i> the mean reversion level of the variance process V.
	 * @param xi The initial value for <i>&xi;</i> the volatility of the variance process V.
	 * @param isCalibrateable If true, the parameters <i>&nu;</i> and <i>&rho;</i> are parameters. Note that the covariance model (<code>covarianceModel</code>) may have its own parameter calibration settings.
	 */
	public LIBORCovarianceModelStochasticHestonVolatility(AbstractLIBORCovarianceModelParametric covarianceModel, BrownianMotion brownianMotion, RandomVariable kappa, RandomVariable theta, RandomVariable xi, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());

		this.covarianceModel = covarianceModel;
		this.brownianMotion = brownianMotion;
		this.kappa = kappa;
		this.theta = theta;
		this.xi = xi;

		this.isCalibrateable = isCalibrateable;
	}

	/**
	 * Create a modification of a given {@link AbstractLIBORCovarianceModelParametric} with a stochastic volatility scaling.
	 *
	 * @param covarianceModel A given AbstractLIBORCovarianceModelParametric.
	 * @param brownianMotion An object implementing {@link BrownianMotion} with at least two factors. This class uses the first two factors, but you may use {@link net.finmath.montecarlo.BrownianMotionView} to change this.
	 * @param kappa The initial value for <i>&kappa;</i>, the mean reversion speed of the variance process V.
	 * @param theta The initial value for <i>&theta;</i> the mean reversion level of the variance process V.
	 * @param xi The initial value for <i>&xi;</i> the volatility of the variance process V.
	 * @param isCalibrateable If true, the parameters <i>&nu;</i> and <i>&rho;</i> are parameters. Note that the covariance model (<code>covarianceModel</code>) may have its own parameter calibration settings.
	 */
	public LIBORCovarianceModelStochasticHestonVolatility(AbstractLIBORCovarianceModelParametric covarianceModel, BrownianMotion brownianMotion, double kappa, double theta, double xi, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());

		this.covarianceModel = covarianceModel;
		this.brownianMotion = brownianMotion;
		this.kappa = new Scalar(kappa);
		this.theta = new Scalar(theta);
		this.xi = new Scalar(xi);

		this.isCalibrateable = isCalibrateable;
	}

	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return covarianceModel.getParameter();
		}

		RandomVariable[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) {
			return new RandomVariable[] { theta, kappa, xi };
		}

		// Append nu and rho to the end of covarianceParameters
		RandomVariable[] jointParameters = new RandomVariable[covarianceParameters.length+3];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length+0] = kappa;
		jointParameters[covarianceParameters.length+1] = theta;
		jointParameters[covarianceParameters.length+2] = xi;

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

		RandomVariable[] covarianceParameters = new RandomVariable[parameter.length-3];
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
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariable[] parameters) {
		LIBORCovarianceModelStochasticHestonVolatility model = (LIBORCovarianceModelStochasticHestonVolatility)this.clone();
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
						return 1;
					}

					@Override
					public int getNumberOfComponents() {
						return 1;
					}

					@Override
					public RandomVariable[] getInitialState() {
						return new RandomVariable[] { brownianMotion.getRandomVariableForConstant(1.0) };
					}

					@Override
					public RandomVariable[] getFactorLoading(int timeIndex, int componentIndex, RandomVariable[] realizationAtTimeIndex) {
						return new RandomVariable[] { realizationAtTimeIndex[0].floor(0).sqrt().mult(xi) };
					}

					@Override
					public RandomVariable[] getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
						return new RandomVariable[] { realizationAtTimeIndex[0].sub(theta).mult(kappa.mult(-1)) };
					}

					@Override
					public RandomVariable applyStateSpaceTransform(int componentIndex, RandomVariable randomVariable) {
						return randomVariable;
					}

					@Override
					public RandomVariable applyStateSpaceTransformInverse(int componentIndex, RandomVariable randomVariable) {
						return randomVariable;
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
				factorLoading[i] = factorLoading[i].mult(stochasticVolatilityScaling.floor(0.0).sqrt());
			}
		}

		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex) {
		return null;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(Map<String, Object> dataModified)
			throws CalculationException {
		AbstractLIBORCovarianceModelParametric covarianceModel = this.covarianceModel;
		BrownianMotion brownianMotion = this.brownianMotion;
		RandomVariable kappa = this.kappa;
		RandomVariable theta = this.theta;
		RandomVariable xi = this.xi;
		boolean isCalibrateable = this.isCalibrateable;
		AbstractRandomVariableFactory randomVariableFactory = null;

		if(dataModified != null) {
			if(dataModified.containsKey("randomVariableFactory")) {
				randomVariableFactory = (AbstractRandomVariableFactory)dataModified.get("randomVariableFactory");
				kappa = randomVariableFactory.createRandomVariable(kappa.doubleValue());
				theta = randomVariableFactory.createRandomVariable(theta.doubleValue());
				xi = randomVariableFactory.createRandomVariable(xi.doubleValue());
			}
			if(!dataModified.containsKey("covarianceModel"))
				covarianceModel = covarianceModel.getCloneWithModifiedData(dataModified);

			// Explicitly passed covarianceModel has priority
			covarianceModel = (AbstractLIBORCovarianceModelParametric)dataModified.getOrDefault("covarianceModel", covarianceModel);
			isCalibrateable = (boolean)dataModified.getOrDefault("isCalibrateable", isCalibrateable);
			brownianMotion = (BrownianMotion)dataModified.getOrDefault("brownianMotion", brownianMotion);

			if(dataModified.getOrDefault("kappa", kappa) instanceof RandomVariable) {
				kappa = (RandomVariable)dataModified.getOrDefault("kappa", kappa);
			}else if(randomVariableFactory==null){
				kappa = new Scalar((double)dataModified.get("kappa"));
			}else {
				kappa = randomVariableFactory.createRandomVariable((double)dataModified.get("kappa"));
			}
			if(dataModified.getOrDefault("theta", theta) instanceof RandomVariable) {
				theta = (RandomVariable)dataModified.getOrDefault("rho", theta);
			}else if(randomVariableFactory==null){
				theta = new Scalar((double)dataModified.get("theta"));
			}else {
				theta = randomVariableFactory.createRandomVariable((double)dataModified.get("theta"));
			}
			if(dataModified.getOrDefault("xi", xi) instanceof RandomVariable) {
				xi = (RandomVariable)dataModified.getOrDefault("xi", xi);
			}else if(randomVariableFactory==null){
				xi = new Scalar((double)dataModified.get("xi"));
			}else {
				xi = randomVariableFactory.createRandomVariable((double)dataModified.get("xi"));
			}
		}

		AbstractLIBORCovarianceModelParametric newModel = new LIBORCovarianceModelStochasticHestonVolatility(covarianceModel, brownianMotion, kappa, theta, xi, isCalibrateable);
		return newModel;
	}
}
