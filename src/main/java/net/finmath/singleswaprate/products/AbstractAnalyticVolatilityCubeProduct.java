package net.finmath.singleswaprate.products;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.modelling.Model;
import net.finmath.singleswaprate.model.VolatilityCubeModel;

/**
 * Abstract layer between interface and implementation, which ensures compatibility of model and product.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public abstract class AbstractAnalyticVolatilityCubeProduct implements AnalyticVolatilityCubeProduct {

	@Override
	public double getValue(final double evaluationTime, final AnalyticModel model) {
		throw new IllegalArgumentException("The product " + this.getClass()
		+ " cannot be valued against a model " + model.getClass() + "."
		+ "It requires a model of type " + VolatilityCubeModel.class + ".");
	}

	@Override
	public Object getValue(final double evaluationTime, final Model model) {
		throw new IllegalArgumentException("The product " + this.getClass()
		+ " cannot be valued against a model " + model.getClass() + "."
		+ "It requires a model of type " + VolatilityCubeModel.class + ".");
	}

	/**
	 * Return the valuation of the product at time 0 using the given model.
	 * The model has to implement the modes of {@link VolatilityCubeModel}.
	 *
	 * @param model The model under which the product is valued.
	 * @return The value of the product using the given model.
	 */
	public double getValue(final VolatilityCubeModel model) {
		return getValue(0.0, model);
	}

}
