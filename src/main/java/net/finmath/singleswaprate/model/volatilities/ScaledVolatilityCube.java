package net.finmath.singleswaprate.model.volatilities;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.singleswaprate.model.VolatilityCubeModel;

/**
 * A volatility cube that always returns a multiple of the value an underlying cube would return.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ScaledVolatilityCube implements VolatilityCube {

	private final String		name;
	private final LocalDate	referenceDate;
	private final String referenceCubeName;
	private final double coefficient;
	private final double correlationDecay;
	private final double iborOisDecorrelation;

	/**
	 * Create the cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param referenceCubeName The name of the underlying cube.
	 * @param coefficient The coefficient with which the value of the underlying cube is to be multiplied.
	 * @param correlationDecay The correlation decay parameter of the cube.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter of the cube.
	 */
	public ScaledVolatilityCube(final String name, final LocalDate referenceDate, final String referenceCubeName, final double coefficient, final double correlationDecay, final double iborOisDecorrelation) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.referenceCubeName = referenceCubeName;
		this.coefficient = coefficient;
		this.correlationDecay = correlationDecay;
		this.iborOisDecorrelation = iborOisDecorrelation;
	}

	public ScaledVolatilityCube(final String name, final LocalDate referenceDate, final String referenceCubeName, final double coefficient, final double correlationDecay) {
		this(name, referenceDate, referenceCubeName, coefficient, correlationDecay, 1.0);
	}

	@Override
	public double getValue(final VolatilityCubeModel model, final double termination, final double maturity, final double strike,
			final QuotingConvention quotingConvention) {
		return model.getVolatilityCube(referenceCubeName).getValue(model, termination, maturity, strike, quotingConvention) * coefficient;
	}

	@Override
	public double getValue(final double termination, final double maturity, final double strike, final QuotingConvention quotingConvention) {
		return getValue(null, termination, maturity, strike, quotingConvention);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	public String getReferenceCubeName() {
		return referenceCubeName;
	}

	@Override
	public double getCorrelationDecay() {
		return correlationDecay;
	}

	@Override
	public Map<String, Object> getParameters() {
		final Map<String,Object> map = new HashMap<>();
		map.put("coefficient", coefficient);
		map.put("Inherent correlationDecay", correlationDecay);
		map.put("iborOisDecorrelation", iborOisDecorrelation);

		return map;
	}

	@Override
	public double getLowestStrike(final VolatilityCubeModel model) {
		return model.getVolatilityCube(referenceCubeName).getLowestStrike(model);
	}

	@Override
	public double getIborOisDecorrelation() {
		return iborOisDecorrelation;
	}

}
