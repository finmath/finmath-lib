/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 13.08.2004
 */
package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.functions.LinearAlgebra;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
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

	private ImmutableRandomVariableInterface[]    basisFunctionsEstimator		= null;
	private ImmutableRandomVariableInterface[]    basisFunctionsPredictor		= null;
    
    /**
     * Creates a class for conditional expectation estimation.
     * 
     * @param basisFunctions A vector of random variables to be used as basis functions.
     */
    public MonteCarloConditionalExpectationRegression(ImmutableRandomVariableInterface[] basisFunctions) {
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
    public MonteCarloConditionalExpectationRegression(ImmutableRandomVariableInterface[] basisFunctionsEstimator, ImmutableRandomVariableInterface[] basisFunctionsPredictor) {
        super();
        this.basisFunctionsEstimator = basisFunctionsEstimator;
        this.basisFunctionsPredictor = basisFunctionsPredictor;
    }

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectation#getConditionalExpectation(net.finmath.stochastic.ImmutableRandomVariableInterface)
     */
    public RandomVariableInterface getConditionalExpectation(ImmutableRandomVariableInterface randomVariable) {
    	double[] linearRegressionParameters = getLinearRegressionParameters(randomVariable);

    	// Calculate estimate
        ImmutableRandomVariableInterface[] basisFunctions = getNonZeroBasisFunctions(basisFunctionsPredictor);

        RandomVariableInterface conditionalExpectation = new RandomVariable(0.0);
        for(int i=0; i<basisFunctions.length; i++) {
            conditionalExpectation = conditionalExpectation.addProduct(basisFunctions[i], linearRegressionParameters[i]);
        }

        return conditionalExpectation;
    }
    
    
	public double[] getLinearRegressionParameters(ImmutableRandomVariableInterface dependents) {        

        // Build XTX - the symmetric matrix consisting of the scalar products of the basis functions.
        ImmutableRandomVariableInterface[] basisFunctions = getNonZeroBasisFunctions(basisFunctionsEstimator);
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

	
    private ImmutableRandomVariableInterface[] getNonZeroBasisFunctions(ImmutableRandomVariableInterface[] basisFunctions) {
    	int numberOfNonZeroBasisFunctions = 0;
        for(int indexBasisFunction = 0; indexBasisFunction<basisFunctions.length; indexBasisFunction++) {
        	if(basisFunctions[indexBasisFunction] != null) {
        		numberOfNonZeroBasisFunctions++;
        	}
        }
        
        ImmutableRandomVariableInterface[] nonZerobasisFunctions = new ImmutableRandomVariableInterface[numberOfNonZeroBasisFunctions];

    	int indexOfNonZeroBasisFunctions = 0;
        for (ImmutableRandomVariableInterface basisFunction : basisFunctions) {
            if (basisFunction != null) {
                nonZerobasisFunctions[indexOfNonZeroBasisFunctions] = basisFunction;
                indexOfNonZeroBasisFunctions++;
            }
        }

        return nonZerobasisFunctions;
	}
}
