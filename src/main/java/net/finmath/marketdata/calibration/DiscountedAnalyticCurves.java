package net.finmath.marketdata.calibration;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;

public class DiscountedAnalyticCurves {
    public DiscountCurve createDiscountCurve(final String discountCurveName, AnalyticModel model) {
		DiscountCurve discountCurve	= model.getDiscountCurve(discountCurveName);
		if(discountCurve == null) {
			discountCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(discountCurveName, new double[] { 0.0 }, new double[] { 1.0 });
			model = model.addCurves(discountCurve);
		}

		return discountCurve;
	}
    
}
