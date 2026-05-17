/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import net.finmath.montecarlo.interestrate.products.AbstractTermStructureMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * A class for calibration products, that is a triple (P,V,w) where P is a product, V is a target value and w is a weight.
 *
 * @author Christian Fries
 */
public class CalibrationProduct {

	private final String 	name;
	private final AbstractTermStructureMonteCarloProduct		product;
	private final RandomVariable						targetValue;
	private final double								weight;
	private final Integer								priority;

	/**
	 * Construct a calibration product. A calibration product consists of a product implementing {@link AbstractTermStructureMonteCarloProduct}, a target value
	 * given as {@link RandomVariable} and a weight. In addition you may give a short name which may be printed by the logger.
	 *
	 * @param name A short name (for example a "SYMBOL" representing the product.
	 * @param product The product.
	 * @param targetValue The target value.
	 * @param weight The calibration weight.
	 * @param priority The priority (may be used when submitting the product for calculation)
	 */
	public CalibrationProduct(final String name, final AbstractTermStructureMonteCarloProduct product, final RandomVariable targetValue, final double weight, final int priority) {
		super();
		this.name			= name;
		this.product		= product;
		this.targetValue	= targetValue;
		this.weight			= weight;
		this.priority		= priority;
	}

	public CalibrationProduct(final String name, final AbstractTermStructureMonteCarloProduct product, final RandomVariable targetValue, final double weight) {
		this(name, product, targetValue, weight, 0);
	}

	public CalibrationProduct(final String name, final AbstractTermStructureMonteCarloProduct product, final double targetValue, final double weight) {
		this(name, product, new Scalar(targetValue), weight);
	}

	public CalibrationProduct(final AbstractTermStructureMonteCarloProduct product, final RandomVariable targetValue, final double weight) {
		this(null, product, targetValue, weight);
	}

	public CalibrationProduct(final AbstractTermStructureMonteCarloProduct product, final double targetValue, final double weight) {
		this(product, new Scalar(targetValue), weight);
	}

	/**
	 * The method returns a short name for this calibration product. If no short name was given, the method returns <code>getProduct().toString()</code>.
	 *
	 * @return A short name for this calibration product. If no short name was given, the method returns <code>getProduct().toString()</code>
	 */
	public String getName() {
		if(name != null) {
			return name;
		} else {
			return product.toString();
		}
	}

	/**
	 * @return the product.
	 */
	public AbstractTermStructureMonteCarloProduct getProduct() {
		return product;
	}

	/**
	 * @return the target value.
	 */
	public RandomVariable getTargetValue() {
		return targetValue;
	}

	/**
	 * @return the calibrationWeight
	 */
	public double getWeight() {
		return weight;
	}

	/**
	 * @return the priority
	 */
	public Integer getPriority() {
		return priority;
	}

	@Override
	public String toString() {
		return "CalibrationProduct [product=" + getProduct()
		+ ", targetValue=" + getTargetValue()
		+ ", weight=" + getWeight() + "]";
	}
}
