package net.finmath.modelling.modelfactory;

import net.finmath.fouriermethod.models.BlackScholesModel;
import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.fouriermethod.models.MertonModel;
import net.finmath.fouriermethod.models.VarianceGammaModel;
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.modelling.descriptor.MertonModelDescriptor;
import net.finmath.modelling.descriptor.VarianceGammaModelDescriptor;
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
	public DescribedModel<? extends AssetModelDescriptor> getModelFromDescriptor(final AssetModelDescriptor descriptor) {

		if(descriptor instanceof BlackScholesModelDescriptor) {
			final DescribedModel<BlackScholesModelDescriptor> model = new BlackScholesModelFourier((BlackScholesModelDescriptor) descriptor);
			return model;
		}
		else if(descriptor instanceof HestonModelDescriptor) {
			final DescribedModel<HestonModelDescriptor> model = new HestonModelFourier((HestonModelDescriptor) descriptor);
			return model;
		}
		else if(descriptor instanceof MertonModelDescriptor) {
			final DescribedModel<MertonModelDescriptor> model = new MertonModelFourier((MertonModelDescriptor) descriptor);
			return model;
		}
		else if(descriptor instanceof VarianceGammaModelDescriptor) {
			final DescribedModel<VarianceGammaModelDescriptor> model = new VarianceGammaModelFourier((VarianceGammaModelDescriptor) descriptor);
			return model;
		}
		else {
			final String name = descriptor.name();
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
		private BlackScholesModelFourier(final BlackScholesModelDescriptor descriptor) {
			super(
					null,
					descriptor.getInitialValue(),
					descriptor.getDiscountCurveForForwardRate(),
					descriptor.getDiscountCurveForDiscountRate(), descriptor.getVolatility()
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
				final ProductDescriptor productDescriptor) {
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
		private HestonModelFourier(final HestonModelDescriptor descriptor) {
			super(
					descriptor.getInitialValue(),
					descriptor.getDiscountCurveForForwardRate(),
					descriptor.getDiscountCurveForDiscountRate(),
					descriptor.getVolatility(),
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
				final ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}
	}

	/**
	 * A described Merton Jump Diffusion model using Fourier method for evaluation.
	 *
	 * @author Alessandro Gnoatto
	 *
	 */
	private static class MertonModelFourier extends MertonModel implements DescribedModel<MertonModelDescriptor>{

		private final MertonModelDescriptor descriptor;
		private final SingleAssetFourierProductFactory productFactory;

		private MertonModelFourier(final MertonModelDescriptor descriptor) {
			super(
					null,
					descriptor.getInitialValue(),
					descriptor.getDiscountCurveForForwardRate(),
					descriptor.getDiscountCurveForDiscountRate(),
					descriptor.getVolatility(),
					descriptor.getJumpIntensity(),
					descriptor.getJumpSizeMean(), descriptor.getJumpSizeStdDev()
					);
			this.descriptor = descriptor;
			productFactory = new SingleAssetFourierProductFactory(descriptor.getReferenceDate());
		}
		@Override
		public MertonModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(
				final ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}

	}

	/**
	 * A described Variance Gamma model using Fourier method for evaluation.
	 *
	 * @author Alessandro Gnoatto
	 *
	 */
	private static class VarianceGammaModelFourier extends VarianceGammaModel implements DescribedModel<VarianceGammaModelDescriptor>{

		private final VarianceGammaModelDescriptor descriptor;
		private final SingleAssetFourierProductFactory productFactory;

		private VarianceGammaModelFourier(VarianceGammaModelDescriptor descriptor) {
			super(null,
					descriptor.getInitialValue(),
					descriptor.getDiscountCurveForForwardRate(),
					descriptor.getDiscountCurveForDiscountRate(),
					descriptor.getSigma(),
					descriptor.getTheta(),
					descriptor.getNu());

			this.descriptor = descriptor;
			productFactory = new SingleAssetFourierProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public VarianceGammaModelDescriptor getDescriptor() {
			return descriptor;
		}
		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(
				ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}

	}
}
