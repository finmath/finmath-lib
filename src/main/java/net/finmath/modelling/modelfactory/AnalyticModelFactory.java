package net.finmath.modelling.modelfactory;

import java.time.LocalDate;
import java.util.Map;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.curves.CurveInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.descriptor.AnalyticModelDescriptor;
import net.finmath.modelling.productfactory.InterestRateAnalyticProductFactory;

/**
 * Factory to build an described analytic model from a descriptor.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AnalyticModelFactory implements ModelFactory<AnalyticModelDescriptor> {

	@Override
	public DescribedModel<AnalyticModelDescriptor> getModelFromDescriptor(AnalyticModelDescriptor descriptor) {
		return new DescribedAnalyticModel(descriptor.getReferenceDate(), descriptor.getCurvesMap(), descriptor.getVolatilitySurfaceMap());
	}

	/**
	 * Class extending {@link net.finmath.marketdata.model.AnalyticModel} with the funtionality of a described model.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	public static class DescribedAnalyticModel extends AnalyticModel implements DescribedModel<AnalyticModelDescriptor> {

		private static final long serialVersionUID = -2884913997994052340L;

		public DescribedAnalyticModel(LocalDate referenceDate, Map<String, CurveInterface> curvesMap, Map<String, VolatilitySurfaceInterface> volatilitySurfaceMap) {
			super(referenceDate, curvesMap, volatilitySurfaceMap);
		}

		@Override
		public AnalyticModelDescriptor getDescriptor() {
			return new AnalyticModelDescriptor(getReferenceDate(), getCurves(), getVolatilitySurfaces());
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {

			return new InterestRateAnalyticProductFactory(getReferenceDate()).getProductFromDescriptor(productDescriptor);

		}

	}

}
