/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.04.2015
 */

package net.finmath.montecarlo.hybridassetinterestrate;

import java.time.LocalDateTime;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * An Equity Hybrid LIBOR Market Model composed of an object implementing
 * <code>LIBORModelMonteCarloSimulationModel</code> providing the interest
 * rate simulation and the numeraire and an object implementing
 * <code>AssetModelMonteCarloSimulationModel</code> providing the
 * asset simulation.
 *
 * <b>The interest rate model needs to be in spot measure.</b>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class HybridAssetLIBORModelMonteCarloSimulationFromModels implements HybridAssetLIBORModelMonteCarloSimulation {

	private final LIBORModelMonteCarloSimulationModel	liborSimulation;
	private final AssetModelMonteCarloSimulationModel	assetSimulation;
	private final DiscountCurve					discountCurve;

	/**
	 * Create an Equity Hybrid LIBOR Market Model composed of an object implementing
	 * <code>LIBORModelMonteCarloSimulationModel</code> providing the interest
	 * rate simulation and the numeraire and an object implementing
	 * <code>AssetModelMonteCarloSimulationModel</code> providing the
	 * asset simulation.
	 *
	 * The interest rate model needs to be in spot measure.
	 *
	 * @param liborSimulation An object implementing <code>LIBORModelMonteCarloSimulationModel</code> providing the interest rate simulation and the numeraire.
	 * @param assetSimulation An object implementing <code>AssetModelMonteCarloSimulationModel</code> providing the asset simulation.
	 * @param discountCurve An optional object implementing <code>DiscountCurveInterface</code> to adjust the numeraire for a deterministic discounting spread.
	 */
	public HybridAssetLIBORModelMonteCarloSimulationFromModels(
			final LIBORModelMonteCarloSimulationModel liborSimulation,
			final AssetModelMonteCarloSimulationModel assetSimulation,
			final DiscountCurve discountCurve) {
		super();
		this.liborSimulation = liborSimulation;
		this.assetSimulation = assetSimulation;
		this.discountCurve = discountCurve;

		if(!liborSimulation.getTimeDiscretization().equals(assetSimulation.getTimeDiscretization())) {
			throw new IllegalArgumentException("The interest rate simulation and the asset simulation need to share the same simulation time discretization.");
		}
	}

	public HybridAssetLIBORModelMonteCarloSimulationFromModels(
			final LIBORModelMonteCarloSimulationModel liborSimulation,
			final AssetModelMonteCarloSimulationModel assetSimulation) {
		this(liborSimulation, assetSimulation, null);
	}

	@Override
	public int getNumberOfPaths() {
		return liborSimulation.getNumberOfPaths();
	}

	@Override
	public LocalDateTime getReferenceDate() {
		return liborSimulation.getReferenceDate();
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return liborSimulation.getTimeDiscretization();
	}

	@Override
	public int getNumberOfFactors() {
		return liborSimulation.getNumberOfFactors();
	}

	@Override
	public double getTime(final int timeIndex) {
		return liborSimulation.getTime(timeIndex);
	}

	@Override
	public TimeDiscretization getLiborPeriodDiscretization() {
		return liborSimulation.getLiborPeriodDiscretization();
	}

	@Override
	public int getTimeIndex(final double time) {
		return liborSimulation.getTimeIndex(time);
	}

	@Override
	public int getNumberOfLibors() {
		return liborSimulation.getNumberOfLibors();
	}

	@Override
	public RandomVariable getRandomVariableForConstant(final double value) {
		return liborSimulation.getRandomVariableForConstant(value);
	}

	@Override
	public double getLiborPeriod(final int timeIndex) {
		return liborSimulation.getLiborPeriod(timeIndex);
	}

	@Override
	public int getLiborPeriodIndex(final double time) {
		return liborSimulation.getLiborPeriodIndex(time);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final int timeIndex) throws CalculationException {
		return liborSimulation.getMonteCarloWeights(timeIndex);
	}

	@Override
	public RandomVariable getLIBOR(final int timeIndex, final int liborIndex) throws CalculationException {
		return liborSimulation.getLIBOR(timeIndex, liborIndex);
	}

	@Override
	public RandomVariable getMonteCarloWeights(final double time) throws CalculationException {
		return liborSimulation.getMonteCarloWeights(time);
	}

	@Override
	public RandomVariable getForwardRate(final double time, final double periodStart, final double periodEnd) throws CalculationException {
		return liborSimulation.getForwardRate(time, periodStart, periodEnd);
	}

	@Override
	public HybridAssetLIBORModelMonteCarloSimulationFromModels getCloneWithModifiedData( final Map<String, Object> dataModified) {
		return null;
	}

	@Override
	public RandomVariable[] getLIBORs(final int timeIndex) throws CalculationException {
		return liborSimulation.getLIBORs(timeIndex);
	}


	@Override
	public RandomVariable getNumeraire(final double time) throws CalculationException {

		RandomVariable numeraire = liborSimulation.getNumeraire(time);

		if(discountCurve != null) {
			// This includes a control for zero bonds
			final double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}

		return numeraire;
	}

	@Override
	public RandomVariable getNumeraire(final int timeIndex) throws CalculationException {
		return getNumeraire(getTime(timeIndex));
	}


	@Override
	public BrownianMotion getBrownianMotion() {
		return liborSimulation.getBrownianMotion();
	}

	@Override
	public TermStructureModel getModel() {
		return liborSimulation.getModel();
	}

	@Override
	public MonteCarloProcess getProcess() {
		return liborSimulation.getProcess();
	}

	/**
	 * @deprecated
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel#getCloneWithModifiedSeed(int)
	 */
	@Override
	@Deprecated
	public HybridAssetLIBORModelMonteCarloSimulationFromModels getCloneWithModifiedSeed(final int seed) {
		return null;
	}

	@Override
	public int getNumberOfAssets() {
		return assetSimulation.getNumberOfAssets();
	}

	@Override
	public RandomVariable getAssetValue(final int timeIndex, final int assetIndex) throws CalculationException {
		final RandomVariable asset = assetSimulation.getAssetValue(timeIndex, assetIndex);
		final RandomVariable changeOfMeasure = liborSimulation.getNumeraire(timeIndex).div(assetSimulation.getNumeraire(timeIndex));
		return asset.mult(changeOfMeasure);
	}

	@Override
	public RandomVariable getAssetValue(final double time, final int assetIndex) throws CalculationException {
		int timeIndex = getTimeIndex(time);

		// We round to the previous stock vaue (may generate loss of volatility and inconsistent forwards).
		if(timeIndex < 0) {
			timeIndex = -timeIndex-2;
		}

		return getAssetValue(timeIndex, assetIndex);
	}

	@Override
	public Map<String, RandomVariable> getModelParameters() {
		// TODO Add implementation
		throw new UnsupportedOperationException();
	}
}
