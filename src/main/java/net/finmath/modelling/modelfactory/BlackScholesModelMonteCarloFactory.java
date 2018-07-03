/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.modelfactory;

import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ModelFactory;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetMonteCarloProductFactory;
import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.IndependentIncrementsInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.montecarlo.process.ProcessEulerScheme;

/**
 * @author Christian Fries
 */
public class BlackScholesModelMonteCarloFactory implements ModelFactory<BlackScholesModelDescriptor> {

	private final AbstractRandomVariableFactory randomVariableFactory;
	private final IndependentIncrementsInterface brownianMotion;


	public BlackScholesModelMonteCarloFactory(AbstractRandomVariableFactory randomVariableFactory, IndependentIncrementsInterface brownianMotion) {
		super();
		this.randomVariableFactory = randomVariableFactory;
		this.brownianMotion = brownianMotion;
	}

	@Override
	public DescribedModel<BlackScholesModelDescriptor> getModelFromDescriptor(BlackScholesModelDescriptor modelDescriptor) {
		
		/*
		 * Build model from description.
		 * Adding product factory.
		 * 
		 * We build the class implementing DescribedModel<BlackScholesModelDescriptor> as an inner class.
		 * For larger applications this should be a dedicated class file.
		 */
		AbstractModel model = new net.finmath.montecarlo.assetderivativevaluation.BlackScholesModelWithCurves(
				modelDescriptor.getInitialValue(),
				modelDescriptor.getDiscountCurveForForwardRate(),
				modelDescriptor.getVolatility(),
				modelDescriptor.getDiscountCurveForDiscountRate(),
				randomVariableFactory
				);

		class BlackScholesMonteCarloModel extends MonteCarloAssetModel implements DescribedModel<BlackScholesModelDescriptor> {

			final SingleAssetMonteCarloProductFactory productFactory = new SingleAssetMonteCarloProductFactory();
			
			BlackScholesMonteCarloModel(AbstractModelInterface model, AbstractProcessInterface process) {
				super(model, process);
			}
			
			@Override
			public BlackScholesModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {
				if(productDescriptor instanceof SingleAssetProductDescriptor) {
					return productFactory.getProductFromDescription((SingleAssetProductDescriptor) productDescriptor);
				}
				else {
					String name = modelDescriptor.name();
					throw new IllegalArgumentException("Unsupported product type " + name);
				}
			}

			@Override
			public DescribedModel<BlackScholesModelDescriptor> getModelWithProductFactory(
					ProductFactory<? extends ProductDescriptor, BlackScholesModelDescriptor> productFactory,
					boolean append) {
				// TODO Auto-generated method stub
				return null;
			}	
		}

        return new BlackScholesMonteCarloModel(model, new ProcessEulerScheme(brownianMotion));
	}

}
