/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.05.2013
 */
package net.finmath.montecarlo.interestrate.models.modelplugins;

import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.stochastic.RandomVariable;

/**
 * Displaced model build on top of a standard covariance model.
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
public class DisplacedLocalVolatilityModel extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = 8032949024525942210L;

	private AbstractLIBORCovarianceModelParametric covarianceModel;
	private double displacement;

	private ForwardCurveInterface forwardCurve;

	private boolean isCalibrateable = false;

	/**
	 * Displaced model build on top of a standard covariance model.
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
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param displacement The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public DisplacedLocalVolatilityModel(AbstractLIBORCovarianceModelParametric covarianceModel, double displacement, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());
		this.covarianceModel	= covarianceModel;
		this.displacement		= displacement;
		this.isCalibrateable	= isCalibrateable;
	}

	@Override
	public Object clone() {
		return new DisplacedLocalVolatilityModel((AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), displacement, isCalibrateable);
	}

	/**
	 * Returns the base covariance model, i.e., the model providing the factor loading <i>F</i>
	 * such that this model's <i>i</i>-th factor loading is
	 * <center>
	 * <i>(a L<sub>i,0</sub> + (1-a)L<sub>i</sub>(t)) F<sub>i</sub>(t)</i>
	 * </center>
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
	public double[] getParameter() {
		if(!isCalibrateable) {
			return covarianceModel.getParameter();
		}

		double[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) {
			return new double[] { displacement };
		}

		// Append displacement to the end of covarianceParameters
		double[] jointParameters = new double[covarianceParameters.length+1];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length] = displacement;

		return jointParameters;
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		DisplacedLocalVolatilityModel model = (DisplacedLocalVolatilityModel)this.clone();
		if(parameters == null || parameters.length == 0) {
			return model;
		}

		if(!isCalibrateable) {
			model.covarianceModel = covarianceModel.getCloneWithModifiedParameters(parameters);
			return model;
		}

		double[] covarianceParameters = new double[parameters.length-1];
		System.arraycopy(parameters, 0, covarianceParameters, 0, covarianceParameters.length);

		model.covarianceModel = covarianceModel.getCloneWithModifiedParameters(covarianceParameters);
		model.displacement = parameters[covarianceParameters.length];
		return model;
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		RandomVariable[] factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);

		if(realizationAtTimeIndex != null && realizationAtTimeIndex[component] != null) {
			RandomVariable localVolatilityFactor = realizationAtTimeIndex[component].add(displacement);
			for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
				factorLoading[factorIndex] = factorLoading[factorIndex].mult(localVolatilityFactor);
			}
		}

		return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariable[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}
}
