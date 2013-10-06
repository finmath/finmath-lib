/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 23.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectation;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.optimizer.GoldenSectionSearch;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements the valuation of a Bermudan option paying
 * N(i) * (S(T(i)) - K(i)) at T(i),
 * when exercised in T(i), where N(i) is the notional, S is the underlying, K(i) is the strike
 * and T(i) the exercise date.
 * 
 * @author Christian Fries
 * @version 1.3
 */
public class BermudanOption extends AbstractAssetMonteCarloProduct {
    
	public enum ExerciseMethod {
		ESTIMATE_COND_EXPECTATION,
		UPPER_BOUND_METHOD
	}
	
    private final double[]	exerciseDates;
    private final double[]	notionals;
    private final double[]	strikes;
    
    private final int			orderOfRegressionPolynomial		= 4;
    private final boolean		intrinsicValueAsBasisFunction	= true;
    
    private ExerciseMethod exerciseMethod = ExerciseMethod.UPPER_BOUND_METHOD;
    
    /**
     * Create a Bermudan option paying
     * N(i) * (S(T(i)) - K(i)) at T(i),
     * when exercised in T(i), where N(i) is the notional, S is the underlying, K(i) is the strike
     * and T(i) the exercise date.
     * 
     * @param exerciseDates The exercise dates (T(i)), given as doubles.
     * @param notionals The notionals (N(i)) for each exercise date.
     * @param strikes The strikes (K(i)) for each exercise date.
     */
    public BermudanOption(
            double[] exerciseDates,
            double[] notionals,
            double[] strikes) {
        super();
        this.exerciseDates = exerciseDates;
        this.notionals = notionals;
        this.strikes = strikes;
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
            double[] exerciseDates,
            double[] notionals,
            double[] strikes,
            ExerciseMethod exerciseMethod) {
        super();
        this.exerciseDates = exerciseDates;
        this.notionals = notionals;
        this.strikes = strikes;
        this.exerciseMethod = exerciseMethod;
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
        RandomVariableInterface   value = model.getRandomVariableForConstant(0.0);

        RandomVariableInterface	exerciseTime = model.getRandomVariableForConstant(exerciseDates[exerciseDates.length-1]+1);

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
            RandomVariableInterface valueOfPaymentsIfExercised = underlyingAtExercise.getMutableCopy().sub(strike).mult(notional).div(numeraireAtPayment).mult(monteCarloWeights);

            // Create a conditional expectation estimator with some basis functions (predictor variables) for conditional expectation estimation.
            MonteCarloConditionalExpectation condExpEstimator;
            if(intrinsicValueAsBasisFunction)	condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(underlyingAtExercise.sub(strike).floor(0.0)));
            else								condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(underlyingAtExercise));

            RandomVariableInterface underlying	= null;
            RandomVariableInterface trigger		= null;
            
            // Calculate the exercise criteria (exercise if the following trigger is negative)
            switch(exerciseMethod) {
            case ESTIMATE_COND_EXPECTATION:
                // Calculate conditional expectation on numeraire relative quantity.
                RandomVariableInterface valueIfNotExcercisedEstimated = condExpEstimator.getConditionalExpectation(value);

                underlying	= valueOfPaymentsIfExercised;
                trigger		= valueIfNotExcercisedEstimated.sub(underlying);
            	break;
            case UPPER_BOUND_METHOD:
            	RandomVariableInterface martingale		= model.getAssetValue(exerciseDates[exerciseDateIndex], 0).div(model.getNumeraire(exerciseDates[exerciseDateIndex]));
                martingale = martingale.sub(martingale.getAverage()).mult(lambda);

                underlying	= valueOfPaymentsIfExercised.getMutableCopy().sub(martingale);
                if(exerciseDateIndex==exerciseDates.length-1) value.sub(martingale);
                trigger		= value.getMutableCopy().sub(underlying);
                break;
            }

            // If trigger is positive keep value, otherwise take underlying
            value = value.barrier(trigger, value, underlying);
            exerciseTime = exerciseTime.barrier(trigger, exerciseTime, exerciseDate);
        }

        // Uncomment the following if you like to check exercise probabilities
        /*
    	double[] probabilities = exerciseTime.getHistogram(exerciseDates);
        for(int exerciseDateIndex=0; exerciseDateIndex<exerciseDates.length; exerciseDateIndex++)
        {
        	double time = exerciseDates[exerciseDateIndex];
        	System.out.println(time + "\t" + probabilities[exerciseDateIndex]);
        }
    	System.out.println("NEVER" + "\t" + probabilities[exerciseDates.length]);
        */
        
        // Note that values is a relative price - no numeraire division is required
        RandomVariableInterface	numeraireAtZero					= model.getNumeraire(evaluationTime);
        RandomVariableInterface	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
        value = value.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);
        
        return value;
    }


    private RandomVariableInterface[] getRegressionBasisFunctions(RandomVariableInterface underlying) {
    	RandomVariableInterface[] basisFunctions;

    	// Create basis functions - here: 1, S, S^2, S^3, S^4
    	basisFunctions = new RandomVariableInterface[orderOfRegressionPolynomial+1];
    	basisFunctions[0] = new RandomVariable(1.0);	// Random variable = 1
    	basisFunctions[1] = underlying.getMutableCopy();			// Random variable = S
    	for(int powerOfRegressionMonomial=2; powerOfRegressionMonomial<=orderOfRegressionPolynomial; powerOfRegressionMonomial++) {
    		basisFunctions[powerOfRegressionMonomial] = underlying.getMutableCopy().pow(powerOfRegressionMonomial);
        }
        
        return basisFunctions;
    }
}
