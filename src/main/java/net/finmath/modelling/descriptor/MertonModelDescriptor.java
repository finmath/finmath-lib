package net.finmath.modelling.descriptor;

import java.time.LocalDate;

import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Descriptor for the Merton Jump Diffusion Model.
 * This provides communication between the Fourier transform framework for calibration and
 * the Monte Carlo engine.
 * @author Alessandro Gnoatto
 *
 */
public class MertonModelDescriptor implements AssetModelDescriptor{

	private final LocalDate referenceDate;

	private final Double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final DiscountCurve discountCurveForDiscountRate;

	private final Double volatility;
	private final Double jumpIntensity;
	private final Double jumpSizeMean;
	private final Double jumpSizeStdDev;

	public MertonModelDescriptor(final LocalDate referenceDate, final Double initialValue,
			final DiscountCurve discountCurveForForwardRate, final DiscountCurve discountCurveForDiscountRate, final Double volatility,
			final Double jumpIntensity, final Double jumpSizeMean, final Double jumpSizeStdDev) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.volatility = volatility;
		this.jumpIntensity = jumpIntensity;
		this.jumpSizeMean = jumpSizeMean;
		this.jumpSizeStdDev = jumpSizeStdDev;
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return "Single asset Merton Jump Diffusion model";
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
	 * @return the volatility
	 */
	public Double getVolatility() {
		return volatility;
	}

	/**
	 * @return the jumpIntensity
	 */
	public Double getJumpIntensity() {
		return jumpIntensity;
	}

	/**
	 * @return the jumpSizeMean
	 */
	public Double getJumpSizeMean() {
		return jumpSizeMean;
	}

	/**
	 * @return the jumpSizeStdDev
	 */
	public Double getJumpSizeStdDev() {
		return jumpSizeStdDev;
	}

}
