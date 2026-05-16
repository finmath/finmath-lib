package net.finmath.finitedifference.assetderivativevaluation.products;

import net.finmath.finitedifference.FiniteDifferenceProduct;
import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;

/**
 * Interface for products valued by a finite-difference equity model.
 *
 * <p>
 * This interface specializes the generic
 * {@link net.finmath.finitedifference.FiniteDifferenceProduct} to the case of
 * equity finite-difference models.
 * </p>
 *
 * <p>
 * Implementations provide valuation methods compatible with
 * {@link FiniteDifferenceEquityModel}. In addition to the standard
 * finite-difference product interface, this interface fixes the admissible
 * model type to {@link FiniteDifferenceEquityModel}.
 * </p>
 *
 * @author Christian Fries
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceEquityProduct extends FiniteDifferenceProduct<FiniteDifferenceEquityModel> {

	@Override
	default Class<FiniteDifferenceEquityModel> getModelClass() {
		return FiniteDifferenceEquityModel.class;
	}
}
