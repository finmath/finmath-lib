package net.finmath.parser;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice;
import net.finmath.marketdata.model.volatilities.SwaptionDataLattice.QuotingConvention;

public class CSVSwaptionParserTest {

	private static final LocalDate REFERENCE_DATE = LocalDate.of(2026, 1, 2);
	private static final String ATM_DATA = "EUR;SHIFT;SWOPT;5Y;1Y;2\n"
			+ "EUR;6M;SWOPT;5Y;1Y;20\n";
	private static final String OTM_DATA = "a;b;c;/EUR/6M/SWOPT/a/b/5Y/C;100;a;b;c;1Y;21\n"
			+ "a;b;c;/EUR/6M/SWOPT/a/b/10Y/C;100;a;b;c;1Y;22\n"
			+ "a;b;c;/EUR/6M/SWOPT/a/b/5Y/C;100;a;b;c;2Y;23\n";

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	private File atmFile;
	private File otmFile;

	@Before
	public void createFiles() throws IOException {
		atmFile = temporaryFolder.newFile("atm.csv");
		otmFile = temporaryFolder.newFile("otm.csv");
		Files.write(atmFile.toPath(), ATM_DATA.getBytes(StandardCharsets.UTF_8));
		Files.write(otmFile.toPath(), OTM_DATA.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	public void noFiltersIncludeAtmAndOtmQuotes() throws IOException {
		assertUnfilteredQuotes(parse(new CSVSwaptionParser(null, null)));
	}

	@Test
	public void emptyFiltersIncludeAtmAndOtmQuotes() throws IOException {
		assertUnfilteredQuotes(parse(new CSVSwaptionParser(new String[0], new String[0], null, null)));
	}

	@Test
	public void maturityAndTenorFiltersIncludeOnlyMatchingQuotes() throws IOException {
		final SwaptionDataLattice data = parse(new CSVSwaptionParser(new String[] { "1Y" }, new String[] { "5Y" }, null, null));
		assertEquals(0.20, data.getValue("1Y5Y", 0), 1E-12);
		assertEquals(0.21, data.getValue("1Y5Y", 100), 1E-12);
		assertFalse(data.containsEntryFor(12, 120, 100));
		assertFalse(data.containsEntryFor(24, 60, 100));
	}

	@Test
	public void maturityFilterLeavesTenorsUnrestricted() throws IOException {
		final SwaptionDataLattice data = parse(new CSVSwaptionParser(new String[] { "1Y" }, new String[0], null, null));
		assertTrue(data.containsEntryFor(12, 60, 100));
		assertTrue(data.containsEntryFor(12, 120, 100));
		assertFalse(data.containsEntryFor(24, 60, 100));
	}

	@Test
	public void tenorFilterLeavesMaturitiesUnrestricted() throws IOException {
		final SwaptionDataLattice data = parse(new CSVSwaptionParser(new String[0], new String[] { "5Y" }, null, null));
		assertTrue(data.containsEntryFor(12, 60, 100));
		assertFalse(data.containsEntryFor(12, 120, 100));
		assertTrue(data.containsEntryFor(24, 60, 100));
	}

	@Test
	public void multiShiftParsingPreservesFileQuotingSettings() throws IOException {
		for(final QuotingConvention convention : new QuotingConvention[] { QuotingConvention.PAYERVOLATILITYLOGNORMAL, QuotingConvention.PAYERVOLATILITYNORMAL }) {
			final double displacementUnit = convention == QuotingConvention.PAYERVOLATILITYLOGNORMAL ? 0.1 : 0;
			final CSVSwaptionParser parser = new CSVSwaptionParser(null, null);
			parser.setFileQuotingConvention(convention, 0.001, displacementUnit);
			final Set<SwaptionDataLattice> lattices = parser.parseCSVMultiShift(atmFile, otmFile, REFERENCE_DATE, "EUR", "6M", "discount");
			assertEquals(1, lattices.size());
			assertCustomQuotes(lattices.iterator().next(), convention, 2 * displacementUnit);
		}
	}

	@Test
	public void zipConversionPreservesFileQuotingSettings() throws IOException {
		final File atmZip = createZip("atm.zip", ATM_DATA);
		final File otmZip = createZip("otm.zip", OTM_DATA);
		for(final QuotingConvention convention : new QuotingConvention[] { QuotingConvention.PAYERVOLATILITYLOGNORMAL, QuotingConvention.PAYERVOLATILITYNORMAL }) {
			final double displacementUnit = convention == QuotingConvention.PAYERVOLATILITYLOGNORMAL ? 0.1 : 0;
			final CSVSwaptionParser parser = new CSVSwaptionParser(new String[] { "1Y" }, new String[] { "5Y" }, null, null);
			parser.setFileQuotingConvention(convention, 0.001, displacementUnit);
			// The source and target conventions match, so conversion needs no curves or schedules.
			final SwaptionDataLattice[] lattices = parser.parseZIPToConvention(atmZip, otmZip, "EUR", "6M", "discount",
					convention, 2 * displacementUnit, new AnalyticModelFromCurvesAndVols(REFERENCE_DATE));
			assertEquals(1, lattices.length);
			assertCustomQuotes(lattices[0], convention, 2 * displacementUnit);
		}
	}

	@Test
	public void zipParsingPreservesEntryDatesAndQuotes() throws IOException {
		final String[] entries = { "20260105.csv", "20260102.csv" };
		final File atmZip = createZip("atm.zip", entries, ATM_DATA, ATM_DATA.replace(";20\n", ";30\n"));
		final File otmZip = createZip("otm.zip", entries, OTM_DATA, OTM_DATA.replace(";21\n", ";31\n"));
		final SwaptionDataLattice[] lattices = new CSVSwaptionParser(null, null).parseZIP(atmZip, otmZip, "EUR", "6M", "discount");

		assertArrayEquals(new LocalDate[] { LocalDate.of(2026, 1, 5), REFERENCE_DATE }, CSVSwaptionParser.getReferenceDates(lattices));
		assertUnfilteredQuotes(lattices[0]);
		assertEquals(0.30, lattices[1].getValue("1Y5Y", 0), 1E-12);
		assertEquals(0.31, lattices[1].getValue("1Y5Y", 100), 1E-12);
	}

	@Test
	public void zipParsingRejectsMismatchedEntryDates() throws IOException {
		final File atmZip = createZip("atm.zip", ATM_DATA);
		final File otmZip = createZip("otm.zip", new String[] { "20260105.csv" }, OTM_DATA);
		final CSVSwaptionParser parser = new CSVSwaptionParser(new String[] { "1Y" }, new String[] { "5Y" }, null, null);

		assertThrows(IllegalArgumentException.class, () -> parser.parseZIP(atmZip, otmZip, "EUR", "6M", "discount"));
		assertThrows(IllegalArgumentException.class, () -> parser.parseZIPToConvention(atmZip, otmZip, "EUR", "6M", "discount",
				QuotingConvention.PAYERVOLATILITYLOGNORMAL, 0.02, new AnalyticModelFromCurvesAndVols(REFERENCE_DATE)));
	}

	@Test
	public void zipConversionSelectsOnlyDatesWithModels() throws IOException {
		final String[] entries = { "20260105.csv", "20260102.csv" };
		final File atmZip = createZip("atm.zip", entries, ATM_DATA, ATM_DATA.replace(";20\n", ";30\n"));
		final File otmZip = createZip("otm.zip", entries, OTM_DATA, OTM_DATA.replace(";21\n", ";31\n"));
		final CSVSwaptionParser parser = new CSVSwaptionParser(new String[] { "1Y" }, new String[] { "5Y" }, null, null);
		final SwaptionDataLattice[] lattices = parser.parseZIPToConvention(atmZip, otmZip, "EUR", "6M", "discount",
				QuotingConvention.PAYERVOLATILITYLOGNORMAL, 0.02, new AnalyticModelFromCurvesAndVols(REFERENCE_DATE));

		assertEquals(1, lattices.length);
		assertEquals(REFERENCE_DATE, lattices[0].getReferenceDate());
		assertEquals(0.30, lattices[0].getValue("1Y5Y", 0), 1E-12);
		assertEquals(0.31, lattices[0].getValue("1Y5Y", 100), 1E-12);
	}

	@Test
	public void zipConversionRejectsModelsWithoutReferenceDates() throws IOException {
		final File atmZip = createZip("atm.zip", ATM_DATA);
		final File otmZip = createZip("otm.zip", OTM_DATA);
		final CSVSwaptionParser parser = new CSVSwaptionParser(new String[] { "1Y" }, new String[] { "5Y" }, null, null);

		assertThrows(IllegalArgumentException.class, () -> parser.parseZIPToConvention(atmZip, otmZip, "EUR", "6M", "discount",
				QuotingConvention.PAYERVOLATILITYLOGNORMAL, 0.02, new AnalyticModelFromCurvesAndVols()));
	}

	@Test
	public void irrelevantRowsDoNotOverwriteValidQuotes() throws IOException {
		Files.write(atmFile.toPath(), (ATM_DATA
				+ "USD;6M;SWOPT;5Y;1Y;99\n"
				+ "EUR;3M;SWOPT;5Y;1Y;99\n"
				+ "EUR;6M;OTHER;5Y;1Y;99\n").getBytes(StandardCharsets.UTF_8));
		Files.write(otmFile.toPath(), (OTM_DATA
				+ "short;row\n"
				+ "a;b;c;incomplete/code;100;a;b;c;1Y;99\n"
				+ "a;b;c;/EUR/6M/SWOPT/a/b/5Y/P;100;a;b;c;1Y;99\n"
				+ "a;b;c;/EUR/6M/SWOPT/a/b/5Y/C;0;a;b;c;1Y;99\n"
				+ "a;b;c;/USD/6M/SWOPT/a/b/5Y/C;100;a;b;c;1Y;99\n"
				+ "a;b;c;/EUR/3M/SWOPT/a/b/5Y/C;100;a;b;c;1Y;99\n"
				+ "a;b;c;/EUR/6M/OTHER/a/b/5Y/C;100;a;b;c;1Y;99\n").getBytes(StandardCharsets.UTF_8));

		final SwaptionDataLattice data = parse(new CSVSwaptionParser(null, null));
		assertEquals(4, data.size());
		assertUnfilteredQuotes(data);
	}

	@Test
	public void tenorGroupingRespectsDisplacementUnitsAndFilters() throws IOException {
		Files.write(atmFile.toPath(), (ATM_DATA
				+ "EUR;SHIFT;SWOPT;10Y;1Y;2\n"
				+ "EUR;SHIFT;SWOPT;10Y;2Y;3\n"
				+ "EUR;SHIFT;SWOPT;20Y;2Y;3\n"
				+ "USD;SHIFT;SWOPT;30Y;1Y;4\n"
				+ "EUR;SHIFT;OTHER;40Y;1Y;5\n").getBytes(StandardCharsets.UTF_8));
		final CSVSwaptionParser parser = new CSVSwaptionParser(null, null);
		parser.setFileQuotingConvention(QuotingConvention.PAYERVOLATILITYLOGNORMAL, 0.01, 0.5);
		final Map<Double, Set<String>> groups = parser.parseTenorsPerShift(atmFile, "EUR");
		assertEquals(2, groups.size());
		assertEquals(new HashSet<>(Arrays.asList("5Y", "10Y")), groups.get(1.0));
		assertEquals(new HashSet<>(Arrays.asList("10Y", "20Y")), groups.get(1.5));

		final CSVSwaptionParser filteredParser = new CSVSwaptionParser(new String[] { "1y" }, new String[] { "10y" }, null, null);
		final Map<Double, Set<String>> filteredGroups = filteredParser.parseTenorsPerShift(atmFile, "EUR");
		assertEquals(1, filteredGroups.size());
		assertEquals(new HashSet<>(Arrays.asList("10Y")), filteredGroups.get(0.02));
	}

	private SwaptionDataLattice parse(final CSVSwaptionParser parser) throws IOException {
		return parser.parseCSV(atmFile, otmFile, REFERENCE_DATE, "EUR", "6M", "discount");
	}

	private void assertUnfilteredQuotes(final SwaptionDataLattice data) {
		assertEquals(0.20, data.getValue("1Y5Y", 0), 1E-12);
		assertEquals(0.21, data.getValue("1Y5Y", 100), 1E-12);
		assertEquals(0.22, data.getValue("1Y10Y", 100), 1E-12);
		assertEquals(0.23, data.getValue("2Y5Y", 100), 1E-12);
	}

	private void assertCustomQuotes(final SwaptionDataLattice data, final QuotingConvention convention, final double displacement) {
		assertEquals(convention, data.getQuotingConvention());
		assertEquals(displacement, data.getDisplacement(), 1E-12);
		assertEquals(0.020, data.getValue("1Y5Y", 0), 1E-12);
		assertEquals(0.021, data.getValue("1Y5Y", 100), 1E-12);
	}

	private File createZip(final String name, final String data) throws IOException {
		return createZip(name, new String[] { "20260102.csv" }, data);
	}

	private File createZip(final String name, final String[] entryNames, final String... data) throws IOException {
		final File archive = temporaryFolder.newFile(name);
		try(ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(archive))) {
			for(int entryIndex = 0; entryIndex < entryNames.length; entryIndex++) {
				stream.putNextEntry(new ZipEntry(entryNames[entryIndex]));
				stream.write(data[entryIndex].getBytes(StandardCharsets.UTF_8));
				stream.closeEntry();
			}
		}
		return archive;
	}
}
