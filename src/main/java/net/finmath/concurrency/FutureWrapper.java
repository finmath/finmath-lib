package net.finmath.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Implementation of the Future<V> interface,
 * without any concurrent execution.
 * 
 * This wrapper comes handy, if we want direct (blocking) valuation
 * to replace concurrent valuation.
 * 
 * @author Christian Fries
 *
 * @param <V> The result type returned by this Future's get method.
 */
public class FutureWrapper<V> implements Future<V> {

	private V object;
	
	public FutureWrapper(V object) {
		super();
		this.object = object;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public V get() throws InterruptedException, ExecutionException {
		return object;
	}

	@Override
	public V get(long arg0, TimeUnit arg1) throws InterruptedException, ExecutionException, TimeoutException {
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
