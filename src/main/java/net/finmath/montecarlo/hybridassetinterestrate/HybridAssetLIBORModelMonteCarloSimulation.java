/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.04.2015
 */

package net.finmath.montecarlo.hybridassetinterestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.TermStructureModelInterface;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * An Equity Hybrid LIBOR Market Model composed of an object implementing
 * <code>LIBORModelMonteCarloSimulationInterface</code> providing the interest
 * rate simulation and the numeraire and an object implementing
 * <code>AssetModelMonteCarloSimulationInterface</code> providing the
 * asset simulation.
 *
 * <b>The interest rate model needs to be in spot measure.</b>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class HybridAssetLIBORModelMonteCarloSimulation implements HybridAssetLIBORModelMonteCarloSimulationInterface {

	private LIBORModelMonteCarloSimulationInterface	liborSimulation;
	private AssetModelMonteCarloSimulationInterface	assetSimulation;
	private DiscountCurveInterface					discountCurve;

	/**
	 * Create an Equity Hybrid LIBOR Market Model composed of an object implementing
	 * <code>LIBORModelMonteCarloSimulationInterface</code> providing the interest
	 * rate simulation and the numeraire and an object implementing
	 * <code>AssetModelMonteCarloSimulationInterface</code> providing the
	 * asset simulation.
	 *
	 * The interest rate model needs to be in spot measure.
	 *
	 * @param liborSimulation An object implementing <code>LIBORModelMonteCarloSimulationInterface</code> providing the interest rate simulation and the numeraire.
	 * @param assetSimulation An object implementing <code>AssetModelMonteCarloSimulationInterface</code> providing the asset simulation.
	 * @param discountCurve An optional object implementing <code>DiscountCurveInterface</code> to adjust the numeraire for a deterministic discounting spread.
	 */
	public HybridAssetLIBORModelMonteCarloSimulation(
			LIBORModelMonteCarloSimulationInterface liborSimulation,
			AssetModelMonteCarloSimulationInterface assetSimulation,
			DiscountCurveInterface discountCurve) {
		super();
		this.liborSimulation = liborSimulation;
		this.assetSimulation = assetSimulation;
		this.discountCurve = discountCurve;

		if(!liborSimulation.getTimeDiscretization().equals(assetSimulation.getTimeDiscretization())) {
			throw new IllegalArgumentException("The interest rate simulation and the asset simulation need to share the same simulation time discretization.");
		}
	}

	public HybridAssetLIBORModelMonteCarloSimulation(
			LIBORModelMonteCarloSimulationInterface liborSimulation,
			AssetModelMonteCarloSimulationInterface assetSimulation) {
		this(liborSimulation, assetSimulation, null);
	}

	@Override
	public int getNumberOfPaths() {
		return liborSimulation.getNumberOfPaths();
	}

	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return liborSimulation.getTimeDiscretization();
	}

	@Override
	public int getNumberOfFactors() {
		return liborSimulation.getNumberOfFactors();
	}

	@Override
	public double getTime(int timeIndex) {
		return liborSimulation.getTime(timeIndex);
	}

	@Override
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return liborSimulation.getLiborPeriodDiscretization();
	}

	@Override
	public int getTimeIndex(double time) {
		return liborSimulation.getTimeIndex(time);
	}

	@Override
	public int getNumberOfLibors() {
		return liborSimulation.getNumberOfLibors();
	}

	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return liborSimulation.getRandomVariableForConstant(value);
	}

	@Override
	public double getLiborPeriod(int timeIndex) {
		return liborSimulation.getLiborPeriod(timeIndex);
	}

	@Override
	public int getLiborPeriodIndex(double time) {
		return liborSimulation.getLiborPeriodIndex(time);
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return liborSimulation.getMonteCarloWeights(timeIndex);
	}

	@Override
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return liborSimulation.getLIBOR(timeIndex, liborIndex);
	}

	@Override
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return liborSimulation.getMonteCarloWeights(time);
	}

	@Override
	public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException {
		return liborSimulation.getLIBOR(time, periodStart, periodEnd);
	}

	@Override
	public HybridAssetLIBORModelMonteCarloSimulation getCloneWithModifiedData( Map<String, Object> dataModified) {
		return null;
	}

	@Override
	public RandomVariableInterface[] getLIBORs(int timeIndex) throws CalculationException {
		return liborSimulation.getLIBORs(timeIndex);
	}


	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {

		RandomVariableInterface numeraire = liborSimulation.getNumeraire(time);

		if(discountCurve != null) {
			// This includes a control for zero bonds
			double deterministicNumeraireAdjustment = numeraire.invert().getAverage() / discountCurve.getDiscountFactor(time);
			numeraire = numeraire.mult(deterministicNumeraireAdjustment);
		}

		return numeraire;
	}

	@Override
	public RandomVariableInterface getNumeraire(int timeIndex) throws CalculationException {
		return getNumeraire(getTime(timeIndex));
	}


	@Override
	public BrownianMotionInterface getBrownianMotion() {
		return liborSimulation.getBrownianMotion();
	}

	@Override
	public TermStructureModelInterface getModel() {
		return liborSimulation.getModel();
	}

	@Override
	public AbstractProcessInterface getProcess() {
		return liborSimulation.getProcess();
	}

	/**
	 * @deprecated
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getCloneWithModifiedSeed(int)
	 */
	@Override
	@Deprecated
	public HybridAssetLIBORModelMonteCarloSimulation getCloneWithModifiedSeed(int seed) {
		return null;
	}

	@Override
	public int getNumberOfAssets() {
		return assetSimulation.getNumberOfAssets();
	}

	@Override
	public RandomVariableInterface getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return assetSimulation.getAssetValue(timeIndex, assetIndex).mult(liborSimulation.getNumeraire(getTime(timeIndex))).div(assetSimulation.getNumeraire(timeIndex));
	}

	@Override
	public RandomVariableInterface getAssetValue(double time, int assetIndex) throws CalculationException {
		int timeIndex = getTimeIndex(time);

		// We round to the previous stock vaue (may generate loss of volatility and inconsistent forwards).
		if(timeIndex < 0) {
			timeIndex = -timeIndex-2;
		}

		return getAssetValue(timeIndex, assetIndex);
	}
}
