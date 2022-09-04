package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;

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
 * </lui>
 *
 * Note: The function depends on the time step size.
 * TODO Fix time stepping
 *
 * @author Christian Fries
 */
public class EvolutionOfCarbonConcentration implements BiFunction<CarbonConcentration, Double, CarbonConcentration> {

	private static double conversionGtCarbonperGtCO2 = 3.0/11.0;

	private static double[][] transitionMatrixDefault;
	static {
		final double b12 = 0.12;		// scale
		final double b23 = 0.007;		// scale
		final double mateq = 588;
		final double mueq = 360;
		final double mleq = 1720;

		final double zeta11 = 1 - b12;
		final double zeta21 = b12;
		final double zeta12 = (mateq/mueq)*zeta21;
		final double zeta22 = 1 - zeta12 - b23;
		final double zeta32 = b23;
		final double zeta23 = zeta32*(mueq/mleq);
		final double zeta33 = 1 - zeta23;

		transitionMatrixDefault = new double[][] { new double[] { zeta11, zeta12, 0.0 }, new double[] { zeta21, zeta22, zeta23 }, new double[] { 0.0, zeta32, zeta33 } };
	}

	private final double timeStepSize;				// time step in the original model (should become a parameter)
	private final double[][] transitionMatrix;		// phi in [i][j] (i = row, j = column)

	public EvolutionOfCarbonConcentration(double timeStepSize, double[][] transitionMatrix) {
		super();
		this.timeStepSize = timeStepSize;
		this.transitionMatrix = transitionMatrix;
	}

	public EvolutionOfCarbonConcentration() {
		// Parameters from original model
		this(5.0, transitionMatrixDefault);
	}

	@Override
	/**
	 * Update CarbonConcentration over one time step with a given emission.
	 *
	 * @param carbonConcentration The CarbonConcentration in time \( t_{i} \)
	 * @param emissions The emissions in GtCO2 / year.
	 */
	public CarbonConcentration apply(CarbonConcentration carbonConcentration, Double emissions) {
		final double[] carbonConcentrationNext = LinearAlgebra.multMatrixVector(transitionMatrix, carbonConcentration.getAsDoubleArray());

		// Add emissions
		carbonConcentrationNext[0] += emissions * timeStepSize * conversionGtCarbonperGtCO2;

		return new CarbonConcentration(carbonConcentrationNext);
	}
}
