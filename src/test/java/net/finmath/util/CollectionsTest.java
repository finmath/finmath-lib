package net.finmath.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

public class CollectionsTest {

	@Test
	public void entryMapPreservesOrderAndCannotBeModified() {
		final SimpleEntry<String, Integer> firstEntry = new SimpleEntry<>("second", 2);
		final Map<String, Integer> map = Collections.orderedMapOf(firstEntry, new SimpleEntry<>("first", 1));

		assertEquals(Arrays.asList("second", "first"), new ArrayList<>(map.keySet()));
		assertEquals(Arrays.asList(2, 1), new ArrayList<>(map.values()));
		firstEntry.setValue(99);
		assertEquals(Integer.valueOf(2), map.get("second"));
		assertThrows(UnsupportedOperationException.class, () -> map.put("third", 3));
		assertThrows(UnsupportedOperationException.class, () -> map.entrySet().iterator().next().setValue(99));
	}

	@Test
	public void keyValueMapPreservesOrderAndCannotBeModified() {
		final Map<String, Integer> map = Collections.orderedMapOf("second", 2, "first", 1);

		assertEquals(Arrays.asList("second", "first"), new ArrayList<>(map.keySet()));
		assertEquals(Arrays.asList(2, 1), new ArrayList<>(map.values()));
		assertThrows(UnsupportedOperationException.class, () -> map.remove("second"));
		assertThrows(UnsupportedOperationException.class, () -> map.values().clear());
	}

	@Test
	public void duplicateKeysAreRejectedEvenWhenTheFirstValueIsNull() {
		assertThrows(IllegalArgumentException.class, () -> Collections.orderedMapOf(
				new SimpleEntry<String, Integer>("duplicate", null), new SimpleEntry<>("duplicate", 1)));
		assertThrows(IllegalArgumentException.class, () -> Collections.orderedMapOf("duplicate", null, "duplicate", 1));
	}
}
