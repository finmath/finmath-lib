package net.finmath.singleswaprate.calibration;

import java.time.LocalDate;

import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.StaticVolatilityCube;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;

/**
 * Calibration for a simple cube that only provides a single value at all coordinates.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class StaticCubeCalibration extends AbstractCubeCalibration {

	private double initialValue = 0.005;
	private double initialCorrelationDecay = 0;
	private final double initialIborOisDecorrelation = 1;

	/**
	 * Create the calibrator.
	 *
	 * @param referenceDate The reference date of the cube.
	 * @param cashPayerPremiums The lattice containing market targets for cash settled payer swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param cashReceiverPremiums The lattice containing market targets for cash settled receiver swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param model The model providing context.
	 * @param annuityMappingType The type of annuity mapping to be used for calibration.
	 */
	public StaticCubeCalibration(final LocalDate referenceDate, final SwaptionDataLattice cashPayerPremiums, final SwaptionDataLattice cashReceiverPremiums,
			final VolatilityCubeModel model, final AnnuityMappingType annuityMappingType) {
		super(referenceDate, cashPayerPremiums, cashReceiverPremiums, model, annuityMappingType);
	}

	/**
	 * Create the calibrator.
	 *
	 * @param referenceDate The reference date of the cube.
	 * @param cashPayerPremiums The lattice containing market targets for cash settled payer swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param cashReceiverPremiums The lattice containing market targets for cash settled receiver swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param model The model providing context.
	 * @param annuityMappingType The type of annuity mapping to be used for calibration.
	 * @param initialValue The value to start the calibration at.
	 * @param initialCorrelationDecay The correlation decay to start the calibration at.
	 */
	public StaticCubeCalibration(final LocalDate referenceDate, final SwaptionDataLattice cashPayerPremiums, final SwaptionDataLattice cashReceiverPremiums,
			final VolatilityCubeModel model, final AnnuityMappingType annuityMappingType, final double initialValue, final double initialCorrelationDecay) {
		super(referenceDate, cashPayerPremiums, cashReceiverPremiums, model, annuityMappingType);

		this.initialValue = initialValue;
		this.initialCorrelationDecay = initialCorrelationDecay;
	}

	@Override
	protected VolatilityCube buildCube(final String cubeName, final double[] parameters) {
		return new StaticVolatilityCube(cubeName, getReferenceDate(), parameters[1], parameters[0]);
	}

	@Override
	protected void initializeParameters() {

		setInitialParameters(new double[]{initialValue, initialCorrelationDecay, initialIborOisDecorrelation});

	}

	@Override
	protected double[] applyParameterBounds(final double[] parameters) {
		final double[] boundedParameters = new double[parameters.length];
		boundedParameters[0] = Math.max(parameters[0], 0);
		boundedParameters[1] = Math.max(parameters[1], 0);
		boundedParameters[2] = parameters[2];
		return boundedParameters;
	}

}
