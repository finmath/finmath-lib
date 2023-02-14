package net.finmath.climate.models.dice.submodels;

import java.util.function.Function;

import net.finmath.functions.LinearAlgebra;
import net.finmath.time.TimeDiscretization;
import net.finmath.util.Cached;
import net.finmath.util.TriFunction;

/**
 *
 * The evolution of the temperature \( \mathrm{d}T(t) = \left( \Gamma_{T} T(t) + F(t) \right) \mathrm{d}t \).
 * 
 * The unit of \( T \) is K (Kelvin).
 * 
 * The evolution is modelled as \( \mathrm{d}T(t) = \left( \Gamma_{T} T(t) + F(t) \right) \mathrm{d}t \).
 * With the given {@link TimeDiscretization} it is approximated via an Euler-step
 * \(
 * 	T(t_{i+1}) = \Phi T(t_{i}) + (forcingToTemp \cdot (forcing, 0, 0) \cdot \Delta t_{i}
 * \)
 * where \( \Phi = (1 + \Gamma_{T} \Delta t_{i}) \).
 *
 * @author Christian Fries
 */
public class EvolutionOfTemperature implements TriFunction<Integer, Temperature2DScalar, Double, Temperature2DScalar> {

	private static double forcingToTempDefault = 0.1005;		// sometimes called xi1 or c1		// TODO check role of time step

	private static double[][] transitionMatrix5YDefault;
	static {
		final double fco22x = 3.6813;       // Forcings of equilibrium CO2 doubling (Wm-2)
		final double t2xco2 = 3.1;			// Equilibrium temp impact (°C per doubling CO2)
		final double c3 = 0.088;			// Transfer coefficient upper to lower stratum
		final double c4 = 0.025;

		final double phi11 = 1-forcingToTempDefault*((fco22x/t2xco2) + c3);
		final double phi12 = forcingToTempDefault*c3;
		final double phi21 = c4;
		final double phi22 = 1-c4;

		transitionMatrix5YDefault = new double[][] { new double[] { phi11, phi12 }, new double[] { phi21, phi22 } };
	}

	private final TimeDiscretization timeDiscretization;
	private final Function<Integer, double[][]> transitionMatrices;		// phi in [i][j] (i = row, j = column)
	private final double forcingToTemp;

	/**
	 * @param timeDiscretization The time discretization.
	 * @param transitionMatrices Transition matrix \( \Phi \) for each time step.
	 * @param forcingToTemp The scaling coefficient for the external forcing.
	 */
	public EvolutionOfTemperature(TimeDiscretization timeDiscretization, Function<Integer, double[][]> transitionMatrices, double forcingToTemp) {
		super();
		this.timeDiscretization = timeDiscretization;
		this.transitionMatrices = transitionMatrices;
		this.forcingToTemp = forcingToTemp;
	}

	public EvolutionOfTemperature(TimeDiscretization timeDiscretization) {
		Function<Integer, Double> timeSteps = ((Integer timeIndex) -> { return timeDiscretization.getTimeStep(timeIndex); });
		this.timeDiscretization = timeDiscretization;
		transitionMatrices = timeSteps.andThen(Cached.<Double, double[][]>of(timeStep -> timeStep == 5.0 ? transitionMatrix5YDefault : LinearAlgebra.matrixPow(transitionMatrix5YDefault, (Double)timeStep/5.0)));
		this.forcingToTemp = forcingToTempDefault;
	}

	@Override
	public Temperature2DScalar apply(Integer timeIndex, Temperature2DScalar temperature, Double forcing) {
		final double timeStep = timeDiscretization.getTimeStep(timeIndex);
		final double[] temperatureNext = LinearAlgebra.multMatrixVector(transitionMatrices.apply(timeIndex), temperature.getAsDoubleArray());
		
		temperatureNext[0] += forcingToTemp * forcing * timeStep;
		return new Temperature2DScalar(temperatureNext);
	}

	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}
}
