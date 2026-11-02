package net.finmath.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.NotSerializableException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileUtilitiesTest {

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void serializedObjectsRoundTripAndReplaceExistingContents() throws Exception {
		final File file = temporaryFolder.newFile("objects.ser");
		final List<String> values = new ArrayList<>(Arrays.asList("EUR", "USD", "GBP"));
		FileUtilities.writeObject(file, values);
		assertEquals(values, FileUtilities.loadObject(file));

		FileUtilities.writeObject(file, Integer.valueOf(7));
		assertEquals(Integer.valueOf(7), FileUtilities.loadObject(file));
	}

	@Test
	public void nullObjectAndNullInputPathReturnNull() throws Exception {
		final File file = temporaryFolder.newFile("null.ser");
		FileUtilities.writeObject(file, null);
		assertNull(FileUtilities.loadObject(file));
		assertNull(FileUtilities.loadObject(null));
	}

	@Test
	public void invalidSerializedDataAndNonSerializableObjectsAreRejected() throws IOException {
		final File file = temporaryFolder.newFile("invalid.ser");
		FileUtilities.writeObject(file, "truncated object");
		final byte[] serialized = Files.readAllBytes(file.toPath());
		Files.write(file.toPath(), Arrays.copyOf(serialized, serialized.length - 1));
		assertThrows(IOException.class, () -> FileUtilities.loadObject(file));
		assertThrows(IOException.class, () -> FileUtilities.loadObject(new File(temporaryFolder.getRoot(), "missing.ser")));
		assertThrows(NotSerializableException.class, () -> FileUtilities.writeObject(file, new Object()));
	}
}
