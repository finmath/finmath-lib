/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 13.08.2004
 */
package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.functions.LinearAlgebra;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * A service that allows to estimate conditional expectation via regression.
 * In oder to estimate the conditional expectation, basis functions have to be
 * specified.
 * 
 * The class can either estimate and predict the conditional expectation within
 * the same simulation (which will eventually introduce a small foresight bias)
 * or use a different simulation for estimation (using <code>basisFunctionsEstimator</code>)
 * to predict conditional expectation within another simulation
 * (using <code>basisFunctionsPredictor</code>). In the latter case, the
 * basis functions have to correspond to the same entities, however, generated in
 * different simulations (number of path, etc., may be different).
 *  
 * @author Christian Fries
 */
public class MonteCarloConditionalExpectationRegression implements MonteCarloConditionalExpectation {

	private RandomVariableInterface[]    basisFunctionsEstimator		= null;
	private RandomVariableInterface[]    basisFunctionsPredictor		= null;
    
    /**
     * Creates a class for conditional expectation estimation.
     * 
     * @param basisFunctions A vector of random variables to be used as basis functions.
     */
    public MonteCarloConditionalExpectationRegression(RandomVariableInterface[] basisFunctions) {
        super();
        this.basisFunctionsEstimator = basisFunctions;
        this.basisFunctionsPredictor = basisFunctions;
    }

    /**
     * Creates a class for conditional expectation estimation.
     * 
     * @param basisFunctionsEstimator A vector of random variables to be used as basis functions for estimation.
     * @param basisFunctionsPredictor A vector of random variables to be used as basis functions for prediction.
     */
    public MonteCarloConditionalExpectationRegression(RandomVariableInterface[] basisFunctionsEstimator, RandomVariableInterface[] basisFunctionsPredictor) {
        super();
        this.basisFunctionsEstimator = basisFunctionsEstimator;
        this.basisFunctionsPredictor = basisFunctionsPredictor;
    }

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectation#getConditionalExpectation(net.finmath.stochastic.RandomVariableInterface)
     */
    public RandomVariableInterface getConditionalExpectation(RandomVariableInterface randomVariable) {
    	double[] linearRegressionParameters = getLinearRegressionParameters(randomVariable);

    	// Calculate estimate
        RandomVariableInterface[] basisFunctions = getNonZeroBasisFunctions(basisFunctionsPredictor);

        RandomVariableInterface conditionalExpectation = basisFunctions[0].mult(linearRegressionParameters[0]);
        for(int i=1; i<basisFunctions.length; i++) {
            conditionalExpectation = conditionalExpectation.addProduct(basisFunctions[i], linearRegressionParameters[i]);
        }

        return conditionalExpectation;
    }
    
    
	public double[] getLinearRegressionParameters(RandomVariableInterface dependents) {

        // Build XTX - the symmetric matrix consisting of the scalar products of the basis functions.
        RandomVariableInterface[] basisFunctions = getNonZeroBasisFunctions(basisFunctionsEstimator);
        double[][] XTX = new double[basisFunctions.length][basisFunctions.length];
        for(int i=0; i<basisFunctions.length; i++) {
            for(int j=i; j<basisFunctions.length; j++) {
            	XTX[i][j] = basisFunctions[i].getAverage(basisFunctions[j]);
            	XTX[j][i] = XTX[i][j];
            }
        }

        // Build XTy - the projection of the dependents random variable on the basis functions.
        double[] XTy = new double[basisFunctions.length];
        for(int i=0; i<basisFunctions.length; i++) {
        	XTy[i] = dependents.getAverage(basisFunctions[i]);
        }

        // Solve X^T X x = X^T y - which gives us the regression coefficients x = linearRegressionParameters
        double[] linearRegressionParameters = LinearAlgebra.solveLinearEquation(XTX, XTy);

        return linearRegressionParameters;
    }

	
    private RandomVariableInterface[] getNonZeroBasisFunctions(RandomVariableInterface[] basisFunctions) {
    	int numberOfNonZeroBasisFunctions = 0;
        for(int indexBasisFunction = 0; indexBasisFunction<basisFunctions.length; indexBasisFunction++) {
        	if(basisFunctions[indexBasisFunction] != null) {
        		numberOfNonZeroBasisFunctions++;
        	}
        }
        
        RandomVariableInterface[] nonZerobasisFunctions = new RandomVariableInterface[numberOfNonZeroBasisFunctions];

    	int indexOfNonZeroBasisFunctions = 0;
        for (RandomVariableInterface basisFunction : basisFunctions) {
            if (basisFunction != null) {
                nonZerobasisFunctions[indexOfNonZeroBasisFunctions] = basisFunction;
                indexOfNonZeroBasisFunctions++;
            }
        }

        return nonZerobasisFunctions;
	}
}
