/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 14.01.2018
 */
package net.finmath.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Provides utility method to write an object to a file and read an object from a file.
 *
 * @author Christian Fries
 */
public class FileUtilities {

	public static Object loadObject(final File pathToFile) throws ClassNotFoundException, IOException {
		if(pathToFile == null) {
			return null;
		}

		// Load serialized object
		final FileInputStream fis = new FileInputStream(pathToFile);
		Object object = null;
		try(ObjectInputStream in = new ObjectInputStream(fis)) {
			object = in.readObject();
		} catch (final IOException e) {
			throw e;
		}
		return object;
	}

	public static void writeObject(final File pathToFile, final Object object) throws IOException {
		/*
		 * Write object to file
		 */
		final FileOutputStream fos = new FileOutputStream(pathToFile, false /* append */);
		try(ObjectOutputStream out = new ObjectOutputStream(fos)) {
			out.writeObject(object);
			out.flush();
		} catch (final IOException e) {
			throw e;
		}
	}
}
