/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.05.2013
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Exponential decay model build on top of a given covariance model.
 *
 * The model constructed for the <i>i</i>-th factor loading is
 * <center>
 * <i>(L<sub>i</sub>(t) + d) F<sub>i</sub>(t)</i>
 * </center>
 * where <i>d</i> is the displacement and <i>L<sub>i</sub></i> is
 * the realization of the <i>i</i>-th component of the stochastic process and
 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
 *
 * The parameter of this model is a joint parameter vector, consisting
 * of the parameter vector of the given base covariance model and
 * appending the displacement parameter at the end.
 *
 * If this model is not calibrateable, its parameter vector is that of the
 * covariance model, i.e., only the displacement parameter will be not
 * part of the calibration.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class ExponentialDecayLocalVolatilityModel extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = 4522227972747028512L;
	private final RandomVariableFactory abstractRandomVariableFactory;
	private final AbstractLIBORCovarianceModelParametric covarianceModel;
	private final RandomVariable decay;

	private boolean isCalibrateable = false;

	/**
	 * Exponential decay model build on top of a standard covariance model.
	 *
	 * The model constructed for the <i>i</i>-th factor loading is
	 * <center>
	 * <i>exp(- a t) F<sub>i</sub>(t)</i>
	 * </center>
	 * where <i>a</i> is the decay parameter and
	 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
	 *
	 * The parameter of this model is a joint parameter vector, consisting
	 * of the parameter vector of the given base covariance model and
	 * appending the decay parameter at the end.
	 *
	 * If this model is not calibrateable, its parameter vector is that of the
	 * underlying covariance model, i.e., only the decay parameter will be not
	 * part of the calibration.
	 *
	 * @param abstractRandomVariableFactory A random variable factory (used when cloning with modifed parameters).
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param decay The decay <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public ExponentialDecayLocalVolatilityModel(RandomVariableFactory abstractRandomVariableFactory, AbstractLIBORCovarianceModelParametric covarianceModel, RandomVariable decay, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());
		this.abstractRandomVariableFactory = abstractRandomVariableFactory;
		this.covarianceModel	= covarianceModel;
		this.decay		= decay;
		this.isCalibrateable	= isCalibrateable;
	}

	/**
	 * Exponential decay model build on top of a standard covariance model.
	 *
	 * The model constructed for the <i>i</i>-th factor loading is
	 * <center>
	 * <i>exp(- a t) F<sub>i</sub>(t)</i>
	 * </center>
	 * where <i>a</i> is the decay parameter and
	 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
	 *
	 * The parameter of this model is a joint parameter vector, consisting
	 * of the parameter vector of the given base covariance model and
	 * appending the decay parameter at the end.
	 *
	 * If this model is not calibrateable, its parameter vector is that of the
	 * underlying covariance model, i.e., only the decay parameter will be not
	 * part of the calibration.
	 *
	 * @param abstractRandomVariableFactory A random variable factory (used for the given parameter and when cloning with modifed parameters).
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param decay The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public ExponentialDecayLocalVolatilityModel(RandomVariableFactory abstractRandomVariableFactory, AbstractLIBORCovarianceModelParametric covarianceModel, double decay, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());
		this.abstractRandomVariableFactory = abstractRandomVariableFactory;
		this.covarianceModel = covarianceModel;
		this.decay = abstractRandomVariableFactory != null ? abstractRandomVariableFactory.createRandomVariable(decay) : new Scalar(decay);
		this.isCalibrateable	= isCalibrateable;
	}

	/**
	 * Exponential decay model build on top of a standard covariance model.
	 *
	 * The model constructed for the <i>i</i>-th factor loading is
	 * <center>
	 * <i>exp(- a t) F<sub>i</sub>(t)</i>
	 * </center>
	 * where <i>a</i> is the decay parameter and
	 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
	 *
	 * The parameter of this model is a joint parameter vector, consisting
	 * of the parameter vector of the given base covariance model and
	 * appending the decay parameter at the end.
	 *
	 * If this model is not calibrateable, its parameter vector is that of the
	 * underlying covariance model, i.e., only the decay parameter will be not
	 * part of the calibration.
	 *
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param decay The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public ExponentialDecayLocalVolatilityModel(AbstractLIBORCovarianceModelParametric covarianceModel, double decay, boolean isCalibrateable) {
		this(null, covarianceModel, decay, isCalibrateable);
	}

	@Override
	public Object clone() {
		return new ExponentialDecayLocalVolatilityModel(abstractRandomVariableFactory, (AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), decay, isCalibrateable);
	}

	/**
	 * Returns the base covariance model, i.e., the model providing the factor loading <i>F</i>
	 * such that this model's <i>i</i>-th factor loading is
	 * <center>
	 * <i>exp(- a t) F<sub>i</sub>(t)</i>
	 * </center>
	 * where <i>a</i> is the decay parameter and
	 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
	 *
	 * @return The base covariance model.
	 */
	public AbstractLIBORCovarianceModelParametric getBaseCovarianceModel() {
		return covarianceModel;
	}

	@Override
	public RandomVariable[] getParameter() {
		if(!isCalibrateable) {
			return covarianceModel.getParameter();
		}

		RandomVariable[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) {
			return new RandomVariable[] { decay };
		}

		// Append decay to the end of covarianceParameters
		RandomVariable[] jointParameters = new RandomVariable[covarianceParameters.length+1];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length] = decay;

		return jointParameters;
	}

	@Override
	public double[] getParameterAsDouble() {
		RandomVariable[] parameters = getParameter();
		double[] parametersAsDouble = new double[parameters.length];
		for(int i=0; i<parameters.length; i++) {
			parametersAsDouble[i] = parameters[i].doubleValue();
		}
		return parametersAsDouble;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariable[] parameters) {
		if(parameters == null || parameters.length == 0) {
			return this;
		}

		if(!isCalibrateable) {
			return new ExponentialDecayLocalVolatilityModel(abstractRandomVariableFactory, covarianceModel.getCloneWithModifiedParameters(parameters), decay, isCalibrateable);
		}

		RandomVariable[] covarianceParameters = new RandomVariable[parameters.length-1];
		System.arraycopy(parameters, 0, covarianceParameters, 0, covarianceParameters.length);

		AbstractLIBORCovarianceModelParametric newCovarianceModel = covarianceModel.getCloneWithModifiedParameters(covarianceParameters);
		RandomVariable newDisplacement = parameters[covarianceParameters.length];

		return new ExponentialDecayLocalVolatilityModel(abstractRandomVariableFactory, newCovarianceModel, newDisplacement, isCalibrateable);
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		return getCloneWithModifiedParameters(Scalar.arrayOf(parameters));
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		RandomVariable[] factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);

		double time = getTimeDiscretization().getTime(timeIndex);
		double fixing = getLiborPeriodDiscretization().getTime(component);
		double timeToMaturity = fixing-time;
		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
			factorLoading[factorIndex] = factorLoading[factorIndex].mult(decay.mult(-timeToMaturity).exp());
		}

		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}

	public RandomVariable getDisplacement() {
		return decay;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(Map<String, Object> dataModified)
			throws CalculationException {
		RandomVariable newDecay = decay;
		boolean isCalibrateable = this.isCalibrateable;
		AbstractLIBORCovarianceModelParametric covarianceModel = this.covarianceModel;

		RandomVariableFactory newRandomVariableFactory = abstractRandomVariableFactory;

		if(dataModified != null) {
			if(dataModified.containsKey("randomVariableFactory")) {
				newRandomVariableFactory = (RandomVariableFactory)dataModified.get("randomVariableFactory");
				newDecay = newRandomVariableFactory.createRandomVariable(newDecay.doubleValue());
			}
			if (!dataModified.containsKey("covarianceModel")) {
				covarianceModel = covarianceModel.getCloneWithModifiedData(dataModified);
			}

			// Explicitly passed covarianceModel has priority
			covarianceModel = (AbstractLIBORCovarianceModelParametric)dataModified.getOrDefault("covarianceModel", covarianceModel);
			isCalibrateable = (boolean)dataModified.getOrDefault("isCalibrateable", isCalibrateable);

			if (dataModified.getOrDefault("decay", newDecay) instanceof RandomVariable) {
				newDecay = (RandomVariable) dataModified.getOrDefault("decay", newDecay);
			} else if (newRandomVariableFactory == null) {
				newDecay = new Scalar((double) dataModified.get("decay"));
			} else {
				newDecay = newRandomVariableFactory.createRandomVariable((double) dataModified.get("decay"));
			}
		}

		AbstractLIBORCovarianceModelParametric newModel = new ExponentialDecayLocalVolatilityModel(newRandomVariableFactory, covarianceModel, newDecay, isCalibrateable);
		return newModel;
	}
}
