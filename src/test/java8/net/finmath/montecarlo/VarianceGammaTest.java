package net.finmath.montecarlo;

import java.text.DecimalFormat;

import org.apache.commons.math3.complex.Complex;
import org.junit.Test;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * We test the Variance Gamma process Monte Carlo implementation by estimating the characteristic function.
 *
 * @author Alessandro Gnoatto
 *
 */
public class VarianceGammaTest {
	static final DecimalFormat formatterReal2	= new DecimalFormat("+#,##00.0000;-#");

	@Test
	public void testCharacteristicFunction() {
		// The parameters
		final int seed			= 53252;
		final int numberOfFactors = 1;
		final int numberOfPaths	= 10000;
		final double lastTime		= 10;
		final double dt			= 0.1;

		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)(lastTime/dt), dt);

		final double sigma = 0.25;
		final double nu = 0.1;
		final double theta = 0.4;

		final VarianceGammaProcess varianceGamma = new VarianceGammaProcess(sigma, nu, theta, timeDiscretization,
				numberOfFactors, numberOfPaths, seed);

		//Initialize process
		RandomVariable process = varianceGamma.getIncrement(0, 0).mult(0.0);

		final Complex z = new Complex(1.0,-1.0);

		//Sum over increments to construct the process path
		for(int i = 0; i< timeDiscretization.getNumberOfTimeSteps()-1; i++) {
			final Complex monteCarloCF = characteristicFunctionByMonteCarlo(z, process);

			final RandomVariable increment = varianceGamma.getIncrement(i, 0);
			process = process.add(increment);

			final Complex exactCF = getCharacteristicFunction(timeDiscretization.getTime(i),z,varianceGamma);

			System.out.println(formatterReal2.format(exactCF.getReal()) + "\t" +formatterReal2.format(exactCF.getImaginary())
			+ "\t" + "\t" + formatterReal2.format(monteCarloCF.getReal()) + "\t" +formatterReal2.format(monteCarloCF.getImaginary()));

		}
	}

	public Complex characteristicFunctionByMonteCarlo(final Complex zeta, final RandomVariable processAtTime) {

		final int states = processAtTime.getRealizations().length;

		Complex runningSum = new Complex(0.0,0.0);

		for(int i = 0; i< states; i++) {
			runningSum = runningSum.add((Complex.I.multiply(zeta.multiply(processAtTime.get(i)))).exp());
		}

		return runningSum.divide(states);
	}

	/*
	 * Helper method to compute the characteristic function in closed form.
	 */
	public Complex getCharacteristicFunction(final double time, final Complex zeta, final VarianceGammaProcess process) {

		final double nu = process.getNu();
		final double sigma = process.getSigma();
		final double theta = process.getTheta();

		final Complex numerator = Complex.ONE;
		final Complex denominator = (Complex.ONE).subtract(Complex.I.multiply(zeta.multiply(theta*nu))).add(zeta.multiply(zeta).multiply(sigma*sigma*0.5*nu));

		return (((numerator.divide(denominator)).log()).multiply(time/nu)).exp();
	}
}
