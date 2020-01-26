/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.util.HashMap;
import java.util.Map;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;

/**
 * @author Christian Fries
 *
 * @version 1.1
 */
public class RandomVariableDifferentiableAADFactory extends AbstractRandomVariableDifferentiableFactory {

	private static final long serialVersionUID = -6035830497454502442L;

	public enum DiracDeltaApproximationMethod {
		DISCRETE_DELTA,
		REGRESSION_ON_DENSITY,
		REGRESSION_ON_DISTRIBUITON,
		ONE,
		ZERO
	}

	/*
	 * diractDeltaApproximationWidthPerStdDev defines the width of the discrete approximation of the Dirac peak
	 * from differentiation of a jump (barrier). It corresponds to the finite difference shift
	 * when the derivative is calculated via finite differences.
	 *
	 * It is a multiplicator to the standard deviation of the random variable.
	 */
	private final DiracDeltaApproximationMethod diracDeltaApproximationMethod;
	private final double diracDeltaApproximationWidthPerStdDev;
	private final double diracDeltaApproximationDensityRegressionWidthPerStdDev;

	private final boolean isGradientRetainsLeafNodesOnly;

	/**
	 * Create a factory for objects of type {@link RandomVariableDifferentiableAAD}.
	 *
	 * Supported propeties are
	 * <ul>
	 * <li>isGradientRetainsLeafNodesOnly: Boolean</li>
	 * <li>diracDeltaApproximationMethod: String</li>
	 * <li>diracDeltaApproximationWidthPerStdDev: Double</li>
	 * </ul>
	 *
	 * @param randomVariableFactoryForNonDifferentiable Random variable factory for the underlying values.
	 * @param properties A key value map with properties.
	 */
	public RandomVariableDifferentiableAADFactory(final RandomVariableFactory randomVariableFactoryForNonDifferentiable, final Map<String, Object> properties) {
		super(randomVariableFactoryForNonDifferentiable);

		/*
		 * diractDeltaApproximationWidthPerStdDev = 0.05 corresponds to 2% of paths used for estimation bin 0.05 = 2%,  0.025 = 1%
		 * diracDeltaApproximationDensityRegressionWidthPerStdDev = 0.50 corresponds to 20% of path used for regression of density, 1.00 = 40%.
		 */
		diracDeltaApproximationMethod = DiracDeltaApproximationMethod.valueOf((String)properties.getOrDefault("diracDeltaApproximationMethod", DiracDeltaApproximationMethod.DISCRETE_DELTA.name()));
		diracDeltaApproximationWidthPerStdDev = (Double)properties.getOrDefault("diracDeltaApproximationWidthPerStdDev", properties.getOrDefault("barrierDiracWidth", 0.05));
		diracDeltaApproximationDensityRegressionWidthPerStdDev = (Double)properties.getOrDefault("diracDeltaApproximationDensityRegressionWidthPerStdDev", 0.5);

		isGradientRetainsLeafNodesOnly = (Boolean) properties.getOrDefault("isGradientRetainsLeafNodesOnly", true);
	}

	/**
	 * @param properties A key value map with properties.
	 */
	public RandomVariableDifferentiableAADFactory(final Map<String, Object> properties) {
		this(new RandomVariableFromArrayFactory(), properties);
	}

	/**
	 * @param randomVariableFactoryForNonDifferentiable Random variable factory for the underlying values.
	 */
	public RandomVariableDifferentiableAADFactory(final RandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		this(randomVariableFactoryForNonDifferentiable, new HashMap<String, Object>());
	}

	public RandomVariableDifferentiableAADFactory() {
		this(new RandomVariableFromArrayFactory());
	}

	@Override
	public RandomVariableDifferentiable createRandomVariable(final double time, final double value) {
		return new RandomVariableDifferentiableAAD(createRandomVariableNonDifferentiable(time, value), this);
	}

	@Override
	public RandomVariableDifferentiable createRandomVariable(final double time, final double[] values) {
		return new RandomVariableDifferentiableAAD(createRandomVariableNonDifferentiable(time, values), this);
	}

	public DiracDeltaApproximationMethod getDiracDeltaApproximationMethod() {
		return diracDeltaApproximationMethod;
	}

	public double getDiracDeltaApproximationWidthPerStdDev() {
		return diracDeltaApproximationWidthPerStdDev;
	}

	public double getDiracDeltaApproximationDensityRegressionWidthPerStdDev() {
		return diracDeltaApproximationDensityRegressionWidthPerStdDev;
	}

	@Deprecated
	public double getBarrierDiracWidth() {
		return getDiracDeltaApproximationWidthPerStdDev();
	}

	public boolean isGradientRetainsLeafNodesOnly() {
		return isGradientRetainsLeafNodesOnly;
	}

	@Override
	public String toString() {
		return "RandomVariableDifferentiableAADFactory [diracDeltaApproximationMethod=" + diracDeltaApproximationMethod
				+ ", diracDeltaApproximationWidthPerStdDev=" + diracDeltaApproximationWidthPerStdDev
				+ ", diracDeltaApproximationDensityRegressionWidthPerStdDev="
				+ diracDeltaApproximationDensityRegressionWidthPerStdDev + ", isGradientRetainsLeafNodesOnly="
				+ isGradientRetainsLeafNodesOnly + ", toString()=" + super.toString() + "]";
	}
}
