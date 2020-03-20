/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.descriptor;

import java.time.LocalDate;

import net.finmath.modelling.SingleAssetProductDescriptor;

/**
 * Describes a European option.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class SingleAssetEuropeanOptionProductDescriptor implements SingleAssetProductDescriptor {

	private static final String productName = "Single asset European option";

	private final String 	underlyingName;
	private final LocalDate maturity;
	private final double 	strike;

	public SingleAssetEuropeanOptionProductDescriptor(final String underlyingName, final LocalDate maturity, final double strike) {
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
	public LocalDate getMaturity() {
		return maturity;
	}

	/**
	 * @return Strike
	 */
	public Double getStrike() {
		return strike;
	}
}
