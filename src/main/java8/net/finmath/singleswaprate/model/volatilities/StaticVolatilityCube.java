package net.finmath.singleswaprate.model.volatilities;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.singleswaprate.model.VolatilityCubeModel;

/**
 * A volatility cube that always returns the given value.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class StaticVolatilityCube implements VolatilityCube {

	private final String name;
	private final LocalDate referenceDate;

	private final double correlationDecay;
	private final double iborOisDecorrelation;

	private final double value;

	//	private final QuotingConvention quotingConvention = QuotingConvention.VOLATILITYNORMAL;

	/**
	 * Create the cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param correlationDecay The correlation decay parameter of the cube.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter of the cube.
	 * @param value The value this cube is to return.
	 */
	public StaticVolatilityCube(final String name, final LocalDate referenceDate, final double correlationDecay, final double iborOisDecorrelation, final double value) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.correlationDecay = correlationDecay;
		this.value = value;
		this.iborOisDecorrelation = iborOisDecorrelation;
	}

	/**
	 * Create the cube. With ibor ois decorrelation set to 1.0.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param correlationDecay The correlation decay parameter of the cube.
	 * @param value The value this cube is to return.
	 */
	public StaticVolatilityCube(final String name, final LocalDate referenceDate, final double correlationDecay, final double value) {
		this(name, referenceDate, correlationDecay, 1.0, value);
	}

	/**
	 * Create the cube. With ibor ois decorrelation set to 1.0 and correlation decay set to 0.0.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param value The value this cube is to return.
	 */
	public StaticVolatilityCube(final String name, final LocalDate referenceDate, final double value) {
		this(name, referenceDate, 0.0, 1.0, value);
	}

	@Override
	public double getValue(final VolatilityCubeModel model, final double termination, final double maturity, final double strike,
			final QuotingConvention quotingConvention) {
		return value;
	}

	@Override
	public double getValue(final double termination, final double maturity, final double strike, final QuotingConvention quotingConvention) {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	@Override
	public double getCorrelationDecay() {
		return correlationDecay;
	}

	@Override
	public double getIborOisDecorrelation() {
		return iborOisDecorrelation;
	}

	@Override
	public Map<String, Object> getParameters() {
		final Map<String, Object> map = new HashMap<>();
		map.put("value", value);
		map.put("Inherent correlationDecay", correlationDecay);
		map.put("iborOisDecorrelation", iborOisDecorrelation);
		return map;
	}

	@Override
	public double getLowestStrike(final VolatilityCubeModel model) {
		return Double.NEGATIVE_INFINITY;
	}

}
