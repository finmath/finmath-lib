/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 26.05.2013
 */
package net.finmath.montecarlo.interestrate.modelplugins;

import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Displaced diffusion model build on top of a standard covariance model.
 * The model constructed is <i>(a + (1-a)L) F</i> where <i>a</i> is
 * the displacement and <i>L</i> is
 * the component of the stochastic process and <i>F</i> is the factor loading
 * loading from the given covariance model.
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
 */
public class DisplacedDiffusionModel extends AbstractLIBORCovarianceModelParametric {

	private AbstractLIBORCovarianceModelParametric covarianceModel;
	private double displacement;

	private ForwardCurveInterface forwardCurve;
	
    private boolean isCalibrateable = false;

	/**
	 * Displaced diffusion model build on top of a standard covariance model.
	 * The model constructed is <i>(a L<sub>0</sub> + (1-a)L) F</i> where <i>a</i> is
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
	 * @param forwardCurve The given forward curve L<sub>0</sub>
	 * @param displacement The displacement <i>a</i>.
	 * @param isCalibrateable If true, the parameter <i>a</i> is a free parameter. Note that the covariance model may have its own parameter calibration settings.
	 */
	public DisplacedDiffusionModel(AbstractLIBORCovarianceModelParametric covarianceModel, ForwardCurveInterface forwardCurve, double displacement, boolean isCalibrateable) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());
		this.covarianceModel	= covarianceModel;
		this.forwardCurve		= forwardCurve;
		this.displacement		= displacement;
		this.isCalibrateable	= isCalibrateable;
	}

	/**
	 * Displaced diffusion model build on top of a standard covariance model.
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
	public DisplacedDiffusionModel(AbstractLIBORCovarianceModelParametric covarianceModel, double displacement, boolean isCalibrateable) {
		this(covarianceModel, null, displacement, isCalibrateable);
	}

	@Override
	public Object clone() {
		DisplacedDiffusionModel model = new DisplacedDiffusionModel((AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), forwardCurve, displacement, isCalibrateable);

		return model;
	}
	
	@Override
	public double[] getParameter() {
		if(!isCalibrateable) return covarianceModel.getParameter();

		double[] covarianceParameters = covarianceModel.getParameter();
		if(covarianceParameters == null) return new double[] { displacement };
		
		// Append displacement to the end of covarianceParameters
		double[] jointParameters = new double[covarianceParameters.length+1];
		System.arraycopy(covarianceParameters, 0, jointParameters, 0, covarianceParameters.length);
		jointParameters[covarianceParameters.length] = displacement;

		return jointParameters;
	}

	@Override
	public void setParameter(double[] parameter) {
		if(parameter == null || parameter.length == 0) return;

		if(!isCalibrateable) {
			covarianceModel.setParameter(parameter);
			return;
		}

		double[] covarianceParameters = new double[parameter.length-1];
		System.arraycopy(parameter, 0, covarianceParameters, 0, covarianceParameters.length);

		covarianceModel.setParameter(covarianceParameters);
		displacement = parameter[covarianceParameters.length];
	}

	@Override
	public RandomVariableInterface[] getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
        RandomVariableInterface[] factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);

        double forward = 1.0;
        if(forwardCurve != null) {
        	double timeToMaturity = getLiborPeriodDiscretization().getTime(component) - getTimeDiscretization().getTime(timeIndex);
        	forward = forwardCurve.getValue(null, Math.max(timeToMaturity, 0.0));
        }
        
        if(realizationAtTimeIndex != null && realizationAtTimeIndex[component] != null) {
        	RandomVariableInterface localVolatilityFactor = realizationAtTimeIndex[component].mult(1.0-displacement).add(displacement * forward);
    		for (int factorIndex = 0; factorIndex < factorLoading.length; factorIndex++) {
    			factorLoading[factorIndex] = factorLoading[factorIndex].mult(localVolatilityFactor);
    		}
        }

        return factorLoading;
	}

	@Override
	public RandomVariable getFactorLoadingPseudoInverse(int timeIndex, int component, int factor, RandomVariableInterface[] realizationAtTimeIndex) {
		throw new UnsupportedOperationException();
	}
}
