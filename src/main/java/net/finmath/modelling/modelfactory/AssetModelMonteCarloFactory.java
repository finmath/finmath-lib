package net.finmath.modelling.modelfactory;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetMonteCarloProductFactory;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.IndependentIncrementsInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.assetderivativevaluation.BlackScholesModelWithCurves;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel;
import net.finmath.montecarlo.assetderivativevaluation.HestonModel.Scheme;
import net.finmath.montecarlo.process.ProcessEulerScheme;

/**
 * Constructs asset models, which evaluate products via Monte-Carlo method.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class AssetModelMonteCarloFactory implements ModelFactory<AssetModelDescriptor> {

	private final HestonModel.Scheme scheme;
	private final AbstractRandomVariableFactory randomVariableFactory;
	private final IndependentIncrementsInterface stochasticDriver;


	/**
	 * Create the factory.
	 *
	 * @param randomVariableFactory The factory to be used by the models to construct random variables.
	 * @param stochasticDriver The stochastic driver of the process.
	 * @param scheme Truncation scheme to be used by the model in the calculation of drift and diffusion coefficients. (Optional parameter, only required by Heston Model).
	 */
	public AssetModelMonteCarloFactory(AbstractRandomVariableFactory randomVariableFactory,
			IndependentIncrementsInterface stochasticDriver, Scheme scheme) {
		super();
		this.scheme = scheme;
		this.randomVariableFactory = randomVariableFactory;
		this.stochasticDriver = stochasticDriver;
	}
	@Override
	public DescribedModel<? extends AssetModelDescriptor> getModelFromDescriptor(AssetModelDescriptor descriptor) {

		if(descriptor instanceof BlackScholesModelDescriptor) {
			DescribedModel<BlackScholesModelDescriptor> model = new BlackScholesModelMonteCarlo((BlackScholesModelDescriptor) descriptor, randomVariableFactory, stochasticDriver);
			return model;
		}
		else if(descriptor instanceof HestonModelDescriptor) {
			if(scheme == null) {
				throw new RuntimeException("Need to provide truncation scheme to factory in order to be able to build a Heston Model");
			}
			DescribedModel<HestonModelDescriptor> model = new HestonModelMonteCarlo((HestonModelDescriptor) descriptor, scheme, randomVariableFactory, stochasticDriver);
			return model;
		}
		else {
			String name = descriptor.name();
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
		private BlackScholesModelMonteCarlo(BlackScholesModelDescriptor descriptor, AbstractRandomVariableFactory randomVariableFactory,
				IndependentIncrementsInterface stochasticDriver) {
			super(new BlackScholesModelWithCurves(
						descriptor.getInitialValue(),
						descriptor.getDiscountCurveForForwardRate(),
						descriptor.getVolatility(),
						descriptor.getDiscountCurveForDiscountRate(),
						randomVariableFactory),
					new ProcessEulerScheme(stochasticDriver));
			this.descriptor 	= descriptor;
			this.productFactory = new SingleAssetMonteCarloProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public BlackScholesModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {
				return productFactory.getProductFromDescriptor((SingleAssetProductDescriptor) productDescriptor);
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
		private HestonModelMonteCarlo(HestonModelDescriptor descriptor, Scheme scheme, AbstractRandomVariableFactory randomVariableFactory,
				IndependentIncrementsInterface stochasticDriver) {
			super(new net.finmath.montecarlo.assetderivativevaluation.HestonModel(descriptor, scheme, randomVariableFactory),
					new ProcessEulerScheme(stochasticDriver));
			this.descriptor 	= descriptor;
			this.productFactory = new SingleAssetMonteCarloProductFactory(descriptor.getReferenceDate());
		}

		@Override
		public HestonModelDescriptor getDescriptor() {
			return descriptor;
		}

		@Override
		public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {
				return productFactory.getProductFromDescriptor((SingleAssetProductDescriptor) productDescriptor);
		}
	}
}
