package net.finmath.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * A simple wrapper, wrapping a Function<K,V> such that all calculations are cached in a ConcurrentHashMap<K, V>.
 * 
 * @author Christian Fries
 *
 * @param <K> The type of the cache key.
 * @param <V> The type of the value.
 */
public class Cached<K,V> implements Function<K,V> {

	private final Function<K,V> mappingFunction;
	private final Map<K,V> cache = new ConcurrentHashMap<K, V>();

	Cached(Function<K,V> mappingFunction) { this.mappingFunction = mappingFunction; }

	public static <K,V> Function<K,V> of(Function<K,V> mappingFunction) {
		return new Cached<K,V>(mappingFunction);
	}

	@Override
	public V apply(K key) {
		return cache.computeIfAbsent(key, mappingFunction); 
	}
}
