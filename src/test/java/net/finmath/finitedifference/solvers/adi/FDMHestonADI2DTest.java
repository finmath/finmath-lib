package net.finmath.finitedifference.solvers.adi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.finmath.exception.CalculationException;
import net.finmath.finitedifference.assetderivativevaluation.boundaries.EuropeanOptionHestonModelBoundary;
import net.finmath.finitedifference.assetderivativevaluation.models.FDMHestonModel;
import net.finmath.finitedifference.assetderivativevaluation.products.EuropeanOption;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.boundaries.StandardBoundaryCondition;
import net.finmath.finitedifference.grids.ExponentialGrid;
import net.finmath.finitedifference.grids.Grid;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.finitedifference.grids.UniformGrid;
import net.finmath.finitedifference.utilities.FiniteDifferenceValueInterpolator;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.modelling.products.CallOrPut;
import net.finmath.time.TimeDiscretizationFromArray;

/** Regression tests for natural variance boundaries in the Heston ADI solver. */
public class FDMHestonADI2DTest {

	private static final double SPOT = 100.0;
	private static final double RATE = 0.05;
	private static final double DIVIDEND = 0.02;
	private static final double KAPPA = 1.5;
	private static final double THETA = 0.09;

	@Test
	public void testGeneratorAtDegenerateVarianceBoundaries() {
		final SpaceTimeDiscretization grid = new SpaceTimeDiscretization(
				new Grid[] {new UniformGrid(4, 0, 200), new ExponentialGrid(8, 0, 0.5, 2.0)},
				new TimeDiscretizationFromArray(0, 1, 1.0), 0.5, new double[] {SPOT, 0.04});
		final EuropeanOption option = new EuropeanOption(1.0, SPOT);
		for(final double xi : new double[] {0.0, 0.3}) {
			final FDMHestonADI2D solver = new FDMHestonADI2D(
					model(grid, 0.04, THETA, KAPPA, xi), option, grid, option.getExercise());
			final double[] variances = grid.getSpaceGrid(1).getGrid();
			final double[] constant = new double[5 * variances.length];
			final double[] variance = new double[constant.length];
			Arrays.fill(constant, 1.0);
			for(int j = 0; j < variances.length; j++) {
				Arrays.fill(variance, 5 * j, 5 * (j + 1), variances[j]);
			}
			final double[] constantGenerator = solver.applyFullExplicitOperator(constant, 0.5);
			final double[] varianceGenerator = solver.applyFullExplicitOperator(variance, 0.5);
			// L(1) = -r and L(v) = kappa*(theta-v) - r*v, also at v=0.
			assertEquals(-RATE, constantGenerator[2], 1E-12);
			assertEquals(KAPPA * THETA, varianceGenerator[2], 1E-12);
			if(xi == 0.0) {
				final int upper = 2 + 5 * (variances.length - 1);
				final double v = variances[variances.length - 1];
				assertEquals(-RATE, constantGenerator[upper], 1E-12);
				assertEquals(KAPPA * (THETA - v) - RATE * v, varianceGenerator[upper], 1E-12);
			}
		}
	}

	@Test
	public void testDeterministicVarianceConvergence() {
		final double expected = deterministicCall(0.04, THETA, KAPPA);
		final double coarse = price(1, 0.04, THETA, KAPPA, 0.0, CallOrPut.CALL);
		final double fine = price(2, 0.04, THETA, KAPPA, 0.0, CallOrPut.CALL);
		assertEquals(expected, fine, 0.005);
		assertTrue(Math.abs(fine - expected) < Math.abs(coarse - expected) / 2.0);
		final double put = price(2, 0.04, THETA, KAPPA, 0.0, CallOrPut.PUT);
		assertEquals(expected + SPOT * Math.exp(-RATE) - SPOT * Math.exp(-DIVIDEND), put, 0.005);
	}

	@Test
	public void testInitiallyZeroVariance() {
		assertEquals(deterministicCall(0.0, THETA, KAPPA),
				price(2, 0.0, THETA, KAPPA, 0.0, CallOrPut.CALL), 0.04);
	}

	@Test
	public void testConstantVarianceControls() {
		for(final double[] parameters : new double[][] {{0.04, KAPPA}, {THETA, 0.0}}) {
			assertEquals(deterministicCall(0.04, parameters[0], parameters[1]),
					price(1, 0.04, parameters[0], parameters[1], 0.0, CallOrPut.CALL), 0.02);
		}
	}

	@Test
	public void testPositiveVolOfVolAgainstFourier() throws CalculationException {
		final net.finmath.fouriermethod.models.HestonModel fourier =
				new net.finmath.fouriermethod.models.HestonModel(
						SPOT, RATE - DIVIDEND, 0.2, RATE, THETA, KAPPA, 0.01, -0.7);
		final double expected = new net.finmath.fouriermethod.products.EuropeanOption(1.0, SPOT).getValue(fourier);
		assertEquals(expected, price(1, 0.04, THETA, KAPPA, 0.01, CallOrPut.CALL), 0.02);
	}

	@Test
	public void testPrescribedVarianceBoundaries() {
		final SpaceTimeDiscretization grid = grid(1, 0.04);
		final FDMHestonModel model = new FDMHestonModel(
				SPOT, 0.04, curve("r", RATE), curve("q", DIVIDEND), KAPPA, THETA, 0.0, -0.7, grid) {
			@Override
			public BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
					final FiniteDifferenceEquityProduct product, final double time, final double... state) {
				final BoundaryCondition[] conditions = new EuropeanOptionHestonModelBoundary(this)
						.getBoundaryConditionsAtLowerBoundary(product, time, state);
				conditions[1] = StandardBoundaryCondition.dirichlet(7.0);
				return conditions;
			}

			@Override
			public BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
					final FiniteDifferenceEquityProduct product, final double time, final double... state) {
				final BoundaryCondition[] conditions = new EuropeanOptionHestonModelBoundary(this)
						.getBoundaryConditionsAtUpperBoundary(product, time, state);
				conditions[1] = StandardBoundaryCondition.dirichlet(11.0);
				return conditions;
			}
		};
		final EuropeanOption option = new EuropeanOption(1.0, SPOT);
		final FDMHestonADI2D solver = new FDMHestonADI2D(model, option, grid, option.getExercise());
		final int n0 = grid.getSpaceGrid(0).getGrid().length;
		final int n1 = grid.getSpaceGrid(1).getGrid().length;
		final double[] values = solver.performDouglasHalfStep(new double[n0 * n1], 0.5, 0.01);
		assertEquals(7.0, values[40], 0.0);
		assertEquals(11.0, values[40 + n0 * (n1 - 1)], 0.0);
	}

	private static double deterministicCall(final double initialVariance, final double theta, final double kappa) {
		final double variance = kappa == 0.0 ? initialVariance
				: theta + (initialVariance - theta) * (-Math.expm1(-kappa)) / kappa;
		return AnalyticFormulas.blackScholesGeneralizedOptionValue(
				SPOT * Math.exp(RATE - DIVIDEND), Math.sqrt(variance), 1.0, SPOT, Math.exp(-RATE));
	}

	private static double price(final int scale, final double v0, final double theta,
			final double kappa, final double xi, final CallOrPut callOrPut) {
		final SpaceTimeDiscretization grid = grid(scale, v0);
		final FDMHestonModel model = model(grid, v0, theta, kappa, xi);
		final double[] values = new EuropeanOption(1.0, SPOT, callOrPut).getValue(0.0, model);
		return FiniteDifferenceValueInterpolator.interpolateValue(values, grid, model.getInitialValue());
	}

	private static SpaceTimeDiscretization grid(final int scale, final double v0) {
		return new SpaceTimeDiscretization(
				new Grid[] {new UniformGrid(160 * scale, 0, 400), new UniformGrid(100 * scale, 0, 0.5)},
				new TimeDiscretizationFromArray(0, 80 * scale, 1.0 / (80 * scale)),
				0.5, new double[] {SPOT, v0});
	}

	private static FDMHestonModel model(final SpaceTimeDiscretization grid, final double v0,
			final double theta, final double kappa, final double xi) {
		return new FDMHestonModel(SPOT, v0, curve("r", RATE), curve("q", DIVIDEND),
				kappa, theta, xi, -0.7, grid);
	}

	private static DiscountCurve curve(final String name, final double rate) {
		return DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(
				name, new double[] {0.0, 1.0}, new double[] {1.0, Math.exp(-rate)});
	}
}
