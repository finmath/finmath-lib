package net.finmath.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice.QuotingConvention;
import net.finmath.time.SchedulePrototype;

/**
 * Provides options to parse {@link SwaptionDataLattice} from csv files.
 *
 * @author Roland Bachl
 */
public class CSVSwaptionParser {

	/**
	 * Value of volatility in file in multiplied by fileQuotingUnit to achieve a mathematical annualized volatility.
	 * If volatility in file is in percent (1.0 -> 0.01), then set this number to 0.01.
	 */
	private double fileQuotingUnit						= 0.01;
	private double fileQuotingUnitForDisplacement		= 0.01;
	private QuotingConvention fileQuotingConvention		= QuotingConvention.PAYERVOLATILITYLOGNORMAL;

	private final String swaptionCode = "SWOPT";
	private final String csvSplitBy	= ";";

	private final Set<String> maturities;
	private final Set<String> tenors;

	private final SchedulePrototype fixMetaSchedule;
	private final SchedulePrototype floatMetaSchedule;


	/**
	 * Create the parser with no filter on the maturities and tenors.
	 *
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 */
	public CSVSwaptionParser(final SchedulePrototype fixMetaSchedule, final SchedulePrototype floatMetaSchedule) {
		super();
		maturities 		= Collections.<String>emptySet();
		tenors				= Collections.<String>emptySet();
		this.fixMetaSchedule 	= fixMetaSchedule;
		this.floatMetaSchedule	= floatMetaSchedule;
	}

	/**
	 * Create the parser with filter on maturities and tenors.
	 *
	 * @param maturities The maturities, which the parser should consider.
	 * @param tenors The tenors, which the parser should consider.
	 * @param fixMetaSchedule The conventions used for the fixed leg of the swaptions.
	 * @param floatMetaSchedule The conventions used for the float leg of the swaptions.
	 */
	public CSVSwaptionParser(final String[] maturities, final String[] tenors, final SchedulePrototype fixMetaSchedule, final SchedulePrototype floatMetaSchedule) {
		super();
		this.maturities 		= Arrays.stream(maturities).map(String::toUpperCase).collect(Collectors.toCollection(HashSet::new));
		this.tenors 			= Arrays.stream(tenors).map(String::toUpperCase).collect(Collectors.toCollection(HashSet::new));
		this.fixMetaSchedule 	= fixMetaSchedule;
		this.floatMetaSchedule	= floatMetaSchedule;
	}

	/**
	 * Set the quoting convention used in the files, together with their unit and the unit of the displacement.
	 * Values and shifts parsed are multiplied by this unit.
	 *
	 * @param fileQuotingConvention The quoting convention.
	 * @param fileQuotingUnit The quoting unit (a scaling factor).
	 * @param fileQuotingUnitForDisplacement The unit of the displacement (a scaling factor).
	 */
	public void setFileQuotingConvention(final QuotingConvention fileQuotingConvention, final double fileQuotingUnit, final double fileQuotingUnitForDisplacement) {
		this.fileQuotingConvention			= fileQuotingConvention;
		this.fileQuotingUnit				= fileQuotingUnit;
		this.fileQuotingUnitForDisplacement	= fileQuotingUnitForDisplacement;
	}

	/**
	 * Extract a single lattice from the pair of csv files. The parser will not check that the files are aligned for the same reference date.
	 *
	 * @param atmFile The file containing data on atm swpations.
	 * @param otmFile The file containing data on otm swaptions.
	 * @param referenceDate The reference date the swaptions should be created with respect to.
	 * @param currency The currency, which should be parsed from the files.
	 * @param index The index, which should be parsed from the files.
	 * @param discountCurveName The name of the discount curve, which should be used by the swaptions.
	 *
	 * @return The lattice containing the data from the files.
	 *
	 * @throws IOException Thrown upon io error with File.
	 */
	public SwaptionDataLattice parseCSV(final File atmFile, final File otmFile, final LocalDate referenceDate, final String currency, final String index, final String discountCurveName) throws IOException {

		final InputStream atmStream 			= new FileInputStream(atmFile);
		final InputStream otmStream			= new FileInputStream(otmFile);
		final SwaptionDataLattice data		= parseStreams(atmStream, otmStream, referenceDate, currency, index, discountCurveName);
		atmStream.close();
		otmStream.close();

		return data;
	}

	/**
	 * Extract an array of SwaptionDataLattice from the zip files.
	 * The reference dates will be taken from the names of the files inside the archives.
	 * The order of the files must be aligned inside the archives.
	 *
	 * @param atmFile The archive containing data on atm swpations.
	 * @param otmFile The archive containing data on otm swaptions.
	 * @param currency The currency, which should be parsed from the files.
	 * @param index The index, which should be parsed from the files.
	 * @param discountCurveName The name of the discount curve, which should be used by the swaptions.
	 *
	 * @return An array of lattices for each reference date.
	 *
	 * @throws IOException Thrown upon io error with File.
	 */
	public SwaptionDataLattice[] parseZIP(final File atmFile, final File otmFile, final String currency, final String index, final String discountCurveName) throws IOException {

		final ZipFile atmZip = new ZipFile(atmFile);
		final ZipFile otmZip = new ZipFile(otmFile);

		final List<SwaptionDataLattice> lattices = new ArrayList<>();

		final Enumeration<? extends ZipEntry> atmEntries = atmZip.entries();
		final Enumeration<? extends ZipEntry> otmEntries = otmZip.entries();

		while(atmEntries.hasMoreElements() && otmEntries.hasMoreElements()) {
			final ZipEntry atmEntry = atmEntries.nextElement();
			final ZipEntry otmEntry = otmEntries.nextElement();

			final LocalDate referenceDate = LocalDate.parse(atmEntry.getName().replaceAll("\\D", ""), DateTimeFormatter.ofPattern("yyyyMMdd"));
			if(! referenceDate.equals(LocalDate.parse(otmEntry.getName().replaceAll("\\D", ""), DateTimeFormatter.ofPattern("yyyyMMdd")))) {
				atmZip.close();
				otmZip.close();
				throw new IllegalArgumentException("Files in zip archive not aligned for reference date.");
			}

			// TODO Add logging in case stream fails.
			try(InputStream atmStream = atmZip.getInputStream(atmEntry)) {
				try(InputStream otmStream = otmZip.getInputStream(otmEntry)) {
					lattices.add(parseStreams(atmStream, otmStream, referenceDate, currency, index, discountCurveName));
				}
			}
		}

		atmZip.close();
		otmZip.close();

		return lattices.toArray(new SwaptionDataLattice[0]);
	}

	/**
	 * Parse a single lattice from streams. The parser will not check that the files are aligned for the same reference date.
	 *
	 * @param atmStream The stream containing data on atm swpations.
	 * @param otmStream The stream containing data on otm swaptions.
	 * @param referenceDate The reference date the swaptions should be created with respect to.
	 * @param currency The currency, which should be parsed from the files.
	 * @param index The index, which should be parsed from the files.
	 * @param discountCurveName The name of the discount curve, which should be used by the swaptions.
	 *
	 * @return The lattice containing the data from the files.
	 *
	 * @throws IOException Thrown upon io error with InputStream.
	 */
	private SwaptionDataLattice parseStreams(final InputStream atmStream, final InputStream otmStream, final LocalDate referenceDate, final String currency, final String index, final String discountCurveName)
			throws IOException {

		final BufferedReader atmReader = new BufferedReader(new InputStreamReader(atmStream));
		final BufferedReader otmReader = new BufferedReader(new InputStreamReader(otmStream));

		double shift					= 0;
		final ArrayList<String> codes			= new ArrayList<>();
		final ArrayList<Integer> moneynesss	= new ArrayList<>();
		final ArrayList<Double> values		= new ArrayList<>();
		String line;

		//Process atm file
		int i = -1;
		while((line = atmReader.readLine()) != null) {

			i = i + 1;

			final String[] inputs = line.split(csvSplitBy);

			final String maturity = inputs[4].toUpperCase();
			final String tenor	= inputs[3].toUpperCase();

			//Eliminate unnecessary lines.
			if(! (inputs[0].equalsIgnoreCase(currency) && inputs[2].split("_")[0].equalsIgnoreCase(swaptionCode))) {
				continue;
			}
			if((!maturities.isEmpty() && ! maturities.contains(maturity)) || (!tenors.isEmpty() && ! tenors.contains(tenor))) {
				continue;
			}

			//Check if this line contains a shift.
			if(inputs[1].equalsIgnoreCase("SHIFT") ) {
				if(shift == 0) {
					shift = Double.parseDouble(inputs[5]) * fileQuotingUnitForDisplacement;
					continue;
				} else if(shift != (Double.parseDouble(inputs[5]) * fileQuotingUnitForDisplacement)) {
					System.out.println(i);
					System.out.println(line);
					throw new IllegalArgumentException("Shift not alligned for all filtered tenors at reference date " + referenceDate + ".");
				}
			}

			//Otherwise check index.
			if(! inputs[1].equalsIgnoreCase(index)) {
				continue;
			}

			//Extract volatility.
			codes.add(maturity + tenor);
			moneynesss.add(0);
			values.add(Double.parseDouble(inputs[5]) * fileQuotingUnit);
		}

		//Process otm file
		while((line = otmReader.readLine()) != null) {
			final String[] inputs = line.split(csvSplitBy);

			if(inputs.length < 10) {
				continue;
			}

			final String[] tokens = inputs[3].split("/");

			if(tokens.length < 8) {
				continue;
			}

			//Ignore puts, being mirror of calls.
			if(tokens[7].equalsIgnoreCase("P")) {
				continue;
			}

			final String maturity = inputs[8].toUpperCase();
			final String tenor	= tokens[6].toUpperCase();
			final int moneyness	= (int) Double.parseDouble(inputs[4]);

			//Eliminate unnecessary lines.
			if(! (tokens[1].equalsIgnoreCase(currency) && tokens[2].equalsIgnoreCase(index) && tokens[3].equalsIgnoreCase(swaptionCode) && moneyness != 0)) {
				continue;
			}
			if((maturities != null && ! maturities.contains(maturity)) || (tenors != null && tenors.contains(tenor))) {
				continue;
			}

			//Extract volatility.
			codes.add(maturity + tenor);
			moneynesss.add(moneyness);
			values.add(Double.parseDouble(inputs[9]) * fileQuotingUnit);
		}

		return new SwaptionDataLattice(referenceDate, fileQuotingConvention, shift, "Forward_" + currency+"_"+index, discountCurveName, floatMetaSchedule, fixMetaSchedule,
				codes.toArray(new String[0]),
				moneynesss.stream().mapToInt(Integer::intValue).toArray(),
				values.stream().mapToDouble(Double::doubleValue).toArray());
	}

	/**
	 * Extract an array of SwaptionDataLattice from the zip files.
	 * The data in the zip file will be converted to the given quoting convention before storing.
	 * The reference dates will be taken from the names of the files inside the archives.
	 * The order of the files must be aligned inside the archives.
	 * Only the data sets for which a model with matching reference date is provided will be evaluated.
	 *
	 * @param atmFile The archive containing data on atm swpations.
	 * @param otmFile The archive containing data on otm swaptions.
	 * @param currency The currency, which should be parsed from the files.
	 * @param index The index, which should be parsed from the files.
	 * @param discountCurveName The name of the discount curve, which should be used by the swaptions.
	 * @param convention The quoting convention to store the data in.
	 * @param displacement The displacement to use, if storing in convention VOLATILITYLOGNORMAL
	 * @param models The models for context to use for each data set top convert to convention.
	 *
	 * @return An array of lattices for each reference date.
	 *
	 * @throws IOException Thrown upon io error with File.
	 */
	public SwaptionDataLattice[] parseZIPToConvention(final File atmFile, final File otmFile, final String currency, final String index, final String discountCurveName,
			final QuotingConvention convention, final double displacement, final AnalyticModel... models) throws IOException {

		//Convert array of model to map for lookup.
		final Map<LocalDate, AnalyticModel> modelMap = new HashMap<>();
		for(final AnalyticModel model : models) {
			if(((AnalyticModelFromCurvesAndVols) model).getReferenceDate()==null) {
				throw new IllegalArgumentException("No reference date assigned to " + model.toString());
			}
			modelMap.put(((AnalyticModelFromCurvesAndVols) model).getReferenceDate(), model);
		}

		final ZipFile atmZip = new ZipFile(atmFile);
		final ZipFile otmZip = new ZipFile(otmFile);

		final List<SwaptionDataLattice> lattices = new ArrayList<>();

		final Enumeration<? extends ZipEntry> atmEntries = atmZip.entries();
		final Enumeration<? extends ZipEntry> otmEntries = otmZip.entries();

		while(atmEntries.hasMoreElements() && otmEntries.hasMoreElements()) {
			final ZipEntry atmEntry = atmEntries.nextElement();
			final ZipEntry otmEntry = otmEntries.nextElement();

			final LocalDate referenceDate = LocalDate.parse(atmEntry.getName().replaceAll("\\D", ""), DateTimeFormatter.ofPattern("yyyyMMdd"));
			if(! referenceDate.equals(LocalDate.parse(otmEntry.getName().replaceAll("\\D", ""), DateTimeFormatter.ofPattern("yyyyMMdd")))) {
				atmZip.close();
				otmZip.close();
				throw new IllegalArgumentException("Files in zip archive not aligned for reference date.");
			}

			//check if there is a model for these entries
			if(! modelMap.containsKey(referenceDate)) {
				continue;
			}

			lattices.add(parseStreamsToConvention(atmZip, atmEntry, otmZip, otmEntry, referenceDate, currency, index, discountCurveName, convention, displacement,
					modelMap.get(referenceDate)));
		}

		atmZip.close();
		otmZip.close();

		return lattices.toArray(new SwaptionDataLattice[0]);
	}

	/**
	 * Parse a single lattice from streams and save the data in the given convention.
	 * The parser will not check that the files are aligned for the same reference date.
	 *
	 * @param atmZip The zip file containing data on atm swpations.
	 * @param atmEntry The entry to use from the atmZip.
	 * @param otmZip The zip file containing data on otm swaptions.
	 * @param otmEntry The entry to use from the otmZip.
	 * @param referenceDate The reference date the swaptions should be created with respect to.
	 * @param currency The currency, which should be parsed from the files.
	 * @param index The index, which should be parsed from the files.
	 * @param discountCurveName The name of the discount curve, which should be used by the swaptions.
	 * @param convention The quoting convention to store the data in.
	 * @param displacement The displacement to use, if storing in convention VOLATILITYLOGNORMAL
	 * @param model The model for context to use when converting data to convention.
	 *
	 * @return The lattice containing the data from the files.
	 *
	 * @throws IOException Thrown upon io error with ZipFile.
	 */
	private SwaptionDataLattice parseStreamsToConvention(final ZipFile atmZip, final ZipEntry atmEntry, final ZipFile otmZip, final ZipEntry otmEntry, final LocalDate referenceDate,
			final String currency, final String index, final String discountCurveName, final QuotingConvention convention, final double displacement, final AnalyticModel model)
					throws IOException {

		//Prepare empty lattice
		SwaptionDataLattice data = new SwaptionDataLattice(referenceDate, convention, displacement, "Forward_"+currency+"_"+index, discountCurveName, floatMetaSchedule, fixMetaSchedule,
				new String[0], new int[0], new double[0]);

		//Add each individual node on the requested maturity x tenor grid.
		for(final String maturity : maturities) {
			for(final String tenor : tenors) {
				final CSVSwaptionParser partialParser = new CSVSwaptionParser(new String[] {maturity}, new String[] {tenor}, fixMetaSchedule, floatMetaSchedule);
				try(InputStream atmStream = atmZip.getInputStream(atmEntry)) {
					try(InputStream otmStream = otmZip.getInputStream(otmEntry)) {
						data = data.append(partialParser.parseStreams(atmStream, otmStream, referenceDate, currency, index, discountCurveName), model);
					}
				}
			}
		}

		return data;
	}

	/**
	 * Extract a set of lattices from the pair of csv files.
	 * Each lattice contains data for matching displacements.
	 * The parser will not check that the files are alligned for the same reference date.
	 *
	 * @param atmFile The file containing data on atm swpations.
	 * @param otmFile The file containing data on otm swaptions.
	 * @param referenceDate The reference date the swaptions should be created with respect to.
	 * @param currency The currency, which should be parsed from the files.
	 * @param index The index, which should be parsed from the files.
	 * @param discountCurveName The name of the discount curve, which should be used by the swaptions.
	 *
	 * @return The lattices containing the data from the files.
	 *
	 * @throws IOException Thrown upon io error with atmFile.
	 */
	public Set<SwaptionDataLattice> parseCSVMultiShift(final File atmFile, final File otmFile, final LocalDate referenceDate, final String currency, final String index, final String discountCurveName)
			throws IOException {

		final Set<SwaptionDataLattice> lattices	= new HashSet<>();
		final Map<Double, Set<String>> tenorMap	= parseTenorsPerShift(atmFile, currency);

		for(final Set<String> tenors : tenorMap.values()) {
			final CSVSwaptionParser partialParser = new CSVSwaptionParser(maturities.toArray(new String[0]), tenors.toArray(new String[0]), fixMetaSchedule, floatMetaSchedule);

			lattices.add(partialParser.parseCSV(atmFile, otmFile, referenceDate, currency, index, discountCurveName));
		}

		return lattices;
	}

	/**
	 * Create a map overview of which tenors in the given csv file share the same displacement.
	 *
	 * @param atmFile The file containing data on atm swpations.
	 * @param currency The currency, which should be parsed from the files.
	 * @return A map overview of tenors per displacement.
	 *
	 * @throws IOException Thrown upon io error with atmFile.
	 */
	public Map<Double, Set<String>> parseTenorsPerShift(final File atmFile, final String currency) throws IOException {

		final BufferedReader atmReader = new BufferedReader(new FileReader(atmFile));

		String line;
		final Map<Double, Set<String>> map = new HashMap<>();

		//Process atm file
		while((line = atmReader.readLine()) != null) {

			final String[] inputs = line.split(csvSplitBy);

			final String maturity = inputs[4].toUpperCase();
			final String tenor	= inputs[3].toUpperCase();

			//Eliminate unnecessary lines.
			if(! (inputs[0].equalsIgnoreCase(currency) && inputs[2].split("_")[0].equalsIgnoreCase(swaptionCode))) {
				continue;
			}
			if((!maturities.isEmpty() && ! maturities.contains(maturity)) || (!tenors.isEmpty() && ! tenors.contains(tenor))) {
				continue;
			}

			//Check if this line contains a shift.
			if(inputs[1].equalsIgnoreCase("SHIFT") ) {
				final double shift = Double.parseDouble(inputs[5]) * fileQuotingUnitForDisplacement;

				if(!map.containsKey(shift)) {
					map.put(shift, new HashSet<>());
				}
				map.get(shift).add(tenor);
			}
		}

		atmReader.close();
		return Collections.unmodifiableMap(map);
	}

	/**
	 * Extract the reference date of each SwaptionDataLattice in an array.
	 *
	 * @param lattices The array of lattices.
	 * @return Array of the respective reference dates.
	 */
	public static LocalDate[] getReferenceDates(final SwaptionDataLattice[] lattices) {
		return Arrays.stream(lattices).map(SwaptionDataLattice::getReferenceDate).toArray(LocalDate[]::new);
	}
}
