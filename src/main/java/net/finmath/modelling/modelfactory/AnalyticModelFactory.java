package net.finmath.modelling.modelfactory;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelInterface;
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
		AnalyticModelInterface model = new AnalyticModel(descriptor.getReferenceDate(), descriptor.getCurvesMap().values());
		for(VolatilitySurfaceInterface surface : descriptor.getVolatilitySurfaceMap().values()) {
			model = model.addVolatilitySurfaces(surface);
		}
		return (DescribedModel<AnalyticModelDescriptor>)model;
	}

}
