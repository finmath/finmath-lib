/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.lang3.Validate;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * This class implements the valuation of a Bermudan option paying
 * <br>
 * <i>	N(i) * (S(T(i)) - K(i))	</i> 	at <i>T(i)</i>,
 * <br>
 * when exercised in T(i), where N(i) is the notional, S is the underlying, K(i) is the strike
 * and T(i) the exercise date.
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
 * @version 1.5
 */
public class BermudanOption extends AbstractAssetMonteCarloProduct {

	public enum ExerciseMethod {
		ESTIMATE_COND_EXPECTATION,
		UPPER_BOUND_METHOD
	}

	private final double[]	exerciseDates;
	private final double[]	notionals;
	private final double[]	strikes;

	private final int			numberOfBasisFunctions;
	private final boolean		intrinsicValueAsBasisFunction;

	private final boolean		useBinning;

	private final ExerciseMethod exerciseMethod;

	/*
	 * For analysis only
	 */
	private RandomVariable		lastValuationExerciseTime;
	private RandomVariable[]	lastValuationExerciseValueAtExerciseTime;
	private RandomVariable[]	lastValuationContinuationValueAtExerciseTime;
	private RandomVariable[]	lastValuationContinuationValueEstimatedAtExerciseTime;	
	
	/**
	 * Create a Bermudan option paying
	 * 		N(i) * (S(T(i)) - K(i)) at T(i),
	 * when exercised in T(i), where N(i) is the notional, S is the underlying, K(i) is the strike
	 * and T(i) the exercise date.
	 *
	 * @param exerciseDates The exercise dates (T(i)), given as doubles.
	 * @param notionals The notionals (N(i)) for each exercise date.
	 * @param strikes The strikes (K(i)) for each exercise date.
	 * @param exerciseMethod The exercise method to be used for the estimation of the exercise boundary.
	 * @param numberOfBasisFunctions The number of basis functions (if exerciseMethod == ESTIMATE_COND_EXPECTATION, otherwise ignored)
	 * @param intrinsicValueAsBasisFunction If true max(S-K,0) is used to build the basis function, otherwise S is used to build the basis function.
	 * @param useBinning If false, the basis functions are polynomials of increasing order (1, S S^2, ...). If true, the basis functions are bins.
	 */
	public BermudanOption(
			final double[]			exerciseDates,
			final double[]			notionals,
			final double[]			strikes,
			final ExerciseMethod	exerciseMethod,			
			final int				numberOfBasisFunctions,
			final boolean			intrinsicValueAsBasisFunction,
			final boolean			useBinning) {
		super();
		Validate.isTrue(numberOfBasisFunctions > 0, "The vaue of numberOfBasisFunctions must be larger or equal 1. %s" , numberOfBasisFunctions);
		
		this.exerciseDates = exerciseDates;
		this.notionals = notionals;
		this.strikes = strikes;
		this.exerciseMethod = exerciseMethod;
		this.numberOfBasisFunctions = numberOfBasisFunctions;
		this.intrinsicValueAsBasisFunction = intrinsicValueAsBasisFunction;
		this.useBinning = useBinning;
	}

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
	 */
	public BermudanOption(
			final double[] exerciseDates,
			final double[] notionals,
			final double[] strikes,
			final ExerciseMethod exerciseMethod) {
		this(exerciseDates, notionals, strikes, exerciseMethod, 5, false, false);
	}

	/**
	 * Create a Bermudan option paying
	 * N(i) * (S(T(i)) - K(i)) at T(i),
	 * when exercised in T(i), where N(i) is the notional, S is the underlying, K(i) is the strike
	 * and T(i) the exercise date.
	 *
	 * The product will use ExerciseMethod.ESTIMATE_COND_EXPECTATION.
	 *
	 * @param exerciseDates The exercise dates (T(i)), given as doubles.
	 * @param notionals The notionals (N(i)) for each exercise date.
	 * @param strikes The strikes (K(i)) for each exercise date.
	 */
	public BermudanOption(
			final double[] exerciseDates,
			final double[] notionals,
			final double[] strikes) {
		this(exerciseDates, notionals, strikes, ExerciseMethod.ESTIMATE_COND_EXPECTATION);
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
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {
		if(exerciseMethod == ExerciseMethod.UPPER_BOUND_METHOD) {
			// Find optimal lambda
			final GoldenSectionSearch optimizer = new GoldenSectionSearch(-1.0, 1.0);
			while(!optimizer.isDone()) {
				final double lambda = optimizer.getNextPoint();
				final double value = this.getValue(evaluationTime, model, lambda).getAverage();
				optimizer.setValue(value);
			}
			return getValue(evaluationTime, model, optimizer.getBestPoint());
		}
		else {
			return getValue(evaluationTime, model, 0.0);
		}
	}

	/**
	 * Valuation where the parameter lambda is used as a factor to the martingale used to remove foresight in the upper bond method.
	 * Used to optimize the value of lambda.
	 * 
	 * For {@link ExerciseMethod} being ESTIMATE_COND_EXPECTATION the lambda has no effect.
	 * 
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @param lambda The parameter lambda used as a factor to the martingale used to remove foresight in the upper bond method.
	 * @return The random variable representing the value of the product discounted to evaluation time.
	 * @throws CalculationException
	 */
	private RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model, final double lambda) throws CalculationException {
		/*
		 * We are going backward in time (note that this bears the risk of an foresight bias).
		 * We store the value of the option, if not exercised in a vector. Is is not allowed to used the specific entry in this vector
		 * in the exercise decision (perfect foresight). Instead some exercise strategy / estimate has to be used.
		 */

		// Initialize our value random variable: the value of the option if we never exercise is zero
		RandomVariable	value			= model.getRandomVariableForConstant(0.0);

		RandomVariable	exerciseTime	= model.getRandomVariableForConstant(exerciseDates[exerciseDates.length-1]+1);
		lastValuationExerciseValueAtExerciseTime = new RandomVariable[exerciseDates.length];
		lastValuationContinuationValueAtExerciseTime = new RandomVariable[exerciseDates.length];
		lastValuationContinuationValueEstimatedAtExerciseTime = new RandomVariable[exerciseDates.length];
				
		for(int exerciseDateIndex=exerciseDates.length-1; exerciseDateIndex>=0; exerciseDateIndex--)
		{
			final double exerciseDate = exerciseDates[exerciseDateIndex];
			final double notional     = notionals[exerciseDateIndex];
			final double strike       = strikes[exerciseDateIndex];

			// Get some model values upon exercise date
			final RandomVariable underlyingAtExercise	= model.getAssetValue(exerciseDate,0);
			final RandomVariable numeraireAtPayment		= model.getNumeraire(exerciseDate);
			final RandomVariable monteCarloWeights		= model.getMonteCarloWeights(exerciseDate);

			// Value received if exercised at current time
			final RandomVariable valueOfPaymentsIfExercised = underlyingAtExercise.sub(strike).mult(notional).div(numeraireAtPayment).mult(monteCarloWeights);

			/*
			 * Calculate the exercise criteria (exercise if the following exerciseCriteria is negative)
			 */
			RandomVariable exerciseValue = null;
			RandomVariable exerciseCriteria = null;
			switch(exerciseMethod) {
			case ESTIMATE_COND_EXPECTATION:
				// Create a conditional expectation estimator with some basis functions (predictor variables) for conditional expectation estimation.
				ArrayList<RandomVariable> basisFunctions;
				RandomVariable basisFunctionUnderlying = intrinsicValueAsBasisFunction ? underlyingAtExercise.sub(strike).floor(0.0) : underlyingAtExercise;
				basisFunctions = useBinning ? getRegressionBasisFunctionsBinning(basisFunctionUnderlying) : getRegressionBasisFunctions(basisFunctionUnderlying);
				
				final ConditionalExpectationEstimator condExpEstimator = new MonteCarloConditionalExpectationRegression(basisFunctions.toArray(new RandomVariable[0]));

				// Calculate conditional expectation on numeraire relative quantity.
				final RandomVariable valueIfNotExcercisedEstimated = value.getConditionalExpectation(condExpEstimator);

				exerciseValue		= valueOfPaymentsIfExercised;
				exerciseCriteria	= valueIfNotExcercisedEstimated.sub(exerciseValue);
				break;
			case UPPER_BOUND_METHOD:
				RandomVariable martingale		= model.getAssetValue(exerciseDates[exerciseDateIndex], 0).div(model.getNumeraire(exerciseDates[exerciseDateIndex]));
				// Construct a martingale with initial value being zero.
				martingale = martingale.sub(martingale.getAverage()).mult(lambda);

				// Initialize value as 0-M, if we are on the last exercise date.
				if(exerciseDateIndex==exerciseDates.length-1) {
					value = value.sub(martingale);
				}

				exerciseValue		= valueOfPaymentsIfExercised.sub(martingale);
				exerciseCriteria	= value.sub(exerciseValue);
				break;
			default:
				throw new IllegalArgumentException("Unknown exerciseMethod " + exerciseMethod + ".");
			}
			lastValuationExerciseValueAtExerciseTime[exerciseDateIndex]					= exerciseValue.mult(model.getNumeraire(exerciseDates[exerciseDateIndex])).div(model.getMonteCarloWeights(exerciseDates[exerciseDateIndex]));
			lastValuationContinuationValueAtExerciseTime[exerciseDateIndex]				= value.mult(model.getNumeraire(exerciseDates[exerciseDateIndex])).div(model.getMonteCarloWeights(exerciseDates[exerciseDateIndex]));
			lastValuationContinuationValueEstimatedAtExerciseTime[exerciseDateIndex]	= exerciseCriteria.add(exerciseValue).mult(model.getNumeraire(exerciseDates[exerciseDateIndex])).div(model.getMonteCarloWeights(exerciseDates[exerciseDateIndex]));

			// If trigger is positive keep value, otherwise take underlying
			value			= exerciseCriteria.choose(value, exerciseValue);
			exerciseTime	= exerciseCriteria.choose(exerciseTime, new Scalar(exerciseDate));

		}
		lastValuationExerciseTime = exerciseTime;

		// Note that values is a relative price - no numeraire division is required
		final RandomVariable	numeraireAtEvalTime			= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloWeightsAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		value = value.mult(numeraireAtEvalTime).div(monteCarloWeightsAtEvalTime);

		return value;
	}

	public double[] getExerciseDates() {
		return exerciseDates;
	}

	public double[] getNotionals() {
		return notionals;
	}

	public double[] getStrikes() {
		return strikes;
	}

	public RandomVariable getLastValuationExerciseTime() {
		return lastValuationExerciseTime;
	}

	public RandomVariable[] getLastValuationExerciseValueAtExerciseTime() {
		return lastValuationExerciseValueAtExerciseTime;
	}

	public RandomVariable[] getLastValuationContinuationValueAtExerciseTime() {
		return lastValuationContinuationValueAtExerciseTime;
	}

	public RandomVariable[] getLastValuationContinuationValueEstimatedAtExerciseTime() {
		return lastValuationContinuationValueEstimatedAtExerciseTime;
	}

	private ArrayList<RandomVariable> getRegressionBasisFunctions(RandomVariable underlying) {
		
		final int orderOfRegressionPolynomial = numberOfBasisFunctions-1;		// Choose maybe something like 4 (numberOfBasisFunctions = 5)

		underlying = new RandomVariableFromDoubleArray(0.0, underlying.getRealizations());

		// Create basis functions - here: 1, S, S^2, S^3, S^4
		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();
		for(int powerOfRegressionMonomial=0; powerOfRegressionMonomial<=orderOfRegressionPolynomial; powerOfRegressionMonomial++) {
			basisFunctions.add(underlying.pow(powerOfRegressionMonomial));
		}

		return basisFunctions;
	}

	private ArrayList<RandomVariable> getRegressionBasisFunctionsBinning(RandomVariable underlying) {

		final int numberOfBins = numberOfBasisFunctions;		// Choose maybe something like 20 (numberOfBasisFunctions = 20)

		underlying = new RandomVariableFromDoubleArray(0.0, underlying.getRealizations());
		final double[] values = underlying.getRealizations();
		Arrays.sort(values);

		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();
		for(int i = 0; i<numberOfBins; i++) {
			final double binLeft = values[(int)((double)i/(double)numberOfBins*values.length)];
			final RandomVariable basisFunction = underlying.sub(binLeft).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
			basisFunctions.add(basisFunction);
		}

		return basisFunctions;
	}
}
