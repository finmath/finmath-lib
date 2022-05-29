package net.finmath.montecarlo.interestrate.models;

import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.components.AbstractProductComponent;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * Models the notional dependent survival probability and default compensation
 * of a funding capacity (funding provider)
 * using a piecewise constant function for the instantaneous survival probability.
 *
 * The piecewise constant instantaneous survival probability has to be provided
 * by a SortedMap&lt;Double, Double&gt; instantaneouseSurvivalProbability.
 *
 * This map defines the mapping \( x_{i} \mapsto q_{i} \). Defining
 * \[ q(x) = q_{i} \text{\ for\ } x \in (x_{i-1}-x_{i}] \] the
 * <code>getDefaultFactors</code> method of this class calculates
 * for a given argument \( (t,x) \):
 * <dl>
 * 	<dt>
 * 		the effective survival probability
 * 	</dt>
 * 	<dd>
 * 		\[ \frac{1}{x} \int_{a}^{a+x} q(\xi) \mathrm{d}\xi \],
 *
 * 		where a denotes the current level of fund provided by this capacity, and
 * 	</dd>
 * 	<dt>
 * 		the effective default compensation factor R, such that
 * 	</dt>
 * 	<dd>
 * 		\[ \frac{1}{x} \int_{a}^{a+R x} q(\xi) \mathrm{d}\xi \ = \ 1 \],
 * 	</dd>
 * </dl>
 *
 * Important: since the class keeps track of past fundings
 * used, it is mandatory that the factors are calculated in
 * time-sequential order.
 *
 * @author Christian Fries
 */
public class FundingCapacity extends AbstractProductComponent {

	private static final long serialVersionUID = 6863200178588875665L;

	private final SortedMap<Double, Double>	instantaneousSurvivalProbability;

	private Double				currentTime;
	private RandomVariable		currentCapacity;

	public class DefaultFactors {
		private final RandomVariable survivalProbability;
		private final RandomVariable defaultCompensation;

		public DefaultFactors(RandomVariable survivalProbability, RandomVariable defaultCompensation) {
			this.survivalProbability = survivalProbability;
			this.defaultCompensation = defaultCompensation;
		}

		public RandomVariable getSurvivalProbability() {
			return survivalProbability;
		}

		public RandomVariable getDefaultCompensation() {
			return defaultCompensation;
		}
	}

	public FundingCapacity(String currency, RandomVariable intialCapacity, SortedMap<Double, Double> instantaneouseSurvivalProbability) {
		super(currency);
		this.currentTime = 0.0;
		this.currentCapacity = intialCapacity;
		this.instantaneousSurvivalProbability = instantaneouseSurvivalProbability;
	}

	public DefaultFactors getDefaultFactors(double time, RandomVariable fundingRequirement) {

		/*
		 * Determine integral bounds (synchronized for thread safety)
		 */
		RandomVariable fundingIntervalLeft, fundingIntervalRight;
		synchronized (currentTime) {
			if(time < currentTime) {
				throw new IllegalStateException("The method getSurvivalProbabilityRequiredFunding must be called in successive order.");
			}
			currentTime = time;

			/*
			 * The fundingRequirement may be negative, in which case funding is returned to the provider.
			 * We first calculate the lower and upper integral bounds from the fundingRequirement.
			 * The integral calculated is always positive, since we require only the factor.
			 */
			final RandomVariable newCapacity	= currentCapacity.add(fundingRequirement);
			fundingIntervalLeft		= currentCapacity.cap(newCapacity);		// min(current,new)
			fundingIntervalRight		= currentCapacity.floor(newCapacity);	// max(current,new)
			currentCapacity = newCapacity;
		}

		RandomVariable integratedSurvivalProbability = new Scalar(0.0);
		RandomVariable integratedDefaultCompensation = new Scalar(0.0);

		double previousFundingLevel = -Double.MAX_VALUE;
		double previousProvidedLevel = -Double.MAX_VALUE;
		for(final Map.Entry<Double, Double> entry : instantaneousSurvivalProbability.entrySet()) {
			final double fundingLevel = entry.getKey();
			final double survivalProbability = entry.getValue();

			final double providedLevel = Math.max(previousProvidedLevel,0) + (fundingLevel-Math.max(previousFundingLevel,0)) * survivalProbability;

			integratedDefaultCompensation = integratedDefaultCompensation.add(
					fundingIntervalRight.cap(providedLevel)
					.sub(fundingIntervalLeft.floor(previousProvidedLevel))
					.floor(0.0)
					.div(survivalProbability));

			integratedSurvivalProbability = integratedSurvivalProbability.add(
					fundingIntervalRight.cap(fundingLevel)
					.sub(fundingIntervalLeft.floor(previousFundingLevel))
					.floor(0.0)
					.mult(survivalProbability));

			previousFundingLevel = fundingLevel;
			previousProvidedLevel = providedLevel;

		}

		// The cap is used to map to avoid 0*infty to zero.
		final RandomVariable oneOverFundingAmount = fundingIntervalRight.sub(fundingIntervalLeft).invert().cap(Double.MAX_VALUE);
		integratedSurvivalProbability = integratedSurvivalProbability.mult(oneOverFundingAmount);
		integratedDefaultCompensation = integratedDefaultCompensation.mult(oneOverFundingAmount);

		return new DefaultFactors(integratedSurvivalProbability, integratedDefaultCompensation);
	}

	public RandomVariable getDefaultCompensationForRequiredFunding(double time, RandomVariable fundingRequirement) {

		RandomVariable fundingIntervalLeft, fundingIntervalRight;
		synchronized (currentTime) {
			if(time < currentTime) {
				throw new IllegalStateException("The method getSurvivalProbabilityRequiredFunding must be called in successive order.");
			}
			currentTime = time;

			/*
			 * The fundingRequirement may be negative, in which case funding is retured to the provides.
			 * We first calculate the lower and upper integral bounds from the fundingRequirement.
			 * The integral calculated is always positive, the correct sign of the integral will be checked later.
			 */
			final RandomVariable newCapacity	= currentCapacity.add(fundingRequirement);
			fundingIntervalLeft		= currentCapacity.cap(newCapacity);		// min(current,new)
			fundingIntervalRight		= currentCapacity.floor(newCapacity);	// max(current,new)
			currentCapacity = newCapacity;
		}

		RandomVariable integratedSurvivalProbability = new Scalar(0.0);
		double previousFundingLevel = -Double.MAX_VALUE;
		final double previousProvidedLevel = -Double.MAX_VALUE;
		for(final Map.Entry<Double, Double> entry : instantaneousSurvivalProbability.entrySet()) {
			final double fundingLevel = entry.getKey();
			final double survivalProbability = entry.getValue();

			final double providedLevel = Math.max(previousProvidedLevel,0) + (fundingLevel-Math.max(previousFundingLevel,0)) * survivalProbability;

			integratedSurvivalProbability = integratedSurvivalProbability.add(
					fundingIntervalRight.cap(providedLevel)
					.sub(fundingIntervalLeft.floor(previousProvidedLevel))
					.floor(0.0)
					.div(survivalProbability));
			previousFundingLevel = fundingLevel;
		}
		integratedSurvivalProbability = integratedSurvivalProbability.div(fundingIntervalRight.sub(fundingIntervalLeft));


		return integratedSurvivalProbability;
	}

	public RandomVariable getSurvivalProbabilityRequiredFunding(double time, RandomVariable fundingRequirement) {

		RandomVariable fundingIntervalLeft, fundingIntervalRight;
		synchronized (currentTime) {
			if(time < currentTime) {
				throw new IllegalStateException("The method getSurvivalProbabilityRequiredFunding must be called in successive order.");
			}

			currentTime = time;

			/*
			 * The fundingRequirement may be negative, in which case funding is retured to the provides.
			 * We first calculate the lower and upper integral bounds from the fundingRequirement.
			 * The integral calculated is always positive, the correct sign of the integral will be checked later.
			 */
			final RandomVariable newCapacity	= currentCapacity.add(fundingRequirement);
			fundingIntervalLeft		= currentCapacity.cap(newCapacity);		// min(current,new)
			fundingIntervalRight		= currentCapacity.floor(newCapacity);	// max(current,new)
			currentCapacity = newCapacity;
		}

		RandomVariable integratedSurvivalProbability = new Scalar(0.0);
		double previousFundingLevel = -Double.MAX_VALUE;
		for(final Map.Entry<Double, Double> entry : instantaneousSurvivalProbability.entrySet()) {
			final double fundingLevel = entry.getKey();
			final double survivalProbability = entry.getValue();

			integratedSurvivalProbability = integratedSurvivalProbability.add(
					fundingIntervalRight.cap(fundingLevel)
					.sub(fundingIntervalLeft.floor(previousFundingLevel))
					.floor(0.0)
					.mult(survivalProbability));
			previousFundingLevel = fundingLevel;
		}
		integratedSurvivalProbability = integratedSurvivalProbability.div(fundingIntervalRight.sub(fundingIntervalLeft));


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
	public RandomVariable getValue(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException {
		throw new UnsupportedOperationException();
	}
}
