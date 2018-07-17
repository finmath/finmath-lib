package net.finmath.optimizer;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;


/**
 * @author Mario Viehmann
 */
public class StochasticLevenbergMarquardtTestInverse {

	@ Test
	public void testInverse() throws SolverException{
		// Create simple matrix with 2 paths
		RandomVariableInterface[][] matrix = new RandomVariable[][] {{new RandomVariable(0.0,new double[]{1.0,1.0}), new RandomVariable(0.0,new double[]{2.0,2.0})}, {new RandomVariable(0.0,new double[]{3.0,3.0}), new RandomVariable(0.0,new double[]{4.0,4.0})}};
		//RandomVariableInterface[][] matrix = new RandomVariable[][] {{new RandomVariable(1.0), new RandomVariable(2.0)}, {new RandomVariable(3.0), new RandomVariable(4.0)}};

		RandomVariableInterface[][] inverse = getInverseMatrix(matrix);
		RandomVariableInterface[][] product =  multiply(matrix, inverse);
		Assert.assertEquals(product[0][0].getAverage(), 1.0, 1E-6);
		Assert.assertEquals(product[0][1].getAverage(), 0.0, 1E-6);
		Assert.assertEquals(product[1][0].getAverage(), 0.0, 1E-6);
		Assert.assertEquals(product[1][1].getAverage(), 1.0, 1E-6);
	}


	public RandomVariableInterface[][] getInverseMatrix(RandomVariableInterface[][] matrix) throws SolverException{
		int numberOfParameters = matrix.length*matrix.length;
		RandomVariableInterface[] parameterSteps = new RandomVariableInterface[numberOfParameters];
		RandomVariableInterface[] weights = new RandomVariableInterface[numberOfParameters];
		RandomVariableInterface[] targetValues = new RandomVariableInterface[numberOfParameters];
		RandomVariableInterface[] initialParameters = new RandomVariableInterface[numberOfParameters];
		Arrays.fill(parameterSteps, new RandomVariable(1E-8));
		Arrays.fill(weights, new RandomVariable(1.0));
		// set identity matrix as target values
		int rowIndex = 0;
		for(int i=0;i<targetValues.length;i++) {
			if(i-rowIndex*matrix.length==rowIndex) {
				targetValues[i]= new RandomVariable(1.0);
				rowIndex++;
			} else {
				targetValues[i]= new RandomVariable(0.0);
			}
			// set Initial parameters
			initialParameters[i]= targetValues[i];
		}

		int maxIteration = 1000000;

		StochasticPathwiseLevenbergMarquardtAD optimizer = new StochasticPathwiseLevenbergMarquardtAD(initialParameters, targetValues, weights, parameterSteps, maxIteration, null, null) {
			private static final long serialVersionUID = 1L;

			@Override
			public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) {

				for(int i=0;i<values.length;i++){
					int rowIndex = i/matrix.length;
					int columnIndex = i % matrix.length;
					values[i] = new RandomVariable(0.0);
					for(int j=0;j<matrix.length;j++){
						RandomVariableInterface factor = matrix[rowIndex][j]==null ? new RandomVariable(0.0) : matrix[rowIndex][j];
						values[i] = values[i].add(factor.mult(parameters[columnIndex+j*matrix.length]));
					}
				}
			}
		};

		// Set solver parameters
		optimizer.run();
		RandomVariableInterface[] bestParameters = optimizer.getBestFitParameters();
		// Wrap to matrix
		RandomVariableInterface[][] inverse = new RandomVariableInterface[matrix.length][matrix.length];
		for(int i=0;i<bestParameters.length;i++){
			rowIndex = i/matrix.length;
			int columnIndex = i % matrix.length;
			inverse[rowIndex][columnIndex]=bestParameters[i];
		}
		return inverse;
	}

	public RandomVariableInterface[][] multiply(RandomVariableInterface[][] A,RandomVariableInterface[][] B){
		RandomVariableInterface[][] AB = new RandomVariableInterface[A.length][B.length];
		RandomVariableInterface ABproduct;
		for(int i=0;i<A.length;i++){
			for(int j=0; j<B.length; j++){
				AB[i][j] = new RandomVariable(0.0);
				for(int k=0;k<B.length;k++) {
					if(A[i][k]==null || B[k][j]==null) {ABproduct = new RandomVariable(0.0);}
					else {ABproduct = A[i][k].mult(B[k][j]);}
					AB[i][j]=AB[i][j].add(ABproduct);
				}
			}
		}
		return AB;
	}
}