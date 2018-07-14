/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.modelfactory;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.descriptor.HestonModelDescriptor;

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
	public DescribedModel<HestonModelDescriptor> getModelFromDescriptor(HestonModelDescriptor descriptor) {
		return new net.finmath.fouriermethod.models.HestonModel(descriptor);
	}
}

