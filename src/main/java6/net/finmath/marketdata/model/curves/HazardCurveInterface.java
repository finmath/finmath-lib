package net.finmath.marketdata.model.curves;


import net.finmath.marketdata.model.AnalyticModelInterface;

/**
 * 
 * @author Alessandro Gnoatto
 *
 */
public interface HazardCurveInterface extends CurveInterface{
	

	/**
	 * 
	 * @param model
	 * @param maturity
	 * @return  The survival probability i.e. P(\tau> maturity) where \tau is the random default time
	 */
	double getSurvivalProbability(AnalyticModelInterface model, double maturity);

}
