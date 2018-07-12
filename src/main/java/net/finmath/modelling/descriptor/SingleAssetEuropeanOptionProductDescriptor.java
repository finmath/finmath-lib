/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.descriptor;

import net.finmath.modelling.SingleAssetProductDescriptor;

/**
 * @author Christian Fries
 *
 */
public class SingleAssetEuropeanOptionProductDescriptor implements SingleAssetProductDescriptor {

	private static final String productName = "Single asset European option";

	private final String underlyingName;
	private final Double maturity;
	private final Double strike;

	public SingleAssetEuropeanOptionProductDescriptor(String underlyingName, Double maturity, Double strike) {
		super();
		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return productName;
	}

	/**
	 * @return Name of the underlying.
	 */
	public String getUnderlyingName() {
		return underlyingName;
	}

	/**
	 * @return Maturity as double.
	 */
	public Double getMaturity() {
		return maturity;
	}

	/**
	 * @return Strike
	 */
	public Double getStrike() {
		return strike;
	}
}
