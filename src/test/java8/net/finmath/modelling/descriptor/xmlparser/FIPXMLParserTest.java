package net.finmath.modelling.descriptor.xmlparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.xml.sax.SAXException;

import net.finmath.modelling.descriptor.InterestRateSwapLegProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateSwapProductDescriptor;

@RunWith(Parameterized.class)
public class FIPXMLParserTest {

	@Parameters(name="file={0}}")
	public static Collection<Object[]> generateData()
	{
		/// @TODO Provide a list of test files here
		final ArrayList<Object[]> parameters = new ArrayList<>();
		parameters.add(new Object[] { new File("test.xml") });
		return parameters;
	}

	private final File file;

	/**
	 * This main method will prompt the user for a test file an run the test with the given file.
	 *
	 * @param args Arguments - not used.
	 * @throws ParserConfigurationException Thrown by the parser.
	 * @throws IOException Thrown, e.g., if the file could not be opened.
	 * @throws SAXException Thrown by the XML parser.
	 */
	public static void main(final String[] args) throws SAXException, IOException, ParserConfigurationException
	{
		final JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		jfc.setDialogTitle("Choose XML");
		jfc.setFileFilter(new FileNameExtensionFilter("FIPXML (.xml)", "xml"));
		if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
			System.exit(1);
		}

		(new FIPXMLParserTest(jfc.getSelectedFile())).testGetSwapProductDescriptor();
	}

	public FIPXMLParserTest(final File file) {
		super();
		this.file = file;
	}

	@Test
	public void testGetSwapProductDescriptor() throws SAXException, IOException, ParserConfigurationException {

		InterestRateSwapProductDescriptor descriptor;
		try {
			descriptor = (new FIPXMLParser()).getSwapProductDescriptor(file);
		} catch (final IllegalArgumentException e) {
			System.out.println("There was a problem with the file: "+e.getMessage());
			//			e.printStackTrace();
			return;
		} catch (final FileNotFoundException e) {
			System.out.println("File not found. We will exit gracefully.");
			return;
		}

		final InterestRateSwapLegProductDescriptor legReceiver	= (InterestRateSwapLegProductDescriptor) descriptor.getLegReceiver();
		final InterestRateSwapLegProductDescriptor legPayer		= (InterestRateSwapLegProductDescriptor) descriptor.getLegPayer();

		System.out.println("Receiver leg:");
		System.out.println(legReceiver.name());
		System.out.println(legReceiver.getForwardCurveName());
		System.out.println(legReceiver.getDiscountCurveName());
		System.out.println(Arrays.toString(legReceiver.getNotionals()));
		System.out.println(Arrays.toString(legReceiver.getSpreads()));
		System.out.println(legReceiver.getLegScheduleDescriptor());

		System.out.println("\n\nPayer leg:");
		System.out.println(legPayer.name());
		System.out.println(legPayer.getForwardCurveName());
		System.out.println(legPayer.getDiscountCurveName());
		System.out.println(Arrays.toString(legPayer.getNotionals()));
		System.out.println(Arrays.toString(legPayer.getSpreads()));
		System.out.println(legPayer.getLegScheduleDescriptor());
	}
}
