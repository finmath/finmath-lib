/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 22.11.2009
 */
package net.finmath.montecarlo.interestrate.products.components;

import java.util.ArrayList;
import java.util.Set;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.conditionalexpectation.RegressionBasisFunctionsProvider;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * An option.
 *
 * Implements the function <code>max(underlying(t)-strike,0)</code> for any <code>underlying</code> object
 * implementing an {@link net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct}.
 *
 * The strike may be a fixed constant value or an object implementing
 * {@link net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct}
 * (resulting in a stochastic strike or exchange option).
 *
 * More precise, the <code>getVaue</code> method returns the value
 * \[
 * 	\left\{
 * 		\begin{array}{ll}
 * 			U(t)-S(t) &amp; \text{if E(t) &gt; 0} \\
 * 			U(t)-S(t) &amp; \text{else.}
 * 		\end{array}
 * 	\right.
 * \]
 * where \( E \) is an estimator for the expectation of \( U(t)-S(t) \) and \( U \) is the value
 * returned by the call to <code>getValue</code> of the underlying product, which may return a
 * sum on discounted futures cash-flows / values (i.e. not yet performing the expectation) and
 * \( S \) is the strike (which may be a fixed value or another underlying product).
 *
 * @author Christian Fries
 * @version 1.2
 * @see net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct
 */
public class Option extends AbstractProductComponent implements RegressionBasisFunctionsProvider {

	private static final long serialVersionUID = 2987369289230532162L;

	private final double							exerciseDate;
	private final double							strikePrice;
	private final AbstractLIBORMonteCarloProduct	underlying;
	private final TermStructureMonteCarloProduct	strikeProduct;
	private final boolean							isCall;

	private final RegressionBasisFunctionsProvider	regressionBasisFunctionsProvider;

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice
	 *
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param strikePrice The strike price.
	 * @param isCall If true, the function implements is underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice. Otherwise it is underlying(exerciseDate) &lt; strikePrice ? underlying : strikePrice.
	 * @param underlying The underlying.
	 * @param regressionBasisFunctionsProvider Used to determine the regression basis functions for the conditional expectation operator.
	 */
	public Option(final double exerciseDate, final double strikePrice, final boolean isCall, final AbstractLIBORMonteCarloProduct underlying, final RegressionBasisFunctionsProvider	regressionBasisFunctionsProvider) {
		super();
		this.exerciseDate	= exerciseDate;
		this.strikePrice	= strikePrice;
		this.underlying		= underlying;
		this.isCall			= isCall;
		strikeProduct	= null;
		this.regressionBasisFunctionsProvider = regressionBasisFunctionsProvider;
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikeProduct ? underlying : strikeProduct
	 *
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param isCall If true, the function implements is underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice. Otherwise it is underlying(exerciseDate) &lt; strikePrice ? underlying : strikePrice.
	 * @param strikeProduct The strike (can be a general AbstractLIBORMonteCarloProduct).
	 * @param underlying The underlying.
	 * @param regressionBasisFunctionsProvider Used to determine the regression basis functions for the conditional expectation operator.
	 */
	public Option(final double exerciseDate, final boolean isCall,  final TermStructureMonteCarloProduct strikeProduct, final AbstractLIBORMonteCarloProduct underlying, final RegressionBasisFunctionsProvider	regressionBasisFunctionsProvider) {
		super();
		this.exerciseDate	= exerciseDate;
		strikePrice	= Double.NaN;
		this.strikeProduct	= strikeProduct;
		this.underlying		= underlying;
		this.isCall			= isCall;
		this.regressionBasisFunctionsProvider = regressionBasisFunctionsProvider;
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikeProduct ? underlying : strikeProduct
	 *
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param isCall If true, the function implements is underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice. Otherwise it is underlying(exerciseDate) &lt; strikePrice ? underlying : strikePrice.
	 * @param strikeProduct The strike (can be a general AbstractLIBORMonteCarloProduct).
	 * @param underlying The underlying.
	 */
	public Option(final double exerciseDate, final boolean isCall,  final TermStructureMonteCarloProduct strikeProduct, final AbstractLIBORMonteCarloProduct underlying) {
		this(exerciseDate, isCall, strikeProduct, underlying, null);
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice
	 *
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param strikePrice The strike price.
	 * @param isCall If true, the function implements is underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice. Otherwise it is underlying(exerciseDate) &lt; strikePrice ? underlying : strikePrice.
	 * @param underlying The underlying.
	 */
	public Option(final double exerciseDate, final double strikePrice, final boolean isCall, final AbstractLIBORMonteCarloProduct underlying) {
		this(exerciseDate, strikePrice, isCall, underlying, null);
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; strikePrice ? underlying : strikePrice
	 *
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param strikePrice The strike price.
	 * @param underlying The underlying.
	 */
	public Option(final double exerciseDate, final double strikePrice, final AbstractLIBORMonteCarloProduct underlying) {
		this(exerciseDate, strikePrice, true, underlying);
	}

	/**
	 * Creates the function underlying(exerciseDate) &ge; 0 ? underlying : 0
	 *
	 * @param exerciseDate The exercise date of the option (given as a double).
	 * @param underlying The underlying.
	 */
	public Option(final double exerciseDate, final AbstractLIBORMonteCarloProduct underlying) {
		this(exerciseDate, 0.0, underlying);
	}

	@Override
	public String getCurrency() {
		return underlying.getCurrency();
	}

	@Override
	public Set<String> queryUnderlyings() {
		Set<String> underlyingNames = null;
		for(final TermStructureMonteCarloProduct product : new TermStructureMonteCarloProduct[] {underlying, strikeProduct}) {
			if(product instanceof AbstractProductComponent) {
				final Set<String> productUnderlyingNames = ((AbstractProductComponent)product).queryUnderlyings();
				if(productUnderlyingNames != null) {
					if(underlyingNames == null) {
						underlyingNames = productUnderlyingNames;
					} else {
						underlyingNames.addAll(productUnderlyingNames);
					}
				}
				else {
					throw new IllegalArgumentException("Underlying cannot be queried for underlyings.");
				}
			}
		}
		return underlyingNames;
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {

		final RandomVariable one	= model.getRandomVariableForConstant(1.0);
		final RandomVariable zero	= model.getRandomVariableForConstant(0.0);

		// TODO >=? -
		if(evaluationTime > exerciseDate) {
			return zero;
		}

		RandomVariable values = underlying.getValue(exerciseDate, model);
		RandomVariable strike;
		if(strikeProduct != null) {
			strike = strikeProduct.getValue(exerciseDate, model);
		} else {
			strike = model.getRandomVariableForConstant(strikePrice);
		}

		RandomVariable exerciseTrigger = values.sub(strike).mult(isCall ? 1.0 : -1.0);

		if(exerciseTrigger.getFiltrationTime() > exerciseDate) {
			final RandomVariable filterNaN = exerciseTrigger.isNaN().sub(1.0).mult(-1.0);
			final RandomVariable exerciseTriggerFiltered = exerciseTrigger.mult(filterNaN);

			/*
			 * Cut off two standard deviations from regression
			 */
			final double exerciseTriggerMean		= exerciseTriggerFiltered.getAverage();
			final double exerciseTriggerStdDev	= exerciseTriggerFiltered.getStandardDeviation();
			final double exerciseTriggerFloor		= exerciseTriggerMean*(1.0-Math.signum(exerciseTriggerMean)*1E-5)-3.0*exerciseTriggerStdDev;
			final double exerciseTriggerCap		= exerciseTriggerMean*(1.0+Math.signum(exerciseTriggerMean)*1E-5)+3.0*exerciseTriggerStdDev;
			RandomVariable filter = exerciseTrigger.sub(exerciseTriggerFloor).choose(one, zero)
					.mult(exerciseTrigger.sub(exerciseTriggerCap).mult(-1.0).choose(one, zero));
			filter = filter.mult(filterNaN);
			// Filter exerciseTrigger and regressionBasisFunctions
			exerciseTrigger = exerciseTrigger.mult(filter);

			final RandomVariable[] regressionBasisFunctions			= regressionBasisFunctionsProvider != null ? regressionBasisFunctionsProvider.getBasisFunctions(evaluationTime, model) : getBasisFunctions(exerciseDate, model);
			final RandomVariable[] filteredRegressionBasisFunctions	= new RandomVariable[regressionBasisFunctions.length];
			for(int i=0; i<regressionBasisFunctions.length; i++) {
				filteredRegressionBasisFunctions[i] = regressionBasisFunctions[i].mult(filter);
			}

			// Remove foresight through conditional expectation
			final ConditionalExpectationEstimator conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(filteredRegressionBasisFunctions, regressionBasisFunctions);

			// Calculate cond. expectation. Note that no discounting (numeraire division) is required!
			exerciseTrigger         = exerciseTrigger.getConditionalExpectation(conditionalExpectationOperator);
		}

		// Apply exercise criteria
		if(strikeProduct != null) {
			values = exerciseTrigger.choose(values, strikeProduct.getValue(exerciseDate, model));
		} else {
			values = exerciseTrigger.choose(values, new Scalar(strikePrice));
		}

		// Discount to evaluation time
		if(evaluationTime != exerciseDate) {
			final RandomVariable	numeraireAtEval			= model.getNumeraire(evaluationTime);
			final RandomVariable	numeraire				= model.getNumeraire(exerciseDate);
			values = values.div(numeraire).mult(numeraireAtEval);
		}

		// Return values
		return values;
	}

	@Override
	public RandomVariable[] getBasisFunctions(final double evaluationTime, final MonteCarloSimulationModel model) throws CalculationException {
		if(model instanceof LIBORModelMonteCarloSimulationModel) {
			return getBasisFunctions(evaluationTime, (LIBORModelMonteCarloSimulationModel)model);
		}
		else {
			throw new IllegalArgumentException("getBasisFunctions requires an model of type LIBORModelMonteCarloSimulationModel.");
		}
	}

	/**
	 * Return the regression basis functions.
	 *
	 * @param exerciseDate The date w.r.t. which the basis functions should be measurable.
	 * @param model The model.
	 * @return Array of random variables.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public RandomVariable[] getBasisFunctions(final double exerciseDate, final LIBORModelMonteCarloSimulationModel model) throws CalculationException {

		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		RandomVariable basisFunction;

		// Constant
		basisFunction = model.getRandomVariableForConstant(1.0);
		basisFunctions.add(basisFunction);

		// LIBORs
		int liborPeriodIndex, liborPeriodIndexEnd;
		RandomVariable rate;

		// 1 Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) {
			liborPeriodIndex = -liborPeriodIndex-1;
		}
		liborPeriodIndexEnd = liborPeriodIndex+1;
		final double periodLength1 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

		basisFunction = basisFunction.discount(rate, periodLength1);
		basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

		// n/2 Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) {
			liborPeriodIndex = -liborPeriodIndex-1;
		}
		liborPeriodIndexEnd = (liborPeriodIndex + model.getNumberOfLibors())/2;

		final double periodLength2 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength2 != periodLength1) {
			rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength2);
			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength2);
			//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength2);
			//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));
		}


		// n Period
		basisFunction = model.getRandomVariableForConstant(1.0);
		liborPeriodIndex = model.getLiborPeriodIndex(exerciseDate);
		if(liborPeriodIndex < 0) {
			liborPeriodIndex = -liborPeriodIndex-1;
		}
		liborPeriodIndexEnd = model.getNumberOfLibors();
		final double periodLength3 = model.getLiborPeriod(liborPeriodIndexEnd) - model.getLiborPeriod(liborPeriodIndex);

		if(periodLength3 != periodLength1 && periodLength3 != periodLength2) {
			rate = model.getForwardRate(exerciseDate, model.getLiborPeriod(liborPeriodIndex), model.getLiborPeriod(liborPeriodIndexEnd));
			basisFunction = basisFunction.discount(rate, periodLength3);
			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));

			basisFunction = basisFunction.discount(rate, periodLength3);
			//			basisFunctions.add(basisFunction);//.div(Math.sqrt(basisFunction.mult(basisFunction).getAverage())));
		}

		return basisFunctions.toArray(new RandomVariable[0]);
	}

	@Override
	public String toString() {
		return "Option [exerciseDate=" + exerciseDate + ", strikePrice="
				+ strikePrice + ", underlying=" + underlying + ", isCall="
				+ isCall + ", toString()=" + super.toString() + "]";
	}
}
