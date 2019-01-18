package net.finmath.optimizer;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;


/**
 * @author Mario Viehmann
 */
public class StochasticPathwiseLevenbergMarquardtTestInverse {

	@ Test
	public void testInverse() throws SolverException{
		// Create simple matrix with 2 paths
		RandomVariable[][] matrix = new RandomVariableFromDoubleArray[][] {{new RandomVariableFromDoubleArray(0.0,new double[]{1.0,1.0}), new RandomVariableFromDoubleArray(0.0,new double[]{2.0,2.0})}, {new RandomVariableFromDoubleArray(0.0,new double[]{3.0,3.0}), new RandomVariableFromDoubleArray(0.0,new double[]{4.0,4.0})}};
		//RandomVariable[][] matrix = new RandomVariableFromDoubleArray[][] {{new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(2.0)}, {new RandomVariableFromDoubleArray(3.0), new RandomVariableFromDoubleArray(4.0)}};

		RandomVariable[][] inverse = getInverseMatrix(matrix);
		RandomVariable[][] product =  multiply(matrix, inverse);
		Assert.assertEquals(product[0][0].getAverage(), 1.0, 1E-6);
		Assert.assertEquals(product[0][1].getAverage(), 0.0, 1E-6);
		Assert.assertEquals(product[1][0].getAverage(), 0.0, 1E-6);
		Assert.assertEquals(product[1][1].getAverage(), 1.0, 1E-6);
	}


	public RandomVariable[][] getInverseMatrix(RandomVariable[][] matrix) throws SolverException{
		int numberOfParameters = matrix.length*matrix.length;
		RandomVariable[] parameterSteps = new RandomVariable[numberOfParameters];
		RandomVariable[] weights = new RandomVariable[numberOfParameters];
		RandomVariable[] targetValues = new RandomVariable[numberOfParameters];
		RandomVariable[] initialParameters = new RandomVariable[numberOfParameters];
		Arrays.fill(parameterSteps, new RandomVariableFromDoubleArray(1E-8));
		Arrays.fill(weights, new RandomVariableFromDoubleArray(1.0));
		// set identity matrix as target values
		int rowIndex = 0;
		for(int i=0;i<targetValues.length;i++) {
			if(i-rowIndex*matrix.length==rowIndex) {
				targetValues[i]= new RandomVariableFromDoubleArray(1.0);
				rowIndex++;
			} else {
				targetValues[i]= new RandomVariableFromDoubleArray(0.0);
			}
			// set Initial parameters
			initialParameters[i]= targetValues[i];
		}

		int maxIteration = 1000000;

		StochasticPathwiseLevenbergMarquardt optimizer = new StochasticPathwiseLevenbergMarquardt(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void setValues(RandomVariable[] parameters, RandomVariable[] values) {

				for(int i=0;i<values.length;i++){
					int rowIndex = i/matrix.length;
					int columnIndex = i % matrix.length;
					values[i] = new RandomVariableFromDoubleArray(0.0);
					for(int j=0;j<matrix.length;j++){
						RandomVariable factor = matrix[rowIndex][j]==null ? new RandomVariableFromDoubleArray(0.0) : matrix[rowIndex][j];
						values[i] = values[i].add(factor.mult(parameters[columnIndex+j*matrix.length]));
					}
				}
			}
		};

		// Set solver parameters
		optimizer.run();
		RandomVariable[] bestParameters = optimizer.getBestFitParameters();
		// Wrap to matrix
		RandomVariable[][] inverse = new RandomVariable[matrix.length][matrix.length];
		for(int i=0;i<bestParameters.length;i++){
			rowIndex = i/matrix.length;
			int columnIndex = i % matrix.length;
			inverse[rowIndex][columnIndex]=bestParameters[i];
		}
		return inverse;
	}

	public RandomVariable[][] multiply(RandomVariable[][] matrixA, RandomVariable[][] matrixB){
		RandomVariable[][] productAB = new RandomVariable[matrixA.length][matrixB.length];
		for(int i=0;i<matrixA.length;i++) {
			for(int j=0; j<matrixB.length; j++) {
				productAB[i][j] = new RandomVariableFromDoubleArray(0.0);
				for(int k=0;k<matrixB.length;k++) {
					if(matrixA[i][k] != null && matrixB[k][j] != null) {
						productAB[i][j] = productAB[i][j].add(matrixA[i][k].mult(matrixB[k][j]));
					}
				}
			}
		}
		return productAB;
	}
}
