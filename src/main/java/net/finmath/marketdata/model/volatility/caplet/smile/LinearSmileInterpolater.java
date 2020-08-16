package net.finmath.marketdata.model.volatility.caplet.smile;

/**
 * This class implements the smile linearly and extrapolates piecewise constant.
 *
 * @author Daniel Willhalm
 */
public class LinearSmileInterpolater implements SmileInterpolationExtrapolationMethod {

	private final double[][] volatilityMatrix;
	private final double[] strikeVector;

	public LinearSmileInterpolater(final double[][] volatilityMatrix, final double[] strikeVector) {
		this.volatilityMatrix = volatilityMatrix;
		this.strikeVector = strikeVector;
	}

	/**
	 * Method that returns the linearly interpolated or constantly extrapolated volatility for a given strike and row index.
	 *
	 * @param strike The desired strike.
	 * @param rowIndex The desired row index.
	 * @return The inter/extrapolated volatility.
	 */
	@Override
	public double calculateInterpolatedExtrapolatedSmileVolatility(final double strike, final int rowIndex) {
		if (strike < strikeVector[0]) {
			//return ((strike-strikeVector[0])*(volatilityMatrix[rowIndex][1]-volatilityMatrix[rowIndex][0])/(strikeVector[1]-strikeVector[0])+volatilityMatrix[rowIndex][0]);
			return volatilityMatrix[rowIndex][0];

		}
		if (strike >= strikeVector[strikeVector.length-1]) {
			//return ((strike-strikeVector[strikeVector.length-1])*(volatilityMatrix[rowIndex][strikeVector.length-1]-volatilityMatrix[rowIndex][strikeVector.length-2])/(strikeVector[strikeVector.length-1]-strikeVector[strikeVector.length-2])+volatilityMatrix[rowIndex][strikeVector.length-1]);
			return volatilityMatrix[rowIndex][strikeVector.length-1];
		}
		int indexLargestStrikeLeftOfInterpolationStrike = 0;
		int i = 0;
		while (strike >= strikeVector[i]) {
			indexLargestStrikeLeftOfInterpolationStrike = i;
			i++;
		}
		if (volatilityMatrix[rowIndex][indexLargestStrikeLeftOfInterpolationStrike+1] == 0.0) {
			return volatilityMatrix[rowIndex][indexLargestStrikeLeftOfInterpolationStrike];
		}
		if (strike == strikeVector[indexLargestStrikeLeftOfInterpolationStrike]) {
			return volatilityMatrix[rowIndex][indexLargestStrikeLeftOfInterpolationStrike];
		}
		return ((strike - strikeVector[indexLargestStrikeLeftOfInterpolationStrike])
				* (volatilityMatrix[rowIndex][indexLargestStrikeLeftOfInterpolationStrike + 1]
						- volatilityMatrix[rowIndex][indexLargestStrikeLeftOfInterpolationStrike])
				/ (strikeVector[indexLargestStrikeLeftOfInterpolationStrike + 1]
						- strikeVector[indexLargestStrikeLeftOfInterpolationStrike])
				+ volatilityMatrix[rowIndex][indexLargestStrikeLeftOfInterpolationStrike]);
	}

}
