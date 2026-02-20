package net.finmath.modelling.products;

/**
 * Defines once and for all for the library how we
 * treat calls and puts via EuropeanOption classes.
 * 
 * @author Alessandro Gnoatto
 */
public enum CallOrPut {
	CALL(1),
	PUT(-1);

	private final int value;

	CallOrPut(final int value) {
		this.value = value;
	}

	/**
	 * Returns the sign associated to an option.
	 * @return 1 for a call and -1 for a put.
	 */
	public int toInteger() {
		return value;
	}

}
