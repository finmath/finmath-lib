package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.FiniteDifferenceModel;
import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;

/**
 * Factory for creating {@link FiniteDifferenceBoundary} instances.
 *
 * <p>
 * The factory creates a boundary class based on naming conventions.
 * Given a product and a model, it constructs the boundary class name
 * as
 * </p>
 *
 * <pre>
 *     &lt;ProductSimpleName&gt;&lt;ModelCoreName&gt;Boundary
 * </pre>
 *
 * <p>
 * where {@code ModelCoreName} is obtained by removing the prefix
 * {@code "FDM"} from the model class name.
 * </p>
 *
 * <p>
 * The boundary class must:
 * </p>
 * <ul>
 *   <li>Be located in the same package as this factory</li>
 *   <li>Provide a constructor accepting the concrete model type</li>
 * </ul>
 *
 * @author Andrea Mazzon
 */
public final class FDBoundaryFactory {

	/**
	 * Creates a {@link FiniteDifferenceBoundary} corresponding to the
	 * given model and product.
	 *
	 * @param model   The finite difference model.
	 * @param product The finite difference product.
	 * @return The corresponding boundary implementation.
	 * @throws IllegalArgumentException If the boundary class cannot be created.
	 */
	public static FiniteDifferenceBoundary createBoundary(
			final FiniteDifferenceModel model,
			final FiniteDifferenceEquityProduct product) {

		final String modelSimpleName = model.getClass().getSimpleName();
		final String modelCoreName = modelSimpleName.replaceFirst("^FDM", "");
		final String packageName = FDBoundaryFactory.class.getPackageName();

		Class<?> productClass = product.getClass();

		while (productClass != null
				&& FiniteDifferenceEquityProduct.class.isAssignableFrom(productClass)) {

			final String boundaryClassName =
					packageName + "."
					+ productClass.getSimpleName()
					+ modelCoreName
					+ "Boundary";

			try {
				final Class<?> boundaryClass = Class.forName(boundaryClassName);
				final var constructor = boundaryClass.getConstructor(model.getClass());
				return (FiniteDifferenceBoundary) constructor.newInstance(model);
			}
			catch (final ClassNotFoundException ignored) {
				productClass = productClass.getSuperclass();
			}
			catch (final ReflectiveOperationException e) {
				throw new IllegalArgumentException(
						"Cannot instantiate boundary " + boundaryClassName
						+ " for model type " + model.getClass()
						+ " and product type " + product.getClass(),
						e
				);
			}
		}

		throw new IllegalArgumentException(
				"Cannot create boundary for model type "
				+ model.getClass()
				+ " and product type "
				+ product.getClass()
		);
	}
}
