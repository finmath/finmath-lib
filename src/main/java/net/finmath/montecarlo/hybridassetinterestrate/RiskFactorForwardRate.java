package net.finmath.montecarlo.hybridassetinterestrate;

public class RiskFactorForwardRate implements RiskFactorID {

	private final String name;
	private final double periodStart;
	private final double periodEnd;

	public RiskFactorForwardRate(String name, double periodStart, double periodEnd) {
		super();
		this.name = name;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
	}

	@Override
	public String getName() {
		return name;
	}

	public double getPeriodStart() {
		return periodStart;
	}

	public double getPeriodEnd() {
		return periodEnd;
	}
}
