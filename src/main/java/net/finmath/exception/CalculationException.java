/*
 * Created on 31.12.2009
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.exception;

/**
 * @author Christian Fries
 */
public class CalculationException extends Exception {

	/**
     * 
     */
    private static final long serialVersionUID = 6848163003188948320L;

	/**
	 * 
	 */
	public CalculationException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public CalculationException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public CalculationException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public CalculationException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

}
