/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.05.2013
 */
package net.finmath.montecarlo.interestrate.models.covariance;

import java.util.Map;

import net.finmath.stochastic.RandomVariable;

/**
 * Special variant of a blended model (or displaced diffusion model)
 * build on top of a standard covariance model
 * using the special function corresponding to the Hull-White local volatility.
 *
 * The model constructed for the <i>i</i>-th factor loading is
 * <center>
 * <i>(1+L<sub>i</sub>(t) d) F<sub>i</sub>(t)</i>
 * </center>
 * where <i>d</i> is a constant (the period length), <i>L<sub>i</sub></i> is
 * the realization of the <i>i</i>-th component of the stochastic process and
 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
 *
 * If this model is combined with an exponential decay volatility model
 * <code>LIBORVolatilityModelTwoParameterExponentialForm</code>, then
 * the resulting LIBOR Market model corresponds to a Hull-White short rate model
 * (with constant short rate volatility and mean reversion).
 *
 * The parameter of this model is the parameter vector of the given base covariance model.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class HullWhiteLocalVolatilityModel extends AbstractLIBORCovarianceModelParametric {

	private static final long serialVersionUID = -4182083344704425769L;

	private final AbstractLIBORCovarianceModelParametric covarianceModel;
	private final double periodLength;

	/**
	 * The model constructed for the <i>i</i>-th factor loading is
	 * <center>
	 * <i>(1+L<sub>i</sub>(t) d) F<sub>i</sub>(t)</i>
	 * </center>
	 * where <i>d</i> is a constant (the period length), <i>L<sub>i</sub></i> is
	 * the realization of the <i>i</i>-th component of the stochastic process and
	 * <i>F<sub>i</sub></i> is the factor loading from the given covariance model.
	 *
	 * The parameter of this model is the parameter vector of the given base covariance model.
	 *
	 * @param covarianceModel The given covariance model specifying the factor loadings <i>F</i>.
	 * @param periodLength The parameter <i>d</i> in the formula above.
	 */
	public HullWhiteLocalVolatilityModel(AbstractLIBORCovarianceModelParametric covarianceModel, double periodLength) {
		super(covarianceModel.getTimeDiscretization(), covarianceModel.getLiborPeriodDiscretization(), covarianceModel.getNumberOfFactors());
		this.covarianceModel	= covarianceModel;
		this.periodLength		= periodLength;
	}

	@Override
	public Object clone() {
		return new HullWhiteLocalVolatilityModel((AbstractLIBORCovarianceModelParametric) covarianceModel.clone(), periodLength);
	}

	/**
	 * Returns the base covariance model, i.e., the model providing the factor loading <i>F</i>.
	 *
	 * @return The base covariance model.
	 */
	public AbstractLIBORCovarianceModelParametric getBaseCovarianceModel() {
		return covarianceModel;
	}

	@Override
	public double[] getParameterAsDouble() {
		return covarianceModel.getParameterAsDouble();
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedParameters(double[] parameters) {
		return new HullWhiteLocalVolatilityModel(covarianceModel.getCloneWithModifiedParameters(parameters), periodLength);
	}

	@Override
	public AbstractLIBORCovarianceModelParametric getCloneWithModifiedData(Map<String, Object> dataModified) {

		throw new UnsupportedOperationException("Method not implemented");
	}

	@Override
	public RandomVariable[] getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		RandomVariable[] factorLoading = covarianceModel.getFactorLoading(timeIndex, component, realizationAtTimeIndex);

		if(realizationAtTimeIndex != null && realizationAtTimeIndex[component] != null) {
			RandomVariable localVolatilityFactor = realizationAtTimeIndex[component].mult(periodLength).add(1.0);
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
