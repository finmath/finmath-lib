package net.finmath.modelling.descriptor;

import java.time.LocalDate;

import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * @author Alessandro Gnoatto
 *
 * @version 1.0
 */
public class VarianceGammaModelDescriptor implements AssetModelDescriptor {

	private final LocalDate referenceDate;

	private final Double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final DiscountCurve discountCurveForDiscountRate;

	private final double sigma;
	private final double theta;
	private final double nu;

	/**
	 * @param referenceDate
	 * @param initialValue
	 * @param discountCurveForForwardRate
	 * @param discountCurveForDiscountRate
	 * @param sigma
	 * @param theta
	 * @param nu
	 */
	public VarianceGammaModelDescriptor(LocalDate referenceDate, Double initialValue,
			DiscountCurve discountCurveForForwardRate, DiscountCurve discountCurveForDiscountRate, double sigma,
			double theta, double nu) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.sigma = sigma;
		this.theta = theta;
		this.nu = nu;
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return "Single asset Variance Gamma model";
	}

	/**
	 * @return the referenceDate
	 */
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	/**
	 * @return the initialValue
	 */
	public Double getInitialValue() {
		return initialValue;
	}

	/**
	 * @return the discountCurveForForwardRate
	 */
	public DiscountCurve getDiscountCurveForForwardRate() {
		return discountCurveForForwardRate;
	}

	/**
	 * @return the discountCurveForDiscountRate
	 */
	public DiscountCurve getDiscountCurveForDiscountRate() {
		return discountCurveForDiscountRate;
	}

	/**
	 * @return the sigma
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * @return the theta
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * @return the nu
	 */
	public double getNu() {
		return nu;
	}

}
