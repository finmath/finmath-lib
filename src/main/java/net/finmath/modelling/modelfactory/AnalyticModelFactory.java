package net.finmath.modelling.modelfactory;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.descriptor.AnalyticModelDescriptor;

/**
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AnalyticModelFactory implements ModelFactory<AnalyticModelDescriptor> {

	@Override
	public DescribedModel<AnalyticModelDescriptor> getModelFromDescriptor(AnalyticModelDescriptor descriptor) {
		AnalyticModel model = new AnalyticModel(descriptor.getCurvesMap().values());
		for(VolatilitySurfaceInterface surface : descriptor.getVolatilitySurfaceMap().values()) {
			model.addVolatilitySurface(surface);
		}
		return model;
	}

}
