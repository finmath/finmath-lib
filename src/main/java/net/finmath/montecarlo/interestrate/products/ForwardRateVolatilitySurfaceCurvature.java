/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.03.2014
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.LIBORMarketModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements the calculation of the curvature of the volatility surface of the forward rates.
 *
 * <br>
 *
 * The value returned by the <code>{@link #getValue(double, TermStructureMonteCarloSimulationModel)}</code> method is calculated as follows:
 * For each forward rate's instantaneous volatility <i>&sigma;(t)</i> we calculate
 * \[	\sqrt{ \frac{1}{t_{n} - t_{1}} \sum_{i=1}^{n-1} ( f(t_{i}) )^{2} \cdot (t_{i+1} - t_{i}) } \]
 * (this is the root mean square / L2 norm of <i>f</i>) where
 * \[	f(t_{i}) = \frac{ x_{i+2} - 2 x_{i+1} + x_{i} }{ (t_{i+2} - t_{i+1}) (t_{i+1} - t_{i}) } \]
 * and where
 * <i>
 * 	x<sub>i</sub> = &sigma;<sup>2</sup>(t<sub>i</sub>)
 * </i> is the
 * instantaneous variance of a specific forward rate.
 *
 * The value returned is then calculated as the average of all those curvatures for all forward rates.
 *
 * Note: A tolerance level can be specified. See the documentation of the constructor <code>{@link #ForwardRateVolatilitySurfaceCurvature(double)}</code>.
 *
 * <br>
 * <br>
 *
 * While this is not a common financial product, this class can be helpful in calibration procedures, e.g.
 * to put an additional constrain on the smoothness / curvature of the model surface.
 *
 * <br>
 * A rough interpretation of this product is a follows: If the value 0.25 is returned, then
 * - on average - the variance of the forward rate oscillates by 0.25 per year, i.e. it moves up by 0.25 and back.
 * This corresponds to a movement of 0.5 and back per year for the volatility.
 * Given that you have 100 other calibrations products, and the weight of all (including this one) is 1.0, then
 * 0.1% deviation of the other calibration products is considered as severe as 0.1 movement per year in the variance (0.3 movement in volatility).
 * Note: You can control the relation of these by weights.
 *
 * @author Christian Fries
 * @date 12.04.2014.
 * @version 1.0
 */
public class ForwardRateVolatilitySurfaceCurvature extends AbstractTermStructureMonteCarloProduct {

	private double tolerance = 0.0;

	/**
	 * Create the calculation of the curvature of the volatility surface of the forward rates
	 */
	public ForwardRateVolatilitySurfaceCurvature() {
		super();
	}

	/**
	 * Create the calculation of the curvature of the volatility surface of the forward rates.
	 *
	 * A tolerance level may be specified. In that case, the curvature
	 * calculated by the getValue method is approximately
	 * <br>
	 * <i>max(<code>curvature</code> - <code>tolerance</code>, 0)</i>.
	 * <br>
	 *
	 * A rough interpretation of the tolerance is as follows:
	 * With a tolerance = 0.04, then
	 * <ul>
	 * 	<li>
	 * 		the variance can oscillate once from 0.0 to 0.04 and back within a year
	 * 		without generating a penalty term
	 * 		(i.e., the volatility is allowed to oscillate once from 0.0 to 0.2 and back within a year), or
	 * 	</li>
	 * 	<li>
	 * 		the variance can oscillate twice from 0.0 to 0.02 and back within a year
	 * 		without generating a penalty term
	 * 		(i.e., the volatility is allowed to oscillate twice from 0.0 to 0.14 (sqrt(0.02)) and back).
	 * 	</li>
	 * </ul>
	 *
	 * @param tolerance The tolerance level.
	 */
	public ForwardRateVolatilitySurfaceCurvature(final double tolerance) {
		super();

		this.tolerance = tolerance;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) {
		final ProcessModel modelBase = model.getModel();
		if(modelBase instanceof LIBORMarketModel) {
			return getValues(evaluationTime, (LIBORMarketModel)modelBase);
		} else {
			throw new IllegalArgumentException("This product requires a simulation where the underlying model is of type LIBORMarketModel.");
		}
	}

	/**
	 * Calculates the squared curvature of the LIBOR instantaneous variance.
	 *
	 * @param evaluationTime Time at which the product is evaluated.
	 * @param model A model implementing the LIBORModelMonteCarloSimulationModel
	 * @return The squared curvature of the LIBOR instantaneous variance (reduced a possible tolerance). The return value is &ge; 0.
	 */
	public RandomVariable getValues(final double evaluationTime, final LIBORMarketModel model) {
		if(evaluationTime > 0) {
			throw new RuntimeException("Forward start evaluation currently not supported.");
		}

		// Fetch the covariance model of the model
		final LIBORCovarianceModel covarianceModel = model.getCovarianceModel();

		// We sum over all forward rates
		final int numberOfComponents = covarianceModel.getLiborPeriodDiscretization().getNumberOfTimeSteps();

		// Accumulator
		RandomVariable	integratedLIBORCurvature	= new RandomVariableFromDoubleArray(0.0);
		for(int componentIndex = 0; componentIndex < numberOfComponents; componentIndex++) {

			// Integrate from 0 up to the fixing of the rate
			final double timeEnd		= covarianceModel.getLiborPeriodDiscretization().getTime(componentIndex);
			int timeEndIndex	= covarianceModel.getTimeDiscretization().getTimeIndex(timeEnd);

			// If timeEnd is not in the time discretization we get timeEndIndex = -insertionPoint-1. In that case, we use the index prior to the insertionPoint
			if(timeEndIndex < 0) {
				timeEndIndex = -timeEndIndex - 2;
			}

			// Sum squared second derivative of the variance for all components at this time step
			RandomVariable integratedLIBORCurvatureCurrentRate = new RandomVariableFromDoubleArray(0.0);
			for(int timeIndex = 0; timeIndex < timeEndIndex-2; timeIndex++) {
				final double timeStep1	= covarianceModel.getTimeDiscretization().getTimeStep(timeIndex);
				final double timeStep2	= covarianceModel.getTimeDiscretization().getTimeStep(timeIndex+1);

				final RandomVariable covarianceLeft		= covarianceModel.getCovariance(timeIndex+0, componentIndex, componentIndex, null);
				final RandomVariable covarianceCenter	= covarianceModel.getCovariance(timeIndex+1, componentIndex, componentIndex, null);
				final RandomVariable covarianceRight		= covarianceModel.getCovariance(timeIndex+2, componentIndex, componentIndex, null);

				// Calculate second derivative
				RandomVariable curvatureSquared = covarianceRight.sub(covarianceCenter.mult(2.0)).add(covarianceLeft);
				curvatureSquared = curvatureSquared.div(timeStep1 * timeStep2);

				// Take square
				curvatureSquared = curvatureSquared.squared();

				// Integrate over time
				integratedLIBORCurvatureCurrentRate = integratedLIBORCurvatureCurrentRate.add(curvatureSquared.mult(timeStep1));
			}

			// Empty interval - skip
			if(timeEnd == 0) {
				continue;
			}

			// Average over time
			integratedLIBORCurvatureCurrentRate = integratedLIBORCurvatureCurrentRate.div(timeEnd);

			// Take square root
			integratedLIBORCurvatureCurrentRate = integratedLIBORCurvatureCurrentRate.sqrt();

			// Take max over all forward rates
			integratedLIBORCurvature = integratedLIBORCurvature.add(integratedLIBORCurvatureCurrentRate);
		}

		integratedLIBORCurvature = integratedLIBORCurvature.div(numberOfComponents);
		return integratedLIBORCurvature.sub(tolerance).floor(0.0);
	}
}
