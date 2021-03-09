/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.05.2013
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Displaced model build on top of a standard covariance model.
 *
 * The model constructed for the <i>i</i>-th factor loading is
 * <i>(L<sub>i</sub>(t) + d) F<sub>i</sub>(t)</i>
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
public class DisplacedLocalVolatilityModel extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = 4522227972747028512L;
	private final AbstractLIBORCovarianceModelParametric covarianceModel;
	private final RandomVariable displacement;

	private ForwardCurve forwardCurve;

	private boolean isCalibrateable = false;

	/**
	 * Displaced model build on top of a standard covariance model.
	 *
	 * The model constructed for the <i>i</i>-th factor loading is
	 * <i>(L<sub>i</sub>(t) + d) F<sub>i</sub>(t)</i>
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
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param displacement The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public DisplacedLocalVolatilityModel(final AbstractLIBORCovarianceModelParametric covarianceModel, final RandomVariable displacement, final boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());
		this.covarianceModel	= covarianceModel;
		this.displacement		= displacement;
		this.isCalibrateable	= isCalibrateable;
	}

	/**
	 * Displaced model build on top of a standard covariance model.
	 *
	 * The model constructed for the <i>i</i>-th factor loading is
	 * <i>(L<sub>i</sub>(t) + d) F<sub>i</sub>(t)</i>
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
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param displacement The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public DisplacedLocalVolatilityModel(final AbstractLIBORCovarianceModelParametric covarianceModel, final double displacement, final boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());
		this.covarianceModel	= covarianceModel;
		this.displacement		= new Scalar(displacement);
		this.isCalibrateable	= isCalibrateable;
	}

	@Override
	public Object clone() {
		return new DisplacedLocalVolatilityModel((AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), displacement, isCalibrateable);
	}

	/**
	 * Returns the base covariance model, i.e., the model providing the factor loading <i>F</i>
	 * such that this model's <i>i</i>-th factor loading is
	 * <i>(a L<sub>i,0</sub> + (1-a)L<sub>i</sub>(t)) F<sub>i</sub>(t)</i>
	 * where <i>a</i> is the displacement and <i>L<sub>i</sub></i> is
	 * the realization of the <i>i</i>-th component of the stochastic process and
	 * <i>F<sub>i</sub></i> is the factor loading loading from the given covariance model.
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

		final RandomVariable[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) {
			return new RandomVariable[] { displacement };
		}

		// Append displacement to the end of covarianceParameters
		final RandomVariable[] jointParameters = new RandomVariable[covarianceParameters.length+1];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length] = displacement;

		return jointParameters;
	}

	@Override
	public double[] getParameterAsDouble() {
		final RandomVariable[] parameters = getParameter();
		final double[] parametersAsDouble = new double[parameters.length];
		for(int i=0; i<parameters.length; i++) {
			parametersAsDouble[i] = parameters[i].doubleValue();
		}
		return parametersAsDouble;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(final RandomVariable[] parameters) {
		if(parameters == null || parameters.length == 0) {
			return this;
		}

		if(!isCalibrateable) {
			return new DisplacedLocalVolatilityModel(covarianceModel.getCloneWithModifiedParameters(parameters), displacement, isCalibrateable);
		}

		final RandomVariable[] covarianceParameters = new RandomVariable[parameters.length-1];
		System.arraycopy(parameters, 0, covarianceParameters, 0, covarianceParameters.length);

		final AbstractLIBORCovarianceModelParametric newCovarianceModel = covarianceModel.getCloneWithModifiedParameters(covarianceParameters);
		final RandomVariable newDisplacement = parameters[covarianceParameters.length];

		return new DisplacedLocalVolatilityModel(newCovarianceModel, newDisplacement, isCalibrateable);
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(final double[] parameters) {
		return getCloneWithModifiedParameters(Scalar.arrayOf(parameters));
	}

	@Override
	public RandomVariable[] getFactorLoading(final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		final RandomVariable[] factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);

		if(realizationAtTimeIndex != null && realizationAtTimeIndex[component] != null) {
			final RandomVariable localVolatilityFactor = realizationAtTimeIndex[component].add(displacement);
			for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
				factorLoading[factorIndex] = factorLoading[factorIndex].mult(localVolatilityFactor);
			}
		}

		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(final int timeIndex, final int component, final int factor, final RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}

	public RandomVariable getDisplacement() {
		return displacement;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(final Map<String, Object> dataModified)
			throws CalculationException {
		RandomVariable displacement = this.displacement;
		boolean isCalibrateable = this.isCalibrateable;
		AbstractLIBORCovarianceModelParametric covarianceModel = this.covarianceModel;
		RandomVariableFactory randomVariableFactory = null;

		if(dataModified != null) {
			if(dataModified.containsKey("randomVariableFactory")) {
				randomVariableFactory = (RandomVariableFactory)dataModified.get("randomVariableFactory");
				displacement = randomVariableFactory.createRandomVariable(displacement.doubleValue());
			}
			if (!dataModified.containsKey("covarianceModel")) {
				covarianceModel = covarianceModel.getCloneWithModifiedData(dataModified);
			}

			// Explicitly passed covarianceModel has priority
			covarianceModel = (AbstractLIBORCovarianceModelParametric)dataModified.getOrDefault("covarianceModel", covarianceModel);
			isCalibrateable = (boolean)dataModified.getOrDefault("isCalibrateable", isCalibrateable);

			if (dataModified.getOrDefault("displacement", displacement) instanceof RandomVariable) {
				displacement = (RandomVariable) dataModified.getOrDefault("displacement", displacement);
			} else if (randomVariableFactory == null) {
				displacement = new Scalar((double) dataModified.get("displacement"));
			} else {
				displacement = randomVariableFactory.createRandomVariable((double) dataModified.get("displacement"));
			}
		}

		final AbstractLIBORCovarianceModelParametric newModel = new DisplacedLocalVolatilityModel(covarianceModel, displacement, isCalibrateable);
		return newModel;
	}
}
