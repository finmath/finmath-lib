/*
 * Created on 30.05.2004
 *
 * (c) Copyright Christian P. Fries, Germany.
 * Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * This is the interface for a one dimensional root finder
 * implemented as an question-and-answer algorithm.
 * 
 * @author Christian Fries
 * @date 2008-04-06
 */
public interface RootFinder {

	/**
	 * @return Next point for which a value should be set
	 * using <code>setValue</code>.
	 */
	public double getNextPoint();
	
	/**
	 * @param value Value corresponding to point returned
	 * by previous <code>getNextPoint</code> call.
	 */
	public void setValue(double value);		
	
	/**
	 * @return Returns the numberOfIterations.
	 */
	public int getNumberOfIterations();
	
	/**
	 * @return Best point obtained so far
	 */
	public double getBestPoint();

	/**
	 * @return Returns the accuracy.
	 */
	public double getAccuracy();
	
	/**
	 * @return Returns true if further improvement is not possible.
	 */
	public boolean isDone();

}
