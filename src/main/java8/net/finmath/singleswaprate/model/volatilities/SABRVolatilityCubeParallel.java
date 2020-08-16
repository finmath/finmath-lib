package net.finmath.singleswaprate.model.volatilities;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import net.finmath.functions.AnalyticFormulas;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;
import net.finmath.singleswaprate.data.DataTable;
import net.finmath.singleswaprate.model.VolatilityCubeModel;

/**
 * A volatility cube that uses a grid of SABR models for the calculation of the volatility with different strikes. All SABR parameters are shared throughout the cube,
 * with the exception of the initial volatility, which is being used to fit the curve on each point of the maturity x termination grid to the ATM levels.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SABRVolatilityCubeParallel implements VolatilityCube, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 4210804671265036606L;
	private final String name;
	private final LocalDate referenceDate;

	private final DataTable underlyingTable;

	private final double iborOisDecorrelation;


	//SABR parameters
	private final double sabrDisplacement;
	private final double sabrBeta;

	private final double sabrRho;
	private final double sabrVolvol;
	private final DataTable baseVolTable;


	//for later use in VolVolCube
	private final double correlationDecay;

	private final QuotingConvention quotingConvention = QuotingConvention.VOLATILITYNORMAL;

	/**
	 * Create the cube. With ibor ois decorrelation set to 1.0.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param swapRateTable Table containing base swap rates.
	 * @param sabrDisplacement Displacement for the entire cube.
	 * @param sabrBeta Beta for the entire cube.
	 * @param sabrRho Rho for the entire cube.
	 * @param sabrVolvol VolVol for the entire cube.
	 * @param baseVolTable Table containing initial volatilities.
	 * @param correlationDecay The correlation decay parameters of this cube.
	 */
	public SABRVolatilityCubeParallel(final String name, final LocalDate referenceDate, final DataTable swapRateTable, final double sabrDisplacement,
			final double sabrBeta, final double sabrRho, final double sabrVolvol, final DataTable baseVolTable, final double correlationDecay) {
		this(name, referenceDate, swapRateTable, sabrDisplacement, sabrBeta, sabrRho, sabrVolvol, baseVolTable, correlationDecay, 1.0);
	}

	/**
	 * Create the cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param swapRateTable Table containing base swap rates.
	 * @param sabrDisplacement Displacement for the entire cube.
	 * @param sabrBeta Beta for the entire cube.
	 * @param sabrRho Rho for the entire cube.
	 * @param sabrVolvol VolVol for the entire cube.
	 * @param baseVolTable Table containing initial volatilities.
	 * @param correlationDecay The correlation decay parameters of this cube.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter of this cube.
	 */
	public SABRVolatilityCubeParallel(final String name, final LocalDate referenceDate, final DataTable swapRateTable, final double sabrDisplacement,
			final double sabrBeta, final double sabrRho, final double sabrVolvol, final DataTable baseVolTable, final double correlationDecay, final double iborOisDecorrelation) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.underlyingTable = swapRateTable;
		this.sabrBeta = sabrBeta;
		this.sabrRho = sabrRho;
		this.sabrVolvol = sabrVolvol;
		this.sabrDisplacement = sabrDisplacement;
		this.correlationDecay = correlationDecay;
		this.baseVolTable = baseVolTable;
		this.iborOisDecorrelation = iborOisDecorrelation;
	}

	@Override
	public double getValue(final VolatilityCubeModel model, final double termination, final double maturity, final double strike, final QuotingConvention quotingConvention) {

		if(termination<maturity) {
			throw new IllegalArgumentException("Termination has to be larger (or equal) maturity. Was termination="+termination+", maturity="+maturity);
		}

		if(quotingConvention != this.quotingConvention) {
			throw new IllegalArgumentException("This cube supports only the Quoting Convention " +this.quotingConvention);
		}

		final double underlying = underlyingTable.getValue(maturity, termination);
		final double baseVol = baseVolTable.getValue(maturity, termination);

		//		if(Double.isNaN(AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(baseVol, sabrBeta, sabrRho, sabrVolvol, sabrDisplacement, underlying, strike, maturity)))
		//			System.out.println(underlying +"\t"+ baseVol +"\t"+ sabrBeta +"\t"+sabrRho+"\t"+sabrVolvol+"\t"+sabrDisplacement+"\t"+strike+"\t"+maturity);

		return AnalyticFormulas.sabrBerestyckiNormalVolatilityApproximation(baseVol, sabrBeta, sabrRho, sabrVolvol,
				sabrDisplacement, underlying, strike, maturity);
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
	public double getCorrelationDecay() {
		return correlationDecay;
	}

	@Override
	public Map<String, Object> getParameters() {
		final Map<String,Object> map = new HashMap<>();
		map.put("sabrBeta", sabrBeta);
		map.put("sabrRho", sabrRho);
		map.put("sabrVolvol", sabrVolvol);
		map.put("sabrDisplacement", sabrDisplacement);
		map.put("underlyingTable", underlyingTable.clone());
		map.put("baseVolTable", baseVolTable.clone());
		map.put("Inherent correlationDecay", correlationDecay);
		map.put("iborOisDecorrelation", iborOisDecorrelation);

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
