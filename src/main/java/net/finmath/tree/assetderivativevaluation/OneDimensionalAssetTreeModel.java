package net.finmath.tree.assetderivativevaluation;

/**
 * From this level of abstraction, we mean that the tree should represent a stock price
 * and not, for example, an interest rate.
 * 
 * @author Alessandro Gnoatto
 */
public interface OneDimensionalAssetTreeModel {

	/**
	 * The risk factor is a stock
	 * @return the initial stock price
	 */
	public double getInitialPrice();

	/**
	 * The risk free rate
	 * @return the risk free rate
	 */
	public double getRiskFreeRate();

	/**
	 * Returns the volatility
	 * @return the volatility
	 */
	public double getVolatility();

}
