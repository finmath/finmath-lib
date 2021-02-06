package net.finmath.modelling.modelfactory;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.modelling.descriptor.MertonModelDescriptor;
import net.finmath.modelling.descriptor.VarianceGammaModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetMonteCarloProductFactory;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModelWithCurves;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel;
import net.finmath.montecarlo.assetderivativevaluation.models.HestonModel.Scheme;

/**
 * Constructs asset models, which evaluate products via Monte-Carlo method.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AssetModelMonteCarloFactory implements ModelFactory<AssetModelDescriptor> {

	private final HestonModel.Scheme scheme;
	private final RandomVariableFactory randomVariableFactory;
	private final IndependentIncrements stochasticDriver;


	/**
	 * Create the factory.
	 *
	 * @param randomVariableFactory The factory to be used by the models to construct random variables.
	 * @param stochasticDriver The stochastic driver of the process.
	 * @param scheme Truncation scheme to be used by the model in the calculation of drift and diffusion coefficients. (Optional parameter, only required by Heston Model).
	 */
	public AssetModelMonteCarloFactory(final RandomVariableFactory randomVariableFactory,
			final IndependentIncrements stochasticDriver, final Scheme scheme) {
		super();
		this.scheme = scheme;
		this.randomVariableFactory = randomVariableFactory;
		this.stochasticDriver = stochasticDriver;
	}

	/**
	 * Create the factory.
	 *
	 * @param randomVariableFactory The factory to be used by the models to construct random variables.
	 * @param stochasticDriver The stochastic driver of the process.
	 */
	public AssetModelMonteCarloFactory(final RandomVariableFactory randomVariableFactory,
			IndependentIncrements stochasticDriver) {
		super();
		this.scheme = null;
		this.randomVariableFactory = randomVariableFactory;
		this.stochasticDriver = stochasticDriver;
	}

	/**
	 * Create the factory.
	 *
	 * @param stochasticDriver The stochastic driver of the process.
	 */
	public AssetModelMonteCarloFactory(IndependentIncrements stochasticDriver) {
		super();
		this.scheme = null;
		this.randomVariableFactory =  new RandomVariableFromArrayFactory();
		this.stochasticDriver = stochasticDriver;
	}

	@Override
	public DescribedModel<? extends AssetModelDescriptor> getModelFromDescriptor(final AssetModelDescriptor descriptor) {

		if(descriptor instanceof BlackScholesModelDescriptor) {
			final DescribedModel<BlackScholesModelDescriptor> model = new BlackScholesModelMonteCarlo((BlackScholesModelDescriptor) descriptor, randomVariableFactory, stochasticDriver);
			return model;
		}
		else if(descriptor instanceof HestonModelDescriptor) {
			if(scheme == null) {
				throw new RuntimeException("Need to provide truncation scheme to factory in order to be able to build a Heston Model");
			}
			final DescribedModel<HestonModelDescriptor> model = new HestonModelMonteCarlo((HestonModelDescriptor) descriptor, scheme, randomVariableFactory, stochasticDriver);
			return model;
		}
		else if(descriptor instanceof MertonModelDescriptor) {
			final DescribedModel<MertonModelDescriptor> model = new MertonModelMonteCarlo((MertonModelDescriptor) descriptor, randomVariableFactory, stochasticDriver);
			return model;
		}
		else if(descriptor instanceof VarianceGammaModelDescriptor) {
			final DescribedModel<VarianceGammaModelDescriptor> model = new VarianceGammaModelMonteCarlo((VarianceGammaModelDescriptor) descriptor, stochasticDriver);
			return model;
		}
		else {
			final String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}



	/**
	 * A described Black-Scholes model using Monte Carlo method for evaluation.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private static class BlackScholesModelMonteCarlo extends MonteCarloAssetModel implements DescribedModel<BlackScholesModelDescriptor> {

		private final BlackScholesModelDescriptor descriptor;

		private final SingleAssetMonteCarloProductFactory productFactory;

		/**
		 * Create the described model.
		 *
		 * @param descriptor The descriptor of the model.
		 * @param randomVariableFactory The factory to be used by the models to construct random variables.
		 * @param stochasticDriver The stochastic driver of the process.
		 */
		private BlackScholesModelMonteCarlo(final BlackScholesModelDescriptor descriptor, final RandomVariableFactory randomVariableFactory,
				final IndependentIncrements stochasticDriver) {
			super(new BlackScholesModelWithCurves(
					descriptor.getInitialValue(),
					descriptor.getDiscountCurveForForwardRate(),
					descriptor.getVolatility(),
					descriptor.getDiscountCurveForDiscountRate(),
					randomVariableFactory),
					stochasticDriver);
			this.descriptor 	= descriptor;
			productFactory = new SingleAssetMonteCarloProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public BlackScholesModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(final ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}
	}

	/**
	 * A described Heston model using Monte Carlo method for evaluation.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	private static class HestonModelMonteCarlo extends MonteCarloAssetModel implements DescribedModel<HestonModelDescriptor> {

		private final HestonModelDescriptor descriptor;

		private final SingleAssetMonteCarloProductFactory productFactory;

		/**
		 * Create the described model.
		 *
		 * @param descriptor The descriptor of the model.
		 * @param scheme Truncation scheme to be used by the model in the calculation of drift and diffusion coefficients.
		 * @param randomVariableFactory The factory to be used by the models to construct random variables.
		 * @param stochasticDriver The stochastic driver of the process.
		 */
		private HestonModelMonteCarlo(final HestonModelDescriptor descriptor, final Scheme scheme, final RandomVariableFactory randomVariableFactory,
				final IndependentIncrements stochasticDriver) {
			super(new net.finmath.montecarlo.assetderivativevaluation.models.HestonModel(descriptor, scheme, randomVariableFactory),
					stochasticDriver);
			this.descriptor 	= descriptor;
			productFactory = new SingleAssetMonteCarloProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public HestonModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(final ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}
	}

	/**
	 * A described Merton model using Monte Carlo method for evaluation.
	 *
	 * @author Alessandro Gnoatto
	 *
	 */
	private static class MertonModelMonteCarlo extends MonteCarloAssetModel implements DescribedModel<MertonModelDescriptor>{

		private final MertonModelDescriptor descriptor;

		private final SingleAssetMonteCarloProductFactory productFactory;

		private MertonModelMonteCarlo(final MertonModelDescriptor descriptor, final RandomVariableFactory randomVariableFactory,
				final IndependentIncrements stochasticDriver) {
			super(new net.finmath.montecarlo.assetderivativevaluation.models.MertonModel(descriptor),
					stochasticDriver);
			this.descriptor = descriptor;
			productFactory = new SingleAssetMonteCarloProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public MertonModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(final ProductDescriptor productDescriptor) {
			return productFactory.getProductFromDescriptor(productDescriptor);
		}
	}

	/**
	 * A described Variance Gamma model using Monte Carlo method for evaluation.
	 *
	 * @author Alessandro Gnoatto
	 *
	 */
	private static class VarianceGammaModelMonteCarlo extends MonteCarloAssetModel implements DescribedModel<VarianceGammaModelDescriptor>{

		private final VarianceGammaModelDescriptor descriptor;

		private final SingleAssetMonteCarloProductFactory productFactory;

		private VarianceGammaModelMonteCarlo(VarianceGammaModelDescriptor descriptor, IndependentIncrements stochasticDriver) {
			super(new net.finmath.montecarlo.assetderivativevaluation.models.VarianceGammaModel(descriptor),
					stochasticDriver);
			this.descriptor = descriptor;
			productFactory = new SingleAssetMonteCarloProductFactory(descriptor.getReferenceDate());
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
