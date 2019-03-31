package net.finmath.modelling.modelfactory;

import net.finmath.fouriermethod.models.BlackScholesModel;
import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetFourierProductFactory;

/**
 * Constructs asset models, which evaluate products via Monte-Carlo method.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AssetModelFourierMethodFactory implements ModelFactory<AssetModelDescriptor> {

	/**
	 * Create the factory.
	 */
	public AssetModelFourierMethodFactory() {
		super();
	}

	@Override
	public DescribedModel<? extends AssetModelDescriptor> getModelFromDescriptor(AssetModelDescriptor descriptor) {

		if(descriptor instanceof BlackScholesModelDescriptor) {
			DescribedModel<BlackScholesModelDescriptor> model = new BlackScholesModelFourier((BlackScholesModelDescriptor) descriptor);
			return model;
		}
		else if(descriptor instanceof HestonModelDescriptor) {
			DescribedModel<HestonModelDescriptor> model = new HestonModelFourier((HestonModelDescriptor) descriptor);
			return model;
		}
		else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}





	/**
	 * A described Black-Scholes model using Fourier method for evaluation.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private static class BlackScholesModelFourier extends BlackScholesModel implements DescribedModel<BlackScholesModelDescriptor> {

		private final BlackScholesModelDescriptor descriptor;
		private final SingleAssetFourierProductFactory productFactory;


		/**
		 * Create a model from a model desciptor.
		 *
		 * @param descriptor A Black Scholes model descriptor.
		 */
		private BlackScholesModelFourier(BlackScholesModelDescriptor descriptor) {
			super(
					descriptor.getInitialValue(),
					descriptor.getDiscountCurveForForwardRate(),
					descriptor.getVolatility(),
					descriptor.getDiscountCurveForDiscountRate()
					);
			this.descriptor 	= descriptor;
			productFactory = new SingleAssetFourierProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public BlackScholesModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(
				ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}
	}

	/**
	 * A described Heston model using Fourier method for evaluation.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private static class HestonModelFourier extends HestonModel implements DescribedModel<HestonModelDescriptor> {

		private final HestonModelDescriptor descriptor;
		private final SingleAssetFourierProductFactory productFactory;

		/**
		 * Create a model from a model desciptor.
		 *
		 * @param descriptor A Heston model descriptor.
		 */
		private HestonModelFourier(HestonModelDescriptor descriptor) {
			super(
					descriptor.getInitialValue(),
					descriptor.getDiscountCurveForForwardRate(),
					descriptor.getVolatility(),
					descriptor.getDiscountCurveForDiscountRate(),
					descriptor.getTheta(),
					descriptor.getKappa(),
					descriptor.getXi(),
					descriptor.getRho()
					);
			this.descriptor 	= descriptor;
			productFactory = new SingleAssetFourierProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public HestonModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(
				ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}
	}
}
