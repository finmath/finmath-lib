/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.modelfactory;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;

/**
 * @author Christian Fries
 */
public class BlackScholesModelFourierFactory implements ModelFactory<BlackScholesModelDescriptor> {

	/**
	 * Create factory.
	 */
	public BlackScholesModelFourierFactory() {
	}

	@Override
	public DescribedModel<BlackScholesModelDescriptor> getModelFromDescriptor(BlackScholesModelDescriptor descriptor) {
		return new net.finmath.fouriermethod.models.BlackScholesModel(descriptor);
	}
}
