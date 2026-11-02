package net.finmath.interpolation;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;

public class RationalFunctionInterpolationTest {

	@Test
	public void smoothMethodsReproduceAffineFunctions() {
		final double[] points = { -2, 0, 1, 3, 5 };
		final double[] values = { -3, 1, 3, 7, 11 };
		for(final InterpolationMethod method : new InterpolationMethod[] { InterpolationMethod.LINEAR,
				InterpolationMethod.CUBIC_SPLINE, InterpolationMethod.AKIMA, InterpolationMethod.AKIMA_CONTINUOUS,
				InterpolationMethod.HARMONIC_SPLINE, InterpolationMethod.HARMONIC_SPLINE_WITH_MONOTONIC_FILTERING }) {
			final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(points, values, method, ExtrapolationMethod.DEFAULT);
			for(final double point : new double[] { -3, -2, -1, 0, 0.5, 1, 2, 3, 4, 5, 6 }) {
				assertEquals(method + " at " + point, 2 * point + 1, interpolation.getValue(point), 1E-12);
			}
		}
	}

	@Test
	public void akimaMethodsReproduceQuadraticFunctionsOnAnEquidistantGrid() {
		final double[] points = { 0, 1, 2, 3, 4, 5 };
		final double[] values = { 0, 1, 4, 9, 16, 25 };
		for(final InterpolationMethod method : new InterpolationMethod[] { InterpolationMethod.AKIMA, InterpolationMethod.AKIMA_CONTINUOUS }) {
			final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(points, values, method, ExtrapolationMethod.DEFAULT);
			for(final double point : new double[] { 0.25, 0.5, 1.5, 2.5, 3.5, 4.75 }) {
				assertEquals(method + " at " + point, point * point, interpolation.getValue(point), 1E-12);
			}
		}
	}

	@Test
	public void piecewiseConstantMethodsUseTheSelectedEndpointExceptAtKnots() {
		for(final InterpolationMethod method : new InterpolationMethod[] { InterpolationMethod.PIECEWISE_CONSTANT,
				InterpolationMethod.PIECEWISE_CONSTANT_LEFTPOINT, InterpolationMethod.PIECEWISE_CONSTANT_RIGHTPOINT }) {
			final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(new double[] { 0, 1, 3 },
					new double[] { 1, 3, 2 }, method, ExtrapolationMethod.CONSTANT);
			final boolean useRightEndpoint = method == InterpolationMethod.PIECEWISE_CONSTANT_RIGHTPOINT;
			assertEquals(useRightEndpoint ? 3 : 1, interpolation.getValue(0.5), 0.0);
			assertEquals(useRightEndpoint ? 2 : 3, interpolation.getValue(2), 0.0);
			assertEquals(1, interpolation.getValue(0), 0.0);
			assertEquals(3, interpolation.getValue(1), 0.0);
			assertEquals(2, interpolation.getValue(3), 0.0);
		}
	}

	@Test
	public void extrapolationUsesTheChosenBoundaryRule() {
		final double[] points = { 0, 1, 3 };
		final double[] values = { 1, 3, 2 };
		final RationalFunctionInterpolation adjacentInterval = new RationalFunctionInterpolation(points, values,
				InterpolationMethod.PIECEWISE_CONSTANT_LEFTPOINT, ExtrapolationMethod.DEFAULT);
		final RationalFunctionInterpolation constant = new RationalFunctionInterpolation(points, values,
				InterpolationMethod.PIECEWISE_CONSTANT_LEFTPOINT, ExtrapolationMethod.CONSTANT);
		final RationalFunctionInterpolation linear = new RationalFunctionInterpolation(points, values,
				InterpolationMethod.PIECEWISE_CONSTANT_LEFTPOINT, ExtrapolationMethod.LINEAR);

		assertEquals(1, adjacentInterval.getValue(-1), 0.0);
		assertEquals(3, adjacentInterval.getValue(4), 0.0);
		assertEquals(1, constant.getValue(-1), 0.0);
		assertEquals(2, constant.getValue(4), 0.0);
		assertEquals(-1, linear.getValue(-1), 0.0);
		assertEquals(1.5, linear.getValue(4), 0.0);
	}

	@Test
	public void akimaMethodsInterpolateTwoPoints() {
		for(final InterpolationMethod method : new InterpolationMethod[] { InterpolationMethod.AKIMA, InterpolationMethod.AKIMA_CONTINUOUS }) {
			final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(new double[] { 0, 2 },
					new double[] { 1, 5 }, method, ExtrapolationMethod.DEFAULT);
			assertEquals(2, interpolation.getValue(0.5), 1E-12);
			assertEquals(4, interpolation.getValue(1.5), 1E-12);
		}
	}

	@Test
	public void serializationPreservesLazyAndInitializedInterpolants() throws Exception {
		for(final boolean initialized : new boolean[] { false, true }) {
			final RationalFunctionInterpolation original = new RationalFunctionInterpolation(new double[] { 0, 1, 2 }, new double[] { 1, 3, 5 });
			if(initialized) {
				assertEquals(2, original.getValue(0.5), 0.0);
			}
			final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			try(ObjectOutputStream output = new ObjectOutputStream(bytes)) {
				output.writeObject(original);
			}
			try(ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
				final RationalFunctionInterpolation restored = (RationalFunctionInterpolation) input.readObject();
				assertEquals(InterpolationMethod.LINEAR, restored.getInterpolationMethod());
				assertEquals(2, restored.applyAsDouble(0.5), 0.0);
				assertEquals(5, restored.getValue(2), 0.0);
				assertEquals(7, restored.getValue(3), 0.0);
			}
		}
	}
}
