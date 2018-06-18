package net.finmath.finitedifference.products;

/**
 * Interface for boundaries conditions provided by 1D FD solvers.
 * 
 * @author Christian Fries
 */
public interface FiniteDifference1DBoundary {

	double valueAtLowerStockPriceBoundary(double stockPrice, double currentTime);

	double valueAtUpperStockPriceBoundary(double stockPrice, double currentTime);
	
}