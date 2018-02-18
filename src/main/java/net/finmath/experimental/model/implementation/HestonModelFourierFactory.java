/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.experimental.model.implementation;

import net.finmath.modelling.Model;
import net.finmath.modelling.ModelFactory;

/**
 * @author Christian Fries
 */
public class HestonModelFourierFactory implements ModelFactory<HestonModelDescriptor> {

	/**
	 * Create factory.
	 */
	public HestonModelFourierFactory() {
	}

	@Override
	public Model<HestonModelDescriptor> getModelFromDescription(HestonModelDescriptor description) {
		return new net.finmath.fouriermethod.models.HestonModel(description);
	}
}
