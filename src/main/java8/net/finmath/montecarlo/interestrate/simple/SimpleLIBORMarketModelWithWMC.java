/*
 * Created on 09.02.2004
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.interestrate.simple;

import java.util.Arrays;

import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCorrelationModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORCovarianceModelFromVolatilityAndCorrelation;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Implements a proxy scheme WMC extension of a libor market model.
 * This implementation uses zero drift in the proxy scheme (for demonstration).
 *
 * @author Christian Fries
 * @version 1.0
 * @date 25.05.2006
 * @since finmath-lib 4.1.0
 */
public class SimpleLIBORMarketModelWithWMC extends SimpleLIBORMarketModel {

	private RandomVariable[]	discreteProcessWeights;
	private final SimpleLIBORMarketModel				targetScheme;

	/**
	 * @param timeDiscretizationFromArray The time discretization of the process (simulation time).
	 * @param liborPeriodDiscretization The discretization of the interest rate curve into forward rates (tenor structure).
	 * @param numberOfPaths The number of paths.
	 * @param liborInitialValues The initial values for the forward rates.
	 * @param volatilityModel  The volatility model to use.
	 * @param correlationModel The correlation model to use.
	 * @param targetScheme The model towards which the Monte-Carlo probabilites should be correted
	 */
	public SimpleLIBORMarketModelWithWMC(
			final TimeDiscretizationFromArray		timeDiscretizationFromArray,
			final TimeDiscretizationFromArray		liborPeriodDiscretization,
			final int				     	numberOfPaths,
			final double[]				liborInitialValues,
			final LIBORVolatilityModel    volatilityModel,
			final LIBORCorrelationModel   correlationModel,
			final SimpleLIBORMarketModel	targetScheme
			) {
		super(	timeDiscretizationFromArray,
				liborPeriodDiscretization,
				numberOfPaths,
				liborInitialValues,
				volatilityModel,
				correlationModel);
		this.targetScheme           = targetScheme;

		this.setBrownianMotion(targetScheme.getBrownianMotion());
		this.setMeasure(targetScheme.getMeasure());
	}


	/**
	 * This scheme has zero drift!
	 */
	@Override
	public RandomVariable getDrift(final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		//    	if(component == 10) return new RandomVariableFromDoubleArray(getTime(timeIndex), (Math.log(0.3)-Math.log(0.1)) / 5.0, getNumberOfPaths());
		//    	else				return super.getDrift(timeIndex, component, realizationAtTimeIndex,  realizationPredictor);
		//        return (new RandomVariableFromDoubleArray(getTime(timeIndex), (Math.log(0.3)-Math.log(0.1)) / 5.0 / this.getFactorLoading(timeIndex, 0, 10).get(0), getNumberOfPaths())).mult(this.getFactorLoading(timeIndex, 0, component)).add(super.getDrift(timeIndex, component, realizationAtTimeIndex,  realizationPredictor));
		//        return (new RandomVariableFromDoubleArray(getTime(timeIndex), 0.0 / this.getFactorLoading(timeIndex, 0, 10).get(0), getNumberOfPaths())).mult(this.getFactorLoading(timeIndex, 0, component)).add(super.getDrift(timeIndex, component, realizationAtTimeIndex,  realizationPredictor));
		return super.getDrift(timeIndex, component, realizationAtTimeIndex,  realizationPredictor);
	}


	/**
	 * This method returns the weights of a weighted Monte Carlo method (the
	 * probability density).
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 */
	@Override
	public RandomVariable getMonteCarloWeights(final int timeIndex) {
		// Christian Fries, 20040930: Note: If volatilty is zero, weighted MC will not work, because no weight can bring a zero probability region to positive probability. On the otherhand there is no drift to correct, if volatility is zero.
		// Christian Fries, 20041001: Note: Weighted MC will not work in a low factor model, because then only a low dimensional hyperplane of the state space will have non zero probability weight and it is not possible to leave this plane by weighted MC.
		// Christian Fries, 20060524: The remark 20041001 is not precise enough. The weighted MC works, but the two measure are not equivalent.

		if (discreteProcessWeights == null || discreteProcessWeights.length == 0) {
			// Precalculate the weights
			discreteProcessWeights = new RandomVariableFromDoubleArray[this.getTimeDiscretization().getNumberOfTimeSteps()+1];

			final double changeOfNumeraire = this.getNumeraire(0).get(0) / targetScheme.getNumeraire(0).get(0);

			discreteProcessWeights[0] = new RandomVariableFromDoubleArray(0.0, 1.0 / this.getNumberOfPaths() / changeOfNumeraire);

			// Initial value of target scheme is needed as a RandomVariableFromDoubleArray later for the drift
			final RandomVariable[] initialValueOfTargetScheme = new RandomVariableFromDoubleArray[this.getNumberOfComponents()];
			for (int componentIndex = 0; componentIndex < this.getNumberOfComponents(); componentIndex++) {
				initialValueOfTargetScheme[componentIndex] = targetScheme.getInitialValue(componentIndex);
			}

			// Initial value shift
			final RandomVariable[] initialValueLogShifts = new RandomVariable[this.getNumberOfComponents()];
			for (int componentIndex = 0; componentIndex < this.getNumberOfComponents(); componentIndex++) {
				final RandomVariable          initialValueLog         = (this.getInitialValue(componentIndex)).log();
				final RandomVariable          initialValueLogTarget   = (targetScheme.getInitialValue(componentIndex)).log();
				initialValueLogShifts[componentIndex] = initialValueLogTarget.sub(initialValueLog);
			}

			// Get the Brownian motion part of this process
			final BrownianMotion brownianMotion = this.getBrownianMotion();

			// Allocate temp. memory
			final double[][] factorDrift = new double[getNumberOfFactors()][getNumberOfPaths()];

			for (int timeIndex2 = 1; timeIndex2 < getTimeDiscretization().getNumberOfTimeSteps()+1; timeIndex2++) {
				final double deltaT = this.getTime(timeIndex2) - this.getTime(timeIndex2 - 1);

				// Allocate memory
				final double[] discreteProcessWeightsTimeIndex = new double[this.getNumberOfPaths()];

				// Copy previous path probabilities
				for (int path = 0; path < getNumberOfPaths(); path++) {
					discreteProcessWeightsTimeIndex[path] = discreteProcessWeights[timeIndex2 - 1].get(path);
				}

				for (int factor = 0; factor < getNumberOfFactors(); factor++) {
					// Clear factorDrift
					Arrays.fill(factorDrift[factor], 0.0);
				}

				// Calculate the factor drift
				//                for (int componentIndex = timeIndex2; componentIndex < this.getNumberOfComponents(); componentIndex++) {
				for (int componentIndex = 0; componentIndex < this.getNumberOfComponents(); componentIndex++) {
					final RandomVariable		initialValueLogShift        = (timeIndex2 == 1) ? initialValueLogShifts[componentIndex] : null;
					// @bug workaround
					// @bug should be target here
					final RandomVariable  driftProxy  = this.getDrift(timeIndex2-1, componentIndex, this.getProcessValue(timeIndex2-1), null);
					final RandomVariable  driftTarget = targetScheme.getDrift(timeIndex2 - 1, componentIndex, timeIndex2 == 1 ? initialValueOfTargetScheme : this.getProcessValue(timeIndex2-1), this.getProcessValue(timeIndex2));

					final double instvol = ((LIBORCovarianceModelFromVolatilityAndCorrelation)getCovarianceModel()).getVolatilityModel().getVolatility(timeIndex2-1, componentIndex).get(0);
					if(instvol == 0) {
						System.out.println("vol zero");
						continue;
					}
					for (int factor = 0; factor < getNumberOfFactors(); factor++) {
						final RandomVariable  factorLoadingPseudoInverse = getCovarianceModel().getFactorLoadingPseudoInverse(timeIndex2-1, componentIndex, factor, null);

						for (int path = 0; path < getNumberOfPaths(); path++) {
							factorDrift[factor][path] += factorLoadingPseudoInverse.get(path) * ((driftTarget.get(path) - driftProxy.get(path)) * deltaT + (initialValueLogShift != null ? initialValueLogShift.get(path) : 0.0));
						}
					}
				}

				/*
				for (int factor = 0; factor < getNumberOfFactors(); factor++) {
					System.out.println(factor + "\t" + factorDrift[factor][0]);
				}
				 */

				// Calculate the transition probabiltiy
				for (int factor = 0; factor < getNumberOfFactors(); factor++) {
					final RandomVariable brownianIncement = brownianMotion.getBrownianIncrement(timeIndex2 - 1, factor);
					for (int path = 0; path < getNumberOfPaths(); path++) {
						final double x = brownianIncement.get(path);
						final double y = x - factorDrift[factor][path];
						final double transitionPobabilityDensityRatio = Math.exp((- y * y + x * x) / (2.0 * deltaT));
						discreteProcessWeightsTimeIndex[path] *= transitionPobabilityDensityRatio;
					}
				}
				discreteProcessWeights[timeIndex2] = new RandomVariableFromDoubleArray(getTime(timeIndex2), discreteProcessWeightsTimeIndex);
			}
		}

		// Return value of process
		return discreteProcessWeights[timeIndex];
	}
}
