/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 02.02.2017
 */

package net.finmath.montecarlo.interestrate.models.covariance;

import net.finmath.time.TimeDiscretization;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class TermStructureTenorTimeScalingPicewiseConstant implements TermStructureTenorTimeScaling {

	private final TimeDiscretization timeDiscretization;
	private final double[] timesIntegrated;

	private final double floor = 0.1-1.0, cap = 10.0-1.0;
	private final double parameterScaling = 100.0;


	public TermStructureTenorTimeScalingPicewiseConstant(final TimeDiscretization timeDiscretization, final double[] parameters) {
		super();
		this.timeDiscretization = timeDiscretization;
		timesIntegrated = new double[timeDiscretization.getNumberOfTimes()];
		for(int timeIntervalIndex=0; timeIntervalIndex<timeDiscretization.getNumberOfTimeSteps(); timeIntervalIndex++) {
			timesIntegrated[timeIntervalIndex+1] = timesIntegrated[timeIntervalIndex] + (1.0+Math.min(Math.max(parameterScaling*parameters[timeIntervalIndex],floor),cap)) * (timeDiscretization.getTimeStep(timeIntervalIndex));
		}
	}

	@Override
	public double getScaledTenorTime(final double periodStart, final double periodEnd) {

		final double timeIntegratedStart;
		{
			final int timeStartIndex = timeDiscretization.getTimeIndex(periodStart);
			if(timeStartIndex >= 0) {
				timeIntegratedStart = timesIntegrated[timeStartIndex];
			}
			else {
				final int timeStartIndexLo = -timeStartIndex-2;
				timeIntegratedStart = (timesIntegrated[timeStartIndexLo+1]-timesIntegrated[timeStartIndexLo])/timeDiscretization.getTimeStep(timeStartIndexLo)*(timeDiscretization.getTime(timeStartIndexLo+1)-periodStart);
			}
		}

		final double timeIntegratedEnd;
		{
			final int timeEndIndex = timeDiscretization.getTimeIndex(periodEnd);
			if(timeEndIndex >= 0) {
				timeIntegratedEnd = timesIntegrated[timeEndIndex];
			}
			else {
				final int timeEndIndexLo = -timeEndIndex-2;
				timeIntegratedEnd = (timesIntegrated[timeEndIndexLo+1]-timesIntegrated[timeEndIndexLo])/timeDiscretization.getTimeStep(timeEndIndexLo)*(periodEnd-timeDiscretization.getTime(timeEndIndexLo));
			}
		}

		final double timeScaled = timeIntegratedEnd - timeIntegratedStart;

		return timeScaled;
	}

	@Override
	public TermStructureTenorTimeScaling getCloneWithModifiedParameters(final double[] parameters) {
		return new TermStructureTenorTimeScalingPicewiseConstant(timeDiscretization, parameters);
	}

	@Override
	public double[] getParameter() {
		final double[] parameter = new double[timeDiscretization.getNumberOfTimeSteps()];
		for(int timeIntervalIndex=0; timeIntervalIndex<timeDiscretization.getNumberOfTimeSteps(); timeIntervalIndex++) {
			parameter[timeIntervalIndex] = ((timesIntegrated[timeIntervalIndex+1] - timesIntegrated[timeIntervalIndex]) / timeDiscretization.getTimeStep(timeIntervalIndex) - 1.0) / parameterScaling;
		}

		return parameter;
	}

	@Override
	public TermStructureTenorTimeScaling clone() {
		return this;
	}
}
