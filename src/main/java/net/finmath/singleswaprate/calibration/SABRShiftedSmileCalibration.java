package net.finmath.singleswaprate.calibration;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.singleswaprate.data.DataTableExtrapolated;
import net.finmath.singleswaprate.data.DataTable;
import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.singleswaprate.data.DataTableLight;
import net.finmath.singleswaprate.data.DataTableLinear;
import net.finmath.singleswaprate.model.volatilities.SABRVolatilityCube;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * Calibration of a {@link SABRVolatilityCube} by shifting increments in the market data of cash settled swaptions onto physically settled swaptions and calibrating a SABR model
 * on the resulting smiles. The calibration happens per node and is thus much faster than a calibration of the whole cube at once.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SABRShiftedSmileCalibration {

	private final LocalDate referenceDate;

	private final AnalyticModel model;

	private final SwaptionDataLattice cashPayerPremiums;
	private final SwaptionDataLattice cashReceiverPremiums;
	private final SwaptionDataLattice physicalPremiumsATM;

	private final SchedulePrototype fixMetaSchedule;
	private final SchedulePrototype floatMetaSchedule;
	private final String discountCurveName;
	private final String forwardCurveName;

	private final double sabrDisplacement;
	private final double sabrBeta;
	private final double correlationDecay;
	private final double iborOisDecorrelation;

	private Map<Integer, DataTableLight> physicalVolatilities;
	private Map<Integer, DataTableLight> cashPayerVolatilities;
	private Map<Integer, DataTableLight> cashReceiverVolatilities;

	private DataTableLight interpolationNodes;
	private boolean useLinearInterpolation		= true;

	private DataTable swapRateTable;
	private DataTable rhoTable;
	private DataTable baseVolTable;
	private DataTable volvolTable;

	private int maxIterations = 500;
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();


	/**
	 * Calibrate a cube via shifting cash settled swaption smiles onto physically settled swaption atm volatility.
	 * Using the default calibration parameters.
	 *
	 * @param name The name of the calibrated cube.
	 * @param referenceDate The reference date of the tables.
	 * @param cashPayerPremiums Lattice containing cash settled payer swaption premiums.
	 * @param cashReceiverPremiums Lattice containing cash settled receiver swaption premiums.
	 * @param physicalPremiumsATM Table containing physical settled swaption atm premiums.
	 * @param model The model for context.
	 * @param sabrDisplacement The displacement parameter the SABR smiles of the cube is supposed to have.
	 * @param sabrBeta The beta parameter the SABR smiles of the cubes are supposed to have.
	 * @param correlationDecay The correlation decay parameter the cube is supposed to have.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter the calibrated cube is sipposed to have.
	 *
	 * @return The calibrated cube.
	 *
	 * @throws SolverException Thrown when solvers fail to find suitable parameters.
	 */
	public static SABRVolatilityCube createSABRVolatilityCube(String name, LocalDate referenceDate,
			SwaptionDataLattice cashPayerPremiums, SwaptionDataLattice cashReceiverPremiums, SwaptionDataLattice physicalPremiumsATM,
			AnalyticModel model, double sabrDisplacement, double sabrBeta,	double correlationDecay, double iborOisDecorrelation) throws SolverException {

		SABRShiftedSmileCalibration factory = new SABRShiftedSmileCalibration(referenceDate, cashPayerPremiums, cashReceiverPremiums, physicalPremiumsATM,
				model, sabrDisplacement, sabrBeta, correlationDecay, iborOisDecorrelation);

		return factory.build(name);
	}

	/**
	 * Return all data points as volatilities that serve as calibration targets. Points are sorted into maps according to their strike in moneyness.
	 *
	 * @param name The name of the tables. Will be amended by their strike in moneyness.
	 * @param referenceDate The reference date of the tables.
	 * @param cashPayerPremiums Lattice containing cash settled payer swaption premiums.
	 * @param cashReceiverPremiums Lattice containing cash settled receiver swaption premiums.
	 * @param physicalPremiumsATM Table containing physical settled swaption atm premiums.
	 * @param model The model for context.
	 * @return The set of maps containing market data volatility points.
	 */
	public static Map<Integer, DataTable> createVolatilityCubeLattice(String name, LocalDate referenceDate,
			SwaptionDataLattice cashPayerPremiums, SwaptionDataLattice cashReceiverPremiums, SwaptionDataLattice physicalPremiumsATM, AnalyticModel model) {

		SABRShiftedSmileCalibration factory = new SABRShiftedSmileCalibration(referenceDate, cashPayerPremiums, cashReceiverPremiums, physicalPremiumsATM,
				model, 0, 0, 0, 0);

		try {
			factory.build(name);
		} catch (SolverException e) {
			// ignore this, lattice doesn't use calibrated cube
		}

		TreeMap<Integer, DataTable> returnMap = new TreeMap<Integer, DataTable>();
		for(Map.Entry<Integer, DataTableLight> entry : factory.physicalVolatilities.entrySet()) {
			returnMap.put(entry.getKey(), entry.getValue().clone());
		}
		return returnMap;
	}

	/**
	 * Create the calibrator to be able to modify calibration parameters before building the cube.
	 *
	 * @param referenceDate The reference date of the calibrated cube.
	 * @param cashPayerPremiums Lattice containing cash settled payer swaption premiums.
	 * @param cashReceiverPremiums Lattice containing cash settled receiver swaption premiums.
	 * @param physicalPremiumsATM Table containing physical settled swaption atm premiums.
	 * @param model The model providing context.
	 * @param sabrDisplacement The displacement parameter the SABR smiles of the cube is supposed to have.
	 * @param sabrBeta The beta parameter the SABR smiles of the cubes are supposed to have.
	 * @param correlationDecay The correlation decay parameter the cube is supposed to have.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter the calibrated cube is supposed to have.
	 */
	public SABRShiftedSmileCalibration(LocalDate referenceDate,	SwaptionDataLattice cashPayerPremiums, SwaptionDataLattice cashReceiverPremiums,
			SwaptionDataLattice physicalPremiumsATM, AnalyticModel model,
			double sabrDisplacement, double sabrBeta, double correlationDecay, double iborOisDecorrelation) {
		super();
		this.referenceDate = referenceDate;
		this.physicalPremiumsATM = physicalPremiumsATM;
		this.cashPayerPremiums = cashPayerPremiums;
		this.cashReceiverPremiums = cashReceiverPremiums;
		this.model = model;
		this.sabrDisplacement = sabrDisplacement;
		this.sabrBeta = sabrBeta;
		this.correlationDecay = correlationDecay;
		this.iborOisDecorrelation = iborOisDecorrelation;

		fixMetaSchedule		= cashPayerPremiums.getFixMetaSchedule();
		floatMetaSchedule	= cashPayerPremiums.getFloatMetaSchedule();
		discountCurveName	= cashPayerPremiums.getDiscountCurveName();
		forwardCurveName	= cashPayerPremiums.getForwardCurveName();
	}

	/**
	 * Perform the calibrations and build the cube.
	 *
	 * @param name The name of the cube.
	 * @return The calibrated cube.
	 *
	 * @throws SolverException Thrown when solvers fail to find suitable parameters.
	 */
	public SABRVolatilityCube build(String name) throws SolverException {

		findInterpolationNodes();
		makeSwapRateTable();

		findPayerVolatilities();
		findReceiverVolatilities();

		makePhysicalVolatilities();
		calibrateSmilesOnNodes();

		return new SABRVolatilityCube(name, referenceDate, swapRateTable, sabrDisplacement, sabrBeta, rhoTable, baseVolTable, volvolTable,
				correlationDecay, iborOisDecorrelation);
	}




	/**
	 * Calibrate the SABR smiles on each node.
	 *
	 * @throws SolverException Thrown when solvers fail to find suitable parameters.
	 */
	private void calibrateSmilesOnNodes() throws SolverException {

		// lists for all parameter maps
		ArrayList<Integer> maturities = new ArrayList<Integer>();
		ArrayList<Integer> terminations = new ArrayList<Integer>();
		ArrayList<Double> sabrRhos = new ArrayList<Double>();
		ArrayList<Double> sabrBaseVols = new ArrayList<Double>();
		ArrayList<Double> sabrVolvols = new ArrayList<Double>();

		// calibrate a SABR smile on each node of the grid
		double[] initialParameters = new double[]{ 0.01, 0.15, 0.3 }; // baseVol, volVol, rho

		int[] maturityArray = new int[interpolationNodes.getMaturities().size()];
		int[] terminationArray = new int[interpolationNodes.getTerminations().size()];

		// going through nodes in reverse, because we want to use results from longer tenors for calibration of shorter ones
		int index = maturityArray.length-1;
		for(int maturity : interpolationNodes.getMaturities()) {
			maturityArray[index--] = maturity;
		}
		index = terminationArray.length-1;
		for(int termination : interpolationNodes.getTerminations()) {
			terminationArray[index--] = termination;
		}

		for(int maturity :  maturityArray) {
			for(int termination : terminationArray) {
				double parSwapRate = swapRateTable.getValue(maturity, termination);
				double sabrMaturity = floatMetaSchedule.generateSchedule(referenceDate, referenceDate.plusMonths(maturity),
						referenceDate.plusMonths(maturity+termination)).getFixing(0);

				// gather smile points
				int count = 0;
				for(int moneyness : physicalVolatilities.keySet()) {
					if(physicalVolatilities.get(moneyness).containsEntryFor(maturity, termination)) {
						count++;
					}
				}

				double[] marketStrikes = new double[count];
				double[] marketVolatilities = new double[count];

				index = 0;
				for(int moneyness : physicalVolatilities.keySet()) {
					if(physicalVolatilities.get(moneyness).containsEntryFor(maturity, termination)) {
						marketStrikes[index] = parSwapRate + moneyness /10000.0;
						marketVolatilities[index++] = physicalVolatilities.get(moneyness).getValue(maturity, termination);
					}
				}


				// calibrate SABR
				LevenbergMarquardt optimizer = new LevenbergMarquardt(
						initialParameters,
						marketVolatilities,
						maxIterations,
						numberOfThreads
						) {
					private static final long serialVersionUID = -7551690451877166912L;

					@Override
					public void setValues(double[] parameters, double[] values) {

						// making sure that volatility stays above 0 and rho between -1 and 1.
						parameters[0] = Math.max(parameters[0], 0);
						parameters[1] = Math.max(parameters[1], 0);
						parameters[2] = Math.max(Math.min(parameters[2], 1), -1);

						for(int i = 0; i < marketStrikes.length; i++) {
							values[i] = AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(parameters[0] /* alpha */, sabrBeta /* beta */,
									parameters[2] /* rho */, parameters[1] /* nu */, sabrDisplacement /* displacement */,
									parSwapRate, marketStrikes[i], sabrMaturity);

						}
					}
				};
				optimizer.run();
//				System.out.println("Optimizer for node "+maturity+"x"+termination+" finished after " +optimizer.getIterations() +
//						" iterations with mean error " + optimizer.getRootMeanSquaredError());

				double[] parameters = optimizer.getBestFitParameters();

				// sort calibrated parameters into lists
				maturities.add(maturity);
				terminations.add(termination);
				sabrBaseVols.add(parameters[0]);
				sabrVolvols.add(parameters[1]);
				sabrRhos.add(parameters[2]);

				initialParameters = parameters;
			}
		}

		if(useLinearInterpolation) {
			baseVolTable = new DataTableLinear("MarketBaseVolatilityTable", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturities,
					terminations, sabrBaseVols);
			volvolTable = new DataTableLinear("MarketVolVolTable", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturities, terminations,
					sabrVolvols);
			rhoTable = new DataTableLinear("MarketRhoTable", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturities, terminations, sabrRhos);
		} else {
			baseVolTable = new DataTableExtrapolated("MarketBaseVolatilityTable", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturities,
					terminations, sabrBaseVols);
			volvolTable = new DataTableExtrapolated("MarketVolVolTable", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturities, terminations,
					sabrVolvols);
			rhoTable = new DataTableExtrapolated("MarketRhoTable", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturities, terminations, sabrRhos);
		}

	}

	/**
	 * Identify the nodes on which to calibrate.
	 */
	private void findInterpolationNodes() {

		ArrayList<Integer> nodeMaturities = new ArrayList<Integer>();
		ArrayList<Integer> nodeTerminations = new ArrayList<Integer>();
		ArrayList<Double> nodeCardinalities = new ArrayList<Double>();

		Set<Integer> payerStrikes = new TreeSet<Integer>(cashPayerPremiums.getGridNodesPerMoneyness().keySet());
		payerStrikes.remove(0);
		Set<Integer> receiverStrikes = new TreeSet<Integer>(cashReceiverPremiums.getGridNodesPerMoneyness().keySet());
		receiverStrikes.remove(0);

		for(int maturity : physicalPremiumsATM.getMaturities()) {
			for(int termination : physicalPremiumsATM.getTenors(0, maturity)) {
				int count = 1;
				for(int strike : payerStrikes) {
					if(cashPayerPremiums.containsEntryFor(maturity, termination, strike)) {
						count++;
					}
				}
				for(int strike : receiverStrikes) {
					if(cashReceiverPremiums.containsEntryFor(maturity, termination, strike)) {
						count++;
					}
				}

				//only consider if there are more than a single point for a given node
				if(count > 1) {
					nodeMaturities.add(maturity);
					nodeTerminations.add(termination);
					nodeCardinalities.add((double) count);
				}
			}
		}
		interpolationNodes = new DataTableLight("NodesWithCardinality", TableConvention.MONTHS, nodeMaturities, nodeTerminations,
				nodeCardinalities);

		// fix holes (as interpolation needs a regular grid)
		if(interpolationNodes.size() != interpolationNodes.getMaturities().size() * interpolationNodes.getTerminations().size()) {
			for(int maturity : interpolationNodes.getMaturities()) {
				for(int termination : interpolationNodes.getTerminations()) {
					if(! interpolationNodes.containsEntryFor(maturity, termination)) {
						interpolationNodes = interpolationNodes.addPoint(maturity, termination, 1);
					}
				}
			}
		}
	}

	/**
	 * Make physical volatility smiles, by shifting cash volatility smiles to physical atm level.
	 */
	private void makePhysicalVolatilities() {

		// atm base
		int[] maturitiesArray = new int[interpolationNodes.size()];
		int[] terminationsArray = new int[interpolationNodes.size()];
		double[] volatilitiesArray = new double[interpolationNodes.size()];

		int index = 0;
		for(int maturity : interpolationNodes.getMaturities()) {
			for(int termination : interpolationNodes.getTerminationsForMaturity(maturity)) {
				maturitiesArray[index] = maturity;
				terminationsArray[index] = termination;

				LocalDate maturityDate = referenceDate.plusMonths(maturity);
				LocalDate terminationDate = maturityDate.plusMonths(termination);

				Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				double annuity = SwapAnnuity.getSwapAnnuity(fixSchedule.getFixing(0), fixSchedule, model.getDiscountCurve(discountCurveName), model);
				double swapRate = swapRateTable.getValue(maturity, termination);

				volatilitiesArray[index++] = AnalyticFormulas.bachelierOptionImpliedVolatility(swapRate, fixSchedule.getFixing(0), swapRate, annuity,
						physicalPremiumsATM.getValue(maturity, termination, 0));
			}
		}

		DataTableLight physicalATMTable =  new DataTableLight("VolatilitiesPhysicalATM", TableConvention.MONTHS, maturitiesArray, terminationsArray,
				volatilitiesArray);

		physicalVolatilities = new TreeMap<Integer, DataTableLight>();
		physicalVolatilities.put(0, physicalATMTable);

		DataTableLight payerATMTable = cashPayerVolatilities.get(0);
		DataTableLight receiverATMTable = cashReceiverVolatilities.get(0);




		// imitate physical smile via cash smiles
		Set<Integer> strikes = new TreeSet<Integer>(cashPayerVolatilities.keySet());
		strikes.addAll(cashReceiverVolatilities.keySet());
		strikes.remove(0);

		for(int strike : strikes) {
			//lists for bulk-initialization of tables
			ArrayList<Integer> maturitiesPositive = new ArrayList<Integer>();
			ArrayList<Integer> terminationsPositive = new ArrayList<Integer>();
			ArrayList<Double> physicalVolatilitiesPositive = new ArrayList<Double>();

			ArrayList<Integer> maturitiesNegative = new ArrayList<Integer>();
			ArrayList<Integer> terminationsNegative = new ArrayList<Integer>();
			ArrayList<Double> physicalVolatilitiesNegative = new ArrayList<Double>();

			// shifting surface by shifting each individual point
			for(int maturity : interpolationNodes.getMaturities()) {
				for(int termination : interpolationNodes.getTerminationsForMaturity(maturity)) {
					double physicalATM = physicalATMTable.getValue(maturity, termination);

					//shifting positive wing like payer
					if(cashPayerVolatilities.containsKey(strike) && cashPayerVolatilities.get(strike).containsEntryFor(maturity, termination)) {
						double payerATM = payerATMTable.getValue(maturity, termination);
						double payerSmile = cashPayerVolatilities.get(strike).getValue(maturity, termination);

						maturitiesPositive.add(maturity);
						terminationsPositive.add(termination);
						physicalVolatilitiesPositive.add(payerSmile - payerATM + physicalATM);
					}

					//shifting negative wing like receiver
					if(cashReceiverVolatilities.containsKey(strike) && cashReceiverVolatilities.get(strike).containsEntryFor(maturity, termination)) {
						double receiverATM = receiverATMTable.getValue(maturity, termination);
						double receiverSmile = cashReceiverVolatilities.get(strike).getValue(maturity, termination);

						maturitiesNegative.add(maturity);
						terminationsNegative.add(termination);
						physicalVolatilitiesNegative.add(receiverSmile - receiverATM + physicalATM);
					}
				}
			}

			DataTableLight physicalPositiveSmileTable = new DataTableLight("VolatilitiesPhysical" +  strike, TableConvention.MONTHS,
					maturitiesPositive, terminationsPositive, physicalVolatilitiesPositive);
			DataTableLight physicalNegativeSmileTable = new DataTableLight("VolatilitiesPhysical" + -strike, TableConvention.MONTHS,
					maturitiesNegative, terminationsNegative, physicalVolatilitiesNegative);


			physicalVolatilities.put( strike, physicalPositiveSmileTable);
			physicalVolatilities.put(-strike, physicalNegativeSmileTable);
		}
	}

	/**
	 * Construct the volatility (half-) smile of cash settled payer swaptions.
	 */
	private void findPayerVolatilities() {

		//convert to volatilities
		cashPayerVolatilities = new TreeMap<Integer, DataTableLight>();
		for(int moneyness : cashPayerPremiums.getGridNodesPerMoneyness().keySet()) {

			ArrayList<Integer> maturities = new ArrayList<Integer>();
			ArrayList<Integer> terminations = new ArrayList<Integer>();
			ArrayList<Double> values = new ArrayList<Double>();

			for(int maturity : interpolationNodes.getMaturities()) {
				for(int termination : interpolationNodes.getTerminationsForMaturity(maturity)) {
					if(cashPayerPremiums.containsEntryFor(maturity, termination, moneyness)){

						LocalDate maturityDate = referenceDate.plusMonths(maturity);
						LocalDate terminationDate = maturityDate.plusMonths(termination);

						Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
						double swapRate = swapRateTable.getValue(maturity, termination);
						double cashAnnuity = cashFunction(swapRate, fixSchedule);

						maturities.add(maturity);
						terminations.add(termination);
						values.add(AnalyticFormulas.bachelierOptionImpliedVolatility(swapRate, fixSchedule.getFixing(0), swapRate + moneyness /10000.0,
								cashAnnuity, cashPayerPremiums.getValue(maturity, termination, moneyness)));
					}
				}
			}
			DataTableLight volatilityTable = new DataTableLight("VolatilitiesPayer"+moneyness, TableConvention.MONTHS, maturities, terminations, values);
			cashPayerVolatilities.put(moneyness, volatilityTable);
		}

	}

	/**
	 * Construct the volatility (half-) smile of cash settled receiver swaptions.
	 */
	private void findReceiverVolatilities() {

		//convert to volatilities
		cashReceiverVolatilities = new TreeMap<Integer, DataTableLight>();
		for(int moneyness : cashReceiverPremiums.getGridNodesPerMoneyness().keySet()) {

			ArrayList<Integer> maturities = new ArrayList<Integer>();
			ArrayList<Integer> terminations = new ArrayList<Integer>();
			ArrayList<Double> values = new ArrayList<Double>();

			for(int maturity : interpolationNodes.getMaturities()) {
				for(int termination : interpolationNodes.getTerminationsForMaturity(maturity)) {
					if(cashReceiverPremiums.containsEntryFor(maturity, termination, moneyness)){

						LocalDate maturityDate = referenceDate.plusMonths(maturity);
						LocalDate terminationDate = maturityDate.plusMonths(termination);

						Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
						double swapRate = swapRateTable.getValue(maturity, termination);
						double cashAnnuity = cashFunction(swapRate, fixSchedule);

						maturities.add(maturity);
						terminations.add(termination);
						values.add(AnalyticFormulas.bachelierOptionImpliedVolatility(swapRate, fixSchedule.getFixing(0), swapRate-moneyness/10000.0,
								cashAnnuity, cashReceiverPremiums.getValue(maturity, termination, moneyness) + moneyness/10000.0 * cashAnnuity));
					}
				}
			}
			DataTableLight volatilityTable = new DataTableLight("VolatilitiesReceiver"+moneyness, TableConvention.MONTHS, maturities, terminations, values);
			cashReceiverVolatilities.put(moneyness, volatilityTable);
		}
	}

	/**
	 * Build the table of swap rates as underlyings for the SABR smiles.
	 */
	private void makeSwapRateTable() {

		int[] maturitiesArray = new int[interpolationNodes.size()];
		int[] terminationsArray = new int[interpolationNodes.size()];
		double[] swapRateArray = new double[interpolationNodes.size()];

		int index = 0;
		for(int maturity : interpolationNodes.getMaturities()) {
			for(int termination : interpolationNodes.getTerminationsForMaturity(maturity)) {
				maturitiesArray[index] = maturity;
				terminationsArray[index] = termination;

				LocalDate maturityDate = referenceDate.plusMonths(maturity);
				LocalDate terminationDate = maturityDate.plusMonths(termination);

				Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturityDate, terminationDate);
				double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

				swapRateArray[index++] = swapRate;
			}
		}

		if(useLinearInterpolation) {
			swapRateTable = new DataTableLinear("MarketParSwapRates", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturitiesArray,
					terminationsArray, swapRateArray);
		} else {
			swapRateTable = new DataTableExtrapolated("MarketParSwapRates", TableConvention.MONTHS, referenceDate, floatMetaSchedule, maturitiesArray,
					terminationsArray, swapRateArray);
		}
	}

	/**
	 * Set the parameters for calibration.
	 *
	 * @param maxIterations The maximum number of iterations done during calibration.
	 * @param numberOfThreads The number of processor threads to be used.
	 */
	public void setCalibrationParameters( int maxIterations, int numberOfThreads) {
		this.maxIterations		= maxIterations;
		this.numberOfThreads 	= numberOfThreads;
	}

	/**
	 * @return The maximum number of iterations of the optimizer.
	 */
	public int getMaxIterations() {
		return maxIterations;
	}


	/**
	 * @return The number of threads the optimizer is allowed to use.
	 */
	public int getNumberOfThreads() {
		return numberOfThreads;
	}

	/**
	 * @return True if tables holding SABR parameters use linear interpolation, false if piecewise cubic spline.
	 */
	public boolean isUseLinearInterpolation() {
		return useLinearInterpolation;
	}

	/**
	 * @param useLinearInterpolation Set whether the interpolation of SABR parameters should be linear as opposed to piecewise cubic spline.
	 */
	public void setUseLinearInterpolation(boolean useLinearInterpolation) {
		this.useLinearInterpolation = useLinearInterpolation;
	}

	/**
	 * Cash function of cash settled swaptions for equidistant tenors.
	 *
	 * @param swapRate The swap rate.
	 * @param schedule The schedule.
	 * @return The result of the cash function.
	 */
	private static double cashFunction(double swapRate, Schedule schedule) {

		int numberOfPeriods = schedule.getNumberOfPeriods();
		double periodLength = 0.0;
		for(int index = 0; index < numberOfPeriods; index++) {
			periodLength += schedule.getPeriodLength(index);
		}
		periodLength /= schedule.getNumberOfPeriods();

		if(swapRate == 0.0) return numberOfPeriods * periodLength;
		else return (1 - Math.pow(1 + periodLength * swapRate, - numberOfPeriods)) / swapRate;
	}

}
