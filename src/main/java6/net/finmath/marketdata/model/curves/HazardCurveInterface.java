package net.finmath.marketdata.model.curves;


import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 *
 * @author Alessandro Gnoatto
 *
 * @version 1.0
 */
public interface HazardCurveInterface extends CurveInterface{


	/**
	 * Return the survival probability for a given maturity.
	 *
	 * @param model The analytic model, providing the context (e.g. in case where this curve is mapping to another curve "by name").
	 * @param maturity The maturity \( T \).
	 * @return  The survival probability, i.e. \( P(\tau &gt; T) \) where \( \tau \) is the random default time and \( T \) is the given maturity.
	 */
	double getSurvivalProbability(AnalyticModelInterface model, double maturity);

}
