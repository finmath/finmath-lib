/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 13.08.2004
 */
package net.finmath.montecarlo.conditionalexpectation;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.SingularValueDecomposition;

import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * A service that allows to estimate conditional expectation via regression.
 * 
 * This implementation uses a localization weight derived from the dependent variable.
 * 
 * In oder to estimate the conditional expectation, basis functions have to be specified.
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
 * @version 1.0
 */
public class MonteCarloConditionalExpectationRegressionLocalizedOnDependents extends MonteCarloConditionalExpectationRegression {

	private final double standardDeviations;
	
	/**
	 * Creates a class for conditional expectation estimation.
	 *
	 * @param basisFunctionsEstimator A vector of random variables to be used as basis functions for estimation.
	 * @param basisFunctionsPredictor A vector of random variables to be used as basis functions for prediction.
	 * @param standardDeviations A standard deviation parameter for the weight function.
	 */
	public MonteCarloConditionalExpectationRegressionLocalizedOnDependents(RandomVariable[] basisFunctionsEstimator, RandomVariable[] basisFunctionsPredictor, double standardDeviations) {
		super(basisFunctionsEstimator, basisFunctionsPredictor);
		this.standardDeviations = standardDeviations;
	}

	/**
	 * Creates a class for conditional expectation estimation.
	 *
	 * @param basisFunctionsEstimator A vector of random variables to be used as basis functions for estimation.
	 * @param standardDeviations A standard deviation parameter for the weight function.
	 */
	public MonteCarloConditionalExpectationRegressionLocalizedOnDependents(RandomVariable[] basisFunctionsEstimator, double standardDeviations) {
		super(basisFunctionsEstimator);
		this.standardDeviations = standardDeviations;
	}

	public MonteCarloConditionalExpectationRegressionLocalizedOnDependents() {
		this(null, 4.0);
	}

	/**
	 * Creates a class for conditional expectation estimation.
	 *
	 * @param basisFunctions A vector of random variables to be used as basis functions.
	 */
	public MonteCarloConditionalExpectationRegressionLocalizedOnDependents(RandomVariable[] basisFunctions) {
		this(basisFunctions, 4.0);
	}

	/**
	 * Creates a class for conditional expectation estimation.
	 *
	 * @param basisFunctionsEstimator A vector of random variables to be used as basis functions for estimation.
	 * @param basisFunctionsPredictor A vector of random variables to be used as basis functions for prediction.
	 */
	public MonteCarloConditionalExpectationRegressionLocalizedOnDependents(RandomVariable[] basisFunctionsEstimator, RandomVariable[] basisFunctionsPredictor) {
		this(basisFunctionsEstimator, basisFunctionsPredictor, 4.0);
	}

	/**
	 * Return the solution x of XTX x = XT y for a given y.
	 * @TODO Performance upon repeated call can be optimized by caching XTX.
	 *
	 * @param dependents The sample vector of the random variable y.
	 * @return The solution x of XTX x = XT y.
	 */
	public double[] getLinearRegressionParameters(RandomVariable dependents) {

		RandomVariable[] basisFunctions = basisFunctionsEstimator.getBasisFunctions().clone();

		RandomVariable localizerWeights = dependents.squared().sub(Math.pow(dependents.getStandardDeviation()*standardDeviations,2.0)).choose(new Scalar(0.0), new Scalar(1.0));		
		dependents = dependents.mult(localizerWeights);
		for(int i=0; i<basisFunctions.length; i++) {
			basisFunctions[i] = basisFunctions[i].mult(localizerWeights);
		}
		
		DecompositionSolver solver;
		// Build XTX - the symmetric matrix consisting of the scalar products of the basis functions.
		double[][] XTX = new double[basisFunctions.length][basisFunctions.length];
				for(int i=0; i<basisFunctions.length; i++) {
					for(int j=i; j<basisFunctions.length; j++) {
						XTX[i][j] = basisFunctions[i].mult(basisFunctions[j]).getAverage();	// Scalar product
						XTX[j][i] = XTX[i][j];												// Symmetric matrix
					}
				}

				solver = new SingularValueDecomposition(new Array2DRowRealMatrix(XTX, false)).getSolver();

		// Build XTy - the projection of the dependents random variable on the basis functions.
		double[] XTy = new double[basisFunctions.length];
		for(int i=0; i<basisFunctions.length; i++) {
			XTy[i] = dependents.mult(basisFunctions[i]).getAverage();				// Scalar product
		}

		// Solve X^T X x = X^T y - which gives us the regression coefficients x = linearRegressionParameters
		double[] linearRegressionParameters = solver.solve(new ArrayRealVector(XTy)).toArray();

		return linearRegressionParameters;
	}
}
