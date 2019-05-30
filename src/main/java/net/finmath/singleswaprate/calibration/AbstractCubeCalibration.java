package net.finmath.singleswaprate.calibration;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.optimizer.LevenbergMarquardt;
import net.finmath.optimizer.SolverException;
import net.finmath.singleswaprate.annuitymapping.AnnuityMappingFactory;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.model.volatilities.VolatilityCube;
import net.finmath.singleswaprate.products.CashSettledPayerSwaption;
import net.finmath.singleswaprate.products.CashSettledReceiverSwaption;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * Abstract class providing a default method of calibrating a parametric cube to market data, which can be implemented quickly for any cube by implementing the methods:
 * <ul>
 * <li><code>buildCube</code></li>
 * <li><code>initializeParameters</code></li>
 * <li><code>applyParameterBounds</code></li>
 * </ul>
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public abstract class AbstractCubeCalibration {

	//input
	protected LocalDate referenceDate;

	protected SwaptionDataLattice cashPayerPremiums;
	protected SwaptionDataLattice cashReceiverPremiums;

	protected String discountCurveName;
	protected String forwardCurveName;

	protected VolatilityCubeModel model;

	protected AnnuityMappingType annuityMappingType;

	private int maxIterations = 250;
	private int numberOfThreads = Runtime.getRuntime().availableProcessors();

	private boolean replicationUseAsOffset = true;
	private double replicationLowerBound   = -0.15;
	private double replicationUpperBound   = 0.15;
	private int replicationNumberOfEvaluationPoints = 500;



	//calculation
	protected double[] initialParameters;
	protected double[] calibratedParameters;

	private double[] marketTargets;
	private ArrayList<SwaptionInfo> payerSwaptions = new ArrayList<SwaptionInfo>();
	private ArrayList<SwaptionInfo> receiverSwaptions = new ArrayList<SwaptionInfo>();


	/**
	 * Create the calibrator.
	 *
	 * @param referenceDate The reference date of the cube.
	 * @param cashPayerPremiums The lattice containing market targets for cash settled payer swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param cashReceiverPremiums The lattice containing market targets for cash settled receiver swaptions. The lattice needs to be quoted in QuotingConvention.PRICE.
	 * @param model The model providing context.
	 * @param annuityMappingType The type of annuity mapping to be used for calibration.
	 *
	 * @throws IllegalArgumentException Triggers when data is not provided in QuotingConvention.Price.
	 */
	public AbstractCubeCalibration(LocalDate referenceDate, SwaptionDataLattice cashPayerPremiums, SwaptionDataLattice cashReceiverPremiums,
			VolatilityCubeModel model, AnnuityMappingType annuityMappingType) {
		super();
		if(cashPayerPremiums.getQuotingConvention() != QuotingConvention.PAYERPRICE || cashReceiverPremiums.getQuotingConvention() != QuotingConvention.RECEIVERPRICE) {
			throw new IllegalArgumentException("Swaption data not provided in QuotingConvention.PAYERPRICE and QuotingConvention.RECEIVERPRICE respectively.");
		}
		this.referenceDate = referenceDate;
		this.cashPayerPremiums = cashPayerPremiums;
		this.cashReceiverPremiums = cashReceiverPremiums;
		this.model = model;
		this.annuityMappingType = annuityMappingType;

		discountCurveName	= cashPayerPremiums.getDiscountCurveName();
		forwardCurveName	= cashPayerPremiums.getForwardCurveName();
	}


	// adjust these methods to adapt to different cubes
	/**
	 * Build the cube from a set of parameters. These need to be an array of all parameters to be calibrated.
	 *
	 * @param name The name the cube will carry.
	 * @param parameters The parameters of the cube as array.
	 * @return The volatility cube.
	 */
	protected abstract VolatilityCube buildCube(String name, double[] parameters);

	/**
	 * Prepare the parameters for the start of the calibration.
	 */
	protected abstract void initializeParameters();

	/**
	 * Apply bounds to parameters. Such as volatility larger zero.
	 *
	 * @param parameters The raw parameters of the cube as array.
	 * @return The parameters with their respective bounds applied.
	 */
	protected abstract double[] applyParameterBounds(double[] parameters);

	//following methods apply to all cubes

	/**
	 * Run the calibration.
	 *
	 * @param cubeName The name of the final cube.
	 * @return The calibrated cube.
	 *
	 * @throws SolverException Thrown, when solvers fail to find suitable parameters.
	 */
	public VolatilityCube calibrate(String cubeName) throws SolverException {

		// sort target data for calibration
		generateTargets();

		//setup initial parameters
		initializeParameters();

		runOptimization();

		return buildCube(cubeName, calibratedParameters);
	}

	/**
	 * Run the calibration.
	 *
	 * @throws SolverException Thrown, when solvers fail to find suitable parameters.
	 */
	private void runOptimization() throws SolverException {

		LevenbergMarquardt optimizer = new LevenbergMarquardt(initialParameters, marketTargets, maxIterations, numberOfThreads) {

			/**
			 *
			 */
			private static final long serialVersionUID = -7604474677930060206L;

			@Override
			public void setValues(double[] parameters, double[] values) {

				//apply bounds to the parameters
				parameters = applyParameterBounds(parameters);

				//get volatility cube and add to temporary model
				String tempCubeName = "tempCube";
				VolatilityCube cube = buildCube(tempCubeName, parameters);
				VolatilityCubeModel tempModel = model.addVolatilityCube(cube);

				//pre allocate space
				Schedule floatSchedule;
				Schedule fixSchedule;
				double forwardSwapRate;
				double strike;
				String mappingName;
				AnnuityMapping mapping;
				AnnuityMappingFactory factory;
				Map<String, AnnuityMapping> container = new HashMap<String, AnnuityMapping>();

				int index = 0;
				//calculate cash payer swaption values
				SchedulePrototype fixMetaSchedule	= cashPayerPremiums.getFixMetaSchedule();
				SchedulePrototype floatMetaSchedule	= cashPayerPremiums.getFloatMetaSchedule();
				for(SwaptionInfo swaption : payerSwaptions) {
					fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					forwardSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, tempModel.getForwardCurve(forwardCurveName), tempModel);
					strike = forwardSwapRate + swaption.moneyness;

					double replicationLowerBound =
							replicationUseAsOffset ? forwardSwapRate + AbstractCubeCalibration.this.replicationLowerBound : AbstractCubeCalibration.this.replicationLowerBound;
					double replicationUpperBound =
							replicationUseAsOffset ? forwardSwapRate + AbstractCubeCalibration.this.replicationUpperBound : AbstractCubeCalibration.this.replicationUpperBound;

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

					CashSettledPayerSwaption css = new CashSettledPayerSwaption(fixSchedule, floatSchedule, strike, discountCurveName, forwardCurveName,
							tempCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound, replicationNumberOfEvaluationPoints);
					values[index++] = css.getValue(floatSchedule.getFixing(0), mapping, tempModel);
				}

				//calculate cash receiver swaption values
				fixMetaSchedule		= cashReceiverPremiums.getFixMetaSchedule();
				floatMetaSchedule	= cashReceiverPremiums.getFloatMetaSchedule();
				for(SwaptionInfo swaption : receiverSwaptions) {
					fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, swaption.maturity, swaption.termination);
					forwardSwapRate	= Swap.getForwardSwapRate(fixSchedule, floatSchedule, tempModel.getForwardCurve(forwardCurveName), tempModel);
					strike = forwardSwapRate + swaption.moneyness;

					double replicationLowerBound =
							replicationUseAsOffset ? forwardSwapRate + AbstractCubeCalibration.this.replicationLowerBound : AbstractCubeCalibration.this.replicationLowerBound;
					double replicationUpperBound =
							replicationUseAsOffset ? forwardSwapRate + AbstractCubeCalibration.this.replicationUpperBound : AbstractCubeCalibration.this.replicationUpperBound;

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

					CashSettledReceiverSwaption css = new CashSettledReceiverSwaption(fixSchedule, floatSchedule, strike, discountCurveName,
							forwardCurveName, tempCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound,
							replicationNumberOfEvaluationPoints);
					values[index++] = css.getValue(floatSchedule.getFixing(0), mapping, tempModel);
				}

				// output for testing
				//					StringBuilder builder = new StringBuilder();
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
		System.out.println("Optimizer finished after " +optimizer.getIterations() +" iterations with mean error " + optimizer.getRootMeanSquaredError());

		calibratedParameters = applyParameterBounds(optimizer.getBestFitParameters());
	}

	/**
	 * Build the target array.
	 *
	 */
	private void generateTargets() {

		//convert data tables to swaptions and target array
		//order: cashPayer(strike maturity termination) cashReceiver(strike maturity termination)

		//prep temp variables
		ArrayList<Double> targetsPayer 	  = new ArrayList<Double>();
		ArrayList<Double> targetsReceiver = new ArrayList<Double>();

		//sort all data into array lists
		for(int moneyness : cashPayerPremiums.getMoneyness()) {
			for(int maturity : cashPayerPremiums.getMaturities(moneyness)) {
				for(int termination : cashPayerPremiums.getTenors(moneyness, maturity)) {
					targetsPayer.add(cashPayerPremiums.getValue(maturity, termination, moneyness));
					payerSwaptions.add( new SwaptionInfo(moneyness, maturity, termination));
				}
			}
		}

		for(int moneyness : cashReceiverPremiums.getMoneyness()) {
			for(int maturity : cashReceiverPremiums.getMaturities(moneyness)) {
				for(int termination : cashReceiverPremiums.getTenors(moneyness, maturity)) {
					targetsReceiver.add(cashReceiverPremiums.getValue(maturity, termination, moneyness));
					receiverSwaptions.add( new SwaptionInfo(-moneyness, maturity, termination));
				}
			}
		}

		this.marketTargets = Stream.concat(targetsPayer.stream(), targetsReceiver.stream()).mapToDouble(Double::doubleValue).toArray();
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
	 * Set the parameters for the swaption replication.
	 *
	 * @param useAsOffset Are the values of lower and upperBound to be understood as offsets from the par swap rate?
	 * @param lowerBound The lowest strike allowed.
	 * @param upperBound The maximal strike allowed.
	 * @param numberOfEvaluationPoints The number of points to be evaluated during the integration.
	 */
	public void setReplicationParameters(boolean useAsOffset, double lowerBound, double upperBound, int numberOfEvaluationPoints) {
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

	/**
	 * Compact identifier for the swaptions to be created during the optimization.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	protected class SwaptionInfo {
		final double moneyness;
		final LocalDate maturity;
		final LocalDate termination;

		/**
		 * Create the swaption with convention <code>inMONTHS</code>.
		 *
		 * @param moneyness
		 * @param maturity
		 * @param termination
		 */
		SwaptionInfo(int moneyness, int maturity, int termination) {
			this.moneyness = moneyness / 10000.0;
			this.maturity = referenceDate.plusMonths(maturity);
			this.termination = this.maturity.plusMonths(termination);
		}

		/**
		 * Create the swaption.
		 *
		 * @param moneyness
		 * @param maturity
		 * @param termination
		 * @param tableConvention
		 *
		 * @throws IOException Triggers when the table convention is not recognized.
		 */
		SwaptionInfo(int moneyness, int maturity, int termination, TableConvention tableConvention) throws IOException {
			this.moneyness = moneyness /10000.0;
			switch(tableConvention) {
			case inMONTHS : this.maturity = referenceDate.plusMonths(maturity); this.termination = this.maturity.plusMonths(termination); break;
			case inYEARS  : this.maturity = referenceDate.plusYears(maturity); this.termination = this.maturity.plusYears(termination); break;
			case inDAYS   : this.maturity = referenceDate.plusDays(maturity); this.termination = this.maturity.plusDays(termination); break;
			case inWEEKS  : this.maturity = referenceDate.plusDays(maturity * 7); this.termination = this.maturity.plusDays(termination * 7); break;
			default : throw new IOException("TableConvention "+tableConvention+" not recognized.");
			}
		}

		@Override
		public String toString() {
			return moneyness +"/"+maturity+"/"+termination;
		}
	}
}
