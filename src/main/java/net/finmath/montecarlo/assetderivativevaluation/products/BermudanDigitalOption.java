/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.stochastic.ConditionalExpectationEstimatorInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements the valuation of a Bermudan digital option paying
 * <br>
 * \( N_{i} \cdot \mathbb{1}(S(T_{i}) - K_{i}) \) at \( T_{i} \),
 * <br>
 * when exercised in \( T_{i} \), where \( N_{i} \) is the notional,
 * \( \mathbb{1} \) is the indicator function,
 * \( S \) is the underlying, \( K_{i} \) is the strike
 * and \( T_{i} \) the exercise date.
 * 
 * The code "demos" the two prominent methods for the valuation of Bermudan (American) products:
 * <ul>
 * 	<li>
 * 		The valuation may be performed using an estimation of the conditional expectation to determine the
 * 		exercise criteria. Apart from a possible foresight bias induced by the Monte-Carlo errors, this give a lower bound
 *		for the Bermudan value.
 * 	<li>
 * 		The valuation may be performed using the dual method based on a minimization problem, which gives an upper bound.
 * </ul>
 * 
 * 
 * @author Christian Fries
 * @version 1.4
 */
public class BermudanDigitalOption extends AbstractAssetMonteCarloProduct {

	public enum ExerciseMethod {
		ESTIMATE_COND_EXPECTATION,
		UPPER_BOUND_METHOD
	}

	private final double[]	exerciseDates;
	private final double[]	notionals;
	private final double[]	strikes;

	private int			orderOfRegressionPolynomial		= 4;
	private boolean		intrinsicValueAsBasisFunction	= true;

	private ExerciseMethod exerciseMethod = ExerciseMethod.ESTIMATE_COND_EXPECTATION;

	/**
	 * Create a Bermudan option paying
	 * N(i) * (S(T(i)) - K(i)) at T(i),
	 * when exercised in T(i), where N(i) is the notional, S is the underlying, K(i) is the strike
	 * and T(i) the exercise date.
	 * 
	 * @param exerciseDates The exercise dates (T(i)), given as doubles.
	 * @param notionals The notionals (N(i)) for each exercise date.
	 * @param strikes The strikes (K(i)) for each exercise date.
	 * @param exerciseMethod The exercise method to be used for the estimation of the exercise boundary.
	 * @param properties Use this map to specify special product parameters, e.g. "orderOfRegressionPolynomial" (Integer).
	 */
	public BermudanDigitalOption(
			double[] exerciseDates,
			double[] notionals,
			double[] strikes,
			ExerciseMethod exerciseMethod,
			Map<String, Object> properties) {
		super();
		this.exerciseDates = exerciseDates;
		this.notionals = notionals;
		this.strikes = strikes;
		this.exerciseMethod = exerciseMethod;
		orderOfRegressionPolynomial = (Integer)properties.getOrDefault("orderOfRegressionPolynomial", 4);
	}

	/**
	 * This method returns the value random variable of the product within the specified model,
	 * evaluated at a given evalutationTime.
	 * Cash-flows prior evaluationTime are not considered.
	 * 
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 * 
	 */
	@Override
	public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {
		if(this.exerciseMethod == ExerciseMethod.UPPER_BOUND_METHOD) {
			// Find optimal lambda
			GoldenSectionSearch optimizer = new GoldenSectionSearch(-1.0, 1.0);
			while(!optimizer.isDone()) {
				double lambda = optimizer.getNextPoint();
				double value = this.getValues(evaluationTime, model, lambda).getAverage();
				optimizer.setValue(value);
			}
			return getValues(evaluationTime, model, optimizer.getBestPoint());
		}
		else {
			return getValues(evaluationTime, model, 0.0);
		}
	}

	private RandomVariableInterface getValues(double evaluationTime, AssetModelMonteCarloSimulationInterface model, double lambda) throws CalculationException {
		/*
		 * We are going backward in time (note that this bears the risk of an foresight bias).
		 * We store the value of the option, if not exercised in a vector. Is is not allowed to used the specific entry in this vector
		 * in the exercise decision (perfect foresight). Instead some exercise strategy / estimate has to be used.
		 */

		// Initialize our value random variable: the value of the option if we never exercise is zero
		RandomVariableInterface	value			= model.getRandomVariableForConstant(0.0);

		RandomVariableInterface	exerciseTime	= model.getRandomVariableForConstant(exerciseDates[exerciseDates.length-1]+1);

		RandomVariableInterface one				= model.getRandomVariableForConstant(1.0);

		for(int exerciseDateIndex=exerciseDates.length-1; exerciseDateIndex>=0; exerciseDateIndex--)
		{
			double exerciseDate = exerciseDates[exerciseDateIndex];
			double notional     = notionals[exerciseDateIndex];
			double strike       = strikes[exerciseDateIndex];

			// Get some model values upon exercise date
			RandomVariableInterface underlyingAtExercise	= model.getAssetValue(exerciseDate,0);
			RandomVariableInterface numeraireAtPayment		= model.getNumeraire(exerciseDate);
			RandomVariableInterface monteCarloWeights		= model.getMonteCarloWeights(exerciseDate);

			// Value received if exercised at current time
			RandomVariableInterface valueOfPaymentsIfExercised = one.mult(notional).div(numeraireAtPayment);//.mult(monteCarloWeights);

			// Create a conditional expectation estimator with some basis functions (predictor variables) for conditional expectation estimation.
			ConditionalExpectationEstimatorInterface condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(underlyingAtExercise));

			RandomVariableInterface underlying	= null;
			RandomVariableInterface trigger		= null;

			// Calculate the exercise criteria (exercise if the following trigger is negative)
			switch(exerciseMethod) {
			case ESTIMATE_COND_EXPECTATION:
				// Calculate conditional expectation on numeraire relative quantity.
				RandomVariableInterface valueIfNotExcercisedEstimated = value.getConditionalExpectation(condExpEstimator);

				underlying	= underlyingAtExercise.sub(strike).mult(notional).div(numeraireAtPayment);//.mult(monteCarloWeights);
				trigger		= underlying.sub(valueIfNotExcercisedEstimated);
				break;
			case UPPER_BOUND_METHOD:
				throw new UnsupportedOperationException(exerciseMethod + " is currently not supported.");
				/*
				RandomVariableInterface martingale		= model.getAssetValue(exerciseDates[exerciseDateIndex], 0).div(model.getNumeraire(exerciseDates[exerciseDateIndex]));
				// Construct a martingale with initial value being zero.
				martingale = martingale.sub(martingale.getAverage()).mult(lambda);

				// Initialize value as 0-M, if we are on the last exercise date.
				if(exerciseDateIndex==exerciseDates.length-1) value = value.sub(martingale);

				underlying	= valueOfPaymentsIfExercised.sub(martingale);
				trigger		= value.sub(underlying);
				break;
				 */
			}

			// If trigger is positive keep value, otherwise take underlying
			value			= value.barrier(trigger, valueOfPaymentsIfExercised, value);
			exerciseTime	= exerciseTime.barrier(trigger, exerciseTime, exerciseDate);
		}

		// Uncomment the following if you like to check exercise probabilities
		/*
		double[] probabilities = exerciseTime.getHistogram(exerciseDates);
		for(int exerciseDateIndex=0; exerciseDateIndex<exerciseDates.length; exerciseDateIndex++) {
			double time = exerciseDates[exerciseDateIndex];
			System.out.println(time + "\t" + probabilities[exerciseDateIndex]);
		}
		System.out.println("NEVER" + "\t" + probabilities[exerciseDates.length]);
		*/
		
		// Note that values is a relative price - no numeraire division is required
		RandomVariableInterface	numeraireAtZero					= model.getNumeraire(evaluationTime);
		RandomVariableInterface	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
		value = value.mult(numeraireAtZero);//.div(monteCarloProbabilitiesAtZero);

		return value;
	}


	private RandomVariableInterface[] getRegressionBasisFunctions(RandomVariableInterface underlying) {
		RandomVariableInterface[] basisFunctions;

		// Create basis functions - here: 1, S, S^2, S^3, S^4
		basisFunctions = new RandomVariableInterface[orderOfRegressionPolynomial+1];
		basisFunctions[0] = new RandomVariable(1.0);	// Random variable = 1
		basisFunctions[1] = underlying;					// Random variable = S
		for(int powerOfRegressionMonomial=2; powerOfRegressionMonomial<=orderOfRegressionPolynomial; powerOfRegressionMonomial++) {
			basisFunctions[powerOfRegressionMonomial] = underlying.pow(powerOfRegressionMonomial);
		}

		return basisFunctions;
	}
}
