package net.finmath.tree.assetderivativevaluation;

/**
 * From this level of abstraction, we mean that the tree should represent a stock price
 * and not, for example, an interest rate.
 *
 * @author Alessandro Gnoatto
 */
public interface OneDimensionalAssetTreeModel {

	/**
	 * The risk factor is a stock.
	 *
	 * @return The initial stock price.
	 */
	double getInitialPrice();

	/**
	 * The risk free rate.
	 *
	 * @return The risk free rate.
	 */
	double getRiskFreeRate();

	/**
	 * Returns the volatility.
	 *
	 * @return The volatility.
	 */
	double getVolatility();
}
