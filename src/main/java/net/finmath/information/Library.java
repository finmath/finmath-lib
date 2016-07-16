/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 18.08.2014
 */

package net.finmath.information;

import java.util.Properties;

/**
 * Provides information on the finmath-lib library, e.g., the version.
 * 
 * @author Christian Fries
 */
public class Library {

	private static Properties properties = null;

	private Library() { }

	private static Properties getProperites() {
		if(properties == null) {
			properties = new Properties();
			try {
				properties.load(Library.class.getResourceAsStream("/finmath-lib.properties"));
			} catch (Exception e) {
				properties = null;
			}
		}

		return properties;
	}

	/**
	 * Return the version string of this instance of finmath-lib.
	 * 
	 * @return The version string of this instance of finmath-lib.
	 */
	public static String getVersionString() {
		String versionString = "UNKNOWN";
		Properties propeties = getProperites();
		if(propeties != null) versionString = propeties.getProperty("finmath-lib.version");
		return versionString;
	}
}
