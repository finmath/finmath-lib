package net.finmath.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
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
		final File archive = temporaryFolder.newFile(name);
		try(ZipOutputStream stream = new ZipOutputStream(new FileOutputStream(archive))) {
			stream.putNextEntry(new ZipEntry("20260102.csv"));
			stream.write(data.getBytes(StandardCharsets.UTF_8));
			stream.closeEntry();
		}
		return archive;
	}
}
