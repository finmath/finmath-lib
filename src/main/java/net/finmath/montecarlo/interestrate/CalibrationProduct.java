/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.Scalar;

/**
 * A class for calibration products, that is a tripple (P,V,w) where P is a product, V is a target value and w is a weight.
 *
 * @author Christian Fries
 */
public class CalibrationProduct {

	private final AbstractLIBORMonteCarloProduct		product;
	private final RandomVariableInterface				targetValue;
	private final double								weight;

	public CalibrationProduct(AbstractLIBORMonteCarloProduct product, RandomVariableInterface targetValue, double weight) {
		super();
		this.product		= product;
		this.targetValue	= targetValue;
		this.weight			= weight;
	}

	public CalibrationProduct(AbstractLIBORMonteCarloProduct product, double targetValue, double weight) {
		super();
		this.product		= product;
		this.targetValue	= new Scalar(targetValue);
		this.weight			= weight;
	}

	/**
	 * @return the product.
	 */
	public AbstractLIBORMonteCarloProduct getProduct() {
		return product;
	}

	/**
	 * @return the target value.
	 */
	public RandomVariableInterface getTargetValue() {
		return targetValue;
	}

	/**
	 * @return the calibrationWeight
	 */
	public double getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return "CalibrationProduct [product=" + getProduct()
		+ ", targetValue=" + getTargetValue()
		+ ", weight=" + getWeight() + "]";
	}
}
