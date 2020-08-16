package net.finmath.singleswaprate.model.volatilities;

import java.time.LocalDate;

import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.optimizer.SolverException;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.calibration.SABRCubeCalibration;
import net.finmath.singleswaprate.calibration.SABRShiftedSmileCalibration;
import net.finmath.singleswaprate.data.DataTable;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.SchedulePrototype;

/**
 * A factory for all volatility cubes, based on common input.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class VolatilityCubeFactory {

	//input
	private final LocalDate referenceDate;

	private final double displacement;
	private final double beta;

	private final double correlationDecay;
	private final double iborOisDecorrelation;

	private final SwaptionDataLattice cashPayerPremiums;
	private final SwaptionDataLattice cashReceiverPremiums;
	private final SwaptionDataLattice physicalPremiumsATM;

	private final AnnuityMappingType annuityMappingType;

	private int maxIterations = 250;
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();

	private final boolean replicationUseAsOffset = true;
	private double replicationLowerBound   = -0.15;
	private double replicationUpperBound   = 0.15;
	private int replicationNumberOfEvaluationPoints = 500;


	/**
	 * Create the factory.
	 *
	 * @param referenceDate The reference date the cubes will have.
	 * @param cashPayerPremiums The lattice containing market targets for cash settled payer swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param cashReceiverPremiums The lattice containing market targets for cash settled receiver swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param physicalPremiumsATM Table containing physical settled swaption atm premiums.
	 * @param displacement The displacement a cube should use.
	 * @param beta The SABR beta parameter SABR cube should use.
	 * @param correlationDecay The correlation decay parameter a cube should use.
	 * @param iborOisDecorrelation The ibor ois decorrelation a cube should use.
	 * @param annuityMappingType The type of annuity mapping to use when building the cube.
	 */
	public VolatilityCubeFactory(final LocalDate referenceDate, final SwaptionDataLattice cashPayerPremiums, final SwaptionDataLattice cashReceiverPremiums, final SwaptionDataLattice physicalPremiumsATM,
			final double displacement, final double beta, final double correlationDecay, final double iborOisDecorrelation, final AnnuityMappingType annuityMappingType) {
		super();

		this.referenceDate = referenceDate;
		this.cashPayerPremiums = cashPayerPremiums;
		this.cashReceiverPremiums = cashReceiverPremiums;
		this.physicalPremiumsATM = physicalPremiumsATM;
		this.displacement = displacement;
		this.beta = beta;
		this.correlationDecay = correlationDecay;
		this.iborOisDecorrelation = iborOisDecorrelation;
		this.annuityMappingType = annuityMappingType;
	}

	/**
	 * Build a {@link SABRVolatilityCubeParallel} from parameters via {@link SABRVolatilityCubeParallelFactory}.
	 *
	 * @param name The name of the cube.
	 * @param rho The SABR parameter rho.
	 * @param volvol The SABR volvol parameter.
	 * @param physicalATMSwaptions Lattice containing at-the-money values of physically settled swaptions.
	 * @param model The model for context.
	 * @return The cube.
	 */
	public SABRVolatilityCubeParallel buildParallelSABRCube(final String name, final double rho, final double volvol, final SwaptionDataLattice physicalATMSwaptions,
			final VolatilityCubeModel model) {

		final SchedulePrototype fixMetaSchedule 	= cashPayerPremiums.getFixMetaSchedule();
		final SchedulePrototype floatMetaSchedule 	= cashReceiverPremiums.getFloatMetaSchedule();
		final String forwardCurveName 			= cashPayerPremiums.getForwardCurveName();
		return SABRVolatilityCubeParallelFactory.createSABRVolatilityCubeParallel(name, referenceDate, fixMetaSchedule, floatMetaSchedule, displacement, beta, rho, volvol,
				correlationDecay, iborOisDecorrelation, physicalATMSwaptions, model, forwardCurveName);
	}

	/**
	 * Build a {@link SABRVolatilityCube} by calibration via {@link SABRShiftedSmileCalibration}.
	 *
	 * @param name The name of the cube.
	 * @param model The model for context.
	 * @return The calibrated cube.
	 *
	 * @throws SolverException Thrown when solvers fail to find suitable parameters.
	 */
	public SABRVolatilityCube buildShiftedSmileSABRCube(final String name, final VolatilityCubeModel model)
			throws SolverException {

		final SABRShiftedSmileCalibration calibrator = new SABRShiftedSmileCalibration(referenceDate,
				cashPayerPremiums, cashReceiverPremiums, physicalPremiumsATM, model, displacement, beta, correlationDecay, iborOisDecorrelation);
		calibrator.setCalibrationParameters(maxIterations, numberOfThreads);

		return calibrator.build(name);
	}

	/**
	 * Build a {@link SABRVolatilityCube} by calibration via {@link SABRCubeCalibration}.
	 *
	 * @param name The name of the cube.
	 * @param model The model for context.
	 * @param terminations The terminations to calibrate to in each slice.
	 * @return The calibrated cube.
	 *
	 * @throws SolverException Thrown when either the calibration of final or initial parameters (if not provided) fails.
	 */
	public SABRVolatilityCube buildSABRVolatilityCube(final String name, final VolatilityCubeModel model, final int[] terminations) throws SolverException {

		final SABRCubeCalibration calibrator = new SABRCubeCalibration(referenceDate, cashPayerPremiums, cashReceiverPremiums, physicalPremiumsATM, model,
				annuityMappingType,	displacement, beta, correlationDecay, iborOisDecorrelation);
		calibrator.setCalibrationParameters(maxIterations, numberOfThreads);
		calibrator.setReplicationParameters(replicationUseAsOffset, replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);

		return calibrator.calibrate(name, terminations);
	}

	/**
	 * Build a {@link SABRVolatilityCube} by calibration via {@link SABRCubeCalibration}.
	 *
	 * @param name The name of the cube.
	 * @param model The model for context.
	 * @param terminations The terminations to calibrate to in each slice.
	 * @param initialRhos Table containing initial rhos for the calibration.
	 * @param initialBaseVols Table containing initial base volatilities for the calibration.
	 * @param initialVolvols Table containing initial volvols for the calibration.
	 * @return The calibrated cube.
	 *
	 * @throws SolverException Thrown when either the calibration of final or initial parameters (if not provided) fails.
	 */
	public SABRVolatilityCube buildSABRVolatilityCube(final String name, final VolatilityCubeModel model, final int[] terminations,
			final DataTable initialRhos, final DataTable initialBaseVols, final DataTable initialVolvols) throws SolverException {

		final SABRCubeCalibration calibrator = new SABRCubeCalibration(referenceDate, cashPayerPremiums, cashReceiverPremiums, physicalPremiumsATM, model,
				annuityMappingType,	displacement, beta, correlationDecay, iborOisDecorrelation);
		calibrator.setCalibrationParameters(maxIterations, numberOfThreads);
		calibrator.setReplicationParameters(replicationUseAsOffset, replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
		calibrator.setInitialParameters(initialRhos, initialBaseVols, initialVolvols);

		return calibrator.calibrate(name, terminations);
	}

	/**
	 * @return The number of threads for calibration.
	 */
	public int getNumberOfThreads() {
		return numberOfThreads;
	}
	/**
	 * @return The maximum number of iterations during calibration.
	 */
	public int getMaxIterations() {
		return maxIterations;
	}

	/**
	 * Set the parameters for calibration.
	 *
	 * @param maxIterations The maximum number of iterations done during calibration.
	 * @param numberOfThreads The number of processor threads to be used.
	 */
	public void setCalibrationParameters( final int maxIterations, final int numberOfThreads) {
		this.maxIterations		= maxIterations;
		this.numberOfThreads 	= numberOfThreads;
	}

	/**
	 * Set the parameters for replication.
	 *
	 * @param lowerBound The lowest swap rate to be evaluated.
	 * @param upperBound The highest swap rate to be evaluated.
	 * @param numberOfEvaluationPoints The number of points to be evaluated during replication.
	 */
	public void setReplicationParameters(final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		this.replicationLowerBound = lowerBound;
		this.replicationUpperBound = upperBound;
		this.replicationNumberOfEvaluationPoints = numberOfEvaluationPoints;
	}

	/**
	 * @return The lowest swap rate to be evaluated during replication.
	 */
	public double getReplicationLowerBound() {
		return replicationLowerBound;
	}

	/**
	 * @return The highest swap rate to be evaluated during replication.
	 */
	public double getReplicationUpperBound() {
		return replicationUpperBound;
	}

	/**
	 * @return The number of points to be evaluated during replication.
	 */
	public double getReplicationNumberOfEvaluationPoints() {
		return replicationNumberOfEvaluationPoints;
	}

}
