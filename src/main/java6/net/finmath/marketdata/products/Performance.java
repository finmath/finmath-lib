/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 06.12.2015
 */
package net.finmath.marketdata.products;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.time.RegularSchedule;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements an analytic product given by the ratio
 * of two analytic products.
 * 
 * This class may become handy when constructing calibrations products.
 * For example: The swap rate is the performance of a <code>SwapLeg</code> relative to a <code>SwapAnnuity</code>.
 * 
 * Note: In the strict sense, the performance is an index an not a product (e.g. it does not have a currency unit).
 * With respect to the implementation we do not make a difference here and implement the AbstractAnalyticProduct interface.
 * 
 * @author Christian Fries
 */
public class Performance extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private final AbstractAnalyticProduct productNumerator;
	private final AbstractAnalyticProduct productDenominator;

	/**
	 * Creates a Performance product.
	 * 
	 * @param productNumerator A product implementing AbstractAnalyticProduct for the numerator.
	 * @param productDenominator A product implementing AbstractAnalyticProduct for the denominator.
	 */
	public Performance(AbstractAnalyticProduct productNumerator, AbstractAnalyticProduct productDenominator) {
		super();
		this.productNumerator = productNumerator;
		this.productDenominator = productDenominator;
	}

	@Override
	public double getValue(double evaluationTime, AnalyticModelInterface model) {	

		double valueNumerator	= productNumerator.getValue(evaluationTime, model);
		double valueDenominator	= productDenominator.getValue(evaluationTime, model);

		double value = valueNumerator / valueDenominator;
		return value;
	}

	@Override
	public String toString() {
		return "Performance [productNumerator=" + productNumerator
				+ ", productDenominator=" + productDenominator + "]";
	}
}
