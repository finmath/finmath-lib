package net.finmath.finitedifference.utilities;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;

/**
 * Layout helper for finite-difference value vectors on multi-dimensional grids.
 *
 * <p>
 * Finite-difference products return values as flattened spatial vectors. This
 * class centralizes the flattening convention used by the library. For a
 * spatial grid with shape {@code n0, n1, ..., nd}, the flattened index is
 * </p>
 *
 * <pre>
 * i0 + n0 * i1 + n0 * n1 * i2 + ...
 * </pre>
 *
 * <p>
 * The layout is inferred from a {@link SpaceTimeDiscretization}. It can be used
 * to validate value vectors and value surfaces, and to convert between
 * multi-index coordinates and flattened vector indices.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class FiniteDifferenceGridLayout {

	/**
	 * The discretization.
	 */
	private final SpaceTimeDiscretization discretization;
	/**
	 * The dimension.
	 */
	private final int dimension;
	/**
	 * The shape.
	 */
	private final int[] shape;
	/**
	 * The strides.
	 */
	private final int[] strides;
	/**
	 * The number of spatial points.
	 */
	private final int numberOfSpatialPoints;

	/**
	 * Creates a grid layout from a space-time discretization.
	 *
	 * @param discretization The space-time discretization.
	 * @throws IllegalArgumentException Thrown if {@code discretization} is
	 *         {@code null} or if one of its spatial grids is empty.
	 */
	public FiniteDifferenceGridLayout(final SpaceTimeDiscretization discretization) {
		if (discretization == null) {
			throw new IllegalArgumentException("discretization must not be null.");
		}

		this.discretization = discretization;
		this.dimension = discretization.getNumberOfSpaceGrids();

		if (dimension <= 0) {
			throw new IllegalArgumentException("The discretization must contain at least one spatial grid.");
		}

		shape = new int[dimension];
		strides = new int[dimension];

		int product = 1;
		for (int i = 0; i < dimension; i++) {
			final int length = discretization.getSpaceGrid(i).getGrid().length;

			if (length <= 0) {
				throw new IllegalArgumentException("Spatial grids must not be empty.");
			}

			shape[i] = length;
			strides[i] = product;
			product *= length;
		}

		numberOfSpatialPoints = product;
	}

	/**
	 * Creates a grid layout from a space-time discretization.
	 *
	 * @param discretization The space-time discretization.
	 * @return The corresponding grid layout.
	 */
	public static FiniteDifferenceGridLayout of(final SpaceTimeDiscretization discretization) {
		return new FiniteDifferenceGridLayout(discretization);
	}

	/**
	 * Returns the underlying space-time discretization.
	 *
	 * @return The space-time discretization.
	 */
	public SpaceTimeDiscretization getDiscretization() {
		return discretization;
	}

	/**
	 * Returns the number of spatial dimensions.
	 *
	 * @return The number of spatial dimensions.
	 */
	public int getDimension() {
		return dimension;
	}

	/**
	 * Returns the spatial shape.
	 *
	 * @return A copy of the spatial shape.
	 */
	public int[] getShape() {
		return shape.clone();
	}

	/**
	 * Returns the number of grid points in a given spatial dimension.
	 *
	 * @param dimension The spatial dimension.
	 * @return The number of grid points in the given dimension.
	 */
	public int getShape(final int dimension) {
		validateDimension(dimension);
		return shape[dimension];
	}

	/**
	 * Returns the flattening stride for a given spatial dimension.
	 *
	 * @param dimension The spatial dimension.
	 * @return The flattening stride.
	 */
	public int getStride(final int dimension) {
		validateDimension(dimension);
		return strides[dimension];
	}

	/**
	 * Returns the total number of spatial grid points.
	 *
	 * @return The total number of spatial grid points.
	 */
	public int getNumberOfSpatialPoints() {
		return numberOfSpatialPoints;
	}

	/**
	 * Flattens a multi-dimensional spatial index.
	 *
	 * @param indices The spatial indices, one per dimension.
	 * @return The flattened vector index.
	 * @throws IllegalArgumentException Thrown if the number of indices does not
	 *         match the spatial dimension or if an index is out of range.
	 */
	public int flatten(final int... indices) {
		if (indices == null || indices.length != dimension) {
			throw new IllegalArgumentException("The number of indices must match the spatial dimension.");
		}

		int flatIndex = 0;
		for (int i = 0; i < dimension; i++) {
			if (indices[i] < 0 || indices[i] >= shape[i]) {
				throw new IllegalArgumentException("Spatial index out of range.");
			}

			flatIndex += indices[i] * strides[i];
		}

		return flatIndex;
	}

	/**
	 * Converts a flattened vector index into a multi-dimensional spatial index.
	 *
	 * @param flatIndex The flattened vector index.
	 * @return The corresponding spatial multi-index.
	 * @throws IllegalArgumentException Thrown if {@code flatIndex} is out of
	 *     range.
	 */
	public int[] unflatten(final int flatIndex) {
		if (flatIndex < 0 || flatIndex >= numberOfSpatialPoints) {
			throw new IllegalArgumentException("flatIndex out of range.");
		}

		final int[] indices = new int[dimension];
		int remainingIndex = flatIndex;

		for (int i = dimension - 1; i >= 0; i--) {
			indices[i] = remainingIndex / strides[i];
			remainingIndex -= indices[i] * strides[i];
		}

		return indices;
	}

	/**
	 * Validates that a value vector matches the spatial layout.
	 *
	 * @param values The value vector.
	 * @throws IllegalArgumentException Thrown if {@code values} is {@code null}
	 *         or if its length does not match the number of spatial points.
	 */
	public void validateVector(final double[] values) {
		if (values == null) {
			throw new IllegalArgumentException("values must not be null.");
		}
		if (values.length != numberOfSpatialPoints) {
			throw new IllegalArgumentException(
					"Value vector length does not match the spatial grid layout."
			);
		}
	}

	/**
	 * Validates that a value surface matches the spatial layout.
	 *
	 * @param values The value surface indexed by flattened space point and time
	 *     index.
	 * @throws IllegalArgumentException Thrown if {@code values} is {@code null}
	 * or if its first dimension does not match the number of spatial points.
	 */
	public void validateSurface(final double[][] values) {
		if (values == null) {
			throw new IllegalArgumentException("values must not be null.");
		}
		if (values.length != numberOfSpatialPoints) {
			throw new IllegalArgumentException(
					"Value surface first dimension does not match the spatial grid layout."
			);
		}
		if (values.length > 0 && values[0] == null) {
			throw new IllegalArgumentException("Value surface rows must not be null.");
		}

		final int numberOfTimes = values.length == 0 ? 0 : values[0].length;
		for (int i = 1; i < values.length; i++) {
			if (values[i] == null) {
				throw new IllegalArgumentException("Value surface rows must not be null.");
			}
			if (values[i].length != numberOfTimes) {
				throw new IllegalArgumentException("Value surface must be rectangular.");
			}
		}
	}

	private void validateDimension(final int dimension) {
		if (dimension < 0 || dimension >= this.dimension) {
			throw new IllegalArgumentException("dimension out of range.");
		}
	}
}
