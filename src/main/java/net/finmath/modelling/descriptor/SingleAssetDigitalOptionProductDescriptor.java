package net.finmath.modelling.descriptor;

import net.finmath.modelling.SingleAssetProductDescriptor;

/**
 * Describes a European digital option.
 *
 * @author Christian Fries
 * @author Roland Bachl
 * @version 1.0
 */
public class SingleAssetDigitalOptionProductDescriptor implements SingleAssetProductDescriptor {

	private static final String productName = "Single asset Digital option";

	private final double maturity;
	private final double strike;
	private final String nameOfUnderlying;

	public SingleAssetDigitalOptionProductDescriptor(String nameOfUnderlying, Double maturity, Double strike) {
		super();
		this.nameOfUnderlying = nameOfUnderlying;
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

	public double getMaturity() {
		return maturity;
	}

	public double getStrike() {
		return strike;
	}

	public String getNameOfUnderlying() {
		return nameOfUnderlying;
	}

}
