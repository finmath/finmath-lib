/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.06.2014
 */

package net.finmath.modelling;

import java.io.Serializable;
import java.util.Map;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.products.AnalyticProductInterface;

/**
 * A product throwing an exception if its <code>getValue</code> method is called.
 *
 * This class can be used to created products trigger an exception only upon valuation (i.e., late).
 *
 * @author Christian Fries
 * @version 1.0
 */
public class UnsupportedProduct implements Product, AnalyticProductInterface, Serializable {

	private static final long serialVersionUID = 5375406324063846793L;
	private final Exception exception;

	/**
	 * Creates an unsupported product throwing an exception if its <code>getValue</code> method is called.
	 *
	 * @param exception The exception to be thrown if this product is evaluated.
	 */
	public UnsupportedProduct(Exception exception) {
		super();
		this.exception = exception;
	}

	@Override
	public Object getValue(double evaluationTime, Model model) {
		throw exception instanceof RuntimeException ? (RuntimeException)exception : new RuntimeException(exception);
	}

	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {
		throw exception instanceof RuntimeException ? (RuntimeException)exception : new RuntimeException(exception);
	}

	@Override
	public Map<String, Object> getValues(double evaluationTime, Model model) {
		throw exception instanceof RuntimeException ? (RuntimeException)exception : new RuntimeException(exception);
	}

	@Override
	public String toString() {
		return "UnsupportedProduct [exception=" + exception + "]";
	}
}
