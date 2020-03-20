/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.06.2014
 */

package net.finmath.modelling;

import java.io.Serializable;
import java.util.Map;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.products.AnalyticProduct;

/**
 * A product throwing an exception if its <code>getValue</code> method is called.
 *
 * This class can be used to created products trigger an exception only upon valuation (i.e., late).
 *
 * @author Christian Fries
 * @version 1.0
 */
public class UnsupportedProduct implements Product, AnalyticProduct, Serializable {

	private static final long serialVersionUID = 5375406324063846793L;
	private final Exception exception;

	/**
	 * Creates an unsupported product throwing an exception if its <code>getValue</code> method is called.
	 *
	 * @param exception The exception to be thrown if this product is evaluated.
	 */
	public UnsupportedProduct(final Exception exception) {
		super();
		this.exception = exception;
	}

	@Override
	public Object getValue(final double evaluationTime, final Model model) {
		throw exception instanceof RuntimeException ? (RuntimeException)exception : new RuntimeException(exception);
	}

	@Override
	public double getValue(final double evaluationTime, final AnalyticModel model) {
		throw exception instanceof RuntimeException ? (RuntimeException)exception : new RuntimeException(exception);
	}

	@Override
	public Map<String, Object> getValues(final double evaluationTime, final Model model) {
		throw exception instanceof RuntimeException ? (RuntimeException)exception : new RuntimeException(exception);
	}

	@Override
	public String toString() {
		return "UnsupportedProduct [exception=" + exception + "]";
	}
}
