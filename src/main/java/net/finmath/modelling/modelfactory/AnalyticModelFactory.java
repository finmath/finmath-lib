package net.finmath.modelling.modelfactory;

import java.time.LocalDate;
import java.util.Map;

import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.volatilities.VolatilitySurface;
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
	public DescribedModel<AnalyticModelDescriptor> getModelFromDescriptor(final AnalyticModelDescriptor descriptor) {
		return new DescribedAnalyticModel(descriptor.getReferenceDate(), descriptor.getCurvesMap(), descriptor.getVolatilitySurfaceMap());
	}

	/**
	 * Class extending {@link net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols} with the functionality of a described model.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	public static class DescribedAnalyticModel extends AnalyticModelFromCurvesAndVols implements DescribedModel<AnalyticModelDescriptor> {

		private static final long serialVersionUID = -2884913997994052340L;

		public DescribedAnalyticModel(final LocalDate referenceDate, final Map<String, Curve> curvesMap, final Map<String, VolatilitySurface> volatilitySurfaceMap) {
			super(referenceDate, curvesMap, volatilitySurfaceMap);
		}

		@Override
		public AnalyticModelDescriptor getDescriptor() {
			return new AnalyticModelDescriptor(getReferenceDate(), getCurves(), getVolatilitySurfaces());
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(final ProductDescriptor productDescriptor) {

			return new InterestRateAnalyticProductFactory(getReferenceDate()).getProductFromDescriptor(productDescriptor);

		}

	}

}
