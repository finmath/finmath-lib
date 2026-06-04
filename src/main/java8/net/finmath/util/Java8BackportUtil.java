package net.finmath.util;

import java.util.HashMap;

public class Java8BackportUtil {

	/**
	 * Handy replacement if we use List.of(o1,...) which is not available in Java 8.
	 * Just replace the import java.util.List by import static net.finmath.util.Java8BackportUtil.List
	 * given that this is the only use of java.util.List, e.g. because it is used inline only.
	 */
	/**
	 * @param <E> The type of the elements of the list.
	 */
	public static class List<E> extends java.util.ArrayList<E> {
		/**
		 * @param <E> The type of the elements of the list.
		 * @param objs The objects to add to the list.
		 * @return List of the given objects.
		 */
		@SafeVarargs
		public static <E> java.util.List<E> of(final E... objs) {
			final java.util.List<E> list = new java.util.ArrayList<E>();
			for(final E obj : objs) {
				list.add(obj);
			}
			return list;
		}
	}

	/**
	 * Handy replacement if we use Map.of(k1, v1) which is not available in Java 8.
	 * Just replace the import java.util.Map by import static net.finmath.util.Java8BackportUtil.Map
	 * (given that this is the only use of java.util.Map, e.g. because it is used inline only).
	 * 
	 * @param <K> The type of the key.
	 * @param <V> The type of the value.
	 */
	public static class Map<K, V> extends java.util.HashMap<K, V> {
		/**
		 * @param objs The objects to add to the map.
		 * @return The map of the given objects.
		 */
		public static java.util.Map of(final Object... objs) {
			final java.util.Map map = new HashMap();
			for(int i=0; i<objs.length; i+=2) {
				map.put(objs[i],objs[i+1]);
			}
			return map;
		}
	}

}
