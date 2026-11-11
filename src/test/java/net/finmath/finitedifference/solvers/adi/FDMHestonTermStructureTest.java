package net.finmath.finitedifference.solvers.adi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.function.DoubleUnaryOperator;

import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMBlackScholesModel;
import net.finmath.finitedifference.assetderivativevaluation.products.EuropeanOption;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.utilities.DiscountCurveRates;
import net.finmath.finitedifference.utilities.FiniteDifferenceValueInterpolator;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.AbstractCurve;
import net.finmath.marketdata.model.curves.CurveBuilder;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.time.TimeDiscretizationFromArray;

/** Regression tests for calendar-time rates, boundary discounting and ADI stepping. */
public class FDMHestonTermStructureTest {

	private static final double SPOT = 100.0;
	private static final double VARIANCE = 0.04;

	@Test
	public void testInstantaneousRatesIncludingZero() {
		for(final double scale : new double[] {1.0, 0.7, 1E-100}) {
			final DiscountCurve curve = new FunctionalDiscountCurve(t -> scale * Math.exp(-0.01*t - 0.08*t*t));
			for(final double t : new double[] {0.0, 1E-12, 5E-5, 1E-4, 0.25, 1.0, 10.0}) {
				assertEquals(0.01 + 0.16*t, DiscountCurveRates.getInstantaneousForwardRate(curve, t), 3E-10);
			}
		}
		// A cubic log discount factor exercises the second-order one-sided stencil.
		final DiscountCurve cubic = new FunctionalDiscountCurve(t -> Math.exp(-0.03*t - 0.02*t*t*t));
		assertEquals(0.03, DiscountCurveRates.getInstantaneousForwardRate(cubic, 0.0), 1E-9);
		for(final double r : new double[] {-0.03, 0.0, 0.05}) {
			for(final double t : new double[] {0.0, 1E-10, 0.5, 20.0}) {
				assertEquals(r, DiscountCurveRates.getInstantaneousForwardRate(curve(r, 0.0), t), 3E-12);
			}
		}
	}

	@Test
	public void testInvalidCurveInputs() {
		assertThrows(IllegalArgumentException.class,
				() -> DiscountCurveRates.getInstantaneousForwardRate(curve(0.0, 0.0), -0.1));
		assertThrows(IllegalArgumentException.class,
				() -> DiscountCurveRates.getInstantaneousForwardRate(curve(0.0, 0.0), Double.NaN));
		for(final double factor : new double[] {0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY}) {
			assertThrows(IllegalArgumentException.class,
					() -> DiscountCurveRates.getInstantaneousForwardRate(new FunctionalDiscountCurve(t -> factor), 0.0));
		}
	}

	@Test
	public void testInterpolatedCurveKnot() {
		final DiscountCurve curve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				"piecewise", new double[] {0.0, 0.5, 1.0}, new double[] {1.0, Math.exp(-0.01), Math.exp(-0.06)},
				InterpolationMethod.LINEAR, ExtrapolationMethod.LINEAR, InterpolationEntity.LOG_OF_VALUE);
		assertEquals(0.02, DiscountCurveRates.getInstantaneousForwardRate(curve, 0.25), 2E-10);
		assertEquals(0.10, DiscountCurveRates.getInstantaneousForwardRate(curve, 0.75), 2E-10);
		assertEquals(0.06, DiscountCurveRates.getInstantaneousForwardRate(curve, 0.5), 2E-10);
	}

	@Test
	public void testGeneratorMomentsAtNaturalBoundaries() {
		final SpaceTimeDiscretization grid = grid(8, 10, 4);
		final EuropeanOption option = new EuropeanOption(1.0, SPOT);
		for(final double xi : new double[] {0.0, 0.3}) {
			final FDMHestonModel model = new FDMHestonModel(SPOT, VARIANCE,
					curve(0.01, 0.08), curve(-0.02, 0.03), 1.5, 0.09, xi, -0.7, grid);
			final FDMHestonADI2D solver = new FDMHestonADI2D(model, option, grid, option.getExercise());
			final double[] spots = grid.getSpaceGrid(0).getGrid();
			final double[] variances = grid.getSpaceGrid(1).getGrid();
			final double[] one = new double[spots.length * variances.length];
			final double[] stock = new double[one.length];
			final double[] variance = new double[one.length];
			Arrays.fill(one, 1.0);
			for(int j = 0; j < variances.length; j++) {
				for(int i = 0; i < spots.length; i++) {
					stock[i + j*spots.length] = spots[i];
					variance[i + j*spots.length] = variances[j];
				}
			}
			for(final double t : new double[] {0.0, 0.25, 0.75, 1.0}) {
				final double r = 0.01 + 0.16*t;
				final double q = -0.02 + 0.06*t;
				final double[] lOne = solver.applyFullExplicitOperator(one, t);
				final double[] lStock = solver.applyFullExplicitOperator(stock, t);
				final double[] lVariance = solver.applyFullExplicitOperator(variance, t);
				for(int j = 0; j < variances.length - (xi == 0.0 ? 0 : 1); j++) {
					final int index = 2 + j*spots.length;
					assertEquals(-r, lOne[index], 2E-10);
					assertEquals(-q * SPOT, lStock[index], 3E-8);
					assertEquals(1.5*(0.09-variances[j]) - r*variances[j], lVariance[index], 2E-10);
				}
			}
		}
	}

	@Test
	public void testBoundaryDiscountRatios() {
		final DiscountCurve r = curve(0.01, 0.08);
		final DiscountCurve q = curve(0.02, 0.03);
		final FDMHestonModel model = model(grid(8, 10, 4), r, q);
		for(final CallOrPut type : CallOrPut.values()) {
			final EuropeanOption option = new EuropeanOption(1.0, SPOT, type);
			for(final double t : new double[] {0.0, 0.4, 1.0}) {
				for(final double s : new double[] {0.0, 100.0, 400.0}) {
					final double signed = s*q.getDiscountFactor(1.0)/q.getDiscountFactor(t)
							- SPOT*r.getDiscountFactor(1.0)/r.getDiscountFactor(t);
					final double expected = Math.max(type == CallOrPut.CALL ? signed : -signed, 0.0);
					assertEquals(expected, model.getBoundaryConditionsAtLowerBoundary(option, t, s, 0.0)[0].getValue(), 1E-12);
					assertEquals(expected, model.getBoundaryConditionsAtUpperBoundary(option, t, s, 0.5)[0].getValue(), 1E-12);
				}
			}
		}
	}

	@Test
	public void testIssue112ReferenceAndGridConvergence() {
		final DiscountCurve r = curve(0.01, 0.08);
		final DiscountCurve q = curve(0.02, 0.0);
		final double expected = reference(r, q, 0.0, SPOT, CallOrPut.CALL);
		assertEquals(11.3129337493, expected, 5E-10);
		final double coarse = price(grid(160, 100, 80), r, q, 0.0, SPOT, CallOrPut.CALL);
		final double fine = price(grid(320, 200, 160), r, q, 0.0, SPOT, CallOrPut.CALL);
		System.out.printf("Issue112 reference=%.10f coarse=%.10f fine=%.10f errors=%+.10f,%+.10f%n",
				expected, coarse, fine, coarse-expected, fine-expected);
		assertEquals(expected, fine, 0.01);
		assertTrue(Math.abs(fine-expected) < 0.65*Math.abs(coarse-expected));
	}

	@Test
	public void testBothCurvesAndNonzeroEvaluationTime() {
		final DiscountCurve r = curve(-0.01, 0.10);
		final DiscountCurve q = curve(0.02, 0.03);
		for(final double t : new double[] {0.0, 0.375}) {
			final double call = price(grid(320, 80, 160), r, q, t, SPOT, CallOrPut.CALL);
			final double put = price(grid(320, 80, 160), r, q, t, SPOT, CallOrPut.PUT);
			final double expectedCall = reference(r, q, t, SPOT, CallOrPut.CALL);
			final double expectedPut = reference(r, q, t, SPOT, CallOrPut.PUT);
			System.out.printf("Both curves t=%.3f call=%.10f reference=%.10f put=%.10f reference=%.10f%n",
					t, call, expectedCall, put, expectedPut);
			assertEquals(expectedCall, call, 0.01);
			assertEquals(expectedPut, put, 0.01);
			assertEquals(expectedCall-expectedPut, call-put, 0.003);
		}
	}

	@Test
	public void testMeanRevertingVarianceWithBothCurves() throws CalculationException {
		final DiscountCurve r = curve(0.01, 0.08);
		final DiscountCurve q = curve(0.02, 0.03);
		final SpaceTimeDiscretization grid = grid(160, 100, 80);
		for(final double xi : new double[] {0.0, 0.01}) {
			final FDMHestonModel model = new FDMHestonModel(SPOT, VARIANCE, r, q, 1.5, 0.09, xi, -0.7, grid);
			final double[] values = new EuropeanOption(1.0, SPOT).getValue(0.0, model);
			final double actual = FiniteDifferenceValueInterpolator.interpolateValue(values, grid, model.getInitialValue());
			// Deterministic r(t),q(t) enter the terminal Heston distribution only
			// through their integrals, so constant rates 0.09 and 0.05 give an oracle.
			final net.finmath.fouriermethod.models.HestonModel fourier =
					new net.finmath.fouriermethod.models.HestonModel(SPOT, 0.04, 0.2, 0.09, 0.09, 1.5, xi, -0.7);
			final double expected = new net.finmath.fouriermethod.products.EuropeanOption(1.0, SPOT).getValue(fourier);
			System.out.printf("Mean-reverting variance xi=%.2f PDE=%.10f Fourier=%.10f%n", xi, actual, expected);
			assertEquals(expected, actual, 0.02);
		}
	}

	@Test
	public void testNonAutonomousDouglasTimeConvergence() {
		// L(S) = -q(t) S with r=0 and v=0: no spatial truncation error and
		// no explicit A0 term. Isolates the old/new times of the ADI stages.
		final DiscountCurve r = curve(0.0, 0.0);
		final DiscountCurve q = curve(0.02, 0.08);
		final double expected = SPOT*Math.exp(-0.10);
		double previousError = Double.NaN;
		for(final int steps : new int[] {10, 20, 40}) {
			final SpaceTimeDiscretization grid = grid(20, 5, steps);
			final FDMHestonModel model = new FDMHestonModel(SPOT, 0.0, r, q, 0.0, 0.0, 0.0, 0.0, grid);
			final double[] values = new EuropeanOption(1.0, 0.0).getValue(0.0, model);
			final double actual = FiniteDifferenceValueInterpolator.interpolateValue(values, grid, model.getInitialValue());
			final double error = Math.abs(actual-expected);
			System.out.printf("Time convergence steps=%d value=%.12f error=%.12g%n", steps, actual, error);
			if(Double.isFinite(previousError)) {
				assertTrue(error < 0.30*previousError);
			}
			previousError = error;
		}
	}

	@Test
	public void testLegacyModelRateConventionIsPreserved() {
		// AbstractADI2D is shared. Migrating only its discount term would mix
		// instantaneous discounting with the zero-rate drift of other models.
		final DiscountCurve r = curve(0.01, 0.08);
		final FDMBlackScholesModel legacy = new FDMBlackScholesModel(SPOT, r, curve(0.02, 0.0), 0.2, grid(8, 10, 4));
		for(final double t : new double[] {0.0, 0.25, 0.75}) {
			final double safeTime = Math.max(t, 1E-10);
			assertEquals(-Math.log(r.getDiscountFactor(safeTime))/safeTime, legacy.getDiscountingRate(t), 0.0);
			if(t > 0.0) {
				assertEquals(0.01 + 0.08*t, legacy.getDiscountingRate(t), 1E-12);
				assertEquals(legacy.getDiscountingRate(t)-0.02, legacy.getDrift(t, SPOT)[0]/SPOT, 1E-12);
			}
		}
	}

	@Test
	public void testFlatCurveCompatibility() {
		final double actual = price(grid(160, 100, 80), curve(0.05, 0.0), curve(0.02, 0.0), 0.0, SPOT, CallOrPut.CALL);
		// Baseline is captured separately from the unmodified PR #113 head.
		System.out.printf("Flat curve price=%.12f%n", actual);
		assertEquals(9.2127763681, actual, 1E-8);
	}

	private static SpaceTimeDiscretization grid(final int spots, final int variances, final int steps) {
		return new SpaceTimeDiscretization(
				new Grid[] {new UniformGrid(spots, 0.0, 400.0), new UniformGrid(variances, 0.0, 0.5)},
				new TimeDiscretizationFromArray(0.0, steps, 1.0/steps), 0.5, new double[] {SPOT, VARIANCE});
	}

	private static FDMHestonModel model(final SpaceTimeDiscretization grid, final DiscountCurve r, final DiscountCurve q) {
		return new FDMHestonModel(SPOT, VARIANCE, r, q, 1.5, VARIANCE, 0.0, -0.7, grid);
	}

	private static double price(final SpaceTimeDiscretization grid, final DiscountCurve r, final DiscountCurve q,
			final double t, final double strike, final CallOrPut type) {
		final FDMHestonModel model = model(grid, r, q);
		final double[] values = new EuropeanOption(1.0, strike, type).getValue(t, model);
		return FiniteDifferenceValueInterpolator.interpolateValue(values, grid, model.getInitialValue());
	}

	private static double reference(final DiscountCurve r, final DiscountCurve q, final double t,
			final double strike, final CallOrPut type) {
		final double dr = r.getDiscountFactor(1.0)/r.getDiscountFactor(t);
		final double dq = q.getDiscountFactor(1.0)/q.getDiscountFactor(t);
		final double call = AnalyticFormulas.blackScholesGeneralizedOptionValue(SPOT*dq/dr, 0.2, 1.0-t, strike, dr);
		return type == CallOrPut.CALL ? call : call - SPOT*dq + strike*dr;
	}

	private static DiscountCurve curve(final double a, final double b) {
		return new FunctionalDiscountCurve(t -> Math.exp(-a*t - b*t*t));
	}

	private static final class FunctionalDiscountCurve extends AbstractCurve implements DiscountCurve {
		private static final long serialVersionUID = 1L;
		private final DoubleUnaryOperator discountFactor;

		private FunctionalDiscountCurve(final DoubleUnaryOperator discountFactor) {
			super("test", null);
			this.discountFactor = discountFactor;
		}

		@Override
		public double getDiscountFactor(final double time) {
			assertTrue(time >= 0.0, "The curve must not be queried at negative times.");
			return discountFactor.applyAsDouble(time);
		}

		@Override
		public double getDiscountFactor(final AnalyticModel model, final double time) {
			return getDiscountFactor(time);
		}

		@Override
		public double getValue(final AnalyticModel model, final double time) {
			return getDiscountFactor(time);
		}

		@Override
		public double[] getParameter() {
			return new double[0];
		}

		@Override
		public void setParameter(final double[] parameter) {
			throw new UnsupportedOperationException();
		}

		@Override
		public CurveBuilder getCloneBuilder() throws CloneNotSupportedException {
			throw new CloneNotSupportedException();
		}
	}
}
