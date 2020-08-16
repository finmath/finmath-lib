package net.finmath.singleswaprate.calibration;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;

import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.annuitymapping.AnnuityMappingFactory;
import net.finmath.singleswaprate.data.DataTable;
import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.singleswaprate.data.DataTableInterpolated;
import net.finmath.singleswaprate.data.DataTableLight;
import net.finmath.singleswaprate.data.DataTableLinear;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.SABRVolatilityCube;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
import net.finmath.singleswaprate.products.CashSettledPayerSwaption;
import net.finmath.singleswaprate.products.CashSettledReceiverSwaption;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * Calibration of {@link SABRVolatilityCube} using custom optimization. Increased performance compared to default approach, by using a {@link SABRShiftedSmileCalibration} to get
 * initial values and then splitting the calibration of the entire cube into calibration of slices along individual maturities. The slices do not interact with each other, because
 * the annuities depend only on sub-tenors of schedules with the same maturity.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SABRCubeCalibration {

	//input
	private final LocalDate referenceDate;

	private final SwaptionDataLattice cashPayerPremiums;
	private final SwaptionDataLattice cashReceiverPremiums;
	private final SwaptionDataLattice physicalPremiumsATM;

	private final SchedulePrototype tableMetaSchedule;

	private final String discountCurveName;
	private final String forwardCurveName;

	private final VolatilityCubeModel model;

	private final AnnuityMappingType annuityMappingType;

	private boolean useLinearInterpolation = true;

	private int maxIterations	= 250;
	private int numberOfThreads	= Runtime.getRuntime().availableProcessors();

	private boolean replicationUseAsOffset			= true;
	private double replicationLowerBound 			= -0.15;
	private double replicationUpperBound			= 0.15;
	private int replicationNumberOfEvaluationPoints = 500;

	private double correlationDecay = 0.045;
	private double iborOisDecorrelation = 1.2;

	private double displacement = 0.15;
	private double beta = 0.5;

	private DataTable swapRateTable;

	// initial parameters taken from these tables
	private DataTable initialRhos = null;
	private DataTable initialBaseVols = null;
	private DataTable initialVolvols = null;

	// used during calculations
	private int[] terminations;
	private double[] parameters;

	private double[] marketTargets;
	private ArrayList<SwaptionInfo> payerSwaptions;
	private ArrayList<SwaptionInfo> receiverSwaptions;

	// to identify maturity when building slices
	private int currentMaturity;


	//gather calibrated parameters in these
	private DataTable rhoTable;
	private DataTable baseVolTable;
	private DataTable volvolTable;

	/**
	 * Create the calibrator.
	 *
	 * @param referenceDate The reference date of the cube.
	 * @param cashPayerPremiums The lattice containing market targets for cash settled payer swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param cashReceiverPremiums The lattice containing market targets for cash settled receiver swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param physicalPremiumsATM Table containing physical settled swaption atm premiums.
	 * @param model The model providing context.
	 * @param annuityMappingType The type of annuity mapping to be used for calibration.
	 *
	 * @throws IllegalArgumentException Triggers when data is not provided in QuotingConvention.Price.
	 */
	public SABRCubeCalibration(final LocalDate referenceDate, final SwaptionDataLattice cashPayerPremiums, final SwaptionDataLattice cashReceiverPremiums, final SwaptionDataLattice physicalPremiumsATM,
			final VolatilityCubeModel model, final AnnuityMappingType annuityMappingType) {
		super();
		if(cashPayerPremiums.getQuotingConvention() != QuotingConvention.PAYERPRICE || cashReceiverPremiums.getQuotingConvention() != QuotingConvention.RECEIVERPRICE) {
			throw new IllegalArgumentException("Swaption data not provided in QuotingConvention.PAYERPRICE or QuotingConvention.RECEIVERPRICE respectively.");
		}
		this.referenceDate = referenceDate;
		this.physicalPremiumsATM = physicalPremiumsATM;
		this.cashPayerPremiums = cashPayerPremiums;
		this.cashReceiverPremiums = cashReceiverPremiums;
		this.model = model;
		this.annuityMappingType = annuityMappingType;

		tableMetaSchedule	= physicalPremiumsATM.getFloatMetaSchedule();
		discountCurveName	= cashPayerPremiums.getDiscountCurveName();
		forwardCurveName	= cashPayerPremiums.getForwardCurveName();
	}

	/**
	 * Create the calibrator.
	 *
	 * @param referenceDate The reference date of the cube.
	 * @param cashPayerPremiums The lattice containing market targets for cash settled payer swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param cashReceiverPremiums The lattice containing market targets for cash settled receiver swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param physicalPremiumsATM Table containing physical settled swaption atm premiums.
	 * @param model The model providing context.
	 * @param annuityMappingType The type of annuity mapping to be used for calibration.
	 * @param sabrDisplacement The displacement parameter for the SABR curves in the resulting cube.
	 * @param sabrBeta The beta parameter for the SABR curves in the resulting cube.
	 * @param correlationDecay The correlation decay parameter for resulting cube.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter for the resulting cube.
	 */
	public SABRCubeCalibration(final LocalDate referenceDate, final SwaptionDataLattice cashPayerPremiums, final SwaptionDataLattice cashReceiverPremiums, final SwaptionDataLattice physicalPremiumsATM,
			final VolatilityCubeModel model, final AnnuityMappingType annuityMappingType,
			final double sabrDisplacement, final double sabrBeta, final double  correlationDecay, final double iborOisDecorrelation) {
		this(referenceDate, cashPayerPremiums, cashReceiverPremiums, physicalPremiumsATM, model, annuityMappingType);

		this.displacement = sabrDisplacement;
		this.beta = sabrBeta;
		this.correlationDecay = correlationDecay;
		this.iborOisDecorrelation = iborOisDecorrelation;
	}

	/**
	 * Run the calibration.
	 *
	 * @param cubeName The name of the final cube.
	 * @param terminations The tenors, which are to be calibrated in each slice.
	 * @return The calibrated cube.
	 *
	 * @throws SolverException Thrown when either the calibration of final or initial parameters (if not provided) fails.
	 */
	public SABRVolatilityCube calibrate(final String cubeName, final int[] terminations) throws SolverException {

		final DataTable nodes = findInterpolationNodes();
		this.terminations = terminations;

		if(useLinearInterpolation) {
			rhoTable = new DataTableLinear("Calibrated Rhos", nodes.getConvention(), referenceDate, tableMetaSchedule);
			baseVolTable = new DataTableLinear("Calibrated baseVols", nodes.getConvention(), referenceDate, tableMetaSchedule);
			volvolTable = new DataTableLinear("Calibrated volVols", nodes.getConvention(), referenceDate, tableMetaSchedule);
		} else {
			rhoTable = new DataTableInterpolated("Calibrated Rhos", nodes.getConvention(), referenceDate, tableMetaSchedule);
			baseVolTable = new DataTableInterpolated("Calibrated baseVols", nodes.getConvention(), referenceDate, tableMetaSchedule);
			volvolTable = new DataTableInterpolated("Calibrated volVols", nodes.getConvention(), referenceDate, tableMetaSchedule);
		}


		// go through maturities in reverse order
		final Integer[] maturities = nodes.getMaturities().toArray(new Integer[0]);
		Arrays.sort(maturities, Collections.reverseOrder());

		findInitialParameters();

		for(final int maturity : maturities) {

			initializeParameters(maturity);

			// sort target data for calibration
			generateTargets(maturity);
			currentMaturity = maturity;

			runOptimization();

			// add calibrated parameters to tables
			gatherParameters();
		}

		return new SABRVolatilityCube(cubeName, referenceDate, swapRateTable, displacement, beta,
				rhoTable, baseVolTable, volvolTable, correlationDecay, iborOisDecorrelation);
	}

	/**
	 * Prepare the parameters for the start of the calibration.
	 *
	 * @param initialRhos The table of initial values for rhos.
	 * @param initialBaseVols The table of initial values for base volatilities.
	 * @param initialVolvols The table of initial values for volvols.
	 */
	public void setInitialParameters(final DataTable initialRhos, final DataTable initialBaseVols, final DataTable initialVolvols) {

		this.initialRhos		= initialRhos;
		this.initialBaseVols	= initialBaseVols;
		this.initialVolvols		= initialVolvols;
	}

	/**
	 * Identify the nodes on which to calibrate.
	 */
	private DataTableLight findInterpolationNodes() {

		final ArrayList<Integer> nodeMaturities = new ArrayList<>();
		final ArrayList<Integer> nodeTerminations = new ArrayList<>();
		final ArrayList<Double> nodeCardinalities = new ArrayList<>();

		final Set<Integer> payerStrikes = new TreeSet<>(cashPayerPremiums.getGridNodesPerMoneyness().keySet());
		payerStrikes.remove(0);
		final Set<Integer> receiverStrikes = new TreeSet<>(cashReceiverPremiums.getGridNodesPerMoneyness().keySet());
		receiverStrikes.remove(0);

		for(final int maturity : cashPayerPremiums.getMaturities()) {
			for(final int termination : cashPayerPremiums.getTenors()) {
				int count = 1;
				for(final int strike : payerStrikes) {
					if(cashPayerPremiums.containsEntryFor(maturity, termination, strike)) {
						count++;
					}
				}
				for(final int strike : receiverStrikes) {
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
		DataTableLight interpolationNodes = new DataTableLight("NodesWithCardinality", TableConvention.MONTHS, nodeMaturities, nodeTerminations,
				nodeCardinalities);

		// fix holes (as interpolation needs a regular grid)
		if(interpolationNodes.size() != interpolationNodes.getMaturities().size() * interpolationNodes.getTerminations().size()) {
			for(final int maturity : interpolationNodes.getMaturities()) {
				for(final int termination : interpolationNodes.getTerminations()) {
					if(! interpolationNodes.containsEntryFor(maturity, termination)) {
						interpolationNodes = interpolationNodes.addPoint(maturity, termination, 1);
					}
				}
			}
		}
		return interpolationNodes;
	}

	/**
	 * Uses a SABRShiftedSmileCalibration to find good initial parameters.
	 *
	 * @throws SolverException Thrown when calibration of initial parameters fails.
	 */
	private void findInitialParameters() throws SolverException {

		if(initialRhos == null || initialBaseVols == null || initialVolvols == null) {
			final SABRShiftedSmileCalibration preCalibration = new SABRShiftedSmileCalibration(referenceDate,
					cashPayerPremiums, cashReceiverPremiums, physicalPremiumsATM, model,
					displacement, beta, correlationDecay, iborOisDecorrelation);
			preCalibration.setCalibrationParameters(500, numberOfThreads);
			preCalibration.setUseLinearInterpolation(useLinearInterpolation);
			final SABRVolatilityCube quickCube = preCalibration.build("ShiftedSmileCube");

			swapRateTable = quickCube.getUnderlyingTable();
			initialRhos = quickCube.getRhoTable();
			initialBaseVols = quickCube.getBaseVolTable();
			initialVolvols = quickCube.getVolvolTable();
		} else {
			makeSwapRateTable();
		}

	}

	private void makeSwapRateTable() {

		final ArrayList<Double> swapRates = new ArrayList<>();
		final ArrayList<Integer> matList = new ArrayList<>();
		final ArrayList<Integer> termList = new ArrayList<>();

		final SchedulePrototype fixMetaSchedule	= physicalPremiumsATM.getFixMetaSchedule();
		final SchedulePrototype floatMetaSchedule	= physicalPremiumsATM.getFloatMetaSchedule();
		for(final int maturity : physicalPremiumsATM.getMaturities()) {
			for(final int termination : terminations) {
				final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturity, termination);
				final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturity, termination);

				swapRates.add(Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model));
				matList.add(maturity);
				termList.add(termination);
			}
		}

		if(useLinearInterpolation) {
			swapRateTable = new DataTableLinear("parSwapRates", TableConvention.MONTHS, referenceDate, floatMetaSchedule, matList, termList, swapRates);
		} else {
			swapRateTable = new DataTableInterpolated("parSwapRates", TableConvention.MONTHS, referenceDate, floatMetaSchedule, matList, termList, swapRates);
		}
	}

	/**
	 * Prepare the parameters for the start of the calibration.
	 *
	 * @param maturity The maturity for which to calibrate.
	 */
	protected void initializeParameters(final int maturity) {

		final int numberOfSmiles = terminations.length;
		parameters = new double[numberOfSmiles * 3];
		final double[] rhos = new double[numberOfSmiles];
		final double[] baseVols = new double[numberOfSmiles];
		final double[] volvols = new double[numberOfSmiles];

		for(int i = 0; i < numberOfSmiles; i++) {
			rhos[i] = initialRhos.getValue(maturity, terminations[i]);
			baseVols[i] = initialBaseVols.getValue(maturity, terminations[i]);
			volvols[i] = initialVolvols.getValue(maturity, terminations[i]);
		}

		System.arraycopy(rhos, 0, parameters, 0, numberOfSmiles);
		System.arraycopy(baseVols, 0, parameters, numberOfSmiles, numberOfSmiles);
		System.arraycopy(volvols, 0, parameters, 2 * numberOfSmiles, numberOfSmiles);

	}

	/**
	 * Build the target array.
	 *
	 */
	private void generateTargets(final int maturity) {

		//convert data tables to swaptions and target array
		//order: cashPayer(strike maturity termination) cashReceiver(strike maturity termination)

		payerSwaptions = new ArrayList<>();
		receiverSwaptions = new ArrayList<>();

		//prep temp variables
		final ArrayList<Double> targetsPayer 	  = new ArrayList<>();
		final ArrayList<Double> targetsReceiver = new ArrayList<>();

		//sort all data into array lists
		for(final int moneyness : cashPayerPremiums.getGridNodesPerMoneyness().keySet()) {
			for(final int termination : cashPayerPremiums.getTenors(moneyness, maturity)) {
				payerSwaptions.add( new SwaptionInfo(moneyness, maturity, termination));
				targetsPayer.add( cashPayerPremiums.getValue(maturity, termination, moneyness));
			}
		}

		for(final int moneyness : cashReceiverPremiums.getGridNodesPerMoneyness().keySet()) {
			for(final int termination : cashReceiverPremiums.getTenors(moneyness, maturity)) {
				receiverSwaptions.add( new SwaptionInfo(-moneyness, maturity, termination));
				targetsReceiver.add( cashReceiverPremiums.getValue(maturity, termination, moneyness));
			}
		}

		final ArrayList<Double> targetsList = targetsPayer;
		targetsList.addAll(targetsReceiver);

		this.marketTargets = ArrayUtils.toPrimitive(targetsList.toArray(new Double[0]));
	}








	/**
	 * Apply bounds to parameters. Such as volatility larger zero.
	 *
	 * @param parameters The raw parameters of the cube as array.
	 * @return The parameters with their respective bounds applied.
	 */
	protected double[] applyParameterBounds(final double[] parameters) {
		final double[] boundedParameters = new double[parameters.length];
		final int numberOfSmiles = terminations.length;

		for(int index = 0; index < numberOfSmiles; index ++)  {
			boundedParameters[index] = Math.min(.999999, Math.max(-.999999, parameters[index]));
			boundedParameters[index + numberOfSmiles] = Math.max(0, parameters[index + numberOfSmiles]);
			boundedParameters[index + 2* numberOfSmiles] = Math.max(0, parameters[index + 2* numberOfSmiles]);
		}

		return boundedParameters;
	}

	/**
	 * Build a volatility cube consisting of only the slice that is currently being calibrated.
	 *
	 * @param name The name of the cube.
	 * @param parameters The parameters of the slice.
	 * @return A slice of the cube.
	 */
	private VolatilityCube buildSlice(final String name, final double[] parameters) {

		final int numberOfSmiles = terminations.length;
		final int[] maturities = new int[numberOfSmiles];
		Arrays.fill(maturities, currentMaturity);

		final double[] rhos = Arrays.copyOf(parameters, numberOfSmiles);
		final double[] baseVols = Arrays.copyOfRange(parameters, numberOfSmiles, numberOfSmiles * 2);
		final double[] volvols = Arrays.copyOfRange(parameters, numberOfSmiles * 2, numberOfSmiles * 3);

		DataTable rhoTable;
		DataTable baseVolTable;
		DataTable volvolTable;
		if(useLinearInterpolation) {
			rhoTable = new DataTableLinear("rho", TableConvention.MONTHS, referenceDate, tableMetaSchedule, maturities,
					terminations, rhos);
			baseVolTable = new DataTableLinear("baseVol", TableConvention.MONTHS, referenceDate, tableMetaSchedule, maturities,
					terminations, baseVols);
			volvolTable = new DataTableLinear("volvol", TableConvention.MONTHS, referenceDate, tableMetaSchedule, maturities,
					terminations, volvols);
		} else {
			rhoTable = new DataTableInterpolated("rho", TableConvention.MONTHS, referenceDate, tableMetaSchedule, maturities,
					terminations, rhos);
			baseVolTable = new DataTableInterpolated("baseVol", TableConvention.MONTHS, referenceDate, tableMetaSchedule, maturities,
					terminations, baseVols);
			volvolTable = new DataTableInterpolated("volvol", TableConvention.MONTHS, referenceDate, tableMetaSchedule, maturities,
					terminations, volvols);
		}

		final VolatilityCube slice = new SABRVolatilityCube(name, referenceDate, swapRateTable, displacement, beta,
				rhoTable, baseVolTable, volvolTable, correlationDecay, iborOisDecorrelation);

		return slice;
	}

	/**
	 * Add the parameters of the calibrated slice to the total set of parameters.
	 */
	private void gatherParameters() {

		final int numberOfSmiles = terminations.length;
		final int[] maturities = new int[numberOfSmiles];
		Arrays.fill(maturities, currentMaturity);

		final double[] rhos = Arrays.copyOf(parameters, numberOfSmiles);
		final double[] baseVols = Arrays.copyOfRange(parameters, numberOfSmiles, numberOfSmiles * 2);
		final double[] volvols = Arrays.copyOfRange(parameters, numberOfSmiles * 2, numberOfSmiles * 3);

		rhoTable = rhoTable.addPoints(maturities, terminations, rhos);
		baseVolTable = baseVolTable.addPoints(maturities, terminations, baseVols);
		volvolTable = volvolTable.addPoints(maturities, terminations, volvols);

	}

	/**
	 * Run the calibration on the current slice.
	 *
	 * @throws SolverException Thrown, when solvers fail to find suitable parameters.
	 */
	private void runOptimization() throws SolverException {

		final LevenbergMarquardt optimizer = new LevenbergMarquardt(parameters, marketTargets, maxIterations, numberOfThreads) {


			/**
			 *
			 */
			private static final long serialVersionUID = -264612909413575260L;

			@Override
			public void setValues(double[] parameters, final double[] values) {

				//apply bounds to the parameters
				parameters = applyParameterBounds(parameters);

				//get volatility cube and add to temporary model
				final String tempCubeName = "tempCubeSlice";
				final VolatilityCube cube = buildSlice(tempCubeName, parameters);
				final VolatilityCubeModel tempModel = model.addVolatilityCube(cube);

				//pre allocate space
				Schedule floatSchedule;
				Schedule fixSchedule;
				double forwardSwapRate;
				double strike;
				String mappingName;
				AnnuityMapping mapping;
				AnnuityMappingFactory factory;
				final Map<String, AnnuityMapping> container = new HashMap<>();

				int index = 0;
				//calculate cash payer swaption values
				SchedulePrototype fixMetaSchedule	= cashPayerPremiums.getFixMetaSchedule();
				SchedulePrototype floatMetaSchedule	= cashPayerPremiums.getFloatMetaSchedule();
				for(final SwaptionInfo swaption : payerSwaptions) {
					fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					forwardSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, tempModel.getForwardCurve(forwardCurveName), tempModel);
					strike = forwardSwapRate + swaption.moneyness;

					final double replicationLowerBound =
							replicationUseAsOffset ? forwardSwapRate + SABRCubeCalibration.this.replicationLowerBound : SABRCubeCalibration.this.replicationLowerBound;
					final double replicationUpperBound =
							replicationUseAsOffset ? forwardSwapRate + SABRCubeCalibration.this.replicationUpperBound : SABRCubeCalibration.this.replicationUpperBound;

					// see if appropriate mapping already exists, otherwise generate
					mappingName = swaption.toString();
					if(container.containsKey(mappingName)) {
						mapping = container.get(mappingName);
					} else {
						factory = new AnnuityMappingFactory(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, tempCubeName, strike,
								replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
						mapping = factory.build(annuityMappingType, tempModel);
						container.put(mappingName, mapping);
					}

					final CashSettledPayerSwaption css = new CashSettledPayerSwaption(fixSchedule, floatSchedule, strike, discountCurveName, forwardCurveName,
							tempCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
					values[index++] = css.getValue(floatSchedule.getFixing(0), mapping, tempModel);
				}

				//calculate cash receiver swaption values
				fixMetaSchedule		= cashReceiverPremiums.getFixMetaSchedule();
				floatMetaSchedule	= cashReceiverPremiums.getFloatMetaSchedule();
				for(final SwaptionInfo swaption : receiverSwaptions) {
					fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					forwardSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, tempModel.getForwardCurve(forwardCurveName), tempModel);
					strike = forwardSwapRate + swaption.moneyness;

					final double replicationLowerBound =
							replicationUseAsOffset ? forwardSwapRate + SABRCubeCalibration.this.replicationLowerBound : SABRCubeCalibration.this.replicationLowerBound;
					final double replicationUpperBound =
							replicationUseAsOffset ? forwardSwapRate + SABRCubeCalibration.this.replicationUpperBound : SABRCubeCalibration.this.replicationUpperBound;

					// see if appropriate mapping already exists, otherwise generate
					mappingName = swaption.toString();
					if(container.containsKey(mappingName)) {
						mapping = container.get(mappingName);
					} else {
						factory = new AnnuityMappingFactory(fixSchedule, floatSchedule, discountCurveName, forwardCurveName, tempCubeName, strike,
								replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
						mapping = factory.build(annuityMappingType, tempModel);
						container.put(mappingName, mapping);
					}

					final CashSettledReceiverSwaption css = new CashSettledReceiverSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
							forwardCurveName, tempCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound,
							replicationNumberOfEvaluationPoints);
					values[index++] = css.getValue(floatSchedule.getFixing(0), mapping, tempModel);
				}

				//				// output for testing
				//					StringBuilder builder = new StringBuilder();
				//					builder.append("Current maturity:\t"+currentMaturity+"\n");
				//					builder.append("Parameters:\t");
				//					for(double parameter : parameters)
				//						builder.append(parameter +"\t");
				//					builder.append("\nMeanSquaredError:\t" + getMeanSquaredError(values) + "\nValues:\t");
				//					for(double value : values)
				//						builder.append(value +"\t");
				//					builder.append("\nTargets:\t");
				//					for(double target : marketTargets)
				//						builder.append(target + "\t");
				////					builder.append("\nSwaptions:\t");
				////					for(SwaptionInfo swaption : payerSwaptions)
				////						builder.append("payer/"+swaption.toString().replaceAll(", ", "/")+"\t");
				////					for(SwaptionInfo swaption : receiverSwaptions)
				////						builder.append("receiver/"+swaption.toString().replaceAll(", ", "/")+"\t");
				//					builder.append('\n');
				//					String string = builder.toString();
				//
				//				synchronized (System.out) {
				//					System.out.println(string);
				//				}
			}

		};
		optimizer.run();
		System.out.println("Optimizer for maturity "+ currentMaturity +" finished after " +optimizer.getIterations() +" iterations with mean error " + optimizer.getRootMeanSquaredError());

		parameters = applyParameterBounds(optimizer.getBestFitParameters());
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
	 * Set the parameters for the swaption replication.
	 *
	 * @param useAsOffset Are the values of lower and upperBound to be understood as offsets from the par swap rate?
	 * @param lowerBound The lowest strike allowed.
	 * @param upperBound The maximal strike allowed.
	 * @param numberOfEvaluationPoints The number of points to be evaluated during the integration.
	 */
	public void setReplicationParameters(final boolean useAsOffset, final double lowerBound, final double upperBound, final int numberOfEvaluationPoints) {
		this.replicationUseAsOffset = useAsOffset;
		this.replicationLowerBound  = lowerBound;
		this.replicationUpperBound  = upperBound;
		this.replicationNumberOfEvaluationPoints = numberOfEvaluationPoints;
	}

	/**
	 * @return True when the replication bounds are to be understood as offset from the par swap rate.
	 */
	public boolean isReplicationUseAsOffset() {
		return replicationUseAsOffset;
	}

	/**
	 * @return The lowest strike allowed during swaption replication.
	 */
	public double getReplicationLowerBound() {
		return replicationLowerBound;
	}

	/**
	 * @return The maximal strike allowed during swaption replication.
	 */
	public double getReplicationUpperBound() {
		return replicationUpperBound;
	}

	/**
	 * @return The number of points to be evaluated during swaption replication.
	 */
	public int getReplicationNumberOfEvaluationPoints() {
		return replicationNumberOfEvaluationPoints;
	}

	public double getCorrelationDecay() {
		return correlationDecay;
	}


	public void setCorrelationDecay(final double correlationDecay) {
		this.correlationDecay = correlationDecay;
	}


	public double getIborOisDecorrelation() {
		return iborOisDecorrelation;
	}


	public void setIborOisDecorrelation(final double iborOisDecorrelation) {
		this.iborOisDecorrelation = iborOisDecorrelation;
	}


	public double getDisplacement() {
		return displacement;
	}

	public double getBeta() {
		return beta;
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
	public void setUseLinearInterpolation(final boolean useLinearInterpolation) {
		this.useLinearInterpolation = useLinearInterpolation;
	}

	/**
	 * Compact identifier for the swaptions to be created during the optimization.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	protected class SwaptionInfo {
		private final double moneyness;
		private final LocalDate maturity;
		private final LocalDate termination;

		SwaptionInfo(final int moneyness, final int maturity, final int termination) {
			this.moneyness = moneyness / 10000.0;
			this.maturity = referenceDate.plusMonths(maturity);
			this.termination = this.maturity.plusMonths(termination);
		}

		SwaptionInfo(final int moneyness, final int maturity, final int termination, final TableConvention tableConvention) throws IOException {
			this.moneyness = moneyness /10000.0;
			switch(tableConvention) {
			case MONTHS : this.maturity = referenceDate.plusMonths(maturity); this.termination = this.maturity.plusMonths(termination); break;
			case YEARS  : this.maturity = referenceDate.plusYears(maturity); this.termination = this.maturity.plusYears(termination); break;
			case DAYS   : this.maturity = referenceDate.plusDays(maturity); this.termination = this.maturity.plusDays(termination); break;
			case WEEKS  : this.maturity = referenceDate.plusDays(maturity * 7); this.termination = this.maturity.plusDays(termination * 7); break;
			default : throw new IOException("TableConvention "+tableConvention+" not recognized.");
			}
		}

		@Override
		public String toString() {
			return moneyness +"/"+maturity+"/"+termination;
		}
	}
}
