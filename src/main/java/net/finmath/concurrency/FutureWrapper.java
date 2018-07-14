package net.finmath.concurrency;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of the Future interface,
 * without any concurrent execution.
 *
 * This wrapper comes handy, if we want to represent the result of a direct (blocking) valuation
 * as a future to replace concurrent valuation.
 *
 * @author Christian Fries
 *
 * @param <V> The result type returned by this Future's get method.
 */
public class FutureWrapper<V> implements Future<V> {

	private V object;

	/**
	 * Create a wrapper to an object that looks like a Future on that object.
	 *
	 * @param object Object to wrap.
	 */
	public FutureWrapper(V object) {
		super();
		this.object = object;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public V get() {
		return object;
	}

	@Override
	public V get(long arg0, TimeUnit arg1) {
		return object;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		return true;
	}
}

