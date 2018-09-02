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

import net.finmath.stochastic.ConditionalExpectationEstimatorInterface;
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
public class MonteCarloConditionalExpectationRegression implements ConditionalExpectationEstimatorInterface {

	/**
	 * Interface for objects specifying regression basis functions (a vector of random variables).
	 *
	 * @author Christian Fries
	 */
	public interface RegressionBasisFunctions {
		RandomVariableInterface[] getBasisFunctions();
	}

	/**
	 * Wrapper to an array of RandomVariableInterface[] implementing RegressionBasisFunctions
	 * @author Christian Fries
	 */
	public class RegressionBasisFunctionsGiven implements RegressionBasisFunctions {
		private final RandomVariableInterface[] basisFunctions;

		public RegressionBasisFunctionsGiven(RandomVariableInterface[] basisFunctions) {
			super();
			this.basisFunctions = basisFunctions;
		}

		@Override
		public RandomVariableInterface[] getBasisFunctions() {
			return basisFunctions;
		}
	}


	private RegressionBasisFunctions basisFunctionsEstimator		= null;
	private RegressionBasisFunctions basisFunctionsPredictor		= null;


	private transient DecompositionSolver solver;
	private final transient Object solverLock;

	public MonteCarloConditionalExpectationRegression() {
		super();
		solverLock = new Object();	// Lock for LazyInit of solver.
	}

	/**
	 * Creates a class for conditional expectation estimation.
	 *
	 * @param basisFunctions A vector of random variables to be used as basis functions.
	 */
	public MonteCarloConditionalExpectationRegression(RandomVariableInterface[] basisFunctions) {
		this();
		this.basisFunctionsEstimator = new RegressionBasisFunctionsGiven(getNonZeroBasisFunctions(basisFunctions));
		this.basisFunctionsPredictor = basisFunctionsEstimator;
	}

	/**
	 * Creates a class for conditional expectation estimation.
	 *
	 * @param basisFunctionsEstimator A vector of random variables to be used as basis functions for estimation.
	 * @param basisFunctionsPredictor A vector of random variables to be used as basis functions for prediction.
	 */
	public MonteCarloConditionalExpectationRegression(RandomVariableInterface[] basisFunctionsEstimator, RandomVariableInterface[] basisFunctionsPredictor) {
		this();
		this.basisFunctionsEstimator = new RegressionBasisFunctionsGiven(getNonZeroBasisFunctions(basisFunctionsEstimator));
		this.basisFunctionsPredictor = new RegressionBasisFunctionsGiven(getNonZeroBasisFunctions(basisFunctionsPredictor));
	}

	@Override
	public RandomVariableInterface getConditionalExpectation(RandomVariableInterface randomVariable) {
		// Get regression parameters x as the solution of XTX x = XT y
		double[] linearRegressionParameters = getLinearRegressionParameters(randomVariable);

		// Calculate estimate, i.e. X x
		RandomVariableInterface[] basisFunctions = this.basisFunctionsPredictor.getBasisFunctions();
		RandomVariableInterface conditionalExpectation = basisFunctions[0].mult(linearRegressionParameters[0]);
		for(int i=1; i<basisFunctions.length; i++) {
			conditionalExpectation = conditionalExpectation.addProduct(basisFunctions[i], linearRegressionParameters[i]);
		}

		return conditionalExpectation;
	}

	/**
	 * Return the solution x of XTX x = XT y for a given y.
	 * @TODO Performance upon repeated call can be optimized by caching XTX.
	 *
	 * @param dependents The sample vector of the random variable y.
	 * @return The solution x of XTX x = XT y.
	 */
	public double[] getLinearRegressionParameters(RandomVariableInterface dependents) {

		RandomVariableInterface[] basisFunctions = basisFunctionsEstimator.getBasisFunctions();

		synchronized (solverLock) {
			if(solver == null) {
				// Build XTX - the symmetric matrix consisting of the scalar products of the basis functions.
				double[][] XTX = new double[basisFunctions.length][basisFunctions.length];
				for(int i=0; i<basisFunctions.length; i++) {
					for(int j=i; j<basisFunctions.length; j++) {
						XTX[i][j] = basisFunctions[i].mult(basisFunctions[j]).getAverage();	// Scalar product
						XTX[j][i] = XTX[i][j];												// Symmetric matrix
					}
				}

				solver = new SingularValueDecomposition(new Array2DRowRealMatrix(XTX, false)).getSolver();
			}
		}

		// Build XTy - the projection of the dependents random variable on the basis functions.
		double[] XTy = new double[basisFunctions.length];
		for(int i=0; i<basisFunctions.length; i++) {
			XTy[i] = dependents.mult(basisFunctions[i]).getAverage();				// Scalar product
		}

		// Solve X^T X x = X^T y - which gives us the regression coefficients x = linearRegressionParameters
		double[] linearRegressionParameters = solver.solve(new ArrayRealVector(XTy)).toArray();

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
