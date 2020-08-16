package net.finmath.marketdata.model.volatility.caplet.tenorconversion;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModel;

/**
 * Interface for a correlation provider for forward curves.
 */
public interface CorrelationProvider {
	double getCorrelation(int newTenor, double firstForwardFixingTimeVectorInYears, double secondForwardFixingTimeVectorInYears, AnalyticModel analyticModel, String indexForDiscount) throws CalculationException;
}
