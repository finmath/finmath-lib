package net.finmath.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.CurveInterpolation.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationEntity;
import net.finmath.marketdata.model.curves.CurveInterpolation.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;

/**
 * Provides options to parse curves.
 *
 * @author Roland Bachl
 */
public class CSVCurveParser {

	private InterpolationMethod interpolationMethod;
	private ExtrapolationMethod extrapolationMethod;
	private InterpolationEntity interpolationEntity;

	/**
	 * Set up the parser with default interpolation.
	 * <ul>
	 * <li><code>InterpolationMethod.LINEAR</code></li>
	 * <li><code>ExtrapolationMethod.CONSTANT</code></li>
	 * <li><code>InterpolationEntity.LOG_OF_VALUE_PER_TIME</code></li>
	 * </ul>
	 */
	public CSVCurveParser() {
		this(InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE_PER_TIME);
	}

	/**
	 * Set up the parser with given interpolation.
	 *
	 * @param interpolationMethod The interpolation method.
	 * @param extrapolationMethod The extrapolation method.
	 * @param interpolationEntity The interpolation entity.
	 */
	public CSVCurveParser(final InterpolationMethod interpolationMethod, final ExtrapolationMethod extrapolationMethod, final InterpolationEntity interpolationEntity) {
		super();
		this.interpolationMethod = interpolationMethod;
		this.extrapolationMethod = extrapolationMethod;
		this.interpolationEntity = interpolationEntity;
	}

	/**
	 * Extract a single discount curve from a csv file.
	 *
	 * @param file The file to be parsed.
	 * @param currency The currency of the curve.
	 * @param index The index of the curve.
	 * @return The discount curve.
	 *
	 * @throws IOException Thrown upon an exception related to File.
	 */
	public DiscountCurve parseCSV(final File file, final String currency, final String index) throws IOException {

		final InputStream stream 				= new FileInputStream(file);
		final DiscountCurve curve	= parseStream(stream, currency, index);
		stream.close();

		return curve;
	}

	/**
	 * Extract an arry of discount curves from a zip archive.
	 *
	 * @param file The archive to be parsed.
	 * @param currency The currency of the curves.
	 * @param index The index of the curves.
	 * @return The array of discount curves.
	 *
	 * @throws IOException Thrown upon an exception related to File.
	 */
	public DiscountCurve[] parseZIP(final File file, final String currency, final String index) throws IOException {

		final List<DiscountCurve> curves = new ArrayList<>();
		try(ZipFile zip = new ZipFile(file)) {

			final Enumeration<? extends ZipEntry> entries = zip.entries();

			while(entries.hasMoreElements()) {
				try(InputStream inputStream = zip.getInputStream(entries.nextElement())) {
					curves.add(parseStream(inputStream, currency, index));
				}
			}
		}

		return curves.toArray(new DiscountCurve[curves.size()]);
	}

	/**
	 * Set interpolation method for parsed curves.
	 *
	 * @param interpolationMethod The interpolation method.
	 * @param extrapolationMethod The extrapolation method.
	 * @param interpolationEntity The interpolation entity.
	 */
	public void setInterpolation(final InterpolationMethod interpolationMethod, final ExtrapolationMethod extrapolationMethod, final InterpolationEntity interpolationEntity) {
		this.interpolationMethod = interpolationMethod;
		this.extrapolationMethod = extrapolationMethod;
		this.interpolationEntity = interpolationEntity;
	}

	/**
	 * Extract the reference date of each curve in an array.
	 *
	 * @param curves The array of curves.
	 * @return Array of the respective reference dates.
	 */
	public static LocalDate[] getReferenceDates(final Curve[] curves) {
		return Arrays.stream(curves).map(Curve::getReferenceDate).toArray(LocalDate[]::new);
	}

	/**
	 * Parse a discount curve from an input stream.
	 *
	 * @param stream
	 * @param currency
	 * @param index
	 * @return
	 * @throws IOException
	 */
	private DiscountCurve parseStream(final InputStream stream, final String currency, final String index) throws IOException {
		System.out.println("Currency " + currency + "Index " + index);

		LocalDate referenceDate;
		final List<Double> times = new ArrayList<>();
		final List<Double> rates = new ArrayList<>();

		final String		csvSplitBy	= ";";
		final BufferedReader reader	= new BufferedReader(new InputStreamReader(stream));
		String	line;

		//Get reference date. Located in A3.
		readNonEmptyLine(reader);
		readNonEmptyLine(reader);
		referenceDate = LocalDate.parse(readNonEmptyLine(reader).replaceFirst("\\D+", ""), DateTimeFormatter.ofPattern("d/MM/yy"));

		while((line = readNonEmptyLine(reader)) != null) {
			final String[] inputs = line.split(csvSplitBy);

			//Eliminate unnecessary lines.
			if(! (inputs[0].equalsIgnoreCase(currency) && inputs[1].equalsIgnoreCase(index))) {
				continue;
			}

			//Extract time and zero rate.
			times.add(Double.parseDouble(inputs[4]) /365);
			rates.add(Double.parseDouble(inputs[5]) /100);
		}

		reader.close();

		return DiscountCurveInterpolation.createDiscountCurveFromZeroRates(currency+"_"+index, referenceDate, times.stream().mapToDouble(Double::doubleValue).toArray(),
				rates.stream().mapToDouble(Double::doubleValue).toArray(), interpolationMethod, extrapolationMethod, interpolationEntity);
	}

	/**
	 * Returns the next line of the reader that is not empty.
	 *
	 * @param reader The reader to be read from.
	 * @return The next non empty line.
	 *
	 * @throws IOException
	 */
	private static String readNonEmptyLine(final BufferedReader reader) throws IOException {
		String line = "";
		while(line.equals("")) {
			line = reader.readLine();
			if(line == null) {
				return null;
			}
		}
		return line;
	}
}
