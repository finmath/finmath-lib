package net.finmath.finitedifference.assetderivativevaluation.products.internal;

import java.util.ArrayDeque;

/**
 * Thread-local stack for temporary product valuation state.
 *
 * <p>
 * This class is intended for product-local state that is needed only during a
 * finite-difference valuation, for example event-condition state used by
 * discretely monitored products. The state is stored in a {@link ThreadLocal}
 * stack so that concurrent valuations of the same product instance and nested
 * valuations within the same thread do not overwrite each other.
 * </p>
 *
 * <p>
 * State is pushed by calling {@link #push(Object)}. The returned {@link Scope}
 * should be closed when the state is no longer needed, preferably by using a
 * try-with-resources block. Closing the scope removes the most recently pushed
 * state from the current thread's stack and clears the thread-local storage
 * when the stack becomes empty.
 * </p>
 *
 * @param <T> The type of valuation state stored in the stack.
 * @author Alessandro Gnoatto
 */
public final class ProductEventStateStack<T> {

	/**
	 * The stack.
	 */
	private transient ThreadLocal<ArrayDeque<T>> stack;

	/**
	 * Pushes a state object onto the current thread's stack.
	 *
	 * <p>
	 * The returned {@link Scope} closes over the pushed state. Calling
	 * {@link Scope#close()} pops the state again. This allows callers to use
	 * the
	 * following pattern:
	 * </p>
	 *
	 * <pre>
	 * try(ProductEventStateStack.Scope ignored = stack.push(state)) {
	 *     // perform valuation using state
	 * }
	 * </pre>
	 *
	 * @param state The state to push.
	 * @return A scope whose {@link Scope#close()} method pops the pushed state.
	 * @throws IllegalArgumentException Thrown if {@code state} is {@code null}.
	 */
	public Scope push(final T state) {
		if (state == null) {
			throw new IllegalArgumentException("state must not be null.");
		}

		getStack().get().push(state);

		return this::pop;
	}

	/**
	 * Returns the current state for the calling thread.
	 *
	 * @return The state at the top of the current thread's stack, or
	 *         {@code null} if the stack is empty.
	 */
	public T currentOrNull() {
		final ArrayDeque<T> currentStack = getStack().get();
		return currentStack.isEmpty() ? null : currentStack.peek();
	}

	/**
	 * Pops the current state from the calling thread's stack.
	 *
	 * <p>
	 * If the stack becomes empty after the pop operation, the thread-local
	 * value
	 * is removed.
	 * </p>
	 *
	 * @throws IllegalStateException Thrown if the current thread's stack is
	 *         empty.
	 */
	private void pop() {
		final ArrayDeque<T> currentStack = getStack().get();

		if (currentStack.isEmpty()) {
			throw new IllegalStateException("No product event state to pop.");
		}

		currentStack.pop();

		if (currentStack.isEmpty()) {
			getStack().remove();
		}
	}

	/**
	 * Returns the current thread-local stack, creating it lazily if necessary.
	 *
	 * @return The thread-local stack.
	 */
	private ThreadLocal<ArrayDeque<T>> getStack() {
		if (stack == null) {
			stack = ThreadLocal.withInitial(ArrayDeque::new);
		}

		return stack;
	}

	/**
	 * Scope object used to release a pushed product event state.
	 *
	 * <p>
	 * Instances are returned by {@link #push(Object)} and are intended for use
	 * with try-with-resources. Closing a scope pops the corresponding state
	 * from
	 * the current thread's stack.
	 * </p>
	 */
	@FunctionalInterface
	public interface Scope extends AutoCloseable {

		/**
		 * Pops the state associated with this scope.
		 */
		@Override
		void close();
	}
}
