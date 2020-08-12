package net.finmath.singleswaprate.model.volatilities;

import java.time.LocalDate;
import java.util.ArrayList;

import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.singleswaprate.data.DataTable;
import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.singleswaprate.data.DataTableInterpolated;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * Build a {@link SABRVolatilityCubeParallel} from given shared parameters and marketdata.
 * This factory does not calibrate the cube, instead with the given parameters a SABR smile is being build onto every node on the tenor grid.
 * The market date is used to adjust the smile to match market values at atm level.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SABRVolatilityCubeParallelFactory {

	//input
	private final String cubeName;
	private final LocalDate referenceDate;
	private final SchedulePrototype floatMetaSchedule;
	private final SchedulePrototype   fixMetaSchedule;


	private final double sabrBeta;
	private final double sabrDisplacement;

	private final double sabrRho;
	private final double sabrVolvol;
	private final double correlationDecay;
	private final double iborOisDecorrelation;

	private final VolatilityCubeModel model;
	private final String forwardCurveName;

	// calculations
	private DataTable baseVolTable;
	private DataTable swapRateTable;

	private final SwaptionDataLattice physicalATMSwaptionsVolatilities;


	/**
	 * Build a {@link SABRVolatilityCubeParallel} from given shared parameters and marketdata.
	 * This factory does not calibrate the cube, instead with the given parameters a SABR smile is being build onto every node on the tenor grid.
	 * The market date is used to adjust the smile to match market values at atm level.
	 *
	 * @param cubeName The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param fixMetaSchedule The schedule meta data to use on the fix legs to match the target market data.
	 * @param floatMetaSchedule The schedule meta data to use on the float legs to match the target market data.
	 * @param sabrDisplacement The displacement of the SABR curves of the cube.
	 * @param sabrBeta The SABR beta parameter of the cube.
	 * @param sabrRho The SABR rho parameter of the cube.
	 * @param sabrVolvol The SABR volvol parameter of the cube.
	 * @param correlationDecay The correlation decay parameter of the cube.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter of the cube.
	 * @param physicalATMSwaptions Lattice containing at-the-money values of physically settled swaptions.
	 * @param model The model for context.
	 * @param forwardCurveName The name of the forward curve to use, when fitting the atm level.
	 * @return The cube.
	 */
	public static SABRVolatilityCubeParallel createSABRVolatilityCubeParallel(final String cubeName, final LocalDate referenceDate, final SchedulePrototype fixMetaSchedule,
			final SchedulePrototype floatMetaSchedule, final double sabrDisplacement, final double sabrBeta, final double sabrRho, final double sabrVolvol, final double correlationDecay, final double iborOisDecorrelation,
			final SwaptionDataLattice physicalATMSwaptions, final VolatilityCubeModel model, final String forwardCurveName) {

		final SABRVolatilityCubeParallelFactory factory = new SABRVolatilityCubeParallelFactory(cubeName, referenceDate, fixMetaSchedule, floatMetaSchedule, sabrDisplacement, sabrBeta,
				sabrRho, sabrVolvol, correlationDecay, iborOisDecorrelation, physicalATMSwaptions.convertLattice(QuotingConvention.PAYERVOLATILITYNORMAL, model), model,
				forwardCurveName);

		return factory.buildParallel();
	}

	/**
	 * Private constructor.
	 *
	 * @param cubeName The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param fixMetaSchedule The schedule meta data to use on the fix legs to match the target market data.
	 * @param floatMetaSchedule The schedule meta data to use on the float legs to match the target market data.
	 * @param sabrDisplacement The displacement of the SABR curves of the cube.
	 * @param sabrBeta The SABR beta parameter of the cube.
	 * @param sabrRho The SABR rho parameter of the cube.
	 * @param sabrVolvol The SABR volvol parameter of the cube.
	 * @param correlationDecay The correlation decay parameter of the cube.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter of the cube.
	 * @param physicalATMSwaptionVolatilities Lattice containing at-the-money normal volatilities of physically settled swaptions.
	 * @param model The model for context.
	 * @param forwardCurveName The name of the forward curve to use, when fitting the atm level.
	 */
	private SABRVolatilityCubeParallelFactory(final String cubeName, final LocalDate referenceDate, final SchedulePrototype fixMetaSchedule, final SchedulePrototype floatMetaSchedule,
			final double sabrDisplacement, final double sabrBeta, final double sabrRho, final double sabrVolvol, final double correlationDecay, final double iborOisDecorrelation,
			final SwaptionDataLattice physicalATMSwaptionVolatilities, final VolatilityCubeModel model, final String forwardCurveName) {
		super();
		this.cubeName = cubeName;
		this.referenceDate = referenceDate;
		this.fixMetaSchedule = fixMetaSchedule;
		this.floatMetaSchedule = floatMetaSchedule;
		this.sabrBeta = sabrBeta;
		this.sabrDisplacement = sabrDisplacement;
		this.sabrRho = sabrRho;
		this.sabrVolvol = sabrVolvol;
		this.correlationDecay = correlationDecay;
		this.iborOisDecorrelation = iborOisDecorrelation;
		this.physicalATMSwaptionsVolatilities = physicalATMSwaptionVolatilities;
		this.model = model;
		this.forwardCurveName = forwardCurveName;
	}

	/**
	 * Build the cube.
	 *
	 * @return The cube.
	 */
	private SABRVolatilityCubeParallel buildParallel() {

		swapRateTable = makeSwapRateTable();
		baseVolTable = makeBaseVolTable();

		return new SABRVolatilityCubeParallel(cubeName, referenceDate, swapRateTable, sabrDisplacement, sabrBeta, sabrRho, sabrVolvol,
				baseVolTable, correlationDecay, iborOisDecorrelation);
	}

	/**
	 * @return Swap rate table as underlying for the cube.
	 */
	private DataTable makeSwapRateTable() {

		final ArrayList<Integer> maturitiesList	= new ArrayList<>();
		final ArrayList<Integer> terminationsList	= new ArrayList<>();
		final ArrayList<Double> swapRateList		= new ArrayList<>();

		for(final int maturity : physicalATMSwaptionsVolatilities.getMaturities(0)) {
			for(final int termination : physicalATMSwaptionsVolatilities.getTenors(0, maturity)) {
				maturitiesList.add(maturity);
				terminationsList.add(termination);

				final LocalDate maturityDate = referenceDate.plusMonths(maturity);
				final LocalDate terminationDate = maturityDate.plusMonths(termination);

				final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				final double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

				swapRateList.add(swapRate);
			}
		}

		return new DataTableInterpolated("Swap Rates", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturitiesList, terminationsList,
				swapRateList);
	}

	/**
	 * Create a table of base volatilities for the SABR smiles to match the market observed atm volatilities.
	 *
	 * @return Table of base volatilities.
	 */
	private DataTable makeBaseVolTable() {

		final int[] maturitiesArray = new int[swapRateTable.size()];
		final int[] terminationsArray = new int[swapRateTable.size()];
		final double[] valuesArray = new double[swapRateTable.size()];

		int index = 0;
		for(final int maturity : swapRateTable.getMaturities()) {
			for(final int termination : swapRateTable.getTerminationsForMaturity(maturity)) {
				maturitiesArray[index] = maturity;
				terminationsArray[index] = termination;

				valuesArray[index++] = 0.01;
			}
		}

		final DataTableInterpolated tempTable = new DataTableInterpolated("Temp Volatilities", TableConvention.MONTHS, referenceDate, floatMetaSchedule,
				maturitiesArray, terminationsArray, valuesArray);

		final VolatilityCube tempCube = new SABRVolatilityCubeParallel("tempCube", referenceDate, swapRateTable, sabrDisplacement, sabrBeta, sabrRho,
				sabrVolvol, tempTable, correlationDecay, iborOisDecorrelation);

		index = 0;
		for(final int maturity : swapRateTable.getMaturities()) {
			for(final int termination : swapRateTable.getTerminationsForMaturity(maturity)) {

				final LocalDate maturityDate = referenceDate.plusMonths(maturity);
				final LocalDate terminationDate = maturityDate.plusMonths(termination);

				final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				final double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

				final double matFraction = floatSchedule.getPeriodStart(0);
				final double termFraction = floatSchedule.getPeriodEnd(floatSchedule.getNumberOfPeriods()-1);
				valuesArray[index++] = 0.01 * physicalATMSwaptionsVolatilities.getValue(maturity, termination, 0)
						/ tempCube.getValue(termFraction, matFraction, swapRate, net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention.VOLATILITYNORMAL);
			}
		}

		return new DataTableInterpolated("Base Volatilities", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturitiesArray, terminationsArray,
				valuesArray);
	}
}
