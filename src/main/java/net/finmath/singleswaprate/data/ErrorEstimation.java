package net.finmath.singleswaprate.data;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.marketdata.products.SwapAnnuity;
import net.finmath.singleswaprate.annuitymapping.AnnuityMapping.AnnuityMappingType;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.singleswaprate.products.CashSettledPayerSwaption;
import net.finmath.singleswaprate.products.CashSettledReceiverSwaption;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * Provides several error estimates between values taken from market data and values taken from a model.
 * The estimates first have to be generated, after which each estimate can be requested.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ErrorEstimation {

	private final LocalDate referenceDate;
	private final SchedulePrototype fixMetaSchedule;
	private final SchedulePrototype floatMetaSchedule;
	private final AnnuityMappingType annuityMappingType;
	private final String discountCurveName;
	private final String forwardCurveName;
	private final String volatilityCubeName;

	private final double replicationLowerBound;
	private final double replicationUpperBound;
	private final int replicationNumberOfEvaluationPoints;

	private final SwaptionDataLattice physicalPremiumsATM;
	private final SwaptionDataLattice cashPayerPremiums;
	private final SwaptionDataLattice cashReceiverPremiums;

	private double[] marketCash;
	private double[] marketPhysical;

	private double[] modelCash;
	private double[] modelPhysical;

	private double[] marketCashTenor;
	private double[] modelCashTenor;
	private int evaluatedMaturity;
	private int evaluatedTermination;


	/**
	 * Create the class.
	 *
	 * @param referenceDate The reference date.
	 * @param fixMetaSchedule The meta data with which to create schedules from the tenor grid for the fixed leg.
	 * @param floatMetaSchedule The meta data with which to create schedules from the tenor grid for the float leg.
	 * @param annuityMappingType The type of annuity mapping to use for cash settled swaptions.
	 * @param physicalPremiumsATM The lattice containing atm physically settled swaption premiums.
	 * @param cashPayerPremiums The lattice containing cash payer premiums.
	 * @param cashReceiverPremiums The lattice containing cash receiver premiums.
	 * @param discountCurveName The name of the discount curve in the model.
	 * @param forwardCurveName The name of the forward curve in the model.
	 * @param volatilityCubeName The name of the volatility cube in the model.
	 * @param replicationLowerBound The lowest strike to use during replication.
	 * @param replicationUpperBound The highest strike to use during replication.
	 * @param replicationNumberOfEvaluationPoints The number of strikes to evaluate during replication.
	 *
	 * @throws IOException Thrown when there is a problem fetching data from the MarketDataHandler.
	 */
	public ErrorEstimation(final LocalDate referenceDate, final SchedulePrototype fixMetaSchedule, final SchedulePrototype floatMetaSchedule, final AnnuityMappingType annuityMappingType,
			final SwaptionDataLattice physicalPremiumsATM, final SwaptionDataLattice cashPayerPremiums, final SwaptionDataLattice cashReceiverPremiums, final String discountCurveName, final String forwardCurveName,
			final String volatilityCubeName, final double replicationLowerBound, final double replicationUpperBound, final int replicationNumberOfEvaluationPoints) throws IOException {
		super();
		this.referenceDate = referenceDate;
		this.fixMetaSchedule = fixMetaSchedule;
		this.floatMetaSchedule = floatMetaSchedule;
		this.annuityMappingType = annuityMappingType;
		this.discountCurveName = discountCurveName;
		this.forwardCurveName = forwardCurveName;
		this.volatilityCubeName = volatilityCubeName;

		this.replicationLowerBound = replicationLowerBound;
		this.replicationUpperBound = replicationUpperBound;
		this.replicationNumberOfEvaluationPoints = replicationNumberOfEvaluationPoints;

		this.physicalPremiumsATM = physicalPremiumsATM;
		this.cashPayerPremiums = cashPayerPremiums;
		this.cashReceiverPremiums = cashReceiverPremiums;

	}

	/**
	 * Evaluate the market data against the model. The nodes to be evaluated on are given by a lattice.
	 * The values of the lattice are not taken into account, only their position. If no lattice (null) is provided, all available data is evaluated.
	 *
	 * @param nodes A lattice indicating on which points errors should be evaluated. Optional.
	 * @param model The model against which to evaluate.
	 */
	public void evaluate(SwaptionDataLattice nodes, final VolatilityCubeModel model) {

		if(nodes == null) {
			nodes = physicalPremiumsATM.append(cashPayerPremiums, model).append(cashReceiverPremiums, model);
		}

		final ArrayList<Double> marketPhysicalList = new ArrayList<>();
		final ArrayList<Double> modelPhysicalList = new ArrayList<>();

		final ArrayList<Double> marketCashPayer = new ArrayList<>();
		final ArrayList<Double> marketCashReceiver = new ArrayList<>();
		final ArrayList<Double> modelCashPayer = new ArrayList<>();
		final ArrayList<Double> modelCashReceiver = new ArrayList<>();


		for(final int maturity : nodes.getMaturities()) {
			for(final int termination : nodes.getTenors()) {
				final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturity, termination);
				final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturity, termination);

				final double optionMaturity 	= fixSchedule.getFixing(0);
				final double swapMaturity		= fixSchedule.getPayment(fixSchedule.getNumberOfPeriods()-1);
				final double annuity = SwapAnnuity.getSwapAnnuity(optionMaturity, fixSchedule, model.getDiscountCurve(discountCurveName), model);
				final double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);
				final double volatility = model.getVolatilityCube(volatilityCubeName).getValue(model, swapMaturity, optionMaturity, swapRate,
						QuotingConvention.VOLATILITYNORMAL);

				marketPhysicalList.add(physicalPremiumsATM.getValue(0, maturity, termination));
				modelPhysicalList.add(AnalyticFormulas.bachelierOptionValue(swapRate, volatility, optionMaturity, swapRate, annuity));

				for(final int moneyness : cashPayerPremiums.getMoneyness()) {

					if(cashPayerPremiums.containsEntryFor(maturity, termination, moneyness)) {
						final double payerStrike = swapRate + moneyness / 10000.0;

						final CashSettledPayerSwaption payer = new CashSettledPayerSwaption(fixSchedule, floatSchedule, payerStrike, discountCurveName,
								forwardCurveName, volatilityCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound,
								replicationNumberOfEvaluationPoints);

						marketCashPayer.add(cashPayerPremiums.getValue(maturity, termination, moneyness));
						modelCashPayer.add(payer.getValue(optionMaturity, model));
					}

					if(cashReceiverPremiums.containsEntryFor(maturity, termination, moneyness)) {
						final double receiverStrike = swapRate - moneyness / 10000.0;

						final CashSettledReceiverSwaption receiver = new CashSettledReceiverSwaption(fixSchedule, floatSchedule, receiverStrike, discountCurveName,
								forwardCurveName, volatilityCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound,
								replicationNumberOfEvaluationPoints);

						marketCashReceiver.add(cashReceiverPremiums.getValue(maturity, termination, moneyness));
						modelCashReceiver.add(receiver.getValue(optionMaturity, model));
					}
				}
			}
		}

		marketPhysical = marketPhysicalList.stream().mapToDouble(Double::doubleValue).toArray();
		modelPhysical = modelPhysicalList.stream().mapToDouble(Double::doubleValue).toArray();

		marketCashPayer.addAll(marketCashReceiver);
		modelCashPayer.addAll(modelCashReceiver);

		marketCash = marketCashPayer.stream().mapToDouble(Double::doubleValue).toArray();
		modelCash = modelCashPayer.stream().mapToDouble(Double::doubleValue).toArray();

	}

	/**
	 * Evaluate the market data against the model at a specific node of the tenor grid.
	 *
	 * @param maturity The maturity at which to evaluate.
	 * @param termination The termination at which to evaluate.
	 * @param model The model against which to evaluate.
	 */
	private void evaluateTenor(final int maturity, final int termination, final VolatilityCubeModel model) {

		final ArrayList<Double> marketCashPayer = new ArrayList<>();
		final ArrayList<Double> marketCashReceiver = new ArrayList<>();
		final ArrayList<Double> modelCashPayer = new ArrayList<>();
		final ArrayList<Double> modelCashReceiver = new ArrayList<>();

		final Schedule fixSchedule = fixMetaSchedule.generateSchedule(referenceDate, maturity, termination);
		final Schedule floatSchedule = floatMetaSchedule.generateSchedule(referenceDate, maturity, termination);

		final double optionMaturity 	= fixSchedule.getFixing(0);
		final double swapRate = Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(forwardCurveName), model);

		for(final int moneyness : cashPayerPremiums.getMoneyness()) {
			if(cashPayerPremiums.containsEntryFor(maturity, termination, moneyness)) {
				final double payerStrike = swapRate + moneyness / 10000.0;

				final CashSettledPayerSwaption payer = new CashSettledPayerSwaption(fixSchedule, floatSchedule, payerStrike, discountCurveName,
						forwardCurveName, volatilityCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound,
						replicationNumberOfEvaluationPoints);

				marketCashPayer.add(cashPayerPremiums.getValue(maturity, termination, moneyness));
				modelCashPayer.add(payer.getValue(optionMaturity, model));
			}
		}

		for(final int moneyness : cashReceiverPremiums.getMoneyness()) {
			if(cashReceiverPremiums.containsEntryFor(maturity, termination, moneyness)) {
				final double receiverStrike = swapRate - moneyness / 10000.0;

				final CashSettledReceiverSwaption receiver = new CashSettledReceiverSwaption(fixSchedule, floatSchedule, receiverStrike, discountCurveName,
						forwardCurveName, volatilityCubeName, annuityMappingType, replicationLowerBound, replicationUpperBound,
						replicationNumberOfEvaluationPoints);

				marketCashReceiver.add(cashReceiverPremiums.getValue(maturity, termination, moneyness));
				modelCashReceiver.add(receiver.getValue(optionMaturity, model));
			}
		}

		marketCashPayer.addAll(marketCashReceiver);
		modelCashPayer.addAll(modelCashReceiver);

		marketCashTenor = marketCashPayer.stream().mapToDouble(Double::doubleValue).toArray();
		modelCashTenor = modelCashPayer.stream().mapToDouble(Double::doubleValue).toArray();

		evaluatedMaturity = maturity;
		evaluatedTermination = termination;
	}

	/**
	 * Get the average error in cash settled swaption premiums.
	 *
	 * @return The average error in cash settled swaption premiums.
	 */
	public double getCashAverageError() {
		double sum = 0;
		double c = 0;
		for(int i = 0; i < marketCash.length; i++) {
			final double y = Math.abs(marketCash[i] - modelCash[i]) - c;
			final double t = sum + y;
			c = (t - sum) -y;
			sum = t;
		}
		return sum / marketCash.length;
	}

	/**
	 * Get the maximal error in cash settled swaption premiums.
	 *
	 * @return The maximal error in cash settled swaption premiums.
	 */
	public double getCashMaxError() {
		double max = 0;
		for(int i = 0; i < marketCash.length; i++) {
			max = Math.max(max, Math.abs(marketCash[i] - modelCash[i]));
		}
		return max;
	}

	/**
	 * Get the average error in cash settled swaption premiums, in percent difference from the market data.
	 *
	 * @return The average error in cash settled swaption premiums, in percent difference from the market data.
	 */
	public double getCashAverageErrorPercent() {
		double sum = 0;
		double c = 0;
		for(int i = 0; i < marketCash.length; i++) {
			final double y = Math.abs(modelCash[i] / marketCash[i] - 1) - c;
			final double t = sum + y;
			c = (t - sum) - y;
			sum = t;
		}
		return sum / marketCash.length;
	}

	/**
	 * Get the maximal error in cash settled swaption premiums, in percent difference from the market data.
	 *
	 * @return The maximal error in cash settled swaption premiums, in percent difference from the market data.
	 */
	public double getCashMaxErrorPercent() {
		double max = 0;
		for(int i = 0; i < marketCash.length; i++) {
			max = Math.max(max, Math.abs(modelCash[i] / marketCash[i] - 1));
		}
		return max;
	}

	/**
	 * Get the average error in physically settled swaption premiums.
	 *
	 * @return The average error in physically settled swaption premiums.
	 */
	public double getPhysicalAverageError() {
		double sum = 0;
		double c = 0;
		for(int i = 0; i < marketPhysical.length; i++) {
			final double y = Math.abs(marketPhysical[i] - modelPhysical[i]) - c;
			final double t = sum + y;
			c = (t - sum) -y;
			sum = t;
		}
		return sum / marketPhysical.length;
	}

	/**
	 * Get the maximal error in physically settled swaption premiums.
	 *
	 * @return The maximal error in physically settled swaption premiums.
	 */
	public double getPhysicalMaxError() {
		double max = 0;
		for(int i = 0; i < marketPhysical.length; i++) {
			max = Math.max(max, Math.abs(marketPhysical[i] - modelPhysical[i]));
		}
		return max;
	}

	/**
	 * Get the average error in physically settled swaption premiums, in percent difference from the market data.
	 *
	 * @return The average error in physically settled swaption premiums, in percent difference from the market data.
	 */
	public double getPhysicalAverageErrorPercent() {
		double sum = 0;
		double c = 0;
		for(int i = 0; i < marketPhysical.length; i++) {
			final double y = Math.abs(modelPhysical[i] / marketPhysical[i] - 1) - c;
			final double t = sum + y;
			c = (t - sum) - y;
			sum = t;
		}
		return sum / marketPhysical.length;
	}

	/**
	 * Get the maximal error in physically settled swaption premiums, in percent difference from the market data.
	 *
	 * @return The maximal error in physically settled swaption premiums, in percent difference from the market data.
	 */
	public double getPhysicalMaxErrorPercent() {
		double max = 0;
		for(int i = 0; i < marketPhysical.length; i++) {
			max = Math.max(max, Math.abs(modelPhysical[i] / marketPhysical[i] - 1));
		}
		return max;
	}

	/**
	 * Get the average error in cash settled swaption premiums at a specific node on the tenor grid.
	 *
	 * @param maturity The maturity at which to evaluate.
	 * @param termination The termination at which to evaluate.
	 * @param model The model against which to evaluate.
	 * @return The average error in cash settled swaption premiums.
	 */
	public double getCashAverageError(final int maturity, final int termination, final VolatilityCubeModel model) {

		if( (maturity != evaluatedMaturity) || (termination != evaluatedTermination) ) {
			evaluateTenor(maturity, termination, model);
		}

		double sum = 0;
		double c = 0;
		for(int i = 0; i < marketCashTenor.length; i++) {
			final double y = Math.abs(marketCashTenor[i] - modelCashTenor[i]) - c;
			final double t = sum + y;
			c = (t - sum) -y;
			sum = t;
		}
		return sum / marketCashTenor.length;
	}

	/**
	 * Get the maximal error in cash settled swaption premiums at a specific node on the tenor grid.
	 *
	 * @param maturity The maturity at which to evaluate.
	 * @param termination The termination at which to evaluate.
	 * @param model The model against which to evaluate.
	 * @return The maximal error in cash settled swaption premiums.
	 */
	public double getCashMaxError(final int maturity, final int termination, final VolatilityCubeModel model) {

		if( (maturity != evaluatedMaturity) || (termination != evaluatedTermination) ) {
			evaluateTenor(maturity, termination, model);
		}

		double max = 0;
		for(int i = 0; i < marketCashTenor.length; i++) {
			max = Math.max(max, Math.abs(marketCashTenor[i] - modelCashTenor[i]));
		}
		return max;
	}

	/**
	 * Get the average error in cash settled swaption premiums, in percent difference from the market data at a specific node on the tenor grid.
	 *
	 * @param maturity The maturity at which to evaluate.
	 * @param termination The termination at which to evaluate.
	 * @param model The model against which to evaluate.
	 * @return The average error in cash settled swaption premiums, in percent difference from the market data.
	 */
	public double getCashAverageErrorPercent(final int maturity, final int termination, final VolatilityCubeModel model) {

		if( (maturity != evaluatedMaturity) || (termination != evaluatedTermination) ) {
			evaluateTenor(maturity, termination, model);
		}

		double sum = 0;
		double c = 0;
		for(int i = 0; i < marketCashTenor.length; i++) {
			final double y = Math.abs(modelCashTenor[i] / marketCashTenor[i] - 1) - c;
			final double t = sum + y;
			c = (t - sum) - y;
			sum = t;
		}
		return sum / marketCashTenor.length;
	}

	/**
	 * Get the maximal error in cash settled swaption premiums, in percent difference from the market data at a specific node on the tenor grid.
	 *
	 * @param maturity The maturity at which to evaluate.
	 * @param termination The termination at which to evaluate.
	 * @param model The model against which to evaluate.
	 * @return The maximal error in cash settled swaption premiums, in percent difference from the market data.
	 */
	public double getCashMaxErrorPercent(final int maturity, final int termination, final VolatilityCubeModel model) {

		if( (maturity != evaluatedMaturity) || (termination != evaluatedTermination) ) {
			evaluateTenor(maturity, termination, model);
		}

		double max = 0;
		for(int i = 0; i < marketCashTenor.length; i++) {
			max = Math.max(max, Math.abs(modelCashTenor[i] / marketCashTenor[i] - 1));
		}
		return max;
	}

}
