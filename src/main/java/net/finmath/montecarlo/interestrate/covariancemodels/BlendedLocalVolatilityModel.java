/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.05.2013
 */
package net.finmath.montecarlo.interestrate.covariancemodels;

import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Blended model (or displaced diffusion model) build on top of a standard covariance model.
 *
 * The model constructed for the <i>i</i>-th factor loading is
 * \[
 * 	( a + (1-a) L_{i}(t) ) F_{i}(t) \text{,}
 * \]
 * or
 * \[
 * 	( a L_{i,0} + (1-a) L_{i}(t) ) F_{i}(t) \text{,}
 * \]
 * if an initial forward curve \( i \mapsto L_{i,0} \) is given,
 * where <i>a</i> is the displacement or blending parameter and <i>L<sub>i</sub></i> is
 * the realization of the <i>i</i>-th component of the stochastic process and
 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
 *
 * If a forward curve is provided, the deterministic value L<sub>i,0</sub> is
 * calculated form this curve (using fixing in <i>T<sub>i</sub></i>),
 * otherwise it is replaced by 1.
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
public class BlendedLocalVolatilityModel extends AbstractLIBORCovarianceModelParametric {

	private AbstractRandomVariableFactory randomVariableFactory;
	private AbstractLIBORCovarianceModelParametric covarianceModel;
	private RandomVariableInterface displacement;

	private ForwardCurveInterface forwardCurve;

	private boolean isCalibrateable = false;

	/**
	 * Displaced diffusion model build on top of a standard covariance model.
	 * The model constructed is <i>(a L<sub>0</sub> + (1-a)L) F</i> where <i>a</i> is
	 * the displacement and <i>L</i> is
	 * the component of the stochastic process and <i>F</i> is the factor loading
	 * from the given covariance model.
	 *
	 * The parameter of this model is a joint parameter vector, where the first
	 * entry is the displacement and the remaining entries are the parameter vector
	 * of the given base covariance model.
	 *
	 * If this model is not calibrateable, its parameter vector is that of the
	 * covariance model.
	 *
	 * @param randomVariableFactory The factory used to create RandomVariableInterface objects from constants.
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param forwardCurve The given forward curve L<sub>0</sub>
	 * @param displacement The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public BlendedLocalVolatilityModel(AbstractRandomVariableFactory randomVariableFactory, AbstractLIBORCovarianceModelParametric covarianceModel, ForwardCurveInterface forwardCurve, double displacement, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());

		this.randomVariableFactory = randomVariableFactory;
		this.covarianceModel	= covarianceModel;
		this.forwardCurve		= forwardCurve;
		this.displacement		= randomVariableFactory.createRandomVariable(displacement);
		this.isCalibrateable	= isCalibrateable;
	}

	/**
	 * Displaced diffusion model build on top of a standard covariance model.
	 *
	 * The model performs a linear interpolation of a log-normal model (a = 0) and a normal model (a = 1).
	 *
	 * The model constructed is <i>(a + (1-a)L) F</i> where <i>a</i> is
	 * the displacement and <i>L</i> is
	 * the component of the stochastic process and <i>F</i> is the factor loading
	 * loading from the given covariance model.
	 *
	 * The parameter of this model is a joint parameter vector, where the first
	 * entry is the displacement and the remaining entries are the parameter vector
	 * of the given base covariance model.
	 *
	 * If this model is not calibrateable, its parameter vector is that of the
	 * covariance model.
	 *
	 * @param randomVariableFactory The factory used to create RandomVariableInterface objects from constants.
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param displacement The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public BlendedLocalVolatilityModel(AbstractRandomVariableFactory randomVariableFactory, AbstractLIBORCovarianceModelParametric covarianceModel, double displacement, boolean isCalibrateable) {
		this(randomVariableFactory, covarianceModel, null, displacement, isCalibrateable);
	}

	/**
	 * Displaced diffusion model build on top of a standard covariance model.
	 *
	 * The model performs a linear interpolation of a log-normal model (a = 0) and a normal model (a = 1).
	 *
	 * The model constructed is <i>(a + (1-a)L) F</i> where <i>a</i> is
	 * the displacement and <i>L</i> is
	 * the component of the stochastic process and <i>F</i> is the factor loading
	 * loading from the given covariance model.
	 *
	 * The parameter of this model is a joint parameter vector, where the first
	 * entry is the displacement and the remaining entries are the parameter vector
	 * of the given base covariance model.
	 *
	 * If this model is not calibrateable, its parameter vector is that of the
	 * covariance model.
	 *
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param displacement The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public BlendedLocalVolatilityModel(AbstractLIBORCovarianceModelParametric covarianceModel, double displacement, boolean isCalibrateable) {
		this(new RandomVariableFactory(), covarianceModel, displacement, isCalibrateable);
	}

	@Override
	public Object clone() {
		return new BlendedLocalVolatilityModel(randomVariableFactory, (AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), forwardCurve, displacement.doubleValue(), isCalibrateable);
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
	public RandomVariableInterface[] getParameter() {
		if(!isCalibrateable) {
			return covarianceModel.getParameter();
		}

		RandomVariableInterface[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) {
			return new RandomVariableInterface[] { displacement };
		}

		// Append displacement to the end of covarianceParameters
		RandomVariableInterface[] jointParameters = new RandomVariableInterface[covarianceParameters.length+1];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length] = displacement;

		return jointParameters;
	}

	private void setParameter(RandomVariableInterface[] parameter) {
		if(parameter == null || parameter.length == 0) {
			return;
		}

		if(!isCalibrateable) {
			covarianceModel = covarianceModel.getCloneWithModifiedParameters(parameter);
			return;
		}

		RandomVariableInterface[] covarianceParameters = new RandomVariableInterface[parameter.length-1];
		System.arraycopy(parameter, 0, covarianceParameters, 0, covarianceParameters.length);

		covarianceModel = covarianceModel.getCloneWithModifiedParameters(covarianceParameters);
		displacement = parameter[covarianceParameters.length];
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(RandomVariableInterface[] parameters) {
		BlendedLocalVolatilityModel model = (BlendedLocalVolatilityModel)this.clone();
		model.setParameter(parameters);
		return model;
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
		RandomVariableInterface[] factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);

		double forward = 1.0;
		if(forwardCurve != null) {
			double timeToMaturity = getLiborPeriodDiscretization().getTime(component) - getTimeDiscretization().getTime(timeIndex);
			// @TODO: Consider using a model context here
			forward = forwardCurve.getForward(null, Math.max(timeToMaturity, 0.0));
		}

		if(realizationAtTimeIndex != null && realizationAtTimeIndex[component] != null) {
			RandomVariableInterface localVolatilityFactor = realizationAtTimeIndex[component].sub(realizationAtTimeIndex[component].mult(displacement)).add(displacement.mult(forward));
			for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
				factorLoading[factorIndex] = factorLoading[factorIndex].mult(localVolatilityFactor);
			}
		}

		return factorLoading;
	}

	@Override
	public RandomVariableInterface getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariableInterface[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}
}
