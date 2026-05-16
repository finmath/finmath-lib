package net.finmath.util;

import java.util.HashMap;

public class Java8BackportUtil {
	
	/**
	 * Handy replacement if we use Map.of(k1, v1) which is not available in Java 8.
	 * Just replace the import java.util.Map by import static net.finmath.util.Java8BackportUtil.Map
	 * (given that this is the only use of java.util.Map, e.g. because it is used inline only).
	 */
	public static class Map<K, V> extends java.util.HashMap<K, V> {
		public static java.util.Map of(Object k1, Object v1) {
			java.util.Map map = new HashMap();
			map.put(k1,v1);
			return map;
		}
	}

}
