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
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetMonteCarloProductFactory;
import net.finmath.montecarlo.IndependentIncrements;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloAssetModel;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcess;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class BlackScholesModelMonteCarloFactory implements ModelFactory<BlackScholesModelDescriptor> {

	private final RandomVariableFactory randomVariableFactory;
	private final IndependentIncrements brownianMotion;


	public BlackScholesModelMonteCarloFactory(final RandomVariableFactory randomVariableFactory, final IndependentIncrements brownianMotion) {
		super();
		this.randomVariableFactory = randomVariableFactory;
		this.brownianMotion = brownianMotion;
	}

	@Override
	public DescribedModel<BlackScholesModelDescriptor> getModelFromDescriptor(final BlackScholesModelDescriptor modelDescriptor) {

		/*
		 * Build model from description.
		 * Adding product factory.
		 *
		 * We build the class implementing DescribedModel<BlackScholesModelDescriptor> as an inner class.
		 * For larger applications this should be a dedicated class file.
		 */
		final AbstractProcessModel model = new net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModelWithCurves(
				modelDescriptor.getInitialValue(),
				modelDescriptor.getDiscountCurveForForwardRate(),
				modelDescriptor.getVolatility(),
				modelDescriptor.getDiscountCurveForDiscountRate(),
				randomVariableFactory
				);

		class BlackScholesMonteCarloModel extends MonteCarloAssetModel implements DescribedModel<BlackScholesModelDescriptor> {

			private final SingleAssetMonteCarloProductFactory productFactory = new SingleAssetMonteCarloProductFactory(modelDescriptor.getReferenceDate());

			BlackScholesMonteCarloModel(final ProcessModel model, final MonteCarloProcess process) {
				super(model, process);
			}

			@Override
			public BlackScholesModelDescriptor getDescriptor() {
				return modelDescriptor;
			}

			@Override
			public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(final ProductDescriptor productDescriptor) {
				if(productDescriptor instanceof SingleAssetProductDescriptor) {
					return productFactory.getProductFromDescriptor(productDescriptor);
				}
				else {
					final String name = modelDescriptor.name();
					throw new IllegalArgumentException("Unsupported product type " + name);
				}
			}
		}

		return new BlackScholesMonteCarloModel(model, new EulerSchemeFromProcessModel(model, brownianMotion));
	}

}
