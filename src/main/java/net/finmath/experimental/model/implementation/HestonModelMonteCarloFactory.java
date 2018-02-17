/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 09.02.2018
 */

package net.finmath.experimental.model.implementation;

import net.finmath.experimental.model.Model;
import net.finmath.experimental.model.ModelFactory;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme;

/**
 * @author Christian Fries
 */
public class HestonModelMonteCarloFactory implements ModelFactory<HestonModelDescriptor> {

	private final net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme scheme;
	private final AbstractRandomVariableFactory randomVariableFactory;


	public HestonModelMonteCarloFactory(Scheme scheme, AbstractRandomVariableFactory randomVariableFactory) {
		super();
		this.scheme = scheme;
		this.randomVariableFactory = randomVariableFactory;
	}


	@Override
	public Model<HestonModelDescriptor> getModelFromDescription(HestonModelDescriptor descriptor) {
		return new net.finmath.montecarlo.assetderivativevaluation.HestonModel(descriptor, scheme, randomVariableFactory);
	}

}
