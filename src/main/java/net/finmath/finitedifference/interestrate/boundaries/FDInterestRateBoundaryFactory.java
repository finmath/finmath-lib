package net.finmath.finitedifference.interestrate.boundaries;

import net.finmath.finitedifference.FiniteDifferenceModel;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;

/**
 * Factory for creating finite-difference interest-rate boundary instances.
 *
 * <p>
 * The factory creates a boundary class based on naming conventions.
 * Given a product and a model, it constructs the boundary class name as
 * </p>
 *
 * <pre>
 *     &lt;ProductSimpleName&gt;&lt;ModelCoreName&gt;Boundary
 * </pre>
 *
 * <p>
 * where {@code ModelCoreName} is obtained by removing the prefix {@code "FDM"}
 * from the model class name.
 * </p>
 *
 * <p>
 * The boundary class must:
 * </p>
 * <ul>
 *   <li>be located in the same package as this factory,</li>
 *   <li>provide a constructor accepting the concrete model type.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public final class FDInterestRateBoundaryFactory {

	private FDInterestRateBoundaryFactory() {
		// Utility class
	}

	/**
	 * Creates a boundary corresponding to the given model and product.
	 *
	 * @param model The finite-difference model.
	 * @param product The finite-difference interest-rate product.
	 * @return The corresponding boundary implementation.
	 */
	public static FiniteDifferenceInterestRateBoundary createBoundary(
			final FiniteDifferenceModel model,
			final FiniteDifferenceInterestRateProduct product) {

		try {
			final String productSimpleName = product.getClass().getSimpleName();
			final String modelSimpleName = model.getClass().getSimpleName();

			final String modelCoreName = modelSimpleName.replace("FDM", "");
			final String boundarySimpleName = productSimpleName + modelCoreName + "Boundary";

			final String packageName = FDInterestRateBoundaryFactory.class.getPackageName();
			final String boundaryClassName = packageName + "." + boundarySimpleName;

			final Class<?> boundaryClass = Class.forName(boundaryClassName);
			final var constructor = boundaryClass.getConstructor(model.getClass());

			return (FiniteDifferenceInterestRateBoundary) constructor.newInstance(model);
		} catch (final ReflectiveOperationException exception) {
			throw new IllegalArgumentException(
					"Cannot create interest-rate boundary for model type "
							+ model.getClass()
							+ " and product type "
							+ product.getClass(),
					exception
			);
		}
	}
}
