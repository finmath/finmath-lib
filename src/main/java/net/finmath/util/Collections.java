package net.finmath.util;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilities for collections
 * 
 * @author Christian Fries
 */
public class Collections {

	/**
	 * The Java (here Java 11) Map.of implementation does not guarantee that the
	 * entrySet retains the ordering, The following implementation generates a map
	 * that retains the ordering of the arguments.
	 * 
	 * @param <K>     Type of the key.
	 * @param <V>     Type of the value.
	 * @param entries The map entries.
	 * @return Map with ordered entries.
	 */
	@SafeVarargs
	public static <K, V> Map<K, V> orderedMapOf(Map.Entry<K, V>... entries) {
		Map<K, V> map = new LinkedHashMap<>();

		for (Map.Entry<K, V> entry : entries) {
			if (map.containsKey(entry.getKey())) {
				throw new IllegalArgumentException("Duplicate key: " + entry.getKey());
			}
			map.put(entry.getKey(), entry.getValue());
		}

		return java.util.Collections.unmodifiableMap(map);
	}

	public static <K, V> Map<K, V> orderedMapOf(Object... keyAndValues) {
		Map<K, V> map = new LinkedHashMap<>();

		for (int i = 0; i < keyAndValues.length; i += 2) {
			@SuppressWarnings("unchecked")
			K key = (K) keyAndValues[i];
			@SuppressWarnings("unchecked")
			V value = (V) keyAndValues[i + 1];
			if (map.containsKey(key)) {
				throw new IllegalArgumentException("Duplicate key: " + key);
			}
			map.put(key, value);
		}

		return java.util.Collections.unmodifiableMap(map);
	}
}
