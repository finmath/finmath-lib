package net.finmath.fouriermethod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.apache.commons.math3.complex.Complex;
import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.BlackScholesModel;
import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;

/** Regression tests for the deterministic-variance limit of the Heston characteristic function. */
public class HestonModelZeroVolOfVolTest {

	private static final double SPOT = 100.0;
	private static final double VOLATILITY = 0.3;
	private static final double FORWARD_RATE = 0.05;
	private static final double DISCOUNT_RATE = 0.02;
	private static final Complex[] ARGUMENTS = {
		Complex.ZERO, new Complex(0.1), new Complex(-2.0), new Complex(10.0),
		new Complex(0.0, -1.0), new Complex(0.7, -1.5)
	};

	@Test
	public void testConstantVarianceAgainstBlackScholes() {
		final BlackScholesModel blackScholes = new BlackScholesModel(SPOT, FORWARD_RATE, DISCOUNT_RATE, VOLATILITY);
		for(final double kappa : new double[] { 0.0, 1E-14, 0.7, 5.0 }) {
			for(final double rho : new double[] { -0.9, 0.0, 0.9 }) {
				final HestonModel heston = model(VOLATILITY * VOLATILITY, kappa, 0.0, rho);
				for(final double time : new double[] { 0.0, 0.01, 1.0, 10.0 }) {
					for(final Complex argument : ARGUMENTS) {
						assertComplexEquals(blackScholes.apply(time).apply(argument), heston.apply(time).apply(argument), 2E-13);
					}
				}
			}
		}
	}

	@Test
	public void testTimeDependentVariance() {
		for(final double theta : new double[] { 0.0, 0.04, 0.16 }) {
			for(final double time : new double[] { 0.1, 1.0, 5.0 }) {
				final double kappa = 0.7;
				final double variance = theta * time + (VOLATILITY * VOLATILITY - theta) * (1.0 - Math.exp(-kappa * time)) / kappa;
				for(final Complex argument : ARGUMENTS) {
					assertComplexEquals(analytic(argument, time, variance), model(theta, kappa, 0.0, -0.7).apply(time).apply(argument), 2E-13);
				}
			}
		}
	}

	@Test
	public void testZeroAndSmallMeanReversion() {
		for(final double kappa : new double[] { 0.0, 1E-16 }) {
			for(final Complex argument : ARGUMENTS) {
				assertComplexEquals(analytic(argument, 2.0, VOLATILITY * VOLATILITY * 2.0),
						model(0.16, kappa, 0.0, 0.5).apply(2.0).apply(argument), 2E-13);
			}
		}
	}

	@Test
	public void testZeroTime() {
		for(final double kappa : new double[] { 0.0, 0.7 }) {
			for(final Complex argument : ARGUMENTS) {
				assertComplexEquals(argument.multiply(Complex.I).multiply(Math.log(SPOT)).exp(),
						model(0.16, kappa, 0.0, -0.7).apply(0.0).apply(argument), 2E-13);
			}
		}
	}

	@Test
	public void testSeparateDiscountCurves() {
		final double time = 2.0;
		final DiscountCurve forwardCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"forward", new double[] { 0.0, time }, new double[] { 1.0, 0.91 });
		final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"discount", new double[] { 0.0, time }, new double[] { 1.0, 0.97 });
		final double variance = 0.16 * time + (VOLATILITY * VOLATILITY - 0.16) * (1.0 - Math.exp(-0.7 * time)) / 0.7;
		final HestonModel heston = new HestonModel(SPOT, forwardCurve, discountCurve, VOLATILITY, 0.16, 0.7, 0.0, -0.7);
		final BlackScholesModel expected = new BlackScholesModel(null, SPOT, forwardCurve, discountCurve, Math.sqrt(variance / time));
		for(final Complex argument : ARGUMENTS) {
			assertComplexEquals(expected.apply(time).apply(argument), heston.apply(time).apply(argument), 2E-13);
		}
		assertComplexEquals(new Complex(0.97), heston.apply(time).apply(Complex.ZERO), 2E-13);
		assertComplexEquals(new Complex(SPOT * 0.97 / 0.91), heston.apply(time).apply(new Complex(0.0, -1.0)), 2E-13);
	}

	@Test
	public void testContinuityFromPositiveVolOfVol() {
		// The existing positive-xi formula loses precision for extremely small xi.
		for(final double theta : new double[] { VOLATILITY * VOLATILITY, 0.16 }) {
			for(final double rho : new double[] { -0.7, 0.0, 0.7 }) {
				for(final Complex argument : new Complex[] { new Complex(0.2), new Complex(1.0), new Complex(0.7, -1.5) }) {
					final Complex limit = model(theta, 0.7, 0.0, rho).apply(2.0).apply(argument);
					for(final double xi : new double[] { 1E-2, 1E-3, 1E-4 }) {
						assertComplexEquals(limit, model(theta, 0.7, xi, rho).apply(2.0).apply(argument), 0.5 * xi);
					}
				}
			}
		}
	}

	@Test
	public void testFourierOptionPrices() throws CalculationException {
		for(final double theta : new double[] { VOLATILITY * VOLATILITY, 0.16 }) {
			final double time = 2.0;
			final double variance = theta * time + (VOLATILITY * VOLATILITY - theta) * (1.0 - Math.exp(-0.7 * time)) / 0.7;
			final BlackScholesModel expected = new BlackScholesModel(SPOT, FORWARD_RATE, DISCOUNT_RATE, Math.sqrt(variance / time));
			for(final double strike : new double[] { 80.0, 100.0, 120.0 }) {
				final EuropeanOption option = new EuropeanOption(time, strike);
				assertEquals(option.getValue(expected), option.getValue(model(theta, 0.7, 0.0, -0.7)), 1E-10);
			}
		}
	}

	@Test
	public void testPositiveVolOfVolRegression() {
		// Values captured from the unmodified implementation at 0cd21fc47facd700304ce3f91a7c1de4f35686d6.
		final double[][] expected = {
			{ 0.58175157593689950, 0.75861289924327660 },
			{ -0.10719125777603698, -0.84113166661220870 },
			{ -1096.3444426493706, -360.09895324437764 },
			{ 0.58088905424782740, 0.75796607910000400 },
			{ -0.077669615581416540, -0.83162225773197540 },
			{ -1113.4933086506871, -316.30399888384795 },
			{ 0.57961631402951150, 0.75727251855114490 },
			{ -0.045938286119396320, -0.83935517790051660 },
			{ -1117.7792404451234, -284.88637442378734 }
		};
		int index = 0;
		for(final double xi : new double[] { 0.05, 0.5, 1.0 }) {
			for(final Complex argument : new Complex[] { new Complex(0.2), new Complex(1.0), new Complex(0.7, -1.5) }) {
				assertComplexEquals(new Complex(expected[index][0], expected[index][1]),
						model(0.16, 0.7, xi, -0.7).apply(2.0).apply(argument), 2E-13);
				index++;
			}
		}
	}

	private static HestonModel model(final double theta, final double kappa, final double xi, final double rho) {
		return new HestonModel(SPOT, FORWARD_RATE, VOLATILITY, DISCOUNT_RATE, theta, kappa, xi, rho);
	}

	private static Complex analytic(final Complex argument, final double time, final double integratedVariance) {
		// Independent real/imaginary evaluation, including complex Fourier contour arguments.
		final double realZ = -argument.getImaginary();
		final double imaginaryZ = argument.getReal();
		final double logForward = Math.log(SPOT) + FORWARD_RATE * time;
		final double realExponent = -DISCOUNT_RATE * time + realZ * logForward
				+ 0.5 * (realZ * realZ - imaginaryZ * imaginaryZ - realZ) * integratedVariance;
		final double imaginaryExponent = imaginaryZ * logForward + 0.5 * (2.0 * realZ - 1.0) * imaginaryZ * integratedVariance;
		return new Complex(Math.cos(imaginaryExponent), Math.sin(imaginaryExponent)).multiply(Math.exp(realExponent));
	}

	private static void assertComplexEquals(final Complex expected, final Complex actual, final double tolerance) {
		assertTrue(Double.isFinite(actual.getReal()) && Double.isFinite(actual.getImaginary()), "Finite characteristic function");
		assertEquals(0.0, actual.subtract(expected).abs(), tolerance * Math.max(1.0, expected.abs()), "Characteristic function");
	}
}
