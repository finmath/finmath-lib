/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 07.05.2013
 */

package net.finmath.montecarlo.hybridassetinterestrate;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

/**
 * A general convexity adjustment for models.
 *
 * @author Christian Fries
 */
public class ConvexityAdjustedModel {

	private final ProcessModel baseModel;
	private final MonteCarloProcess		measureTransformModel;
	private final Map<Integer,Integer> factorLoadingMap;


	public ConvexityAdjustedModel(ProcessModel baseModel, MonteCarloProcess measureTransformModel, Map<Integer,Integer> factorLoadingMap) {
		super();
		this.baseModel = baseModel;
		this.measureTransformModel = measureTransformModel;
		this.factorLoadingMap = factorLoadingMap;
	}

	public RandomVariable[] getDrift(RandomVariable[] driftUnadjusted, MonteCarloProcess process, int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
		/*
		 * Add convexity adjustment
		 */
		final RandomVariable[] driftAdjusted = new RandomVariable[baseModel.getNumberOfComponents()];

		RandomVariable[] processValueTransformModel = null;
		try {
			processValueTransformModel = measureTransformModel.getProcessValue(timeIndex);

			for(int componentIndex=0; componentIndex<baseModel.getNumberOfComponents(); componentIndex++) {
				if(driftUnadjusted[componentIndex] != null) {
					driftAdjusted[componentIndex] = driftUnadjusted[componentIndex];

					final RandomVariable[] factorLoadingsBaseModel = baseModel.getFactorLoading(process, timeIndex, componentIndex, realizationAtTimeIndex);
					final RandomVariable[] factorLoadingsTransfrom = measureTransformModel.getModel().getFactorLoading(measureTransformModel, timeIndex, 0, processValueTransformModel);

					for(int factorIndex=0; factorIndex<factorLoadingsBaseModel.length; factorIndex++) {
						if(factorLoadingMap.containsKey(factorIndex)) {
							driftAdjusted[componentIndex] = driftAdjusted[componentIndex].addProduct(
									factorLoadingsBaseModel[factorIndex].mult(-1),
									factorLoadingsTransfrom[factorLoadingMap.get(factorIndex)]);
						}
					}
					driftAdjusted[componentIndex] = driftAdjusted[componentIndex];
				}
			}
		} catch (final CalculationException e) {}

		return driftAdjusted;
	}
}
