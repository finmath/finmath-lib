/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.07.2012
 */


package net.finmath.timeseries.models.parametric;

import java.util.HashMap;
import java.util.Map;

import net.finmath.timeseries.HistoricalSimulationModel;

/**
 * Implementation of standard historical simulation.
 *
 * @author Christian Fries, Norman Seeger
 * @version 1.0
 */

public class SimpleHistroricalSimulation implements HistoricalSimulationModel {

	private final double[] values;

	private final int windowIndexStart;
	private final int windowIndexEnd;

	public SimpleHistroricalSimulation(final double[] values) {
		this.values = values;
		windowIndexStart	= 0;
		windowIndexEnd		= values.length-1;

	}

	public SimpleHistroricalSimulation(final double[] values, final int windowIndexStart, final int windowIndexEnd) {
		this.values = values;
		this.windowIndexStart	= windowIndexStart;
		this.windowIndexEnd		= windowIndexEnd;

	}


	/* (non-Javadoc)
	 * @see net.finmath.timeseries.HistoricalSimulationModel#getCloneWithWindow(int, int)
	 */
	@Override
	public HistoricalSimulationModel getCloneWithWindow(final int windowIndexStart, final int windowIndexEnd) {
		return new SimpleHistroricalSimulation(values, windowIndexStart, windowIndexEnd);
	}

	public double[] getSzenarios(final int relAbsFlag) {
		final double[] szenarios = new double[windowIndexEnd-windowIndexStart+1-1];

		double y;

		for (int i = windowIndexStart+1; i <= windowIndexEnd; i++) {

			if (relAbsFlag==1) {
				y = Math.log(values[i]/values[i-1]);
				//double y = (values[i]-values[i-1]))/values[i-1]);
			} else {
				y = values[i] - values[i-1];
			}

			szenarios[i-windowIndexStart-1]	= y;
		}
		java.util.Arrays.sort(szenarios);

		return szenarios;
	}
	public double[] getQuantilPredictions(final int relAbsFlag,  final double[] quantiles) {
		final double[] szenarios = this.getSzenarios(relAbsFlag);

		final double[] quantileValues = new double[quantiles.length];
		for(int i=0; i<quantiles.length; i++) {
			final double quantile = quantiles[i];
			final double quantileIndex = szenarios.length * quantile  - 1;
			final int quantileIndexLo = (int)quantileIndex;
			final int quantileIndexHi = quantileIndexLo+1;

			final double evalLo = szenarios[Math.max(quantileIndexLo,0               )];
			final double evalHi = szenarios[Math.max(quantileIndexHi,0               )];

			if (relAbsFlag==1) {
				final double szenarioChange =
						(quantileIndexHi-quantileIndex) * Math.exp(evalLo)  // ?????????????????????
						+ (quantileIndex-quantileIndexLo) * Math.exp(evalHi);

				final double quantileValue = values[windowIndexEnd] * szenarioChange;
				quantileValues[i] = quantileValue;

			} else {
				final double szenarioChange =
						(quantileIndexHi-quantileIndex) * evalLo  // ?????????????????????
						+ (quantileIndex-quantileIndexLo) * evalHi;

				final double quantileValue = values[windowIndexEnd] + szenarioChange;
				quantileValues[i] = quantileValue;

			}

		}

		return quantileValues;
	}


	/* (non-Javadoc)
	 * @see net.finmath.timeseries.HistoricalSimulationModel#getBestParameters()
	 */
	@Override
	public Map<String, Object> getBestParameters() {
		return getBestParameters(null);
	}


	@Override
	public Map<String, Object> getBestParameters(final Map<String, Object> guess) {
		final int relAbsFlag = 1;
		final double[] quantiles = {0.01, 0.05, 0.5};
		final double[] quantileValues = this.getQuantilPredictions(relAbsFlag, quantiles);

		final Map<String, Object> results = new HashMap<>();
		results.put("Szenarios", this.getSzenarios(relAbsFlag));
		results.put("Quantile=1%", quantileValues[0]);
		results.put("Quantile=5%", quantileValues[1]);
		results.put("Quantile=50%", quantileValues[2]);
		return results;
	}

}
