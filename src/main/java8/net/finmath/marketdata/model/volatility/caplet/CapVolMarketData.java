package net.finmath.marketdata.model.volatility.caplet;

/**
 * This class is a container for all the cap data needed to perform the caplet bootstrapping.
 *
 * @author Daniel Willhalm
 * @author Christian Fries (review and fixes)
 */
public class CapVolMarketData {

	private final CapTenorStructure capTenorStructure;
	private final double[] expiryVectorInYears;
	private final int[] expiryVectorInMonths;
	private final double[] strikeVector;
	private final double[][] capVolatilities;
	private final double shift;
	private final int underlyingTenorInMonths;
	private final int tenorChangeTimeInMonths;
	private final int underlyingTenorInMonthsBeforeChange;
	private final String index;
	private final String indexBeforeChange;
	private final String discountIndex;

	/**
	 * The constructor of the cap volatility market data class.
	 * In case the underlying tenor changes throughout the expiry dates two indexes
	 * and tenors are submitted as parameters.
	 *
	 * @param shift The shift of the volatilities.
	 * @param underlyingTenorInMonths The underlying tenor in months.
	 * @param underlyingTenorInMonthsBeforeChange The underlying tenor in months before the tenor change.
	 * @param capVolatilities The matrix with cap volatilities as entries.
	 * @param expiryVectorInMonths The expiry dates given in months.
	 * @param strikeVector The caplet strikes.
	 * @param index The forward curve index.
	 * @param indexBeforeChange The forward curve index before the tenor change.
	 * @param discountIndex The discount curve index.
	 * @param capTenorStructure Enum that determines the currency.
	 * @param tenorChangeTimeInMonths The time in months after which the tenor changes.
	 */
	public CapVolMarketData(
			final String index,
			final String discountIndex,
			final String indexBeforeChange,
			final CapTenorStructure capTenorStructure,
			final int[] expiryVectorInMonths,
			final double[] strikeVector,
			final double[][] capVolatilities,
			final double shift,
			final int underlyingTenorInMonths,
			final int tenorChangeTimeInMonths,
			final int underlyingTenorInMonthsBeforeChange) {
		super();
		this.capTenorStructure = capTenorStructure;
		this.expiryVectorInMonths = expiryVectorInMonths;
		this.strikeVector = strikeVector;
		this.capVolatilities = capVolatilities;
		this.shift = shift;
		this.underlyingTenorInMonths = underlyingTenorInMonths;
		this.tenorChangeTimeInMonths = tenorChangeTimeInMonths;
		this.underlyingTenorInMonthsBeforeChange = underlyingTenorInMonthsBeforeChange;
		this.index = index;
		this.indexBeforeChange = indexBeforeChange;
		this.discountIndex = discountIndex;

		if (this.expiryVectorInMonths.length != this.capVolatilities.length) {
			throw new IllegalArgumentException("number of maturities and matrix not compatible. Maturity dates: " + this.expiryVectorInMonths.length + ", Matrix rows: " + this.capVolatilities[0].length);
		}
		if (this.strikeVector.length != this.capVolatilities[0].length) {
			throw new IllegalArgumentException("number of strikes and matrix not compatible. Strikes: " + this.strikeVector.length + ", Matrix columns: " + this.capVolatilities[0].length);
		}

		expiryVectorInYears = new double[this.expiryVectorInMonths.length];
		for (int i = 0; i < this.expiryVectorInMonths.length; i++) {
			expiryVectorInYears[i] = (this.expiryVectorInMonths[i])/12.0;
		}
	}

	/**
	 * Overloaded constructor of the cap volatility market data class
	 * that assumes no tenor change.
	 *
	 * @param index The forward curve index.
	 * @param discountIndex The discount curve index.
	 * @param capVolatilities The matrix with cap volatilities as entries.
	 * @param expiryVectorInMonths The expiry dates given in months.
	 * @param strikeVector The caplet strikes.
	 * @param capTenorStructure Enum that determines the currency.
	 * @param shift The shift of the volatilities.
	 * @param underlyingTenorInMonths The underlying tenor in months.
	 */
	public CapVolMarketData(final String index, final String discountIndex, final CapTenorStructure capTenorStructure, final int[] expiryVectorInMonths, final double[] strikeVector, final double[][] capVolatilities, final double shift, final int underlyingTenorInMonths) {
		this(index, discountIndex, null, capTenorStructure, expiryVectorInMonths, strikeVector, capVolatilities, shift, underlyingTenorInMonths, 0, underlyingTenorInMonths);
	}

	public double getCapVolData(final int expiry, final double strike) {
		return capVolatilities[getRowIndex(expiry)][getColumnIndex(strike)];
	}

	public double getCapVolData(final int i, final int j) {
		return capVolatilities[i][j];
	}

	public double getShift() {
		return shift;
	}

	public int getNumberOfStrikes() {
		return strikeVector.length;
	}

	public int getNumberOfExpiryDates() {
		return expiryVectorInMonths.length;
	}

	public int getMaxExpiryInMonths() {
		return expiryVectorInMonths[expiryVectorInMonths.length-1];
	}

	public double getMaxExpiryInYears() {
		return expiryVectorInYears[expiryVectorInYears.length-1];
	}

	public int getExpiryInMonths(final int i) {
		return expiryVectorInMonths[i];
	}

	public double getExpiryInYears(final int i) {
		return expiryVectorInYears[i];
	}

	public double[][] getVolMatrix() {
		return capVolatilities;
	}

	public double[] getStrikeVector() {
		return strikeVector;
	}

	public int[] getExpiryVectorInMonths() {
		return expiryVectorInMonths;
	}

	public double[] getExpiryVectorInYears() {
		return expiryVectorInYears;
	}

	public int getRowIndex(final int expiryInMonths) {
		for (int i = 0; i < expiryVectorInMonths.length; i++) {
			if (expiryVectorInMonths[i]==expiryInMonths) {
				return i;
			}
		}
		return -1;
	}

	public double getStrike(final int j) {
		return strikeVector[j];
	}

	public int getColumnIndex(final double strike) {
		for (int j = 0; j < strikeVector.length; j++) {
			if (strikeVector[j]==strike) {
				return j;
			}
		}
		return -1;
	}

	public CapTenorStructure getCapTenorStructure() {
		return capTenorStructure;
	}

	public int getUnderlyingTenorInMonths() {
		return underlyingTenorInMonths;
	}

	public int getUnderlyingTenorInMonthsBeforeChange() {
		return underlyingTenorInMonthsBeforeChange;
	}

	public int getTenorChangeTimeInMonths() {
		return tenorChangeTimeInMonths;
	}

	public String getIndex() {
		return index;
	}

	public String getIndexBeforeChange() {
		return indexBeforeChange;
	}

	public String getDiscountIndex() {
		return discountIndex;
	}

	public void setCapVolMatrixEntry(final int i, final int j, final double newValue) {
		capVolatilities[i][j] = newValue;
	}

	public static String getOffsetCodeFromIndex(final String index) {
		final String[] split = index.split("(?<=\\D)(?=\\d)");
		return split[split.length-1];
	}
}
