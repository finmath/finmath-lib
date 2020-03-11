/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.marketdata.model.curves.locallinearregression;

import java.time.LocalDate;

import org.apache.commons.math3.distribution.AbstractRealDistribution;
import org.apache.commons.math3.distribution.CauchyDistribution;
import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;

import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;

/**
 * This class implements the method of local linear regression with discrete kernel function, see see https://ssrn.com/abstract=3073942
 *
 * In particular it represents the implementation of proposition 2 and 3 of the paper.
 *
 * This class allows choosing between three different kernel functions, i.e. a normal, a Laplace or a Cauchy kernel.
 *
 * For the kernel types provided see {@link net.finmath.marketdata.model.curves.locallinearregression.CurveEstimation.Distribution}.
 *
 * The resulting curve is piecewise linear. That means, only the knot points of the curve are computed in this algorithm.
 * The final curve is then provided with linear interpolation of the knot points,
 * see {@link net.finmath.marketdata.model.curves.CurveInterpolation}.
 *
 * @author Moritz Scherrmann
 * @author Christian Fries
 * @version 1.0
 */
public class CurveEstimation{

	/**
	 * Possible kernel types.
	 */
	public enum Distribution {
		NORMAL,
		LAPLACE,
		CAUCHY
	}

	private final LocalDate referenceDate;
	private final double bandwidth;
	private final double[] independentValues;
	private final double[] dependentValues;
	private final Partition partition;
	private final DiscountCurveInterpolation regressionCurve=null;
	private AbstractRealDistribution kernel;

	/**
	 * Creates a curve estimation object.
	 *
	 * @param referenceDate The reference date for the resulting regression curve, i.e., the date which defined t=0.
	 * @param bandwidth The bandwidth parameter of the regression.
	 * @param independentValues The realization of a random variable X.
	 * @param dependentValues The realization of a random variable Y.
	 * @param partitionValues The values to create a partition. It is important that min(partition) &le; min(X) and max(partition) &ge; max(X).
	 * @param weight The weight needed to create a partition.
	 * @param distribution The kernel type.
	 */
	public CurveEstimation(
			final LocalDate referenceDate,
			final double bandwidth,
			final double[] independentValues,
			final double[] dependentValues,
			final double[] partitionValues,
			final double weight,
			final Distribution distribution){
		this.referenceDate = referenceDate;
		this.bandwidth = bandwidth;
		this.independentValues = independentValues;
		this.dependentValues = dependentValues;
		partition = new Partition(partitionValues.clone(), weight);


		switch(distribution) {
		case LAPLACE:
			kernel=new LaplaceDistribution(0,1);
			break;
		case CAUCHY:
			kernel=new CauchyDistribution();
			break;
		case NORMAL:
		default:
			kernel=new NormalDistribution();
			break;
		}

	}

	/**
	 * Creates a curve estimation object with a normal kernel.
	 *
	 * @param referenceDate The reference date for the resulting regression curve, i.e., the date which defined t=0.
	 * @param bandwidth The bandwidth parameter of the regression.
	 * @param independentValues The realization of a random variable X.
	 * @param dependentValues The realization of a random variable Y.
	 * @param partitionValues The values to create a partition. It is important that min(partition) &le; min(X) and max(partition) &ge; max(X).
	 * @param weight The weight needed to create a partition.
	 */
	public CurveEstimation(
			final LocalDate referenceDate,
			final double bandwidth,
			final double[] independentValues,
			final double[] dependentValues,
			final double[] partitionValues,
			final double weight) {
		this(referenceDate,bandwidth,independentValues,dependentValues,partitionValues,weight,Distribution.NORMAL);
	}

	/**
	 * Returns the curve resulting from the local linear regression with discrete kernel.
	 *
	 * @return The regression curve.
	 */
	public Curve getRegressionCurve(){
		// @TODO Add threadsafe lazy init.
		if(regressionCurve !=null) {
			return regressionCurve;
		}
		final DoubleMatrix a = solveEquationSystem();
		final double[] curvePoints=new double[partition.getLength()];
		curvePoints[0]=a.get(0);
		for(int i=1;i<curvePoints.length;i++) {
			curvePoints[i]=curvePoints[i-1]+a.get(i)*(partition.getIntervalLength(i-1));
		}
		return new CurveInterpolation(
				"RegressionCurve",
				referenceDate,
				CurveInterpolation.InterpolationMethod.LINEAR,
				CurveInterpolation.ExtrapolationMethod.CONSTANT,
				CurveInterpolation.InterpolationEntity.VALUE,
				partition.getPoints(),
				curvePoints);
	}


	/**
	 * Returns the vector a from proposition 3 in Beier/Fries (2017).
	 *
	 * @return The vector a.
	 */
	private DoubleMatrix solveEquationSystem(){
		DoubleMatrix R=new DoubleMatrix(partition.getLength());
		final DoubleMatrix M=new DoubleMatrix(partition.getLength(),partition.getLength());
		final DoubleMatrix partitionAsVector=new DoubleMatrix(partition.getPoints());
		final DoubleMatrix shiftedPartition=new DoubleMatrix(partition.getLength());
		for(int j=1; j<shiftedPartition.length;j++) {
			shiftedPartition.put(j, partition.getPoint(j-1));
		}
		final DoubleMatrix partitionIncrements= partitionAsVector.sub(shiftedPartition).put(0,1);
		final DoubleMatrix kernelValues=new DoubleMatrix(partition.getLength()-1);
		DoubleMatrix M1_1= new DoubleMatrix(1);
		DoubleMatrix MFirstCol= new DoubleMatrix(partition.getLength()-1);
		DoubleMatrix MSubDiagonal= new DoubleMatrix(partition.getLength()-1);
		DoubleMatrix MSubMatrix= new DoubleMatrix(partition.getLength()-1,partition.getLength()-1);
		DoubleMatrix MSubMatrixSum= new DoubleMatrix(partition.getLength()-1);

		for(int i=0;i<independentValues.length;i++){

			final DoubleMatrix oneZeroVector= new DoubleMatrix(partition.getLength());
			DoubleMatrix kernelSum= new DoubleMatrix(partition.getLength());
			final DoubleMatrix shiftedKernelVector= new DoubleMatrix(partition.getLength());


			for(int r=0;r<partition.getLength()-1;r++){
				oneZeroVector.put(r, 1);
				kernelValues.put( r,kernel.density((partition.getIntervalReferencePoint(r)-independentValues[i])/bandwidth));
				shiftedKernelVector.put(r+1,kernelValues.get( r) );
				kernelSum=kernelSum.add(oneZeroVector.mmul(kernelValues.get(r)));
			}

			R=R.add(shiftedPartition.neg().add(independentValues[i]).mul(shiftedKernelVector)
					.add(partitionIncrements.mul(kernelSum)).mul(dependentValues[i]));

			M1_1=M1_1.add( kernelSum.get(0));

			MFirstCol=MFirstCol.add(
					partitionAsVector.getRange(0,partitionAsVector.length-1).neg().add(independentValues[i])
					.mul(kernelValues).add(
							partitionIncrements.getRange(1,partitionAsVector.length)
							.mul(kernelSum.getRange(1, kernelSum.length))));

			MSubDiagonal=MSubDiagonal.add(
					partitionAsVector.getRange(0,partitionAsVector.length-1).neg().add(independentValues[i])
					.mul(partitionAsVector.getRange(0,partitionAsVector.length-1).neg().add(independentValues[i]))
					.mul(kernelValues).add(
							partitionIncrements.getRange(1,partitionAsVector.length)
							.mul(partitionIncrements.getRange(1,partitionAsVector.length)
									.mul(kernelSum.getRange(1, kernelSum.length)))));

			MSubMatrixSum=MSubMatrixSum.add(
					partitionAsVector.getRange(0, partitionAsVector.length-1).neg().add(independentValues[i])
					.mul(kernelValues).add(
							partitionIncrements.getRange(1, partitionIncrements.length)
							.mul(kernelSum.getRange(1, kernelSum.length))));
		}

		final DoubleMatrix partitionIncrementMatrix= new DoubleMatrix(partition.getLength()-1,partition.getLength()-1);
		final DoubleMatrix matrixDefine= DoubleMatrix.ones(partition.getLength()-1);
		for(int m=0;m<matrixDefine.length-1;m++) {
			matrixDefine.put(m, 0);
			partitionIncrementMatrix.putColumn(m, matrixDefine.mul(partitionIncrements.get(m+1)));
		}

		MSubMatrix=partitionIncrementMatrix.mulColumnVector(MSubMatrixSum);
		MSubMatrix=MSubMatrix.add(MSubMatrix.transpose()).add(DoubleMatrix.diag(MSubDiagonal));

		final int[] rowColIndex =new int[partition.getLength()-1];
		for(int n=0;n<rowColIndex.length;n++) {
			rowColIndex[n]=n+1;
		}

		M.put(0,0,M1_1.get(0));
		M.put(rowColIndex, 0, MFirstCol);
		M.put(0, rowColIndex, MFirstCol.transpose());
		M.put(rowColIndex, rowColIndex, MSubMatrix);

		return Solve.solve(M, R);
	}
}

