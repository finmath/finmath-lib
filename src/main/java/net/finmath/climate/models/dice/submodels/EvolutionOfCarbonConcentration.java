package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;

import net.finmath.functions.LinearAlgebra;

/**
 * The evolution of the carbon concentration M with a given emission E.
 * \(
 * 	M(t_{i+1}) = \Phi M(t_{i}) + unitConversion * E(t_{i}) \Delta t_{i}
 * \)
 *
 * Note: the emission are in GtCO2/year while the carbon concentration is in GtC.
 *
 * Unit conversions
 * <ul>
 * 	<li>1 t Carbon = 3.666 t CO2</li>
 * </ul>
 *
 * Note: The function depends on the time step size.
 * TODO Fix time stepping
 *
 * @author Christian Fries
 */
public class EvolutionOfCarbonConcentration implements BiFunction<CarbonConcentration3DScalar, Double, CarbonConcentration3DScalar> {

	private static double conversionGtCarbonperGtCO2 = 3.0/11.0;

	private static double[][] transitionMatrix5YDefault;
	private static double[][] transitionMatrix1YDefault;
	// Original transition matrix is a 5Y transition matrix
	static {
		final double b12 = 0.12;		// scale
		final double b23 = 0.007;		// scale
		final double mateq = 588;
		final double mueq = 360;
		final double mleq = 1720;

		final double zeta11 = 1 - b12;  //b11
		final double zeta21 = b12;
		final double zeta12 = b12*(mateq/mueq);
		final double zeta22 = 1 - zeta12 - b23;
		final double zeta32 = b23;
		final double zeta23 = b23*(mueq/mleq);
		final double zeta33 = 1 - b23;

		transitionMatrix5YDefault = new double[][] { new double[] { zeta11, zeta12, 0.0 }, new double[] { zeta21, zeta22, zeta23 }, new double[] { 0.0, zeta32, zeta33 } };
		
		transitionMatrix1YDefault = new double[][] {
			new double[] { 0.97221,   0.0278745, 0.0 },
			new double[] { 0.0455284, 0.95293, 0.0 },
			new double[] { 0.00145916, 0.000239206, 0.998596 }
		};
	}

	private final double timeStep;
	private final double[][] transitionMatrix;		// phi in [i][j] (i = row, j = column)

	public EvolutionOfCarbonConcentration(double timeStep, double[][] transitionMatrix) {
		super();
		this.timeStep = timeStep;
		this.transitionMatrix = transitionMatrix;
	}

	public EvolutionOfCarbonConcentration(double timeStep) {
		// Parameters from original model
		this(timeStep, timeStep == 5 ? transitionMatrix5YDefault : matrixPow(transitionMatrix5YDefault, timeStep/5.0));
	}

	@Override
	/**
	 * Update CarbonConcentration over one time step with a given emission.
	 *
	 * @param carbonConcentration The CarbonConcentration in time \( t_{i} \)
	 * @param emissions The emissions in GtCO2 / year.
	 */
	public CarbonConcentration3DScalar apply(CarbonConcentration3DScalar carbonConcentration, Double emissions) {
		final double[] carbonConcentrationNext = LinearAlgebra.multMatrixVector(transitionMatrix, carbonConcentration.getAsDoubleArray());

		// Add emissions
		carbonConcentrationNext[0] += emissions * timeStep * conversionGtCarbonperGtCO2;

		return new CarbonConcentration3DScalar(carbonConcentrationNext);
	}
	
	private static double[][] matrixPow(double[][] matrix, double exponent) {
		return matrixExp(matrixLog(new Array2DRowRealMatrix(matrix)).scalarMultiply(exponent)).getData();
	}

	/*
	 * Thre are better ways doing this - but this here is sufficient for our purpose.
	 */

	private static RealMatrix matrixExp(RealMatrix matrix) {
		RealMatrix exp = MatrixUtils.createRealIdentityMatrix(matrix.getRowDimension());;
		double factor = 1.0;
		for(int k=1; k<10; k++) {
			factor = factor * k;
			exp = exp.add(matrix.power(k).scalarMultiply(1.0/factor));
		}
		return exp;
	}

	private static RealMatrix matrixLog(RealMatrix matrix) {
		RealMatrix m = matrix.subtract(MatrixUtils.createRealIdentityMatrix(matrix.getRowDimension()));
		RealMatrix log = m.copy();
		for(int k=2; k<10; k++) {
			log = log.add(m.power(k).scalarMultiply((k%2 == 0 ? -1.0 : 1.0)/k));
		}
		return log;
	}
}