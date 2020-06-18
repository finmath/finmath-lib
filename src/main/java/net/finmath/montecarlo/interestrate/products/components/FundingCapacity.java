package net.finmath.montecarlo.interestrate.products.components;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Model the survival probability of a funding capacity
 * using a piecewise constant function for the instantaneous survival probability.
 * 
 * @author Christian Fries
 */
public class FundingCapacity extends AbstractProductComponent {

	private static final long serialVersionUID = 6863200178588875665L;

	private final SortedMap<Double, Double>	instantaneousSurvivalProbability;

	private Double				currentTime;
	private RandomVariable		currentCapacity;

	public FundingCapacity(String currency, RandomVariable intialCapacity, SortedMap<Double, Double> instantaneouseSurvivalProbability) {
		super(currency);
		this.currentTime = 0.0;
		this.currentCapacity = intialCapacity;
		this.instantaneousSurvivalProbability = instantaneouseSurvivalProbability;
	}

	public RandomVariable getSurvivalProbabilityRequiredFunding(double time, RandomVariable fundingRequirement) {

		synchronized (currentTime) {
			if(time < currentTime) {
				throw new IllegalStateException("The method getSurvivalProbabilityRequiredFunding must be called in successive order.");
			}
		}
		currentTime = time;

		RandomVariable fundingIntervallLeft, fundingIntervallRight;
		synchronized (currentCapacity) {
			RandomVariable newCapacity	= currentCapacity.add(fundingRequirement);		
			fundingIntervallLeft		= currentCapacity.cap(newCapacity);
			fundingIntervallRight		= currentCapacity.floor(newCapacity);
			currentCapacity = newCapacity;
		}

		RandomVariable integratedSurvivalProbability = new Scalar(0.0);
		double previousFundingLevel = -Double.MAX_VALUE;
		for(Map.Entry<Double, Double> entry : instantaneousSurvivalProbability.entrySet()) {
			double fundingLevel = entry.getKey();
			double survivalProbability = entry.getValue();

			integratedSurvivalProbability = integratedSurvivalProbability.add(
					fundingIntervallRight.cap(fundingLevel)
					.sub(fundingIntervallLeft.floor(previousFundingLevel))
					.floor(0.0)
					.mult(survivalProbability));
			previousFundingLevel = fundingLevel;
		}
		integratedSurvivalProbability = integratedSurvivalProbability.div(fundingIntervallRight.sub(fundingIntervallLeft));


		return integratedSurvivalProbability;		
	}

	public RandomVariable getCurrentFundingLevel() {
		return currentCapacity;
	}

	@Override
	public Set<String> queryUnderlyings() {
		return null;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException {
		throw new UnsupportedOperationException();
	}


}
