/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.descriptor;

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
	public Model<HestonModelDescriptor> getModelFromDescriptor(HestonModelDescriptor descriptor) {
		return new net.finmath.fouriermethod.models.HestonModel(descriptor);
	}
}
