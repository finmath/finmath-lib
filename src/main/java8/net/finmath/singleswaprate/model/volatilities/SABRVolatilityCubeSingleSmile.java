package net.finmath.singleswaprate.model.volatilities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.singleswaprate.model.VolatilityCubeModel;

/**
 * A simplified volatility cube that provides a volatility smile in strike for all possible maturities and terminations, based on a single set of SABR parameters.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SABRVolatilityCubeSingleSmile implements VolatilityCube, Serializable {

	private static final long serialVersionUID = -2465149876882995369L;

	private final String name;
	private final LocalDate referenceDate;

	private final double correlationDecay;
	private final double iborOisDecorrelation;

	//SABR parameters
	private final double underlying;
	private final double sabrAlpha;
	private final double sabrBeta;
	private final double sabrRho;
	private final double sabrNu;
	private final double sabrDisplacement;

	private final QuotingConvention quotingConvention = QuotingConvention.VOLATILITYNORMAL;

	/**
	 * Create the cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param underlying The dummy underlying to be used for the SABR model.
	 * @param sabrAlpha The initial value of the stochastic volatility process of the SABR model.
	 * @param sabrBeta The beta CEV parameter of the SABR model.
	 * @param sabrRho Correlation (leverages) of the stochastic volatility.
	 * @param sabrNu Volatility of the stochastic volatility (vol-of-vol).
	 * @param sabrDisplacement The displacement parameter of the smile.
	 */
	public SABRVolatilityCubeSingleSmile(final String name, final LocalDate referenceDate, final double underlying, final double sabrAlpha,
			final double sabrBeta, final double sabrRho, final double sabrNu, final double sabrDisplacement) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.underlying = underlying;
		this.sabrAlpha = sabrAlpha;
		this.sabrBeta = sabrBeta;
		this.sabrRho = sabrRho;
		this.sabrNu = sabrNu;
		this.sabrDisplacement = sabrDisplacement;
		this.correlationDecay = 1.0;
		this.iborOisDecorrelation = 1.0;
	}

	/**
	 * Create the cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param underlying The dummy underlying to be used for the SABR model.
	 * @param sabrAlpha The initial value of the stochastic volatility process of the SABR model.
	 * @param sabrBeta The beta CEV parameter of the SABR model.
	 * @param sabrRho Correlation (leverages) of the stochastic volatility.
	 * @param sabrNu Volatility of the stochastic volatility (vol-of-vol).
	 * @param sabrDisplacement The displacement parameter of the smile.
	 * @param correlationDecay The correlation decay inherent to this cube.
	 */
	public SABRVolatilityCubeSingleSmile(final String name, final LocalDate referenceDate, final double underlying, final double sabrAlpha,
			final double sabrBeta, final double sabrRho, final double sabrNu, final double sabrDisplacement, final double correlationDecay) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.underlying = underlying;
		this.sabrAlpha = sabrAlpha;
		this.sabrBeta = sabrBeta;
		this.sabrRho = sabrRho;
		this.sabrNu = sabrNu;
		this.sabrDisplacement = sabrDisplacement;
		this.correlationDecay = correlationDecay;
		this.iborOisDecorrelation = 1.0;
	}

	/**
	 * Create the cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param underlying The dummy underlying to be used for the SABR model.
	 * @param sabrAlpha The initial value of the stochastic volatility process of the SABR model.
	 * @param sabrBeta The beta CEV parameter of the SABR model.
	 * @param sabrRho Correlation (leverages) of the stochastic volatility.
	 * @param sabrNu Volatility of the stochastic volatility (vol-of-vol).
	 * @param sabrDisplacement The displacement parameter of the smile.
	 * @param correlationDecay The correlation decay inherent to this cube.
	 * @param iborOisDecorrelation The ibor ois decorrealtion parameter of this cube.
	 */
	public SABRVolatilityCubeSingleSmile(final String name, final LocalDate referenceDate, final double underlying, final double sabrAlpha,
			final double sabrBeta, final double sabrRho, final double sabrNu, final double sabrDisplacement, final double correlationDecay, final double iborOisDecorrelation) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.underlying = underlying;
		this.sabrAlpha = sabrAlpha;
		this.sabrBeta = sabrBeta;
		this.sabrRho = sabrRho;
		this.sabrNu = sabrNu;
		this.sabrDisplacement = sabrDisplacement;
		this.correlationDecay = correlationDecay;
		this.iborOisDecorrelation = iborOisDecorrelation;
	}

	@Override
	public double getValue(final VolatilityCubeModel model, final double termination, final double maturity, final double strike,
			final QuotingConvention quotingConvention) {

		if(quotingConvention == this.quotingConvention) {
			return AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(sabrAlpha, sabrBeta, sabrRho, sabrNu, sabrDisplacement, underlying, strike, maturity);
		} else {
			throw new IllegalArgumentException("This cube supports only the Quoting Convention " +this.quotingConvention);
		}
		//TODO support other conventions
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

	@Override
	public String toString() {
		return super.toString() + "\n\"" + this.getName() + "\"" + getParameters().toString();
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return new SABRVolatilityCubeSingleSmile(name, referenceDate, underlying, sabrAlpha, sabrBeta, sabrRho, sabrNu, sabrDisplacement, correlationDecay, iborOisDecorrelation);
	}

	@Override
	public double getCorrelationDecay() {
		return correlationDecay;
	}

	@Override
	public Map<String, Object> getParameters() {
		final Map<String,Object> map = new HashMap<>();
		map.put("sabrAlpha", sabrAlpha);
		map.put("sabrBeta", sabrBeta);
		map.put("sabrRho", sabrRho);
		map.put("sabrNu", sabrNu);
		map.put("sabrDisplacement", sabrDisplacement);
		map.put("InherentCorrelationDecay", correlationDecay);
		map.put("iborOisDecorrelation", iborOisDecorrelation);
		map.put("DummyUnderlying", underlying);

		return map;
	}

	@Override
	public double getLowestStrike(final VolatilityCubeModel model) {
		return -sabrDisplacement;
	}

	@Override
	public double getIborOisDecorrelation() {
		return iborOisDecorrelation;
	}
}
