package net.finmath.singleswaprate.products;

import net.finmath.marketdata.products.AnalyticProduct;
import net.finmath.singleswaprate.model.VolatilityCubeModel;

/**
 * The interface of a product to be evaluated using a {@link VolatilityCubeModel}.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface AnalyticVolatilityCubeProduct extends AnalyticProduct {

	/**
	 * Return the valuation of the product using the given model.
	 * The model has to implement the modes of {@link VolatilityCubeModel}.
	 *
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param model The model under which the product is valued.
	 * @return The value of the product using the given model.
	 */
	double getValue(double evaluationTime, VolatilityCubeModel model);
}
