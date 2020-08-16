package net.finmath.singleswaprate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice.QuotingConvention;
import net.finmath.marketdata.products.Swap;
import net.finmath.singleswaprate.data.DataTable;
import net.finmath.singleswaprate.data.DataTable.TableConvention;
import net.finmath.singleswaprate.model.VolatilityCubeModel;
import net.finmath.time.Schedule;
import net.finmath.time.SchedulePrototype;

/**
 * A collection of utility methods for dealing with the {@link net.finmath.singleswaprate} package.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class Utils {

	/**
	 * Convert a {@link DataTable} containing swaption data to a {@link SwaptionDataLattice}.
	 * The table needs to be in {@link TableConvention#MONTHS}.
	 *
	 * @param table The table in convention {@link TableConvention#MONTHS} containing swaption data.
	 * @param quotingConvention The quoting convention of the data.
	 * @param referenceDate The reference date associated with the swaptions.
	 * @param discountCurveName The name of the discount curve to be used for the swaptions.
	 * @param forwardCurveName The name of the forward curve to be used for the swaptions.
	 * @param fixMetaSchedule The ScheduleMetaData to be used for the fix schedules of the swaptions.
	 * @param floatMetaSchedule The ScheduleMetaData to be used for the float schedules of the swaptions.
	 * @return SwaptionDataLattice containing the swaptions of the table.
	 */
	public static SwaptionDataLattice convertTableToLattice(final DataTable table, final QuotingConvention quotingConvention, final LocalDate referenceDate,
			final String discountCurveName, final String forwardCurveName, final SchedulePrototype fixMetaSchedule, final SchedulePrototype floatMetaSchedule) {

		return convertMapOfTablesToLattice(
				new HashMap<Integer, DataTable>() {private static final long serialVersionUID = 1L; { put(0, table); }},
				quotingConvention,
				referenceDate,
				discountCurveName,
				forwardCurveName,
				fixMetaSchedule,
				floatMetaSchedule);
	}

	/**
	 * Convert a map of {@link DataTable} containing swaption data to a {@link SwaptionDataLattice}.
	 * The data of the swaptions is arranged in tables by moneyness, which is used as key in the map.
	 * The tables need to be in {@link TableConvention#MONTHS}.
	 *
	 * @param tables A map of tables, containing swaption data in convention {@link TableConvention#MONTHS}, per moneyness.
	 * @param quotingConvention The quoting convention of the data.
	 * @param referenceDate The reference date associated with the swaptions.
	 * @param discountCurveName The name of the discount curve to be used for the swaptions.
	 * @param forwardCurveName The name of the forward curve to be used for the swaptions.
	 * @param fixMetaSchedule The ScheduleMetaData to be used for the fix schedules of the swaptions.
	 * @param floatMetaSchedule The ScheduleMetaData to be used for the float schedules of the swaptions.
	 * @return SwaptionDataLattice containing the swaptions of the tables.
	 */
	public static SwaptionDataLattice convertMapOfTablesToLattice(final Map<Integer, DataTable> tables, final QuotingConvention quotingConvention, final LocalDate referenceDate,
			final String discountCurveName, final String forwardCurveName, final SchedulePrototype fixMetaSchedule, final SchedulePrototype floatMetaSchedule) {

		final List<Integer> moneynesss = new ArrayList<>();
		final List<Integer> maturities = new ArrayList<>();
		final List<Integer> tenors	 = new ArrayList<>();
		final List<Double>  values 	 = new ArrayList<>();

		for(final int moneyness : tables.keySet()) {
			final DataTable table = tables.get(moneyness);
			if(table.getConvention() != TableConvention.MONTHS) {
				throw new IllegalArgumentException("This method is only set up to handle tables with convention 'inMONTHS'.");
			}
			for(final int maturity : table.getMaturities()) {
				for(final int termination : table.getTerminationsForMaturity(maturity)) {
					moneynesss.add(moneyness);
					maturities.add(maturity);
					tenors.add(termination);
					values.add(table.getValue(maturity, termination));
				}
			}
		}

		return new SwaptionDataLattice(referenceDate, quotingConvention, 0, forwardCurveName, discountCurveName, floatMetaSchedule, fixMetaSchedule,
				maturities.stream().mapToInt(Integer::intValue).toArray(),
				tenors.stream().mapToInt(Integer::intValue).toArray(),
				moneynesss.stream().mapToInt(Integer::intValue).toArray(),
				values.stream().mapToDouble(Double::doubleValue).toArray());
	}

	/**
	 * Create smiles for physically settled swaptions by shifting the smiles from cash settled swaptions onto atm levels of physically settled swaptions.
	 *
	 * @param model Contains curves to translate swaption data to normal volatility. Can be null, if data already in normal volatility.
	 * @param physicalSwaptions The physically settled atm swaptions.
	 * @param cashSwaptions The smile points with corresponding atm nodes of cash swaptions.
	 * @return The lattice containing the shifted physically settled swaption smiles.
	 */
	public static SwaptionDataLattice shiftCashToPhysicalSmile(final VolatilityCubeModel model, final SwaptionDataLattice physicalSwaptions, final SwaptionDataLattice... cashSwaptions) {

		SwaptionDataLattice physicalLattice = physicalSwaptions.convertLattice(QuotingConvention.PAYERVOLATILITYNORMAL, model);

		for(final SwaptionDataLattice cashLatticeUnconverted : cashSwaptions) {
			final SwaptionDataLattice cashLattice = convertCashLatticeToNormalVolatility(cashLatticeUnconverted, model);

			final List<Integer> smileMoneynesss	= new ArrayList<>();
			final List<Integer> smileMaturities	= new ArrayList<>();
			final List<Integer> smileTerminations	= new ArrayList<>();
			final List<Double>  smileVolatilities	= new ArrayList<>();

			for(final int moneyness : cashLattice.getMoneyness()) {
				if(moneyness == 0) {
					continue;
				}

				for(final int maturity : cashLattice.getMaturities(moneyness)) {
					for(final int termination : cashLattice.getTenors(moneyness, maturity)) {
						if((!cashLattice.containsEntryFor(maturity, termination, 0)) || (!physicalLattice.containsEntryFor(maturity, termination, 0)) ) {
							continue;
						}

						smileMoneynesss.add(moneyness);
						smileMaturities.add(maturity);
						smileTerminations.add(termination);
						smileVolatilities.add(physicalLattice.getValue(maturity, termination, 0) +
								cashLattice.getValue(maturity, termination, moneyness) - cashLattice.getValue(maturity, termination, 0));
					}
				}
			}

			final SwaptionDataLattice newSwaptions = new SwaptionDataLattice(cashLattice.getReferenceDate(), QuotingConvention.PAYERVOLATILITYNORMAL, cashLattice.getForwardCurveName(),
					cashLattice.getDiscountCurveName(), cashLattice.getFloatMetaSchedule(), cashLattice.getFixMetaSchedule(),
					smileMaturities.stream().mapToInt(Integer::intValue).toArray(),
					smileTerminations.stream().mapToInt(Integer::intValue).toArray(),
					smileMoneynesss.stream().mapToInt(Integer::intValue).toArray(),
					smileVolatilities.stream().mapToDouble(Double::doubleValue).toArray());

			physicalLattice = physicalLattice.append(newSwaptions, model);
		}

		return physicalLattice;
	}

	/**
	 * Convert a lattice containing cash settled swaption prices to payer normal volatilities.
	 * Conversion assumes put-call-parity.
	 *
	 * @param cashLattice The lattice of cash settled swaptions.
	 * @param model The model containing curves for conversion.
	 * @return The converted lattice.
	 */
	public static SwaptionDataLattice convertCashLatticeToNormalVolatility(final SwaptionDataLattice cashLattice,
			final VolatilityCubeModel model) {

		final SchedulePrototype fixMetaSchedule	= cashLattice.getFixMetaSchedule();
		final SchedulePrototype floatMetaSchedule	= cashLattice.getFloatMetaSchedule();
		final LocalDate referenceDate = cashLattice.getReferenceDate();

		final List<Integer> maturities	= new ArrayList<>();
		final List<Integer> tenors		= new ArrayList<>();
		final List<Integer> moneynesss	= new ArrayList<>();
		final List<Double>  values		= new ArrayList<>();

		boolean isPayer;
		if(cashLattice.getQuotingConvention() == QuotingConvention.PAYERPRICE) {
			isPayer = true;
		} else if(cashLattice.getQuotingConvention() == QuotingConvention.RECEIVERPRICE){
			isPayer = false;
		} else {
			throw new IllegalArgumentException("This conversion assumes a lattice in convention PAYERPRICE or RECEIVERPRICE.");
		}

		for(final int moneyness : cashLattice.getMoneyness()) {
			for(final int maturity : cashLattice.getMaturities(moneyness)) {
				for(final int tenor : cashLattice.getTenors(moneyness, maturity)) {
					final Schedule fixSchedule	= fixMetaSchedule.generateSchedule(referenceDate, maturity, tenor);
					final Schedule floatSchedule	= floatMetaSchedule.generateSchedule(referenceDate, maturity, tenor);
					final double parSwapRate		= Swap.getForwardSwapRate(fixSchedule, floatSchedule, model.getForwardCurve(cashLattice.getForwardCurveName()), model);
					final double strike			= parSwapRate + 0.0001 * (isPayer ? moneyness : - moneyness);
					final double cashAnnuity		= cashFunction(parSwapRate, fixSchedule);
					final double optionValue		= cashLattice.getValue(maturity, tenor, moneyness);

					maturities.add(maturity);
					tenors.add(tenor);
					if(isPayer) {
						moneynesss.add(moneyness);
						values.add(AnalyticFormulas.bachelierOptionImpliedVolatility(parSwapRate, fixSchedule.getFixing(0), strike, cashAnnuity, optionValue));
					} else {
						moneynesss.add(-moneyness);
						values.add(AnalyticFormulas.bachelierOptionImpliedVolatility(parSwapRate, fixSchedule.getFixing(0), strike, cashAnnuity, optionValue
								+ 0.0001 * moneyness * cashAnnuity));
					}
				}
			}
		}
		return new SwaptionDataLattice(referenceDate, QuotingConvention.PAYERVOLATILITYNORMAL, cashLattice.getForwardCurveName(), cashLattice.getDiscountCurveName(),
				floatMetaSchedule, fixMetaSchedule,
				maturities.stream().mapToInt(Integer::intValue).toArray(),
				tenors.stream().mapToInt(Integer::intValue).toArray(),
				moneynesss.stream().mapToInt(Integer::intValue).toArray(),
				values.stream().mapToDouble(Double::doubleValue).toArray());
	}

	/**
	 * Cash function of cash settled swaptions for equidistant tenors.
	 *
	 * @param swapRate The swap rate.
	 * @param schedule The schedule.
	 * @return The result of the cash function.
	 */
	private static double cashFunction(final double swapRate, final Schedule schedule) {

		final int numberOfPeriods = schedule.getNumberOfPeriods();
		double periodLength = 0.0;
		for(int index = 0; index < numberOfPeriods; index++) {
			periodLength += schedule.getPeriodLength(index);
		}
		periodLength /= schedule.getNumberOfPeriods();

		if(swapRate == 0.0) {
			return numberOfPeriods * periodLength;
		} else {
			return (1 - Math.pow(1 + periodLength * swapRate, - numberOfPeriods)) / swapRate;
		}
	}
}
