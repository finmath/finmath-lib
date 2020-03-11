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
 * @version 1.0
 */
public class FutureWrapper<V> implements Future<V> {

	private final V object;

	/**
	 * Create a wrapper to an object that looks like a Future on that object.
	 *
	 * @param object Object to wrap.
	 */
	public FutureWrapper(final V object) {
		super();
		this.object = object;
	}

	@Override
	public boolean cancel(final boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public V get() {
		return object;
	}

	@Override
	public V get(final long arg0, final TimeUnit arg1) {
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
