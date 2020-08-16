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
 * A volatility cube that uses a grid of SABR models for the calculation of the volatility with different strikes. The SABR parameters \( \beta \) and displacement are globally set for the entire cube.
 * For the other SABR parameters the cube stores a grid of maturity x termination and a set of parameters for each node of this grid. Between the nodes of this grid, the SABR parameters are interpolated.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class SABRVolatilityCube implements VolatilityCube, Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -3359125061963953634L;
	private final String name;
	private final LocalDate referenceDate;

	private final DataTable underlyingTable;


	//SABR parameters
	private final double sabrDisplacement;
	private final double sabrBeta;

	private final DataTable rhoTable;
	private final DataTable baseVolTable;
	private final DataTable volvolTable;

	// for use with the multi curve piterbarg annuity mapping
	private final double iborOisDecorrelation;

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
	 * @param rhoTable Table containing rhos.
	 * @param baseVolTable Table containing initial volatilities.
	 * @param volvolTable Table containing volvols.
	 * @param correlationDecay The correlation decay parameters of this cube.
	 */
	public SABRVolatilityCube(final String name, final LocalDate referenceDate, final DataTable swapRateTable,
			final double sabrDisplacement, final double sabrBeta, final DataTable rhoTable,
			final DataTable baseVolTable, final DataTable volvolTable, final double correlationDecay) {
		this(name, referenceDate, swapRateTable, sabrDisplacement, sabrBeta, rhoTable, baseVolTable, volvolTable, correlationDecay, 1.0);
	}

	/**
	 * Create the cube.
	 *
	 * @param name The name of the cube.
	 * @param referenceDate The reference date of the cube.
	 * @param swapRateTable Table containing base swap rates.
	 * @param sabrDisplacement Displacement for the entire cube.
	 * @param sabrBeta Beta for the entire cube.
	 * @param rhoTable Table containing rhos.
	 * @param baseVolTable Table containing initial volatilities.
	 * @param volvolTable Table containing volvols.
	 * @param correlationDecay The correlation decay parameters of this cube.
	 * @param iborOisDecorrelation The ibor ois decorrelation parameter of this cube.
	 */
	public SABRVolatilityCube(final String name, final LocalDate referenceDate, final DataTable swapRateTable,
			final double sabrDisplacement, final double sabrBeta, final DataTable rhoTable,
			final DataTable baseVolTable, final DataTable volvolTable, final double correlationDecay, final double iborOisDecorrelation) {
		super();
		this.name = name;
		this.referenceDate = referenceDate;
		this.underlyingTable = swapRateTable;
		this.sabrDisplacement = sabrDisplacement;
		this.sabrBeta = sabrBeta;
		this.rhoTable = rhoTable;
		this.baseVolTable = baseVolTable;
		this.volvolTable = volvolTable;
		this.correlationDecay = correlationDecay;
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
		final double sabrRho = rhoTable.getValue(maturity, termination);
		final double baseVol = baseVolTable.getValue(maturity, termination);
		final double sabrVolvol = volvolTable.getValue(maturity, termination);

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
		map.put("sabrDisplacement", sabrDisplacement);
		map.put("underlyingTable", underlyingTable.clone());
		map.put("baseVolTable", baseVolTable.clone());
		map.put("volvolTable", volvolTable.clone());
		map.put("rhoTable", rhoTable.clone());
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

	/**
	 * @return The table of swap rates.
	 */
	public DataTable getUnderlyingTable() {
		return underlyingTable.clone();
	}

	/**
	 * @return The table of rhos.
	 */
	public DataTable getRhoTable() {
		return rhoTable.clone();
	}

	/**
	 * @return The table containing initial volatilities.
	 */
	public DataTable getBaseVolTable() {
		return baseVolTable.clone();
	}

	/**
	 * @return The table containing volvols.
	 */
	public DataTable getVolvolTable() {
		return volvolTable.clone();
	}
}
