package net.finmath.modelling.descriptor.xmlparser;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import net.finmath.modelling.ProductDescriptor;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface XMLParser {

	public ProductDescriptor getProductDescriptor(File file) throws SAXException, IOException, ParserConfigurationException;

}
