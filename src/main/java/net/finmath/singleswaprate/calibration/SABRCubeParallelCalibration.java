package net.finmath.singleswaprate.calibration;

import java.time.LocalDate;

import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.SABRVolatilityCubeParallel;
import net.finmath.singleswaprate.model.volatilities.SABRVolatilityCubeParallelFactory;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;

/**
 * Calibrates a {@link SABRVolatilityCubeParallel}.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SABRCubeParallelCalibration extends AbstractCubeCalibration {

	private double initialCorrelationDecay = 0;
	private double initialIborOisDecorrelation = 1;
	private double initialDisplacement = 0.25;

	private double initialBeta = 0.5;
	private double initialRho = 0.1;
	private double initialVolvol = 0.0001;

	private final SwaptionDataLattice physicalATMSwaptions;

	/**
	 * Create the calibrator.
	 *
	 * @param referenceDate The reference date of the cube.
	 * @param cashPayerPremiums The lattice containing market targets for cash settled payer swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param cashReceiverPremiums The lattice containing market targets for cash settled receiver swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param physicalATMSwaptions Lattice containing at-the-money values of physically settled swaptions.
	 * @param model The model providing context.
	 * @param annuityMappingType The type of annuity mapping to be used for calibration.
	 */
	public SABRCubeParallelCalibration(final LocalDate referenceDate, final SwaptionDataLattice cashPayerPremiums, final SwaptionDataLattice cashReceiverPremiums,
			final SwaptionDataLattice physicalATMSwaptions, final VolatilityCubeModel model, final AnnuityMappingType annuityMappingType) {
		super(referenceDate, cashPayerPremiums, cashReceiverPremiums, model, annuityMappingType);
		this.physicalATMSwaptions = physicalATMSwaptions;
	}

	@Override
	protected VolatilityCube buildCube(final String name, final double[] parameters) {

		return SABRVolatilityCubeParallelFactory.createSABRVolatilityCubeParallel(name, getReferenceDate(), physicalATMSwaptions.getFixMetaSchedule(),
				physicalATMSwaptions.getFloatMetaSchedule(), initialDisplacement, initialBeta, parameters[0], parameters[1],
				initialCorrelationDecay, initialIborOisDecorrelation, physicalATMSwaptions, getModel(), getForwardCurveName());
	}

	@Override
	protected void initializeParameters() {

		setInitialParameters(new double[] { initialRho, initialVolvol });
	}

	@Override
	protected double[] applyParameterBounds(final double[] parameters) {
		final double[] boundedParameters = new double[parameters.length];

		//		boundedParameters[0] = Math.max(0, Math.min(0.99, parameters[0])); // Math.max(0,  parameters[0]);
		boundedParameters[0] = Math.max(-0.999999, Math.min(0.999999, parameters[0])); // parameters[1];
		boundedParameters[1] = Math.max(parameters[1], 0);

		return boundedParameters;
	}

	public double getInitialCorrelationDecay() {
		return initialCorrelationDecay;
	}

	public void setInitialCorrelationDecay(final double initialCorrelationDecay) {
		this.initialCorrelationDecay = initialCorrelationDecay;
	}

	public double getInitialIborOisDecorrelation() {
		return initialIborOisDecorrelation;
	}

	public void setInitialIborOisDecorrelation(final double initialIborOisDecorrelation) {
		this.initialIborOisDecorrelation = initialIborOisDecorrelation;
	}

	public double getInitialDisplacement() {
		return initialDisplacement;
	}

	public void setInitialDisplacement(final double initialDisplacement) {
		this.initialDisplacement = initialDisplacement;
	}

	public double getInitialBeta() {
		return initialBeta;
	}

	public void setInitialBeta(final double initialBeta) {
		this.initialBeta = initialBeta;
	}

	public double getInitialRho() {
		return initialRho;
	}

	public void setInitialRho(final double initialRho) {
		this.initialRho = initialRho;
	}

	public double getInitialVolvol() {
		return initialVolvol;
	}

	public void setInitialVolvol(final double initialVolvol) {
		this.initialVolvol = initialVolvol;
	}


}
