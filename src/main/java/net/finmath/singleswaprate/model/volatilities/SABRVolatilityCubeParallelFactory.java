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
	private String cubeName;
	private LocalDate referenceDate;
	private SchedulePrototype floatMetaSchedule;
	private SchedulePrototype   fixMetaSchedule;


	private double sabrBeta;
	private double sabrDisplacement;

	private double sabrRho;
	private double sabrVolvol;
	private double correlationDecay;
	private double iborOisDecorrelation;

	private VolatilityCubeModel model;
	private String forwardCurveName;

	// calculations
	private DataTable baseVolTable;
	private DataTable swapRateTable;

	private SwaptionDataLattice physicalATMSwaptionsVolatilities;


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
	public static SABRVolatilityCubeParallel createSABRVolatilityCubeParallel(String cubeName, LocalDate referenceDate, SchedulePrototype fixMetaSchedule,
			SchedulePrototype floatMetaSchedule, double sabrDisplacement, double sabrBeta, double sabrRho, double sabrVolvol, double correlationDecay, double iborOisDecorrelation,
			SwaptionDataLattice physicalATMSwaptions, VolatilityCubeModel model, String forwardCurveName) {

		SABRVolatilityCubeParallelFactory factory = new SABRVolatilityCubeParallelFactory(cubeName, referenceDate, fixMetaSchedule, floatMetaSchedule, sabrDisplacement, sabrBeta,
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
	 * @param discountCurveName The name of the discount curve to use, when fitting the atm level.
	 * @param forwardCurveName The name of the forward curve to use, when fitting the atm level.
	 */
	private SABRVolatilityCubeParallelFactory(String cubeName, LocalDate referenceDate, SchedulePrototype fixMetaSchedule, SchedulePrototype floatMetaSchedule,
			double sabrDisplacement, double sabrBeta, double sabrRho, double sabrVolvol, double correlationDecay, double iborOisDecorrelation,
			SwaptionDataLattice physicalATMSwaptionVolatilities, VolatilityCubeModel model, String forwardCurveName) {
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

		ArrayList<Integer> maturitiesList	= new ArrayList<>();
		ArrayList<Integer> terminationsList	= new ArrayList<>();
		ArrayList<Double> swapRateList		= new ArrayList<>();

		for(int maturity : physicalATMSwaptionsVolatilities.getMaturities(0)) {
			for(int termination : physicalATMSwaptionsVolatilities.getTenors(0, maturity)) {
				maturitiesList.add(maturity);
				terminationsList.add(termination);

				LocalDate maturityDate = referenceDate.plusMonths(maturity);
				LocalDate terminationDate = maturityDate.plusMonths(termination);

				Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

				swapRateList.add(swapRate);
			}
		}

		return new DataTableInterpolated("Swap Rates", TableConvention.inMONTHS, referenceDate, floatMetaSchedule, maturitiesList, terminationsList,
				swapRateList);
	}

	/**
	 * Create a table of base volatilities for the SABR smiles to match the market observed atm volatilities.
	 *
	 * @return Table of base volatilities.
	 */
	private DataTable makeBaseVolTable() {

		int[] maturitiesArray = new int[swapRateTable.size()];
		int[] terminationsArray = new int[swapRateTable.size()];
		double[] valuesArray = new double[swapRateTable.size()];

		int index = 0;
		for(int maturity : swapRateTable.getMaturities()) {
			for(int termination : swapRateTable.getTerminationsForMaturity(maturity)) {
				maturitiesArray[index] = maturity;
				terminationsArray[index] = termination;

				valuesArray[index++] = 0.01;
			}
		}

		DataTableInterpolated tempTable = new DataTableInterpolated("Temp Volatilities", TableConvention.inMONTHS, referenceDate, floatMetaSchedule,
				maturitiesArray, terminationsArray, valuesArray);

		VolatilityCube tempCube = new SABRVolatilityCubeParallel("tempCube", referenceDate, swapRateTable, sabrDisplacement, sabrBeta, sabrRho,
				sabrVolvol, tempTable, correlationDecay, iborOisDecorrelation);

		index = 0;
		for(int maturity : swapRateTable.getMaturities()) {
			for(int termination : swapRateTable.getTerminationsForMaturity(maturity)) {

				LocalDate maturityDate = referenceDate.plusMonths(maturity);
				LocalDate terminationDate = maturityDate.plusMonths(termination);

				Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

				double matFraction = floatSchedule.getPeriodStart(0);
				double termFraction = floatSchedule.getPeriodEnd(floatSchedule.getNumberOfPeriods()-1);
				valuesArray[index++] = 0.01 * physicalATMSwaptionsVolatilities.getValue(maturity, termination, 0)
						/ tempCube.getValue(termFraction, matFraction, swapRate, net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention.VOLATILITYNORMAL);
			}
		}

		return new DataTableInterpolated("Base Volatilities", TableConvention.inMONTHS, referenceDate, floatMetaSchedule, maturitiesArray, terminationsArray,
				valuesArray);
	}
}
