package net.finmath.modelling.descriptor.xmlparser;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import net.finmath.modelling.ProductDescriptor;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public interface XMLParser {

	public static ProductDescriptor getProductDescriptor(File file) throws SAXException, IOException, ParserConfigurationException {
		
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file); 
		doc.getDocumentElement().normalize();
		
		//Check compatibility and assign proper parser
		if(doc.getDocumentElement().getNodeName().equalsIgnoreCase("FIPXML")) {
			return FIPXMLParser.getProductDescriptor(file);
		} else {
			throw new IllegalArgumentException("There is no parser for XML of type "+doc.getDocumentElement().getNodeName()+".");
		}
	}
}
