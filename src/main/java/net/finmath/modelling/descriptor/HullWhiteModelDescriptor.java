package net.finmath.modelling.descriptor;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.montecarlo.interestrate.modelplugins.ShortRateVolailityModelInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * A descriptor describing the Hull-White model.
 *
 * @TODO Note that discretization of forward rates should maybe NOT be part of the descriptor. It belongs to the numerical methods.
 *
 * @author Christian Fries
 * @author Roland Bachl
 * @version 1.0
 */
public class HullWhiteModelDescriptor implements InterestRateModelDescriptor {

	private final TimeDiscretizationInterface		liborPeriodDiscretization;
	private final String							forwardCurveName;
	// replace with descriptor?
	private final AnalyticModelInterface			curveModel;
	private final ForwardCurveInterface				forwardRateCurve;
	private final DiscountCurveInterface			discountCurve;
	private final DiscountCurveInterface			discountCurveFromForwardCurve;
	//replace with descriptor?
	private final ShortRateVolailityModelInterface 	volatilityModel;

	public HullWhiteModelDescriptor(TimeDiscretizationInterface liborPeriodDiscretization, String forwardCurveName,
			AnalyticModelInterface curveModel, ForwardCurveInterface forwardRateCurve,
			DiscountCurveInterface discountCurve, DiscountCurveInterface discountCurveFromForwardCurve,
			ShortRateVolailityModelInterface volatilityModel) {
		super();
		this.liborPeriodDiscretization = liborPeriodDiscretization;
		this.forwardCurveName = forwardCurveName;
		this.curveModel = curveModel;
		this.forwardRateCurve = forwardRateCurve;
		this.discountCurve = discountCurve;
		this.discountCurveFromForwardCurve = discountCurveFromForwardCurve;
		this.volatilityModel = volatilityModel;
	}

	@Override
	public Integer version() {
		return 1;
	}

	@Override
	public String name() {
		return "Hull White Libor Model Descriptor";
	}

}
