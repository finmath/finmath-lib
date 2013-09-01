/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 * 
 * Created on 21.06.2008
 */
package net.finmath.optimizer;

/**
 * Exception thrown by solvers <code>net.finmath.rootfinder</code> or <code>net.finmath.optimizer</code>.
 *
 * @author Christian Fries
 */
public class SolverException extends Exception {

    private static final long serialVersionUID = 7123998462171729835L;

	/**
	 * @param message
	 */
	public SolverException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public SolverException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public SolverException(String message, Throwable cause) {
		super(message, cause);
	}

}
