package net.finmath.util;

@FunctionalInterface
public interface TriFunction<U, V, W, R> {
	
	R apply(U u, V v, W w);
}
