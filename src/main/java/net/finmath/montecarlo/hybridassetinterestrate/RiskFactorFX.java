package net.finmath.montecarlo.hybridassetinterestrate;

public class RiskFactorFX implements RiskFactorID {

	private final String name;

	public RiskFactorFX(String name) {
		super();
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}
}
