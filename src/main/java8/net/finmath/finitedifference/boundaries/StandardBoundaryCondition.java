package net.finmath.finitedifference.boundaries;

/**
 * Standard immutable implementation of {@link BoundaryCondition}.
 *
 * @author Alessandro Gnoatto
 */
public final class StandardBoundaryCondition implements BoundaryCondition {

	/**
	 * The none.
	 */
	private static final StandardBoundaryCondition NONE =
			new StandardBoundaryCondition(BoundaryConditionType.NONE, Double.NaN);

	/**
	 * The type.
	 */
	private final BoundaryConditionType type;
	/**
	 * The value.
	 */
	private final double value;

	private StandardBoundaryCondition(final BoundaryConditionType type, final double value) {
		this.type = type;
		this.value = value;
	}

	/**
	 * Creates a Dirichlet boundary condition.
	 *
	 * @param value The prescribed boundary value.
	 * @return A Dirichlet boundary condition.
	 */
	public static StandardBoundaryCondition dirichlet(final double value) {
		return new StandardBoundaryCondition(BoundaryConditionType.DIRICHLET, value);
	}

	/**
	 * Creates a boundary condition representing "do not overwrite the PDE row".
	 *
	 * @return A NONE boundary condition.
	 */
	public static StandardBoundaryCondition none() {
		return NONE;
	}

	@Override
	public BoundaryConditionType getType() {
		return type;
	}

	@Override
	public double getValue() {
		return value;
	}
}
