/*
 * Created on 31.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.exception;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class CalculationException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = 6848163003188948320L;

	/**
	 * A wrapper for exceptions associated with numerical algorithm of finmath lib
	 */
	public CalculationException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * Create an exception with error message.
	 *
	 * @param message The error message.
	 */
	public CalculationException(final String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Create an exception from another exception.
	 *
	 * @param cause The cause.
	 */
	public CalculationException(final Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Create an exception from another exception with error message.
	 *
	 * @param message The error message.
	 * @param cause The cause.
	 */
	public CalculationException(final String message, final Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}
}
