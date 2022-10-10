package net.finmath.climate.models.dice.submodels;

import java.util.function.BiFunction;
import java.util.stream.IntStream;

import net.finmath.functions.LinearAlgebra;

/**
 *
 * The evolution of the temperature.
 * \(
 * 	T(t_{i+1}) = \Phi T(t_{i}) + (forcingToTemp \cdot (forcing, 0, 0)
 * \)
 *
 * @author Christian Fries
 */
public class EvolutionOfTemperature implements BiFunction<Temperature2DScalar, Double, Temperature2DScalar> {

	private static double c1 = 0.1005;		// sometimes called xi1

	private static double[][] transitionMatrixDefault;
	static {
		final double fco22x = 3.6813;         // Forcings of equilibrium CO2 doubling (Wm-2)
		final double t2xco2 = 3.1;			// Equilibrium temp impact (°C per doubling CO2)
		final double c3 = 0.088;				// Transfer coefficient upper to lower stratum
		final double c4 = 0.025;

		final double phi11 = 1-c1*((fco22x/t2xco2) + c3);
		final double phi12 = c1*c3;
		final double phi21 = c4;
		final double phi22 = 1-c4;

		transitionMatrixDefault = new double[][] { new double[] { phi11, phi12 }, new double[] { phi21, phi22 } };
	}

	private final double timeStep;
	private final double[][] transitionMatrix;		// phi in [i][j] (i = row, j = column)
	private final double forcingToTemp;

	/**
	 * @param timeStep The time step.
	 * @param transitionMatrix Transition matrix \( \Phi \)
	 * @param forcingToTemp The scaling coefficient for the external forcing.
	 */
	public EvolutionOfTemperature(double timeStep, double[][] transitionMatrix, double forcingToTemp) {
		super();
		this.timeStep = timeStep;
		this.transitionMatrix = transitionMatrix;
		this.forcingToTemp = forcingToTemp;
	}

	public EvolutionOfTemperature(double timeStep) {
		this(timeStep, transitionMatrixDefault, c1);
	}

	@Override
	public Temperature2DScalar apply(Temperature2DScalar temperature, Double forcing) {
		// This is a bit clumsy code. We have to convert the row vector to a column vector, multiply it, then convert it back to a row.
		final double[] temperatureNext = LinearAlgebra.multMatrixVector(transitionMatrix, temperature.getAsDoubleArray());
		
		// TODO - matrix need rescaled from 5Y to 1Y
		final double[] temperatureNextScaled = IntStream.range(0, temperatureNext.length).mapToDouble(i -> temperature.getAsDoubleArray()[i] + timeStep/5.0 * (temperatureNext[i]-temperature.getAsDoubleArray()[i])).toArray();
		temperatureNextScaled[0] += forcingToTemp * forcing * timeStep;
		return new Temperature2DScalar(temperatureNextScaled);
	}

}
