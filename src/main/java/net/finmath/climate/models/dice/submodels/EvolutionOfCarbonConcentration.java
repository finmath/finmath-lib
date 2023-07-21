package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

import net.finmath.functions.LinearAlgebra;
import net.finmath.time.TimeDiscretization;
import net.finmath.util.Cached;
import net.finmath.util.TriFunction;

/**
 * The evolution of the carbon concentration M with a given emission E \( \mathrm{d}M(t) = \left( \Gamma_{M} M(t) + E(t) \right) \mathrm{d}t \).
 *
 * The unit of \( M \) is GtC (Gigatons of Carbon).
 *
 * The evolution is modelled as \( \mathrm{d}M(t) = \left( \Gamma_{M} M(t) + E(t) \right) \mathrm{d}t \right).
 * With the given {@link TimeDiscretization} it is approximated via an Euler-step
 * \(
 * 	M(t_{i+1}) = \Phi M(t_{i}) + unitConversion \cdot E(t_{i}) \Delta t_{i}
 * \)
 * where \( \Phi = (1 + \Gamma_{M} \Delta t_{i}) \).
 *
 * Note: the emission E are in GtCO2/year while the carbon concentration is in M GtC.
 *
 * Unit conversions
 * <ul>
 * 	<li>1 t Carbon = 3.666 t CO2</li>
 * </ul>
 *
 * @author Christian Fries
 */
public class EvolutionOfCarbonConcentration implements TriFunction<Integer, CarbonConcentration3DScalar, Double, CarbonConcentration3DScalar> {

	private static double conversionGtCperGtCO2 = 1/3.666;

	private static double[][] transitionMatrix5YDefault;
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
		final double zeta33 = 1 - zeta23;

		transitionMatrix5YDefault = new double[][] { new double[] { zeta11, zeta12, 0.0 }, new double[] { zeta21, zeta22, zeta23 }, new double[] { 0.0, zeta32, zeta33 } };
	}

	private final TimeDiscretization timeDiscretization;
	private final Function<Integer, double[][]> transitionMatrices;		// phi in [i][j] (i = row, j = column)

	public EvolutionOfCarbonConcentration(TimeDiscretization timeDiscretization, Function<Integer, double[][]> transitionMatrices) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.transitionMatrices = transitionMatrices;
	}

	public EvolutionOfCarbonConcentration(TimeDiscretization timeDiscretization) {
		Function<Integer, Double> timeSteps = ((Integer timeIndex) -> { return timeDiscretization.getTimeStep(timeIndex); });
		this.timeDiscretization = timeDiscretization;
		transitionMatrices = timeSteps.andThen(Cached.<Double, double[][]>of(timeStep -> timeStep == 5.0 ? transitionMatrix5YDefault : LinearAlgebra.matrixPow(transitionMatrix5YDefault, (Double)timeStep/5.0)));
	}

	/**
	 * Update CarbonConcentration over one time step with a given emission.
	 *
	 * @param carbonConcentration The CarbonConcentration in time \( t_{i} \)
	 * @param emissions The emissions in GtCO2 / year.
	 */
	public CarbonConcentration3DScalar apply(Integer timeIndex, CarbonConcentration3DScalar carbonConcentration, Double emissions) {
		final double timeStep = timeDiscretization.getTimeStep(timeIndex);
		final double[] carbonConcentrationNext = LinearAlgebra.multMatrixVector(transitionMatrices.apply(timeIndex), carbonConcentration.getAsDoubleArray());

		// Add emissions
		carbonConcentrationNext[0] += emissions * timeStep * conversionGtCperGtCO2;

		return new CarbonConcentration3DScalar(carbonConcentrationNext);
	}

	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}
}