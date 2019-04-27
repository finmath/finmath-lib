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
		int seed			= 53252;
		int numberOfFactors = 1;
		int numberOfPaths	= 10000;
		double lastTime		= 10;
		double dt			= 0.1;

		// Create the time discretization
		TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)(lastTime/dt), dt);

		double sigma = 0.25;
		double nu = 0.1;
		double theta = 0.4;

		VarianceGammaProcess varianceGamma = new VarianceGammaProcess(sigma, nu, theta, timeDiscretization,
				numberOfFactors, numberOfPaths, seed);

		//Initialize process
		RandomVariable process = varianceGamma.getIncrement(0, 0).mult(0.0);

		Complex z = new Complex(1.0,-1.0);

		//Sum over increments to construct the process path
		for(int i = 0; i< timeDiscretization.getNumberOfTimeSteps()-1; i++) {
			Complex monteCarloCF = characteristicFunctionByMonteCarlo(z, process);

			RandomVariable increment = varianceGamma.getIncrement(i, 0);
			process = process.add(increment);

			Complex exactCF = getCharacteristicFunction(timeDiscretization.getTime(i),z,varianceGamma);

			System.out.println(formatterReal2.format(exactCF.getReal()) + "\t" +formatterReal2.format(exactCF.getImaginary())
			+ "\t" + "\t" + formatterReal2.format(monteCarloCF.getReal()) + "\t" +formatterReal2.format(monteCarloCF.getImaginary()));

		}
	}

	public Complex characteristicFunctionByMonteCarlo(Complex zeta, RandomVariable processAtTime) {

		int states = processAtTime.getRealizations().length;

		Complex runningSum = new Complex(0.0,0.0);

		for(int i = 0; i< states; i++) {
			runningSum = runningSum.add((Complex.I.multiply(zeta.multiply(processAtTime.get(i)))).exp());
		}

		return runningSum.divide(states);
	}

	/*
	 * Helper method to compute the characteristic function in closed form.
	 */
	public Complex getCharacteristicFunction(double time, Complex zeta, VarianceGammaProcess process) {

		double nu = process.getNu();
		double sigma = process.getSigma();
		double theta = process.getTheta();

		Complex numerator = Complex.ONE;
		Complex denominator = (Complex.ONE).subtract(Complex.I.multiply(zeta.multiply(theta*nu))).add(zeta.multiply(zeta).multiply(sigma*sigma*0.5*nu));

		return (((numerator.divide(denominator)).log()).multiply(time/nu)).exp();
	}
}
