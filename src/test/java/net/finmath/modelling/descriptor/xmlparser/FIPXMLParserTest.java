package net.finmath.modelling.descriptor.xmlparser;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.ParserConfigurationException;

import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

import net.finmath.modelling.descriptor.InterestRateSwapLegProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateSwapProductDescriptor;

public class FIPXMLParserTest {

	static private File file;
	
	@BeforeClass
	public static void getFile() {
		
		JFileChooser jfc = new JFileChooser(System.getProperty("user.home"));
		jfc.setDialogTitle("Choose XML");
		jfc.setFileFilter(new FileNameExtensionFilter("FIPXML (.xml)", "xml"));
		if(jfc.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
			System.exit(1);
		};
		file = jfc.getSelectedFile();
	}
	
	@Test
	public void testGetSwapProductDescriptor() throws SAXException, IOException, ParserConfigurationException {
		
		InterestRateSwapProductDescriptor descriptor;
		try {
			descriptor = FIPXMLParser.getSwapProductDescriptor(file);
		} catch (IllegalArgumentException e) {
			System.out.println("There was a problem with the file: "+e.getMessage());
//			e.printStackTrace();
			return;
		}
		
		InterestRateSwapLegProductDescriptor legReceiver	= (InterestRateSwapLegProductDescriptor) descriptor.getLegReceiver();
		InterestRateSwapLegProductDescriptor legPayer		= (InterestRateSwapLegProductDescriptor) descriptor.getLegPayer();
		
		System.out.println("Receiver leg:");
		System.out.println(legReceiver.name());
		System.out.println(legReceiver.getForwardCurveName());
		System.out.println(legReceiver.getDiscountCurveName());
		System.out.println(Arrays.toString(legReceiver.getNotionals()));
		System.out.println(legReceiver.getSpread());
		System.out.println(legReceiver.getLegSchedule());
		
		System.out.println("\n\nPayer leg:");
		System.out.println(legPayer.name());
		System.out.println(legPayer.getForwardCurveName());
		System.out.println(legPayer.getDiscountCurveName());
		System.out.println(Arrays.toString(legPayer.getNotionals()));
		System.out.println(legPayer.getSpread());
		System.out.println(legPayer.getLegSchedule());
	}
}
