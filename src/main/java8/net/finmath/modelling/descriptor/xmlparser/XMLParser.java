package net.finmath.modelling.descriptor.xmlparser;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import net.finmath.modelling.ProductDescriptor;

/**
 * Interface for XML parsers creating a product descriptor from an XML file.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public interface XMLParser {

	/**
	 * Parse a product descriptor from a file.
	 *
	 * @param file File containing a trade.
	 * @return Product descriptor extracted from the file.
	 * @throws SAXException Thrown by the xml parser.
	 * @throws IOException Thrown if the file in not found or another IO error occured.
	 * @throws ParserConfigurationException Thrown by the xml parser.
	 */
	ProductDescriptor getProductDescriptor(File file) throws SAXException, IOException, ParserConfigurationException;

}
