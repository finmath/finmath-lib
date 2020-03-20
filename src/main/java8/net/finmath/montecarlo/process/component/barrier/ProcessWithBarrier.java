/*
 * Created on 29.06.2008
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.montecarlo.process.component.barrier;


/**
 * @author Christian Fries
 * @since finmath-lib 4.1.0
 * @version 1.0
 */
public interface ProcessWithBarrier {

	Barrier getBarrier();

	void setBarrier(Barrier barrier);
}
